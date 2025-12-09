"""
So sánh model cũ vs model mới
Đánh giá improvement sau continue training
"""

import json
from pathlib import Path

print("="*60)
print("YOLO TRAINING RESULTS COMPARISON")
print("="*60)
print()

# Old model metrics
old_metrics = {
    'checkpoint': 'Epoch 44',
    'mAP50': 0.7081,
    'mAP50-95': 0.5166,
    'precision': 0.693,
    'recall': 0.681
}

# New model metrics (từ training output)
new_metrics = {
    'checkpoint': 'Epoch 94',
    'mAP50': 0.7277,
    'mAP50-95': 0.5468,
    'precision': 0.7047,
    'recall': 0.7012
}

print("DETECTION METRICS COMPARISON")
print("-"*60)
print(f"{'Metric':<20} {'Old (Epoch 44)':<20} {'New (Epoch 94)':<20} {'Change':<10}")
print("-"*60)

# mAP50
map50_change = ((new_metrics['mAP50'] - old_metrics['mAP50']) / old_metrics['mAP50']) * 100
print(f"{'mAP50':<20} {old_metrics['mAP50']:.4f} ({old_metrics['mAP50']*100:.2f}%) {new_metrics['mAP50']:.4f} ({new_metrics['mAP50']*100:.2f}%)  {map50_change:+.2f}%")

# mAP50-95
map5095_change = ((new_metrics['mAP50-95'] - old_metrics['mAP50-95']) / old_metrics['mAP50-95']) * 100
print(f"{'mAP50-95':<20} {old_metrics['mAP50-95']:.4f} ({old_metrics['mAP50-95']*100:.2f}%) {new_metrics['mAP50-95']:.4f} ({new_metrics['mAP50-95']*100:.2f}%)  {map5095_change:+.2f}%")

# Precision
prec_change = ((new_metrics['precision'] - old_metrics['precision']) / old_metrics['precision']) * 100
print(f"{'Precision':<20} {old_metrics['precision']:.4f} ({old_metrics['precision']*100:.2f}%) {new_metrics['precision']:.4f} ({new_metrics['precision']*100:.2f}%)  {prec_change:+.2f}%")

# Recall
recall_change = ((new_metrics['recall'] - old_metrics['recall']) / old_metrics['recall']) * 100
print(f"{'Recall':<20} {old_metrics['recall']:.4f} ({old_metrics['recall']*100:.2f}%) {new_metrics['recall']:.4f} ({new_metrics['recall']*100:.2f}%)  {recall_change:+.2f}%")

print()
print("="*60)
print("SUMMARY")
print("="*60)

# Overall improvement
avg_improvement = (map50_change + map5095_change + prec_change + recall_change) / 4

print(f"\nAverage Improvement: {avg_improvement:+.2f}%")
print(f"Training Epochs: 44 → 94 (+50 epochs)")
print(f"Training Time: ~3-4 hours")

if map50_change > 0:
    print("\n✅ SUCCESS! Model improved after continue training!")
    print(f"   mAP50 increased by {map50_change:.2f}%")
    
    # Estimate impact on total extraction
    old_total_detection = 80.1  # From previous evaluation
    estimated_new = old_total_detection * (1 + map50_change/100)
    
    print(f"\n📊 Estimated Impact:")
    print(f"   Old Total Detection: {old_total_detection}%")
    print(f"   Estimated New: {estimated_new:.1f}%")
    print(f"   Expected gain: {estimated_new - old_total_detection:+.1f}%")
    
    print("\n✓ RECOMMENDATION: Use new model!")
    use_new = True
else:
    print("\n⚠️  Model did not improve significantly")
    print("   Consider: More epochs, different hyperparameters, or data augmentation")
    use_new = False

print("\n" + "="*60)
print("NEXT STEPS")
print("="*60)

if use_new:
    print("\n1. Test new model on full dataset:")
    print("   python test_new_model.py")
    print()
    print("2. Update inference scripts:")
    print("   - Update model path in inference_easyocr.py")
    print("   - Run: python evaluate_easyocr_full.py")
    print()
    print("3. Compare end-to-end results:")
    print("   - Old: 61.1% total accuracy")
    print("   - New: [Expected ~64-66%]")
else:
    print("\n1. Analyze training curves:")
    print("   explorer D:\\ORC_Service\\runs\\detect\\sroie_invoice_continued")
    print()
    print("2. Check for overfitting:")
    print("   - Review results.csv")
    print("   - Compare train vs val loss")
    print()
    print("3. Try alternative approaches:")
    print("   - Stronger augmentation")
    print("   - Different model size (YOLOv8s)")
    print("   - Class-specific training")

# Save comparison
comparison = {
    'old_model': old_metrics,
    'new_model': new_metrics,
    'improvements': {
        'mAP50': map50_change,
        'mAP50-95': map5095_change,
        'precision': prec_change,
        'recall': recall_change,
        'average': avg_improvement
    },
    'recommendation': 'use_new_model' if use_new else 'keep_old_model'
}

with open('d:/ORC_Service/model_comparison.json', 'w') as f:
    json.dump(comparison, f, indent=2)

print(f"\n✓ Comparison saved to: model_comparison.json")
