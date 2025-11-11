#!/usr/bin/env python3#!/usr/bin/env python3#!/usr/bin/env python3

"""

Mass Dataset Processor for 50GB Vietnamese Financial Data""""""

"""

Mass Dataset Processor for 50GB Vietnamese Financial DataMass Dataset Processor for 50GB Vietnamese Financial Data

import os

import jsonProcesses the massive controlled dataset with Vietnamese NLP classificationProcesses the massive controlled dataset with Vietnamese NLP classification

import logging

from typing import List, Dict, Any""""""

import glob

from datetime import datetime

from concurrent.futures import ProcessPoolExecutor

import multiprocessing as mpimport osimport os



# Setup loggingimport jsonimport json

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

logger = logging.getLogger(__name__)import loggingimport logging



class MassDatasetProcessor:from typing import List, Dict, Anyfrom typing import List, Dict, Any

    """

    Process the massive 50GB Vietnamese dataset with AI classificationimport globimport glob

    """

    from datetime import datetimefrom datetime import datetime

    def __init__(self, data_dir: str = "./data", output_dir: str = "./processed", batch_size: int = 5000):

        self.data_dir = data_dirfrom concurrent.futures import ProcessPoolExecutorfrom concurrent.futures import ProcessPoolExecutor

        self.output_dir = output_dir

        self.batch_size = batch_sizeimport multiprocessing as mpimport multiprocessing as mp

        

        # Create output directoryfrom simple_vietnamese_nlp import SimpleVietnameseNLPProcessorfrom simple_vietnamese_nlp import SimpleVietnameseNLPProcessor

        os.makedirs(output_dir, exist_ok=True)

        

        logger.info(f"Initializing Mass Dataset Processor...")

        logger.info(f"Data directory: {data_dir}")# Setup logging# Setup logging

        logger.info(f"Output directory: {output_dir}")

        logger.info(f"Batch size: {batch_size:,}")logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

    

    def process_single_chunk(self, chunk_file: str) -> Dict[str, Any]:logger = logging.getLogger(__name__)logger = logging.getLogger(__name__)

        """Process a single chunk file"""

        chunk_name = os.path.basename(chunk_file)

        logger.info(f"Processing {chunk_name}...")

        class MassDatasetProcessor:class MassDatasetProcessor:

        try:

            # Load chunk    """ """

            with open(chunk_file, 'r', encoding='utf-8') as f:

                transactions = json.load(f)    Process the massive 50GB Vietnamese dataset with AI classification Process the massive 50GB Vietnamese dataset with AI classification

            

            chunk_size = len(transactions)    """ """

            logger.info(f"Loaded {chunk_size:,} transactions from {chunk_name}")

                 

            # Process in smaller batches

            processed_transactions = []    def __init__(self, data_dir: str = "./data", output_dir: str = "./processed", batch_size: int = 5000): def __init__(self, data_dir: str = "./data", output_dir: str = "./processed", batch_size: int = 5000):

            for i in range(0, chunk_size, self.batch_size):

                batch = transactions[i:i + self.batch_size]        self.data_dir = data_dir self.data_dir = data_dir

                # Simulate processing

                for transaction in batch:        self.output_dir = output_dir self.output_dir = output_dir

                    transaction['ai_category'] = 'processed'

                    transaction['ai_confidence'] = 0.95        self.batch_size = batch_size self.batch_size = batch_size

                processed_transactions.extend(batch)

                        self.processor = SimpleVietnameseNLPProcessor() self.processor = SimpleVietnameseNLPProcessor()

                logger.info(f"Processed batch {i//self.batch_size + 1}/{(chunk_size-1)//self.batch_size + 1} for {chunk_name}")

                     

            # Generate output filename

            chunk_num = chunk_name.replace('transactions_controlled_chunk_', '').replace('.json', '')        # Create output directory # Create output directory

            output_file = os.path.join(self.output_dir, f"processed_transactions_chunk_{chunk_num}.json")

                    os.makedirs(output_dir, exist_ok=True) os.makedirs(output_dir, exist_ok=True)

            # Save processed chunk

            with open(output_file, 'w', encoding='utf-8') as f:         

                json.dump(processed_transactions, f, ensure_ascii=False, indent=1)

                    logger.info(f"Initializing Mass Dataset Processor...") logger.info(f" Initializing Mass Dataset Processor...")

            file_size_mb = os.path.getsize(output_file) / (1024 * 1024)

                    logger.info(f"Data directory: {data_dir}") logger.info(f" Data directory: {data_dir}")

            logger.info(f"Saved {output_file} ({file_size_mb:.1f}MB)")

                    logger.info(f"Output directory: {output_dir}") logger.info(f" Output directory: {output_dir}")

            return {

                'chunk_file': chunk_name,        logger.info(f"Batch size: {batch_size:,}") logger.info(f" Batch size: {batch_size:,}")

                'output_file': output_file,

                'transactions_processed': len(processed_transactions),     

                'file_size_mb': file_size_mb,

                'status': 'success'    def process_single_chunk(self, chunk_file: str) -> Dict[str, Any]: def process_single_chunk(self, chunk_file: str) -> Dict[str, Any]:

            }

                    """ """

        except Exception as e:

            logger.error(f"Error processing {chunk_name}: {e}")        Process a single chunk file Process a single chunk file

            return {

                'chunk_file': chunk_name,         

                'error': str(e),

                'status': 'error'        Args: Args:

            }

            chunk_file: Path to chunk file chunk_file: Path to chunk file

def main():

    """Main processing function"""         

    print("Mass Vietnamese Dataset Processor")

    print("=" * 50)        Returns: Returns:

    

    # Initialize processor            Processing results Processing results

    processor = MassDatasetProcessor(

        data_dir="./data",        """ """

        output_dir="./processed", 

        batch_size=2500        chunk_name = os.path.basename(chunk_file) chunk_name = os.path.basename(chunk_file)

    )

            logger.info(f"Processing {chunk_name}...") logger.info(f" Processing {chunk_name}...")

    logger.info("Mass Dataset Processor ready!")

         

if __name__ == "__main__":

    main()        try: try:

            # Load chunk # Load chunk

            with open(chunk_file, 'r', encoding='utf-8') as f: with open(chunk_file, 'r', encoding='utf-8') as f:

                transactions = json.load(f) transactions = json.load(f)

             

            chunk_size = len(transactions) chunk_size = len(transactions)

            logger.info(f"Loaded {chunk_size:,} transactions from {chunk_name}") logger.info(f" Loaded {chunk_size:,} transactions from {chunk_name}")

             

            # Process in smaller batches # Process in smaller batches

            processed_transactions = [] processed_transactions = []

            for i in range(0, chunk_size, self.batch_size): for i in range(0, chunk_size, self.batch_size):

                batch = transactions[i:i + self.batch_size] batch = transactions[i:i + self.batch_size]

                processed_batch = self.processor.process_transaction_batch(batch) processed_batch = self.processor.process_transaction_batch(batch)

                processed_transactions.extend(processed_batch) processed_transactions.extend(processed_batch)

                 

                logger.info(f"Processed batch {i//self.batch_size + 1}/{(chunk_size-1)//self.batch_size + 1} for {chunk_name}") logger.info(f" Processed batch {i//self.batch_size + 1}/{(chunk_size-1)//self.batch_size + 1} for {chunk_name}")

             

            # Generate output filename # Generate output filename

            chunk_num = chunk_name.replace('transactions_controlled_chunk_', '').replace('.json', '') chunk_num = chunk_name.replace('transactions_controlled_chunk_', '').replace('.json', '')

            output_file = os.path.join(self.output_dir, f"processed_transactions_chunk_{chunk_num}.json") output_file = os.path.join(self.output_dir, f"processed_transactions_chunk_{chunk_num}.json")

             

            # Save processed chunk # Save processed chunk

            with open(output_file, 'w', encoding='utf-8') as f: with open(output_file, 'w', encoding='utf-8') as f:

                json.dump(processed_transactions, f, ensure_ascii=False, indent=1) json.dump(processed_transactions, f, ensure_ascii=False, indent=1)

             

            file_size_mb = os.path.getsize(output_file) / (1024 * 1024) file_size_mb = os.path.getsize(output_file) / (1024 * 1024)

             

            logger.info(f"Saved {output_file} ({file_size_mb:.1f}MB)") logger.info(f" Saved {output_file} ({file_size_mb:.1f}MB)")

             

            return { return {

                'chunk_file': chunk_name, 'chunk_file': chunk_name,

                'output_file': output_file, 'output_file': output_file,

                'transactions_processed': len(processed_transactions), 'transactions_processed': len(processed_transactions),

                'file_size_mb': file_size_mb, 'file_size_mb': file_size_mb,

                'status': 'success' 'status': 'success'

            } }

             

        except Exception as e: except Exception as e:

            logger.error(f"Error processing {chunk_name}: {e}") logger.error(f" Error processing {chunk_name}: {e}")

            return { return {

                'chunk_file': chunk_name, 'chunk_file': chunk_name,

                'error': str(e), 'error': str(e),

                'status': 'error' 'status': 'error'

            } }

     

    def get_processing_stats(self) -> Dict[str, Any]: def get_processing_stats(self) -> Dict[str, Any]:

        """Get current processing statistics""" """Get current processing statistics"""

         

        # Count source files # Count source files

        source_pattern = os.path.join(self.data_dir, "transactions_controlled_chunk_*.json") source_pattern = os.path.join(self.data_dir, "transactions_controlled_chunk_*.json")

        source_files = glob.glob(source_pattern) source_files = glob.glob(source_pattern)

         

        # Count processed files  # Count processed files 

        processed_pattern = os.path.join(self.output_dir, "processed_transactions_chunk_*.json") processed_pattern = os.path.join(self.output_dir, "processed_transactions_chunk_*.json")

        processed_files = glob.glob(processed_pattern) processed_files = glob.glob(processed_pattern)

         

        # Calculate sizes # Calculate sizes

        source_size = sum(os.path.getsize(f) for f in source_files) / (1024**3)  # GB source_size = sum(os.path.getsize(f) for f in source_files) / (1024**3) # GB

        processed_size = sum(os.path.getsize(f) for f in processed_files) / (1024**3) if processed_files else 0  # GB processed_size = sum(os.path.getsize(f) for f in processed_files) / (1024**3) if processed_files else 0 # GB

         

        return { return {

            'source_files': len(source_files), 'source_files': len(source_files),

            'processed_files': len(processed_files), 'processed_files': len(processed_files),

            'source_size_gb': source_size, 'source_size_gb': source_size,

            'processed_size_gb': processed_size, 'processed_size_gb': processed_size,

            'completion_percentage': (len(processed_files) / len(source_files) * 100) if source_files else 0 'completion_percentage': (len(processed_files) / len(source_files) * 100) if source_files else 0

        } }

     

    def process_all_chunks(self, max_workers: int = None): def process_all_chunks(self, max_workers: int = None):

        """ """

        Process all chunk files in the dataset Process all chunk files in the dataset

         

        Args: Args:

            max_workers: Maximum number of worker processes max_workers: Maximum number of worker processes

        """ """

        # Find all controlled dataset files # Find all controlled dataset files

        pattern = os.path.join(self.data_dir, "transactions_controlled_chunk_*.json") pattern = os.path.join(self.data_dir, "transactions_controlled_chunk_*.json")

        chunk_files = sorted(glob.glob(pattern)) chunk_files = sorted(glob.glob(pattern))

         

        if not chunk_files: if not chunk_files:

            logger.error(f"No chunk files found in {self.data_dir}") logger.error(f" No chunk files found in {self.data_dir}")

            return return

         

        logger.info(f"Found {len(chunk_files)} chunk files to process") logger.info(f" Found {len(chunk_files)} chunk files to process")

         

        # Get initial stats # Get initial stats

        stats = self.get_processing_stats() stats = self.get_processing_stats()

        logger.info(f"Source dataset: {stats['source_files']} files, {stats['source_size_gb']:.3f}GB") logger.info(f" Source dataset: {stats['source_files']} files, {stats['source_size_gb']:.3f}GB")

        logger.info(f"Already processed: {stats['processed_files']} files, {stats['processed_size_gb']:.3f}GB") logger.info(f" Already processed: {stats['processed_files']} files, {stats['processed_size_gb']:.3f}GB")

         

        # Filter out already processed files # Filter out already processed files

        processed_chunks = set() processed_chunks = set()

        for pf in glob.glob(os.path.join(self.output_dir, "processed_transactions_chunk_*.json")): for pf in glob.glob(os.path.join(self.output_dir, "processed_transactions_chunk_*.json")):

            chunk_num = os.path.basename(pf).replace('processed_transactions_chunk_', '').replace('.json', '') chunk_num = os.path.basename(pf).replace('processed_transactions_chunk_', '').replace('.json', '')

            processed_chunks.add(f"transactions_controlled_chunk_{chunk_num}.json") processed_chunks.add(f"transactions_controlled_chunk_{chunk_num}.json")

         

        remaining_files = [f for f in chunk_files if os.path.basename(f) not in processed_chunks] remaining_files = [f for f in chunk_files if os.path.basename(f) not in processed_chunks]

         

        if not remaining_files: if not remaining_files:

            logger.info("All chunks already processed!") logger.info(" All chunks already processed!")

            return return

         

        logger.info(f"Remaining to process: {len(remaining_files)} files") logger.info(f"⏳ Remaining to process: {len(remaining_files)} files")

         

        # Determine number of workers # Determine number of workers

        if max_workers is None: if max_workers is None:

            max_workers = min(mp.cpu_count(), 4)  # Don't overwhelm the system max_workers = min(mp.cpu_count(), 4) # Don't overwhelm the system

         

        logger.info(f"Using {max_workers} worker processes") logger.info(f"👥 Using {max_workers} worker processes")

         

        # Process chunks # Process chunks

        start_time = datetime.now() start_time = datetime.now()

        total_processed = 0 total_processed = 0

        successful = 0 successful = 0

        failed = 0 failed = 0

         

        # Process in single thread for now to avoid memory issues # Process in single thread for now to avoid memory issues

        for i, chunk_file in enumerate(remaining_files): for i, chunk_file in enumerate(remaining_files):

            logger.info(f"\nProcessing chunk {i+1}/{len(remaining_files)}: {os.path.basename(chunk_file)}") logger.info(f"\n Processing chunk {i+1}/{len(remaining_files)}: {os.path.basename(chunk_file)}")

             

            result = self.process_single_chunk(chunk_file) result = self.process_single_chunk(chunk_file)

             

            if result['status'] == 'success': if result['status'] == 'success':

                total_processed += result['transactions_processed'] total_processed += result['transactions_processed']

                successful += 1 successful += 1

                logger.info(f"Success: {result['transactions_processed']:,} transactions") logger.info(f" Success: {result['transactions_processed']:,} transactions")

            else: else:

                failed += 1 failed += 1

                logger.error(f"Failed: {result.get('error', 'Unknown error')}") logger.error(f" Failed: {result.get('error', 'Unknown error')}")

             

            # Progress update # Progress update

            progress = ((i + 1) / len(remaining_files)) * 100 progress = ((i + 1) / len(remaining_files)) * 100

            elapsed = datetime.now() - start_time elapsed = datetime.now() - start_time

             

            logger.info(f"Progress: {progress:.1f}% ({i+1}/{len(remaining_files)})") logger.info(f" Progress: {progress:.1f}% ({i+1}/{len(remaining_files)})")

            logger.info(f"Elapsed: {elapsed}") logger.info(f"⏱ Elapsed: {elapsed}")

            logger.info(f"Total processed: {total_processed:,} transactions") logger.info(f" Total processed: {total_processed:,} transactions")

             

            # Update stats periodically # Update stats periodically

            if (i + 1) % 10 == 0: if (i + 1) % 10 == 0:

                current_stats = self.get_processing_stats() current_stats = self.get_processing_stats()

                logger.info(f"Current processed size: {current_stats['processed_size_gb']:.3f}GB") logger.info(f" Current processed size: {current_stats['processed_size_gb']:.3f}GB")

         

        # Final statistics # Final statistics

        end_time = datetime.now() end_time = datetime.now()

        total_time = end_time - start_time total_time = end_time - start_time

         

        final_stats = self.get_processing_stats() final_stats = self.get_processing_stats()

         

        logger.info("\n" + "="*80) logger.info("\n" + "="*80)

        logger.info("MASS DATASET PROCESSING COMPLETED!") logger.info(" MASS DATASET PROCESSING COMPLETED!")

        logger.info(f"Total time: {total_time}") logger.info(f"⏱ Total time: {total_time}")

        logger.info(f"Successful chunks: {successful}") logger.info(f" Successful chunks: {successful}")

        logger.info(f"Failed chunks: {failed}") logger.info(f" Failed chunks: {failed}")

        logger.info(f"Total transactions processed: {total_processed:,}") logger.info(f" Total transactions processed: {total_processed:,}")

        logger.info(f"Total processed files: {final_stats['processed_files']}") logger.info(f" Total processed files: {final_stats['processed_files']}")

        logger.info(f"Final processed size: {final_stats['processed_size_gb']:.3f}GB") logger.info(f" Final processed size: {final_stats['processed_size_gb']:.3f}GB")

        logger.info(f"Completion: {final_stats['completion_percentage']:.1f}%") logger.info(f" Completion: {final_stats['completion_percentage']:.1f}%")

        logger.info("="*80) logger.info("="*80)

         

        return { return {

            'total_processed': total_processed, 'total_processed': total_processed,

            'successful_chunks': successful, 'successful_chunks': successful,

            'failed_chunks': failed, 'failed_chunks': failed,

            'processing_time': total_time, 'processing_time': total_time,

            'final_stats': final_stats 'final_stats': final_stats

        } }

     

    def validate_processed_data(self, sample_size: int = 1000): def validate_processed_data(self, sample_size: int = 1000):

        """ """

        Validate processed data quality Validate processed data quality

         

        Args: Args:

            sample_size: Number of transactions to validate sample_size: Number of transactions to validate

        """ """

        logger.info(f"Validating processed data quality (sample: {sample_size})...") logger.info(f" Validating processed data quality (sample: {sample_size})...")

         

        # Get processed files # Get processed files

        processed_files = glob.glob(os.path.join(self.output_dir, "processed_transactions_chunk_*.json")) processed_files = glob.glob(os.path.join(self.output_dir, "processed_transactions_chunk_*.json"))

         

        if not processed_files: if not processed_files:

            logger.error("No processed files found for validation") logger.error(" No processed files found for validation")

            return return

         

        # Sample from random files # Sample from random files

        import random import random

        sample_files = random.sample(processed_files, min(5, len(processed_files))) sample_files = random.sample(processed_files, min(5, len(processed_files)))

         

        total_sampled = 0 total_sampled = 0

        category_counts = {} category_counts = {}

        confidence_scores = [] confidence_scores = []

        entity_stats = {'amounts': 0, 'merchants': 0, 'locations': 0} entity_stats = {'amounts': 0, 'merchants': 0, 'locations': 0}

         

        for file_path in sample_files: for file_path in sample_files:

            with open(file_path, 'r', encoding='utf-8') as f: with open(file_path, 'r', encoding='utf-8') as f:

                transactions = json.load(f) transactions = json.load(f)

             

            # Sample transactions from this file # Sample transactions from this file

            sample_count = min(sample_size // len(sample_files), len(transactions)) sample_count = min(sample_size // len(sample_files), len(transactions))

            sample_transactions = random.sample(transactions, sample_count) sample_transactions = random.sample(transactions, sample_count)

             

            for transaction in sample_transactions: for transaction in sample_transactions:

                total_sampled += 1 total_sampled += 1

                 

                # Count categories # Count categories

                category = transaction.get('ai_category', 'unknown') category = transaction.get('ai_category', 'unknown')

                category_counts[category] = category_counts.get(category, 0) + 1 category_counts[category] = category_counts.get(category, 0) + 1

                 

                # Collect confidence scores # Collect confidence scores

                confidence = transaction.get('ai_confidence', 0) confidence = transaction.get('ai_confidence', 0)

                confidence_scores.append(confidence) confidence_scores.append(confidence)

                 

                # Count entities # Count entities

                entities = transaction.get('extracted_entities', {}) entities = transaction.get('extracted_entities', {})

                for entity_type in entity_stats: for entity_type in entity_stats:

                    if entities.get(entity_type): if entities.get(entity_type):

                        entity_stats[entity_type] += 1 entity_stats[entity_type] += 1

         

        # Calculate statistics # Calculate statistics

        avg_confidence = sum(confidence_scores) / len(confidence_scores) if confidence_scores else 0 avg_confidence = sum(confidence_scores) / len(confidence_scores) if confidence_scores else 0

         

        logger.info("\nVALIDATION RESULTS:") logger.info("\n VALIDATION RESULTS:")

        logger.info(f"Samples validated: {total_sampled:,}") logger.info(f" Samples validated: {total_sampled:,}")

        logger.info(f"Average confidence: {avg_confidence:.3f}") logger.info(f" Average confidence: {avg_confidence:.3f}")

         

        logger.info("\nCategory distribution:") logger.info("\n Category distribution:")

        for category, count in sorted(category_counts.items(), key=lambda x: x[1], reverse=True): for category, count in sorted(category_counts.items(), key=lambda x: x[1], reverse=True):

            percentage = (count / total_sampled) * 100 percentage = (count / total_sampled) * 100

            logger.info(f"  {category}: {count} ({percentage:.1f}%)") logger.info(f" {category}: {count} ({percentage:.1f}%)")

         

        logger.info("\nEntity extraction stats:") logger.info("\n Entity extraction stats:")

        for entity_type, count in entity_stats.items(): for entity_type, count in entity_stats.items():

            percentage = (count / total_sampled) * 100 percentage = (count / total_sampled) * 100

            logger.info(f"  {entity_type}: {count}/{total_sampled} ({percentage:.1f}%)") logger.info(f" {entity_type}: {count}/{total_sampled} ({percentage:.1f}%)")

         

        return { return {

            'samples_validated': total_sampled, 'samples_validated': total_sampled,

            'average_confidence': avg_confidence, 'average_confidence': avg_confidence,

            'category_distribution': category_counts, 'category_distribution': category_counts,

            'entity_stats': entity_stats 'entity_stats': entity_stats

        } }



def main():def main():

    """Main processing function""" """Main processing function"""

    print("Mass Vietnamese Dataset Processor") print(" Mass Vietnamese Dataset Processor")

    print("=" * 50) print("=" * 50)

     

    # Initialize processor # Initialize processor

    processor = MassDatasetProcessor( processor = MassDatasetProcessor(

        data_dir="./data", data_dir="./data",

        output_dir="./processed",  output_dir="./processed", 

        batch_size=2500  # Smaller batches for stability batch_size=2500 # Smaller batches for stability

    ) )

     

    # Get current stats # Get current stats

    stats = processor.get_processing_stats() stats = processor.get_processing_stats()

    print(f"\nCurrent Status:") print(f"\n Current Status:")

    print(f"Source files: {stats['source_files']}") print(f"Source files: {stats['source_files']}")

    print(f"Source size: {stats['source_size_gb']:.3f}GB")  print(f"Source size: {stats['source_size_gb']:.3f}GB") 

    print(f"Processed files: {stats['processed_files']}") print(f"Processed files: {stats['processed_files']}")

    print(f"Processed size: {stats['processed_size_gb']:.3f}GB") print(f"Processed size: {stats['processed_size_gb']:.3f}GB")

    print(f"Completion: {stats['completion_percentage']:.1f}%") print(f"Completion: {stats['completion_percentage']:.1f}%")

     

    if stats['completion_percentage'] >= 100: if stats['completion_percentage'] >= 100:

        print("\nDataset already fully processed!") print("\n Dataset already fully processed!")

         

        # Run validation # Run validation

        response = input("\nRun data validation? (y/n): ") response = input("\n🤔 Run data validation? (y/n): ")

        if response.lower() in ['y', 'yes']: if response.lower() in ['y', 'yes']:

            processor.validate_processed_data() processor.validate_processed_data()

         

        return return

     

    # Ask user to proceed # Ask user to proceed

    remaining = stats['source_files'] - stats['processed_files'] remaining = stats['source_files'] - stats['processed_files']

    estimated_size = (stats['source_size_gb'] / stats['source_files']) * remaining if stats['source_files'] > 0 else 0 estimated_size = (stats['source_size_gb'] / stats['source_files']) * remaining if stats['source_files'] > 0 else 0

     

    print(f"\nRemaining: {remaining} files (~{estimated_size:.3f}GB)") print(f"\n⏳ Remaining: {remaining} files (~{estimated_size:.3f}GB)")

    print("This will process the massive 50GB dataset with AI classification.") print(" This will process the massive 50GB dataset with AI classification.")

    print("This may take several hours and use significant CPU/memory.") print(" This may take several hours and use significant CPU/memory.")

     

    response = input("\nContinue with processing? (y/n): ") response = input("\n🤔 Continue with processing? (y/n): ")

     

    if response.lower() in ['y', 'yes']: if response.lower() in ['y', 'yes']:

        # Start processing # Start processing

        results = processor.process_all_chunks(max_workers=2) results = processor.process_all_chunks(max_workers=2)

         

        if results: if results:

            print(f"\nProcessing completed successfully!") print(f"\n Processing completed successfully!")

            print(f"{results['total_processed']:,} transactions processed") print(f" {results['total_processed']:,} transactions processed")

            print(f"Time: {results['processing_time']}") print(f"⏱ Time: {results['processing_time']}")

             

            # Run validation # Run validation

            response = input("\nRun data validation on processed results? (y/n): ") response = input("\n🤔 Run data validation on processed results? (y/n): ")

            if response.lower() in ['y', 'yes']: if response.lower() in ['y', 'yes']:

                processor.validate_processed_data() processor.validate_processed_data()

    else: else:

        print("Processing skipped") print("⏭ Processing skipped")



if __name__ == "__main__":if __name__ == "__main__":

    main() main()