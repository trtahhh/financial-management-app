"""
Monitor YOLO training progress
Theo dõi metrics và estimated time
"""

import time
from pathlib import Path
import pandas as pd
import json

def monitor_training():
    """Monitor training progress from results.csv"""
    
    results_path = Path("d:/ORC_Service/runs/detect/sroie_invoice_continued/results.csv")
    
    print("="*60)
    print("YOLO TRAINING MONITOR")
    print("="*60)
    print()
    print("Monitoring: sroie_invoice_continued")
    print("Press Ctrl+C to stop monitoring")
    print()
    
    last_epoch = 0
    start_time = time.time()
    
    while True:
        try:
            if not results_path.exists():
                print("Waiting for training to start...")
                time.sleep(5)
                continue
            
            # Read results
            df = pd.read_csv(results_path)
            df.columns = df.columns.str.strip()
            
            if len(df) == 0:
                time.sleep(5)
                continue
            
            current_epoch = len(df)
            
            if current_epoch > last_epoch:
                last_epoch = current_epoch
                
                # Get latest metrics
                latest = df.iloc[-1]
                
                # Calculate progress
                total_epochs = 94
                progress = (current_epoch / total_epochs) * 100
                
                # Estimate time remaining
                elapsed = time.time() - start_time
                if current_epoch > 1:
                    time_per_epoch = elapsed / current_epoch
                    remaining_epochs = total_epochs - current_epoch
                    eta_seconds = time_per_epoch * remaining_epochs
                    eta_hours = eta_seconds / 3600
                else:
                    eta_hours = 0
                
                # Print progress
                print(f"\r[Epoch {current_epoch}/{total_epochs}] "
                      f"Progress: {progress:.1f}% | "
                      f"mAP50: {latest.get('metrics/mAP50(B)', 0):.4f} | "
                      f"Loss: {latest.get('train/box_loss', 0):.4f} | "
                      f"ETA: {eta_hours:.1f}h", end="")
                
                # Print detailed every 10 epochs
                if current_epoch % 10 == 0:
                    print()
                    print("-"*60)
                    print(f"Epoch {current_epoch} Summary:")
                    print(f"  mAP50: {latest.get('metrics/mAP50(B)', 0):.4f}")
                    print(f"  mAP50-95: {latest.get('metrics/mAP50-95(B)', 0):.4f}")
                    print(f"  Precision: {latest.get('metrics/precision(B)', 0):.4f}")
                    print(f"  Recall: {latest.get('metrics/recall(B)', 0):.4f}")
                    print(f"  Box Loss: {latest.get('train/box_loss', 0):.4f}")
                    print(f"  Cls Loss: {latest.get('train/cls_loss', 0):.4f}")
                    print(f"  DFL Loss: {latest.get('train/dfl_loss', 0):.4f}")
                    print("-"*60)
                    print()
            
            time.sleep(10)  # Check every 10 seconds
            
        except KeyboardInterrupt:
            print("\n\nMonitoring stopped by user")
            break
        except Exception as e:
            print(f"\nError: {e}")
            time.sleep(5)
    
    # Final summary
    if results_path.exists():
        df = pd.read_csv(results_path)
        df.columns = df.columns.str.strip()
        
        if len(df) > 0:
            print("\n" + "="*60)
            print("TRAINING SUMMARY")
            print("="*60)
            
            latest = df.iloc[-1]
            
            print(f"\nCurrent Epoch: {len(df)}/94")
            print(f"Best mAP50: {df['metrics/mAP50(B)'].max():.4f}")
            print(f"Current mAP50: {latest.get('metrics/mAP50(B)', 0):.4f}")
            print(f"Current mAP50-95: {latest.get('metrics/mAP50-95(B)', 0):.4f}")
            
            # Compare with baseline
            baseline_map50 = 0.7081
            current_map50 = df['metrics/mAP50(B)'].max()
            improvement = ((current_map50 - baseline_map50) / baseline_map50) * 100
            
            print(f"\nBaseline mAP50: {baseline_map50:.4f}")
            print(f"Improvement: {improvement:+.2f}%")
            
            if current_map50 > baseline_map50:
                print("\n✓ Training is improving the model!")
            else:
                print("\n⚠️  Model not yet better than baseline")

if __name__ == "__main__":
    monitor_training()
