"""
Inference với EasyOCR - Không cần cài Tesseract
EasyOCR sử dụng deep learning, độ chính xác cao cho text nhận dạng
"""

import torch
from ultralytics import YOLO
import cv2
import numpy as np
from pathlib import Path
import json
from typing import Dict, List
import re

try:
    import easyocr
    EASYOCR_AVAILABLE = True
except ImportError:
    EASYOCR_AVAILABLE = False
    print("Warning: easyocr not installed. Install with: pip install easyocr")

# Class mapping
CLASS_NAMES = {
    0: 'company',
    1: 'date',
    2: 'total',
    3: 'address'
}

class InvoiceOCR_EasyOCR:
    """
    Hệ thống OCR với YOLOv8 + EasyOCR
    """
    
    def __init__(
        self,
        yolo_model_path: str,
        device: str = 'cuda',
        yolo_conf: float = 0.15,
        yolo_iou: float = 0.45,
        languages: List[str] = ['en']
    ):
        """
        Args:
            yolo_model_path: Đường dẫn đến YOLO model
            device: Device ('cuda' hoặc 'cpu')
            yolo_conf: Confidence threshold cho YOLO
            yolo_iou: IoU threshold cho YOLO NMS
            languages: Ngôn ngữ cho EasyOCR (default: English)
        """
        self.device = torch.device(device if torch.cuda.is_available() else 'cpu')
        print(f"Using device: {self.device}")
        
        if not EASYOCR_AVAILABLE:
            raise ImportError("easyocr not installed. Run: pip install easyocr")
        
        # Load YOLO model
        print(f"Loading YOLO model from {yolo_model_path}")
        self.yolo_model = YOLO(yolo_model_path)
        self.yolo_conf = yolo_conf
        self.yolo_iou = yolo_iou
        
        # Initialize EasyOCR
        print(f"Initializing EasyOCR with languages: {languages}")
        use_gpu = device == 'cuda' and torch.cuda.is_available()
        self.reader = easyocr.Reader(languages, gpu=use_gpu)
        
        print("✓ Models loaded successfully!")
    
    def detect_regions(self, image: np.ndarray) -> List[Dict]:
        """Phát hiện các vùng thông tin"""
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
    
    def recognize_text(self, cropped_image: np.ndarray, field_type: str = None) -> str:
        """
        Nhận dạng văn bản bằng EasyOCR
        
        Args:
            cropped_image: Ảnh đã crop
            field_type: Loại field để post-process
        """
        try:
            # EasyOCR nhận ảnh RGB
            if len(cropped_image.shape) == 3 and cropped_image.shape[2] == 3:
                # BGR to RGB
                rgb_image = cv2.cvtColor(cropped_image, cv2.COLOR_BGR2RGB)
            else:
                rgb_image = cropped_image
            
            # OCR
            results = self.reader.readtext(rgb_image, detail=0, paragraph=False)
            
            # Ghép text
            text = ' '.join(results)
            text = text.strip()
            
            # Post-processing theo loại field
            if field_type == 'total':
                # Trích xuất số tiền
                text = self.extract_amount(text)
            elif field_type == 'date':
                # Trích xuất ngày tháng
                text = self.extract_date(text)
            
            return text
            
        except Exception as e:
            print(f"OCR error: {e}")
            return ""
    
    def extract_amount(self, text: str) -> str:
        """Trích xuất số tiền từ text"""
        # Tìm pattern số tiền: 123.45 hoặc 123,45 hoặc 1,234.56
        patterns = [
            r'\d+\.\d{2}',  # 123.45
            r'\d+,\d{2}',   # 123,45
            r'\d{1,3}(?:,\d{3})*(?:\.\d{2})?',  # 1,234.56
            r'\d+\.?\d*'    # 123 hoặc 123.4
        ]
        
        for pattern in patterns:
            matches = re.findall(pattern, text)
            if matches:
                # Lấy số lớn nhất (thường là total)
                amounts = []
                for m in matches:
                    try:
                        # Chuẩn hóa: loại bỏ dấu phẩy
                        amount = float(m.replace(',', ''))
                        amounts.append((amount, m))
                    except:
                        pass
                
                if amounts:
                    # Trả về số lớn nhất
                    return max(amounts, key=lambda x: x[0])[1]
        
        return text
    
    def extract_date(self, text: str) -> str:
        """Trích xuất ngày tháng từ text"""
        # Tìm pattern ngày: dd/mm/yyyy, dd-mm-yyyy, etc.
        patterns = [
            r'\d{1,2}[/-]\d{1,2}[/-]\d{2,4}',  # 25/12/2023
            r'\d{4}[/-]\d{1,2}[/-]\d{1,2}',     # 2023-12-25
        ]
        
        for pattern in patterns:
            match = re.search(pattern, text)
            if match:
                return match.group()
        
        return text
    
    def process_invoice(
        self,
        image_path: str,
        save_visualization: bool = False,
        output_path: str = None
    ) -> Dict:
        """Xử lý toàn bộ hóa đơn"""
        # Load image
        image = cv2.imread(image_path)
        if image is None:
            raise ValueError(f"Cannot read image: {image_path}")
        
        # Detect regions
        detections = self.detect_regions(image)
        
        # Recognize text
        results = {
            'company': '',
            'date': '',
            'total': '',
            'address': '',
            'detections': []
        }
        
        # Group detections by class and select best one
        class_detections = {}
        for detection in detections:
            class_name = detection['class_name']
            conf = detection['confidence']
            
            if class_name not in class_detections or conf > class_detections[class_name]['confidence']:
                class_detections[class_name] = detection
        
        # OCR for each class
        for class_name, detection in class_detections.items():
            cropped_image = detection['cropped_image']
            
            # Recognize text
            text = self.recognize_text(cropped_image, class_name)
            
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
                
                # Draw box
                cv2.rectangle(vis_image, (x1, y1), (x2, y2), (0, 255, 0), 2)
                
                # Draw label
                label = f"{det['class_name']}: {det['text']}"
                label_size, _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.5, 2)
                
                cv2.rectangle(
                    vis_image,
                    (x1, y1 - label_size[1] - 10),
                    (x1 + label_size[0], y1),
                    (0, 255, 0),
                    -1
                )
                
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
    ocr_system: InvoiceOCR_EasyOCR,
    image_dir: str,
    output_json: str,
    output_vis_dir: str = None
):
    """Xử lý batch nhiều hóa đơn"""
    from tqdm import tqdm
    
    image_path = Path(image_dir)
    image_files = list(image_path.glob('*.jpg')) + list(image_path.glob('*.png'))
    
    if output_vis_dir:
        Path(output_vis_dir).mkdir(parents=True, exist_ok=True)
    
    all_results = {}
    
    print(f"Processing {len(image_files)} images...")
    
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
    
    # Print statistics
    successful = sum(1 for r in all_results.values() if 'error' not in r)
    total_extracted = sum(1 for r in all_results.values() if r.get('total', ''))
    
    print(f"✓ Processed: {successful}/{len(image_files)}")
    print(f"✓ Total extracted: {total_extracted}/{successful}")

if __name__ == "__main__":
    # Cấu hình
    # Model improved: Epoch 94 (mAP50: 72.77%, +2.77% from baseline)
    YOLO_MODEL = "d:/ORC_Service/runs/detect/sroie_invoice_continued/weights/best.pt"
    
    # Test single image
    TEST_IMAGE = "d:/ORC_Service/SROIE2019/test/img/X00016469670.jpg"
    OUTPUT_VIS = "d:/ORC_Service/result_easyocr.jpg"
    
    print("Installing/Checking EasyOCR...")
    
    # Initialize OCR system
    ocr = InvoiceOCR_EasyOCR(
        yolo_model_path=YOLO_MODEL,
        device='cuda',
        yolo_conf=0.25,
        yolo_iou=0.45,
        languages=['en']
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
        output_json="d:/ORC_Service/batch_results_easyocr.json",
        output_vis_dir="d:/ORC_Service/visualizations_easyocr"
    )
    
    print("\n✓ All done!")
