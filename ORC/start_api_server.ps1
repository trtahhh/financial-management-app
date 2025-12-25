# ORC API Server Starter
# Khởi động FastAPI server cho OCR hóa đơn (YOLO + VietOCR)

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   ORC API SERVER STARTER" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra Python
Write-Host "Kiểm tra Python..." -ForegroundColor Yellow
$pythonCmd = Get-Command python -ErrorAction SilentlyContinue
if (-not $pythonCmd) {
    Write-Host "Python không được tìm thấy. Vui lòng cài đặt Python 3.8+" -ForegroundColor Red
    exit 1
}

$pythonVersion = python --version
Write-Host "Tìm thấy: $pythonVersion" -ForegroundColor Green

# Di chuyển đến thư mục ORC
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath
Write-Host "Thư mục làm việc: $scriptPath" -ForegroundColor Cyan

# Kiểm tra file api_server.py
if (-not (Test-Path "api_server.py")) {
    Write-Host "Không tìm thấy api_server.py" -ForegroundColor Red
    exit 1
}

# Kiểm tra file receipt_ocr.py
if (-not (Test-Path "receipt_ocr.py")) {
    Write-Host "Không tìm thấy receipt_ocr.py" -ForegroundColor Red
    exit 1
}

# Kiểm tra YOLO model
$modelPath = "runs/detect/receipt_detector3/weights/best.pt"
if (-not (Test-Path $modelPath)) {
    Write-Host "Cảnh báo: Không tìm thấy model tại $modelPath" -ForegroundColor Yellow
    Write-Host "Hệ thống sẽ thử tìm model khác..." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Kiểm tra dependencies..." -ForegroundColor Yellow

# Kiểm tra packages
$packages = @{
    "fastapi" = "fastapi"
    "uvicorn" = "uvicorn"
    "cv2" = "opencv-python"
    "ultralytics" = "ultralytics"
    "vietocr" = "vietocr"
}

$missing = @()
foreach ($key in $packages.Keys) {
    python -c "import $key" 2>$null
    if ($LASTEXITCODE -ne 0) {
        $missing += $packages[$key]
    }
}

if ($missing.Count -gt 0) {
    $pkgList = $missing -join ", "
    Write-Host "Thiếu các package: $pkgList" -ForegroundColor Yellow
    Write-Host "Cài đặt dependencies..." -ForegroundColor Yellow
    
    if (Test-Path "requirements.txt") {
        python -m pip install -r requirements.txt
    } else {
        python -m pip install fastapi uvicorn python-multipart opencv-python ultralytics vietocr torch
    }
}

Write-Host "Dependencies đã sẵn sàng" -ForegroundColor Green
Write-Host ""

# Khởi động server
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   ĐANG KHỞI ĐỘNG SERVER..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Server sẽ chạy tại: http://localhost:8001" -ForegroundColor Green
Write-Host "API Docs:           http://localhost:8001/docs" -ForegroundColor Green  
Write-Host "Nhấn Ctrl+C để dừng server" -ForegroundColor Yellow
Write-Host ""

try {
    python api_server.py
} catch {
    Write-Host ""
    Write-Host "Lỗi khi khởi động server: $_" -ForegroundColor Red
    exit 1
} finally {
    Write-Host ""
    Write-Host "Server đã dừng" -ForegroundColor Yellow
}
