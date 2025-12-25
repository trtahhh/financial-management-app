"""
EXAMPLES - Các ví dụ sử dụng Receipt OCR
"""

from receipt_ocr import ReceiptOCR
from pathlib import Path
import pandas as pd


def example_1_simple_scan():
    """VÍ DỤ 1: Quét đơn giản 1 ảnh"""
    print("\n" + "="*60)
    print("VÍ DỤ 1: QUÉT ĐƠN GIẢN 1 ẢNH")
    print("="*60)
    
    # Khởi tạo
    ocr = ReceiptOCR()
    
    # Quét
    result = ocr.scan("archive/train_images/train_images/mcocr_public_145013aagqw.jpg")
    
    # Hiển thị
    print(f"\n✅ Kết quả:")
    print(f"   Cửa hàng: {result['store_name']}")
    print(f"   Tổng tiền: {result['total_amount']:,} đ")
    print(f"   Ngày: {result['date']}")
    print(f"   Địa chỉ: {result['address'][:50]}...")


def example_2_batch_scan():
    """VÍ DỤ 2: Quét nhiều ảnh"""
    print("\n" + "="*60)
    print("VÍ DỤ 2: QUÉT NHIỀU ẢNH")
    print("="*60)
    
    ocr = ReceiptOCR()
    
    # Lấy 5 ảnh đầu
    image_dir = Path("archive/train_images/train_images")
    images = list(image_dir.glob("*.jpg"))[:5]
    
    # Quét batch
    results = ocr.batch_scan([str(img) for img in images])
    
    # Hiển thị
    print(f"\n✅ Đã quét {len(results)} ảnh:")
    for i, r in enumerate(results, 1):
        print(f"\n{i}. {Path(r['image_path']).name}")
        print(f"   • Cửa hàng: {r['store_name']}")
        print(f"   • Tổng tiền: {r['total_amount']:,} đ")
        print(f"   • Thành công: {'✓' if r['success'] else '✗'}")


def example_3_export_excel():
    """VÍ DỤ 3: Quét và export Excel"""
    print("\n" + "="*60)
    print("VÍ DỤ 3: QUÉT VÀ EXPORT EXCEL")
    print("="*60)
    
    ocr = ReceiptOCR()
    
    # Quét nhiều ảnh
    image_dir = Path("archive/train_images/train_images")
    images = list(image_dir.glob("*.jpg"))[:10]
    
    print(f"\n📄 Đang quét {len(images)} hóa đơn...")
    results = ocr.batch_scan([str(img) for img in images])
    
    # Tạo DataFrame
    df = pd.DataFrame([{
        'File': Path(r['image_path']).name,
        'Tên cửa hàng': r['store_name'],
        'Tổng tiền': r['total_amount'],
        'Ngày': r['date'],
        'Địa chỉ': r['address'],
        'Thành công': r['success'],
        'Độ đầy đủ (%)': r['completeness']
    } for r in results])
    
    # Export Excel
    output_file = "receipts_export.xlsx"
    df.to_excel(output_file, index=False)
    
    print(f"\n✅ Đã export {len(results)} hóa đơn vào {output_file}")
    print(f"\n📊 Tổng kết:")
    print(f"   • Thành công: {df['Thành công'].sum()}/{len(df)}")
    print(f"   • Tổng tiền: {df['Tổng tiền'].sum():,} đ")
    print(f"   • Độ đầy đủ TB: {df['Độ đầy đủ (%)'].mean():.1f}%")


def example_4_error_handling():
    """VÍ DỤ 4: Xử lý lỗi"""
    print("\n" + "="*60)
    print("VÍ DỤ 4: XỬ LÝ LỖI")
    print("="*60)
    
    ocr = ReceiptOCR()
    
    # Test với file không tồn tại
    result = ocr.scan("nonexistent.jpg")
    
    if result['success']:
        print("\n✅ Quét thành công!")
        print(f"   Tổng tiền: {result['total_amount']:,} đ")
    else:
        print("\n❌ Quét thất bại!")
        print(f"   Lỗi: {result['message']}")
        print(f"   Độ đầy đủ: {result['completeness']:.0f}%")


def example_5_filter_results():
    """VÍ DỤ 5: Lọc kết quả theo điều kiện"""
    print("\n" + "="*60)
    print("VÍ DỤ 5: LỌC KẾT QUẢ")
    print("="*60)
    
    ocr = ReceiptOCR()
    
    # Quét
    image_dir = Path("archive/train_images/train_images")
    images = list(image_dir.glob("*.jpg"))[:10]
    results = ocr.batch_scan([str(img) for img in images])
    
    # Lọc hóa đơn > 100,000đ
    high_value = [r for r in results if r['total_amount'] > 100000]
    print(f"\n💰 Hóa đơn > 100,000đ: {len(high_value)}")
    for r in high_value[:3]:
        print(f"   • {r['store_name']}: {r['total_amount']:,}đ")
    
    # Lọc theo cửa hàng
    vinmart = [r for r in results if 'VINMART' in r['store_name']]
    print(f"\n🏪 Hóa đơn VINMART: {len(vinmart)}")
    
    # Lọc theo tháng
    august = [r for r in results if r['date'].startswith('') and '/08/' in r['date']]
    print(f"\n📅 Hóa đơn tháng 8: {len(august)}")


def example_6_web_integration():
    """VÍ DỤ 6: Tích hợp Web (Flask example)"""
    print("\n" + "="*60)
    print("VÍ DỤ 6: CODE TÍCH HỢP WEB")
    print("="*60)
    
    code = '''
# Flask Example
from flask import Flask, request, jsonify
from receipt_ocr import ReceiptOCR

app = Flask(__name__)
ocr = ReceiptOCR()

@app.route('/api/scan', methods=['POST'])
def scan_receipt():
    file = request.files['receipt']
    image_bytes = file.read()
    
    result = ocr.scan_image_bytes(image_bytes)
    
    return jsonify({
        'success': result['success'],
        'data': {
            'store': result['store_name'],
            'total': result['total_amount'],
            'date': result['date'],
            'address': result['address']
        }
    })

if __name__ == '__main__':
    app.run(port=5000)
    '''
    
    print(code)
    print("\n✅ Chạy: python flask_app.py")
    print("   API: POST http://localhost:5000/api/scan")


if __name__ == "__main__":
    print("\n" + "="*60)
    print("🚀 RECEIPT OCR - EXAMPLES")
    print("="*60)
    
    # Chạy các ví dụ
    example_1_simple_scan()
    
    # Bỏ comment để chạy các ví dụ khác
    # example_2_batch_scan()
    # example_3_export_excel()
    # example_4_error_handling()
    # example_5_filter_results()
    # example_6_web_integration()
    
    print("\n" + "="*60)
    print("✅ HOÀN TẤT!")
    print("="*60)
    print("\nBỏ comment các example khác để test thêm!")
    print("Xem USAGE_GUIDE.md để biết thêm chi tiết.\n")
