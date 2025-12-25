"""
Module trích xuất thông tin từ text OCR sử dụng Regex và Rule-based
Trích xuất: store_name, invoice_date, items, total_amount, address
"""

import re
from typing import Dict, List, Optional, Tuple
from datetime import datetime
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class InformationExtractor:
    """Lớp trích xuất thông tin từ hóa đơn"""
    
    # Patterns cho các trường thông tin
    DATE_PATTERNS = [
        r'\d{1,2}[/-]\d{1,2}[/-]\d{2,4}',  # dd/mm/yyyy, dd-mm-yyyy
        r'\d{1,2}\s*/\s*\d{1,2}\s*/\s*\d{2,4}',  # với khoảng trắng
        r'\d{4}[/-]\d{1,2}[/-]\d{1,2}',  # yyyy-mm-dd
        r'\d{1,2}\s+\d{1,2}\s+\d{2,4}',  # dd mm yyyy
    ]
    
    TIME_PATTERNS = [
        r'\d{1,2}:\d{2}:\d{2}',  # hh:mm:ss
        r'\d{1,2}:\d{2}',  # hh:mm
        r'\d{1,2}\s*:\s*\d{2}\s*:\s*\d{2}',  # với khoảng trắng
    ]
    
    AMOUNT_PATTERNS = [
        r'[\d,.]+\s*(?:đ|VNĐ|vnd|VND|vnđ)',  # Số + đơn vị tiền
        r'[\d,.]+',  # Chỉ số
    ]
    
    # Keywords cho từng loại thông tin
    DATE_KEYWORDS = [
        'ngày', 'date', 'thời gian', 'time', 'ngay', 'ngay ban',
        'ngày bán', 'timestamp'
    ]
    
    TOTAL_KEYWORDS = [
        'tổng', 'total', 'cộng', 'tong', 'thanh toán', 'phải trả',
        'tổng tiền', 'tổng cộng', 'tong tien', 'tong cong',
        'sum', 'amount', 'grand total'
    ]
    
    STORE_KEYWORDS = [
        'cửa hàng', 'siêu thị', 'minimart', 'shop', 'store',
        'vinmart', 'co.op', 'coop', 'mart', 'công ty', 'cong ty'
    ]
    
    ADDRESS_KEYWORDS = [
        'địa chỉ', 'address', 'dia chi', 'đường', 'duong', 'phường',
        'quận', 'huyện', 'thành phố', 'tỉnh', 'số', 'so'
    ]
    
    def __init__(self):
        """Khởi tạo extractor"""
        pass
    
    def extract_dates(self, text: str) -> List[str]:
        """
        Trích xuất ngày tháng từ text
        
        Args:
            text: Text đầu vào
            
        Returns:
            List[str]: Danh sách các ngày tìm được
        """
        dates = []
        
        for pattern in self.DATE_PATTERNS:
            matches = re.findall(pattern, text)
            dates.extend(matches)
        
        return dates
    
    def extract_times(self, text: str) -> List[str]:
        """
        Trích xuất giờ từ text
        
        Args:
            text: Text đầu vào
            
        Returns:
            List[str]: Danh sách giờ tìm được
        """
        times = []
        
        for pattern in self.TIME_PATTERNS:
            matches = re.findall(pattern, text)
            times.extend(matches)
        
        return times
    
    def extract_amounts(self, text: str) -> List[str]:
        """
        Trích xuất số tiền từ text
        
        Args:
            text: Text đầu vào
            
        Returns:
            List[str]: Danh sách số tiền tìm được
        """
        amounts = []
        
        for pattern in self.AMOUNT_PATTERNS:
            matches = re.findall(pattern, text, re.IGNORECASE)
            amounts.extend(matches)
        
        return amounts
    
    def normalize_amount(self, amount_str: str) -> Optional[float]:
        """
        Chuẩn hóa chuỗi số tiền thành float
        
        Args:
            amount_str: Chuỗi số tiền
            
        Returns:
            float: Số tiền đã chuẩn hóa
        """
        # Xóa đơn vị tiền
        cleaned = re.sub(r'[đvnđVND\s]', '', amount_str, flags=re.IGNORECASE)
        
        # Xóa dấu phẩy và dấu chấm (trừ dấu thập phân cuối)
        # Giả sử dấu cuối cùng là dấu thập phân
        cleaned = cleaned.replace('.', '').replace(',', '')
        
        try:
            return float(cleaned)
        except ValueError:
            return None
    
    def normalize_date(self, date_str: str) -> Optional[str]:
        """
        Chuẩn hóa ngày tháng về format dd/mm/yyyy
        
        Args:
            date_str: Chuỗi ngày tháng
            
        Returns:
            str: Ngày đã chuẩn hóa (dd/mm/yyyy)
        """
        # Thử các format khác nhau
        formats = [
            '%d/%m/%Y', '%d-%m-%Y', '%d/%m/%y', '%d-%m-%y',
            '%Y/%m/%d', '%Y-%m-%d',
            '%d %m %Y', '%d %m %y'
        ]
        
        date_str_clean = date_str.strip()
        
        for fmt in formats:
            try:
                dt = datetime.strptime(date_str_clean, fmt)
                return dt.strftime('%d/%m/%Y')
            except ValueError:
                continue
        
        return None
    
    def find_field_by_keywords(self, text_lines: List[str], 
                              keywords: List[str]) -> Optional[str]:
        """
        Tìm trường thông tin dựa vào keywords
        
        Args:
            text_lines: Danh sách các dòng text
            keywords: Danh sách keywords
            
        Returns:
            str: Text của trường tìm được
        """
        for line in text_lines:
            line_lower = line.lower()
            for keyword in keywords:
                if keyword.lower() in line_lower:
                    # Lấy phần sau keyword
                    parts = re.split(keyword, line, flags=re.IGNORECASE)
                    if len(parts) > 1:
                        value = parts[1].strip()
                        # Xóa dấu : và khoảng trắng đầu
                        value = re.sub(r'^[\s:]+', '', value)
                        if value:
                            return value
        
        return None
    
    def extract_store_name(self, regions: List[Dict]) -> Optional[str]:
        """
        Trích xuất tên cửa hàng
        
        Args:
            regions: Danh sách các vùng đã OCR
            
        Returns:
            str: Tên cửa hàng
        """
        # Ưu tiên vùng có category = 15 (SELLER/store_name)
        candidates = []
        for region in regions:
            # Check category ID (15 = SELLER)
            if region.get('category_id') == 15 and region.get('text'):
                text = region['text'].strip()
                if text and len(text) > 1:  # Ignore single characters
                    candidates.append(text)
            # Also check label for backward compatibility
            elif region.get('label') == 'store_name' and region.get('text'):
                text = region['text'].strip()
                if text and len(text) > 1:
                    candidates.append(text)
        
        # Return the longest valid candidate (usually the actual store name)
        if candidates:
            return max(candidates, key=len)
        
        # Tìm theo keywords
        texts = [r.get('text', '') for r in regions if r.get('text')]
        result = self.find_field_by_keywords(texts, self.STORE_KEYWORDS)
        
        if result:
            return result
        
        return None
    
    def extract_date(self, regions: List[Dict]) -> Optional[str]:
        """
        Trích xuất ngày hóa đơn
        
        Args:
            regions: Danh sách các vùng đã OCR
            
        Returns:
            str: Ngày hóa đơn (dd/mm/yyyy)
        """
        # Ưu tiên vùng có category = 17 (TIMESTAMP/date)
        for region in regions:
            # Check category ID (17 = TIMESTAMP)
            if region.get('category_id') == 17 and region.get('text'):
                dates = self.extract_dates(region['text'])
                if dates:
                    normalized = self.normalize_date(dates[0])
                    if normalized:
                        return normalized
            # Also check label for backward compatibility
            elif region.get('label') == 'date' and region.get('text'):
                dates = self.extract_dates(region['text'])
                if dates:
                    normalized = self.normalize_date(dates[0])
                    if normalized:
                        return normalized
        
        # Tìm trong toàn bộ text
        all_text = ' '.join([r.get('text', '') for r in regions])
        dates = self.extract_dates(all_text)
        
        for date in dates:
            normalized = self.normalize_date(date)
            if normalized:
                return normalized
        
        return None
    
    def extract_total_amount(self, regions: List[Dict]) -> Optional[float]:
        """
        Trích xuất tổng tiền
        
        Args:
            regions: Danh sách các vùng đã OCR
            
        Returns:
            float: Tổng tiền
        """
        # Ưu tiên vùng có category = 18 (TOTAL_COST)
        candidates = []
        
        for region in regions:
            # Check category ID (18 = TOTAL_COST)
            if region.get('category_id') == 18 and region.get('text'):
                text = region['text']
                # Extract all numbers from the text
                amounts = self.extract_amounts(text)
                for amount in amounts:
                    normalized = self.normalize_amount(amount)
                    if normalized and normalized > 0:  # Only positive amounts
                        candidates.append(normalized)
            # Also check label for backward compatibility
            elif region.get('label') == 'total_amount' and region.get('text'):
                amounts = self.extract_amounts(region['text'])
                for amount in amounts:
                    normalized = self.normalize_amount(amount)
                    if normalized and normalized > 0:
                        candidates.append(normalized)
        
        # Return the largest amount (usually the total)
        if candidates:
            return max(candidates)
        
        # Fallback: Tìm theo keywords trong tất cả text
        texts = [r.get('text', '') for r in regions if r.get('text')]
        for line in texts:
            line_lower = line.lower()
            for keyword in self.TOTAL_KEYWORDS:
                if keyword in line_lower:
                    amounts = self.extract_amounts(line)
                    for amount in amounts:
                        normalized = self.normalize_amount(amount)
                        if normalized and normalized > 0:
                            candidates.append(normalized)
        
        # Trả về số lớn nhất
        if candidates:
            return max(candidates)
        
        return None
    
    def extract_address(self, regions: List[Dict]) -> Optional[str]:
        """
        Trích xuất địa chỉ
        
        Args:
            regions: Danh sách các vùng đã OCR
            
        Returns:
            str: Địa chỉ
        """
        addresses = []
        
        # Ưu tiên vùng có category = 16 (ADDRESS)
        for region in regions:
            # Check category ID (16 = ADDRESS)
            if region.get('category_id') == 16 and region.get('text'):
                text = region['text'].strip()
                if text and len(text) > 3:  # Filter out too short text
                    addresses.append(text)
            # Also check label for backward compatibility
            elif region.get('label') == 'address' and region.get('text'):
                text = region['text'].strip()
                if text and len(text) > 3:
                    addresses.append(text)
        
        if addresses:
            return ' '.join(addresses)  # Join with space instead of comma
        
        # Tìm theo keywords
        texts = [r.get('text', '') for r in regions if r.get('text')]
        result = self.find_field_by_keywords(texts, self.ADDRESS_KEYWORDS)
        
        return result
    
    def extract_items(self, regions: List[Dict]) -> List[Dict]:
        """
        Trích xuất danh sách mặt hàng
        
        Args:
            regions: Danh sách các vùng đã OCR
            
        Returns:
            List[Dict]: Danh sách items [{name, quantity, unit_price, total_price}, ...]
        """
        items = []
        
        # Pattern cho item: tên - số lượng - đơn giá - thành tiền
        # Thường có format: "Sản phẩm X    2    15,000    30,000"
        
        texts = [r.get('text', '') for r in regions if r.get('text')]
        
        for line in texts:
            # Skip các dòng header hoặc footer
            line_lower = line.lower()
            if any(kw in line_lower for kw in self.TOTAL_KEYWORDS + self.DATE_KEYWORDS + self.STORE_KEYWORDS):
                continue
            
            # Tìm các số trong dòng
            amounts = self.extract_amounts(line)
            
            # Nếu có ít nhất 2 số (số lượng và giá), coi là item
            if len(amounts) >= 2:
                # Parse item
                item = {
                    'name': '',
                    'quantity': None,
                    'unit_price': None,
                    'total_price': None
                }
                
                # Lấy tên (phần text trước số đầu tiên)
                first_number_match = re.search(r'[\d,.]+', line)
                if first_number_match:
                    item['name'] = line[:first_number_match.start()].strip()
                
                # Parse các số
                numbers = [self.normalize_amount(a) for a in amounts]
                numbers = [n for n in numbers if n is not None]
                
                if len(numbers) >= 2:
                    item['quantity'] = numbers[0] if numbers[0] < 100 else 1
                    item['unit_price'] = numbers[1] if len(numbers) >= 2 else None
                    item['total_price'] = numbers[-1] if len(numbers) >= 2 else None
                    
                    if item['name']:
                        items.append(item)
        
        return items
    
    def extract_all(self, regions: List[Dict]) -> Dict:
        """
        Trích xuất toàn bộ thông tin từ hóa đơn
        
        Args:
            regions: Danh sách các vùng đã OCR
            
        Returns:
            Dict: Dictionary chứa tất cả thông tin
        """
        result = {
            'store_name': self.extract_store_name(regions),
            'invoice_date': self.extract_date(regions),
            'address': self.extract_address(regions),
            'items': self.extract_items(regions),
            'total_amount': self.extract_total_amount(regions)
        }
        
        logger.info(f"Đã trích xuất: store_name={result['store_name']}, "
                   f"date={result['invoice_date']}, "
                   f"total={result['total_amount']}, "
                   f"items={len(result['items'])}")
        
        return result
    
    def validate_extraction(self, data: Dict) -> Dict:
        """
        Kiểm tra và đánh giá chất lượng extraction
        
        Args:
            data: Dữ liệu đã extract
            
        Returns:
            Dict: Thông tin validation
        """
        validation = {
            'has_store_name': data.get('store_name') is not None,
            'has_date': data.get('invoice_date') is not None,
            'has_total': data.get('total_amount') is not None,
            'has_address': data.get('address') is not None,
            'num_items': len(data.get('items', [])),
            'completeness': 0.0
        }
        
        # Tính độ hoàn thiện
        fields = ['store_name', 'invoice_date', 'total_amount', 'address']
        present = sum([1 for f in fields if data.get(f) is not None])
        validation['completeness'] = present / len(fields)
        
        return validation
    
    def post_process(self, data: Dict) -> Dict:
        """
        Xử lý sau extraction để chuẩn hóa dữ liệu
        
        Args:
            data: Dữ liệu đã extract
            
        Returns:
            Dict: Dữ liệu đã xử lý
        """
        result = data.copy()
        
        # Chuẩn hóa store_name
        if result.get('store_name'):
            result['store_name'] = result['store_name'].strip().upper()
        
        # Chuẩn hóa address
        if result.get('address'):
            result['address'] = result['address'].strip()
        
        # Đảm bảo items là list
        if not isinstance(result.get('items'), list):
            result['items'] = []
        
        return result


if __name__ == "__main__":
    # Test code
    extractor = InformationExtractor()
    
    print("InformationExtractor module loaded successfully!")
    print("\nSupported extractions:")
    print("- store_name")
    print("- invoice_date")
    print("- address")
    print("- items (name, quantity, unit_price, total_price)")
    print("- total_amount")
    
    # Test date extraction
    test_text = "Ngày: 15/08/2020 10:30:45"
    dates = extractor.extract_dates(test_text)
    print(f"\nTest date extraction: '{test_text}'")
    print(f"Found: {dates}")
    
    # Test amount extraction
    test_text2 = "Tổng tiền: 123,456 VNĐ"
    amounts = extractor.extract_amounts(test_text2)
    print(f"\nTest amount extraction: '{test_text2}'")
    print(f"Found: {amounts}")
    if amounts:
        normalized = extractor.normalize_amount(amounts[0])
        print(f"Normalized: {normalized}")
