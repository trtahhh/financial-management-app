"""
Pipeline chính cho hệ thống OCR hóa đơn tiếng Việt
Tích hợp tất cả các module: Detector, Preprocessor, OCR, Extractor
"""

import cv2
import numpy as np
import json
from pathlib import Path
from typing import Dict, List, Optional
import logging
import argparse

from preprocess import ImagePreprocessor
from detector import RegionDetector
from ocr import TextRecognizer
from extractor import InformationExtractor

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class ReceiptOCRPipeline:
    """Pipeline hoàn chỉnh cho OCR hóa đơn"""
    
    def __init__(self, 
                 yolo_model_path: Optional[str] = None,
                 use_gpu: bool = False,
                 languages: List[str] = ['vi', 'en']):
        """
        Khởi tạo pipeline
        
        Args:
            yolo_model_path: Đường dẫn model YOLO (nếu có)
            use_gpu: Sử dụng GPU cho OCR
            languages: Danh sách ngôn ngữ cho OCR
        """
        logger.info("Đang khởi tạo Receipt OCR Pipeline...")
        
        # Khởi tạo các module
        self.preprocessor = ImagePreprocessor()
        self.detector = RegionDetector(model_path=yolo_model_path)
        self.ocr = TextRecognizer(languages=languages, gpu=use_gpu)
        self.extractor = InformationExtractor()
        
        logger.info("Pipeline đã sẵn sàng!")
    
    def process_image(self, 
                     image_path: str,
                     annotation_file: Optional[str] = None,
                     csv_file: Optional[str] = None,
                     use_yolo: bool = True,  # Changed default to True
                     save_visualization: bool = False,
                     output_dir: Optional[str] = None) -> Dict:
        """
        Xử lý một ảnh hóa đơn
        
        Args:
            image_path: Đường dẫn ảnh
            annotation_file: File annotation (nếu không dùng YOLO)
            csv_file: File CSV chứa annotation
            use_yolo: Có dùng YOLO model không
            save_visualization: Có lưu ảnh visualization không
            output_dir: Thư mục lưu kết quả
            
        Returns:
            Dict: Kết quả trích xuất thông tin
        """
        logger.info(f"Đang xử lý: {image_path}")
        
        # 1. Đọc ảnh
        image = self.preprocessor.read_image(image_path)
        original_image = image.copy()
        
        # 2. Phát hiện vùng (Detection)
        if csv_file:
            # Load từ CSV
            image_id = Path(image_path).stem + '.jpg'
            regions = self.detector.load_annotations_from_csv(csv_file, image_id)
        elif annotation_file:
            # Load từ file annotation
            annotations = self.detector.load_annotations_from_file(annotation_file)
            regions = annotations
        elif use_yolo and self.detector.model is not None:
            # Dùng YOLO
            detections = self.detector.detect_with_yolo(image)
            regions = detections
        else:
            logger.warning("Không có nguồn detection!")
            regions = []
        
        logger.info(f"Phát hiện được {len(regions)} vùng")
        
        # 3. OCR từng vùng
        ocr_results = []
        for region in regions:
            try:
                # Tiền xử lý vùng
                if 'bbox' in region:
                    bbox = region['bbox']
                    cropped = self.preprocessor.crop_region(original_image, bbox)
                elif 'polygon' in region:
                    cropped = self.preprocessor.crop_polygon(original_image, region['polygon'])
                else:
                    continue
                
                # Enhance cho OCR
                enhanced = self.preprocessor.preprocess_for_ocr(cropped)
                
                # OCR
                text = self.ocr.get_text_only(enhanced, paragraph=False)
                
                region_copy = region.copy()
                region_copy['text'] = text
                ocr_results.append(region_copy)
                
                logger.debug(f"OCR: {text[:50]}...")
            
            except Exception as e:
                logger.warning(f"Lỗi khi OCR region: {e}")
                continue
        
        logger.info(f"Hoàn thành OCR cho {len(ocr_results)} vùng")
        
        # 4. Trích xuất thông tin
        extracted_info = self.extractor.extract_all(ocr_results)
        
        # 5. Post-processing
        extracted_info = self.extractor.post_process(extracted_info)
        
        # 6. Validation
        validation = self.extractor.validate_extraction(extracted_info)
        extracted_info['validation'] = validation
        
        # 7. Visualization (nếu cần)
        if save_visualization and output_dir:
            output_path = Path(output_dir)
            output_path.mkdir(parents=True, exist_ok=True)
            
            # Vẽ detection
            vis_img = self.detector.visualize_detections(
                original_image, 
                ocr_results,
                save_path=str(output_path / f"{Path(image_path).stem}_detection.jpg")
            )
        
        logger.info(f"Hoàn thành xử lý: {image_path}")
        logger.info(f"Kết quả: {json.dumps(extracted_info, ensure_ascii=False, indent=2)}")
        
        return extracted_info
    
    def process_batch(self,
                     image_dir: str,
                     annotation_dir: Optional[str] = None,
                     csv_file: Optional[str] = None,
                     output_file: str = "results.json",
                     save_visualization: bool = False,
                     output_dir: Optional[str] = None) -> List[Dict]:
        """
        Xử lý batch ảnh
        
        Args:
            image_dir: Thư mục chứa ảnh
            annotation_dir: Thư mục chứa annotation
            csv_file: File CSV annotation
            output_file: File lưu kết quả
            save_visualization: Có lưu visualization không
            output_dir: Thư mục lưu visualization
            
        Returns:
            List[Dict]: Danh sách kết quả
        """
        image_path = Path(image_dir)
        results = []
        
        # Lấy danh sách ảnh
        image_files = list(image_path.glob("*.jpg")) + \
                     list(image_path.glob("*.png")) + \
                     list(image_path.glob("*.jpeg"))
        
        logger.info(f"Tìm thấy {len(image_files)} ảnh")
        
        for i, img_file in enumerate(image_files, 1):
            logger.info(f"Đang xử lý ảnh {i}/{len(image_files)}: {img_file.name}")
            
            # Tìm annotation file tương ứng
            annotation_file = None
            if annotation_dir:
                ann_path = Path(annotation_dir) / f"{img_file.stem}.txt"
                if ann_path.exists():
                    annotation_file = str(ann_path)
            
            try:
                result = self.process_image(
                    str(img_file),
                    annotation_file=annotation_file,
                    csv_file=csv_file,
                    save_visualization=save_visualization,
                    output_dir=output_dir
                )
                
                result['image_file'] = img_file.name
                results.append(result)
            
            except Exception as e:
                logger.error(f"Lỗi khi xử lý {img_file}: {e}")
                results.append({
                    'image_file': img_file.name,
                    'error': str(e)
                })
        
        # Lưu kết quả
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(results, f, ensure_ascii=False, indent=2)
        
        logger.info(f"Đã lưu kết quả vào {output_file}")
        
        return results
    
    def export_to_json(self, data: Dict, output_path: str):
        """
        Xuất kết quả ra file JSON
        
        Args:
            data: Dữ liệu cần xuất
            output_path: Đường dẫn file output
        """
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        logger.info(f"Đã xuất JSON tại {output_path}")


def main():
    """Hàm main để chạy từ command line"""
    parser = argparse.ArgumentParser(description='OCR hóa đơn tiếng Việt')
    
    parser.add_argument('--image', type=str, help='Đường dẫn ảnh đầu vào')
    parser.add_argument('--image-dir', type=str, help='Thư mục chứa ảnh')
    parser.add_argument('--annotation', type=str, help='File annotation')
    parser.add_argument('--annotation-dir', type=str, help='Thư mục annotation')
    parser.add_argument('--csv', type=str, help='File CSV annotation')
    parser.add_argument('--yolo-model', type=str, help='Đường dẫn YOLO model')
    parser.add_argument('--use-yolo', action='store_true', help='Sử dụng YOLO')
    parser.add_argument('--gpu', action='store_true', help='Sử dụng GPU cho OCR')
    parser.add_argument('--output', type=str, default='result.json', help='File output')
    parser.add_argument('--output-dir', type=str, help='Thư mục lưu visualization')
    parser.add_argument('--visualize', action='store_true', help='Lưu visualization')
    
    args = parser.parse_args()
    
    # Khởi tạo pipeline
    pipeline = ReceiptOCRPipeline(
        yolo_model_path=args.yolo_model,
        use_gpu=args.gpu
    )
    
    # Xử lý
    if args.image:
        # Xử lý một ảnh
        result = pipeline.process_image(
            args.image,
            annotation_file=args.annotation,
            csv_file=args.csv,
            use_yolo=args.use_yolo,
            save_visualization=args.visualize,
            output_dir=args.output_dir
        )
        
        # Lưu kết quả
        pipeline.export_to_json(result, args.output)
        
        print("\n" + "="*50)
        print("KẾT QUẢ TRÍCH XUẤT:")
        print("="*50)
        print(json.dumps(result, ensure_ascii=False, indent=2))
    
    elif args.image_dir:
        # Xử lý batch
        results = pipeline.process_batch(
            args.image_dir,
            annotation_dir=args.annotation_dir,
            csv_file=args.csv,
            output_file=args.output,
            save_visualization=args.visualize,
            output_dir=args.output_dir
        )
        
        print(f"\nĐã xử lý {len(results)} ảnh")
        print(f"Kết quả đã lưu tại: {args.output}")
    
    else:
        parser.print_help()


if __name__ == "__main__":
    # Example usage
    print("="*60)
    print("HỆ THỐNG OCR HÓA ĐƠN TIẾNG VIỆT")
    print("="*60)
    print("\nUsage examples:")
    print("\n1. Xử lý một ảnh với annotation có sẵn:")
    print("   python main.py --image path/to/image.jpg --annotation path/to/annotation.txt --output result.json")
    print("\n2. Xử lý batch ảnh với CSV annotation:")
    print("   python main.py --image-dir path/to/images --csv path/to/annotations.csv --output results.json")
    print("\n3. Xử lý với YOLO model:")
    print("   python main.py --image path/to/image.jpg --yolo-model path/to/model.pt --use-yolo --output result.json")
    print("\n4. Xử lý với GPU và visualization:")
    print("   python main.py --image path/to/image.jpg --annotation path/to/annotation.txt --gpu --visualize --output-dir output/")
    print("\n" + "="*60)
    
    main()
