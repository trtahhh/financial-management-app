# Khởi động AI Service với Virtual Environment
# Sử dụng: .\start_service.ps1

# Đường dẫn đến project
$ProjectPath = "C:\Users\tuana\OneDrive\Desktop\Projects\finacial-management-app"
$VenvPath = "$ProjectPath\.venv\Scripts\Activate.ps1"
$ServicePath = "$ProjectPath\ai-service"

Write-Host "🚀 Khởi động Vietnamese Financial AI Service..." -ForegroundColor Green

# Kích hoạt virtual environment
Write-Host "📦 Kích hoạt virtual environment..." -ForegroundColor Yellow
& $VenvPath

# Di chuyển đến thư mục service
Set-Location $ServicePath

# Kiểm tra các file cần thiết
Write-Host "🔍 Kiểm tra các file cần thiết..." -ForegroundColor Yellow
$RequiredFiles = @("main.py", "enhanced_vietnamese_ai.py", "vietnamese_transaction_classifier.pkl", "tfidf_vectorizer.pkl")

foreach ($file in $RequiredFiles) {
    if (Test-Path $file) {
        Write-Host "✅ $file - OK" -ForegroundColor Green
    } else {
        Write-Host "❌ $file - THIẾU" -ForegroundColor Red
    }
}

# Khởi động service
Write-Host "🌟 Khởi động FastAPI service trên cổng 8001..." -ForegroundColor Green
python main.py