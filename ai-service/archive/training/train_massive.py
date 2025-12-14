from enhanced_trainer import EnhancedVietnameseClassifierTrainer
import time

print(" INDUSTRIAL-SCALE VIETNAMESE AI TRAINING")
print("=" * 60)

start_time = time.time()

# Initialize trainer
trainer = EnhancedVietnameseClassifierTrainer()

# Load massive dataset
print(" Loading massive dataset...")
df = trainer.load_dataset('massive_vietnamese_dataset_200k.json')

print(f" Dataset loaded: {len(df):,} samples")
print(" Starting training with 200K Vietnamese transactions...")

# Train with massive dataset (no hyperparameter tuning for speed)
results = trainer.train_enhanced_model(df, use_hyperparameter_tuning=False)

end_time = time.time()
training_duration = end_time - start_time

print("\n" + "=" * 60)
print(" INDUSTRIAL-SCALE TRAINING COMPLETED!")
print("=" * 60)
print(f" Final accuracy: {results['accuracy']:.4f} ({results['accuracy']*100:.2f}%)")
print(f" CV Mean accuracy: {results['cv_scores'].mean():.4f} ± {results['cv_scores'].std()*2:.4f}")
print(f"⏱ Training time: {training_duration:.1f} seconds ({training_duration/60:.1f} minutes)")
print(f" Samples processed: {len(df):,}")
print(f" Processing speed: {len(df)/training_duration:.0f} samples/second")

print("\n MASSIVE VIETNAMESE AI MODEL READY FOR PRODUCTION!")