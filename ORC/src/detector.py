"""
Module phát hiện vùng quan trọng trên hóa đơn sử dụng YOLOv8
Phát hiện các vùng: store_name, date, address, items_table, total_amount
"""

import cv2
import numpy as np
from typing import List, Dict, Tuple, Optional
import logging
from pathlib import Path

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class RegionDetector:
    """Lớp phát hiện vùng quan trọng trên hóa đơn"""
    
    # Mapping category_id từ dataset
    CATEGORY_MAPPING = {
        15: 'store_name',    # SELLER
        16: 'address',       # ADDRESS
        17: 'date',          # TIMESTAMP
        18: 'total_amount'   # TOTAL_COST
    }
    
    def __init__(self, model_path: Optional[str] = None, confidence_threshold: float = 0.25):
        """
        Khởi tạo detector
        
        Args:
            model_path: Đường dẫn đến model YOLOv8 (nếu None sẽ dùng annotation có sẵn)
            confidence_threshold: Ngưỡng confidence tối thiểu
        """
        self.model_path = model_path
        self.confidence_threshold = confidence_threshold
        self.model = None
        
        if model_path and Path(model_path).exists():
            self._load_model()
    
    def _load_model(self):
        """Load YOLO model"""
        try:
            from ultralytics import YOLO
            self.model = YOLO(self.model_path)
            logger.info(f"Đã load model YOLO từ {self.model_path}")
        except ImportError:
            logger.warning("Chưa cài đặt ultralytics. Sẽ sử dụng annotation có sẵn.")
            self.model = None
        except Exception as e:
            logger.error(f"Lỗi khi load model: {e}")
            self.model = None
    
    def detect_with_yolo(self, image: np.ndarray) -> List[Dict]:
        """
        Phát hiện vùng sử dụng YOLO model
        
        Args:
            image: Ảnh đầu vào
            
        Returns:
            List[Dict]: Danh sách các vùng phát hiện được
        """
        if self.model is None:
            logger.warning("Model chưa được load!")
            return []
        
        results = self.model(image, conf=self.confidence_threshold)
        
        detections = []
        for result in results:
            boxes = result.boxes
            for box in boxes:
                x1, y1, x2, y2 = box.xyxy[0].cpu().numpy()
                conf = float(box.conf[0])
                cls = int(box.cls[0])
                
                # Convert class id to label
                label = self.CATEGORY_MAPPING.get(cls, f'unknown_{cls}')
                
                detections.append({
                    'label': label,
                    'category_id': cls,  # Add category_id for extraction
                    'bbox': [int(x1), int(y1), int(x2-x1), int(y2-y1)],
                    'confidence': conf,
                    'polygon': [[int(x1), int(y1)], [int(x2), int(y1)], 
                               [int(x2), int(y2)], [int(x1), int(y2)]]
                })
        
        return detections
    
    def load_annotations_from_file(self, annotation_file: str) -> List[Dict]:
        """
        Load annotations từ file txt (format của dataset)
        
        Args:
            annotation_file: Đường dẫn file annotation
            
        Returns:
            List[Dict]: Danh sách các annotation
        """
        annotations = []
        
        try:
            with open(annotation_file, 'r', encoding='utf-8') as f:
                lines = f.readlines()
            
            for line in lines:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                
                # Parse coordinates
                coords_str = line.rstrip(',')
                coords = [int(float(x)) for x in coords_str.split(',')]
                
                if len(coords) < 8:  # Ít nhất 4 điểm
                    continue
                
                # Tạo polygon
                polygon = [(coords[i], coords[i+1]) for i in range(0, len(coords), 2)]
                
                # Tính bounding box
                xs = [p[0] for p in polygon]
                ys = [p[1] for p in polygon]
                x_min, x_max = min(xs), max(xs)
                y_min, y_max = min(ys), max(ys)
                bbox = [x_min, y_min, x_max - x_min, y_max - y_min]
                
                annotations.append({
                    'polygon': polygon,
                    'bbox': bbox,
                    'label': 'text_region'  # Generic label
                })
        
        except Exception as e:
            logger.error(f"Lỗi khi đọc annotation file {annotation_file}: {e}")
        
        return annotations
    
    def load_annotations_from_csv(self, csv_file: str, image_id: str) -> List[Dict]:
        """
        Load annotations từ CSV file
        
        Args:
            csv_file: Đường dẫn file CSV
            image_id: ID của ảnh cần lấy annotation
            
        Returns:
            List[Dict]: Danh sách các annotation
        """
        import pandas as pd
        import json
        
        try:
            df = pd.read_csv(csv_file)
            
            # Lọc row theo image_id
            row = df[df['img_id'] == image_id]
            
            if row.empty:
                logger.warning(f"Không tìm thấy annotation cho {image_id}")
                return []
            
            # Parse anno_polygons
            anno_polygons_str = row['anno_polygons'].values[0]
            anno_labels_str = row['anno_labels'].values[0]
            
            # Parse JSON
            polygons = json.loads(anno_polygons_str.replace("'", '"'))
            labels = anno_labels_str.split('|||')
            
            annotations = []
            for i, poly_data in enumerate(polygons):
                category_id = poly_data['category_id']
                segmentation = poly_data['segmentation'][0]  # Lấy segmentation đầu tiên
                bbox = poly_data['bbox']
                
                # Convert segmentation to polygon
                polygon = [(segmentation[j], segmentation[j+1]) 
                          for j in range(0, len(segmentation), 2)]
                
                label = self.CATEGORY_MAPPING.get(category_id, 'unknown')
                
                annotations.append({
                    'label': label,
                    'category_id': category_id,
                    'polygon': polygon,
                    'bbox': bbox,
                    'text': labels[i] if i < len(labels) else ''
                })
            
            return annotations
        
        except Exception as e:
            logger.error(f"Lỗi khi đọc CSV file: {e}")
            return []
    
    def detect_regions(self, image: np.ndarray, 
                      annotation_file: Optional[str] = None,
                      use_yolo: bool = False) -> Dict[str, List[Dict]]:
        """
        Phát hiện các vùng quan trọng trên hóa đơn
        
        Args:
            image: Ảnh đầu vào
            annotation_file: File annotation (nếu không dùng YOLO)
            use_yolo: Có sử dụng YOLO model không
            
        Returns:
            Dict[str, List[Dict]]: Dictionary chứa các vùng theo loại
        """
        regions = {
            'store_name': [],
            'address': [],
            'date': [],
            'total_amount': [],
            'items': [],
            'all_text': []
        }
        
        # Detect với YOLO
        if use_yolo and self.model is not None:
            detections = self.detect_with_yolo(image)
        # Hoặc load từ annotation
        elif annotation_file:
            detections = self.load_annotations_from_file(annotation_file)
        else:
            logger.warning("Không có nguồn detection!")
            return regions
        
        # Phân loại vùng
        for det in detections:
            label = det.get('label', 'text_region')
            
            if label == 'store_name':
                regions['store_name'].append(det)
            elif label == 'address':
                regions['address'].append(det)
            elif label == 'date':
                regions['date'].append(det)
            elif label == 'total_amount':
                regions['total_amount'].append(det)
            
            # Lưu tất cả vào all_text
            regions['all_text'].append(det)
        
        return regions
    
    def group_text_lines(self, regions: List[Dict], 
                        vertical_threshold: int = 10) -> List[List[Dict]]:
        """
        Nhóm các vùng text thành các dòng
        
        Args:
            regions: Danh sách các vùng text
            vertical_threshold: Ngưỡng khoảng cách dọc để gộp thành 1 dòng
            
        Returns:
            List[List[Dict]]: Danh sách các dòng text
        """
        if not regions:
            return []
        
        # Sắp xếp theo tọa độ y
        sorted_regions = sorted(regions, key=lambda r: r['bbox'][1])
        
        lines = []
        current_line = [sorted_regions[0]]
        current_y = sorted_regions[0]['bbox'][1]
        
        for region in sorted_regions[1:]:
            y = region['bbox'][1]
            
            if abs(y - current_y) <= vertical_threshold:
                current_line.append(region)
            else:
                # Sắp xếp theo x trong dòng
                current_line.sort(key=lambda r: r['bbox'][0])
                lines.append(current_line)
                current_line = [region]
                current_y = y
        
        # Thêm dòng cuối
        if current_line:
            current_line.sort(key=lambda r: r['bbox'][0])
            lines.append(current_line)
        
        return lines
    
    def visualize_detections(self, image: np.ndarray, 
                            detections: List[Dict],
                            save_path: Optional[str] = None) -> np.ndarray:
        """
        Vẽ các vùng phát hiện được lên ảnh
        
        Args:
            image: Ảnh gốc
            detections: Danh sách các detection
            save_path: Đường dẫn lưu ảnh (optional)
            
        Returns:
            np.ndarray: Ảnh đã vẽ
        """
        result = image.copy()
        
        # Màu cho từng loại
        colors = {
            'store_name': (0, 255, 0),      # Xanh lá
            'address': (255, 0, 0),         # Xanh dương
            'date': (0, 165, 255),          # Cam
            'total_amount': (0, 0, 255),    # Đỏ
            'text_region': (255, 255, 0)    # Cyan
        }
        
        for det in detections:
            label = det.get('label', 'text_region')
            bbox = det['bbox']
            color = colors.get(label, (255, 255, 255))
            
            # Vẽ bounding box
            x, y, w, h = bbox
            cv2.rectangle(result, (x, y), (x+w, y+h), color, 2)
            
            # Vẽ label
            cv2.putText(result, label, (x, y-5), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)
            
            # Vẽ polygon nếu có
            if 'polygon' in det:
                pts = np.array(det['polygon'], dtype=np.int32)
                cv2.polylines(result, [pts], True, color, 1)
        
        if save_path:
            cv2.imwrite(save_path, result)
            logger.info(f"Đã lưu visualization tại {save_path}")
        
        return result


def train_yolo_model(data_yaml: str, epochs: int = 100, 
                     imgsz: int = 640, batch: int = 16):
    """
    Train YOLO model cho detection
    
    Args:
        data_yaml: Đường dẫn file cấu hình data
        epochs: Số epoch
        imgsz: Kích thước ảnh
        batch: Batch size
    """
    try:
        from ultralytics import YOLO
        
        # Khởi tạo model
        model = YOLO('yolov8n.pt')  # nano model
        
        # Train
        results = model.train(
            data=data_yaml,
            epochs=epochs,
            imgsz=imgsz,
            batch=batch,
            name='receipt_detector',
            patience=50,
            save=True,
            device='cuda'  # hoặc 'cpu'
        )
        
        logger.info("Training completed!")
        return model
    
    except Exception as e:
        logger.error(f"Lỗi khi train model: {e}")
        return None


if __name__ == "__main__":
    # Test code
    detector = RegionDetector()
    
    print("RegionDetector module loaded successfully!")
    print("Category mapping:")
    for cat_id, label in detector.CATEGORY_MAPPING.items():
        print(f"  {cat_id}: {label}")
