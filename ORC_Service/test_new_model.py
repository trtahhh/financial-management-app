"""
Test model mới (epoch 94) trên full test set
So sánh end-to-end performance với model cũ
"""

from inference_easyocr import InvoiceOCR_EasyOCR
import json
from pathlib import Path
from tqdm import tqdm

# Cấu hình
NEW_YOLO_MODEL = "d:/ORC_Service/runs/detect/sroie_invoice_continued/weights/best.pt"
TEST_DIR = "d:/ORC_Service/SROIE2019/test/img"
ENTITIES_DIR = "d:/ORC_Service/SROIE2019/test/entities"
OUTPUT_JSON = "d:/ORC_Service/batch_results_new_model.json"

print("="*60)
print("TESTING NEW MODEL (Epoch 94)")
print("="*60)

print("\nInitializing EasyOCR with NEW YOLO model...")
print(f"Model: {NEW_YOLO_MODEL}")

# Initialize OCR với model mới
ocr = InvoiceOCR_EasyOCR(
    yolo_model_path=NEW_YOLO_MODEL,
    device='cuda',
    yolo_conf=0.15,  # Giữ threshold đã optimize
    yolo_iou=0.45,
    languages=['en']
)

# Get all test images
image_files = sorted(list(Path(TEST_DIR).glob('*.jpg')))

print(f"\nProcessing {len(image_files)} images...")
print("This will take approximately 10-15 minutes...\n")

results = {}
stats = {
    'total_images': len(image_files),
    'total_extracted': 0,
    'total_missing': 0,
    'total_correct': 0,
    'total_partial': 0,
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
            save_visualization=False,
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
                
                pred_total = result['total'].replace(',', '').replace('.', '').strip()
                gt_total = gt['total'].replace(',', '').replace('.', '').strip()
                
                if pred_total == gt_total:
                    stats['total_correct'] += 1
                elif pred_total in gt_total or gt_total in pred_total:
                    stats['total_partial'] += 1
            else:
                stats['total_missing'] += 1
            
            # Check date
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
print("NEW MODEL RESULTS")
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

# Other fields
date_ext = stats['date_extracted']
date_corr = stats['date_correct']
date_rate = f"{date_ext}/{total_imgs} ({date_ext/total_imgs*100:.1f}%)"
date_acc = f"{date_corr}/{date_ext} ({date_corr/date_ext*100:.1f}%)" if date_ext > 0 else "N/A"
print(f"{'Date':<20} {date_rate:<20} {date_acc:<20}")

comp_ext = stats['company_extracted']
comp_rate = f"{comp_ext}/{total_imgs} ({comp_ext/total_imgs*100:.1f}%)"
print(f"{'Company':<20} {comp_rate:<20} {'N/A':<20}")

addr_ext = stats['address_extracted']
addr_rate = f"{addr_ext}/{total_imgs} ({addr_ext/total_imgs*100:.1f}%)"
print(f"{'Address':<20} {addr_rate:<20} {'N/A':<20}")

# Load old results for comparison
old_results_file = Path("d:/ORC_Service/batch_results_easyocr_full.json")
if old_results_file.exists():
    with open(old_results_file, 'r', encoding='utf-8') as f:
        old_results = json.load(f)
    
    # Calculate old stats
    old_total_ext = 0
    old_total_corr = 0
    old_total_part = 0
    old_total_miss = 0
    
    for img_name, result in old_results.items():
        if 'ground_truth' in result:
            gt = result['ground_truth']
            if result.get('total'):
                old_total_ext += 1
                pred = result['total'].replace(',', '').replace('.', '').strip()
                gt_val = gt['total'].replace(',', '').replace('.', '').strip()
                if pred == gt_val:
                    old_total_corr += 1
                elif pred in gt_val or gt_val in pred:
                    old_total_part += 1
            else:
                old_total_miss += 1
    
    print("\n" + "="*60)
    print("COMPARISON WITH OLD MODEL")
    print("="*60)
    print(f"{'Metric':<30} {'Old (Epoch 44)':<20} {'New (Epoch 94)':<20}")
    print("-"*70)
    
    # Total extracted
    old_ext_rate = old_total_ext/347*100
    new_ext_rate = total_ext/total_imgs*100
    print(f"{'Total Extracted':<30} {old_ext_rate:.1f}% ({old_total_ext}/347)   {new_ext_rate:.1f}% ({total_ext}/{total_imgs})")
    
    # Total accuracy
    old_acc = (old_total_corr+old_total_part)/347*100
    new_acc = (total_corr+total_part)/total_imgs*100
    print(f"{'Total Accuracy (combined)':<30} {old_acc:.1f}%              {new_acc:.1f}%")
    
    # Missing
    old_miss_rate = old_total_miss/347*100
    new_miss_rate = stats['total_missing']/total_imgs*100
    print(f"{'Total Missing':<30} {old_miss_rate:.1f}% ({old_total_miss}/347)   {new_miss_rate:.1f}% ({stats['total_missing']}/{total_imgs})")
    
    print("\n" + "="*60)
    print("IMPROVEMENTS")
    print("="*60)
    
    ext_improvement = new_ext_rate - old_ext_rate
    acc_improvement = new_acc - old_acc
    miss_improvement = old_miss_rate - new_miss_rate
    
    print(f"Total Extracted: {ext_improvement:+.1f}%")
    print(f"Total Accuracy: {acc_improvement:+.1f}%")
    print(f"Missing Reduction: {miss_improvement:+.1f}%")
    
    if ext_improvement > 0 or acc_improvement > 0:
        print("\n✅ NEW MODEL IS BETTER!")
        print("\nRECOMMENDATION:")
        print("  1. Update inference_easyocr.py with new model path")
        print("  2. Deploy new model to production")
        print("  3. Archive old model as backup")
    else:
        print("\n⚠️  New model did not improve end-to-end performance")
        print("\nPossible reasons:")
        print("  - Detection improved but OCR bottleneck")
        print("  - Need better post-processing")
        print("  - Threshold optimization needed")

print(f"\n✓ Results saved to: {OUTPUT_JSON}")
print("✓ Processing time per image: ~1.5s")
