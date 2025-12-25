"""
RECEIPT OCR REST API
FastAPI server để sử dụng từ bất kỳ ngôn ngữ/framework nào

Installation:
    pip install fastapi uvicorn python-multipart

Run server:
    python api_server.py
    
API sẽ chạy tại: http://localhost:8000
Documentation: http://localhost:8000/docs
"""

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional
import uvicorn
import io
from pathlib import Path

from receipt_ocr import ReceiptOCR

# Khởi tạo FastAPI
app = FastAPI(
    title="Receipt OCR API",
    description="API quét và trích xuất thông tin hóa đơn tiếng Việt",
    version="1.0.0"
)

# CORS - cho phép gọi từ frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Khởi tạo OCR
print("🚀 Đang khởi tạo Receipt OCR...")
ocr_engine = ReceiptOCR()
print("✅ API sẵn sàng!")


# Response model
class OCRResult(BaseModel):
    store_name: str
    total_amount: int
    date: str
    address: str
    success: bool
    completeness: float
    message: str


@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "status": "running",
        "message": "Receipt OCR API is running",
        "version": "1.0.0"
    }


@app.post("/scan", response_model=OCRResult)
async def scan_receipt(file: UploadFile = File(...)):
    """
    Quét hóa đơn từ file upload
    
    Parameters:
    - file: Ảnh hóa đơn (jpg, png, etc.)
    
    Returns:
    - store_name: Tên cửa hàng
    - total_amount: Tổng tiền (VNĐ)
    - date: Ngày tháng (DD/MM/YYYY)
    - address: Địa chỉ
    - success: True nếu thành công
    - completeness: % thông tin đầy đủ
    - message: Thông báo
    
    Example curl:
        curl -X POST "http://localhost:8000/scan" -F "file=@receipt.jpg"
    """
    try:
        # Đọc file
        contents = await file.read()
        
        # Kiểm tra định dạng
        if not file.content_type.startswith('image/'):
            raise HTTPException(
                status_code=400,
                detail="File phải là ảnh (jpg, png, etc.)"
            )
        
        # Scan
        result = ocr_engine.scan_image_bytes(contents)
        
        return result
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Lỗi xử lý: {str(e)}"
        )


@app.post("/scan-path")
async def scan_receipt_path(image_path: str):
    """
    Quét hóa đơn từ đường dẫn file trên server
    
    Parameters:
    - image_path: Đường dẫn đến ảnh
    
    Returns:
    - Thông tin hóa đơn đã trích xuất
    
    Example:
        curl -X POST "http://localhost:8000/scan-path" -H "Content-Type: application/json" -d '{"image_path": "receipt.jpg"}'
    """
    try:
        result = ocr_engine.scan(image_path)
        return result
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Lỗi xử lý: {str(e)}"
        )


@app.get("/health")
async def health_check():
    """Check API health status"""
    return {
        "status": "healthy",
        "ocr_ready": True
    }


@app.post("/api/ocr/parse-invoice")
async def parse_invoice(file: UploadFile = File(...)):
    """
    Endpoint tương thích với backend Java
    Parse hóa đơn từ ảnh upload và trả về format như ORC_Service
    
    Returns:
    {
        "success": true,
        "message": "OCR thành công",
        "data": {
            "company": "Tên cửa hàng",
            "date": "DD/MM/YYYY",
            "total": "Số tiền",
            "address": "Địa chỉ",
            "rawText": "Toàn bộ text"
        }
    }
    """
    import time
    start_time = time.time()
    
    try:
        # Đọc file
        contents = await file.read()
        read_time = time.time() - start_time
        print(f"⏱️ File read time: {read_time:.2f}s")
        
        # Kiểm tra định dạng
        if not file.content_type.startswith('image/'):
            return JSONResponse(
                status_code=400,
                content={
                    "success": False,
                    "message": "File không phải là ảnh",
                    "error": "Invalid file type"
                }
            )
        
        # Scan bằng OCR engine
        ocr_start = time.time()
        result = ocr_engine.scan_image_bytes(contents)
        ocr_time = time.time() - ocr_start
        print(f"⏱️ OCR processing time: {ocr_time:.2f}s")
        
        # Format response để tương thích với backend Java
        response_data = {
            "company": result.get("store_name", ""),
            "date": result.get("date", ""),
            "total": str(result.get("total_amount", 0)),
            "address": result.get("address", ""),
            # rawText: kết hợp tất cả thông tin để phục vụ classifier
            "rawText": format_raw_text_from_receipt(result)
        }
        
        total_time = time.time() - start_time
        print(f"⏱️ Total request time: {total_time:.2f}s\n")
        
        return {
            "success": result.get("success", False),
            "message": result.get("message", "OCR completed"),
            "data": response_data
        }
        
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        return JSONResponse(
            status_code=500,
            content={
                "success": False,
                "message": f"Lỗi xử lý: {str(e)}",
                "error": str(e)
            }
        )


def format_raw_text_from_receipt(result: dict) -> str:
    """
    Format kết quả OCR thành text thuần để phục vụ cho classifier
    """
    text_parts = []
    
    # Thêm các trường chính
    if result.get("store_name"):
        text_parts.append(f"{result['store_name']}")
    if result.get("address"):
        text_parts.append(f"{result['address']}")
    if result.get("date"):
        text_parts.append(f"Date: {result['date']}")
    if result.get("total_amount"):
        text_parts.append(f"Total: {result['total_amount']}")
    
    return "\n".join(text_parts)


if __name__ == "__main__":
    print("\n" + "="*60)
    print("🚀 RECEIPT OCR API SERVER")
    print("="*60)
    print("\n📡 API endpoints:")
    print("  - POST /scan                    - Upload ảnh để quét (format đơn giản)")
    print("  - POST /api/ocr/parse-invoice   - Upload ảnh (format tương thích backend Java)")
    print("  - POST /scan-path               - Quét từ đường dẫn")
    print("  - GET  /health                  - Health check")
    print("  - GET  /docs                    - API documentation")
    print("\n🌐 Server: http://localhost:8001")
    print("📖 Docs:   http://localhost:8001/docs")
    print("="*60 + "\n")
    
    # Chạy server tại cổng 8001 (tương thích với backend config)
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8001,
        log_level="info"
    )
