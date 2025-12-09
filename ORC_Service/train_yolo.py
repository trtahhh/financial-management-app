"""
Script huấn luyện YOLOv8 để phát hiện các vùng thông tin trên hóa đơn
Sử dụng trên Google Colab với GPU
"""

from ultralytics import YOLO
import torch
import os
from pathlib import Path

def train_yolo_model(
    data_yaml='sroie.yaml',
    img_size=640,
    epochs=50,
    batch_size=16,
    model_name='yolov8n.pt',
    project='runs/detect',
    name='sroie_invoice'
):
    """
    Huấn luyện mô hình YOLOv8
    
    Args:
        data_yaml: Đường dẫn đến file config yaml
        img_size: Kích thước ảnh input
        epochs: Số epoch huấn luyện
        batch_size: Batch size
        model_name: Tên pretrained model (yolov8n, yolov8s, yolov8m, yolov8l, yolov8x)
        project: Thư mục lưu kết quả
        name: Tên experiment
    """
    
    # Kiểm tra GPU
    device = 'cuda' if torch.cuda.is_available() else 'cpu'
    print(f"Sử dụng device: {device}")
    if device == 'cuda':
        print(f"GPU: {torch.cuda.get_device_name(0)}")
        print(f"CUDA Version: {torch.version.cuda}")
    
    # Load pretrained model
    print(f"\nĐang load model: {model_name}")
    model = YOLO(model_name)
    
    # Hiển thị thông tin model
    print(f"\nThông tin model:")
    print(f"- Model: {model_name}")
    print(f"- Image size: {img_size}x{img_size}")
    print(f"- Epochs: {epochs}")
    print(f"- Batch size: {batch_size}")
    
    # Bắt đầu huấn luyện
    print("\n" + "="*50)
    print("BẮT ĐẦU HUẤN LUYỆN")
    print("="*50)
    
    results = model.train(
        data=data_yaml,
        epochs=epochs,
        imgsz=img_size,
        batch=batch_size,
        device=device,
        project=project,
        name=name,
        
        # Augmentation parameters
        hsv_h=0.015,      # HSV-Hue augmentation
        hsv_s=0.7,        # HSV-Saturation augmentation
        hsv_v=0.4,        # HSV-Value augmentation
        degrees=0.0,      # Rotation (+/- degrees)
        translate=0.1,    # Translation (+/- fraction)
        scale=0.5,        # Scale (+/- gain)
        shear=0.0,        # Shear (+/- degrees)
        perspective=0.0,  # Perspective (+/- fraction)
        flipud=0.0,       # Flip up-down (probability)
        fliplr=0.5,       # Flip left-right (probability)
        mosaic=1.0,       # Mosaic augmentation (probability)
        mixup=0.0,        # MixUp augmentation (probability)
        
        # Hyperparameters
        lr0=0.01,         # Initial learning rate
        lrf=0.01,         # Final learning rate (lr0 * lrf)
        momentum=0.937,   # SGD momentum
        weight_decay=0.0005,  # Optimizer weight decay
        warmup_epochs=3.0,    # Warmup epochs
        warmup_momentum=0.8,  # Warmup initial momentum
        warmup_bias_lr=0.1,   # Warmup initial bias lr
        
        # Other settings
        box=7.5,          # Box loss gain
        cls=0.5,          # Cls loss gain
        dfl=1.5,          # DFL loss gain
        
        patience=50,      # Early stopping patience
        save=True,        # Save checkpoints
        save_period=10,   # Save checkpoint every x epochs
        
        # Validation
        val=True,         # Validate during training
        
        # Visualization
        plots=True,       # Save plots
        
        # Other
        verbose=True,
        seed=0,
        deterministic=True,
        single_cls=False,
        rect=False,
        cos_lr=False,
        close_mosaic=10,  # Disable mosaic last N epochs
        amp=True,         # Automatic Mixed Precision
        fraction=1.0,     # Train on fraction of data
        workers=0,        # Disable multiprocessing workers (fix Windows crash)
        
        # Resume
        resume=False,
    )
    
    print("\n" + "="*50)
    print("HOÀN THÀNH HUẤN LUYỆN")
    print("="*50)
    
    # Đường dẫn đến model tốt nhất
    best_model_path = Path(project) / name / 'weights' / 'best.pt'
    last_model_path = Path(project) / name / 'weights' / 'last.pt'
    
    print(f"\nModel tốt nhất: {best_model_path}")
    print(f"Model cuối cùng: {last_model_path}")
    
    return results, best_model_path

def validate_model(model_path, data_yaml, img_size=640):
    """
    Đánh giá mô hình trên validation set
    
    Args:
        model_path: Đường dẫn đến model đã train
        data_yaml: Đường dẫn đến file config yaml
        img_size: Kích thước ảnh
    """
    print("\n" + "="*50)
    print("ĐÁNH GIÁ MÔ HÌNH")
    print("="*50)
    
    model = YOLO(model_path)
    
    # Validate
    metrics = model.val(
        data=data_yaml,
        imgsz=img_size,
        batch=16,
        conf=0.25,
        iou=0.6,
        device='cuda' if torch.cuda.is_available() else 'cpu'
    )
    
    # In kết quả
    print(f"\nmAP50: {metrics.box.map50:.4f}")
    print(f"mAP50-95: {metrics.box.map:.4f}")
    print(f"Precision: {metrics.box.mp:.4f}")
    print(f"Recall: {metrics.box.mr:.4f}")
    
    return metrics

def export_model(model_path, export_format='onnx', img_size=640):
    """
    Export model sang các format khác
    
    Args:
        model_path: Đường dẫn đến model
        export_format: Format export ('onnx', 'torchscript', 'tflite', etc.)
        img_size: Kích thước ảnh
    """
    print("\n" + "="*50)
    print(f"EXPORT MODEL SANG {export_format.upper()}")
    print("="*50)
    
    model = YOLO(model_path)
    
    # Export
    export_path = model.export(
        format=export_format,
        imgsz=img_size,
        dynamic=False,
        simplify=True,
        opset=12 if export_format == 'onnx' else None
    )
    
    print(f"\nModel đã được export: {export_path}")
    return export_path

if __name__ == "__main__":
    # Cấu hình cho local machine với GPU nhỏ
    DATA_YAML = 'd:/ORC_Service/SROIE_YOLO/sroie.yaml'
    IMG_SIZE = 640
    EPOCHS = 50
    BATCH_SIZE = 4  # Giảm batch size cho GPU 2GB
    MODEL_NAME = 'yolov8n.pt'  # Có thể thay bằng yolov8s.pt, yolov8m.pt, etc.
    
    # Huấn luyện
    results, best_model = train_yolo_model(
        data_yaml=DATA_YAML,
        img_size=IMG_SIZE,
        epochs=EPOCHS,
        batch_size=BATCH_SIZE,
        model_name=MODEL_NAME,
        project='d:/ORC_Service/runs/detect',
        name='sroie_invoice'
    )
    
    # Đánh giá
    metrics = validate_model(best_model, DATA_YAML, IMG_SIZE)
    
    # Export sang ONNX
    onnx_path = export_model(best_model, 'onnx', IMG_SIZE)
    
    print("\n✓ Hoàn thành tất cả các bước!")
