"""
Script inference cho hệ thống OCR hoàn chỉnh
Pipeline: Phát hiện vùng (YOLOv8) -> Nhận dạng văn bản (CRNN) -> Kết quả JSON
"""

import torch
import torch.nn.functional as F
from ultralytics import YOLO
import cv2
import numpy as np
from pathlib import Path
import json
import string
from typing import Dict, List, Tuple

from crnn_model import create_crnn_model

# Character set
CHARS = string.digits + string.ascii_letters + string.punctuation + ' '
CHAR_TO_IDX = {char: idx + 1 for idx, char in enumerate(CHARS)}
IDX_TO_CHAR = {idx + 1: char for idx, char in enumerate(CHARS)}
IDX_TO_CHAR[0] = ''
NUM_CLASSES = len(CHARS) + 1

# Class mapping
CLASS_NAMES = {
    0: 'company',
    1: 'date',
    2: 'total',
    3: 'address'
}

class InvoiceOCR:
    """
    Hệ thống OCR hoàn chỉnh cho hóa đơn
    """
    
    def __init__(
        self,
        yolo_model_path: str,
        crnn_model_path: str,
        device: str = 'cuda',
        yolo_conf: float = 0.25,
        yolo_iou: float = 0.45,
        crnn_img_height: int = 32
    ):
        """
        Args:
            yolo_model_path: Đường dẫn đến YOLO model (.pt hoặc .onnx)
            crnn_model_path: Đường dẫn đến CRNN model (.pt)
            device: Device ('cuda' hoặc 'cpu')
            yolo_conf: Confidence threshold cho YOLO
            yolo_iou: IoU threshold cho YOLO NMS
            crnn_img_height: Chiều cao ảnh cho CRNN
        """
        self.device = torch.device(device if torch.cuda.is_available() else 'cpu')
        print(f"Using device: {self.device}")
        
        # Load YOLO model
        print(f"Loading YOLO model from {yolo_model_path}")
        self.yolo_model = YOLO(yolo_model_path)
        self.yolo_conf = yolo_conf
        self.yolo_iou = yolo_iou
        
        # Load CRNN model
        print(f"Loading CRNN model from {crnn_model_path}")
        self.crnn_model = create_crnn_model(
            img_height=crnn_img_height,
            num_channels=1,
            num_classes=NUM_CLASSES,
            hidden_size=256
        ).to(self.device)
        
        # Load weights
        state_dict = torch.load(crnn_model_path, map_location=self.device)
        self.crnn_model.load_state_dict(state_dict)
        self.crnn_model.eval()
        
        self.crnn_img_height = crnn_img_height
        
        print("✓ Models loaded successfully!")
    
    def detect_regions(self, image: np.ndarray) -> List[Dict]:
        """
        Phát hiện các vùng thông tin trên hóa đơn
        
        Args:
            image: Ảnh đầu vào (numpy array, BGR)
        
        Returns:
            List of detected regions with format:
            [
                {
                    'class_name': str,
                    'class_id': int,
                    'confidence': float,
                    'bbox': [x1, y1, x2, y2],
                    'cropped_image': np.ndarray
                },
                ...
            ]
        """
        # Predict với YOLO
        results = self.yolo_model.predict(
            source=image,
            conf=self.yolo_conf,
            iou=self.yolo_iou,
            verbose=False
        )
        
        detections = []
        
        if len(results) > 0 and results[0].boxes is not None:
            boxes = results[0].boxes
            
            for box, conf, cls in zip(
                boxes.xyxy.cpu().numpy(),
                boxes.conf.cpu().numpy(),
                boxes.cls.cpu().numpy()
            ):
                x1, y1, x2, y2 = map(int, box)
                class_id = int(cls)
                class_name = CLASS_NAMES.get(class_id, f'class_{class_id}')
                
                # Crop region
                cropped = image[y1:y2, x1:x2]
                
                if cropped.size > 0:
                    detections.append({
                        'class_name': class_name,
                        'class_id': class_id,
                        'confidence': float(conf),
                        'bbox': [x1, y1, x2, y2],
                        'cropped_image': cropped
                    })
        
        return detections
    
    def preprocess_for_crnn(self, image: np.ndarray) -> torch.Tensor:
        """
        Tiền xử lý ảnh cho CRNN
        
        Args:
            image: Ảnh đã crop (BGR)
        
        Returns:
            Tensor (1, 1, H, W)
        """
        # Convert to grayscale
        if len(image.shape) == 3:
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        else:
            gray = image
        
        # Resize với aspect ratio
        h, w = gray.shape
        new_w = int(w * self.crnn_img_height / h)
        new_w = max(new_w, 1)
        
        resized = cv2.resize(gray, (new_w, self.crnn_img_height))
        
        # Normalize
        normalized = resized.astype(np.float32) / 255.0
        normalized = (normalized - 0.5) / 0.5
        
        # To tensor
        tensor = torch.FloatTensor(normalized).unsqueeze(0).unsqueeze(0)  # (1, 1, H, W)
        
        return tensor
    
    def decode_crnn_output(self, output: torch.Tensor, blank: int = 0) -> str:
        """
        Decode CRNN output
        
        Args:
            output: (seq_len, num_classes) - Log probabilities
            blank: Blank token index
        
        Returns:
            Decoded text string
        """
        # Get best path
        _, max_indices = output.max(1)  # (seq_len,)
        
        # Remove consecutive duplicates and blank
        chars = []
        prev_idx = None
        for idx in max_indices:
            idx = idx.item()
            if idx != blank and idx != prev_idx:
                if idx in IDX_TO_CHAR:
                    chars.append(IDX_TO_CHAR[idx])
            prev_idx = idx
        
        return ''.join(chars)
    
    def recognize_text(self, cropped_image: np.ndarray) -> str:
        """
        Nhận dạng văn bản từ ảnh đã crop
        
        Args:
            cropped_image: Ảnh đã crop
        
        Returns:
            Recognized text
        """
        # Preprocess
        input_tensor = self.preprocess_for_crnn(cropped_image).to(self.device)
        
        # Inference
        with torch.no_grad():
            output = self.crnn_model(input_tensor)  # (1, seq_len, num_classes)
            output = output.squeeze(0)  # (seq_len, num_classes)
            log_probs = F.log_softmax(output, dim=1)
        
        # Decode
        text = self.decode_crnn_output(log_probs)
        
        return text
    
    def process_invoice(
        self,
        image_path: str,
        save_visualization: bool = False,
        output_path: str = None
    ) -> Dict:
        """
        Xử lý toàn bộ hóa đơn
        
        Args:
            image_path: Đường dẫn đến ảnh
            save_visualization: Có lưu ảnh visualization không
            output_path: Đường dẫn lưu visualization
        
        Returns:
            Dictionary chứa kết quả:
            {
                'company': str,
                'date': str,
                'total': str,
                'address': str,
                'detections': List[Dict]
            }
        """
        # Load image
        image = cv2.imread(image_path)
        if image is None:
            raise ValueError(f"Cannot read image: {image_path}")
        
        # Step 1: Detect regions
        detections = self.detect_regions(image)
        
        # Step 2: Recognize text for each region
        results = {
            'company': '',
            'date': '',
            'total': '',
            'address': '',
            'detections': []
        }
        
        for detection in detections:
            class_name = detection['class_name']
            cropped_image = detection['cropped_image']
            
            # Recognize text
            text = self.recognize_text(cropped_image)
            
            # Update results
            if class_name in results:
                results[class_name] = text
            
            # Add to detections list
            results['detections'].append({
                'class_name': class_name,
                'text': text,
                'confidence': detection['confidence'],
                'bbox': detection['bbox']
            })
        
        # Visualization
        if save_visualization and output_path:
            vis_image = image.copy()
            
            for det in results['detections']:
                x1, y1, x2, y2 = det['bbox']
                
                # Draw bounding box
                cv2.rectangle(vis_image, (x1, y1), (x2, y2), (0, 255, 0), 2)
                
                # Draw label
                label = f"{det['class_name']}: {det['text']}"
                label_size, _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.5, 2)
                
                # Background for text
                cv2.rectangle(
                    vis_image,
                    (x1, y1 - label_size[1] - 10),
                    (x1 + label_size[0], y1),
                    (0, 255, 0),
                    -1
                )
                
                # Text
                cv2.putText(
                    vis_image,
                    label,
                    (x1, y1 - 5),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.5,
                    (0, 0, 0),
                    2
                )
            
            cv2.imwrite(output_path, vis_image)
        
        return results

def batch_process_invoices(
    ocr_system: InvoiceOCR,
    image_dir: str,
    output_json: str,
    output_vis_dir: str = None
):
    """
    Xử lý batch nhiều hóa đơn
    
    Args:
        ocr_system: InvoiceOCR instance
        image_dir: Thư mục chứa ảnh
        output_json: File JSON lưu kết quả
        output_vis_dir: Thư mục lưu visualization
    """
    image_path = Path(image_dir)
    image_files = list(image_path.glob('*.jpg')) + list(image_path.glob('*.png'))
    
    if output_vis_dir:
        Path(output_vis_dir).mkdir(parents=True, exist_ok=True)
    
    all_results = {}
    
    print(f"Processing {len(image_files)} images...")
    
    from tqdm import tqdm
    for img_file in tqdm(image_files):
        img_name = img_file.stem
        
        try:
            vis_path = None
            if output_vis_dir:
                vis_path = str(Path(output_vis_dir) / f"{img_name}_result.jpg")
            
            result = ocr_system.process_invoice(
                image_path=str(img_file),
                save_visualization=output_vis_dir is not None,
                output_path=vis_path
            )
            
            all_results[img_name] = result
            
        except Exception as e:
            print(f"Error processing {img_file}: {e}")
            all_results[img_name] = {'error': str(e)}
    
    # Save results
    with open(output_json, 'w', encoding='utf-8') as f:
        json.dump(all_results, f, indent=2, ensure_ascii=False)
    
    print(f"\n✓ Results saved to {output_json}")

if __name__ == "__main__":
    # Cấu hình
    YOLO_MODEL = "d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt"
    CRNN_MODEL = "d:/ORC_Service/crnn_models/best_crnn.pt"
    
    # Test single image
    TEST_IMAGE = "d:/ORC_Service/SROIE2019/test/img/X00016469670.jpg"
    OUTPUT_VIS = "d:/ORC_Service/result_visualization.jpg"
    
    # Initialize OCR system
    ocr = InvoiceOCR(
        yolo_model_path=YOLO_MODEL,
        crnn_model_path=CRNN_MODEL,
        device='cuda',
        yolo_conf=0.25,
        yolo_iou=0.45,
        crnn_img_height=32
    )
    
    print("\n" + "="*50)
    print("PROCESSING SINGLE IMAGE")
    print("="*50)
    
    # Process single image
    result = ocr.process_invoice(
        image_path=TEST_IMAGE,
        save_visualization=True,
        output_path=OUTPUT_VIS
    )
    
    # Print results
    print("\nResults:")
    print(json.dumps(result, indent=2, ensure_ascii=False))
    
    # Batch processing
    print("\n" + "="*50)
    print("BATCH PROCESSING")
    print("="*50)
    
    batch_process_invoices(
        ocr_system=ocr,
        image_dir="d:/ORC_Service/SROIE2019/test/img",
        output_json="d:/ORC_Service/batch_results.json",
        output_vis_dir="d:/ORC_Service/visualizations"
    )
    
    print("\n✓ All done!")
