# Test Python AI Service Integration
# This script tests the full 4-layer categorization system

import requests
import json

# Test data - Vietnamese transactions
test_transactions = [
    {"description": "Phở bò Hà Nội 50k", "expected": "Ăn uống"},
    {"description": "Grab đi làm 35k", "expected": "Giao thông"},
    {"description": "Mua áo Zara 500k", "expected": "Mua sắm"},
    {"description": "Cafe sáng Highlands 25k", "expected": "Ăn uống"},
    {"description": "Tiền điện EVN 450k", "expected": "Tiện ích"},
    {"description": "CGV xem phim 180k", "expected": "Giải trí"},
    {"description": "Pharmacity mua thuốc 85k", "expected": "Sức khỏe"},
    {"description": "Học phí trung tâm anh ngữ 2500k", "expected": "Giáo dục"},
]

def test_python_ai_service():
    """Test Python AI Service"""
    print("=" * 80)
    print("TESTING PYTHON AI SERVICE (Port 8001)")
    print("=" * 80)
    
    url = "http://localhost:8001/classify"
    
    correct = 0
    total = len(test_transactions)
    
    for i, test in enumerate(test_transactions, 1):
        try:
            response = requests.post(url, json={"description": test["description"]})
            
            if response.status_code == 200:
                result = response.json()
                predicted = result.get("predicted_category")
                confidence = result.get("confidence", 0)
                
                status = "✓" if predicted == test["expected"] else "✗"
                if predicted == test["expected"]:
                    correct += 1
                
                print(f"\n{i}. {test['description']}")
                print(f"   Expected:  {test['expected']}")
                print(f"   Predicted: {predicted} ({confidence:.2%})")
                print(f"   Status:    {status}")
            else:
                print(f"\n{i}. ERROR: Status {response.status_code}")
                print(f"   {test['description']}")
                
        except Exception as e:
            print(f"\n{i}. FAILED: {e}")
            print(f"   {test['description']}")
    
    print("\n" + "=" * 80)
    print(f"ACCURACY: {correct}/{total} = {correct/total*100:.1f}%")
    print("=" * 80)

def test_health_check():
    """Test health check endpoint"""
    print("\n" + "=" * 80)
    print("HEALTH CHECK")
    print("=" * 80)
    
    try:
        response = requests.get("http://localhost:8001/health")
        if response.status_code == 200:
            health = response.json()
            print(f"✓ Service: {health.get('service')}")
            print(f"✓ Status: {health.get('status')}")
            print(f"✓ AI Available: {health.get('ai_available')}")
            print(f"✓ Planning Available: {health.get('planning_available')}")
            print(f"✓ Ultra Available: {health.get('ultra_available')}")
            
            ml_libs = health.get('ml_libraries', {})
            print(f"\n✓ ML Libraries:")
            for lib, status in ml_libs.items():
                icon = "✓" if status else "✗"
                print(f"   {icon} {lib}")
        else:
            print(f"✗ Health check failed: {response.status_code}")
    except Exception as e:
        print(f"✗ Cannot connect to service: {e}")

if __name__ == "__main__":
    print("\n🚀 FULL INTEGRATION TEST\n")
    
    test_health_check()
    test_python_ai_service()
    
    print("\n✅ Testing complete!")
