# Financial Management App

Ứng dụng quản lý tài chính cá nhân với giao diện web hiện đại và AI Chat hỗ trợ.

## 🚀 Tính năng chính

### 💰 Quản lý tài chính
- **Giao dịch**: Thêm, sửa, xóa giao dịch thu chi
- **Danh mục**: Quản lý danh mục chi tiêu tùy chỉnh
- **Ví**: Quản lý nhiều ví tiền khác nhau
- **Ngân sách**: Thiết lập và theo dõi ngân sách theo tháng
- **Mục tiêu**: Đặt và theo dõi mục tiêu tài chính
- **Giao dịch định kỳ**: Tự động tạo giao dịch theo lịch

### 🤖 AI Chat thông minh
- **Tư vấn tài chính**: Lời khuyên về tiết kiệm, đầu tư, quản lý chi tiêu
- **Báo cáo tự động**: Tạo báo cáo tài chính bằng AI
- **Hỗ trợ đa ngôn ngữ**: Giao tiếp bằng tiếng Việt
- **Phân tích thông minh**: Hiểu ý định người dùng và đưa ra gợi ý phù hợp

### 🚀 AI Phân tích nâng cao (MỚI)
- **Phân tích tài chính toàn diện**: Đánh giá tình hình dựa trên dữ liệu thực tế
- **Dự báo tài chính tương lai**: Dự báo 3, 6, 12 tháng tới
- **Phân tích xu hướng chi tiêu**: Pattern và chu kỳ chi tiêu
- **Tối ưu hóa ngân sách**: Cải thiện hiệu quả quản lý ngân sách
- **Phân tích rủi ro tài chính**: Đánh giá và bảo vệ tài chính
- **Lời khuyên đầu tư cá nhân hóa**: Tư vấn theo profile cá nhân

### 📊 Báo cáo tích hợp
- **Báo cáo tổng hợp**: Tổng quan thu chi, chi tiêu theo danh mục
- **Báo cáo giao dịch**: Chi tiết giao dịch theo thời gian
- **Báo cáo ngân sách**: Theo dõi sử dụng ngân sách
- **Xuất báo cáo**: Copy, tải về, in báo cáo trực tiếp từ AI Chat

### 🔐 Bảo mật
- **JWT Authentication**: Xác thực người dùng an toàn
- **Mã hóa mật khẩu**: Sử dụng BCrypt
- **Phân quyền**: Mỗi người dùng chỉ thấy dữ liệu của mình

## 🛠️ Công nghệ sử dụng

### Backend
- **Spring Boot 3.x**: Framework Java hiện đại
- **Spring Security**: Bảo mật và xác thực
- **Spring Data JPA**: Truy cập cơ sở dữ liệu
- **SQL Server**: Cơ sở dữ liệu chính
- **JWT**: Xác thực token
- **Lombok**: Giảm boilerplate code

### Frontend
- **Node.js + Express**: Server-side rendering
- **EJS**: Template engine
- **Vanilla JavaScript**: Giao diện người dùng
- **Bootstrap 5**: CSS framework
- **Chart.js**: Biểu đồ tương tác

### AI & Báo cáo
- **OpenRouter API**: Kết nối với các mô hình AI
- **ReportService**: Tạo báo cáo tự động
- **Text Export**: Xuất báo cáo dạng văn bản
- **Smart Parsing**: Phân tích yêu cầu báo cáo từ tin nhắn

### AI Phân tích nâng cao
- **AIFinancialAnalysisService**: Service phân tích tài chính AI
- **AIFinancialAnalysisController**: API endpoints cho phân tích nâng cao
- **Prompt Engineering**: Tối ưu hóa prompt cho từng loại phân tích
- **Data Context**: Cung cấp context dữ liệu thực tế cho AI

## 📱 Cách sử dụng

### 1. Khởi động ứng dụng
```bash
# Backend (Spring Boot)
cd backend
./mvnw spring-boot:run

# Frontend (Node.js)
cd frontend
npm install
npm start
```

### 2. Sử dụng AI Chat
- Truy cập trang **AI Chat**
- Gõ câu hỏi về tài chính
- Sử dụng các template có sẵn để tạo báo cáo

### 3. Tạo báo cáo bằng AI
```
# Báo cáo tổng hợp
"Tạo báo cáo tổng hợp tháng này"

# Báo cáo giao dịch
"Báo cáo giao dịch từ 01/01 đến 31/01"

# Báo cáo ngân sách
"Báo cáo ngân sách tháng 12 năm 2024"
```

### 4. Sử dụng AI Phân tích nâng cao
```
# Phân tích tài chính
"Phân tích thực tế tình hình tài chính của tôi"
"Tình hình hiện tại"

# Dự báo tương lai
"Dự báo tài chính 6 tháng tới"
"Dự báo tài chính 12 tháng tới"

# Phân tích xu hướng
"Phân tích xu hướng chi tiêu"
"Phân tích pattern chi tiêu"

# Tối ưu hóa
"Tối ưu hóa ngân sách"
"Cải thiện hiệu quả ngân sách"

# Phân tích rủi ro
"Phân tích rủi ro tài chính"
"Bảo vệ tài chính"

# Tư vấn đầu tư
"Lời khuyên đầu tư cá nhân"
"Tư vấn đầu tư theo profile"
```

### 4. Xuất báo cáo
- **Copy**: Sao chép vào clipboard
- **Tải về**: Lưu file .txt
- **In**: In báo cáo trực tiếp

## 🔧 Cấu hình

### Database
```properties
# application.properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=FinancialManagement
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### AI Chat
```properties
# OpenRouter API
openrouter.api.key=your_api_key
openrouter.api.url=https://openrouter.ai/api/v1
```

## 📊 Cấu trúc cơ sở dữ liệu

### Bảng chính
- **Users**: Thông tin người dùng
- **Transactions**: Giao dịch thu chi
- **Categories**: Danh mục chi tiêu
- **Wallets**: Ví tiền
- **Budgets**: Ngân sách
- **Goals**: Mục tiêu tài chính

### Quan hệ
- User → Transactions (1:N)
- User → Categories (1:N)
- User → Wallets (1:N)
- User → Budgets (1:N)
- User → Goals (1:N)

## 🚨 Xử lý lỗi

### Transaction Issues
**Lỗi "Violation of PRIMARY KEY constraint" khi tạo giao dịch:**
- **Nguyên nhân**: IDENTITY sequence bị lỗi hoặc mapper set ID thủ công
- **Giải pháp**:
  1. Restart backend sau khi sửa code
  2. Chạy script `database/fix_transaction_sequence.sql` để reset sequence
  3. Kiểm tra database có dữ liệu cũ với ID trùng không
- **Files đã sửa**:
  - `TransactionMapperImpl.java` (comment out `entity.setId()`)
  - `TransactionService.java` (xử lý create/update riêng biệt)

### Budget Issues
**Lỗi "Category is required" khi tạo ngân sách:**
- **Nguyên nhân**: Frontend gửi `category_id` nhưng backend DTO mong đợi `categoryId`
- **Giải pháp**:
  1. Restart frontend sau khi sửa code
  2. Kiểm tra form validation hoạt động đúng
  3. Đảm bảo đã chọn danh mục trước khi submit
- **Files đã sửa**:
  - `frontend/public/js/budgets.js` (sửa field mapping và validation)

### AI Chat Issues
**Lỗi kết nối AI hoặc không tạo được báo cáo:**
- **Nguyên nhân**: API key hết hạn hoặc lỗi kết nối
- **Giải pháp**:
  1. Kiểm tra OpenRouter API key trong `application.properties`
  2. Restart backend sau khi cập nhật cấu hình
  3. Kiểm tra log backend để debug

## 🔄 Cập nhật gần đây

### v2.1.0 - AI Chat tích hợp báo cáo
- ✅ Tích hợp chức năng báo cáo vào AI Chat
- ✅ Tự động nhận diện yêu cầu báo cáo
- ✅ Hỗ trợ 3 loại báo cáo: tổng hợp, giao dịch, ngân sách
- ✅ Xuất báo cáo: copy, tải về, in
- ✅ Template báo cáo có sẵn
- ✅ Giao diện chat hiện đại và responsive

### v2.0.0 - Cải thiện giao diện
- ✅ Thiết kế lại UI/UX
- ✅ Responsive design cho mobile
- ✅ Cải thiện performance
- ✅ Sửa lỗi giao dịch và ngân sách

## 📝 Ghi chú phát triển

### AI Chat Integration
- **ReportService**: Xử lý logic tạo báo cáo
- **AIFinanceService**: Tích hợp báo cáo vào AI Chat
- **Smart Parsing**: Phân tích yêu cầu từ tin nhắn tự nhiên
- **Export Options**: Copy, download, print báo cáo

### Performance Optimization
- **Lazy Loading**: Tải dữ liệu theo nhu cầu
- **Caching**: Cache các báo cáo thường dùng
- **Async Processing**: Xử lý báo cáo không đồng bộ

## 🤝 Đóng góp

1. Fork repository
2. Tạo feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## 📄 License

Dự án này được phát hành dưới MIT License - xem file [LICENSE](LICENSE) để biết thêm chi tiết.

## 📞 Hỗ trợ

Nếu gặp vấn đề hoặc có câu hỏi:
- Tạo issue trên GitHub
- Liên hệ qua email: support@financeapp.com
- Tham khảo tài liệu API: `/api/docs`

---

**Lưu ý**: Đây là dự án demo, vui lòng không sử dụng cho mục đích sản xuất mà không có kiểm tra bảo mật đầy đủ.
