"""
ULTIMATE YOLO-BASED OCR SYSTEM FOR VIETNAMESE RECEIPTS
Không sử dụng CSV Annotations - chỉ dùng YOLO detection
Tối ưu hóa tối đa cho 4 trường: Tên cửa hàng, Tổng tiền, Ngày tháng, Địa chỉ
"""

import cv2
import numpy as np
from pathlib import Path
import pandas as pd
from vietocr.tool.predictor import Predictor
from vietocr.tool.config import Cfg
import torch
import logging
import re
from difflib import SequenceMatcher
from typing import Dict, List, Tuple, Optional
from ultralytics import YOLO
from PIL import Image

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class AdvancedPreprocessor:
    """Preprocessing nâng cao với nhiều phương pháp kết hợp"""
    
    @staticmethod
    def super_upscale(image: np.ndarray, scale: int = 3) -> np.ndarray:
        """Upscale 3x để tăng độ phân giải"""
        h, w = image.shape[:2]
        return cv2.resize(image, (w * scale, h * scale), interpolation=cv2.INTER_CUBIC)
    
    @staticmethod
    def advanced_denoise(image: np.ndarray) -> np.ndarray:
        """Khử nhiễu đa tầng"""
        # Bước 1: fastNlMeansDenoising
        denoised = cv2.fastNlMeansDenoising(image, None, h=10, templateWindowSize=7, searchWindowSize=21)
        # Bước 2: Bilateral filter
        denoised = cv2.bilateralFilter(denoised, 9, 75, 75)
        # Bước 3: Morphological closing
        kernel = np.ones((2, 2), np.uint8)
        denoised = cv2.morphologyEx(denoised, cv2.MORPH_CLOSE, kernel)
        return denoised
    
    @staticmethod
    def adaptive_contrast(image: np.ndarray) -> np.ndarray:
        """CLAHE với clipLimit cao"""
        clahe = cv2.createCLAHE(clipLimit=5.0, tileGridSize=(8, 8))
        return clahe.apply(image)
    
    @staticmethod
    def extreme_sharpen(image: np.ndarray) -> np.ndarray:
        """Sharpening cực mạnh"""
        gaussian = cv2.GaussianBlur(image, (0, 0), 3.0)
        sharpened = cv2.addWeighted(image, 2.5, gaussian, -1.5, 0)
        return np.clip(sharpened, 0, 255).astype(np.uint8)
    
    @staticmethod
    def multi_threshold(image: np.ndarray) -> np.ndarray:
        """Kết hợp nhiều phương pháp threshold"""
        _, otsu = cv2.threshold(image, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        adaptive_gauss = cv2.adaptiveThreshold(image, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                               cv2.THRESH_BINARY, 21, 10)
        adaptive_mean = cv2.adaptiveThreshold(image, 255, cv2.ADAPTIVE_THRESH_MEAN_C,
                                              cv2.THRESH_BINARY, 21, 10)
        combined = cv2.bitwise_and(otsu, cv2.bitwise_and(adaptive_gauss, adaptive_mean))
        return combined
    
    def process_for_ocr(self, image: np.ndarray) -> np.ndarray:
        """Pipeline cho OCR - mạnh nhất"""
        if len(image.shape) == 3:
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        else:
            gray = image.copy()
        
        # Super upscale
        upscaled = self.super_upscale(gray, scale=3)
        # Advanced denoise
        denoised = self.advanced_denoise(upscaled)
        # Adaptive contrast
        contrasted = self.adaptive_contrast(denoised)
        # Extreme sharpen
        sharpened = self.extreme_sharpen(contrasted)
        # Multi threshold
        binary = self.multi_threshold(sharpened)
        
        return binary


class HybridVietOCR:
    """VietOCR với nhiều cấu hình khác nhau"""
    
    def __init__(self):
        logger.info("Khởi tạo Hybrid VietOCR...")
        
        self.config = Cfg.load_config_from_name('vgg_transformer')
        self.config['cnn']['pretrained'] = False
        self.config['device'] = 'cuda' if torch.cuda.is_available() else 'cpu'
        self.config['predictor']['beamsearch'] = True
        self.predictor = Predictor(self.config)
        
        logger.info(f"✅ Sử dụng {'GPU' if torch.cuda.is_available() else 'CPU'}")
        logger.info("✅ VietOCR ready!")
    
    def recognize_multi_config(self, image: np.ndarray, preprocessor: AdvancedPreprocessor) -> str:
        """Nhận dạng với nhiều cấu hình preprocessing"""
        results = []
        
        # Config 1: Full preprocessing pipeline
        processed1 = preprocessor.process_for_ocr(image)
        pil_img1 = Image.fromarray(processed1)
        text1 = self.predictor.predict(pil_img1)
        results.append(text1)
        
        # Config 2: Stronger denoise + contrast
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY) if len(image.shape) == 3 else image
        upscaled = preprocessor.super_upscale(gray, scale=3)
        denoised = preprocessor.advanced_denoise(upscaled)
        contrasted = preprocessor.adaptive_contrast(denoised)
        pil_img2 = Image.fromarray(contrasted)
        text2 = self.predictor.predict(pil_img2)
        results.append(text2)
        
        # Config 3: Binary only with Otsu
        _, binary = cv2.threshold(upscaled, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        pil_img3 = Image.fromarray(binary)
        text3 = self.predictor.predict(pil_img3)
        results.append(text3)
        
        # Config 4: Original upscaled
        pil_img4 = Image.fromarray(upscaled)
        text4 = self.predictor.predict(pil_img4)
        results.append(text4)
        
        # Chọn kết quả tốt nhất
        best_result = self._select_best_result(results)
        return best_result
    
    def _select_best_result(self, results: List[str]) -> str:
        """Chọn kết quả tốt nhất"""
        if not results:
            return ""
        
        scores = []
        for text in results:
            score = 0
            score += len(text) * 2
            score += sum(1 for c in text if c.isalpha()) * 3
            score += sum(1 for c in text if c.isdigit()) * 1.5
            score -= sum(1 for c in text if not (c.isalnum() or c.isspace() or c in ',./-:()'))
            scores.append(score)
        
        best_idx = scores.index(max(scores))
        return results[best_idx]


class SmartExtractor:
    """Trích xuất thông tin thông minh"""
    
    KNOWN_STORES = {
        'VINMART': ['VINMART', 'VIN MART', 'VINCOMMERCE', 'VIN COMMERCE', 'VINCOM'],
        'CO.OP': ['CO.OP', 'COOP', 'CO OP', 'SAIGON COOP'],
        'LOTTE': ['LOTTE', 'LOTTEMART', 'LOTTE MART'],
        'BIG C': ['BIG C', 'BIGC', 'BIG-C'],
        'CIRCLE K': ['CIRCLE K', 'CIRCLEK'],
        'GS25': ['GS25', 'GS 25'],
        'FAMILY MART': ['FAMILY MART', 'FAMILYMART'],
        'MINISTOP': ['MINISTOP', 'MINI STOP'],
        'MINIMART': ['MINIMART', 'MINI MART', 'ANAN', 'MINIMART ANAN'],
        'GUARDIAN': ['GUARDIAN'],
        'MEGA MARKET': ['MEGA MARKET', 'MEGAMARKET', 'MM MEGA MARKET'],
        'AEON': ['AEON', 'AEON MALL'],
        'WINMART': ['WINMART', 'WIN MART'],
        'UNIQLO': ['UNIQLO', 'UNIOLO', 'UNIQL0', 'UNIOLO'],
        'H&M': ['H&M', 'H & M'],
        'ZARA': ['ZARA'],
        '7-ELEVEN': ['7-ELEVEN', '7 ELEVEN', '7ELEVEN'],
    }
    
    def __init__(self):
        self.amount_patterns = [
            re.compile(r'(\d{1,3}(?:[,.\s]\d{3})+)'),
            re.compile(r'(\d{4,})'),
            re.compile(r'(\d{1,3}(?:[,.\s]\d{3})+)\s*đ'),
            re.compile(r'đ\s*(\d{1,3}(?:[,.\s]\d{3})+)'),
        ]
        
        self.date_patterns = [
            re.compile(r'(\d{1,2})/(\d{1,2})/(\d{4})'),
            re.compile(r'(\d{1,2})-(\d{1,2})-(\d{4})'),
            re.compile(r'(\d{4})/(\d{1,2})/(\d{1,2})'),
            re.compile(r'(\d{1,2})\.(\d{1,2})\.(\d{4})'),
            re.compile(r'ngày\s*(\d{1,2})\s*tháng\s*(\d{1,2})\s*năm\s*(\d{4})'),
            re.compile(r'(\d{1,2})/(\d{1,2})/(\d{2})'),
        ]
        
        self.total_keywords = ['tổng', 'total', 'tong', 'thanh toán', 'phải trả', 'tổng tiền', 'sum', 'grand total']
    
    def fuzzy_match(self, text: str, target: str) -> float:
        """Fuzzy matching"""
        text_clean = text.upper().strip()
        target_clean = target.upper().strip()
        return SequenceMatcher(None, text_clean, target_clean).ratio()
    
    def extract_store_name(self, ocr_results: List[Dict], category_id: int = 15) -> str:
        """Trích xuất tên cửa hàng"""
        store_candidates = [r for r in ocr_results if r.get('category_id') == category_id]
        
        if not store_candidates:
            return ""
        
        all_store_text = ' '.join([r['text'] for r in store_candidates])
        
        best_match = ""
        best_score = 0.55
        
        for canonical_name, variants in self.KNOWN_STORES.items():
            for variant in variants:
                score = self.fuzzy_match(all_store_text, variant)
                if score > best_score:
                    best_score = score
                    best_match = canonical_name
        
        if not best_match and store_candidates:
            best_match = max(store_candidates, key=lambda x: len(x['text']))['text']
        
        return best_match
    
    def normalize_amount(self, amount_str: str) -> int:
        """Chuẩn hóa số tiền"""
        normalized = amount_str.replace('.', ',').replace(' ', ',').replace('_', ',')
        normalized = ''.join(c if c.isdigit() or c == ',' else '' for c in normalized)
        normalized = normalized.replace(',', '')
        
        try:
            return int(normalized)
        except:
            return 0
    
    def extract_total_amount(self, ocr_results: List[Dict], category_id: int = 18) -> int:
        """Trích xuất tổng tiền"""
        # First, try to find amounts in TOTAL category detections
        amount_candidates = [r for r in ocr_results if r.get('category_id') == category_id]
        
        total_amounts = []
        
        for result in amount_candidates:
            text = result['text']
            
            for pattern in self.amount_patterns:
                matches = pattern.findall(text)
                for match in matches:
                    amount = self.normalize_amount(match if isinstance(match, str) else match[0])
                    if 1000 <= amount <= 100000000:
                        total_amounts.append(amount)
        
        # If we found amounts in TOTAL category, return the largest
        if total_amounts:
            return max(total_amounts)
        
        # Fallback: Look for amounts near total keywords in all text
        keyword_amounts = []
        
        for result in ocr_results:
            text = result['text'].lower()
            
            for keyword in self.total_keywords:
                if keyword in text:
                    # Found a total keyword, extract amounts from this text
                    for pattern in self.amount_patterns:
                        matches = pattern.findall(result['text'])
                        for match in matches:
                            amount = self.normalize_amount(match if isinstance(match, str) else match[0])
                            if 1000 <= amount <= 100000000:
                                keyword_amounts.append(amount)
                    break  # Only check once per result
        
        # Return the largest amount found near keywords
        if keyword_amounts:
            return max(keyword_amounts)
        
        # Last resort: Look for amounts in all text, but filter out likely address numbers
        all_amounts = []
        
        for result in ocr_results:
            text = result['text']
            
            # Skip if it looks like an address (contains street/road keywords)
            if any(word in text.lower() for word in ['đường', 'phố', 'số', 'street', 'road', 'địa chỉ', 'address']):
                continue
            
            for pattern in self.amount_patterns:
                matches = pattern.findall(text)
                for match in matches:
                    amount = self.normalize_amount(match if isinstance(match, str) else match[0])
                    if 1000 <= amount <= 100000000:
                        all_amounts.append(amount)
        
        return max(all_amounts) if all_amounts else 0
    
    def extract_date(self, ocr_results: List[Dict], category_id: int = 17) -> str:
        """Trích xuất ngày tháng"""
        date_candidates = [r for r in ocr_results if r.get('category_id') == category_id]
        
        for result in date_candidates:
            text = result['text']
            
            for pattern in self.date_patterns:
                match = pattern.search(text)
                if match:
                    groups = match.groups()
                    if len(groups) == 3:
                        try:
                            if len(groups[2]) == 2:
                                year = '20' + groups[2]
                            else:
                                year = groups[2]
                            
                            if int(groups[0]) > 31:
                                return f"{groups[2]}/{groups[1]}/{groups[0]}"
                            else:
                                return f"{groups[0]}/{groups[1]}/{year}"
                        except:
                            pass
        
        return ""
    
    def extract_address(self, ocr_results: List[Dict], category_id: int = 16) -> str:
        """Trích xuất địa chỉ"""
        address_candidates = [r for r in ocr_results if r.get('category_id') == category_id]
        
        if not address_candidates:
            return ""
        
        addresses = [r['text'] for r in address_candidates]
        return ' '.join(addresses[:3])


class YOLODetector:
    """YOLO-based text detection"""
    
    CATEGORY_MAPPING = {
        0: (15, 'STORE'),
        1: (16, 'ADDRESS'),
        2: (17, 'DATE'),
        3: (18, 'TOTAL')
    }
    
    def __init__(self, model_path: str):
        logger.info(f"Khởi tạo YOLO model từ {model_path}...")
        self.model = YOLO(model_path)
        logger.info("✅ YOLO model ready!")
    
    def detect(self, image: np.ndarray, conf_threshold: float = 0.01) -> List[Dict]:
        """Detect text regions với YOLO"""
        results = self.model(image, conf=conf_threshold, verbose=False)
        
        detections = []
        for result in results:
            boxes = result.boxes
            
            for i in range(len(boxes)):
                conf = float(boxes.conf[i])
                cls = int(boxes.cls[i])
                xyxy = boxes.xyxy[i].cpu().numpy()
                
                x1, y1, x2, y2 = map(int, xyxy)
                
                category_id, label = self.CATEGORY_MAPPING.get(cls, (0, 'UNKNOWN'))
                
                detections.append({
                    'bbox': [x1, y1, x2, y2],
                    'confidence': conf,
                    'class': cls,
                    'label': label,
                    'category_id': category_id
                })
        
        return detections


class UltimateYOLOOCRPipeline:
    """Pipeline OCR hoàn chỉnh sử dụng YOLO"""
    
    CATEGORY_MAPPING = {
        15: 'STORE',
        16: 'ADDRESS',
        17: 'DATE',
        18: 'TOTAL'
    }
    
    def __init__(self, yolo_model_path: str):
        logger.info("=" * 60)
        logger.info("🚀 ULTIMATE YOLO OCR PIPELINE (NO CSV)")
        logger.info("=" * 60)
        
        self.detector = YOLODetector(yolo_model_path)
        self.preprocessor = AdvancedPreprocessor()
        self.ocr = HybridVietOCR()
        self.extractor = SmartExtractor()
        
        logger.info("✅ Pipeline ready!")
    
    def process_image(self, image_path: Path) -> Dict:
        """Xử lý một ảnh hoàn chỉnh"""
        logger.info("\n" + "=" * 60)
        logger.info(f"📄 Processing: {image_path.name}")
        logger.info("=" * 60)
        
        # Load image
        image = cv2.imread(str(image_path))
        if image is None:
            logger.error(f"❌ Cannot read image: {image_path}")
            return None
        
        # Detect với YOLO
        detections = self.detector.detect(image, conf_threshold=0.01)
        logger.info(f"🎯 Detected {len(detections)} regions")
        
        # OCR cho từng vùng
        ocr_results = []
        for detection in detections:
            bbox = detection['bbox']
            x1, y1, x2, y2 = bbox
            
            # Crop region với padding
            pad = 5
            h, w = image.shape[:2]
            x1 = max(0, x1 - pad)
            y1 = max(0, y1 - pad)
            x2 = min(w, x2 + pad)
            y2 = min(h, y2 + pad)
            
            region = image[y1:y2, x1:x2]
            if region.size == 0:
                continue
            
            # Preprocess và OCR
            text = self.ocr.recognize_multi_config(region, self.preprocessor)
            
            ocr_results.append({
                'text': text,
                'bbox': bbox,
                'confidence': detection['confidence'],
                'label': detection['label'],
                'category_id': detection['category_id']
            })
            
            label_name = self.CATEGORY_MAPPING.get(detection['category_id'], 'OTHER')
            logger.info(f"  📝 {label_name}: {text[:50]}... (conf: {detection['confidence']:.2f})")
        
        # Extract thông tin
        logger.info("\n📊 RESULTS:")
        
        store_name = self.extractor.extract_store_name(ocr_results)
        total_amount = self.extractor.extract_total_amount(ocr_results)
        date = self.extractor.extract_date(ocr_results)
        address = self.extractor.extract_address(ocr_results)
        
        logger.info(f"  🏪 Store: {store_name}")
        logger.info(f"  💰 Total: {total_amount:,} đ")
        logger.info(f"  📅 Date: {date}")
        logger.info(f"  📍 Address: {address[:70]}...")
        
        # Tính completeness
        completeness = sum([
            bool(store_name),
            bool(total_amount > 0),
            bool(date),
            bool(address)
        ]) / 4 * 100
        
        logger.info(f"  ✅ Completeness: {completeness:.0f}%")
        
        return {
            'image_name': image_path.name,
            'store_name': store_name,
            'total_amount': total_amount,
            'date': date,
            'address': address,
            'completeness': completeness,
            'ocr_results': ocr_results
        }
    
    def visualize_results(self, image_path: Path, result: Dict, output_path: Path):
        """Vẽ kết quả lên ảnh"""
        image = cv2.imread(str(image_path))
        
        for ocr_result in result['ocr_results']:
            bbox = ocr_result['bbox']
            x1, y1, x2, y2 = bbox
            
            # Màu theo category
            colors = {15: (0, 255, 0), 16: (255, 0, 0), 17: (0, 0, 255), 18: (255, 255, 0)}
            color = colors.get(ocr_result['category_id'], (128, 128, 128))
            
            cv2.rectangle(image, (x1, y1), (x2, y2), color, 2)
            
            label = ocr_result['label']
            cv2.putText(image, label, (x1, y1 - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)
        
        cv2.imwrite(str(output_path), image)


def main():
    """Test với 10 ảnh ngẫu nhiên"""
    print("=" * 60)
    print("🚀 ULTIMATE YOLO OCR TEST (NO CSV)")
    print("=" * 60)
    
    # Paths
    yolo_model_path = "runs/detect/receipt_detector3/weights/best.pt"
    train_dir = Path("archive/train_images/train_images")
    output_dir = Path("yolo_ultimate_output")
    output_dir.mkdir(exist_ok=True)
    
    # Get random images
    all_images = list(train_dir.glob("*.jpg"))
    import random
    random.seed(42)
    random_images = random.sample(all_images, min(10, len(all_images)))
    
    # Initialize pipeline
    pipeline = UltimateYOLOOCRPipeline(yolo_model_path)
    
    # Process images
    results = []
    for img_path in random_images:
        result = pipeline.process_image(img_path)
        if result:
            results.append(result)
            
            # Visualize
            vis_path = output_dir / f"vis_{img_path.name}"
            pipeline.visualize_results(img_path, result, vis_path)
    
    # Summary
    print("\n" + "=" * 60)
    print("📊 SUMMARY")
    print("=" * 60)
    
    success = sum(1 for r in results if r['completeness'] >= 75)
    avg_completeness = sum(r['completeness'] for r in results) / len(results) if results else 0
    store_success = sum(1 for r in results if r['store_name'])
    amount_success = sum(1 for r in results if r['total_amount'] > 0)
    date_success = sum(1 for r in results if r['date'])
    address_success = sum(1 for r in results if r['address'])
    
    print(f"  Total: {len(results)} images")
    print(f"  Success (≥75%): {success}/{len(results)} ({success/len(results)*100:.1f}%)")
    print(f"  Avg Completeness: {avg_completeness:.1f}%")
    print(f"  Store Name: {store_success}/{len(results)}")
    print(f"  Total Amount: {amount_success}/{len(results)}")
    print(f"  Date: {date_success}/{len(results)}")
    print(f"  Address: {address_success}/{len(results)}")
    print(f"\n  📁 Output: {output_dir}/")
    print("=" * 60)
    
    # Save results
    results_df = pd.DataFrame([{
        'image_name': r['image_name'],
        'store_name': r['store_name'],
        'total_amount': r['total_amount'],
        'date': r['date'],
        'address': r['address'],
        'completeness': r['completeness']
    } for r in results])
    
    results_df.to_csv(output_dir / 'results.csv', index=False, encoding='utf-8-sig')
    logger.info(f"💾 Results saved to {output_dir / 'results.csv'}")


if __name__ == "__main__":
    main()
