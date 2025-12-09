"""
Continue training YOLOv8 từ epoch 44 đến 94
Sử dụng best checkpoint với mAP50=70.81%
"""

from ultralytics import YOLO
import torch

print("="*60)
print("CONTINUE TRAINING YOLO")
print("="*60)
print()
print("Configuration:")
print(f"  Starting checkpoint: epoch 44 (mAP50: 70.81%)")
print(f"  Target epochs: 94 (thêm 50 epochs)")
print(f"  Batch size: 4 (GPU 2GB)")
print(f"  Image size: 640x640")
print(f"  Device: CUDA")
print(f"  Workers: 0 (Windows compatibility)")
print()
print("Expected improvements:")
print(f"  mAP50: 70.81% → 75-80% (+5-10%)")
print(f"  Total detection: 80.1% → 85-88%")
print(f"  Estimated time: 3-4 hours")
print()

# Check GPU
if torch.cuda.is_available():
    print(f"✓ GPU detected: {torch.cuda.get_device_name(0)}")
    print(f"  VRAM: {torch.cuda.get_device_properties(0).total_memory / 1024**3:.1f} GB")
else:
    print("⚠️  No GPU detected, training will be slow!")
    response = input("Continue with CPU? (y/n): ")
    if response.lower() != 'y':
        exit()

print()
confirm = input("Start training? This will take 3-4 hours. (y/n): ")

if confirm.lower() != 'y':
    print("Training cancelled.")
    exit()

print("\n" + "="*60)
print("STARTING TRAINING")
print("="*60)

# Load best checkpoint
checkpoint_path = "d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt"
model = YOLO(checkpoint_path)

print(f"\n✓ Loaded checkpoint: {checkpoint_path}")
print("✓ Starting training...")
print()

# Continue training
results = model.train(
    data="d:/ORC_Service/sroie.yaml",
    epochs=94,  # Train thêm đến epoch 94
    batch=4,    # Batch size 4 cho GPU 2GB
    imgsz=640,
    patience=50,  # Early stopping patience
    save=True,
    save_period=10,  # Save checkpoint mỗi 10 epochs
    project="d:/ORC_Service/runs/detect",
    name="sroie_invoice_continued",
    exist_ok=True,
    workers=0,  # Windows compatibility
    device=0,   # CUDA device 0
    verbose=True,
    plots=True,
    
    # Optimizer settings
    optimizer='SGD',
    lr0=0.001,  # Giảm learning rate cho fine-tuning
    lrf=0.01,
    momentum=0.937,
    weight_decay=0.0005,
    warmup_epochs=3,
    warmup_momentum=0.8,
    warmup_bias_lr=0.1,
    
    # Augmentation (giữ nguyên như training ban đầu)
    hsv_h=0.015,
    hsv_s=0.7,
    hsv_v=0.4,
    degrees=0.0,
    translate=0.1,
    scale=0.5,
    shear=0.0,
    perspective=0.0,
    flipud=0.0,
    fliplr=0.5,
    mosaic=1.0,
    mixup=0.0,
    copy_paste=0.0
)

print("\n" + "="*60)
print("TRAINING COMPLETED!")
print("="*60)

# Get final metrics
final_metrics = results.results_dict
print(f"\nFinal metrics:")
print(f"  mAP50: {final_metrics.get('metrics/mAP50(B)', 'N/A'):.4f}")
print(f"  mAP50-95: {final_metrics.get('metrics/mAP50-95(B)', 'N/A'):.4f}")
print(f"  Precision: {final_metrics.get('metrics/precision(B)', 'N/A'):.4f}")
print(f"  Recall: {final_metrics.get('metrics/recall(B)', 'N/A'):.4f}")

# Model location
save_dir = results.save_dir
print(f"\n✓ Models saved to: {save_dir}")
print(f"  - Best model: {save_dir}/weights/best.pt")
print(f"  - Last model: {save_dir}/weights/last.pt")
print(f"  - Training plots: {save_dir}/*.png")

print("\n" + "="*60)
print("NEXT STEPS")
print("="*60)
print("\n1. Đánh giá model mới:")
print("   - Copy best.pt vào thư mục hiện tại")
print("   - Chạy lại: python evaluate_easyocr_full.py")
print()
print("2. So sánh kết quả:")
print("   - Old mAP50: 70.81%")
print("   - New mAP50: [Check trong results]")
print()
print("3. Nếu improved, update inference scripts:")
print("   - Thay đổi model path trong inference_easyocr.py")
print("   - Test trên sample images")

print("\n✓ Done!")
