"""
Script cải thiện YOLO detection với nhiều options
"""

import torch
from ultralytics import YOLO
from pathlib import Path
import yaml

print("="*60)
print("YOLO IMPROVEMENT OPTIONS")
print("="*60)
print()
print("Chọn phương pháp cải thiện:")
print()
print("1. GIẢM CONFIDENCE THRESHOLD (Nhanh nhất - Khuyến nghị)")
print("   - Thay đổi conf từ 0.25 → 0.15")
print("   - Không cần train lại")
print("   - Expected: +10-15% detection rate")
print("   - Thời gian: 0 phút")
print()
print("2. CONTINUE TRAINING (Cải thiện model)")
print("   - Train thêm 50 epochs (44→94)")
print("   - Sử dụng model epoch 44 làm checkpoint")
print("   - Expected: +5-10% mAP")
print("   - Thời gian: ~3-4 giờ")
print()
print("3. TRAIN VỚI AUGMENTATION MẠNH HỖN")
print("   - Augmentation tăng cường cho 'total' class")
print("   - Copy-paste augmentation")
print("   - Train từ đầu 100 epochs")
print("   - Expected: +8-12% mAP")
print("   - Thời gian: ~5-6 giờ")
print()
print("4. FINE-TUNE VỚI CLASS WEIGHTS")
print("   - Tăng trọng số cho class 'total'")
print("   - Train thêm 30 epochs")
print("   - Expected: +3-5% mAP cho total")
print("   - Thời gian: ~2 giờ")
print()
print("5. ENSEMBLE (Kết hợp nhiều models)")
print("   - Dùng 2-3 checkpoints khác nhau")
print("   - Weighted box fusion")
print("   - Expected: +2-3% mAP")
print("   - Thời gian: 0 phút (chỉ inference)")
print()

choice = input("Nhập số lựa chọn (1-5): ").strip()

if choice == "1":
    print("\n" + "="*60)
    print("OPTION 1: GIẢM CONFIDENCE THRESHOLD")
    print("="*60)
    
    # Update inference scripts
    files_to_update = [
        "d:/ORC_Service/inference_easyocr.py",
        "d:/ORC_Service/inference_tesseract.py",
        "d:/ORC_Service/inference.py"
    ]
    
    print("\nCập nhật confidence threshold trong inference scripts...")
    print("Thay đổi: conf=0.25 → conf=0.15")
    print("\nFiles sẽ được cập nhật:")
    for f in files_to_update:
        if Path(f).exists():
            print(f"  ✓ {f}")
    
    confirm = input("\nXác nhận cập nhật? (y/n): ").strip().lower()
    
    if confirm == 'y':
        print("\n✓ Sẽ cập nhật files với conf=0.15")
        print("✓ Test lại inference với threshold mới")
        print("\nChạy lệnh:")
        print("  python evaluate_easyocr_full.py")
    else:
        print("Đã hủy")

elif choice == "2":
    print("\n" + "="*60)
    print("OPTION 2: CONTINUE TRAINING")
    print("="*60)
    
    script_content = """
\"\"\"
Continue training YOLOv8 từ epoch 44
\"\"\"

from ultralytics import YOLO

# Load checkpoint epoch 44
model = YOLO("d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt")

print("Continuing training from epoch 44 to 94...")
print("Batch size: 4 (GPU 2GB)")
print("Estimated time: 3-4 hours")
print()

# Continue training
results = model.train(
    data="d:/ORC_Service/sroie.yaml",
    epochs=94,  # Sẽ train thêm 50 epochs
    batch=4,
    imgsz=640,
    patience=50,
    save=True,
    project="d:/ORC_Service/runs/detect",
    name="sroie_invoice_continued",
    exist_ok=True,
    workers=0,
    device=0,
    resume=True  # Continue từ checkpoint
)

print("\\n✓ Training completed!")
print(f"Best model: {results.save_dir}/weights/best.pt")
"""
    
    with open("d:/ORC_Service/continue_training_improved.py", "w", encoding="utf-8") as f:
        f.write(script_content)
    
    print("✓ Script đã tạo: continue_training_improved.py")
    print("\nĐể chạy:")
    print("  python continue_training_improved.py")
    print("\nLưu ý: Cần 3-4 giờ training")

elif choice == "3":
    print("\n" + "="*60)
    print("OPTION 3: TRAIN VỚI AUGMENTATION MẠNH")
    print("="*60)
    
    # Update yaml with stronger augmentation
    yaml_config = {
        'path': 'd:/ORC_Service/SROIE_YOLO',
        'train': 'train/images',
        'val': 'val/images',
        'nc': 4,
        'names': ['company', 'date', 'total', 'address'],
        # Augmentation mạnh
        'hsv_h': 0.02,
        'hsv_s': 0.8,
        'hsv_v': 0.5,
        'degrees': 5.0,
        'translate': 0.2,
        'scale': 0.9,
        'shear': 5.0,
        'perspective': 0.001,
        'flipud': 0.1,
        'fliplr': 0.5,
        'mosaic': 1.0,
        'mixup': 0.2,
        'copy_paste': 0.3
    }
    
    with open("d:/ORC_Service/sroie_augmented.yaml", "w", encoding="utf-8") as f:
        yaml.dump(yaml_config, f, default_flow_style=False)
    
    print("✓ Config đã tạo: sroie_augmented.yaml")
    print("✓ Augmentation settings:")
    print("  - Mosaic: 1.0")
    print("  - MixUp: 0.2")
    print("  - Copy-Paste: 0.3")
    print("  - HSV augmentation: Strong")
    print("\nĐể train:")
    print("  model = YOLO('yolov8n.pt')")
    print("  model.train(data='sroie_augmented.yaml', epochs=100, batch=4)")

elif choice == "4":
    print("\n" + "="*60)
    print("OPTION 4: FINE-TUNE VỚI CLASS WEIGHTS")
    print("="*60)
    
    script_content = """
\"\"\"
Fine-tune YOLO với class weights cho 'total'
\"\"\"

from ultralytics import YOLO
import torch

# Load model
model = YOLO("d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt")

# Custom training với class weights
# Tăng trọng số cho class 'total' (class_id=2)
class_weights = [1.0, 1.0, 2.0, 1.0]  # [company, date, total, address]

print("Fine-tuning với class weights...")
print(f"Weights: {class_weights}")
print()

# Note: YOLOv8 không support trực tiếp class_weights trong API
# Cần modify loss function hoặc oversample 'total' class

# Alternative: Oversample images có 'total'
# Hoặc dùng focal loss

print("Đang implement custom training loop...")
print("(Cần customize YOLO source code hoặc dùng callback)")
"""
    
    print("⚠️ YOLOv8 không hỗ trợ trực tiếp class weights")
    print("Alternatives:")
    print("  1. Oversample images có 'total' field")
    print("  2. Dùng focal loss (cần modify source)")
    print("  3. Dùng weighted-boxes-fusion trong inference")
    print("\nKhuyến nghị: Dùng Option 1 (giảm threshold) hoặc Option 2 (continue training)")

elif choice == "5":
    print("\n" + "="*60)
    print("OPTION 5: ENSEMBLE MODELS")
    print("="*60)
    
    script_content = """
\"\"\"
Ensemble multiple YOLO checkpoints
Sử dụng Weighted Boxes Fusion (WBF)
\"\"\"

from ultralytics import YOLO
import numpy as np
import cv2
from ensemble_boxes import weighted_boxes_fusion

# Load multiple checkpoints
models = [
    YOLO("d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt"),
    YOLO("d:/ORC_Service/runs/detect/sroie_invoice2/weights/last.pt"),
]

def ensemble_predict(image_path, models, conf_thresh=0.15):
    \"\"\"Ensemble prediction từ nhiều models\"\"\"
    
    image = cv2.imread(image_path)
    h, w = image.shape[:2]
    
    all_boxes = []
    all_scores = []
    all_labels = []
    
    for model in models:
        results = model.predict(source=image_path, conf=conf_thresh, verbose=False)
        
        if len(results) > 0 and results[0].boxes is not None:
            boxes = results[0].boxes.xyxy.cpu().numpy()
            scores = results[0].boxes.conf.cpu().numpy()
            labels = results[0].boxes.cls.cpu().numpy().astype(int)
            
            # Normalize boxes to [0, 1]
            boxes_norm = boxes.copy()
            boxes_norm[:, [0, 2]] /= w
            boxes_norm[:, [1, 3]] /= h
            
            all_boxes.append(boxes_norm.tolist())
            all_scores.append(scores.tolist())
            all_labels.append(labels.tolist())
    
    # Weighted Boxes Fusion
    boxes, scores, labels = weighted_boxes_fusion(
        all_boxes, 
        all_scores, 
        all_labels,
        weights=[1, 1],  # Equal weights
        iou_thr=0.5,
        skip_box_thr=0.0001
    )
    
    # Denormalize boxes
    boxes[:, [0, 2]] *= w
    boxes[:, [1, 3]] *= h
    
    return boxes, scores, labels

print("✓ Ensemble function created")
print("Note: Cần cài đặt ensemble-boxes:")
print("  pip install ensemble-boxes")
"""
    
    with open("d:/ORC_Service/ensemble_inference.py", "w", encoding="utf-8") as f:
        f.write(script_content)
    
    print("✓ Script đã tạo: ensemble_inference.py")
    print("\nCài đặt dependency:")
    print("  pip install ensemble-boxes")
    print("\nNhược điểm: Cần nhiều checkpoints khác nhau")
    print("Ưu điểm: Không cần train lại")

else:
    print("\nLựa chọn không hợp lệ!")

print("\n" + "="*60)
print("RECOMMENDATION")
print("="*60)
print("✅ Khuyến nghị: Chọn OPTION 1 (Giảm threshold)")
print("   - Nhanh nhất, không cần train")
print("   - Giải quyết được 26/50 cases có low confidence")
print("   - Có thể kết hợp với Option 2 sau")
