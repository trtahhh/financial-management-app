"""
Script chuyển đổi dữ liệu SROIE sang định dạng YOLO
Mỗi file box chứa tọa độ các từ, file entities chứa thông tin cần trích xuất
"""

import os
import json
import shutil
from pathlib import Path
from PIL import Image
import re

# Định nghĩa các class cần phát hiện
CLASSES = {
    'company': 0,
    'date': 1,
    'total': 2,
    'address': 3
}

def parse_box_file(box_file):
    """Đọc file box và trả về danh sách các word với tọa độ"""
    words = []
    with open(box_file, 'r', encoding='utf-8', errors='ignore') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            parts = line.split(',')
            if len(parts) >= 9:
                # Format: x1,y1,x2,y2,x3,y3,x4,y4,text
                coords = [int(parts[i]) for i in range(8)]
                text = ','.join(parts[8:])  # Text có thể chứa dấu phẩy
                words.append({
                    'coords': coords,
                    'text': text
                })
    return words

def parse_entities_file(entities_file):
    """Đọc file entities và trả về dictionary"""
    with open(entities_file, 'r', encoding='utf-8', errors='ignore') as f:
        return json.load(f)

def normalize_text(text):
    """Chuẩn hóa text để so sánh"""
    # Loại bỏ khoảng trắng thừa, chuyển về lowercase
    return re.sub(r'\s+', ' ', text.strip().lower())

def find_word_boxes_for_entity(entity_text, words, used_indices):
    """
    Tìm các word boxes tương ứng với entity text
    Trả về danh sách indices của words và bounding box tổng hợp
    """
    entity_normalized = normalize_text(entity_text)
    
    # Thử tìm khớp chính xác trước
    for i, word in enumerate(words):
        if i in used_indices:
            continue
        if normalize_text(word['text']) == entity_normalized:
            return [i], word['coords']
    
    # Thử tìm substring hoặc multi-word match
    best_match = []
    best_score = 0
    
    # Tìm chuỗi liên tiếp các words
    for start_idx in range(len(words)):
        if start_idx in used_indices:
            continue
        for end_idx in range(start_idx + 1, min(start_idx + 10, len(words) + 1)):
            if any(i in used_indices for i in range(start_idx, end_idx)):
                break
            
            combined_text = ' '.join([words[i]['text'] for i in range(start_idx, end_idx)])
            combined_normalized = normalize_text(combined_text)
            
            # Tính độ tương đồng
            if entity_normalized in combined_normalized or combined_normalized in entity_normalized:
                score = min(len(entity_normalized), len(combined_normalized))
                if score > best_score:
                    best_score = score
                    best_match = list(range(start_idx, end_idx))
    
    if best_match:
        # Tính bounding box tổng hợp
        all_coords = [words[i]['coords'] for i in best_match]
        min_x = min([min(coords[0], coords[2], coords[4], coords[6]) for coords in all_coords])
        min_y = min([min(coords[1], coords[3], coords[5], coords[7]) for coords in all_coords])
        max_x = max([max(coords[0], coords[2], coords[4], coords[6]) for coords in all_coords])
        max_y = max([max(coords[1], coords[3], coords[5], coords[7]) for coords in all_coords])
        
        bbox = [min_x, min_y, max_x, min_y, max_x, max_y, min_x, max_y]
        return best_match, bbox
    
    return [], None

def coords_to_yolo_format(coords, img_width, img_height):
    """
    Chuyển đổi tọa độ 4 điểm sang format YOLO (x_center, y_center, width, height)
    Tất cả được normalize về [0, 1]
    """
    x_coords = [coords[0], coords[2], coords[4], coords[6]]
    y_coords = [coords[1], coords[3], coords[5], coords[7]]
    
    x_min = min(x_coords)
    x_max = max(x_coords)
    y_min = min(y_coords)
    y_max = max(y_coords)
    
    x_center = (x_min + x_max) / 2.0 / img_width
    y_center = (y_min + y_max) / 2.0 / img_height
    width = (x_max - x_min) / img_width
    height = (y_max - y_min) / img_height
    
    return x_center, y_center, width, height

def convert_sroie_to_yolo(sroie_root, output_root):
    """
    Chuyển đổi toàn bộ dataset SROIE sang format YOLO
    
    Args:
        sroie_root: Thư mục gốc chứa train/test
        output_root: Thư mục output cho dataset YOLO
    """
    sroie_path = Path(sroie_root)
    output_path = Path(output_root)
    
    # Tạo cấu trúc thư mục YOLO
    for split in ['train', 'val']:
        (output_path / split / 'images').mkdir(parents=True, exist_ok=True)
        (output_path / split / 'labels').mkdir(parents=True, exist_ok=True)
    
    stats = {'train': {'total': 0, 'success': 0}, 'val': {'total': 0, 'success': 0}}
    
    # Xử lý train và test (test sẽ được dùng làm validation)
    for sroie_split, yolo_split in [('train', 'train'), ('test', 'val')]:
        img_dir = sroie_path / sroie_split / 'img'
        box_dir = sroie_path / sroie_split / 'box'
        entities_dir = sroie_path / sroie_split / 'entities'
        
        if not img_dir.exists():
            print(f"Warning: {img_dir} không tồn tại")
            continue
        
        # Lấy danh sách các file ảnh
        image_files = list(img_dir.glob('*.jpg')) + list(img_dir.glob('*.png'))
        
        for img_file in image_files:
            stats[yolo_split]['total'] += 1
            base_name = img_file.stem
            
            box_file = box_dir / f"{base_name}.txt"
            entities_file = entities_dir / f"{base_name}.txt"
            
            if not box_file.exists() or not entities_file.exists():
                print(f"Skip {base_name}: thiếu box hoặc entities file")
                continue
            
            # Đọc image để lấy kích thước
            try:
                img = Image.open(img_file)
                img_width, img_height = img.size
            except Exception as e:
                print(f"Error opening {img_file}: {e}")
                continue
            
            # Parse dữ liệu
            words = parse_box_file(box_file)
            entities = parse_entities_file(entities_file)
            
            # Tạo YOLO labels
            yolo_labels = []
            used_indices = set()
            
            for entity_name, entity_value in entities.items():
                if entity_name not in CLASSES or not entity_value:
                    continue
                
                class_id = CLASSES[entity_name]
                
                # Tìm word boxes tương ứng
                matched_indices, bbox = find_word_boxes_for_entity(
                    entity_value, words, used_indices
                )
                
                if bbox:
                    # Chuyển sang YOLO format
                    x_center, y_center, width, height = coords_to_yolo_format(
                        bbox, img_width, img_height
                    )
                    
                    yolo_labels.append(
                        f"{class_id} {x_center:.6f} {y_center:.6f} {width:.6f} {height:.6f}"
                    )
                    
                    # Đánh dấu các word đã sử dụng
                    used_indices.update(matched_indices)
            
            # Lưu labels và copy ảnh
            if yolo_labels:
                label_file = output_path / yolo_split / 'labels' / f"{base_name}.txt"
                with open(label_file, 'w', encoding='utf-8') as f:
                    f.write('\n'.join(yolo_labels))
                
                # Copy ảnh
                output_img = output_path / yolo_split / 'images' / img_file.name
                shutil.copy2(img_file, output_img)
                
                stats[yolo_split]['success'] += 1
            else:
                print(f"Warning: {base_name} không có label nào được tạo")
    
    # In thống kê
    print("\n" + "="*50)
    print("THỐNG KÊ CHUYỂN ĐỔI")
    print("="*50)
    for split, stat in stats.items():
        print(f"{split.upper()}: {stat['success']}/{stat['total']} ảnh thành công")
    print("="*50)
    
    return stats

if __name__ == "__main__":
    # Cấu hình đường dẫn
    SROIE_ROOT = "d:/ORC_Service/SROIE2019"
    OUTPUT_ROOT = "d:/ORC_Service/SROIE_YOLO"
    
    print("Bắt đầu chuyển đổi SROIE sang YOLO format...")
    stats = convert_sroie_to_yolo(SROIE_ROOT, OUTPUT_ROOT)
    print("\nHoàn thành!")
