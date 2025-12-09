"""
Script test để kiểm tra OCR Service hoạt động
"""
import requests
import json
from pathlib import Path
import sys

# Config
OCR_SERVICE_URL = "http://localhost:8001"
BACKEND_URL = "http://localhost:8080"

def test_ocr_service_health():
    """Test 1: Kiểm tra OCR service có đang chạy không"""
    print("\n" + "="*60)
    print("TEST 1: OCR Service Health Check")
    print("="*60)
    
    try:
        response = requests.get(f"{OCR_SERVICE_URL}/health", timeout=5)
        if response.status_code == 200:
            data = response.json()
            print("✅ OCR Service đang chạy")
            print(f"   Status: {data.get('status')}")
            print(f"   Model loaded: {data.get('model_loaded')}")
            return True
        else:
            print(f"❌ OCR Service lỗi: {response.status_code}")
            return False
    except requests.exceptions.ConnectionError:
        print("❌ Không thể kết nối tới OCR Service")
        print(f"   Đảm bảo service đang chạy tại {OCR_SERVICE_URL}")
        return False
    except Exception as e:
        print(f"❌ Lỗi: {e}")
        return False

def test_ocr_parse():
    """Test 2: Test OCR parsing với ảnh mẫu"""
    print("\n" + "="*60)
    print("TEST 2: OCR Parsing")
    print("="*60)
    
    # Tìm ảnh test
    test_images = [
        "SROIE2019/test/img/X00016469670.jpg",
        "demo_output/X00016469670_result.jpg",
        "SROIE2019/train/img/X00016469612.jpg"
    ]
    
    test_image = None
    for img in test_images:
        if Path(img).exists():
            test_image = img
            break
    
    if not test_image:
        print("⚠️  Không tìm thấy ảnh test")
        print("   Bỏ qua test này")
        return None
    
    print(f"📁 Sử dụng ảnh: {test_image}")
    
    try:
        with open(test_image, 'rb') as f:
            files = {'file': f}
            response = requests.post(
                f"{OCR_SERVICE_URL}/api/ocr/parse-invoice",
                files=files,
                timeout=30
            )
        
        if response.status_code == 200:
            data = response.json()
            
            if data.get('success'):
                print("✅ OCR thành công")
                result = data.get('data', {})
                print(f"   Company: {result.get('company', 'N/A')}")
                print(f"   Date: {result.get('date', 'N/A')}")
                print(f"   Total: {result.get('total', 'N/A')}")
                print(f"   Address: {result.get('address', 'N/A')[:50]}...")
                print(f"   Detections: {result.get('num_detections', 0)}")
                return True
            else:
                print(f"❌ OCR thất bại: {data.get('message')}")
                return False
        else:
            print(f"❌ HTTP Error: {response.status_code}")
            print(f"   Response: {response.text[:200]}")
            return False
            
    except Exception as e:
        print(f"❌ Lỗi: {e}")
        return False

def test_backend_health():
    """Test 3: Kiểm tra Backend có kết nối được với OCR không"""
    print("\n" + "="*60)
    print("TEST 3: Backend Integration Check")
    print("="*60)
    
    try:
        response = requests.get(f"{BACKEND_URL}/api/ocr/health", timeout=5)
        if response.status_code == 200:
            data = response.json()
            print("✅ Backend đang chạy")
            print(f"   Current provider: {data.get('currentProvider')}")
            
            providers = data.get('providers', {})
            for provider_name, provider_info in providers.items():
                status = "✅" if provider_info.get('available') else "❌"
                print(f"   {status} {provider_name}: {provider_info}")
            
            return True
        else:
            print(f"❌ Backend lỗi: {response.status_code}")
            return False
    except requests.exceptions.ConnectionError:
        print("❌ Không thể kết nối tới Backend")
        print(f"   Đảm bảo backend đang chạy tại {BACKEND_URL}")
        return False
    except Exception as e:
        print(f"❌ Lỗi: {e}")
        return False

def main():
    print("="*60)
    print("OCR SERVICE INTEGRATION TEST")
    print("="*60)
    
    results = {
        'ocr_health': test_ocr_service_health(),
        'ocr_parse': test_ocr_parse(),
        'backend_health': test_backend_health()
    }
    
    print("\n" + "="*60)
    print("KẾT QUẢ TỔNG HỢP")
    print("="*60)
    
    for test_name, result in results.items():
        if result is True:
            status = "✅ PASS"
        elif result is False:
            status = "❌ FAIL"
        else:
            status = "⚠️  SKIP"
        print(f"{test_name:20s}: {status}")
    
    # Tổng kết
    passed = sum(1 for r in results.values() if r is True)
    failed = sum(1 for r in results.values() if r is False)
    skipped = sum(1 for r in results.values() if r is None)
    
    print("\n" + "="*60)
    if failed == 0:
        print("🎉 TẤT CẢ TESTS ĐỀU PASS!")
        print("   Hệ thống OCR đã sẵn sàng sử dụng")
    else:
        print(f"⚠️  CÓ {failed} TESTS FAILED")
        print("   Vui lòng kiểm tra lại cấu hình")
    
    print(f"\n   Passed: {passed} | Failed: {failed} | Skipped: {skipped}")
    print("="*60)
    
    return 0 if failed == 0 else 1

if __name__ == "__main__":
    sys.exit(main())
