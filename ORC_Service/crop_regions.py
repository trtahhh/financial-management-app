"""
Script crop các vùng ảnh sau khi phát hiện bởi YOLOv8
Dữ liệu crop được sử dụng để huấn luyện CRNN
"""

from ultralytics import YOLO
import cv2
import numpy as np
from pathlib import Path
import json
from tqdm import tqdm

# Class mapping
CLASS_NAMES = {
    0: 'company',
    1: 'date',
    2: 'total',
    3: 'address'
}

def crop_detected_regions(
    model_path,
    image_dir,
    output_dir,
    entities_dir=None,
    conf_threshold=0.25,
    save_visualization=True
):
    """
    Crop các vùng được phát hiện bởi YOLO
    
    Args:
        model_path: Đường dẫn đến YOLO model
        image_dir: Thư mục chứa ảnh
        output_dir: Thư mục lưu ảnh đã crop
        entities_dir: Thư mục chứa ground truth entities (để lấy text label)
        conf_threshold: Ngưỡng confidence
        save_visualization: Có lưu ảnh visualization không
    """
    
    # Load model
    print(f"Đang load model: {model_path}")
    model = YOLO(model_path)
    
    # Tạo thư mục output
    output_path = Path(output_dir)
    crops_dir = output_path / 'crops'
    vis_dir = output_path / 'visualizations'
    labels_file = output_path / 'labels.json'
    
    crops_dir.mkdir(parents=True, exist_ok=True)
    if save_visualization:
        vis_dir.mkdir(parents=True, exist_ok=True)
    
    # Dictionary lưu labels cho CRNN training
    crnn_labels = {}
    
    # Lấy danh sách ảnh
    image_path = Path(image_dir)
    image_files = list(image_path.glob('*.jpg')) + list(image_path.glob('*.png'))
    
    print(f"\nBắt đầu crop {len(image_files)} ảnh...")
    
    for img_file in tqdm(image_files):
        img_name = img_file.stem
        
        # Load ảnh
        image = cv2.imread(str(img_file))
        if image is None:
            print(f"Warning: Không thể đọc {img_file}")
            continue
        
        # Predict
        results = model.predict(
            source=image,
            conf=conf_threshold,
            iou=0.45,
            verbose=False
        )
        
        # Load ground truth nếu có
        gt_entities = {}
        if entities_dir:
            entities_file = Path(entities_dir) / f"{img_name}.txt"
            if entities_file.exists():
                try:
                    with open(entities_file, 'r', encoding='utf-8') as f:
                        gt_entities = json.load(f)
                except Exception as e:
                    print(f"Warning: Không thể đọc {entities_file}: {e}")
        
        # Xử lý kết quả
        if len(results) > 0 and results[0].boxes is not None:
            boxes = results[0].boxes
            
            # Visualization
            if save_visualization:
                vis_image = image.copy()
            
            # Crop từng detection
            for idx, (box, conf, cls) in enumerate(zip(
                boxes.xyxy.cpu().numpy(),
                boxes.conf.cpu().numpy(),
                boxes.cls.cpu().numpy()
            )):
                x1, y1, x2, y2 = map(int, box)
                class_id = int(cls)
                class_name = CLASS_NAMES.get(class_id, f'class_{class_id}')
                
                # Crop region
                cropped = image[y1:y2, x1:x2]
                
                if cropped.size == 0:
                    continue
                
                # Tên file crop
                crop_filename = f"{img_name}_{class_name}_{idx}.jpg"
                crop_path = crops_dir / crop_filename
                
                # Lưu crop
                cv2.imwrite(str(crop_path), cropped)
                
                # Lưu label nếu có ground truth
                if class_name in gt_entities:
                    crnn_labels[crop_filename] = {
                        'text': gt_entities[class_name],
                        'class': class_name,
                        'confidence': float(conf)
                    }
                
                # Vẽ visualization
                if save_visualization:
                    cv2.rectangle(vis_image, (x1, y1), (x2, y2), (0, 255, 0), 2)
                    label = f"{class_name}: {conf:.2f}"
                    cv2.putText(
                        vis_image, label, (x1, y1-10),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2
                    )
            
            # Lưu visualization
            if save_visualization:
                vis_path = vis_dir / f"{img_name}_detected.jpg"
                cv2.imwrite(str(vis_path), vis_image)
    
    # Lưu labels cho CRNN
    with open(labels_file, 'w', encoding='utf-8') as f:
        json.dump(crnn_labels, f, indent=2, ensure_ascii=False)
    
    print(f"\n✓ Hoàn thành!")
    print(f"- Số lượng crops: {len(list(crops_dir.glob('*.jpg')))}")
    print(f"- Số lượng labels: {len(crnn_labels)}")
    print(f"- Crops lưu tại: {crops_dir}")
    print(f"- Labels lưu tại: {labels_file}")
    
    return crnn_labels

def create_crnn_dataset(crops_dir, labels_file, output_dir):
    """
    Tạo dataset cho CRNN từ crops và labels
    Tổ chức theo format: train/val split
    
    Args:
        crops_dir: Thư mục chứa ảnh đã crop
        labels_file: File JSON chứa labels
        output_dir: Thư mục output cho CRNN dataset
    """
    from sklearn.model_selection import train_test_split
    import shutil
    
    # Load labels
    with open(labels_file, 'r', encoding='utf-8') as f:
        labels = json.load(f)
    
    # Tạo danh sách file có label
    labeled_files = list(labels.keys())
    
    # Split train/val (80/20)
    train_files, val_files = train_test_split(
        labeled_files, test_size=0.2, random_state=42
    )
    
    # Tạo thư mục
    output_path = Path(output_dir)
    for split in ['train', 'val']:
        (output_path / split).mkdir(parents=True, exist_ok=True)
    
    # Copy files và tạo labels
    crops_path = Path(crops_dir)
    
    for split, files in [('train', train_files), ('val', val_files)]:
        split_labels = {}
        
        for filename in files:
            src = crops_path / filename
            if src.exists():
                dst = output_path / split / filename
                shutil.copy2(src, dst)
                split_labels[filename] = labels[filename]['text']
        
        # Lưu labels cho split này
        label_file = output_path / split / 'labels.txt'
        with open(label_file, 'w', encoding='utf-8') as f:
            for filename, text in split_labels.items():
                f.write(f"{filename}\t{text}\n")
    
    print(f"\n✓ Tạo CRNN dataset hoàn thành!")
    print(f"- Train: {len(train_files)} ảnh")
    print(f"- Val: {len(val_files)} ảnh")
    print(f"- Dataset lưu tại: {output_dir}")

if __name__ == "__main__":
    # Cấu hình
    MODEL_PATH = "d:/ORC_Service/runs/detect/sroie_invoice2/weights/best.pt"
    IMAGE_DIR = "d:/ORC_Service/SROIE2019/train/img"
    ENTITIES_DIR = "d:/ORC_Service/SROIE2019/train/entities"
    OUTPUT_DIR = "d:/ORC_Service/SROIE_CROPS"
    
    # Crop regions
    labels = crop_detected_regions(
        model_path=MODEL_PATH,
        image_dir=IMAGE_DIR,
        output_dir=OUTPUT_DIR,
        entities_dir=ENTITIES_DIR,
        conf_threshold=0.25,
        save_visualization=True
    )
    
    # Tạo CRNN dataset
    create_crnn_dataset(
        crops_dir=f"{OUTPUT_DIR}/crops",
        labels_file=f"{OUTPUT_DIR}/labels.json",
        output_dir="d:/ORC_Service/CRNN_DATASET"
    )
