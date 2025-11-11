from enhanced_trainer import EnhancedVietnameseClassifierTrainer

# Quick retrain với clean dataset
trainer = EnhancedVietnameseClassifierTrainer()
df = trainer.load_dataset('cleaned_vietnamese_transactions.json')
results = trainer.train_enhanced_model(df, use_hyperparameter_tuning=False)
print(f" Clean Model Accuracy: {results['accuracy']:.4f} ({results['accuracy']*100:.2f}%)")