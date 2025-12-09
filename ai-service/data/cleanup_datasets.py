#!/usr/bin/env python3
"""
Dataset Cleanup and Management Script
Cleans old dataset files and manages storage space
"""

import os
import glob
from pathlib import Path
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def cleanup_old_datasets():
    """Remove old uncontrolled dataset files"""
    
    # Files to clean
    patterns = [
        "transactions_massive_chunk_*.json",
        "financial_knowledge_*.json", 
        "sample_transactions_*.json"
    ]
    
    total_freed = 0
    files_removed = 0
    
    logger.info("🧹 Starting dataset cleanup...")
    
    for pattern in patterns:
        files = glob.glob(pattern)
        for file_path in files:
            try:
                file_size = os.path.getsize(file_path)
                os.remove(file_path)
                total_freed += file_size
                files_removed += 1
                logger.info(f"🗑️ Removed: {file_path} ({file_size/(1024*1024):.1f}MB)")
            except Exception as e:
                logger.error(f"❌ Error removing {file_path}: {e}")
    
    freed_mb = total_freed / (1024 * 1024)
    freed_gb = total_freed / (1024 * 1024 * 1024)
    
    logger.info(f"\n✅ Cleanup completed:")
    logger.info(f"📁 Files removed: {files_removed}")
    logger.info(f"💾 Space freed: {freed_mb:.1f}MB ({freed_gb:.3f}GB)")
    
    return files_removed, freed_gb

def get_current_dataset_info():
    """Get information about current controlled dataset"""
    
    controlled_files = glob.glob("transactions_controlled_chunk_*.json")
    
    if not controlled_files:
        logger.info("📊 No controlled dataset files found")
        return None
    
    total_size = 0
    for file_path in controlled_files:
        total_size += os.path.getsize(file_path)
    
    size_gb = total_size / (1024 * 1024 * 1024)
    
    logger.info(f"📊 Current controlled dataset:")
    logger.info(f"📁 Files: {len(controlled_files)}")
    logger.info(f"💾 Total size: {size_gb:.3f}GB")
    
    return {
        'files': len(controlled_files),
        'size_gb': size_gb,
        'file_list': controlled_files
    }

if __name__ == "__main__":
    print("🛠️ Dataset Management Tool")
    print("="*50)
    
    # Show current status
    get_current_dataset_info()
    
    # Ask user for confirmation
    response = input("\n🤔 Do you want to clean old uncontrolled dataset files? (y/n): ")
    
    if response.lower() in ['y', 'yes']:
        cleanup_old_datasets()
        print("\n📊 Updated dataset info:")
        get_current_dataset_info()
    else:
        print("⏭️ Cleanup skipped")
    
    print("\n✅ Done!")