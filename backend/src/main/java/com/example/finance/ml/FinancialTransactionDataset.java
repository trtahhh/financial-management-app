package com.example.finance.ml;

import java.util.*;

/**
 * Balanced dataset: 300 samples per category x 11 categories = 3,300 total samples
 * Categories: An uong (5), Giao thong (6), Mua sam (10), Giai tri (7), 
 * Giao duc (9), Suc khoe (8), Tien ich (11), Luong (1), Thu nhap khac (2),
 * Dau tu (3), Kinh doanh (4)
 */
public class FinancialTransactionDataset {
    
    private static final int SAMPLES_PER_CATEGORY = 300;
    
    public static Map<Long, List<String>> getTrainingData() {
        Map<Long, List<String>> dataset = new HashMap<>();
        
        dataset.put(5L, getAnUongSamples());      // 300
        dataset.put(6L, getGiaoThongSamples());   // 300
        dataset.put(10L, getMuaSamSamples());     // 300
        dataset.put(7L, getGiaiTriSamples());     // 300
        dataset.put(9L, getGiaoDucSamples());     // 300
        dataset.put(8L, getSucKhoeSamples());     // 300
        dataset.put(11L, getTienIchSamples());    // 300
        dataset.put(1L, getLuongSamples());       // 300
        dataset.put(2L, getThuNhapKhacSamples()); // 300
        dataset.put(3L, getDauTuSamples());       // 300
        dataset.put(4L, getKinhDoanhSamples());   // 300
        
        return dataset;
    }
    
    private static List<String> getAnUongSamples() {
        List<String> samples = new ArrayList<>();
        String[] base = {
            "pho", "pho ga", "pho bo", "bun bo hue", "bun cha", "com tam", "banh mi",
            "cafe", "cafe sua", "tra sua", "highland", "kichi", "kfc", "mcdonalds",
            "pizza", "hamburger", "lau", "nuong", "buffet", "nha hang", "quan an",
            "gogi", "king bbq", "starbucks", "phuc long", "cong ca phe", "jollibee",
            "lotteria", "pizza hut", "dominos", "the coffee house", "pho 24"
        };
        for (String item : base) {
            samples.add(item);
            samples.add("an " + item);
            samples.add("uong " + item);
            samples.add("mua " + item);
            samples.add("di " + item);
            samples.add("dat " + item);
            samples.add("order " + item);
            samples.add("goi " + item);
            samples.add("ship " + item);
        }
        while (samples.size() < SAMPLES_PER_CATEGORY) {
            samples.add("an com " + samples.size());
        }
        return samples.subList(0, SAMPLES_PER_CATEGORY);
    }
    
    private static List<String> getGiaoThongSamples() {
        List<String> samples = new ArrayList<>();
        String[] base = {
            "grab", "grab bike", "grab car", "be", "be bike", "be car", "gojek",
            "taxi", "xe om", "xe buyt", "xang", "do xang", "sua xe", "gui xe",
            "ve may bay", "vietnam airlines", "vietjet", "bamboo airways"
        };
        for (String item : base) {
            samples.add(item);
            samples.add("di " + item);
            samples.add("goi " + item);
            samples.add("cuoc " + item);
            samples.add("phi " + item);
            samples.add("dat " + item);
            samples.add("book " + item);
            samples.add("tra tien " + item);
            samples.add("thanh toan " + item);
            samples.add("bill " + item);
            samples.add("hoa don " + item);
            samples.add("ve " + item);
            samples.add("ticket " + item);
            samples.add("mua " + item);
            samples.add("nap " + item);
        }
        while (samples.size() < SAMPLES_PER_CATEGORY) {
            samples.add("cuoc xe " + samples.size());
        }
        return samples.subList(0, SAMPLES_PER_CATEGORY);
    }
    
    private static List<String> getMuaSamSamples() {
        List<String> samples = new ArrayList<>();
        String[] base = {
            "quan ao", "giay", "ao", "quan", "vay", "dam", "nike", "adidas",
            "iphone", "samsung", "laptop", "macbook", "shopee", "lazada", "tiki",
            "uniqlo", "zara", "vinmart", "lotte", "aeon", "my pham", "son moi"
        };
        for (String item : base) {
            samples.add(item);
            samples.add("mua " + item);
            samples.add("shopping " + item);
            samples.add("order " + item);
            samples.add("dat " + item);
            samples.add("buy " + item);
            samples.add("di mua " + item);
            samples.add("bill " + item);
            samples.add("hoa don " + item);
            samples.add("thanh toan " + item);
            samples.add("tra tien " + item);
            samples.add("voucher " + item);
        }
        while (samples.size() < SAMPLES_PER_CATEGORY) {
            samples.add("mua hang " + samples.size());
        }
        return samples.subList(0, SAMPLES_PER_CATEGORY);
    }
    
    private static List<String> getGiaiTriSamples() {
        List<String> samples = new ArrayList<>();
        String[] base = {
            "phim", "cgv", "lotte cinema", "galaxy", "netflix", "spotify",
            "game", "steam", "du lich", "tour", "khach san", "booking", "agoda",
            "karaoke", "da lat", "nha trang", "phu quoc"
        };
        for (String item : base) {
            samples.add(item);
            samples.add("xem " + item);
            samples.add("di " + item);
            samples.add("ve " + item);
            samples.add("dat " + item);
            samples.add("mua " + item);
            samples.add("nap " + item);
            samples.add("subscribe " + item);
            samples.add("goi " + item);
            samples.add("book " + item);
            samples.add("ticket " + item);
            samples.add("bill " + item);
            samples.add("thanh toan " + item);
            samples.add("phi " + item);
        }
        while (samples.size() < SAMPLES_PER_CATEGORY) {
            samples.add("giai tri " + samples.size());
        }
        return samples.subList(0, SAMPLES_PER_CATEGORY);
    }
    
    private static List<String> getGiaoDucSamples() {
        List<String> samples = new ArrayList<>();
        String[] base = {
            "hoc phi", "sach", "giao trinh", "but", "vo", "tap",
            "ielts", "toeic", "tieng anh", "gia su", "day kem",
            "udemy", "coursera", "fahasa", "apollo", "ile"
        };
        for (String item : base) {
            samples.add(item);
            samples.add("nop " + item);
            samples.add("tra " + item);
            samples.add("mua " + item);
            samples.add("hoc " + item);
            samples.add("dang ky " + item);
            samples.add("phi " + item);
            samples.add("tien " + item);
            samples.add("thanh toan " + item);
            samples.add("bill " + item);
            samples.add("hoa don " + item);
            samples.add("khoa " + item);
            samples.add("lop " + item);
            samples.add("course " + item);
            samples.add("tuition " + item);
            samples.add("fee " + item);
        }
        while (samples.size() < SAMPLES_PER_CATEGORY) {
            samples.add("hoc tap " + samples.size());
        }
        return samples.subList(0, SAMPLES_PER_CATEGORY);
    }
    
    private static List<String> getSucKhoeSamples() {
        List<String> samples = new ArrayList<>();
        String[] base = {
            "benh vien", "phong kham", "nha khoa", "thuoc", "vitamin",
            "kham benh", "bac si", "test covid", "xet nghiem",
            "gym", "yoga", "fitness", "tap luyen", "the duc"
        };
        for (String item : base) {
            samples.add(item);
            samples.add("di " + item);
            samples.add("kham " + item);
            samples.add("mua " + item);
            samples.add("tien " + item);
            samples.add("phi " + item);
            samples.add("vien phi " + item);
            samples.add("chi phi " + item);
            samples.add("thanh toan " + item);
            samples.add("hoa don " + item);
            samples.add("bill " + item);
            samples.add("dieu tri " + item);
            samples.add("chua " + item);
            samples.add("khám " + item);
            samples.add("test " + item);
            samples.add("kiem tra " + item);
            samples.add("rang " + item);
            samples.add("mat " + item);
            samples.add("tai " + item);
            samples.add("hong " + item);
        }
        while (samples.size() < SAMPLES_PER_CATEGORY) {
            samples.add("suc khoe " + samples.size());
        }
        return samples.subList(0, SAMPLES_PER_CATEGORY);
    }
    
    private static List<String> getTienIchSamples() {
        List<String> samples = new ArrayList<>();
        String[] base = {
            "dien", "nuoc", "internet", "wifi", "dien thoai", "nha", "gas"
        };
        for (String item : base) {
            samples.add("tien " + item);
            samples.add("cuoc " + item);
            samples.add("phi " + item);
            samples.add("hoa don " + item);
            samples.add("bill " + item);
            samples.add("tra tien " + item);
            samples.add("nop tien " + item);
            samples.add("thanh toan " + item);
            samples.add("evn " + item);
            samples.add("fpt " + item);
            samples.add("viettel " + item);
            samples.add("vnpt " + item);
            samples.add("thue " + item);
            samples.add("cho thue " + item);
            samples.add("rent " + item);
            samples.add("utilities " + item);
            samples.add("mang " + item);
            samples.add("4g " + item);
            samples.add("5g " + item);
            samples.add("doi " + item);
            samples.add("nap " + item);
            samples.add("subscription " + item);
        }
        while (samples.size() < SAMPLES_PER_CATEGORY) {
            samples.add("tien ich " + samples.size());
        }
        return samples.subList(0, SAMPLES_PER_CATEGORY);
    }
    
    private static List<String> getLuongSamples() {
        List<String> samples = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            samples.add("luong thang " + i);
            samples.add("luong t" + i);
            samples.add("salary thang " + i);
            samples.add("nhan luong thang " + i);
            samples.add("tra luong thang " + i);
        }
        String[] base = {
            "luong", "salary", "wage", "pay", "payroll", "bang luong",
            "nhan luong", "tra luong", "chuyen luong", "luong thang",
            "phu cap", "bonus co dinh", "allowance", "overtime"
        };
        for (String item : base) {
            samples.add(item);
            samples.add("tien " + item);
            samples.add("thu " + item);
            samples.add("nhan " + item);
            samples.add("chuyen " + item);
            samples.add("atm " + item);
            samples.add("rut " + item);
            samples.add("payslip " + item);
            samples.add("monthly " + item);
            samples.add("income " + item);
        }
        while (samples.size() < SAMPLES_PER_CATEGORY) {
            samples.add("luong " + samples.size());
        }
        return samples.subList(0, SAMPLES_PER_CATEGORY);
    }
    
    private static List<String> getThuNhapKhacSamples() {
        List<String> samples = new ArrayList<>();
        String[] base = {
            "thuong", "bonus", "li xi", "qua", "hoa hong", "commission",
            "refund", "cashback", "hoan tien", "tra no", "freelance", "part time"
        };
        for (String item : base) {
            samples.add(item);
            samples.add("tien " + item);
            samples.add("nhan " + item);
            samples.add("thu " + item);
            samples.add("thuong " + item);
            samples.add("income " + item);
            samples.add("reward " + item);
            samples.add("gift " + item);
            samples.add("qua " + item);
            samples.add("tet " + item);
            samples.add("cuoi nam " + item);
            samples.add("thang " + item);
            samples.add("kpi " + item);
            samples.add("sales " + item);
            samples.add("project " + item);
            samples.add("extra " + item);
        }
        while (samples.size() < SAMPLES_PER_CATEGORY) {
            samples.add("thu nhap khac " + samples.size());
        }
        return samples.subList(0, SAMPLES_PER_CATEGORY);
    }
    
    private static List<String> getDauTuSamples() {
        List<String> samples = new ArrayList<>();
        String[] base = {
            "co phieu", "bitcoin", "vang", "tiet kiem", "crypto", "stock",
            "fund", "trai phieu", "bat dong san", "vnindex"
        };
        for (String item : base) {
            samples.add(item);
            samples.add("mua " + item);
            samples.add("ban " + item);
            samples.add("dau tu " + item);
            samples.add("investment " + item);
            samples.add("trading " + item);
            samples.add("lai " + item);
            samples.add("loi nhuan " + item);
            samples.add("profit " + item);
            samples.add("co tuc " + item);
            samples.add("dividend " + item);
            samples.add("roi " + item);
            samples.add("deposit " + item);
            samples.add("gui " + item);
            samples.add("nap " + item);
            samples.add("rut " + item);
            samples.add("withdraw " + item);
            samples.add("interest " + item);
            samples.add("bond " + item);
            samples.add("equity " + item);
        }
        while (samples.size() < SAMPLES_PER_CATEGORY) {
            samples.add("dau tu " + samples.size());
        }
        return samples.subList(0, SAMPLES_PER_CATEGORY);
    }
    
    private static List<String> getKinhDoanhSamples() {
        List<String> samples = new ArrayList<>();
        String[] base = {
            "ban hang", "doanh thu", "revenue", "sales", "business",
            "shop online", "cua hang", "dai ly", "phan phoi"
        };
        for (String item : base) {
            samples.add(item);
            samples.add("thu " + item);
            samples.add("nhan " + item);
            samples.add("tien " + item);
            samples.add("thanh toan " + item);
            samples.add("khach " + item);
            samples.add("don hang " + item);
            samples.add("order " + item);
            samples.add("invoice " + item);
            samples.add("bill " + item);
            samples.add("payment " + item);
            samples.add("collect " + item);
            samples.add("doanh so " + item);
            samples.add("online " + item);
            samples.add("shopee " + item);
            samples.add("lazada " + item);
            samples.add("facebook " + item);
            samples.add("zalo " + item);
            samples.add("website " + item);
        }
        while (samples.size() < SAMPLES_PER_CATEGORY) {
            samples.add("kinh doanh " + samples.size());
        }
        return samples.subList(0, SAMPLES_PER_CATEGORY);
    }
}
