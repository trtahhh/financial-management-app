"""
Utility functions cho hệ thống OCR
"""

import cv2
import numpy as np
from typing import List, Dict, Tuple
import matplotlib.pyplot as plt
from pathlib import Path


def visualize_comparison(original_img, processed_img, title1="Original", title2="Processed"):
    """
    Hiển thị so sánh 2 ảnh
    
    Args:
        original_img: Ảnh gốc
        processed_img: Ảnh đã xử lý
        title1: Tiêu đề ảnh 1
        title2: Tiêu đề ảnh 2
    """
    plt.figure(figsize=(12, 6))
    
    plt.subplot(1, 2, 1)
    plt.imshow(cv2.cvtColor(original_img, cv2.COLOR_BGR2RGB))
    plt.title(title1)
    plt.axis('off')
    
    plt.subplot(1, 2, 2)
    if len(processed_img.shape) == 2:
        plt.imshow(processed_img, cmap='gray')
    else:
        plt.imshow(cv2.cvtColor(processed_img, cv2.COLOR_BGR2RGB))
    plt.title(title2)
    plt.axis('off')
    
    plt.tight_layout()
    plt.show()


def save_results_to_csv(results: List[Dict], output_file: str):
    """
    Lưu kết quả vào CSV
    
    Args:
        results: Danh sách kết quả
        output_file: File output
    """
    import pandas as pd
    
    # Flatten data
    rows = []
    for result in results:
        row = {
            'image_file': result.get('image_file', ''),
            'store_name': result.get('store_name', ''),
            'invoice_date': result.get('invoice_date', ''),
            'address': result.get('address', ''),
            'total_amount': result.get('total_amount', ''),
            'num_items': len(result.get('items', [])),
            'completeness': result.get('validation', {}).get('completeness', 0)
        }
        rows.append(row)
    
    df = pd.DataFrame(rows)
    df.to_csv(output_file, index=False, encoding='utf-8-sig')
    print(f"Đã lưu CSV tại {output_file}")


def evaluate_extraction(predictions: List[Dict], ground_truth: List[Dict]) -> Dict:
    """
    Đánh giá kết quả extraction
    
    Args:
        predictions: Kết quả dự đoán
        ground_truth: Ground truth
        
    Returns:
        Dict: Metrics
    """
    metrics = {
        'store_name_accuracy': 0.0,
        'date_accuracy': 0.0,
        'total_accuracy': 0.0,
        'address_accuracy': 0.0,
        'overall_accuracy': 0.0
    }
    
    if len(predictions) != len(ground_truth):
        print("Warning: Số lượng predictions và ground truth không khớp!")
        return metrics
    
    correct_store = 0
    correct_date = 0
    correct_total = 0
    correct_address = 0
    
    for pred, gt in zip(predictions, ground_truth):
        if pred.get('store_name') == gt.get('store_name'):
            correct_store += 1
        if pred.get('invoice_date') == gt.get('invoice_date'):
            correct_date += 1
        if pred.get('total_amount') == gt.get('total_amount'):
            correct_total += 1
        if pred.get('address') == gt.get('address'):
            correct_address += 1
    
    n = len(predictions)
    metrics['store_name_accuracy'] = correct_store / n
    metrics['date_accuracy'] = correct_date / n
    metrics['total_accuracy'] = correct_total / n
    metrics['address_accuracy'] = correct_address / n
    metrics['overall_accuracy'] = (correct_store + correct_date + correct_total + correct_address) / (4 * n)
    
    return metrics


def create_summary_report(results: List[Dict], output_file: str = "summary_report.txt"):
    """
    Tạo báo cáo tổng kết
    
    Args:
        results: Danh sách kết quả
        output_file: File output
    """
    total = len(results)
    
    has_store = sum(1 for r in results if r.get('store_name'))
    has_date = sum(1 for r in results if r.get('invoice_date'))
    has_total = sum(1 for r in results if r.get('total_amount'))
    has_address = sum(1 for r in results if r.get('address'))
    
    avg_items = np.mean([len(r.get('items', [])) for r in results])
    avg_completeness = np.mean([r.get('validation', {}).get('completeness', 0) for r in results])
    
    report = f"""
╔══════════════════════════════════════════════════════════╗
║          BÁO CÁO TỔNG KẾT OCR HÓA ĐƠN                   ║
╚══════════════════════════════════════════════════════════╝

📊 THỐNG KÊ TỔNG QUAN
{'─' * 60}
Tổng số hóa đơn xử lý:        {total}

📋 TỶ LỆ TRÍCH XUẤT THÀNH CÔNG
{'─' * 60}
Tên cửa hàng:                 {has_store}/{total} ({has_store/total*100:.1f}%)
Ngày hóa đơn:                 {has_date}/{total} ({has_date/total*100:.1f}%)
Địa chỉ:                      {has_address}/{total} ({has_address/total*100:.1f}%)
Tổng tiền:                    {has_total}/{total} ({has_total/total*100:.1f}%)

📦 CHI TIẾT
{'─' * 60}
Trung bình số mặt hàng/hóa đơn: {avg_items:.1f}
Độ hoàn thiện trung bình:      {avg_completeness*100:.1f}%

🎯 ĐÁNH GIÁ CHUNG
{'─' * 60}
{'Xuất sắc' if avg_completeness >= 0.9 else 'Tốt' if avg_completeness >= 0.7 else 'Khá' if avg_completeness >= 0.5 else 'Cần cải thiện'}

╔══════════════════════════════════════════════════════════╗
║  © 2024 Vietnamese Receipt OCR System                    ║
╚══════════════════════════════════════════════════════════╝
    """
    
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(report)
    print(f"\n📄 Báo cáo đã lưu tại: {output_file}")


def batch_resize_images(input_dir: str, output_dir: str, target_size: Tuple[int, int] = (1024, 1024)):
    """
    Resize batch ảnh
    
    Args:
        input_dir: Thư mục ảnh đầu vào
        output_dir: Thư mục ảnh output
        target_size: Kích thước mục tiêu (width, height)
    """
    input_path = Path(input_dir)
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    
    image_files = list(input_path.glob("*.jpg")) + list(input_path.glob("*.png"))
    
    for img_file in image_files:
        img = cv2.imread(str(img_file))
        if img is None:
            continue
        
        resized = cv2.resize(img, target_size, interpolation=cv2.INTER_AREA)
        
        output_file = output_path / img_file.name
        cv2.imwrite(str(output_file), resized)
    
    print(f"Đã resize {len(image_files)} ảnh")


if __name__ == "__main__":
    print("Utils module loaded successfully!")
