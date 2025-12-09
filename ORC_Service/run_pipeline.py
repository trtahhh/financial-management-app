"""
Script chạy toàn bộ pipeline từ đầu đến cuối
Bao gồm: chuyển đổi dữ liệu, huấn luyện YOLO, crop, huấn luyện CRNN, inference
"""

import os
import sys
from pathlib import Path
import argparse

def run_command(cmd, description):
    """Chạy command và hiển thị progress"""
    print("\n" + "="*60)
    print(f">>> {description}")
    print("="*60)
    print(f"Command: {cmd}\n")
    
    result = os.system(cmd)
    
    if result != 0:
        print(f"\n❌ Error: {description} failed with code {result}")
        sys.exit(1)
    
    print(f"\n✓ {description} completed successfully!")

def main():
    parser = argparse.ArgumentParser(description='Run complete OCR pipeline')
    parser.add_argument('--sroie-path', type=str, default='d:/ORC_Service/SROIE2019',
                        help='Path to SROIE2019 dataset')
    parser.add_argument('--yolo-epochs', type=int, default=50,
                        help='Number of epochs for YOLO training')
    parser.add_argument('--crnn-epochs', type=int, default=100,
                        help='Number of epochs for CRNN training')
    parser.add_argument('--skip-convert', action='store_true',
                        help='Skip data conversion step')
    parser.add_argument('--skip-yolo-train', action='store_true',
                        help='Skip YOLO training step')
    parser.add_argument('--skip-crop', action='store_true',
                        help='Skip cropping step')
    parser.add_argument('--skip-crnn-train', action='store_true',
                        help='Skip CRNN training step')
    parser.add_argument('--inference-only', action='store_true',
                        help='Run inference only (skip all training)')
    
    args = parser.parse_args()
    
    print("="*60)
    print("INVOICE OCR PIPELINE")
    print("="*60)
    print(f"SROIE path: {args.sroie_path}")
    print(f"YOLO epochs: {args.yolo_epochs}")
    print(f"CRNN epochs: {args.crnn_epochs}")
    print("="*60)
    
    if args.inference_only:
        print("\n>>> Running inference only...")
        run_command(
            'python inference.py',
            'Inference'
        )
        return
    
    # Step 1: Convert SROIE to YOLO format
    if not args.skip_convert:
        run_command(
            'python convert_sroie_to_yolo.py',
            'Step 1: Convert SROIE to YOLO format'
        )
    else:
        print("\n>>> Skipping data conversion")
    
    # Step 2: Train YOLOv8
    if not args.skip_yolo_train:
        run_command(
            'python train_yolo.py',
            'Step 2: Train YOLOv8'
        )
    else:
        print("\n>>> Skipping YOLO training")
    
    # Step 3: Crop regions
    if not args.skip_crop:
        run_command(
            'python crop_regions.py',
            'Step 3: Crop detected regions'
        )
    else:
        print("\n>>> Skipping cropping")
    
    # Step 4: Train CRNN
    if not args.skip_crnn_train:
        run_command(
            'python train_crnn.py',
            'Step 4: Train CRNN'
        )
    else:
        print("\n>>> Skipping CRNN training")
    
    # Step 5: Run inference
    run_command(
        'python inference.py',
        'Step 5: Run inference'
    )
    
    print("\n" + "="*60)
    print("✓ PIPELINE COMPLETED SUCCESSFULLY!")
    print("="*60)
    print("\nOutput files:")
    print("- YOLO model: runs/detect/sroie_invoice/weights/best.pt")
    print("- CRNN model: crnn_models/best_crnn.pt")
    print("- Inference results: batch_results.json")
    print("- Visualizations: visualizations/")
    print("="*60)

if __name__ == "__main__":
    main()
