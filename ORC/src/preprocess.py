"""
Module xử lý tiền xử lý ảnh cho hệ thống OCR
Bao gồm các chức năng chuẩn hóa, tăng cường, và cắt ảnh theo bounding box
"""

import cv2
import numpy as np
from typing import Tuple, List, Optional
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class ImagePreprocessor:
    """Lớp xử lý tiền xử lý ảnh"""
    
    def __init__(self, target_size: Optional[Tuple[int, int]] = None):
        """
        Khởi tạo preprocessor
        
        Args:
            target_size: Kích thước mục tiêu (width, height), None = giữ nguyên
        """
        self.target_size = target_size
    
    def read_image(self, image_path: str) -> np.ndarray:
        """
        Đọc ảnh từ đường dẫn
        
        Args:
            image_path: Đường dẫn đến file ảnh
            
        Returns:
            np.ndarray: Ảnh dưới dạng numpy array
        """
        img = cv2.imread(image_path)
        if img is None:
            raise ValueError(f"Không thể đọc ảnh từ {image_path}")
        
        logger.info(f"Đã đọc ảnh: {image_path}, kích thước: {img.shape}")
        return img
    
    def resize_image(self, image: np.ndarray, size: Optional[Tuple[int, int]] = None) -> np.ndarray:
        """
        Resize ảnh về kích thước mong muốn
        
        Args:
            image: Ảnh đầu vào
            size: Kích thước mục tiêu (width, height)
            
        Returns:
            np.ndarray: Ảnh đã resize
        """
        if size is None:
            size = self.target_size
        
        if size is None:
            return image
        
        return cv2.resize(image, size, interpolation=cv2.INTER_AREA)
    
    def convert_to_grayscale(self, image: np.ndarray) -> np.ndarray:
        """
        Chuyển ảnh sang grayscale
        
        Args:
            image: Ảnh đầu vào (BGR)
            
        Returns:
            np.ndarray: Ảnh grayscale
        """
        if len(image.shape) == 3:
            return cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        return image
    
    def apply_threshold(self, image: np.ndarray, method='adaptive') -> np.ndarray:
        """
        Áp dụng threshold để binarize ảnh
        
        Args:
            image: Ảnh grayscale
            method: Phương pháp threshold ('adaptive', 'otsu', 'binary')
            
        Returns:
            np.ndarray: Ảnh đã threshold
        """
        if method == 'adaptive':
            return cv2.adaptiveThreshold(
                image, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                cv2.THRESH_BINARY, 11, 2
            )
        elif method == 'otsu':
            _, binary = cv2.threshold(
                image, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU
            )
            return binary
        else:  # binary
            _, binary = cv2.threshold(image, 127, 255, cv2.THRESH_BINARY)
            return binary
    
    def denoise(self, image: np.ndarray) -> np.ndarray:
        """
        Giảm nhiễu ảnh
        
        Args:
            image: Ảnh đầu vào
            
        Returns:
            np.ndarray: Ảnh đã giảm nhiễu
        """
        if len(image.shape) == 3:
            return cv2.fastNlMeansDenoisingColored(image, None, 10, 10, 7, 21)
        else:
            return cv2.fastNlMeansDenoising(image, None, 10, 7, 21)
    
    def deskew(self, image: np.ndarray) -> np.ndarray:
        """
        Điều chỉnh góc nghiêng của ảnh
        
        Args:
            image: Ảnh đầu vào
            
        Returns:
            np.ndarray: Ảnh đã điều chỉnh
        """
        gray = self.convert_to_grayscale(image) if len(image.shape) == 3 else image
        
        # Tìm góc nghiêng
        coords = np.column_stack(np.where(gray > 0))
        if len(coords) == 0:
            return image
        
        angle = cv2.minAreaRect(coords)[-1]
        
        # Điều chỉnh góc
        if angle < -45:
            angle = -(90 + angle)
        else:
            angle = -angle
        
        # Xoay ảnh
        if abs(angle) > 0.5:  # Chỉ xoay nếu góc lớn hơn 0.5 độ
            (h, w) = image.shape[:2]
            center = (w // 2, h // 2)
            M = cv2.getRotationMatrix2D(center, angle, 1.0)
            rotated = cv2.warpAffine(
                image, M, (w, h),
                flags=cv2.INTER_CUBIC,
                borderMode=cv2.BORDER_REPLICATE
            )
            return rotated
        
        return image
    
    def sharpen(self, image: np.ndarray) -> np.ndarray:
        """
        Làm sắc nét ảnh
        
        Args:
            image: Ảnh đầu vào
            
        Returns:
            np.ndarray: Ảnh đã làm sắc nét
        """
        kernel = np.array([[-1, -1, -1],
                          [-1,  9, -1],
                          [-1, -1, -1]])
        return cv2.filter2D(image, -1, kernel)
    
    def crop_region(self, image: np.ndarray, bbox: List[float]) -> np.ndarray:
        """
        Cắt vùng ảnh theo bounding box
        
        Args:
            image: Ảnh đầu vào
            bbox: Bounding box [x, y, width, height]
            
        Returns:
            np.ndarray: Vùng ảnh đã cắt
        """
        x, y, w, h = [int(v) for v in bbox]
        
        # Đảm bảo tọa độ nằm trong ảnh
        x = max(0, x)
        y = max(0, y)
        w = min(w, image.shape[1] - x)
        h = min(h, image.shape[0] - y)
        
        return image[y:y+h, x:x+w]
    
    def crop_polygon(self, image: np.ndarray, polygon: List[Tuple[int, int]]) -> np.ndarray:
        """
        Cắt vùng ảnh theo polygon
        
        Args:
            image: Ảnh đầu vào
            polygon: Danh sách các điểm [(x1, y1), (x2, y2), ...]
            
        Returns:
            np.ndarray: Vùng ảnh đã cắt
        """
        # Tạo mask từ polygon
        mask = np.zeros(image.shape[:2], dtype=np.uint8)
        points = np.array(polygon, dtype=np.int32)
        cv2.fillPoly(mask, [points], 255)
        
        # Tìm bounding rect
        x, y, w, h = cv2.boundingRect(points)
        
        # Crop ảnh
        cropped = image[y:y+h, x:x+w].copy()
        mask_cropped = mask[y:y+h, x:x+w]
        
        # Áp dụng mask
        if len(cropped.shape) == 3:
            result = cv2.bitwise_and(cropped, cropped, mask=mask_cropped)
        else:
            result = cv2.bitwise_and(cropped, mask_cropped)
        
        return result
    
    def enhance_contrast(self, image: np.ndarray) -> np.ndarray:
        """
        Tăng độ tương phản của ảnh
        
        Args:
            image: Ảnh đầu vào
            
        Returns:
            np.ndarray: Ảnh đã tăng độ tương phản
        """
        if len(image.shape) == 3:
            # Convert to LAB color space
            lab = cv2.cvtColor(image, cv2.COLOR_BGR2LAB)
            l, a, b = cv2.split(lab)
            
            # Apply CLAHE to L channel
            clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8, 8))
            l = clahe.apply(l)
            
            # Merge channels
            enhanced = cv2.merge([l, a, b])
            return cv2.cvtColor(enhanced, cv2.COLOR_LAB2BGR)
        else:
            clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8, 8))
            return clahe.apply(image)
    
    def preprocess_for_ocr(self, image: np.ndarray, denoise_img: bool = True,
                          enhance: bool = True, upscale: bool = True) -> np.ndarray:
        """
        Pipeline tiền xử lý ảnh cho OCR (tối ưu cho VietOCR)
        
        Args:
            image: Ảnh đầu vào
            denoise_img: Có giảm nhiễu không
            enhance: Có tăng cường không
            upscale: Có phóng to ảnh nhỏ không (quan trọng cho VietOCR)
            
        Returns:
            np.ndarray: Ảnh đã tiền xử lý
        """
        result = image.copy()
        
        # Upscale small images (VietOCR works better with larger images)
        if upscale:
            h, w = result.shape[:2]
            # If image is too small, scale it up
            if h < 64 or w < 200:
                scale_factor = max(64 / h, 200 / w)
                new_w = int(w * scale_factor)
                new_h = int(h * scale_factor)
                result = cv2.resize(result, (new_w, new_h), interpolation=cv2.INTER_CUBIC)
        
        # Enhance contrast first (very important for OCR)
        if enhance:
            result = self.enhance_contrast(result)
        
        # Denoise (but use gentler parameters to preserve text)
        if denoise_img and len(result.shape) == 3:
            result = cv2.fastNlMeansDenoisingColored(result, None, 5, 5, 7, 21)
        elif denoise_img:
            result = cv2.fastNlMeansDenoising(result, None, 5, 7, 21)
        
        # Sharpen to make text clearer
        result = self.sharpen(result)
        
        return result
    
    def preprocess_pipeline(self, image_path: str, bbox: Optional[List[float]] = None,
                          for_ocr: bool = True) -> np.ndarray:
        """
        Pipeline hoàn chỉnh tiền xử lý ảnh
        
        Args:
            image_path: Đường dẫn ảnh
            bbox: Bounding box để crop [x, y, w, h]
            for_ocr: Có áp dụng preprocessing cho OCR không
            
        Returns:
            np.ndarray: Ảnh đã xử lý
        """
        # Đọc ảnh
        image = self.read_image(image_path)
        
        # Crop nếu có bbox
        if bbox is not None:
            image = self.crop_region(image, bbox)
        
        # Tiền xử lý cho OCR
        if for_ocr:
            image = self.preprocess_for_ocr(image)
        
        return image


def polygon_to_bbox(polygon: List[Tuple[int, int]]) -> List[int]:
    """
    Chuyển đổi polygon thành bounding box
    
    Args:
        polygon: Danh sách các điểm [(x1, y1), (x2, y2), ...]
        
    Returns:
        List[int]: [x, y, width, height]
    """
    points = np.array(polygon)
    x_min = int(np.min(points[:, 0]))
    y_min = int(np.min(points[:, 1]))
    x_max = int(np.max(points[:, 0]))
    y_max = int(np.max(points[:, 1]))
    
    return [x_min, y_min, x_max - x_min, y_max - y_min]


def parse_annotation_line(line: str) -> List[Tuple[int, int]]:
    """
    Parse một dòng annotation thành list các điểm
    
    Args:
        line: Dòng text chứa tọa độ (x1,y1,x2,y2,...)
        
    Returns:
        List[Tuple[int, int]]: Danh sách các điểm
    """
    coords = [int(float(x)) for x in line.strip().rstrip(',').split(',')]
    points = [(coords[i], coords[i+1]) for i in range(0, len(coords), 2)]
    return points


if __name__ == "__main__":
    # Test code
    preprocessor = ImagePreprocessor()
    
    # Example usage
    print("ImagePreprocessor module loaded successfully!")
    print("Available methods:")
    print("- read_image()")
    print("- preprocess_for_ocr()")
    print("- crop_region()")
    print("- crop_polygon()")
    print("- enhance_contrast()")
    print("- denoise()")
