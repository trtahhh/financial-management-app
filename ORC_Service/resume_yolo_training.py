"""
Script resume training từ checkpoint cuối cùng
"""

from ultralytics import YOLO
import torch

def main():
    # Cấu hình
    CHECKPOINT = "d:/ORC_Service/runs/detect/sroie_invoice2/weights/last.pt"
    DATA_YAML = "d:/ORC_Service/SROIE_YOLO/sroie.yaml"

    print("="*60)
    print("RESUME TRAINING YOLOV8")
    print("="*60)
    print(f"Checkpoint: {CHECKPOINT}")
    print(f"Data: {DATA_YAML}")
    print("="*60)

    # Load checkpoint
    model = YOLO(CHECKPOINT)

    print("\nResuming training...")

    # Resume training với workers=0 để tránh crash trên Windows
    results = model.train(
        data=DATA_YAML,
        resume=True,  # Resume từ checkpoint
        workers=0,    # Disable multiprocessing để tránh crash
        device=0      # Use GPU
    )

    print("\n✓ Training hoàn thành!")
    print(f"Best model: runs/detect/sroie_invoice2/weights/best.pt")

if __name__ == '__main__':
    main()
