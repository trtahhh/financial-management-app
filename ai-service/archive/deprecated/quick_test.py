#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Quick test to verify simple_vietnamese_nlp works"""

try:
    from simple_vietnamese_nlp import SimpleVietnameseNLPProcessor
    print("✓ Import successful")
    
    processor = SimpleVietnameseNLPProcessor()
    print("✓ Processor initialized")
    
    result = processor.classify_transaction("Quán phở Hùng - Phở bò tái 75k")
    print(f"✓ Classification successful: {result['predicted_category']} ({result['confidence']:.2f})")
    print(f"✓ All probabilities: {result.get('all_probabilities', {})}")
    
except SyntaxError as e:
    print(f"✗ Syntax Error: {e}")
    import traceback
    traceback.print_exc()
except Exception as e:
    print(f"✗ Error: {e}")
    import traceback
    traceback.print_exc()
