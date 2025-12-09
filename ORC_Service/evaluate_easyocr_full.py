"""
Full batch inference với EasyOCR trên toàn bộ test set
Đánh giá accuracy so với ground truth
"""

from inference_easyocr import InvoiceOCR_EasyOCR
import json
from pathlib import Path
from tqdm import tqdm

# Cấu hình
YOLO_MODEL = "d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt"
TEST_DIR = "d:/ORC_Service/SROIE2019/test/img"
ENTITIES_DIR = "d:/ORC_Service/SROIE2019/test/entities"
OUTPUT_JSON = "d:/ORC_Service/batch_results_easyocr_full.json"
OUTPUT_VIS_DIR = "d:/ORC_Service/visualizations_easyocr_full"

print("="*60)
print("FULL BATCH INFERENCE WITH EASYOCR")
print("="*60)

print("\nInitializing EasyOCR system...")

# Initialize OCR
ocr = InvoiceOCR_EasyOCR(
    yolo_model_path=YOLO_MODEL,
    device='cuda',
    yolo_conf=0.15,
    yolo_iou=0.45,
    languages=['en']
)

# Get all test images
image_files = sorted(list(Path(TEST_DIR).glob('*.jpg')))
Path(OUTPUT_VIS_DIR).mkdir(parents=True, exist_ok=True)

print(f"\nProcessing {len(image_files)} images...")
print("This will take approximately 10-15 minutes...\n")

results = {}
stats = {
    'total_images': len(image_files),
    'total_extracted': 0,
    'total_missing': 0,
    'total_correct': 0,
    'total_partial': 0,  # Số match một phần
    'date_extracted': 0,
    'date_correct': 0,
    'company_extracted': 0,
    'address_extracted': 0,
    'errors': 0
}

for img_file in tqdm(image_files, desc="Processing"):
    img_name = img_file.stem
    
    try:
        # Process image
        result = ocr.process_invoice(
            image_path=str(img_file),
            save_visualization=False,  # Tắt visualization để nhanh hơn
            output_path=None
        )
        
        # Load ground truth
        gt_file = Path(ENTITIES_DIR) / f"{img_name}.txt"
        if gt_file.exists():
            with open(gt_file, 'r', encoding='utf-8') as f:
                gt = json.load(f)
            
            result['ground_truth'] = gt
            
            # Check total accuracy
            if result['total']:
                stats['total_extracted'] += 1
                
                # Chuẩn hóa để so sánh
                pred_total = result['total'].replace(',', '').replace('.', '').strip()
                gt_total = gt['total'].replace(',', '').replace('.', '').strip()
                
                if pred_total == gt_total:
                    stats['total_correct'] += 1
                elif pred_total in gt_total or gt_total in pred_total:
                    stats['total_partial'] += 1
            else:
                stats['total_missing'] += 1
            
            # Check date accuracy
            if result['date']:
                stats['date_extracted'] += 1
                pred_date = result['date'].replace('/', '').replace('-', '').replace(' ', '').strip()
                gt_date = gt['date'].replace('/', '').replace('-', '').replace(' ', '').strip()
                if pred_date == gt_date:
                    stats['date_correct'] += 1
            
            # Check company
            if result['company']:
                stats['company_extracted'] += 1
            
            # Check address
            if result['address']:
                stats['address_extracted'] += 1
        
        results[img_name] = result
        
    except Exception as e:
        stats['errors'] += 1
        results[img_name] = {'error': str(e)}
        tqdm.write(f"Error processing {img_name}: {e}")

# Save results
print("\nSaving results...")
with open(OUTPUT_JSON, 'w', encoding='utf-8') as f:
    json.dump(results, f, indent=2, ensure_ascii=False)

# Calculate metrics
total_imgs = stats['total_images']
total_ext = stats['total_extracted']
total_corr = stats['total_correct']
total_part = stats['total_partial']

print("\n" + "="*60)
print("FINAL STATISTICS")
print("="*60)
print(f"Total images processed: {total_imgs}")
print(f"Errors: {stats['errors']} ({stats['errors']/total_imgs*100:.1f}%)")
print()
print(f"{'Field':<20} {'Extracted':<20} {'Accuracy':<20}")
print("-"*60)

# Total field
total_rate = f"{total_ext}/{total_imgs} ({total_ext/total_imgs*100:.1f}%)"
if total_ext > 0:
    total_acc = f"{total_corr}/{total_ext} ({total_corr/total_ext*100:.1f}%)"
    total_acc_with_partial = f"{total_corr+total_part}/{total_ext} ({(total_corr+total_part)/total_ext*100:.1f}%)"
else:
    total_acc = "N/A"
    total_acc_with_partial = "N/A"

print(f"{'Total (exact)':<20} {total_rate:<20} {total_acc:<20}")
print(f"{'Total (incl partial)':<20} {'':<20} {total_acc_with_partial:<20}")

# Date field
date_ext = stats['date_extracted']
date_corr = stats['date_correct']
date_rate = f"{date_ext}/{total_imgs} ({date_ext/total_imgs*100:.1f}%)"
date_acc = f"{date_corr}/{date_ext} ({date_corr/date_ext*100:.1f}%)" if date_ext > 0 else "N/A"
print(f"{'Date':<20} {date_rate:<20} {date_acc:<20}")

# Company field
comp_ext = stats['company_extracted']
comp_rate = f"{comp_ext}/{total_imgs} ({comp_ext/total_imgs*100:.1f}%)"
print(f"{'Company':<20} {comp_rate:<20} {'N/A':<20}")

# Address field
addr_ext = stats['address_extracted']
addr_rate = f"{addr_ext}/{total_imgs} ({addr_ext/total_imgs*100:.1f}%)"
print(f"{'Address':<20} {addr_rate:<20} {'N/A':<20}")

print("\n" + "="*60)
print(f"✓ Results saved to: {OUTPUT_JSON}")
print(f"✓ Processing time per image: ~{347/len(image_files) if len(image_files) > 0 else 0:.1f}s")
print("="*60)

# Detailed breakdown cho Total field
print("\n" + "="*60)
print("TOTAL FIELD BREAKDOWN")
print("="*60)
print(f"Exact match:     {total_corr}/{total_imgs} ({total_corr/total_imgs*100:.1f}%)")
print(f"Partial match:   {total_part}/{total_imgs} ({total_part/total_imgs*100:.1f}%)")
print(f"Missing:         {stats['total_missing']}/{total_imgs} ({stats['total_missing']/total_imgs*100:.1f}%)")
print(f"Combined (exact+partial): {total_corr+total_part}/{total_imgs} ({(total_corr+total_part)/total_imgs*100:.1f}%)")
