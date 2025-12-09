"""
Script demo nhanh để test OCR trên 1 ảnh
"""

import sys
import json
from pathlib import Path

# Import inference module
try:
    from inference import InvoiceOCR
except ImportError:
    print("Error: Cannot import inference module")
    print("Make sure all required files are in the same directory")
    sys.exit(1)

def demo_single_image(
    image_path,
    yolo_model='runs/detect/sroie_invoice/weights/best.pt',
    crnn_model='crnn_models/best_crnn.pt',
    output_dir='demo_output'
):
    """
    Demo OCR trên 1 ảnh
    
    Args:
        image_path: Đường dẫn đến ảnh
        yolo_model: Đường dẫn đến YOLO model
        crnn_model: Đường dẫn đến CRNN model
        output_dir: Thư mục lưu kết quả
    """
    
    # Kiểm tra file tồn tại
    if not Path(image_path).exists():
        print(f"❌ Error: Image not found: {image_path}")
        return
    
    if not Path(yolo_model).exists():
        print(f"❌ Error: YOLO model not found: {yolo_model}")
        print("Please train the model first or provide correct path")
        return
    
    if not Path(crnn_model).exists():
        print(f"❌ Error: CRNN model not found: {crnn_model}")
        print("Please train the model first or provide correct path")
        return
    
    # Tạo output directory
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    
    print("\n" + "="*60)
    print("INVOICE OCR DEMO")
    print("="*60)
    print(f"Image: {image_path}")
    print(f"YOLO model: {yolo_model}")
    print(f"CRNN model: {crnn_model}")
    print("="*60 + "\n")
    
    # Khởi tạo OCR system
    print("Loading models...")
    try:
        ocr = InvoiceOCR(
            yolo_model_path=yolo_model,
            crnn_model_path=crnn_model,
            device='cuda',
            yolo_conf=0.25,
            yolo_iou=0.45,
            crnn_img_height=32
        )
    except Exception as e:
        print(f"❌ Error loading models: {e}")
        return
    
    print("✓ Models loaded successfully!\n")
    
    # Process image
    print("Processing image...")
    try:
        img_name = Path(image_path).stem
        output_vis = output_path / f"{img_name}_result.jpg"
        output_json = output_path / f"{img_name}_result.json"
        
        result = ocr.process_invoice(
            image_path=image_path,
            save_visualization=True,
            output_path=str(output_vis)
        )
        
        # Save JSON
        with open(output_json, 'w', encoding='utf-8') as f:
            json.dump(result, f, indent=2, ensure_ascii=False)
        
    except Exception as e:
        print(f"❌ Error processing image: {e}")
        return
    
    print("✓ Processing completed!\n")
    
    # Display results
    print("="*60)
    print("EXTRACTED INFORMATION")
    print("="*60)
    print(f"Company : {result.get('company', 'N/A')}")
    print(f"Date    : {result.get('date', 'N/A')}")
    print(f"Total   : {result.get('total', 'N/A')}")
    print(f"Address : {result.get('address', 'N/A')}")
    print("="*60)
    
    print(f"\nDetections: {len(result.get('detections', []))}")
    for i, det in enumerate(result.get('detections', []), 1):
        print(f"  {i}. {det['class_name']}: \"{det['text']}\" (conf: {det['confidence']:.2f})")
    
    print("\n" + "="*60)
    print("OUTPUT FILES")
    print("="*60)
    print(f"✓ Visualization: {output_vis}")
    print(f"✓ JSON result: {output_json}")
    print("="*60)

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description='Demo Invoice OCR on single image')
    parser.add_argument('image', type=str, help='Path to invoice image')
    parser.add_argument('--yolo-model', type=str, 
                        default='runs/detect/sroie_invoice/weights/best.pt',
                        help='Path to YOLO model')
    parser.add_argument('--crnn-model', type=str,
                        default='crnn_models/best_crnn.pt',
                        help='Path to CRNN model')
    parser.add_argument('--output-dir', type=str,
                        default='demo_output',
                        help='Output directory for results')
    
    args = parser.parse_args()
    
    demo_single_image(
        image_path=args.image,
        yolo_model=args.yolo_model,
        crnn_model=args.crnn_model,
        output_dir=args.output_dir
    )

if __name__ == "__main__":
    # Example usage
    if len(sys.argv) == 1:
        print("Usage:")
        print("  python demo.py <image_path> [--yolo-model PATH] [--crnn-model PATH]")
        print("\nExample:")
        print("  python demo.py SROIE2019/test/img/X00016469670.jpg")
        print("\nFor help:")
        print("  python demo.py --help")
        sys.exit(0)
    
    main()
