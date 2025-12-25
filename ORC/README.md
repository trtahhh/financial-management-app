# 🧾 Vietnamese Receipt OCR System

Hệ thống OCR quét và trích xuất thông tin tự động từ hóa đơn tiếng Việt sử dụng YOLO + VietOCR.

> **🔥 MỚI:** Tích hợp với Financial Management App! Xem [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)

## ✨ Tính năng

- 🏪 **Tên cửa hàng** - Nhận dạng tự động với fuzzy matching
- 💰 **Tổng tiền** - Trích xuất chính xác số tiền
- 📅 **Ngày tháng** - Hỗ trợ nhiều format
- 📍 **Địa chỉ** - Trích xuất địa chỉ đầy đủ
- 🌐 **REST API** - FastAPI server cho tích hợp dễ dàng

## 📊 Hiệu năng

- **Độ chính xác:** 92.5% average completeness
- **Success rate:** 100% (10/10 test images)
- **Tốc độ:** 2-3s/ảnh (GPU) | 5-7s/ảnh (CPU)

| Field | Accuracy |
|-------|----------|
| Store Name | 100% (10/10) |
| Total Amount | 90% (9/10) |
| Date | 80% (8/10) |
| Address | 100% (10/10) |

## 🚀 Quick Start

### 1. Cài đặt

```bash
# Clone repository
git clone <your-repo>
cd ORC

# Install dependencies
pip install -r requirements.txt
```

### 2. Khởi động API Server

```powershell
# Windows PowerShell
.\start_api_server.ps1

# Hoặc trực tiếp
python api_server.py
```

Server sẽ chạy tại: **http://localhost:8001**

### 3. Sử dụng

#### Qua API (Khuyến nghị cho production)

```bash
# Test với curl
curl -X POST "http://localhost:8001/api/ocr/parse-invoice" \
  -F "file=@receipt.jpg"
```

#### Qua Python Module

```python
from receipt_ocr import ReceiptOCR

# Khởi tạo
ocr = ReceiptOCR()

# Quét hóa đơn
result = ocr.scan("receipt.jpg")

# Kết quả
print(f"Cửa hàng: {result['store_name']}")
print(f"Tổng tiền: {result['total_amount']:,} đ")
print(f"Ngày: {result['date']}")
```

## 📁 Cấu trúc Project

```
ORC/
├── receipt_ocr.py              # ⭐ Module chính để sử dụng
├── api_server.py               # 🌐 REST API server
├── examples.py                 # 📝 Code examples
├── ultimate_yolo_ocr.py        # 🔧 Core OCR engine
├── train_yolo.py               # 🎓 YOLO training script
├── runs/detect/receipt_detector3/weights/
│   └── best.pt                 # 🧠 YOLO model (92.3% mAP)
├── src/                        # 📦 Source modules
└── USAGE_GUIDE.md              # 📖 Hướng dẫn chi tiết
```

## 💡 Các cách sử dụng

### 1️⃣ Python Module (Đơn giản nhất)

```python
from receipt_ocr import ReceiptOCR

ocr = ReceiptOCR()
result = ocr.scan("receipt.jpg")
```

### 2️⃣ REST API

```bash
# Chạy server
python api_server.py

# Call API
curl -X POST "http://localhost:8000/scan" -F "file=@receipt.jpg"
```

### 3️⃣ Batch Processing

```python
from receipt_ocr import ReceiptOCR

ocr = ReceiptOCR()
images = ["receipt1.jpg", "receipt2.jpg", "receipt3.jpg"]
results = ocr.batch_scan(images)
```

## 📚 Tài liệu

- [USAGE_GUIDE.md](USAGE_GUIDE.md) - Hướng dẫn chi tiết
- [examples.py](examples.py) - Code examples
- API Docs: http://localhost:8000/docs (sau khi chạy api_server.py)

## 🔧 API Reference

### ReceiptOCR Class

#### `__init__(model_path: str)`
Khởi tạo OCR engine.

#### `scan(image_path: str) -> Dict`
Quét 1 ảnh hóa đơn.

**Returns:**
```python
{
    'store_name': str,      # Tên cửa hàng
    'total_amount': int,    # Tổng tiền (VNĐ)
    'date': str,            # DD/MM/YYYY
    'address': str,         # Địa chỉ
    'success': bool,        # True nếu ≥75% completeness
    'completeness': float,  # 0-100%
    'message': str          # Thông báo
}
```

#### `scan_image_bytes(image_bytes: bytes) -> Dict`
Quét từ bytes (cho upload file).

#### `batch_scan(image_paths: list) -> list`
Quét nhiều ảnh.

## 🎯 Use Cases

### Ứng dụng kế toán
```python
# Quét hóa đơn → Tự động nhập database
result = ocr.scan("receipt.jpg")
db.insert_receipt(result)
```

### Web/Mobile App
```python
# API endpoint cho upload ảnh
python api_server.py
# Frontend upload → Backend xử lý → Trả JSON
```

### Automation
```python
# Quét hàng loạt → Export Excel
results = ocr.batch_scan(receipt_folder)
pd.DataFrame(results).to_excel("receipts.xlsx")
```

## 🛠️ Technology Stack

- **Detection:** YOLOv8n (custom trained on Vietnamese receipts)
- **OCR:** VietOCR (vgg_transformer model)
- **Preprocessing:** OpenCV (upscaling, denoising, contrast enhancement)
- **Framework:** PyTorch with CUDA support
- **API:** FastAPI (optional)

## ⚙️ Cấu hình

### GPU/CPU
Tự động detect và sử dụng GPU nếu có, fallback về CPU nếu không.

### Custom Model
```python
ocr = ReceiptOCR(model_path="path/to/your/model.pt")
```

## 📈 Performance Tips

1. **GPU:** Nhanh hơn 2-3 lần so với CPU
2. **Batch Processing:** Hiệu quả hơn khi xử lý nhiều ảnh
3. **Image Quality:** Ảnh rõ nét cho kết quả tốt hơn

## 🐛 Troubleshooting

### Lỗi: Model not found
```bash
# Kiểm tra model tồn tại
ls runs/detect/receipt_detector3/weights/best.pt
```

### Lỗi: CUDA out of memory
```python
# Hệ thống sẽ tự động fallback về CPU
```

### Kết quả không chính xác
- Kiểm tra chất lượng ảnh input
- Thử với ảnh độ phân giải cao hơn
- Đảm bảo ảnh không bị mờ/nghiêng

## 📦 Dependencies

Xem [requirements.txt](requirements.txt)

Core:
- Python 3.8+
- PyTorch 2.0+
- OpenCV 4.8+
- VietOCR 0.3.12
- Ultralytics 8.0+

## 🎓 Training

Model YOLO đã được train với:
- **Dataset:** MC-OCR 2021 (Vietnamese receipts)
- **Epochs:** 100
- **mAP50:** 92.3%
- **Classes:** STORE, ADDRESS, DATE, TOTAL

Để train lại:
```bash
python train_yolo.py
```

## 📝 Examples

### Example 1: Simple Scan
```python
from receipt_ocr import ReceiptOCR

ocr = ReceiptOCR()
result = ocr.scan("receipt.jpg")

if result['success']:
    print(f"✅ Tổng tiền: {result['total_amount']:,}đ")
else:
    print(f"❌ {result['message']}")
```

### Example 2: Export to Excel
```python
import pandas as pd

results = ocr.batch_scan(["r1.jpg", "r2.jpg", "r3.jpg"])
df = pd.DataFrame(results)
df.to_excel("receipts.xlsx")
```

### Example 3: Web API
```bash
python api_server.py
# Visit: http://localhost:8000/docs
```

Xem thêm examples trong [examples.py](examples.py)

## 🤝 Contributing

Contributions are welcome! 

## 📄 License

MIT License

## 👨‍💻 Author

Vietnamese Receipt OCR System

## 🙏 Acknowledgments

- YOLOv8 by Ultralytics
- VietOCR by pbcquoc
- MC-OCR 2021 Dataset

## 📞 Support

Nếu có vấn đề:
1. Kiểm tra [USAGE_GUIDE.md](USAGE_GUIDE.md)
2. Xem [examples.py](examples.py)
3. Check API docs: http://localhost:8000/docs

---

⭐ **Star this repo if you find it useful!**
