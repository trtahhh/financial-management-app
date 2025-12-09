"""
Mô hình CRNN (Convolutional Recurrent Neural Network) để nhận dạng văn bản
Sử dụng CNN để trích xuất đặc trưng và RNN (LSTM) để xử lý chuỗi
"""

import torch
import torch.nn as nn
from torch.nn import functional as F

class BidirectionalLSTM(nn.Module):
    """LSTM hai chiều"""
    
    def __init__(self, input_size, hidden_size, output_size):
        super(BidirectionalLSTM, self).__init__()
        self.rnn = nn.LSTM(input_size, hidden_size, bidirectional=True, batch_first=True)
        self.linear = nn.Linear(hidden_size * 2, output_size)
    
    def forward(self, input):
        """
        Args:
            input: (batch, seq_len, input_size)
        Returns:
            output: (batch, seq_len, output_size)
        """
        recurrent, _ = self.rnn(input)
        output = self.linear(recurrent)
        return output

class CRNN(nn.Module):
    """
    CRNN model cho OCR
    Architecture:
        CNN: Trích xuất đặc trưng từ ảnh
        RNN: Xử lý chuỗi đặc trưng
        CTC: Decoding không cần alignment
    """
    
    def __init__(self, img_height, num_channels, num_classes, hidden_size=256):
        """
        Args:
            img_height: Chiều cao ảnh input (width có thể thay đổi)
            num_channels: Số channel ảnh (1 cho grayscale, 3 cho RGB)
            num_classes: Số lượng ký tự (bao gồm blank token cho CTC)
            hidden_size: Kích thước hidden layer của LSTM
        """
        super(CRNN, self).__init__()
        
        self.img_height = img_height
        self.num_channels = num_channels
        self.num_classes = num_classes
        self.hidden_size = hidden_size
        
        # CNN layers để trích xuất đặc trưng
        # Input: (batch, channels, height, width)
        self.cnn = nn.Sequential(
            # Conv1: (batch, 64, height, width)
            nn.Conv2d(num_channels, 64, kernel_size=3, stride=1, padding=1),
            nn.ReLU(True),
            nn.MaxPool2d(kernel_size=2, stride=2),  # height/2, width/2
            
            # Conv2: (batch, 128, height/2, width/2)
            nn.Conv2d(64, 128, kernel_size=3, stride=1, padding=1),
            nn.ReLU(True),
            nn.MaxPool2d(kernel_size=2, stride=2),  # height/4, width/4
            
            # Conv3: (batch, 256, height/4, width/4)
            nn.Conv2d(128, 256, kernel_size=3, stride=1, padding=1),
            nn.BatchNorm2d(256),
            nn.ReLU(True),
            
            # Conv4: (batch, 256, height/4, width/4)
            nn.Conv2d(256, 256, kernel_size=3, stride=1, padding=1),
            nn.ReLU(True),
            nn.MaxPool2d(kernel_size=(2, 1)),  # height/8, width/4
            
            # Conv5: (batch, 512, height/8, width/4)
            nn.Conv2d(256, 512, kernel_size=3, stride=1, padding=1),
            nn.BatchNorm2d(512),
            nn.ReLU(True),
            
            # Conv6: (batch, 512, height/8, width/4)
            nn.Conv2d(512, 512, kernel_size=3, stride=1, padding=1),
            nn.ReLU(True),
            nn.MaxPool2d(kernel_size=(2, 1)),  # height/16, width/4
            
            # Conv7: (batch, 512, height/16, width/4)
            nn.Conv2d(512, 512, kernel_size=2, stride=1, padding=0),
            nn.BatchNorm2d(512),
            nn.ReLU(True)
        )
        
        # RNN layers
        # Sau CNN, feature map có shape (batch, 512, height', width')
        # height' phụ thuộc vào img_height ban đầu
        # Ví dụ: img_height=32 -> height' sẽ là 1 sau các pooling
        self.rnn = nn.Sequential(
            BidirectionalLSTM(512, hidden_size, hidden_size),
            BidirectionalLSTM(hidden_size, hidden_size, num_classes)
        )
    
    def forward(self, input):
        """
        Args:
            input: (batch, channels, height, width)
        Returns:
            output: (batch, seq_len, num_classes) - log probabilities
        """
        # CNN feature extraction
        conv = self.cnn(input)  # (batch, 512, height', width')
        
        # Reshape cho RNN
        # (batch, channels, height, width) -> (batch, width, channels * height)
        batch, channels, height, width = conv.size()
        
        # Collapse height
        conv = conv.squeeze(2)  # Giả sử height' = 1: (batch, 512, width)
        
        # Permute để có sequence dimension
        conv = conv.permute(0, 2, 1)  # (batch, width, 512)
        
        # RNN
        output = self.rnn(conv)  # (batch, width, num_classes)
        
        return output
    
    def get_feature_size(self, img_height):
        """Tính kích thước feature sau CNN"""
        # Sau các pooling layers
        h = img_height
        h = h // 2  # MaxPool 2x2
        h = h // 2  # MaxPool 2x2
        h = h // 2  # MaxPool (2,1)
        h = h // 2  # MaxPool (2,1)
        h = h - 1   # Conv 2x2 kernel, no padding
        return h

class CTCLoss(nn.Module):
    """
    CTC Loss wrapper
    Connectionist Temporal Classification loss
    """
    
    def __init__(self):
        super(CTCLoss, self).__init__()
        self.loss_fn = nn.CTCLoss(blank=0, reduction='mean', zero_infinity=True)
    
    def forward(self, log_probs, targets, input_lengths, target_lengths):
        """
        Args:
            log_probs: (seq_len, batch, num_classes) - Log probabilities từ model
            targets: (sum(target_lengths),) - Ground truth labels (concatenated)
            input_lengths: (batch,) - Độ dài sequence của mỗi sample
            target_lengths: (batch,) - Độ dài target của mỗi sample
        Returns:
            loss: scalar
        """
        return self.loss_fn(log_probs, targets, input_lengths, target_lengths)

def create_crnn_model(img_height=32, num_channels=1, num_classes=None, hidden_size=256):
    """
    Tạo CRNN model
    
    Args:
        img_height: Chiều cao ảnh (chiều rộng có thể thay đổi)
        num_channels: Số channels (1 = grayscale, 3 = RGB)
        num_classes: Số lượng classes (characters + blank)
        hidden_size: Hidden size cho LSTM
    
    Returns:
        model: CRNN model
    """
    if num_classes is None:
        # Default: 95 ASCII printable characters + blank
        num_classes = 96
    
    model = CRNN(
        img_height=img_height,
        num_channels=num_channels,
        num_classes=num_classes,
        hidden_size=hidden_size
    )
    
    return model

def get_model_size(model):
    """Tính số lượng parameters trong model"""
    param_size = 0
    param_sum = 0
    for param in model.parameters():
        param_size += param.nelement() * param.element_size()
        param_sum += param.nelement()
    buffer_size = 0
    buffer_sum = 0
    for buffer in model.buffers():
        buffer_size += buffer.nelement() * buffer.element_size()
        buffer_sum += buffer.nelement()
    all_size = (param_size + buffer_size) / 1024 / 1024
    return {
        'params': param_sum,
        'size_mb': all_size
    }

if __name__ == "__main__":
    # Test model
    img_height = 32
    img_width = 100
    batch_size = 4
    num_channels = 1
    num_classes = 96  # 95 printable ASCII + 1 blank
    
    # Tạo model
    model = create_crnn_model(
        img_height=img_height,
        num_channels=num_channels,
        num_classes=num_classes,
        hidden_size=256
    )
    
    # Print model info
    print("="*50)
    print("CRNN MODEL INFORMATION")
    print("="*50)
    print(model)
    print("\n" + "="*50)
    
    model_info = get_model_size(model)
    print(f"Number of parameters: {model_info['params']:,}")
    print(f"Model size: {model_info['size_mb']:.2f} MB")
    print("="*50)
    
    # Test forward pass
    dummy_input = torch.randn(batch_size, num_channels, img_height, img_width)
    
    model.eval()
    with torch.no_grad():
        output = model(dummy_input)
    
    print(f"\nInput shape: {dummy_input.shape}")
    print(f"Output shape: {output.shape}")
    print(f"Expected: (batch={batch_size}, seq_len, num_classes={num_classes})")
    
    print("\n✓ Model test successful!")
