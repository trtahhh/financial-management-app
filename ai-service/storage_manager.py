#!/usr/bin/env python3
"""
Intelligent Storage Manager for Vietnamese Financial Dataset
Manages disk space efficiently while maintaining data quality
"""

import os
import json
import shutil
import glob
import logging
from pathlib import Path
from typing import Dict, List, Tuple, Optional
import zipfile
import gzip
import pickle

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class IntelligentStorageManager:
 def __init__(self, base_dir: str = "."):
 self.base_dir = Path(base_dir)
 self.data_dir = self.base_dir / "data" 
 self.processed_dir = self.base_dir / "processed"
 self.compressed_dir = self.base_dir / "compressed"
 self.archive_dir = self.base_dir / "archive"
 
 # Create directories
 for dir_path in [self.compressed_dir, self.archive_dir]:
 dir_path.mkdir(exist_ok=True)
 
 def get_disk_usage(self) -> Dict[str, float]:
 """Get current disk usage in GB"""
 usage = {}
 
 # Check each directory
 for name, path in [
 ("raw_data", self.data_dir),
 ("processed", self.processed_dir), 
 ("compressed", self.compressed_dir),
 ("archive", self.archive_dir)
 ]:
 if path.exists():
 total_size = sum(f.stat().st_size for f in path.rglob('*') if f.is_file())
 usage[name] = round(total_size / (1024**3), 2)
 else:
 usage[name] = 0
 
 usage["total"] = sum(usage.values())
 return usage
 
 def compress_chunk_files(self, pattern: str = "*.json", compression_level: int = 9) -> Dict:
 """Compress JSON chunk files to save space"""
 
 logger.info(f"🗜 Starting compression of {pattern} files...")
 
 files_compressed = 0
 original_size = 0
 compressed_size = 0
 
 # Find all matching files
 json_files = []
 for directory in [self.data_dir, self.processed_dir]:
 if directory.exists():
 json_files.extend(directory.glob(pattern))
 
 logger.info(f" Found {len(json_files)} files to compress")
 
 for json_file in json_files:
 try:
 # Get original size
 orig_size = json_file.stat().st_size
 
 # Create compressed version
 compressed_file = self.compressed_dir / f"{json_file.stem}.json.gz"
 
 with open(json_file, 'rb') as f_in:
 with gzip.open(compressed_file, 'wb', compresslevel=compression_level) as f_out:
 shutil.copyfileobj(f_in, f_out)
 
 # Get compressed size
 comp_size = compressed_file.stat().st_size
 
 # Update stats
 original_size += orig_size
 compressed_size += comp_size
 files_compressed += 1
 
 # Remove original file after successful compression
 json_file.unlink()
 
 compression_ratio = (1 - comp_size/orig_size) * 100
 logger.info(f" {json_file.name} -> {compressed_file.name} ({compression_ratio:.1f}% reduction)")
 
 except Exception as e:
 logger.error(f" Error compressing {json_file}: {e}")
 
 # Calculate final stats
 total_saved = original_size - compressed_size
 overall_ratio = (total_saved / original_size) * 100 if original_size > 0 else 0
 
 results = {
 "files_compressed": files_compressed,
 "original_size_gb": round(original_size / (1024**3), 2),
 "compressed_size_gb": round(compressed_size / (1024**3), 2), 
 "space_saved_gb": round(total_saved / (1024**3), 2),
 "compression_ratio": round(overall_ratio, 1)
 }
 
 logger.info(f"\n Compression completed!")
 logger.info(f" Files processed: {files_compressed}")
 logger.info(f" Original size: {results['original_size_gb']:.2f}GB")
 logger.info(f"📦 Compressed size: {results['compressed_size_gb']:.2f}GB")
 logger.info(f" Space saved: {results['space_saved_gb']:.2f}GB ({results['compression_ratio']:.1f}%)")
 
 return results
 
 def create_efficient_sample(self, sample_size_gb: float = 5.0) -> Dict:
 """Create a high-quality sample dataset from compressed files"""
 
 logger.info(f" Creating {sample_size_gb}GB sample dataset...")
 
 sample_dir = self.base_dir / "sample"
 sample_dir.mkdir(exist_ok=True)
 
 # Find compressed files
 compressed_files = list(self.compressed_dir.glob("*.json.gz"))
 
 if not compressed_files:
 logger.warning(" No compressed files found!")
 return {"error": "No compressed files available"}
 
 logger.info(f" Found {len(compressed_files)} compressed files")
 
 target_size_bytes = int(sample_size_gb * 1024**3)
 current_size = 0
 sample_transactions = []
 files_used = 0
 
 # Process files until we reach target size
 for comp_file in compressed_files:
 if current_size >= target_size_bytes:
 break
 
 try:
 logger.info(f" Reading {comp_file.name}...")
 
 with gzip.open(comp_file, 'rt', encoding='utf-8') as f:
 data = json.load(f)
 
 # Add transactions from this file
 for transaction in data:
 sample_transactions.append(transaction)
 
 # Estimate current size (rough calculation)
 current_size = len(json.dumps(sample_transactions, ensure_ascii=False).encode('utf-8'))
 
 if current_size >= target_size_bytes:
 break
 
 files_used += 1
 
 except Exception as e:
 logger.error(f" Error reading {comp_file}: {e}")
 
 # Save sample dataset
 sample_file = sample_dir / f"vietnamese_financial_sample_{sample_size_gb}gb.json"
 
 with open(sample_file, 'w', encoding='utf-8') as f:
 json.dump(sample_transactions, f, ensure_ascii=False, indent=1)
 
 # Get actual file size
 actual_size_gb = round(sample_file.stat().st_size / (1024**3), 3)
 
 results = {
 "sample_file": str(sample_file),
 "target_size_gb": sample_size_gb,
 "actual_size_gb": actual_size_gb,
 "transactions_count": len(sample_transactions),
 "files_used": files_used,
 "efficiency": round((actual_size_gb / sample_size_gb) * 100, 1)
 }
 
 logger.info(f" Sample created: {results['actual_size_gb']}GB with {results['transactions_count']:,} transactions")
 
 return results
 
 def cleanup_space(self, keep_sample: bool = True, keep_compressed: bool = True) -> Dict:
 """Aggressive space cleanup while preserving essential data"""
 
 logger.info("🧹 Starting aggressive space cleanup...")
 
 cleaned_size = 0
 files_removed = 0
 
 # Remove uncompressed raw data files (keeping compressed versions)
 if keep_compressed:
 raw_patterns = [
 self.data_dir / "transactions_controlled_chunk_*.json",
 self.processed_dir / "processed_transactions_chunk_*.json"
 ]
 
 for pattern in raw_patterns:
 for file_path in pattern.parent.glob(pattern.name):
 try:
 file_size = file_path.stat().st_size
 file_path.unlink()
 cleaned_size += file_size
 files_removed += 1
 logger.info(f"🗑 Removed {file_path.name} ({file_size/(1024**2):.1f}MB)")
 except Exception as e:
 logger.error(f" Error removing {file_path}: {e}")
 
 # Remove temporary files
 temp_patterns = ["*.tmp", "*.temp", "*.log", "*.cache"]
 for pattern in temp_patterns:
 for temp_file in self.base_dir.rglob(pattern):
 try:
 file_size = temp_file.stat().st_size
 temp_file.unlink()
 cleaned_size += file_size
 files_removed += 1
 except Exception:
 pass
 
 results = {
 "files_removed": files_removed,
 "space_freed_gb": round(cleaned_size / (1024**3), 2)
 }
 
 logger.info(f" Cleanup completed: {results['files_removed']} files, {results['space_freed_gb']}GB freed")
 
 return results
 
 def get_storage_report(self) -> Dict:
 """Generate comprehensive storage report"""
 
 usage = self.get_disk_usage()
 
 report = {
 "timestamp": str(Path(__file__).stat().st_mtime),
 "disk_usage": usage,
 "recommendations": []
 }
 
 # Generate recommendations
 total_size = usage["total"]
 
 if total_size > 60: # More than 60GB
 report["recommendations"].append(" CRITICAL: Dataset exceeds 60GB - immediate compression needed")
 elif total_size > 40: # More than 40GB
 report["recommendations"].append(" WARNING: Dataset exceeds 40GB - consider compression")
 
 if usage["raw_data"] > usage["compressed"]:
 report["recommendations"].append(" TIP: Raw data larger than compressed - run compression")
 
 if usage["processed"] > 20:
 report["recommendations"].append("📦 TIP: Processed data >20GB - consider archiving")
 
 return report

def main():
 """Main storage management interface"""
 
 manager = IntelligentStorageManager()
 
 print(" Vietnamese Financial Dataset - Storage Manager")
 print("=" * 60)
 
 # Get current status
 report = manager.get_storage_report()
 usage = report["disk_usage"]
 
 print(f" Current Storage Usage:")
 print(f" Raw Data: {usage['raw_data']:.2f}GB")
 print(f" Processed: {usage['processed']:.2f}GB") 
 print(f" Compressed: {usage['compressed']:.2f}GB")
 print(f" Archive: {usage['archive']:.2f}GB")
 print(f" TOTAL: {usage['total']:.2f}GB")
 
 print(f"\n Recommendations:")
 for rec in report["recommendations"]:
 print(f" {rec}")
 
 print(f"\n🤔 What would you like to do?")
 print("1. Compress all JSON files (save 70-80% space)")
 print("2. Create 5GB sample dataset")
 print("3. Aggressive cleanup (remove uncompressed files)")
 print("4. Full optimization (compress + cleanup + sample)")
 print("5. Exit")
 
 choice = input("\nEnter your choice (1-5): ").strip()
 
 if choice == "1":
 manager.compress_chunk_files()
 elif choice == "2":
 manager.create_efficient_sample(5.0)
 elif choice == "3":
 manager.cleanup_space()
 elif choice == "4":
 print(" Running full optimization...")
 manager.compress_chunk_files()
 manager.create_efficient_sample(5.0)
 manager.cleanup_space()
 print(" Full optimization completed!")
 else:
 print("👋 Goodbye!")

if __name__ == "__main__":
 main()