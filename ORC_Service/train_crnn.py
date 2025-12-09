"""
Script huấn luyện mô hình CRNN để nhận dạng văn bản
Sử dụng CTC Loss và các kỹ thuật augmentation
"""

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from torch.nn import functional as F
import cv2
import numpy as np
from pathlib import Path
import string
from tqdm import tqdm
import json

from crnn_model import create_crnn_model, CTCLoss

# Character set: printable ASCII
CHARS = string.digits + string.ascii_letters + string.punctuation + ' '
CHAR_TO_IDX = {char: idx + 1 for idx, char in enumerate(CHARS)}  # 0 reserved for blank
IDX_TO_CHAR = {idx + 1: char for idx, char in enumerate(CHARS)}
IDX_TO_CHAR[0] = ''  # Blank character
NUM_CLASSES = len(CHARS) + 1  # +1 for CTC blank

class TextImageDataset(Dataset):
    """
    Dataset cho CRNN training
    Đọc ảnh và text labels từ file
    """
    
    def __init__(self, image_dir, labels_file, img_height=32, img_width=None, augment=False):
        """
        Args:
            image_dir: Thư mục chứa ảnh
            labels_file: File chứa labels (format: filename\ttext)
            img_height: Chiều cao ảnh (resize)
            img_width: Chiều rộng ảnh (None = giữ nguyên aspect ratio)
            augment: Có sử dụng augmentation không
        """
        self.image_dir = Path(image_dir)
        self.img_height = img_height
        self.img_width = img_width
        self.augment = augment
        
        # Load labels
        self.samples = []
        with open(labels_file, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                parts = line.split('\t')
                if len(parts) >= 2:
                    filename = parts[0]
                    text = '\t'.join(parts[1:])  # Text có thể chứa tab
                    
                    # Filter text chỉ giữ lại ký tự trong CHARS
                    filtered_text = ''.join([c for c in text if c in CHARS])
                    if filtered_text:
                        self.samples.append((filename, filtered_text))
        
        print(f"Loaded {len(self.samples)} samples from {labels_file}")
    
    def __len__(self):
        return len(self.samples)
    
    def __getitem__(self, idx):
        filename, text = self.samples[idx]
        
        # Load image
        img_path = self.image_dir / filename
        image = cv2.imread(str(img_path))
        
        if image is None:
            # Return dummy data nếu không đọc được ảnh
            image = np.zeros((self.img_height, 100, 3), dtype=np.uint8)
        
        # Convert to grayscale
        if len(image.shape) == 3:
            image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        
        # Resize
        h, w = image.shape
        if self.img_width is not None:
            new_w = self.img_width
        else:
            # Keep aspect ratio
            new_w = int(w * self.img_height / h)
            new_w = max(new_w, 1)  # At least 1 pixel
        
        image = cv2.resize(image, (new_w, self.img_height))
        
        # Augmentation
        if self.augment:
            image = self.augment_image(image)
        
        # Normalize
        image = image.astype(np.float32) / 255.0
        image = (image - 0.5) / 0.5  # Normalize to [-1, 1]
        
        # Add channel dimension
        image = np.expand_dims(image, axis=0)  # (1, H, W)
        
        # Convert text to indices
        text_indices = [CHAR_TO_IDX[c] for c in text]
        
        return {
            'image': torch.FloatTensor(image),
            'text': text,
            'text_indices': torch.LongTensor(text_indices),
            'text_length': len(text_indices)
        }
    
    def augment_image(self, image):
        """Simple augmentation"""
        # Random brightness
        if np.random.rand() < 0.5:
            alpha = np.random.uniform(0.8, 1.2)
            image = np.clip(image * alpha, 0, 255).astype(np.uint8)
        
        # Random contrast
        if np.random.rand() < 0.5:
            alpha = np.random.uniform(0.8, 1.2)
            mean = image.mean()
            image = np.clip((image - mean) * alpha + mean, 0, 255).astype(np.uint8)
        
        return image

def collate_fn(batch):
    """
    Collate function cho DataLoader
    Xử lý batch với các ảnh có width khác nhau
    """
    images = [item['image'] for item in batch]
    texts = [item['text'] for item in batch]
    text_indices = [item['text_indices'] for item in batch]
    text_lengths = [item['text_length'] for item in batch]
    
    # Pad images to same width
    max_width = max([img.shape[2] for img in images])
    batch_size = len(images)
    img_height = images[0].shape[1]
    
    padded_images = torch.zeros(batch_size, 1, img_height, max_width)
    for i, img in enumerate(images):
        padded_images[i, :, :, :img.shape[2]] = img
    
    # Concatenate text indices
    text_indices_cat = torch.cat(text_indices)
    text_lengths = torch.LongTensor(text_lengths)
    
    return {
        'images': padded_images,
        'texts': texts,
        'text_indices': text_indices_cat,
        'text_lengths': text_lengths
    }

def decode_predictions(preds, blank=0):
    """
    Decode CTC predictions
    
    Args:
        preds: (seq_len, batch, num_classes) - Log probabilities
        blank: Blank token index
    
    Returns:
        decoded: List of decoded strings
    """
    # Get best path
    _, max_indices = preds.max(2)  # (seq_len, batch)
    max_indices = max_indices.transpose(0, 1)  # (batch, seq_len)
    
    decoded = []
    for indices in max_indices:
        # Remove consecutive duplicates and blank
        chars = []
        prev_idx = None
        for idx in indices:
            idx = idx.item()
            if idx != blank and idx != prev_idx:
                if idx in IDX_TO_CHAR:
                    chars.append(IDX_TO_CHAR[idx])
            prev_idx = idx
        decoded.append(''.join(chars))
    
    return decoded

def train_crnn(
    train_dir,
    val_dir,
    output_dir='crnn_output',
    img_height=32,
    img_width=None,
    batch_size=32,
    num_epochs=100,
    learning_rate=0.001,
    num_workers=4,
    device='cuda'
):
    """
    Huấn luyện CRNN model
    
    Args:
        train_dir: Thư mục train data
        val_dir: Thư mục validation data
        output_dir: Thư mục lưu model
        img_height: Chiều cao ảnh
        img_width: Chiều rộng ảnh (None = giữ aspect ratio)
        batch_size: Batch size
        num_epochs: Số epochs
        learning_rate: Learning rate
        num_workers: Số workers cho DataLoader
        device: Device ('cuda' hoặc 'cpu')
    """
    
    # Tạo output directory
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    
    # Device
    device = torch.device(device if torch.cuda.is_available() else 'cpu')
    print(f"Using device: {device}")
    
    # Create datasets
    train_dataset = TextImageDataset(
        image_dir=train_dir,
        labels_file=Path(train_dir) / 'labels.txt',
        img_height=img_height,
        img_width=img_width,
        augment=True
    )
    
    val_dataset = TextImageDataset(
        image_dir=val_dir,
        labels_file=Path(val_dir) / 'labels.txt',
        img_height=img_height,
        img_width=img_width,
        augment=False
    )
    
    # Create dataloaders
    train_loader = DataLoader(
        train_dataset,
        batch_size=batch_size,
        shuffle=True,
        num_workers=num_workers,
        collate_fn=collate_fn,
        pin_memory=True if device.type == 'cuda' else False
    )
    
    val_loader = DataLoader(
        val_dataset,
        batch_size=batch_size,
        shuffle=False,
        num_workers=num_workers,
        collate_fn=collate_fn,
        pin_memory=True if device.type == 'cuda' else False
    )
    
    # Create model
    model = create_crnn_model(
        img_height=img_height,
        num_channels=1,
        num_classes=NUM_CLASSES,
        hidden_size=256
    ).to(device)
    
    print("\n" + "="*50)
    print("MODEL INFORMATION")
    print("="*50)
    print(f"Number of classes: {NUM_CLASSES}")
    print(f"Image height: {img_height}")
    print(f"Image width: {'dynamic' if img_width is None else img_width}")
    
    # Loss and optimizer
    criterion = CTCLoss()
    optimizer = optim.Adam(model.parameters(), lr=learning_rate)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(
        optimizer, mode='min', factor=0.5, patience=5, verbose=True
    )
    
    # Training history
    history = {
        'train_loss': [],
        'val_loss': [],
        'val_accuracy': []
    }
    
    best_val_loss = float('inf')
    
    print("\n" + "="*50)
    print("START TRAINING")
    print("="*50)
    
    for epoch in range(num_epochs):
        # Training
        model.train()
        train_loss = 0.0
        
        pbar = tqdm(train_loader, desc=f"Epoch {epoch+1}/{num_epochs} [Train]")
        for batch in pbar:
            images = batch['images'].to(device)
            text_indices = batch['text_indices'].to(device)
            text_lengths = batch['text_lengths']
            
            # Forward
            optimizer.zero_grad()
            outputs = model(images)  # (batch, seq_len, num_classes)
            
            # Prepare for CTC loss
            outputs = outputs.permute(1, 0, 2)  # (seq_len, batch, num_classes)
            log_probs = F.log_softmax(outputs, dim=2)
            
            input_lengths = torch.full(
                size=(images.size(0),),
                fill_value=outputs.size(0),
                dtype=torch.long
            )
            
            # Compute loss
            loss = criterion(log_probs, text_indices, input_lengths, text_lengths)
            
            # Backward
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 5.0)
            optimizer.step()
            
            train_loss += loss.item()
            pbar.set_postfix({'loss': f'{loss.item():.4f}'})
        
        train_loss /= len(train_loader)
        history['train_loss'].append(train_loss)
        
        # Validation
        model.eval()
        val_loss = 0.0
        correct = 0
        total = 0
        
        with torch.no_grad():
            pbar = tqdm(val_loader, desc=f"Epoch {epoch+1}/{num_epochs} [Val]")
            for batch in pbar:
                images = batch['images'].to(device)
                text_indices = batch['text_indices'].to(device)
                text_lengths = batch['text_lengths']
                texts = batch['texts']
                
                # Forward
                outputs = model(images)
                
                # CTC loss
                outputs_ctc = outputs.permute(1, 0, 2)
                log_probs = F.log_softmax(outputs_ctc, dim=2)
                input_lengths = torch.full(
                    size=(images.size(0),),
                    fill_value=outputs_ctc.size(0),
                    dtype=torch.long
                )
                
                loss = criterion(log_probs, text_indices, input_lengths, text_lengths)
                val_loss += loss.item()
                
                # Decode predictions
                pred_texts = decode_predictions(log_probs)
                
                # Accuracy
                for pred, gt in zip(pred_texts, texts):
                    if pred == gt:
                        correct += 1
                    total += 1
                
                pbar.set_postfix({'loss': f'{loss.item():.4f}'})
        
        val_loss /= len(val_loader)
        val_accuracy = correct / total if total > 0 else 0
        
        history['val_loss'].append(val_loss)
        history['val_accuracy'].append(val_accuracy)
        
        # Update scheduler
        scheduler.step(val_loss)
        
        # Print epoch summary
        print(f"\nEpoch {epoch+1}/{num_epochs}")
        print(f"  Train Loss: {train_loss:.4f}")
        print(f"  Val Loss: {val_loss:.4f}")
        print(f"  Val Accuracy: {val_accuracy:.4f}")
        
        # Save best model
        if val_loss < best_val_loss:
            best_val_loss = val_loss
            torch.save(model.state_dict(), output_path / 'best_crnn.pt')
            print(f"  ✓ Saved best model (val_loss: {val_loss:.4f})")
        
        # Save checkpoint
        if (epoch + 1) % 10 == 0:
            torch.save({
                'epoch': epoch,
                'model_state_dict': model.state_dict(),
                'optimizer_state_dict': optimizer.state_dict(),
                'train_loss': train_loss,
                'val_loss': val_loss,
            }, output_path / f'checkpoint_epoch_{epoch+1}.pt')
    
    # Save final model
    torch.save(model.state_dict(), output_path / 'final_crnn.pt')
    
    # Save history
    with open(output_path / 'history.json', 'w') as f:
        json.dump(history, f, indent=2)
    
    print("\n" + "="*50)
    print("TRAINING COMPLETED")
    print("="*50)
    print(f"Best validation loss: {best_val_loss:.4f}")
    print(f"Models saved in: {output_path}")

if __name__ == "__main__":
    # Cấu hình cho local machine
    TRAIN_DIR = "d:/ORC_Service/CRNN_DATASET/train"
    VAL_DIR = "d:/ORC_Service/CRNN_DATASET/val"
    OUTPUT_DIR = "d:/ORC_Service/crnn_models"
    
    IMG_HEIGHT = 32
    IMG_WIDTH = None  # Dynamic width
    BATCH_SIZE = 16  # Giảm batch size cho GPU 2GB
    NUM_EPOCHS = 100
    LEARNING_RATE = 0.001
    
    # Train
    train_crnn(
        train_dir=TRAIN_DIR,
        val_dir=VAL_DIR,
        output_dir=OUTPUT_DIR,
        img_height=IMG_HEIGHT,
        img_width=IMG_WIDTH,
        batch_size=BATCH_SIZE,
        num_epochs=NUM_EPOCHS,
        learning_rate=LEARNING_RATE,
        num_workers=0,  # Disable workers để tránh crash trên Windows
        device='cuda'
    )
