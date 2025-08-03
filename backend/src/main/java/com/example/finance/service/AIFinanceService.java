package com.example.finance.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AIFinanceService {

    // Từ khóa cho các chủ đề tài chính
    private static final Map<String, List<String>> KEYWORDS = new HashMap<>();
    
    static {
        KEYWORDS.put("greeting", Arrays.asList("xin chào", "chào", "hello", "hi", "hế lô", "chào bạn"));
        KEYWORDS.put("saving", Arrays.asList("tiết kiệm", "để dành", "tích lũy", "gửi tiết kiệm", "lãi suất"));
        KEYWORDS.put("budget", Arrays.asList("ngân sách", "chi tiêu", "budget", "quản lý chi tiêu", "kế hoạch tài chính"));
        KEYWORDS.put("investment", Arrays.asList("đầu tư", "chứng khoán", "cổ phiếu", "trái phiếu", "quỹ đầu tư", "bitcoin", "crypto"));
        KEYWORDS.put("debt", Arrays.asList("nợ", "vay", "trả nợ", "thanh toán nợ", "thẻ tín dụng"));
        KEYWORDS.put("expense", Arrays.asList("chi phí", "khoản chi", "tiền chi", "hóa đơn", "thanh toán"));
        KEYWORDS.put("income", Arrays.asList("thu nhập", "lương", "tiền lương", "kiếm tiền", "nguồn thu"));
        KEYWORDS.put("thanks", Arrays.asList("cảm ơn", "thank", "thanks", "cám ơn", "tks"));
        KEYWORDS.put("help", Arrays.asList("giúp", "help", "hướng dẫn", "tư vấn", "lời khuyên"));
    }

    public String processMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Bạn có thể hỏi tôi về bất kỳ vấn đề tài chính nào!";
        }

        String normalizedMessage = message.toLowerCase().trim();
        
        // Phân loại tin nhắn
        String category = categorizeMessage(normalizedMessage);
        
        return generateResponse(category, normalizedMessage);
    }

    private String categorizeMessage(String message) {
        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (message.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return "general";
    }

    private String generateResponse(String category, String message) {
        switch (category) {
            case "greeting":
                return getRandomResponse(Arrays.asList(
                    "Xin chào! Tôi là trợ lý AI tài chính của bạn. Tôi có thể giúp bạn về tiết kiệm, đầu tư, quản lý chi tiêu và nhiều vấn đề tài chính khác.",
                    "Chào bạn! Rất vui được hỗ trợ bạn về các vấn đề tài chính. Bạn muốn tôi tư vấn điều gì?",
                    "Hello! Tôi ở đây để giúp bạn quản lý tài chính tốt hơn. Hãy cho tôi biết bạn cần hỗ trợ gì nhé!"
                ));

            case "saving":
                return getRandomResponse(Arrays.asList(
                    "Tiết kiệm là nền tảng của sự ổn định tài chính! Tôi khuyên bạn nên:\n• Áp dụng quy tắc 50/30/20: 50% cho nhu cầu thiết yếu, 30% cho giải trí, 20% cho tiết kiệm\n• Tự động chuyển tiền tiết kiệm ngay khi nhận lương\n• Tìm tài khoản tiết kiệm có lãi suất cao\n• Đặt mục tiêu tiết kiệm cụ thể",
                    "Để tiết kiệm hiệu quả, bạn có thể:\n• Theo dõi chi tiêu hàng ngày\n• Cắt giảm các khoản chi không cần thiết\n• Mua sắm thông minh với danh sách và so sánh giá\n• Đầu tư vào tài khoản tiết kiệm có kỳ hạn\n• Tạo quỹ khẩn cấp bằng 3-6 tháng chi tiêu",
                    "Bí quyết tiết kiệm:\n• Bắt đầu từ việc nhỏ - tiết kiệm 100,000đ/tháng cũng là bước đầu tốt\n• Sử dụng phương pháp 'trả cho bản thân trước'\n• Tận dụng các chương trình khuyến mãi và cashback\n• Đầu tư vào tài khoản tiết kiệm lãi suất cao"
                ));

            case "budget":
                return getRandomResponse(Arrays.asList(
                    "Lập ngân sách là kỹ năng quan trọng! Hãy thử:\n• Ghi chép tất cả thu chi trong 1 tháng\n• Phân loại chi tiêu: cần thiết, muốn có, tiết kiệm\n• Đặt giới hạn cho từng danh mục\n• Xem xét và điều chỉnh hàng tháng\n• Sử dụng app quản lý chi tiêu",
                    "Để quản lý ngân sách hiệu quả:\n• Áp dụng quy tắc 50/30/20\n• Ưu tiên thanh toán nợ và tiết kiệm\n• Dự trù 10% cho các chi phí bất ngờ\n• Thiết lập mục tiêu tài chính ngắn và dài hạn\n• Đánh giá lại ngân sách mỗi 3 tháng",
                    "Ngân sách thông minh:\n• Bắt đầu với việc theo dõi thu chi hiện tại\n• Xác định các khoản chi cố định và biến đổi\n• Tạo các 'phong bì' tiền cho từng mục đích\n• Luôn để dành tiền cho việc giải trí và thưởng cho bản thân"
                ));

            case "investment":
                return getRandomResponse(Arrays.asList(
                    "Đầu tư là cách để tiền sinh tiền! Một số lời khuyên:\n• Bắt đầu sớm, dù số tiền nhỏ\n• Đa dạng hóa danh mục đầu tư\n• Tìm hiểu trước khi đầu tư vào bất kỳ sản phẩm nào\n• Chỉ đầu tư tiền dư thừa, không vay để đầu tư\n• Kiên nhẫn với đầu tư dài hạn",
                    "Về đầu tư, tôi khuyên:\n• Học hỏi về các loại tài sản: cổ phiếu, trái phiếu, quỹ đầu tư\n• Bắt đầu với quỹ đầu tư chỉ số có phí thấp\n• Đầu tư định kỳ (DCA) để giảm rủi ro\n• Không đầu tư vào thứ mình không hiểu\n• Có kế hoạch dài hạn và kiên trì",
                    "Đầu tư thông minh:\n• Xây dựng quỹ khẩn cấp trước khi đầu tư\n• Phân bổ tài sản theo độ tuổi và mục tiêu\n• Tái đầu tư lợi nhuận để tận dụng lãi kép\n• Thường xuyên xem xét và cân bằng lại danh mục\n• Tránh đầu tư theo cảm xúc"
                ));

            case "debt":
                return getRandomResponse(Arrays.asList(
                    "Quản lý nợ hiệu quả:\n• Liệt kê tất cả các khoản nợ và lãi suất\n• Ưu tiên trả nợ lãi suất cao trước\n• Cân nhắc hợp nhất nợ nếu có thể\n• Tránh tạo thêm nợ mới\n• Thương lượng với ngân hàng về lãi suất",
                    "Để thoát khỏi nợ nần:\n• Áp dụng phương pháp 'tuyết lăn': trả hết nợ nhỏ trước\n• Tăng thu nhập thêm để trả nợ nhanh hơn\n• Cắt giảm chi tiêu không cần thiết\n• Tránh sử dụng thẻ tín dụng khi chưa trả hết nợ\n• Xây dựng kế hoạch trả nợ cụ thể",
                    "Lời khuyên về nợ:\n• Luôn trả tối thiểu đúng hạn để tránh phí phạt\n• Ưu tiên trả nợ thẻ tín dụng (lãi suất cao)\n• Cân nhắc vay hợp nhất với lãi suất thấp hơn\n• Thiết lập tự động trả nợ\n• Tìm hiểu về tư vấn nợ miễn phí nếu cần"
                ));

            case "expense":
                return getRandomResponse(Arrays.asList(
                    "Quản lý chi tiêu hiệu quả:\n• Phân biệt 'muốn có' và 'cần thiết'\n• Áp dụng quy tắc 24h trước khi mua đồ đắt tiền\n• So sánh giá trước khi mua\n• Tận dụng cashback và khuyến mãi\n• Thiết lập ngân sách cho từng danh mục chi tiêu",
                    "Để kiểm soát chi tiêu:\n• Ghi chép mọi khoản chi, dù nhỏ\n• Xem xét chi tiêu hàng tuần\n• Tìm cách tiết kiệm ở các khoản chi lớn\n• Sử dụng phương pháp thanh toán tiền mặt\n• Tránh mua sắm khi căng thẳng hoặc buồn",
                    "Chi tiêu thông minh:\n• Lập kế hoạch mua sắm trước\n• Mua theo mùa để được giá tốt\n• Cân nhắc mua đồ cũ chất lượng\n• Đầu tư vào chất lượng cho đồ dùng lâu dài\n• Tính toán chi phí trên mỗi lần sử dụng"
                ));

            case "income":
                return getRandomResponse(Arrays.asList(
                    "Tăng thu nhập:\n• Phát triển kỹ năng để thăng tiến trong công việc\n• Tìm kiếm nguồn thu nhập thụ động\n• Làm thêm công việc phù hợp với kỹ năng\n• Đầu tư vào giáo dục và chứng chỉ\n• Khởi nghiệp hoặc kinh doanh nhỏ",
                    "Đa dạng hóa thu nhập:\n• Phát triển kỹ năng số để làm freelance\n• Cho thuê tài sản nếu có\n• Bán sản phẩm handmade hoặc dịch vụ\n• Đầu tư để tạo thu nhập thụ động\n• Tham gia các khóa học nâng cao nghiệp vụ",
                    "Quản lý thu nhập:\n• Tự động phân chia thu nhập ngay khi nhận lương\n• Đầu tư một phần vào bản thân\n• Tránh lạm phát lối sống khi thu nhập tăng\n• Đàm phán lương thường xuyên\n• Tạo nhiều nguồn thu nhập khác nhau"
                ));

            case "thanks":
                return getRandomResponse(Arrays.asList(
                    "Không có gì! Tôi luôn sẵn sàng hỗ trợ bạn về các vấn đề tài chính. Hãy hỏi tôi bất cứ khi nào cần nhé!",
                    "Rất vui được giúp đỡ! Nếu bạn có thêm câu hỏi về tài chính, đừng ngần ngại hỏi tôi.",
                    "Tôi rất vui khi có thể hỗ trợ bạn! Chúc bạn quản lý tài chính thành công!"
                ));

            case "help":
                return "Tôi có thể giúp bạn về:\n\n💰 **Tiết kiệm**: Lập kế hoạch tiết kiệm, tìm tài khoản lãi suất cao\n📊 **Ngân sách**: Quản lý chi tiêu, phân bổ thu nhập\n📈 **Đầu tư**: Tư vấn đầu tư cơ bản, quản lý rủi ro\n💳 **Quản lý nợ**: Chiến lược trả nợ, tối ưu hóa nợ\n🏠 **Chi tiêu**: Kiểm soát chi phí, mua sắm thông minh\n💼 **Thu nhập**: Tăng thu nhập, đa dạng hóa nguồn thu\n\nHãy hỏi tôi về bất kỳ chủ đề nào bạn quan tâm!";

            default:
                return generateGeneralResponse(message);
        }
    }

    private String generateGeneralResponse(String message) {
        List<String> generalResponses = Arrays.asList(
            "Đó là một câu hỏi hay! Tuy tôi chuyên về tài chính, nhưng tôi luôn sẵn sàng lắng nghe. Bạn có muốn tôi tư vấn về tiết kiệm, đầu tư, hoặc quản lý chi tiêu không?",
            "Cảm ơn bạn đã chia sẻ! Như một trợ lý AI tài chính, tôi có thể giúp bạn về ngân sách, tiết kiệm, đầu tư và quản lý nợ. Bạn quan tâm đến chủ đề nào?",
            "Tôi hiểu! Tuy tôi tập trung vào lĩnh vực tài chính, nhưng tôi nghĩ mọi thứ đều liên quan đến tiền bạc theo cách nào đó. Bạn có muốn tôi tư vấn về quản lý tài chính cá nhân không?",
            "Thật thú vị! Là một AI chuyên về tài chính, tôi luôn sẵn sàng giúp bạn với các vấn đề về tiền bạc. Bạn có câu hỏi nào về tiết kiệm, đầu tư, hoặc chi tiêu không?"
        );
        
        return getRandomResponse(generalResponses);
    }

    private String getRandomResponse(List<String> responses) {
        Random random = new Random();
        return responses.get(random.nextInt(responses.size()));
    }
}
