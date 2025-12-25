"""
RECEIPT OCR MODULE - Dùng cho dự án
Cách sử dụng đơn giản để tích hợp vào project
"""

import cv2
import numpy as np
from pathlib import Path
from typing import Dict, Optional
import sys

# Import từ ultimate_yolo_ocr
sys.path.append(str(Path(__file__).parent))
from ultimate_yolo_ocr import UltimateYOLOOCRPipeline


class ReceiptOCR:
    """
    Class đơn giản để quét và trích xuất thông tin hóa đơn
    
    Sử dụng:
    >>> ocr = ReceiptOCR()
    >>> result = ocr.scan("path/to/receipt.jpg")
    >>> print(result)
    {
        'store_name': 'VINMART',
        'total_amount': 250000,
        'date': '15/08/2020',
        'address': '590 Trần Phú...',
        'success': True,
        'completeness': 100
    }
    """
    
    def __init__(self, model_path: str = "runs/detect/receipt_detector3/weights/best.pt"):
        """
        Khởi tạo Receipt OCR
        
        Args:
            model_path: Đường dẫn đến YOLO model (mặc định: model đã train)
        """
        print("🚀 Khởi tạo Receipt OCR System...")
        self.pipeline = UltimateYOLOOCRPipeline(model_path)
        print("✅ Sẵn sàng quét hóa đơn!")
    
    def scan(self, image_path: str) -> Dict:
        """
        Quét và trích xuất thông tin từ hóa đơn
        
        Args:
            image_path: Đường dẫn đến ảnh hóa đơn
        
        Returns:
            Dictionary chứa thông tin đã trích xuất:
            {
                'store_name': str,      # Tên cửa hàng
                'total_amount': int,    # Tổng tiền (VNĐ)
                'date': str,            # Ngày (DD/MM/YYYY)
                'address': str,         # Địa chỉ
                'success': bool,        # Thành công hay không
                'completeness': float,  # % thông tin đầy đủ
                'message': str          # Thông báo
            }
        """
        try:
            # Kiểm tra file tồn tại
            img_path = Path(image_path)
            if not img_path.exists():
                return {
                    'store_name': '',
                    'total_amount': 0,
                    'date': '',
                    'address': '',
                    'success': False,
                    'completeness': 0,
                    'message': f'File không tồn tại: {image_path}'
                }
            
            # Xử lý ảnh
            result = self.pipeline.process_image(img_path)
            
            if result is None:
                return {
                    'store_name': '',
                    'total_amount': 0,
                    'date': '',
                    'address': '',
                    'success': False,
                    'completeness': 0,
                    'message': 'Không thể xử lý ảnh'
                }
            
            # Format kết quả
            success = result['completeness'] >= 75
            
            return {
                'store_name': result['store_name'],
                'total_amount': result['total_amount'],
                'date': result['date'],
                'address': result['address'],
                'success': success,
                'completeness': result['completeness'],
                'message': 'Thành công' if success else 'Thiếu thông tin'
            }
            
        except Exception as e:
            return {
                'store_name': '',
                'total_amount': 0,
                'date': '',
                'address': '',
                'success': False,
                'completeness': 0,
                'message': f'Lỗi: {str(e)}'
            }
    
    def scan_image_bytes(self, image_bytes: bytes) -> Dict:
        """
        Quét từ bytes (dùng cho upload file)
        
        Args:
            image_bytes: Bytes của ảnh
        
        Returns:
            Dictionary chứa thông tin đã trích xuất
        """
        try:
            # Convert bytes to numpy array
            nparr = np.frombuffer(image_bytes, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            
            if img is None:
                return {
                    'store_name': '',
                    'total_amount': 0,
                    'date': '',
                    'address': '',
                    'success': False,
                    'completeness': 0,
                    'message': 'Không thể đọc ảnh'
                }
            
            # Lưu tạm
            temp_path = Path("temp_receipt.jpg")
            cv2.imwrite(str(temp_path), img)
            
            # Scan
            result = self.scan(str(temp_path))
            
            # Xóa file tạm
            if temp_path.exists():
                temp_path.unlink()
            
            return result
            
        except Exception as e:
            return {
                'store_name': '',
                'total_amount': 0,
                'date': '',
                'address': '',
                'success': False,
                'completeness': 0,
                'message': f'Lỗi: {str(e)}'
            }
    
    def batch_scan(self, image_paths: list) -> list:
        """
        Quét nhiều ảnh cùng lúc
        
        Args:
            image_paths: List đường dẫn ảnh
        
        Returns:
            List các dictionary kết quả
        """
        results = []
        for img_path in image_paths:
            result = self.scan(img_path)
            result['image_path'] = img_path
            results.append(result)
        return results


# Example usage
if __name__ == "__main__":
    # Khởi tạo
    ocr = ReceiptOCR()
    
    # Quét 1 ảnh
    print("\n" + "="*60)
    print("TEST: Quét 1 ảnh")
    print("="*60)
    
    test_image = "archive/train_images/train_images/mcocr_public_145013aagqw.jpg"
    result = ocr.scan(test_image)
    
    print(f"\n📄 Kết quả:")
    print(f"  🏪 Tên cửa hàng: {result['store_name']}")
    print(f"  💰 Tổng tiền: {result['total_amount']:,} đ")
    print(f"  📅 Ngày: {result['date']}")
    print(f"  📍 Địa chỉ: {result['address'][:50]}...")
    print(f"  ✅ Thành công: {result['success']}")
    print(f"  📊 Độ đầy đủ: {result['completeness']:.0f}%")
    print(f"  💬 Thông báo: {result['message']}")
