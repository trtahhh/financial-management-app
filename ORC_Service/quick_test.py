"""
Quick test script - Test improved OCR system
"""

from inference_easyocr import InvoiceOCR_EasyOCR
import json
from pathlib import Path

# Test image
test_image = "SROIE2019/test/img/X00016469670.jpg"

print("="*70)
print("QUICK TEST - OCR SYSTEM (Model Improved - Epoch 94)")
print("="*70)
print(f"\nTest image: {test_image}")

# Initialize OCR with improved model
print("\n⏳ Loading models...")
ocr = InvoiceOCR_EasyOCR(
    yolo_model_path='runs/detect/sroie_invoice_continued/weights/best.pt',
    device='cuda',
    yolo_conf=0.15,
    yolo_iou=0.45,
    languages=['en']
)
print("✓ Models loaded!")

# Process
print("\n⏳ Processing invoice...")
result = ocr.process_invoice(
    test_image,
    save_visualization=True,
    output_path='quick_test_result.jpg'
)

# Display results
print("\n" + "="*70)
print("EXTRACTED INFORMATION")
print("="*70)
print(f"\nCompany : {result['company'] if result['company'] else 'N/A'}")
print(f"Date    : {result['date'] if result['date'] else 'N/A'}")
print(f"Total   : {result['total'] if result['total'] else 'N/A'}")
print(f"Address : {result['address'][:50] if result['address'] else 'N/A'}")

print(f"\nDetections: {len(result['detections'])}")
for i, det in enumerate(result['detections'], 1):
    print(f"  {i}. {det['class_name']:<10} conf={det['confidence']:.3f}  text=\"{det['text'][:30]}\"")

# Load ground truth
entities_file = Path("SROIE2019/test/entities") / (Path(test_image).stem + ".txt")
if entities_file.exists():
    with open(entities_file, 'r', encoding='utf-8') as f:
        gt = json.load(f)
    
    print("\n" + "="*70)
    print("GROUND TRUTH COMPARISON")
    print("="*70)
    
    # Compare
    comp_match = result['company'] and result['company'].lower() in gt['company'].lower()
    
    pred_date = result['date'].replace('/', '').replace('-', '') if result['date'] else ''
    gt_date = gt['date'].replace('/', '').replace('-', '')
    date_match = pred_date == gt_date
    
    pred_total = result['total'].replace(',', '').replace('.', '') if result['total'] else ''
    gt_total = gt['total'].replace(',', '').replace('.', '')
    total_match = pred_total and (pred_total == gt_total or pred_total in gt_total or gt_total in pred_total)
    
    addr_match = bool(result['address'] and result['address'][:10].lower() in gt['address'].lower())
    
    print(f"\nCompany : {'✓ CORRECT' if comp_match else '✗ WRONG'}")
    print(f"  Predicted : {result['company']}")
    print(f"  GT        : {gt['company']}")
    
    print(f"\nDate    : {'✓ CORRECT' if date_match else '✗ WRONG'}")
    print(f"  Predicted : {result['date']}")
    print(f"  GT        : {gt['date']}")
    
    print(f"\nTotal   : {'✓ CORRECT' if total_match else '✗ WRONG'}")
    print(f"  Predicted : {result['total']}")
    print(f"  GT        : {gt['total']}")
    
    print(f"\nAddress : {'✓ CORRECT' if addr_match else '✗ WRONG'}")
    print(f"  Predicted : {result['address'][:50] if result['address'] else 'N/A'}")
    print(f"  GT        : {gt['address'][:50]}")
    
    matches = sum([int(comp_match), int(date_match), int(total_match), int(addr_match)])
    print(f"\n{'='*70}")
    print(f"Accuracy: {matches}/4 fields ({matches/4*100:.1f}%)")

print("\n" + "="*70)
print("OUTPUT")
print("="*70)
print(f"\n✓ Visualization saved: quick_test_result.jpg")
print("\n✓ Test completed!")
print("\nĐể test ảnh khác:")
print("  python quick_test.py")
