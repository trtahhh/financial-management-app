"""
Phân tích lỗi detection của YOLO để xác định vấn đề
"""

import json
from pathlib import Path
from ultralytics import YOLO
import cv2
from collections import defaultdict

# Load model
model = YOLO("d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt")

# Load ground truth
entities_dir = Path("d:/ORC_Service/SROIE2019/test/entities")
test_img_dir = Path("d:/ORC_Service/SROIE2019/test/img")

# Class mapping
CLASS_NAMES = {0: 'company', 1: 'date', 2: 'total', 3: 'address'}

# Statistics
stats = {
    'total_images': 0,
    'missing_total': [],
    'missing_date': [],
    'missing_company': [],
    'missing_address': [],
    'low_conf_total': [],  # Detected but confidence < 0.5
    'class_distribution': defaultdict(int)
}

print("Analyzing YOLO detection errors...")
print("Processing test images...\n")

# Sample 50 images để phân tích nhanh
test_images = sorted(list(test_img_dir.glob('*.jpg')))[:50]

for img_file in test_images:
    stats['total_images'] += 1
    img_name = img_file.stem
    
    # Load ground truth
    gt_file = entities_dir / f"{img_name}.txt"
    if not gt_file.exists():
        continue
    
    with open(gt_file, 'r', encoding='utf-8') as f:
        gt = json.load(f)
    
    # Run detection
    results = model.predict(source=str(img_file), conf=0.25, verbose=False)
    
    # Get detected classes
    detected_classes = set()
    low_conf_classes = {}
    
    if len(results) > 0 and results[0].boxes is not None:
        for cls, conf in zip(results[0].boxes.cls.cpu().numpy(), 
                            results[0].boxes.conf.cpu().numpy()):
            class_id = int(cls)
            class_name = CLASS_NAMES.get(class_id, f'class_{class_id}')
            detected_classes.add(class_name)
            stats['class_distribution'][class_name] += 1
            
            if conf < 0.5:
                low_conf_classes[class_name] = float(conf)
    
    # Check missing fields
    if gt.get('total') and 'total' not in detected_classes:
        stats['missing_total'].append({
            'image': img_name,
            'gt_total': gt['total'],
            'detected': list(detected_classes)
        })
    
    if gt.get('date') and 'date' not in detected_classes:
        stats['missing_date'].append(img_name)
    
    if gt.get('company') and 'company' not in detected_classes:
        stats['missing_company'].append(img_name)
    
    if gt.get('address') and 'address' not in detected_classes:
        stats['missing_address'].append(img_name)
    
    # Low confidence total
    if 'total' in low_conf_classes:
        stats['low_conf_total'].append({
            'image': img_name,
            'confidence': low_conf_classes['total'],
            'gt_total': gt.get('total')
        })

# Print analysis
print("="*60)
print("YOLO DETECTION ERROR ANALYSIS")
print("="*60)
print(f"Analyzed images: {stats['total_images']}")
print()

print("Missing Detection Rate:")
print(f"  Total:   {len(stats['missing_total'])}/{stats['total_images']} ({len(stats['missing_total'])/stats['total_images']*100:.1f}%)")
print(f"  Date:    {len(stats['missing_date'])}/{stats['total_images']} ({len(stats['missing_date'])/stats['total_images']*100:.1f}%)")
print(f"  Company: {len(stats['missing_company'])}/{stats['total_images']} ({len(stats['missing_company'])/stats['total_images']*100:.1f}%)")
print(f"  Address: {len(stats['missing_address'])}/{stats['total_images']} ({len(stats['missing_address'])/stats['total_images']*100:.1f}%)")
print()

print("Class Distribution (detections):")
for cls, count in sorted(stats['class_distribution'].items()):
    print(f"  {cls}: {count}")
print()

print("Low Confidence Total Detections:")
print(f"  Count: {len(stats['low_conf_total'])}")
if stats['low_conf_total']:
    print("  Examples:")
    for item in stats['low_conf_total'][:5]:
        print(f"    {item['image']}: conf={item['confidence']:.3f}, gt={item['gt_total']}")
print()

print("Examples of Missing Total Detection:")
for item in stats['missing_total'][:10]:
    print(f"  {item['image']}: GT={item['gt_total']}, Detected={item['detected']}")

# Save detailed report
with open('d:/ORC_Service/yolo_error_analysis.json', 'w', encoding='utf-8') as f:
    json.dump(stats, f, indent=2, ensure_ascii=False)

print(f"\n✓ Detailed report saved to: d:/ORC_Service/yolo_error_analysis.json")

# Recommendations
print("\n" + "="*60)
print("RECOMMENDATIONS FOR IMPROVEMENT")
print("="*60)

missing_rate = len(stats['missing_total'])/stats['total_images']*100

if missing_rate > 20:
    print("1. TRAIN LONGER:")
    print("   - Current: 44 epochs with mAP50=70.81%")
    print("   - Recommend: Continue training to 100 epochs")
    print("   - Expected improvement: +5-10% mAP")
    print()

if len(stats['low_conf_total']) > 5:
    print("2. LOWER CONFIDENCE THRESHOLD:")
    print(f"   - Current threshold: 0.25")
    print(f"   - Found {len(stats['low_conf_total'])} low-conf total detections")
    print(f"   - Try threshold: 0.15-0.20")
    print()

print("3. DATA AUGMENTATION:")
print("   - Add more aggressive augmentation for 'total' class")
print("   - Use copy-paste augmentation")
print("   - Add synthetic total fields")
print()

print("4. MODEL SIZE:")
print("   - Current: YOLOv8n (nano)")
print("   - Try: YOLOv8s (small) if GPU allows")
print("   - Expected improvement: +3-5% mAP")
print()

print("5. CLASS BALANCE:")
print("   - Check if 'total' class is underrepresented")
print("   - Add class weights to loss function")
