"""
Test nhanh với confidence threshold mới (0.15)
So sánh với kết quả cũ (0.25)
"""

from inference_easyocr import InvoiceOCR_EasyOCR
import json
from pathlib import Path

# Cấu hình
YOLO_MODEL = "d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt"
TEST_DIR = "d:/ORC_Service/SROIE2019/test/img"
ENTITIES_DIR = "d:/ORC_Service/SROIE2019/test/entities"

print("="*60)
print("TESTING IMPROVED YOLO (conf=0.15)")
print("="*60)

print("\nInitializing EasyOCR with conf=0.15...")

# Initialize OCR với threshold mới
ocr = InvoiceOCR_EasyOCR(
    yolo_model_path=YOLO_MODEL,
    device='cuda',
    yolo_conf=0.15,  # ĐÃ GIẢM từ 0.25
    yolo_iou=0.45,
    languages=['en']
)

# Test trên 50 ảnh đầu
image_files = sorted(list(Path(TEST_DIR).glob('*.jpg')))[:50]

print(f"\nTesting on {len(image_files)} images...")

stats = {
    'total_extracted': 0,
    'total_missing': 0,
    'total_correct': 0,
    'total_partial': 0,
    'date_extracted': 0,
    'company_extracted': 0
}

for img_file in image_files:
    img_name = img_file.stem
    
    # Process
    result = ocr.process_invoice(
        image_path=str(img_file),
        save_visualization=False,
        output_path=None
    )
    
    # Load ground truth
    gt_file = Path(ENTITIES_DIR) / f"{img_name}.txt"
    if gt_file.exists():
        with open(gt_file, 'r', encoding='utf-8') as f:
            gt = json.load(f)
        
        # Check total
        if result['total']:
            stats['total_extracted'] += 1
            
            pred_total = result['total'].replace(',', '').replace('.', '').strip()
            gt_total = gt['total'].replace(',', '').replace('.', '').strip()
            
            if pred_total == gt_total:
                stats['total_correct'] += 1
            elif pred_total in gt_total or gt_total in pred_total:
                stats['total_partial'] += 1
        else:
            stats['total_missing'] += 1
        
        if result['date']:
            stats['date_extracted'] += 1
        
        if result['company']:
            stats['company_extracted'] += 1

# Print results
print("\n" + "="*60)
print("RESULTS WITH conf=0.15")
print("="*60)

total_ext_rate = stats['total_extracted']/len(image_files)*100
total_miss_rate = stats['total_missing']/len(image_files)*100

print(f"Total extracted:  {stats['total_extracted']}/{len(image_files)} ({total_ext_rate:.1f}%)")
print(f"Total missing:    {stats['total_missing']}/{len(image_files)} ({total_miss_rate:.1f}%)")

if stats['total_extracted'] > 0:
    total_acc = stats['total_correct']/stats['total_extracted']*100
    total_acc_partial = (stats['total_correct']+stats['total_partial'])/stats['total_extracted']*100
    print(f"Total accuracy:   {stats['total_correct']}/{stats['total_extracted']} ({total_acc:.1f}%)")
    print(f"Total (w/ partial): {stats['total_correct']+stats['total_partial']}/{stats['total_extracted']} ({total_acc_partial:.1f}%)")

print(f"Date extracted:   {stats['date_extracted']}/{len(image_files)} ({stats['date_extracted']/len(image_files)*100:.1f}%)")
print(f"Company extracted: {stats['company_extracted']}/{len(image_files)} ({stats['company_extracted']/len(image_files)*100:.1f}%)")

# Compare with old results
print("\n" + "="*60)
print("COMPARISON")
print("="*60)

old_results = {
    'total_extracted': 38,  # 24% missing = 76% extracted từ analyze_yolo_errors
    'total_missing': 12,
    'conf_threshold': 0.25
}

new_total_extracted = stats['total_extracted']
improvement = new_total_extracted - old_results['total_extracted']

print(f"{'Metric':<25} {'Old (0.25)':<15} {'New (0.15)':<15} {'Change':<10}")
print("-"*70)
print(f"{'Total extracted':<25} {old_results['total_extracted']}/50 (76.0%) {new_total_extracted}/50 ({total_ext_rate:.1f}%)  {'+'+str(improvement) if improvement > 0 else str(improvement)}")
print(f"{'Total missing':<25} {old_results['total_missing']}/50 (24.0%) {stats['total_missing']}/50 ({total_miss_rate:.1f}%)  {'-'+str(old_results['total_missing']-stats['total_missing']) if old_results['total_missing']-stats['total_missing'] > 0 else str(old_results['total_missing']-stats['total_missing'])}")

improvement_pct = (improvement / old_results['total_extracted']) * 100 if old_results['total_extracted'] > 0 else 0

print(f"\n✓ Improvement: {improvement_pct:+.1f}% detection rate")

if improvement > 0:
    print("\n🎉 SUCCESS! Giảm threshold đã cải thiện detection!")
    print("\nRecommendation:")
    print("  - Chạy full evaluation: python evaluate_easyocr_full.py")
    print("  - Expected total extraction: ~85%+ (từ 71.8%)")
else:
    print("\n⚠️  Cần xem xét thêm options khác")
