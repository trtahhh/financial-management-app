package com.example.finance.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Knowledge Base chứa 50+ giải pháp tiết kiệm cụ thể
 * Bot học từ đây để đưa ra lời khuyên phù hợp với từng category/situation
 */
@Service
public class SavingsKnowledgeBase {
    
    private final Map<String, List<SavingTip>> categoryTips = new HashMap<>();
    private final List<SavingTip> generalTips = new ArrayList<>();
    private final List<SavingTip> emergencyTips = new ArrayList<>();
    
    public SavingsKnowledgeBase() {
        initializeKnowledgeBase();
    }
    
    /**
     * Lấy tips theo category và spending level
     */
    public List<SavingTip> getTipsForCategory(String category, double spendingLevel) {
        List<SavingTip> tips = categoryTips.getOrDefault(category.toLowerCase(), new ArrayList<>());
        
        // Filter by spending level
        String level = spendingLevel > 80 ? "critical" : 
                      spendingLevel > 60 ? "high" : 
                      spendingLevel > 40 ? "medium" : "low";
        
        return tips.stream()
            .filter(tip -> tip.getApplicableLevels().contains(level) || 
                          tip.getApplicableLevels().contains("all"))
            .sorted((a, b) -> Integer.compare(b.getImpactScore(), a.getImpactScore()))
            .limit(5)
            .collect(Collectors.toList());
    }
    
    /**
     * Lấy emergency tips khi overspending nghiêm trọng
     */
    public List<SavingTip> getEmergencyTips() {
        return emergencyTips;
    }
    
    /**
     * Lấy general tips áp dụng cho mọi người
     */
    public List<SavingTip> getGeneralTips(int count) {
        return generalTips.stream()
            .sorted((a, b) -> Integer.compare(b.getImpactScore(), a.getImpactScore()))
            .limit(count)
            .collect(Collectors.toList());
    }
    
    /**
     * Search tips by keyword
     */
    public List<SavingTip> searchTips(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        List<SavingTip> results = new ArrayList<>();
        
        categoryTips.values().forEach(tips -> 
            results.addAll(tips.stream()
                .filter(tip -> tip.getTitle().toLowerCase().contains(lowerKeyword) ||
                              tip.getDescription().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList()))
        );
        
        results.addAll(generalTips.stream()
            .filter(tip -> tip.getTitle().toLowerCase().contains(lowerKeyword) ||
                          tip.getDescription().toLowerCase().contains(lowerKeyword))
            .collect(Collectors.toList()));
        
        return results.stream()
            .distinct()
            .sorted((a, b) -> Integer.compare(b.getImpactScore(), a.getImpactScore()))
            .collect(Collectors.toList());
    }
    
    private void initializeKnowledgeBase() {
        initializeFoodTips();
        initializeTransportTips();
        initializeShoppingTips();
        initializeEntertainmentTips();
        initializeHealthTips();
        initializeUtilityTips();
        initializeGeneralTips();
        initializeEmergencyTips();
    }
    
    private void initializeFoodTips() {
        List<SavingTip> foodTips = new ArrayList<>();
        
        foodTips.add(new SavingTip(
            "Ăn cơm nhà",
            "Tự nấu ăn tại nhà thay vì ăn ngoài. Tiết kiệm 60-70% chi phí ăn uống.",
            Arrays.asList("critical", "high", "all"),
            9,
            "60-70%",
            "immediate",
            Arrays.asList(
                "Lập menu tuần trước",
                "Mua nguyên liệu tại chợ",
                "Nấu nhiều và bảo quản",
                "Tận dụng thức ăn thừa"
            )
        ));
        
        foodTips.add(new SavingTip(
            "Mang cơm trưa đi làm",
            "Chuẩn bị cơm trưa tại nhà. Tiết kiệm 50-60k/ngày = 1-1.5 triệu/tháng.",
            Arrays.asList("high", "medium", "all"),
            8,
            "50-60k/ngày",
            "immediate",
            Arrays.asList(
                "Nấu sẵn tối hôm trước",
                "Mua hộp đựng cơm đẹp",
                "Chế biến đơn giản, dễ làm",
                "Rủ đồng nghiệp cùng làm"
            )
        ));
        
        foodTips.add(new SavingTip(
            "Pha cafe tại nhà",
            "Thay vì uống cafe ngoài 40-50k, pha tại nhà chỉ 5-10k. Tiết kiệm 30-40k/ly.",
            Arrays.asList("high", "medium", "all"),
            7,
            "30-40k/ly",
            "immediate",
            Arrays.asList(
                "Mua máy pha cafe mini",
                "Mua cafe hạt/bột chất lượng",
                "Học cách pha latte, capuchino",
                "Dùng cốc giữ nhiệt đẹp"
            )
        ));
        
        foodTips.add(new SavingTip(
            "Mua sắm tại chợ",
            "Mua nguyên liệu tại chợ truyền thống thay vì siêu thị. Rẻ hơn 30-40%.",
            Arrays.asList("high", "medium", "all"),
            7,
            "30-40%",
            "short-term",
            Arrays.asList(
                "Đi chợ sáng sớm (rẻ hơn)",
                "Mua theo mùa",
                "Mặc cả khéo léo",
                "Mua số lượng lớn để trữ"
            )
        ));
        
        foodTips.add(new SavingTip(
            "Ngừng order đồ ăn online",
            "Phí ship + phí dịch vụ tăng 25-35%. Tự đi mua hoặc nấu ăn.",
            Arrays.asList("critical", "high", "all"),
            8,
            "25-35%",
            "immediate",
            Arrays.asList(
                "Xóa app Grab Food, Baemin",
                "Lập kế hoạch ăn uống",
                "Chuẩn bị đồ ăn vặt tại nhà",
                "Tự đi mua nếu thực sự cần"
            )
        ));
        
        foodTips.add(new SavingTip(
            "Giảm ăn snack/đồ vặt",
            "Đồ ăn vặt không cần thiết tiêu tốn 500k-1tr/tháng. Thay bằng trái cây.",
            Arrays.asList("medium", "all"),
            6,
            "500k-1tr/tháng",
            "immediate",
            Arrays.asList(
                "Mua trái cây thay snack",
                "Tự làm bánh, smoothie",
                "Uống nước thay đồ uống có đường",
                "Tránh mua khi đói"
            )
        ));
        
        foodTips.add(new SavingTip(
            "Nấu ăn theo mùa",
            "Mua rau củ theo mùa, giá rẻ hơn 40-50%. Chất lượng tốt hơn.",
            Arrays.asList("medium", "low", "all"),
            6,
            "40-50%",
            "short-term",
            Arrays.asList(
                "Tìm hiểu rau củ theo mùa",
                "Linh hoạt thay đổi menu",
                "Mua nhiều khi mùa, đông lạnh",
                "Tận dụng ưu đãi siêu thị"
            )
        ));
        
        categoryTips.put("ăn uống", foodTips);
        categoryTips.put("food", foodTips);
    }
    
    private void initializeTransportTips() {
        List<SavingTip> transportTips = new ArrayList<>();
        
        transportTips.add(new SavingTip(
            "Đi xe buýt thay Grab",
            "Xe buýt rẻ hơn Grab 80-90%. 7k vs 50-70k cho cùng quãng đường.",
            Arrays.asList("critical", "high", "all"),
            9,
            "80-90%",
            "immediate",
            Arrays.asList(
                "Tải app BusMap",
                "Mua vé tháng nếu đi thường xuyên",
                "Kết hợp xe buýt + đi bộ",
                "Quen với lộ trình"
            )
        ));
        
        transportTips.add(new SavingTip(
            "Mua vé tháng xe buýt/MRT",
            "Vé tháng 200-300k, tiết kiệm 50% so với mua lẻ hàng ngày.",
            Arrays.asList("high", "medium", "all"),
            8,
            "50%",
            "immediate",
            Arrays.asList(
                "Mua vào đầu tháng",
                "Tính toán số ngày đi",
                "Kết hợp nhiều tuyến",
                "Share với bạn bè"
            )
        ));
        
        transportTips.add(new SavingTip(
            "Đi xe đạp/đi bộ",
            "Quãng đường < 3km nên đi bộ/xe đạp. Tiết kiệm 100% + tốt cho sức khỏe.",
            Arrays.asList("high", "medium", "all"),
            8,
            "100%",
            "immediate",
            Arrays.asList(
                "Mua xe đạp cũ giá rẻ",
                "Đi sớm để kịp giờ",
                "Mang theo áo mưa",
                "Tập thể dục luôn"
            )
        ));
        
        transportTips.add(new SavingTip(
            "Carpool với đồng nghiệp",
            "Đi chung xe với đồng nghiệp, chia sẻ phí xăng. Tiết kiệm 50-70%.",
            Arrays.asList("medium", "all"),
            7,
            "50-70%",
            "short-term",
            Arrays.asList(
                "Tìm người cùng đường",
                "Luân phiên lái xe",
                "Chia đều chi phí",
                "Sắp xếp giờ linh hoạt"
            )
        ));
        
        transportTips.add(new SavingTip(
            "Kết hợp công việc khi ra ngoài",
            "Làm nhiều việc trong 1 chuyến đi. Giảm số lần ra ngoài.",
            Arrays.asList("medium", "low", "all"),
            6,
            "30-40%",
            "immediate",
            Arrays.asList(
                "Lập danh sách việc cần làm",
                "Chọn địa điểm gần nhau",
                "Đi vào cuối tuần",
                "Mua sắm tập trung"
            )
        ));
        
        transportTips.add(new SavingTip(
            "Work from home",
            "Làm việc từ xa nếu công ty cho phép. Tiết kiệm chi phí đi lại hoàn toàn.",
            Arrays.asList("critical", "high", "all"),
            9,
            "100%",
            "immediate",
            Arrays.asList(
                "Thỏa thuận với sếp",
                "Bắt đầu 1-2 ngày/tuần",
                "Chuẩn bị không gian làm việc",
                "Đảm bảo hiệu suất"
            )
        ));
        
        categoryTips.put("di chuyển", transportTips);
        categoryTips.put("transport", transportTips);
    }
    
    private void initializeShoppingTips() {
        List<SavingTip> shoppingTips = new ArrayList<>();
        
        shoppingTips.add(new SavingTip(
            "Quy tắc 24-48 giờ",
            "Chờ 24-48h trước khi mua đồ không thiết yếu. Giảm 70% mua sắm bốc đồng.",
            Arrays.asList("critical", "high", "medium", "all"),
            9,
            "70%",
            "immediate",
            Arrays.asList(
                "Thêm vào wishlist, chờ 2 ngày",
                "Tự hỏi: Có thực sự cần?",
                "Kiểm tra đồ có sẵn",
                "Nếu sau 2 ngày vẫn muốn mới mua"
            )
        ));
        
        shoppingTips.add(new SavingTip(
            "Mua hàng second-hand",
            "Đồ cũ chất lượng tốt giá rẻ 50-70%. Facebook, Chợ Tốt.",
            Arrays.asList("high", "medium", "all"),
            8,
            "50-70%",
            "short-term",
            Arrays.asList(
                "Tìm trên Facebook Marketplace",
                "Chợ Tốt, Chotot.vn",
                "Kiểm tra kỹ trước khi mua",
                "Mặc cả thông minh"
            )
        ));
        
        shoppingTips.add(new SavingTip(
            "Lập danh sách cần thiết",
            "Chỉ mua đồ trong list cần thiết. Tránh mua ngẫu hứng.",
            Arrays.asList("high", "medium", "all"),
            7,
            "50-60%",
            "immediate",
            Arrays.asList(
                "Viết list trước khi đi mua",
                "Phân biệt want vs need",
                "Chỉ mua trong list",
                "Review hàng tháng"
            )
        ));
        
        shoppingTips.add(new SavingTip(
            "Tận dụng đồ có sẵn",
            "Sử dụng lại, sửa chữa đồ cũ thay vì mua mới.",
            Arrays.asList("medium", "all"),
            7,
            "100%",
            "immediate",
            Arrays.asList(
                "Kiểm kê đồ hiện có",
                "Học sửa chữa cơ bản",
                "DIY thay vì mua mới",
                "Cho đi/đổi đồ không dùng"
            )
        ));
        
        shoppingTips.add(new SavingTip(
            "Mua vào dịp sale",
            "Chờ Black Friday, sale cuối mùa. Giảm giá 30-70%.",
            Arrays.asList("medium", "low", "all"),
            6,
            "30-70%",
            "short-term",
            Arrays.asList(
                "Đánh dấu lịch sale lớn",
                "Theo dõi giá trước",
                "So sánh nhiều nơi",
                "Vẫn áp dụng quy tắc 24h"
            )
        ));
        
        shoppingTips.add(new SavingTip(
            "Xóa app shopping",
            "Xóa Shopee, Lazada, Tiki để giảm mua sắm bốc đồng.",
            Arrays.asList("critical", "high", "all"),
            8,
            "80%",
            "immediate",
            Arrays.asList(
                "Xóa app khỏi điện thoại",
                "Tắt thông báo email",
                "Unsubscribe newsletters",
                "Chỉ mua khi thực sự cần"
            )
        ));
        
        categoryTips.put("mua sắm", shoppingTips);
        categoryTips.put("shopping", shoppingTips);
    }
    
    private void initializeEntertainmentTips() {
        List<SavingTip> entertainmentTips = new ArrayList<>();
        
        entertainmentTips.add(new SavingTip(
            "Hủy subscription không dùng",
            "Netflix, Spotify Premium không dùng thường xuyên? Hủy ngay. Tiết kiệm 200-400k/tháng.",
            Arrays.asList("critical", "high", "all"),
            9,
            "200-400k/tháng",
            "immediate",
            Arrays.asList(
                "Kiểm tra các subscription đang có",
                "Hủy những cái ít dùng",
                "Dùng bản free hoặc chia sẻ",
                "Review hàng quý"
            )
        ));
        
        entertainmentTips.add(new SavingTip(
            "Hoạt động miễn phí",
            "Công viên, thư viện, triển lãm miễn phí. Tiết kiệm 100%.",
            Arrays.asList("critical", "high", "all"),
            8,
            "100%",
            "immediate",
            Arrays.asList(
                "Đi công viên cuối tuần",
                "Thư viện, bảo tàng miễn phí",
                "Tập thể dục ngoài trời",
                "Picnic thay vì cafe"
            )
        ));
        
        entertainmentTips.add(new SavingTip(
            "Xem phim tại nhà",
            "Xem phim online tại nhà thay vì rạp. Tiết kiệm 80-100k/lần.",
            Arrays.asList("high", "medium", "all"),
            7,
            "80-100k/lần",
            "immediate",
            Arrays.asList(
                "Netflix/YouTube miễn phí",
                "Mua bắp rang tại nhà",
                "Xem với gia đình/bạn bè",
                "Chỉ ra rạp với phim đặc biệt"
            )
        ));
        
        entertainmentTips.add(new SavingTip(
            "Giảm cafe/bar",
            "Cafe/bar 1-2 lần/tuần thay vì hàng ngày. Tiết kiệm 60-70%.",
            Arrays.asList("high", "medium", "all"),
            7,
            "60-70%",
            "short-term",
            Arrays.asList(
                "Giới hạn 1-2 lần/tuần",
                "Chọn chỗ giá rẻ",
                "Uống nước lọc thay đồ uống đắt",
                "Gặp bạn tại công viên"
            )
        ));
        
        entertainmentTips.add(new SavingTip(
            "Game miễn phí",
            "Chơi game free thay vì mua game/nạp tiền. Tiết kiệm 100%.",
            Arrays.asList("medium", "all"),
            6,
            "100%",
            "immediate",
            Arrays.asList(
                "Tìm game free chất lượng",
                "Tham gia event nhận quà",
                "Tránh nạp tiền",
                "Giới hạn thời gian chơi"
            )
        ));
        
        categoryTips.put("giải trí", entertainmentTips);
        categoryTips.put("entertainment", entertainmentTips);
    }
    
    private void initializeHealthTips() {
        List<SavingTip> healthTips = new ArrayList<>();
        
        healthTips.add(new SavingTip(
            "Tập thể dục tại nhà/công viên",
            "Tập gym tại nhà/công viên thay vì phòng gym. Tiết kiệm 500k-1tr/tháng.",
            Arrays.asList("high", "medium", "all"),
            7,
            "500k-1tr/tháng",
            "immediate",
            Arrays.asList(
                "Tập với video YouTube",
                "Chạy bộ công viên",
                "Mua tạ nhỏ tập tại nhà",
                "Join nhóm tập miễn phí"
            )
        ));
        
        healthTips.add(new SavingTip(
            "Mua thuốc generic",
            "Thuốc generic cùng thành phần, rẻ hơn 50-70% thuốc thương hiệu.",
            Arrays.asList("medium", "all"),
            6,
            "50-70%",
            "short-term",
            Arrays.asList(
                "Hỏi bác sĩ thuốc generic",
                "So sánh thành phần",
                "Mua tại nhà thuốc tin cậy",
                "Kiểm tra hạn sử dụng"
            )
        ));
        
        categoryTips.put("sức khỏe", healthTips);
        categoryTips.put("health", healthTips);
    }
    
    private void initializeUtilityTips() {
        List<SavingTip> utilityTips = new ArrayList<>();
        
        utilityTips.add(new SavingTip(
            "Tiết kiệm điện",
            "Tắt đèn/điều hòa khi không dùng. Giảm 20-30% hóa đơn điện.",
            Arrays.asList("medium", "all"),
            6,
            "20-30%",
            "immediate",
            Arrays.asList(
                "Tắt đèn khi ra khỏi phòng",
                "Điều hòa 26°C",
                "Rút phích cắm thiết bị không dùng",
                "Dùng đèn LED"
            )
        ));
        
        utilityTips.add(new SavingTip(
            "Tiết kiệm nước",
            "Tắt vòi khi không dùng, sửa vòi hỏng. Giảm 15-20% hóa đơn.",
            Arrays.asList("medium", "all"),
            5,
            "15-20%",
            "immediate",
            Arrays.asList(
                "Tắt vòi khi đánh răng",
                "Tắm nhanh 5-7 phút",
                "Sửa vòi rò rỉ",
                "Tái sử dụng nước giặt"
            )
        ));
        
        categoryTips.put("hóa đơn", utilityTips);
        categoryTips.put("bills", utilityTips);
    }
    
    private void initializeGeneralTips() {
        generalTips.add(new SavingTip(
            "Quy tắc 50/30/20",
            "50% nhu cầu thiết yếu, 30% cá nhân, 20% tiết kiệm/nợ.",
            Arrays.asList("all"),
            8,
            "Cân bằng tài chính",
            "long-term",
            Arrays.asList(
                "Tính toán chi tiêu hàng tháng",
                "Phân bổ theo tỷ lệ",
                "Điều chỉnh nếu cần",
                "Theo dõi hàng tháng"
            )
        ));
        
        generalTips.add(new SavingTip(
            "Tự động tiết kiệm",
            "Tự động chuyển 10-20% lương vào tài khoản tiết kiệm ngay khi nhận lương.",
            Arrays.asList("all"),
            9,
            "Đảm bảo tiết kiệm",
            "immediate",
            Arrays.asList(
                "Setup auto-transfer",
                "Chuyển ngay khi có lương",
                "Không động vào tiền tiết kiệm",
                "Tăng dần tỷ lệ"
            )
        ));
        
        generalTips.add(new SavingTip(
            "Theo dõi chi tiêu hàng ngày",
            "Ghi chép mỗi khoản chi. Nhận biết thói quen xấu.",
            Arrays.asList("all"),
            8,
            "Kiểm soát chi tiêu",
            "immediate",
            Arrays.asList(
                "Dùng app quản lý tài chính",
                "Ghi chép ngay khi chi",
                "Review cuối ngày",
                "Phân tích cuối tuần"
            )
        ));
    }
    
    private void initializeEmergencyTips() {
        emergencyTips.add(new SavingTip(
            "Ngưng mọi chi tiêu không thiết yếu",
            "1 tháng không mua sắm, giải trí trả phí. Tập trung tiết kiệm.",
            Arrays.asList("critical"),
            10,
            "70-80%",
            "immediate",
            Arrays.asList(
                "Cancel tất cả subscriptions",
                "100% ăn nhà",
                "Chỉ đi xe buýt/đi bộ",
                "Zero shopping",
                "Hoạt động miễn phí"
            )
        ));
        
        emergencyTips.add(new SavingTip(
            "Bán đồ không dùng",
            "Bán đồ cũ trên Facebook, Chợ Tốt. Thu về 2-5 triệu.",
            Arrays.asList("critical", "high"),
            8,
            "2-5 triệu",
            "immediate",
            Arrays.asList(
                "Kiểm kê đồ không dùng",
                "Chụp ảnh đẹp",
                "Đăng Facebook Marketplace",
                "Giá hợp lý để bán nhanh"
            )
        ));
        
        emergencyTips.add(new SavingTip(
            "Làm thêm giờ/freelance",
            "Tăng thu nhập tạm thời. Overtime, freelance online.",
            Arrays.asList("critical", "high"),
            9,
            "Tăng thu nhập",
            "short-term",
            Arrays.asList(
                "Hỏi làm thêm giờ công ty",
                "Tìm việc freelance Fiverr/Upwork",
                "Dạy kèm, gia sư",
                "Part-time cuối tuần"
            )
        ));
    }
    
    // DTO Class
    public static class SavingTip {
        private String title;
        private String description;
        private List<String> applicableLevels; // critical, high, medium, low, all
        private int impactScore; // 1-10
        private String savingsAmount;
        private String timeframe; // immediate, short-term, long-term
        private List<String> actionSteps;
        
        public SavingTip(String title, String description, List<String> applicableLevels,
                       int impactScore, String savingsAmount, String timeframe, List<String> actionSteps) {
            this.title = title; this.description = description;
            this.applicableLevels = applicableLevels; this.impactScore = impactScore;
            this.savingsAmount = savingsAmount; this.timeframe = timeframe;
            this.actionSteps = actionSteps;
        }
        
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public List<String> getApplicableLevels() { return applicableLevels; }
        public int getImpactScore() { return impactScore; }
        public String getSavingsAmount() { return savingsAmount; }
        public String getTimeframe() { return timeframe; }
        public List<String> getActionSteps() { return actionSteps; }
    }
}
