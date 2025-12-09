"""
Inference với Tesseract OCR thay vì CRNN
Cải thiện độ chính xác cho text recognition
"""

import torch
from ultralytics import YOLO
import cv2
import numpy as np
from pathlib import Path
import json
from typing import Dict, List
try:
    import pytesseract
    # Set Tesseract path for Windows
    pytesseract.pytesseract.tesseract_cmd = r'C:\Program Files\Tesseract-OCR\tesseract.exe'
    TESSERACT_AVAILABLE = True
except ImportError:
    TESSERACT_AVAILABLE = False
    print("Warning: pytesseract not installed. Install with: pip install pytesseract")

# Class mapping
CLASS_NAMES = {
    0: 'company',
    1: 'date',
    2: 'total',
    3: 'address'
}

class InvoiceOCR_Tesseract:
    """
    Hệ thống OCR với YOLOv8 + Tesseract
    """
    
    def __init__(
        self,
        yolo_model_path: str,
        device: str = 'cuda',
        yolo_conf: float = 0.15,
        yolo_iou: float = 0.45,
        tesseract_config: str = '--psm 6'
    ):
        """
        Args:
            yolo_model_path: Đường dẫn đến YOLO model
            device: Device ('cuda' hoặc 'cpu')
            yolo_conf: Confidence threshold cho YOLO
            yolo_iou: IoU threshold cho YOLO NMS
            tesseract_config: Config cho Tesseract
        """
        self.device = torch.device(device if torch.cuda.is_available() else 'cpu')
        print(f"Using device: {self.device}")
        
        if not TESSERACT_AVAILABLE:
            raise ImportError("pytesseract not installed. Run: pip install pytesseract")
        
        # Load YOLO model
        print(f"Loading YOLO model from {yolo_model_path}")
        self.yolo_model = YOLO(yolo_model_path)
        self.yolo_conf = yolo_conf
        self.yolo_iou = yolo_iou
        
        self.tesseract_config = tesseract_config
        
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
    
    def preprocess_for_ocr(self, image: np.ndarray, field_type: str = None) -> np.ndarray:
        """
        Tiền xử lý ảnh cho OCR
        
        Args:
            image: Ảnh crop
            field_type: Loại field (total, date, etc.) để xử lý đặc biệt
        """
        # Convert to grayscale
        if len(image.shape) == 3:
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        else:
            gray = image
        
        # Resize to better resolution
        scale = 2.0
        width = int(gray.shape[1] * scale)
        height = int(gray.shape[0] * scale)
        gray = cv2.resize(gray, (width, height), interpolation=cv2.INTER_CUBIC)
        
        # Denoise
        gray = cv2.fastNlMeansDenoising(gray, None, 10, 7, 21)
        
        # Thresholding
        if field_type == 'total':
            # Cho số tiền, dùng adaptive threshold
            gray = cv2.adaptiveThreshold(
                gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
                cv2.THRESH_BINARY, 11, 2
            )
        else:
            # Binary threshold cho text thường
            _, gray = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        
        return gray
    
    def recognize_text(self, cropped_image: np.ndarray, field_type: str = None) -> str:
        """
        Nhận dạng văn bản bằng Tesseract
        
        Args:
            cropped_image: Ảnh đã crop
            field_type: Loại field để config phù hợp
        """
        # Preprocess
        processed = self.preprocess_for_ocr(cropped_image, field_type)
        
        # Config theo loại field
        if field_type == 'total':
            # Chỉ nhận dạng số và dấu chấm/phẩy
            config = self.tesseract_config + ' -c tessedit_char_whitelist=0123456789.,'
        elif field_type == 'date':
            # Số và dấu gạch chéo/gạch ngang
            config = self.tesseract_config + ' -c tessedit_char_whitelist=0123456789/-'
        else:
            config = self.tesseract_config
        
        # OCR
        try:
            text = pytesseract.image_to_string(processed, config=config)
            text = text.strip()
            
            # Post-processing cho số tiền
            if field_type == 'total':
                # Loại bỏ ký tự không phải số
                text = ''.join(c for c in text if c.isdigit() or c in '.,')
                # Chuẩn hóa dấu phân cách
                text = text.replace(',', '.')
            
            return text
        except Exception as e:
            print(f"OCR error: {e}")
            return ""
    
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
    ocr_system: InvoiceOCR_Tesseract,
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

if __name__ == "__main__":
    # Cấu hình
    YOLO_MODEL = "d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt"
    
    # Test single image
    TEST_IMAGE = "d:/ORC_Service/SROIE2019/test/img/X00016469670.jpg"
    OUTPUT_VIS = "d:/ORC_Service/result_tesseract.jpg"
    
    # Initialize OCR system
    ocr = InvoiceOCR_Tesseract(
        yolo_model_path=YOLO_MODEL,
        device='cuda',
        yolo_conf=0.15,
        yolo_iou=0.45
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
        output_json="d:/ORC_Service/batch_results_tesseract.json",
        output_vis_dir="d:/ORC_Service/visualizations_tesseract"
    )
    
    print("\n✓ All done!")
