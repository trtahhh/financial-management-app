"""
Script theo dõi tiến độ training YOLOv8
Hiển thị metrics từ file results.csv
"""

import pandas as pd
import os
import time
from pathlib import Path

def monitor_training(results_dir, refresh_interval=10):
    """
    Theo dõi tiến độ training
    
    Args:
        results_dir: Thư mục chứa file results.csv
        refresh_interval: Thời gian refresh (giây)
    """
    results_file = Path(results_dir) / 'results.csv'
    
    print("="*80)
    print("THEO DÕI TIẾN ĐỘ TRAINING YOLOV8")
    print("="*80)
    print(f"Results file: {results_file}")
    print(f"Refresh interval: {refresh_interval}s")
    print("="*80)
    print("\nPress Ctrl+C to stop monitoring\n")
    
    last_epoch = 0
    
    try:
        while True:
            if results_file.exists():
                try:
                    # Đọc file results
                    df = pd.read_csv(results_file)
                    df.columns = df.columns.str.strip()  # Remove whitespace
                    
                    if len(df) > last_epoch:
                        # Có epoch mới
                        new_rows = df.iloc[last_epoch:]
                        
                        for idx, row in new_rows.iterrows():
                            epoch = int(row['epoch']) + 1
                            
                            # Extract metrics
                            train_box_loss = row.get('train/box_loss', 0)
                            train_cls_loss = row.get('train/cls_loss', 0)
                            train_dfl_loss = row.get('train/dfl_loss', 0)
                            
                            val_box_loss = row.get('val/box_loss', 0)
                            val_cls_loss = row.get('val/cls_loss', 0)
                            val_dfl_loss = row.get('val/dfl_loss', 0)
                            
                            metrics_mAP50 = row.get('metrics/mAP50(B)', 0)
                            metrics_mAP50_95 = row.get('metrics/mAP50-95(B)', 0)
                            
                            # Hiển thị
                            print(f"\n{'='*80}")
                            print(f"EPOCH {epoch:3d}")
                            print(f"{'='*80}")
                            print(f"Train Loss: box={train_box_loss:.4f} | cls={train_cls_loss:.4f} | dfl={train_dfl_loss:.4f}")
                            print(f"Val Loss  : box={val_box_loss:.4f} | cls={val_cls_loss:.4f} | dfl={val_dfl_loss:.4f}")
                            print(f"mAP       : mAP50={metrics_mAP50:.4f} | mAP50-95={metrics_mAP50_95:.4f}")
                            print(f"{'='*80}")
                        
                        last_epoch = len(df)
                        
                        # Thống kê tổng quan
                        print(f"\n📊 Progress: {last_epoch} epochs completed")
                        if metrics_mAP50 > 0:
                            print(f"🎯 Best mAP50 so far: {df['metrics/mAP50(B)'].max():.4f}")
                    
                    else:
                        # Chưa có epoch mới
                        print(f"⏳ Waiting for new epoch... (current: {last_epoch})", end='\r')
                
                except Exception as e:
                    print(f"⚠️  Error reading results file: {e}")
            
            else:
                print(f"⏳ Waiting for results file to be created...", end='\r')
            
            time.sleep(refresh_interval)
    
    except KeyboardInterrupt:
        print("\n\n✓ Monitoring stopped by user")
        if last_epoch > 0:
            print(f"\nFinal stats: {last_epoch} epochs completed")
            if results_file.exists():
                df = pd.read_csv(results_file)
                df.columns = df.columns.str.strip()
                print(f"Best mAP50: {df['metrics/mAP50(B)'].max():.4f}")
                print(f"Best mAP50-95: {df['metrics/mAP50-95(B)'].max():.4f}")

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description='Monitor YOLOv8 training progress')
    parser.add_argument('--results-dir', type=str,
                        default='d:/ORC_Service/runs/detect/sroie_invoice2',
                        help='Directory containing results.csv')
    parser.add_argument('--refresh', type=int, default=10,
                        help='Refresh interval in seconds')
    
    args = parser.parse_args()
    
    monitor_training(args.results_dir, args.refresh)
