"""
Script để train YOLO model cho detection hóa đơn
"""

from ultralytics import YOLO
import yaml
from pathlib import Path
import shutil
import json
import pandas as pd
import cv2
import numpy as np


def convert_csv_to_yolo_format(csv_file, images_dir, output_dir):
    """
    Convert CSV annotation sang YOLO format
    
    Args:
        csv_file: File CSV chứa annotation
        images_dir: Thư mục chứa ảnh
        output_dir: Thư mục output
    """
    output_path = Path(output_dir)
    
    # Tạo cấu trúc thư mục
    train_images = output_path / 'train' / 'images'
    train_labels = output_path / 'train' / 'labels'
    val_images = output_path / 'val' / 'images'
    val_labels = output_path / 'val' / 'labels'
    
    for p in [train_images, train_labels, val_images, val_labels]:
        p.mkdir(parents=True, exist_ok=True)
    
    # Đọc CSV
    df = pd.read_csv(csv_file)
    
    # Category mapping
    category_mapping = {
        15: 0,  # SELLER -> store_name
        16: 1,  # ADDRESS -> address
        17: 2,  # TIMESTAMP -> date
        18: 3   # TOTAL_COST -> total_amount
    }
    
    # Split train/val (80/20)
    train_size = int(len(df) * 0.8)
    
    for idx, row in df.iterrows():
        img_id = row['img_id']
        
        # Đường dẫn ảnh gốc
        src_img = Path(images_dir) / img_id
        if not src_img.exists():
            continue
        
        # Đọc kích thước ảnh
        img = cv2.imread(str(src_img))
        if img is None:
            continue
        
        img_h, img_w = img.shape[:2]
        
        # Parse annotations
        try:
            anno_polygons = json.loads(row['anno_polygons'].replace("'", '"'))
        except:
            continue
        
        # Chuyển sang YOLO format
        yolo_lines = []
        for poly_data in anno_polygons:
            category_id = poly_data['category_id']
            if category_id not in category_mapping:
                continue
            
            class_id = category_mapping[category_id]
            bbox = poly_data['bbox']  # [x, y, width, height]
            
            # Convert to YOLO format (normalized)
            x_center = (bbox[0] + bbox[2] / 2) / img_w
            y_center = (bbox[1] + bbox[3] / 2) / img_h
            width = bbox[2] / img_w
            height = bbox[3] / img_h
            
            yolo_lines.append(f"{class_id} {x_center} {y_center} {width} {height}")
        
        if not yolo_lines:
            continue
        
        # Quyết định train hay val
        if idx < train_size:
            dst_img = train_images / img_id
            dst_label = train_labels / f"{Path(img_id).stem}.txt"
        else:
            dst_img = val_images / img_id
            dst_label = val_labels / f"{Path(img_id).stem}.txt"
        
        # Copy ảnh
        shutil.copy(src_img, dst_img)
        
        # Ghi label
        with open(dst_label, 'w') as f:
            f.write('\n'.join(yolo_lines))
    
    print(f"Converted {len(df)} images")
    print(f"Train: {train_size}, Val: {len(df) - train_size}")


def create_data_yaml(output_dir):
    """Tạo file data.yaml cho YOLO"""
    
    data = {
        'path': str(Path(output_dir).absolute()),
        'train': 'train/images',
        'val': 'val/images',
        'nc': 4,
        'names': ['store_name', 'address', 'date', 'total_amount']
    }
    
    yaml_path = Path(output_dir) / 'data.yaml'
    with open(yaml_path, 'w') as f:
        yaml.dump(data, f, default_flow_style=False)
    
    print(f"Created data.yaml at {yaml_path}")
    return yaml_path


def train_yolo(data_yaml, epochs=100, batch=16, imgsz=640):
    """
    Train YOLO model
    
    Args:
        data_yaml: Đường dẫn file data.yaml
        epochs: Số epochs
        batch: Batch size
        imgsz: Kích thước ảnh
    """
    # Load pretrained model
    model = YOLO('yolov8n.pt')  # nano model
    
    # Train
    results = model.train(
        data=str(data_yaml),
        epochs=epochs,
        imgsz=imgsz,
        batch=batch,
        name='receipt_detector',
        patience=50,
        save=True,
        device=0,  # GPU 0 (sử dụng CUDA)
        workers=4,
        pretrained=True,
        optimizer='auto',
        verbose=True,
        seed=42,
        deterministic=True,
        single_cls=False,
        rect=False,
        cos_lr=False,
        close_mosaic=10,
        resume=False,
        amp=True,
        fraction=1.0,
        profile=False,
        freeze=None,
        lr0=0.01,
        lrf=0.01,
        momentum=0.937,
        weight_decay=0.0005,
        warmup_epochs=3.0,
        warmup_momentum=0.8,
        warmup_bias_lr=0.1,
        box=7.5,
        cls=0.5,
        dfl=1.5,
        pose=12.0,
        kobj=1.0,
        label_smoothing=0.0,
        nbs=64,
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
    
    print("Training completed!")
    print(f"Best model saved at: runs/detect/receipt_detector/weights/best.pt")
    
    return model


def main():
    """Main function"""
    
    print("="*60)
    print("TRAIN YOLO MODEL CHO DETECTION HÓA ĐƠN")
    print("="*60)
    
    # Đường dẫn
    csv_file = "archive/mcocr_train_df.csv"
    images_dir = "archive/train_images/train_images"
    output_dir = "yolo_dataset"
    
    # 1. Convert data
    print("\n📊 Đang convert data sang YOLO format...")
    convert_csv_to_yolo_format(csv_file, images_dir, output_dir)
    
    # 2. Tạo data.yaml
    print("\n📝 Đang tạo data.yaml...")
    data_yaml = create_data_yaml(output_dir)
    
    # 3. Train
    print("\n🚀 Bắt đầu training...")
    print("⚠️  Lưu ý: Training có thể mất vài giờ!")
    
    response = input("\nBạn có muốn tiếp tục training? (y/n): ")
    if response.lower() == 'y':
        model = train_yolo(data_yaml, epochs=100, batch=16, imgsz=640)  # Tăng batch size với GPU
        print("\n✅ Training hoàn thành!")
        print("\n📍 Model đã lưu tại: runs/detect/receipt_detector/weights/best.pt")
        print("\n💡 Sử dụng model:")
        print("   python src/main.py --image path/to/image.jpg \\")
        print("                      --yolo-model runs/detect/receipt_detector/weights/best.pt \\")
        print("                      --use-yolo \\")
        print("                      --output result.json")
    else:
        print("\nĐã hủy training.")


if __name__ == "__main__":
    main()
