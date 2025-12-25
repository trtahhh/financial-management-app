"""
Module nhận dạng văn bản sử dụng VietOCR
Hỗ trợ tiếng Việt tối ưu
"""

from vietocr.tool.predictor import Predictor
from vietocr.tool.config import Cfg
from PIL import Image
import numpy as np
from typing import List, Dict, Tuple, Optional
import logging
import cv2
import torch

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class TextRecognizer:
    """Lớp nhận dạng văn bản từ ảnh sử dụng VietOCR"""
    
    def __init__(self, languages: List[str] = ['vi', 'en'], gpu: bool = False):
        """
        Khởi tạo VietOCR
        
        Args:
            languages: Danh sách ngôn ngữ (chỉ hỗ trợ 'vi')
            gpu: Có sử dụng GPU không
        """
        self.languages = languages
        self.gpu = gpu
        self.detector = None
        self._initialize_reader()
    
    def _initialize_reader(self):
        """Khởi tạo VietOCR predictor"""
        try:
            logger.info("Đang khởi tạo VietOCR...")
            
            # Cấu hình VietOCR
            config = Cfg.load_config_from_name('vgg_transformer')  # hoặc 'vgg_seq2seq'
            
            # Cấu hình device
            if self.gpu and torch.cuda.is_available():
                config['device'] = 'cuda:0'
                logger.info("Sử dụng GPU cho VietOCR")
            else:
                config['device'] = 'cpu'
                logger.info("Sử dụng CPU cho VietOCR")
            
            # Tăng batch size nếu có GPU
            config['predictor']['beamsearch'] = False  # Tắt beam search để nhanh hơn
            
            # Khởi tạo predictor
            self.detector = Predictor(config)
            logger.info("Đã khởi tạo VietOCR thành công")
            
        except Exception as e:
            logger.error(f"Lỗi khi khởi tạo VietOCR: {e}")
            self.detector = None
    
    def recognize_text(self, image: np.ndarray, 
                      detail: int = 1,
                      paragraph: bool = False,
                      min_size: int = 10,
                      contrast_ths: float = 0.1,
                      adjust_contrast: float = 0.5,
                      filter_ths: float = 0.003) -> List[Tuple]:
        """
        Nhận dạng văn bản từ ảnh
        VietOCR chỉ nhận dạng text, không detect bbox
        
        Args:
            image: Ảnh đầu vào (numpy array hoặc PIL Image)
            Các tham số khác được giữ để tương thích, nhưng không được sử dụng
            
        Returns:
            List[Tuple]: [(None, text, 1.0)] - bbox=None vì VietOCR không detect
        """
        if self.detector is None:
            logger.error("VietOCR chưa được khởi tạo!")
            return []
        
        try:
            # Convert numpy array to PIL Image
            if isinstance(image, np.ndarray):
                # Convert BGR to RGB if needed
                if len(image.shape) == 3 and image.shape[2] == 3:
                    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
                else:
                    image_rgb = image
                pil_image = Image.fromarray(image_rgb)
            else:
                pil_image = image
            
            # Nhận dạng text
            text = self.detector.predict(pil_image)
            
            logger.info(f"Nhận dạng được {len(text.split())} từ")
            
            # Trả về format tương thích với EasyOCR
            # VietOCR không trả bbox, nên set None
            return [(None, text, 1.0)]
        
        except Exception as e:
            logger.error(f"Lỗi khi nhận dạng text: {e}")
            return []
    
    def recognize_region(self, image: np.ndarray, bbox: List[int],
                        detail: int = 1) -> str:
        """
        Nhận dạng text trong một vùng cụ thể
        
        Args:
            image: Ảnh gốc
            bbox: Bounding box [x, y, width, height]
            detail: Mức độ chi tiết
            
        Returns:
            str: Text nhận dạng được
        """
        # Crop vùng
        x, y, w, h = bbox
        x, y, w, h = max(0, x), max(0, y), max(1, w), max(1, h)
        
        if y + h > image.shape[0]:
            h = image.shape[0] - y
        if x + w > image.shape[1]:
            w = image.shape[1] - x
        
        region = image[y:y+h, x:x+w]
        
        if region.size == 0:
            return ""
        
        # Nhận dạng
        results = self.recognize_text(region, detail=detail)
        
        # Gộp text
        if not results:
            return ""
        
        texts = [text for (_, text, _) in results]
        return ' '.join(texts)
    
    def recognize_polygon(self, image: np.ndarray, 
                         polygon: List[Tuple[int, int]],
                         detail: int = 1) -> str:
        """
        Nhận dạng text trong vùng polygon
        
        Args:
            image: Ảnh gốc
            polygon: Danh sách các điểm polygon
            detail: Mức độ chi tiết
            
        Returns:
            str: Text nhận dạng được
        """
        # Tạo mask
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
        
        # Nhận dạng
        ocr_results = self.recognize_text(result, detail=detail)
        
        if not ocr_results:
            return ""
        
        texts = [text for (_, text, _) in ocr_results]
        return ' '.join(texts)
    
    def recognize_multiple_regions(self, image: np.ndarray,
                                  regions: List[Dict],
                                  use_polygon: bool = True) -> List[Dict]:
        """
        Nhận dạng text cho nhiều vùng
        
        Args:
            image: Ảnh gốc
            regions: Danh sách các vùng cần nhận dạng
            use_polygon: Sử dụng polygon hay bbox
            
        Returns:
            List[Dict]: Danh sách vùng đã có text
        """
        results = []
        
        for region in regions:
            region_copy = region.copy()
            
            try:
                if use_polygon and 'polygon' in region:
                    text = self.recognize_polygon(image, region['polygon'])
                else:
                    text = self.recognize_region(image, region['bbox'])
                
                region_copy['text'] = text
                results.append(region_copy)
                
            except Exception as e:
                logger.warning(f"Lỗi khi nhận dạng region: {e}")
                region_copy['text'] = ""
                results.append(region_copy)
        
        return results
    
    def get_text_only(self, image: np.ndarray, 
                     paragraph: bool = True) -> str:
        """
        Lấy toàn bộ text từ ảnh (không có bbox)
        
        Args:
            image: Ảnh đầu vào
            paragraph: Gộp thành đoạn văn
            
        Returns:
            str: Text nhận dạng được
        """
        results = self.recognize_text(image, detail=1, paragraph=paragraph)
        
        if not results:
            return ""
        
        texts = [text for (_, text, _) in results]
        
        if paragraph:
            return '\n'.join(texts)
        else:
            return ' '.join(texts)
    
    def recognize_with_confidence(self, image: np.ndarray,
                                 confidence_threshold: float = 0.5) -> List[Dict]:
        """
        Nhận dạng text với ngưỡng confidence
        
        Args:
            image: Ảnh đầu vào
            confidence_threshold: Ngưỡng confidence tối thiểu
            
        Returns:
            List[Dict]: Danh sách kết quả với confidence >= threshold
        """
        results = self.recognize_text(image, detail=1)
        
        filtered_results = []
        for bbox, text, conf in results:
            if conf >= confidence_threshold:
                filtered_results.append({
                    'bbox': bbox,
                    'text': text,
                    'confidence': conf
                })
        
        return filtered_results
    
    def recognize_lines(self, image: np.ndarray) -> List[str]:
        """
        Nhận dạng text theo từng dòng
        
        Args:
            image: Ảnh đầu vào
            
        Returns:
            List[str]: Danh sách các dòng text
        """
        results = self.recognize_text(image, detail=1)
        
        if not results:
            return []
        
        # Nhóm theo dòng (dựa vào tọa độ y)
        lines_dict = {}
        for bbox, text, conf in results:
            # Lấy y trung bình
            points = np.array(bbox)
            y_avg = int(np.mean(points[:, 1]))
            
            # Gom vào dòng (threshold = 15 pixels)
            found = False
            for y_key in lines_dict.keys():
                if abs(y_key - y_avg) < 15:
                    lines_dict[y_key].append((bbox[0][0], text))  # (x, text)
                    found = True
                    break
            
            if not found:
                lines_dict[y_avg] = [(bbox[0][0], text)]
        
        # Sắp xếp theo y, sau đó sắp xếp text trong dòng theo x
        lines = []
        for y in sorted(lines_dict.keys()):
            line_texts = [text for _, text in sorted(lines_dict[y], key=lambda x: x[0])]
            lines.append(' '.join(line_texts))
        
        return lines
    
    def batch_recognize(self, images: List[np.ndarray],
                       batch_size: int = 4) -> List[List[Tuple]]:
        """
        Nhận dạng batch ảnh
        
        Args:
            images: Danh sách ảnh
            batch_size: Kích thước batch
            
        Returns:
            List[List[Tuple]]: Kết quả cho từng ảnh
        """
        all_results = []
        
        for i in range(0, len(images), batch_size):
            batch = images[i:i+batch_size]
            
            for img in batch:
                results = self.recognize_text(img)
                all_results.append(results)
        
        return all_results
    
    def visualize_ocr_results(self, image: np.ndarray,
                             results: List[Tuple],
                             save_path: Optional[str] = None) -> np.ndarray:
        """
        Vẽ kết quả OCR lên ảnh
        
        Args:
            image: Ảnh gốc
            results: Kết quả OCR
            save_path: Đường dẫn lưu ảnh
            
        Returns:
            np.ndarray: Ảnh đã vẽ
        """
        result_img = image.copy()
        
        for bbox, text, conf in results:
            # Vẽ bbox
            points = np.array(bbox, dtype=np.int32)
            cv2.polylines(result_img, [points], True, (0, 255, 0), 2)
            
            # Vẽ text
            x, y = int(bbox[0][0]), int(bbox[0][1]) - 5
            cv2.putText(result_img, text, (x, y),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 1)
            
            # Vẽ confidence
            conf_text = f"{conf:.2f}"
            cv2.putText(result_img, conf_text, (x, y - 15),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.4, (255, 0, 0), 1)
        
        if save_path:
            cv2.imwrite(save_path, result_img)
            logger.info(f"Đã lưu visualization tại {save_path}")
        
        return result_img


class VietnameseTextNormalizer:
    """Lớp chuẩn hóa text tiếng Việt"""
    
    @staticmethod
    def remove_extra_spaces(text: str) -> str:
        """Xóa khoảng trắng thừa"""
        return ' '.join(text.split())
    
    @staticmethod
    def normalize_punctuation(text: str) -> str:
        """Chuẩn hóa dấu câu"""
        # Thêm khoảng trắng sau dấu câu
        for punct in ['.', ',', '!', '?', ':', ';']:
            text = text.replace(punct, f'{punct} ')
        return text
    
    @staticmethod
    def fix_common_ocr_errors(text: str) -> str:
        """Sửa lỗi OCR phổ biến"""
        replacements = {
            ' 0 ': ' O ',  # Số 0 thành chữ O
            '|': 'l',      # Pipe thành l
            '5': 'S',      # Trong một số context
        }
        
        result = text
        for old, new in replacements.items():
            result = result.replace(old, new)
        
        return result
    
    @staticmethod
    def normalize(text: str) -> str:
        """Pipeline chuẩn hóa hoàn chỉnh"""
        text = VietnameseTextNormalizer.remove_extra_spaces(text)
        text = VietnameseTextNormalizer.normalize_punctuation(text)
        text = VietnameseTextNormalizer.remove_extra_spaces(text)
        return text.strip()


if __name__ == "__main__":
    # Test code
    print("TextRecognizer module loaded successfully!")
    print("Supported languages: ['vi']")
    print("\nInitializing VietOCR...")
    
    try:
        recognizer = TextRecognizer(languages=['vi'], gpu=False)
        print("✓ VietOCR initialized successfully!")
    except Exception as e:
        print(f"✗ Error initializing VietOCR: {e}")
