"""
Test nhanh EasyOCR trên 1 ảnh
"""

from inference_easyocr import InvoiceOCR_EasyOCR
import json

# Cấu hình
YOLO_MODEL = "d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt"
TEST_IMAGE = "d:/ORC_Service/SROIE2019/test/img/X00016469670.jpg"
OUTPUT_VIS = "d:/ORC_Service/result_easyocr_demo.jpg"

print("Initializing EasyOCR system...")

# Initialize OCR
ocr = InvoiceOCR_EasyOCR(
    yolo_model_path=YOLO_MODEL,
    device='cuda',
    yolo_conf=0.25,
    yolo_iou=0.45,
    languages=['en']
)

print("\nProcessing image...")

# Process
result = ocr.process_invoice(
    image_path=TEST_IMAGE,
    save_visualization=True,
    output_path=OUTPUT_VIS
)

# Print results
print("\n" + "="*60)
print("RESULTS")
print("="*60)
print(f"Company: {result['company']}")
print(f"Date: {result['date']}")
print(f"Total: {result['total']}")
print(f"Address: {result['address']}")
print("\nDetailed:")
print(json.dumps(result, indent=2, ensure_ascii=False))

print(f"\n✓ Visualization saved to: {OUTPUT_VIS}")
