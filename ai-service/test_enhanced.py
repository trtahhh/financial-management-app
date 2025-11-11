from enhanced_vietnamese_ai import VietnameseFinancialAI

# Test enhanced AI với model mới
ai = VietnameseFinancialAI()
print(' Enhanced AI loaded!')

test_cases = [
 'Mua cà phê Starbucks 65000 VND',
 'Đi taxi Grab 45000 VND', 
 'Mua áo Uniqlo 300000 VND',
 'Xem phim CGV 120000 VND',
 'Khám bệnh phòng khám 200000 VND',
 'Học tiếng Anh 500000 VND',
 'Mua bitcoin 1000000 VND',
 'Trả tiền điện 150000 VND'
]

print("\n Testing Enhanced Vietnamese Classifier:")
print("=" * 60)

for test in test_cases:
 result = ai.classify_transaction(test)
 if 'error' not in result:
 category = result['category']
 confidence = result['confidence']
 print(f" {test}")
 print(f" → {category} (confidence: {confidence:.3f})")
 else:
 print(f" {test} → ERROR: {result['error']}")
 print()

print(" Enhanced AI testing completed!")