"""
Batch test EasyOCR trên 10 ảnh đầu tiên
"""

from inference_easyocr import InvoiceOCR_EasyOCR
import json
from pathlib import Path

# Cấu hình
YOLO_MODEL = "d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt"
TEST_DIR = "d:/ORC_Service/SROIE2019/test/img"
ENTITIES_DIR = "d:/ORC_Service/SROIE2019/test/entities"
OUTPUT_JSON = "d:/ORC_Service/batch_easyocr_sample.json"
OUTPUT_VIS_DIR = "d:/ORC_Service/visualizations_easyocr_sample"

print("Initializing EasyOCR system...")

# Initialize OCR
ocr = InvoiceOCR_EasyOCR(
    yolo_model_path=YOLO_MODEL,
    device='cuda',
    yolo_conf=0.25,
    yolo_iou=0.45,
    languages=['en']
)

# Get 10 test images
image_files = sorted(list(Path(TEST_DIR).glob('*.jpg')))[:10]
Path(OUTPUT_VIS_DIR).mkdir(parents=True, exist_ok=True)

print(f"\nProcessing {len(image_files)} images...")

results = {}
stats = {
    'total_extracted': 0,
    'total_missing': 0,
    'total_correct': 0,
    'date_correct': 0,
    'company_extracted': 0
}

for img_file in image_files:
    img_name = img_file.stem
    print(f"\nProcessing {img_name}...")
    
    # Process image
    result = ocr.process_invoice(
        image_path=str(img_file),
        save_visualization=True,
        output_path=str(Path(OUTPUT_VIS_DIR) / f"{img_name}_result.jpg")
    )
    
    # Load ground truth
    gt_file = Path(ENTITIES_DIR) / f"{img_name}.txt"
    if gt_file.exists():
        with open(gt_file, 'r', encoding='utf-8') as f:
            gt = json.load(f)
        
        result['ground_truth'] = gt
        
        # Check accuracy
        if result['total']:
            stats['total_extracted'] += 1
            # So sánh (bỏ qua dấu chấm phẩy)
            pred_total = result['total'].replace(',', '').replace('.', '')
            gt_total = gt['total'].replace(',', '').replace('.', '')
            if pred_total in gt_total or gt_total in pred_total:
                stats['total_correct'] += 1
        else:
            stats['total_missing'] += 1
        
        if result['date']:
            pred_date = result['date'].replace('/', '').replace('-', '')
            gt_date = gt['date'].replace('/', '').replace('-', '')
            if pred_date == gt_date:
                stats['date_correct'] += 1
        
        if result['company']:
            stats['company_extracted'] += 1
    
    results[img_name] = result
    
    # Print quick result
    print(f"  Company: {result['company'][:40] if result['company'] else 'N/A'}")
    print(f"  Date: {result['date']}")
    print(f"  Total: {result['total']}")
    if 'ground_truth' in result:
        print(f"  GT Total: {result['ground_truth']['total']}")

# Save results
with open(OUTPUT_JSON, 'w', encoding='utf-8') as f:
    json.dump({'results': results, 'statistics': stats}, f, indent=2, ensure_ascii=False)

# Print statistics
print("\n" + "="*60)
print("STATISTICS")
print("="*60)
print(f"Total extracted: {stats['total_extracted']}/{len(image_files)} ({stats['total_extracted']/len(image_files)*100:.1f}%)")
print(f"Total correct: {stats['total_correct']}/{stats['total_extracted']} ({stats['total_correct']/stats['total_extracted']*100:.1f}% of extracted)" if stats['total_extracted'] > 0 else "Total correct: 0/0")
print(f"Total missing: {stats['total_missing']}/{len(image_files)}")
print(f"Date correct: {stats['date_correct']}/{len(image_files)} ({stats['date_correct']/len(image_files)*100:.1f}%)")
print(f"Company extracted: {stats['company_extracted']}/{len(image_files)} ({stats['company_extracted']/len(image_files)*100:.1f}%)")

print(f"\n✓ Results saved to: {OUTPUT_JSON}")
print(f"✓ Visualizations in: {OUTPUT_VIS_DIR}")
