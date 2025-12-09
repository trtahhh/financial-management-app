"""
Script tiếp tục training từ epoch 44 với cấu hình mới
"""

from ultralytics import YOLO
import torch

def main():
    print("="*60)
    print("CONTINUE TRAINING YOLOV8 (FIXED)")
    print("="*60)
    
    # Load best model từ epoch trước
    BEST_MODEL = "d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt"
    DATA_YAML = "d:/ORC_Service/SROIE_YOLO/sroie.yaml"
    
    print(f"Loading: {BEST_MODEL}")
    model = YOLO(BEST_MODEL)
    
    # Continue training thêm 6 epochs nữa (để đủ 50)
    # với workers=0 để tránh crash
    print("\nStarting training...")
    
    results = model.train(
        data=DATA_YAML,
        epochs=50,        # Tổng số epochs
        imgsz=640,
        batch=4,
        device=0,
        project='d:/ORC_Service/runs/detect',
        name='sroie_invoice_final',
        
        # Settings giống như trước
        hsv_h=0.015,
        hsv_s=0.7,
        hsv_v=0.4,
        degrees=0.0,
        translate=0.1,
        scale=0.5,
        fliplr=0.5,
        mosaic=1.0,
        
        lr0=0.01,
        lrf=0.01,
        momentum=0.937,
        weight_decay=0.0005,
        warmup_epochs=3.0,
        
        patience=50,
        save=True,
        save_period=10,
        plots=True,
        val=True,
        
        # FIX: Disable workers
        workers=0,
        
        # Các settings khác
        close_mosaic=10,
        amp=True,
        deterministic=True,
        seed=0
    )
    
    print("\n✓ Training hoàn thành!")
    print(f"Model: runs/detect/sroie_invoice_final/weights/best.pt")

if __name__ == '__main__':
    main()
