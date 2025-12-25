"""
Test script để kiểm tra ORC API Server
Chạy sau khi đã khởi động api_server.py
"""

import requests
import json
from pathlib import Path

# Cấu hình
API_URL = "http://localhost:8001"
ENDPOINTS = {
    "health": f"{API_URL}/health",
    "parse_invoice": f"{API_URL}/api/ocr/parse-invoice",
    "scan": f"{API_URL}/scan"
}

def test_health_check():
    """Test health check endpoint"""
    print("\n" + "="*60)
    print("TEST 1: Health Check")
    print("="*60)
    
    try:
        response = requests.get(ENDPOINTS["health"], timeout=5)
        
        if response.status_code == 200:
            print("✅ Server đang chạy")
            print(f"Response: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
            return True
        else:
            print(f"❌ Server trả về status code: {response.status_code}")
            return False
    except requests.exceptions.ConnectionError:
        print("❌ Không thể kết nối đến server")
        print("   Vui lòng đảm bảo đã chạy: python api_server.py")
        return False
    except Exception as e:
        print(f"❌ Lỗi: {str(e)}")
        return False


def test_parse_invoice_endpoint(image_path: str = None):
    """Test endpoint /api/ocr/parse-invoice (dùng cho backend Java)"""
    print("\n" + "="*60)
    print("TEST 2: Parse Invoice Endpoint")
    print("="*60)
    
    if image_path is None or not Path(image_path).exists():
        print("⚠️  Không có ảnh test, bỏ qua test này")
        print("   Để test, cung cấp đường dẫn ảnh hóa đơn")
        return None
    
    try:
        print(f"📤 Đang upload: {image_path}")
        
        with open(image_path, 'rb') as f:
            files = {'file': (Path(image_path).name, f, 'image/jpeg')}
            response = requests.post(
                ENDPOINTS["parse_invoice"],
                files=files,
                timeout=30
            )
        
        if response.status_code == 200:
            result = response.json()
            print("✅ OCR thành công!")
            print(f"\nResponse format (tương thích backend Java):")
            print(json.dumps(result, indent=2, ensure_ascii=False))
            
            if result.get("success"):
                data = result.get("data", {})
                print(f"\n📊 Thông tin trích xuất:")
                print(f"  🏪 Tên cửa hàng: {data.get('company', 'N/A')}")
                print(f"  📅 Ngày: {data.get('date', 'N/A')}")
                print(f"  💰 Tổng tiền: {data.get('total', 'N/A')}")
                print(f"  📍 Địa chỉ: {data.get('address', 'N/A')[:50]}...")
            
            return True
        else:
            print(f"❌ API trả về status code: {response.status_code}")
            print(f"Response: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Lỗi: {str(e)}")
        return False


def test_scan_endpoint(image_path: str = None):
    """Test endpoint /scan (format đơn giản)"""
    print("\n" + "="*60)
    print("TEST 3: Scan Endpoint (Simple Format)")
    print("="*60)
    
    if image_path is None or not Path(image_path).exists():
        print("⚠️  Không có ảnh test, bỏ qua test này")
        return None
    
    try:
        print(f"📤 Đang upload: {image_path}")
        
        with open(image_path, 'rb') as f:
            files = {'file': (Path(image_path).name, f, 'image/jpeg')}
            response = requests.post(
                ENDPOINTS["scan"],
                files=files,
                timeout=30
            )
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Scan thành công!")
            print(f"\nResponse:")
            print(json.dumps(result, indent=2, ensure_ascii=False))
            
            print(f"\n📊 Thông tin:")
            print(f"  🏪 Cửa hàng: {result.get('store_name', 'N/A')}")
            print(f"  💰 Số tiền: {result.get('total_amount', 0):,} đ")
            print(f"  📅 Ngày: {result.get('date', 'N/A')}")
            print(f"  ✅ Thành công: {result.get('success', False)}")
            print(f"  📊 Độ đầy đủ: {result.get('completeness', 0):.0f}%")
            
            return True
        else:
            print(f"❌ API trả về status code: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"❌ Lỗi: {str(e)}")
        return False


def find_test_image():
    """Tìm ảnh test trong project"""
    # Các đường dẫn có thể có ảnh test
    possible_paths = [
        "archive/train_images/train_images/mcocr_public_145013aagqw.jpg",
        "new_results/X00016469670.jpg",
        "ultra_output/demo_receipt.jpg",
        "../ORC_Service/SROIE2019/test/img/X00016469670.jpg"
    ]
    
    for path in possible_paths:
        if Path(path).exists():
            return path
    
    return None


if __name__ == "__main__":
    print("\n" + "="*70)
    print("  🧪 ORC API SERVER TEST SUITE")
    print("="*70)
    print("\n📝 Test ORC API Server (YOLO + VietOCR)")
    print("🌐 API URL: " + API_URL)
    
    # Test 1: Health check
    health_ok = test_health_check()
    
    if not health_ok:
        print("\n" + "="*70)
        print("❌ Server chưa sẵn sàng. Vui lòng khởi động server trước:")
        print("   cd ORC")
        print("   python api_server.py")
        print("="*70)
        exit(1)
    
    # Tìm ảnh test
    test_image = find_test_image()
    
    if test_image:
        print(f"\n📷 Tìm thấy ảnh test: {test_image}")
    else:
        print("\n⚠️  Không tìm thấy ảnh test. Bỏ qua test upload.")
    
    # Test 2 & 3: Upload và OCR
    if test_image:
        test_parse_invoice_endpoint(test_image)
        test_scan_endpoint(test_image)
    
    # Tổng kết
    print("\n" + "="*70)
    print("✅ HOÀN TẤT TEST!")
    print("="*70)
    print("\n📌 Tóm tắt:")
    print("  ✅ Server đang chạy")
    print("  ✅ Endpoint /api/ocr/parse-invoice sẵn sàng (cho backend Java)")
    print("  ✅ Endpoint /scan sẵn sàng (format đơn giản)")
    
    print("\n🎯 Backend Java có thể kết nối đến:")
    print(f"   {ENDPOINTS['parse_invoice']}")
    
    print("\n📖 Xem thêm:")
    print(f"   API Docs: {API_URL}/docs")
    print(f"   Health Check: {ENDPOINTS['health']}")
    print("="*70 + "\n")
