# 💰 Financial Management App
## Ứng dụng Quản Lý Tài Chính Cá Nhân Thông Minh với AI Tiếng Việt

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot) 
[![Node.js](https://img.shields.io/badge/Node.js-18%2B-green.svg)](https://nodejs.org/) 
[![Python](https://img.shields.io/badge/Python-3.9%2B-blue.svg)](https://python.org/) 
[![AI Accuracy](https://img.shields.io/badge/AI%20Accuracy-90.47%25-blue.svg)](./ai-service) 
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)

**Version:** 3.0.0 | **Status:** ✅ Production Ready | **Last Updated:** December 2025 ---

## 📋 Mục Lục

1. [Giới Thiệu](#giới-thiệu)
2. [Tính Năng Chính](#tính-năng-chính)
3. [Công Nghệ Sử Dụng](#công-nghệ-sử-dụng)
4. [Cài Đặt](#cài-đặt)
5. [Hướng Dẫn Sử Dụng](#hướng-dẫn-sử-dụng)
6. [API Documentation](#api-documentation)
7. [Khắc Phục Lỗi](#khắc-phục-lỗi)

---

## 🎯 Giới Thiệu

**Financial Management App** là ứng dụng quản lý tài chính cá nhân toàn diện với hệ thống AI tiếng Việt tích hợp, giúp người dùng:

✅ **Quản lý giao dịch** - Tự động phân loại thu/chi với độ chính xác **90.47%**  
✅ **Quản lý ngân sách** - Đặt, theo dõi và nhận cảnh báo ngân sách  
✅ **Lập kế hoạch tài chính** - AI giúp lập kế hoạch tiết kiệm chi tiết  
✅ **Phân tích tài chính** - Dashboard tương tác, báo cáo thông minh  
✅ **Chat AI** - Tư vấn tài chính từ chatbot học máy  

### 📊 Chỉ Số Chính

| Chỉ Số | Giá Trị | Mô Tả |
|--------|--------|--------|
| **Độ Chính Xác AI** | 90.47% | Phân loại giao dịch tiếng Việt |
| **Dữ Liệu Huấn Luyện** | 200K mẫu | Giao dịch tiếng Việt chất lượng cao (41.37 MB) |
| **Tốc Độ Xử Lý** | 844/s | Giao dịch trên giây |
| **Thư Viện ML** | 9 | XGBoost, LightGBM, Prophet, SHAP, v.v. |
| **API Endpoints** | 25+ | Đầy đủ phủ sóng REST API |
| **Danh Mục** | 8+ | Ăn uống, Giao thông, Mua sắm, v.v. |

---

## ⚡ Tính Năng Chính ### 💳 1. Quản Lý Giao Dịch
- ➕ Thêm, sửa, xóa giao dịch thu/chi
- 🤖 **AI Tự Động Phân Loại** - Độ chính xác 90.47%
- 📊 Thống kê theo thời gian, danh mục, ví tiền
- 📎 Đính kèm file (hoá đơn, biên lai)
- 🔄 Giao dịch định kỳ (hàng ngày/tuần/tháng)

### 💰 2. Quản Lý Ví Tiền
- 🏦 Hỗ trợ nhiều ví (tiền mặt, ngân hàng, v.v.)
- 💱 Chuyển tiền giữa các ví
- 📈 Theo dõi số dư theo thời gian
- 🔐 Bảo mật bằng JWT Token

### 📁 3. Danh Mục Chi Tiêu
- 🎨 8 danh mục mặc định + tùy chỉnh
- 🌈 **Hệ Thống Màu Thông Minh** - 14+ màu sắc
- 📊 Theo dõi chi tiêu mỗi danh mục
- 💡 AI insights cho từng danh mục

### 🎯 4. Ngân Sách
- 💵 Đặt ngân sách tháng/danh mục
- ⚠️ **Cảnh Báo Email** khi vượt 80%
- 🤖 **Đề Xuất Ngân Sách Thông Minh** (AI)
- 📊 Theo dõi sử dụng ngân sách theo thời gian

### 🏆 5. Mục Tiêu Tiết Kiệm
- 🎯 Đặt mục tiêu tiết kiệm
- 📈 Theo dõi tiến độ
- 🗺️ **Lộ Trình Tiết Kiệm Chi Tiết** - AI lập kế hoạch
- ✨ Cập nhật trạng thái tự động khi hoàn thành

### 🤖 6. AI Chatbot Tài Chính
- 💬 Chat tự nhiên tiếng Việt
- 🔍 **Smart Analytics** - 7 loại truy vấn
- 💡 Sinh insights ngữ cảnh
- ⚡ Gợi ý hành động nhanh
- 📥 Export báo cáo (sao chép/tải/in)

**Các truy vấn được hỗ trợ:**
```
- "Chi tiêu tháng này?" → Phân tích chi tiêu tháng
- "Tiền ăn uống tuần này?" → Chi tiêu ăn uống trong tuần
- "So sánh tháng này với tháng trước" → So sánh xu hướng
- "Top 5 khoản chi lớn nhất?" → Chi tiêu cao nhất
- "Tạo báo cáo tổng hợp" → Tự động sinh báo cáo
```

---

## 🧠 Hệ Thống AI Tiếng Việt

### 🚀 Ultra AI Budget - 9 Thư Viện ML

| Thư Viện | Công Dụng | Trạng Thái |
|----------|-----------|-----------|
| **XGBoost** | Gradient boosting (+10-20% độ chính xác) | ✅ |
| **LightGBM** | Boosting nhanh (3-5x nhanh hơn) | ✅ |
| **Prophet** | Dự báo chuỗi thời gian (Facebook) | ✅ |
| **SHAP** | Giải thích AI | ✅ |
| **Optuna** | Tự động điều chỉnh hyperparameter | ✅ |
| **SMOTE/ADASYN** | Xử lý dữ liệu không cân bằng | ✅ |
| **TextBlob** | Phân tích cảm xúc | ✅ |
| **VADER** | Cảm xúc mạng xã hội | ✅ |
| **Word2Vec** | Embedding từ | ✅ |

**Khả Năng:**
- 📊 Dự báo ensemble (XGBoost + LightGBM)
- 🔮 Dự báo xu hướng chi tiêu 6-12 tháng
- 📈 Phân tích rủi ro tài chính
- ⚙️ Tự động tối ưu hóa ngân sách
- 📉 Dự báo chuỗi thời gian với Prophet

---

## 🛠️ Công Nghệ Sử Dụng ### 🖥️ Backend
- **Spring Boot 3.x** - Framework Java doanh nghiệp
- **Spring Security + JWT** - Xác thực & phân quyền
- **Spring Data JPA** - Truy cập dữ liệu
- **SQL Server** - Cơ sở dữ liệu chính
- **Lombok** - Giảm boilerplate code
- **Spring Mail** - Gửi email thông báo
- **MapStruct** - Mapping DTO

### 💻 Frontend
- **Node.js + Express** - Server-side rendering
- **EJS** - Template engine
- **Vanilla JavaScript** - Client-side logic
- **Bootstrap 5** - CSS framework
- **Chart.js** - Biểu đồ tương tác
- **Axios** - HTTP client

### 🤖 AI Service
- **FastAPI 2.0** - API hiệu suất cao
- **scikit-learn** - Random Forest classifier
- **XGBoost + LightGBM** - Gradient boosting
- **Prophet** - Dự báo chuỗi thời gian
- **SHAP** - Giải thích AI
- **Optuna** - Điều chỉnh hyperparameter
- **Underthesea + pyvi** - Xử lý NLP tiếng Việt
- **NumPy + Pandas** - Xử lý dữ liệu

### 🗄️ Database
- **SQL Server 2019+** - Cơ sở dữ liệu chính
- **Spring Data JPA** - ORM

---

## 📦 Cài Đặt

### ⚙️ Yêu Cầu Trước

- Java 17+
- Node.js 18+
- Python 3.9+
- SQL Server 2019+
- Maven 3.8+

### 1️⃣ Clone Repository

```bash
git clone https://github.com/trtahhh/financial-management-app.git
cd financial-management-app
```

### 2️⃣ Cài Đặt Cơ Sở Dữ Liệu

```sql
CREATE DATABASE FinancialManagement;
USE FinancialManagement;
-- Chạy file: database/schema/FinancialManagement_Complete_Fixed.sql
```

Cập nhật `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=FinancialManagement
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```
chạy orc 
python ocr_api.py 
### 3️⃣ Khởi Động AI Service (Bắt Buộc)

```bash
cd ai-service
pip install -r requirements.txt

# Windows
.\start_service.ps1

# Hoặc chạy thủ công
python main.py
```

AI Service chạy tại: `http://localhost:8001`

### 4️⃣ Khởi Động Backend

```bash
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend chạy tại: `http://localhost:8080`

### 5️⃣ Khởi Động Frontend

```bash
cd frontend
npm install
npm start
```

Frontend chạy tại: `http://localhost:3000`

---

## 📖 Hướng Dẫn Sử Dụng

### 💳 Thêm Giao Dịch

1. Vào **Giao Dịch** → **Thêm Giao Dịch**
2. Nhập thông tin:
   - **Loại**: Thu/Chi
   - **Số Tiền**: 500,000 VND
   - **Danh Mục**: AI sẽ tự động gợi ý (Ăn uống)
   - **Ví Tiền**: Tiền mặt
   - **Mô Tả**: "Cà phê Starbucks"
3. Klik **Lưu**

### 🤖 Sử Dụng AI Chat

Vào **Chat** và hỏi:

```
"Chi tiêu tháng này?"
→ AI phân tích và báo cáo chi tiêu tháng

"Tạo báo cáo tổng hợp"
→ Tự động sinh báo cáo chi tiết

"Tiết kiệm 2 triệu/tháng được không?"
→ AI lập kế hoạch tiết kiệm cụ thể
```

### 🎯 Quản Lý Ngân Sách

1. Vào **Ngân Sách** → **Đề Xuất Thông Minh**
2. Chọn khoảng phân tích: 1/3/6 tháng
3. Klik **Lấy Đề Xuất** → AI phân tích mẫu chi tiêu
4. Klik **Áp Dụng** → Tự động tạo ngân sách

### 🏆 Lộ Trình Tiết Kiệm

1. Vào **Mục Tiêu** → **Lộ Trình Tiết Kiệm**
2. Nhập:
   - **Mục Tiêu**: 10,000,000 VND
   - **Mục Đích**: Nhà/Xe/Du Lịch
3. AI sinh lộ trình chi tiết:
   - ⏰ Timeline cần thiết
   - 💰 Số tiền tiết kiệm/tháng
   - 📋 Các bước cụ thể
   - 💡 Mẹo tối ưu hóa

---

## 🔌 API Documentation ### 🤖 API Giao Dịch

#### Phân Loại Giao Dịch
```bash
POST /api/ai/classify
Content-Type: application/json

{
  "text": "Mua cà phê Highlands 50000 VND"
}

Response:
{
  "category": "ăn uống",
  "confidence": 0.94
}
```

#### Smart Analytics
```bash
GET /api/ai/smart-analytics

Response:
{
  "healthScore": 75,
  "insights": ["Chi tiêu tăng 15%", "Tiền ăn uống cao nhất"],
  "recommendations": ["Giảm ăn ngoài", "Quản lý ngân sách tốt hơn"]
}
```

### 📋 Danh Sách API Chính

| Endpoint | Phương Thức | Mô Tả |
|----------|-----------|--------|
| `/api/transactions` | GET/POST | Lấy/Tạo giao dịch |
| `/api/categories` | GET/POST | Quản lý danh mục |
| `/api/budgets` | GET/POST | Quản lý ngân sách |
| `/api/goals` | GET/POST | Quản lý mục tiêu |
| `/api/wallets` | GET/POST | Quản lý ví tiền |
| `/api/ai/classify` | POST | Phân loại AI |
| `/api/ai/smart-analytics` | GET | Analytics thông minh |
| `/api/ai/smart-budget` | GET | Đề xuất ngân sách |
| `/api/ai/chat` | POST | Chat với AI |
| `/api/dashboard/data` | GET | Dữ liệu dashboard |

📚 Xem tài liệu đầy đủ: [AI Service README](./ai-service/README.md)

---

## 📊 Kiến Trúc Cơ Sở Dữ Liệu

### Bảng Chính

- **Users** - Tài khoản người dùng
- **Transactions** - Ghi chép thu/chi
- **Categories** - Danh mục chi tiêu
- **Wallets** - Ví tiền
- **Budgets** - Ngân sách
- **Goals** - Mục tiêu tiết kiệm
- **Notifications** - Thông báo
- **RecurringTransactions** - Giao dịch định kỳ

### Mối Quan Hệ

```
User (1) ──→ (*) Transactions
User (1) ──→ (*) Categories
User (1) ──→ (*) Wallets
User (1) ──→ (*) Budgets
User (1) ──→ (*) Goals
User (1) ──→ (*) Notifications
```

---

## 🚀 Hiệu Suất

| Chỉ Số | Giá Trị |
|--------|--------|
| Phản hồi API | < 100ms |
| Phân loại AI | < 50ms |
| Truy vấn DB | < 20ms |
| Load Frontend | < 2s |
| Người dùng đồng thời | 100+ |

---

## ❌ Khắc Phục Lỗi

### ❓ AI Service không khởi động

```bash
# Kiểm tra phiên bản Python
python --version
# Phải >= 3.9

# Cài đặt lại dependencies
cd ai-service
pip install -r requirements.txt --force-reinstall

# Khởi động thủ công
python main.py
```

### ❓ Backend không kết nối cơ sở dữ liệu

```sql
-- Kiểm tra kết nối
sqlcmd -S localhost -U sa -P password

-- Kiểm tra cơ sở dữ liệu
USE FinancialManagement;
SELECT COUNT(*) FROM Users;
```

### ❓ Lỗi PRIMARY KEY trong Transactions

```sql
-- Reset IDENTITY sequence
DBCC CHECKIDENT ('Transactions', RESEED, 0);
```

### ❓ Email thông báo không gửi

```properties
# Kiểm tra cài đặt Gmail App Password
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

# Bật thông báo
notification.email.budget-alerts=true
```

### ❓ Frontend không kết nối Backend

```bash
# Kiểm tra file .env hoặc config
# Đảm bảo backend đang chạy ở http://localhost:8080

# Khởi động lại frontend
cd frontend
npm start
```

### ❓ Transaction filtering không hoạt động

- ✅ Kiểm tra xem date range filter có được chọn không
- ✅ Mở DevTools Console để xem lỗi
- ✅ Đảm bảo backend API trả về dữ liệu đúng

---

## 🤝 Đóng Góp

1. Fork repository
2. Tạo branch feature: `git checkout -b feature/your-feature`
3. Commit changes: `git commit -m "Add your feature"`
4. Push to branch: `git push origin feature/your-feature`
5. Tạo Pull Request

---

## 📝 License

MIT License - Xem [LICENSE](LICENSE)

---

## 📞 Hỗ Trợ

- 📧 Email: support@financeapp.com
- 🐛 Issues: [GitHub Issues](https://github.com/trtahhh/financial-management-app/issues)
- 📚 Wiki: [Documentation](https://github.com/trtahhh/financial-management-app/wiki)

---

## 🎉 Cảm Ơn

Cảm ơn bạn đã sử dụng **Financial Management App**!

Nếu thấy hữu ích, hãy **⭐ Star** repository này.

<div align="center">

**Made with ❤️ by Financial Management Team**

[🏠 Home](.) | [🤖 AI Service](./ai-service) | [📚 Docs](./docs) | [🐛 Issues](https://github.com/trtahhh/financial-management-app/issues)

</div> 