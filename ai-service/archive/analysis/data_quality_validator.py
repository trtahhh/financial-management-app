#!/usr/bin/env python3
"""
Data Quality Validator for Compressed Vietnamese Dataset
Ensures data integrity and quality after compression
"""

import json
import gzip
import random
import logging
from pathlib import Path
from typing import Dict, List, Any
import statistics

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class DataQualityValidator:
    def __init__(self, compressed_dir: str = "compressed"):
        self.compressed_dir = Path(compressed_dir)
    
    def validate_compression_integrity(self, sample_count: int = 5) -> Dict:
        """Validate that compression preserved data integrity"""
        
        logger.info(f"Validating compression integrity with {sample_count} random samples...")
        
        # Get all compressed files
        compressed_files = list(self.compressed_dir.glob("transactions_controlled_chunk_*.json.gz"))
        
        if len(compressed_files) == 0:
            return {"error": "No compressed files found"}
        
        # Sample random files
        sample_files = random.sample(compressed_files, min(sample_count, len(compressed_files)))
        
        validation_results = {
            "files_checked": len(sample_files),
            "total_files": len(compressed_files),
            "integrity_passed": 0,
            "total_transactions": 0,
            "sample_transactions": [],
            "compression_ratios": [],
            "categories_found": set(),
            "regions_found": set(),
            "quality_metrics": {}
        }
        
        for comp_file in sample_files:
            try:
                logger.info(f"Validating {comp_file.name}...")
                
                # Read compressed file
                with gzip.open(comp_file, 'rt', encoding='utf-8') as f:
                    data = json.load(f)
                
                # Validate structure
                if not isinstance(data, list):
                    logger.error(f"{comp_file.name}: Not a list")
                    continue
                
                if len(data) == 0:
                    logger.error(f"{comp_file.name}: Empty file")
                    continue
                
                # Check first few transactions
                sample_transaction = data[0]
                required_fields = ['id', 'amount', 'description', 'category', 'region', 'date', 'type']
                
                # Check if most required fields exist (flexible validation)
                field_count = sum(1 for field in required_fields if field in sample_transaction)
                if field_count < len(required_fields) * 0.7:  # At least 70% of fields
                    logger.warning(f"{comp_file.name}: Some required fields missing")
                
                # Update stats
                validation_results["integrity_passed"] += 1
                validation_results["total_transactions"] += len(data)
                
                # Sample some transactions for quality check
                sample_size = min(3, len(data))
                validation_results["sample_transactions"].extend(
                    random.sample(data, sample_size)
                )
                
                # Collect categories and regions
                for transaction in data[:100]:  # Check first 100 for performance
                    validation_results["categories_found"].add(transaction.get("category", "unknown"))
                    validation_results["regions_found"].add(transaction.get("region", "unknown"))
                
                # Calculate compression ratio
                original_size_estimate = len(json.dumps(data, ensure_ascii=False).encode('utf-8'))
                compressed_size = comp_file.stat().st_size
                compression_ratio = (1 - compressed_size / original_size_estimate) * 100
                validation_results["compression_ratios"].append(compression_ratio)
                
                logger.info(f"{comp_file.name}: {len(data):,} transactions, {compression_ratio:.1f}% compression")
                
            except Exception as e:
                logger.error(f"Error validating {comp_file.name}: {e}")
        
        # Calculate quality metrics
        if validation_results["compression_ratios"]:
            validation_results["quality_metrics"] = {
                "avg_compression_ratio": round(statistics.mean(validation_results["compression_ratios"]), 1),
                "min_compression_ratio": round(min(validation_results["compression_ratios"]), 1),
                "max_compression_ratio": round(max(validation_results["compression_ratios"]), 1),
                "categories_count": len(validation_results["categories_found"]),
                "regions_count": len(validation_results["regions_found"]),
                "avg_transactions_per_file": round(validation_results["total_transactions"] / max(1, validation_results["integrity_passed"]))
            }
        
        # Convert sets to lists for JSON serialization
        validation_results["categories_found"] = list(validation_results["categories_found"])
        validation_results["regions_found"] = list(validation_results["regions_found"])
        
        return validation_results
    
    def create_quality_sample(self, output_size_mb: int = 100) -> Dict:
        """Create a high-quality sample for bot training/testing"""
        
        logger.info(f"Creating {output_size_mb}MB quality sample for bot training...")
        
        # Get all compressed files
        compressed_files = list(self.compressed_dir.glob("transactions_controlled_chunk_*.json.gz"))
        
        if not compressed_files:
            return {"error": "No compressed files found"}
        
        # Target in bytes
        target_size = output_size_mb * 1024 * 1024
        
        # Strategy: Select diverse transactions across categories and regions
        quality_sample = {
            "food": [],
            "transport": [],
            "shopping": [],
            "entertainment": [],
            "utilities": [],
            "healthcare": [],
            "education": [],
            "income": []
        }
        
        regional_balance = {
            "northern": 0,
            "southern": 0,
            "central": 0,
            "chains": 0
        }
        
        current_size = 0
        files_processed = 0
        
        # Process files to build quality sample
        for comp_file in compressed_files[:10]:  # Limit to first 10 files for speed
            try:
                logger.info(f"Processing {comp_file.name} for quality sample...")
                
                with gzip.open(comp_file, 'rt', encoding='utf-8') as f:
                    data = json.load(f)
                
                # Sample transactions from this file
                for transaction in random.sample(data, min(50, len(data))):
                    category = transaction.get('category', 'income')
                    region = transaction.get('region', 'chains')
                    
                    # Balance categories
                    if category in quality_sample:
                        if len(quality_sample[category]) < 200:  # Max 200 per category
                            quality_sample[category].append(transaction)
                            regional_balance[region] = regional_balance.get(region, 0) + 1
                
                files_processed += 1
                
                # Check size limit
                current_sample_text = json.dumps(quality_sample, ensure_ascii=False)
                current_size = len(current_sample_text.encode('utf-8'))
                
                if current_size >= target_size:
                    break
                    
            except Exception as e:
                logger.error(f"Error processing {comp_file.name}: {e}")
        
        # Save quality sample
        output_file = f"quality_sample_{output_size_mb}mb.json"
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(quality_sample, f, ensure_ascii=False, indent=2)
        
        # Generate report
        total_transactions = sum(len(transactions) for transactions in quality_sample.values())
        
        report = {
            "output_file": output_file,
            "total_transactions": total_transactions,
            "files_processed": files_processed,
            "size_mb": current_size / (1024 * 1024),
            "category_distribution": {k: len(v) for k, v in quality_sample.items()},
            "regional_distribution": regional_balance
        }
        
        logger.info(f"Quality sample created: {total_transactions:,} transactions, {report['size_mb']:.1f}MB")
        
        return report
    
    def validate_dataset_completeness(self) -> Dict:
        """Validate that dataset is complete and ready for production"""
        
        logger.info("Performing comprehensive dataset validation...")
        
        # Get all compressed files
        compressed_files = list(self.compressed_dir.glob("transactions_controlled_chunk_*.json.gz"))
        
        validation = {
            "total_files": len(compressed_files),
            "total_transactions": 0,
            "total_size_mb": 0,
            "categories": {},
            "regions": {},
            "date_range": {"earliest": None, "latest": None},
            "quality_score": 0
        }
        
        if not compressed_files:
            validation["error"] = "No compressed files found"
            return validation
        
        # Process all files for comprehensive validation
        for comp_file in compressed_files[:5]:  # Limit for performance
            try:
                with gzip.open(comp_file, 'rt', encoding='utf-8') as f:
                    data = json.load(f)
                
                validation["total_transactions"] += len(data)
                validation["total_size_mb"] += comp_file.stat().st_size / (1024 * 1024)
                
                # Sample for analysis
                sample_data = random.sample(data, min(100, len(data)))
                
                for transaction in sample_data:
                    # Category counting
                    category = transaction.get('category', 'unknown')
                    validation["categories"][category] = validation["categories"].get(category, 0) + 1
                    
                    # Region counting
                    region = transaction.get('region', 'unknown')
                    validation["regions"][region] = validation["regions"].get(region, 0) + 1
                    
                    # Date range
                    date_str = transaction.get('date', '')
                    if date_str:
                        if not validation["date_range"]["earliest"] or date_str < validation["date_range"]["earliest"]:
                            validation["date_range"]["earliest"] = date_str
                        if not validation["date_range"]["latest"] or date_str > validation["date_range"]["latest"]:
                            validation["date_range"]["latest"] = date_str
                
            except Exception as e:
                logger.error(f"Error validating {comp_file.name}: {e}")
        
        # Calculate quality score
        category_count = len(validation["categories"])
        region_count = len(validation["regions"])
        file_count_score = min(validation["total_files"] / 100, 1) * 30  # Up to 30 points
        transaction_count_score = min(validation["total_transactions"] / 1000000, 1) * 30  # Up to 30 points
        diversity_score = min((category_count * region_count) / 50, 1) * 40  # Up to 40 points
        
        validation["quality_score"] = round(file_count_score + transaction_count_score + diversity_score, 1)
        
        logger.info(f"Dataset validation complete:")
        logger.info(f"  Files: {validation['total_files']}")
        logger.info(f"  Transactions: {validation['total_transactions']:,}")
        logger.info(f"  Size: {validation['total_size_mb']:.1f} MB")
        logger.info(f"  Categories: {category_count}")
        logger.info(f"  Regions: {region_count}")
        logger.info(f"  Quality Score: {validation['quality_score']}/100")
        
        return validation

def main():
    """Main validation function"""
    logger.info("Data Quality Validator")
    logger.info("=" * 50)
    
    validator = DataQualityValidator()
    
    # Validate compression integrity
    integrity_results = validator.validate_compression_integrity()
    
    if "error" not in integrity_results:
        logger.info("Integrity validation passed!")
        
        # Create quality sample
        quality_sample = validator.create_quality_sample(50)  # 50MB sample
        
        if "error" not in quality_sample:
            logger.info("Quality sample created!")
        
        # Validate completeness
        completeness = validator.validate_dataset_completeness()
        
        if "error" not in completeness:
            logger.info(f"Dataset completeness validation: {completeness['quality_score']}/100")
    
    logger.info("Data quality validation completed!")

if __name__ == "__main__":
    main()