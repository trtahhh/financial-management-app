"""
FastAPI Service cho OCR Hóa đơn tự huấn luyện
Thay thế OCR.space API bằng mô hình YOLO + EasyOCR đã train
"""

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import uvicorn
import cv2
import numpy as np
from pathlib import Path
import tempfile
import logging
from typing import Dict, List, Optional
import json

# Import OCR system với EasyOCR
try:
    from inference_easyocr import InvoiceOCR_EasyOCR as InvoiceOCR
    OCR_ENGINE = "EasyOCR"
except ImportError:
    print("EasyOCR not available, falling back to CRNN")
    from inference import InvoiceOCR
    OCR_ENGINE = "CRNN"

# Logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize FastAPI
app = FastAPI(
    title="Custom Invoice OCR API",
    description=f"OCR service sử dụng mô hình YOLO + {OCR_ENGINE} tự huấn luyện",
    version="1.0.0"
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Response models
class DetectionResult(BaseModel):
    class_name: str
    text: str
    confidence: float
    bbox: List[int]

class OCRResponse(BaseModel):
    success: bool
    message: str
    data: Optional[Dict] = None
    error: Optional[str] = None

# Global OCR system
ocr_system: Optional[InvoiceOCR] = None

@app.on_event("startup")
def load_models():
    """Load mô hình khi khởi động service"""
    global ocr_system
    
    try:
        logger.info("Đang tải mô hình OCR...")
        
        # Đường dẫn tới models (có thể config từ environment)
        yolo_model_path = "runs/detect/sroie_invoice_continued/weights/best.pt"
        
        # Kiểm tra file tồn tại
        if not Path(yolo_model_path).exists():
            logger.warning(f"YOLO model không tìm thấy tại {yolo_model_path}, thử path khác...")
            yolo_model_path = "runs/detect/sroie_invoice2/weights/best.pt"
        
        if not Path(yolo_model_path).exists():
            raise FileNotFoundError(f"YOLO model không tìm thấy: {yolo_model_path}")
        
        # Khởi tạo OCR system với EasyOCR hoặc CRNN
        if OCR_ENGINE == "EasyOCR":
            logger.info("Initializing YOLO + EasyOCR system...")
            ocr_system = InvoiceOCR(
                yolo_model_path=yolo_model_path,
                device='cuda',  # Tự động fallback về CPU nếu không có CUDA
                yolo_conf=0.25,
                yolo_iou=0.45,
                languages=['en']  # EasyOCR hỗ trợ nhiều ngôn ngữ
            )
        else:
            logger.info("Initializing YOLO + CRNN system...")
            crnn_model_path = "crnn_models/best_crnn.pt"
            if not Path(crnn_model_path).exists():
                raise FileNotFoundError(f"CRNN model không tìm thấy: {crnn_model_path}")
            ocr_system = InvoiceOCR(
                yolo_model_path=yolo_model_path,
                crnn_model_path=crnn_model_path,
                device='cuda',
                yolo_conf=0.25,
                yolo_iou=0.45,
                crnn_img_height=32
            )
        
        logger.info("✅ Mô hình OCR đã sẵn sàng!")
        
    except Exception as e:
        logger.error(f"❌ Lỗi khi tải mô hình: {e}")
        raise

@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "service": "Custom Invoice OCR API",
        "status": "running",
        "ocr_engine": OCR_ENGINE,
        "model_loaded": ocr_system is not None
    }

@app.get("/health")
async def health_check():
    """Kiểm tra trạng thái service"""
    if ocr_system is None:
        return {"status": "error", "message": "Models not loaded"}
    return {"status": "healthy", "message": "Service is running"}

@app.post("/api/ocr/parse-invoice", response_model=OCRResponse)
async def parse_invoice(file: UploadFile = File(...)):
    """
    Parse hóa đơn từ ảnh upload
    
    Input: Image file (jpg, png, etc.)
    Output: JSON với các trường: company, date, total, address
    """
    
    if ocr_system is None:
        raise HTTPException(status_code=500, detail="OCR system chưa được khởi tạo")
    
    try:
        # Validate file type
        if not file.content_type.startswith('image/'):
            return OCRResponse(
                success=False,
                message="File không phải là ảnh",
                error="Invalid file type"
            )
        
        logger.info(f"Nhận request OCR: {file.filename}, size: {file.size} bytes")
        
        # Đọc file
        contents = await file.read()
        nparr = np.frombuffer(contents, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if image is None:
            return OCRResponse(
                success=False,
                message="Không thể đọc ảnh",
                error="Failed to decode image"
            )
        
        # Lưu tạm để xử lý
        with tempfile.NamedTemporaryFile(delete=False, suffix='.jpg') as tmp_file:
            tmp_path = tmp_file.name
            cv2.imwrite(tmp_path, image)
        
        # Process với OCR system
        try:
            result = ocr_system.process_invoice(
                image_path=tmp_path,
                save_visualization=False,
                output_path=None
            )
            
            # Format response để tương thích với format cũ của OCR.space
            response_data = {
                "company": result.get("company", ""),
                "date": result.get("date", ""),
                "total": result.get("total", ""),
                "address": result.get("address", ""),
                "detections": result.get("detections", []),
                "num_detections": len(result.get("detections", [])),
                # Thêm raw text để classifier có thể dùng
                "rawText": format_raw_text(result)
            }
            
            logger.info(f"OCR thành công: {len(result.get('detections', []))} vùng được phát hiện")
            
            return OCRResponse(
                success=True,
                message="OCR thành công",
                data=response_data
            )
            
        finally:
            # Cleanup temp file
            Path(tmp_path).unlink(missing_ok=True)
    
    except Exception as e:
        logger.error(f"Lỗi OCR: {str(e)}", exc_info=True)
        return OCRResponse(
            success=False,
            message="Lỗi khi xử lý OCR",
            error=str(e)
        )

def format_raw_text(result: Dict) -> str:
    """
    Format kết quả OCR thành text thuần để phục vụ cho classifier
    Deduplicate các dòng để tránh lặp lại
    """
    seen_lines = set()  # Track đã thêm dòng nào
    text_parts = []
    
    # Thêm các trường chính FIRST (priority cao)
    if result.get("company"):
        company_text = str(result['company']).strip()
        if company_text and company_text not in seen_lines:
            text_parts.append(f"Company: {company_text}")
            seen_lines.add(company_text)
    
    if result.get("date"):
        date_text = str(result['date']).strip()
        if date_text and date_text not in seen_lines:
            text_parts.append(f"Date: {date_text}")
            seen_lines.add(date_text)
    
    if result.get("total"):
        total_text = str(result['total']).strip()
        if total_text and total_text not in seen_lines:
            text_parts.append(f"Total: {total_text}")
            seen_lines.add(total_text)
    
    if result.get("address"):
        address_text = str(result['address']).strip()
        if address_text and address_text not in seen_lines:
            text_parts.append(f"Address: {address_text}")
            seen_lines.add(address_text)
    
    # Thêm tất cả text đã detect (skip nếu đã thêm)
    for det in result.get("detections", []):
        det_text = det.get("text", "")
        if det_text:
            det_text_stripped = str(det_text).strip()
            # Skip nếu text này chứa trong các giá trị đã thêm
            if det_text_stripped and det_text_stripped not in seen_lines:
                # Kiểm tra xem text này có phải là một phần của dòng nào đó không
                is_duplicate = False
                for seen_line in seen_lines:
                    if det_text_stripped.lower() in seen_line.lower() or \
                       seen_line.lower() in det_text_stripped.lower():
                        is_duplicate = True
                        break
                
                if not is_duplicate:
                    text_parts.append(det_text_stripped)
                    seen_lines.add(det_text_stripped)
    
    return "\n".join(text_parts)

@app.post("/api/ocr/batch-parse")
async def batch_parse_invoices(files: List[UploadFile] = File(...)):
    """
    Parse nhiều hóa đơn cùng lúc
    """
    if ocr_system is None:
        raise HTTPException(status_code=500, detail="OCR system chưa được khởi tạo")
    
    results = []
    
    for file in files:
        try:
            result = await parse_invoice(file)
            results.append({
                "filename": file.filename,
                "result": result.dict()
            })
        except Exception as e:
            results.append({
                "filename": file.filename,
                "error": str(e)
            })
    
    return {
        "success": True,
        "total_files": len(files),
        "results": results
    }

if __name__ == "__main__":
    # Chạy server
    import argparse
    
    parser = argparse.ArgumentParser(description="Custom OCR API Server")
    parser.add_argument("--host", type=str, default="0.0.0.0", help="Host address")
    parser.add_argument("--port", type=int, default=8001, help="Port number")
    parser.add_argument("--reload", action="store_true", help="Enable auto-reload")
    
    args = parser.parse_args()
    
    logger.info(f"Khởi động OCR API server tại http://{args.host}:{args.port}")
    
    uvicorn.run(
        "ocr_api:app",
        host=args.host,
        port=args.port,
        reload=args.reload
    )
