# 💰 Financial Management App

> Ứng dụng quản lý tài chính cá nhân thông minh với AI tiếng Việt - Industrial Scale Production Ready

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![FastAPI](https://img.shields.io/badge/FastAPI-2.0-009688.svg)](https://fastapi.tiangolo.com/)
[![AI Accuracy](https://img.shields.io/badge/AI%20Accuracy-90.47%25-blue.svg)](./ai-service)
[![License](https://img.shields.io/badge/license-MIT-orange.svg)](LICENSE)

**Version:** 3.0.0 | **Status:** Production Ready ✅

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#️-tech-stack)
- [Installation](#-installation)
- [Usage Guide](#-usage-guide)
- [AI Features](#-ai-features-details)
- [API Documentation](#-api-documentation)
- [Database Schema](#️-database-schema)
- [Troubleshooting](#-troubleshooting)

---

## 🎯 Overview

Financial Management App is a comprehensive personal finance management system with **industrial-scale Vietnamese AI**, helping users:

- ✅ Auto-manage income/expenses with **90.47% AI classification accuracy**
- ✅ Smart financial planning with **9 advanced ML libraries**
- ✅ Personalized financial advice from **RAG Chatbot**
- ✅ Track budgets & goals with **interactive dashboard**
- ✅ Auto financial reports with **AI Analytics**

### 🏆 Key Metrics

| Metric | Value | Description |
|--------|-------|-------------|
| **AI Accuracy** | 90.47% | Vietnamese transaction classification |
| **Training Data** | 200K samples | High-quality Vietnamese transactions (41.37 MB) |
| **Processing Speed** | 844/s | Transactions per second |
| **ML Libraries** | 9 advanced | XGBoost, LightGBM, Prophet, SHAP, etc. |
| **API Endpoints** | 25+ | Complete REST API coverage |
| **Categories** | 8 | Food, Transport, Shopping, etc. |

---

## 🚀 Features

### 💰 **Core Financial Management**

#### 1. **Transactions**
- ➕ Add, edit, delete income/expense transactions
- 🤖 **AI Auto-Categorization** - 90.47% accuracy
- 📊 Statistics by time, category, wallet
- 📁 File attachments (receipts, documents)
- 🔄 Recurring transactions (daily/weekly/monthly)

#### 2. **Categories**
- 📂 8 default categories + custom
- 🎨 **Smart Color System** - 14+ distinct colors
- 📊 Spending tracking per category
- 💡 AI insights for each category

#### 3. **Wallets**
- 💳 Multiple wallets (cash, bank, etc.)
- 💱 Transfer between wallets
- 📈 Real-time balance tracking
- 🔒 Secured with JWT authentication

#### 4. **Budgets**
- 💰 Set monthly/category budgets
- 📧 **Email Alerts** when exceeding 80%
- 🤖 **Smart Budget Recommendations** (AI)
- 📊 Real-time budget usage tracking

#### 5. **Goals**
- 🎯 Set savings goals
- 📈 Progress tracking
- 🤖 **AI Planning Wizard** - Smart planning
- 💰 **Savings Path** - Detailed savings roadmap
- ✅ Auto status update on completion

### 🤖 **Vietnamese AI System**

#### **Ultra AI Budget (9 ML Libraries)**
Most advanced budget AI system with:

| Library | Use Case | Status |
|---------|----------|--------|
| **XGBoost** | Gradient boosting (+10-20% accuracy) | ✅ |
| **LightGBM** | Fast boosting (3-5x faster) | ✅ |
| **Prophet** | Time series forecasting (Facebook) | ✅ |
| **SHAP** | AI explainability | ✅ |
| **Optuna** | Auto hyperparameter tuning | ✅ |
| **SMOTE/ADASYN** | Imbalanced data handling | ✅ |
| **TextBlob** | Sentiment analysis | ✅ |
| **VADER** | Social media sentiment | ✅ |
| **Word2Vec** | Word embeddings | ✅ |

**Capabilities:**
- 📊 Ensemble predictions (XGBoost + LightGBM)
- 🔮 6-12 month spending trend forecasting
- 🎯 Financial risk analysis
- 💡 Auto budget optimization
- 📈 Time series forecasting with Prophet

#### **MoMo Chat with Ultra AI**
Momo Moni-style financial chat AI:

- 💬 Natural Vietnamese conversation
- 📊 Smart Analytics (7 query types)
- 📈 Contextual insights generation
- ⚡ Quick action suggestions
- 📄 Export reports (copy/download/print)

**Supported Queries:**
```
- "Chi tiêu tháng này" (This month spending)
- "Tiền ăn uống tuần này" (Food spending this week)
- "So sánh tháng này với tháng trước" (Compare months)
- "Top 5 khoản chi lớn nhất" (Top 5 expenses)
- "Tạo báo cáo tổng hợp" (Create summary report)
```

#### **AI Features List**

1. **Auto-Categorization** - 90.47% accuracy, 844 samples/s
2. **Personalized Tips** - Category-specific recommendations
3. **Smart Analytics** - Financial Health Score + insights
4. **Smart Budget** - AI-powered budget recommendations
5. **Savings Tips** - Knowledge base with 6 Vietnamese guides
6. **Savings Path** - Detailed savings roadmap
7. **RAG Advisor** - Context-aware financial advice
8. **Overspending Detection** - Real-time alerts
9. **Ultra Planning** - 9 ML libraries integration
10. **MoMo Chat** - Natural language financial assistant

---

## 🛠️ Tech Stack

### **Backend**
- Spring Boot 3.x - Enterprise Java framework
- Spring Security - JWT authentication
- Spring Data JPA - Database access
- SQL Server - Primary database
- Lombok - Reduce boilerplate
- Spring Mail - Email notifications
- MapStruct - DTO mapping

### **Frontend**
- Node.js + Express - Server-side rendering
- EJS - Template engine
- Vanilla JavaScript - Client-side
- Bootstrap 5 - CSS framework
- Chart.js - Interactive charts
- Axios - HTTP client

### **AI Service** ([Details](./ai-service))
- FastAPI 2.0 - High-performance API
- scikit-learn - Random Forest classifier
- XGBoost + LightGBM - Gradient boosting
- Prophet - Time series forecasting
- SHAP - AI explainability
- Optuna - Hyperparameter tuning
- SMOTE/ADASYN - Imbalanced data
- TextBlob + VADER - Sentiment analysis
- Word2Vec - Word embeddings
- Underthesea + pyvi - Vietnamese NLP
- NumPy + Pandas - Data processing

---

## 🚀 Installation

### **Prerequisites**
- Java 17+
- Node.js 18+
- Python 3.9+
- SQL Server 2019+
- Maven 3.8+

### **1. Clone Repository**
```bash
git clone https://github.com/trtahhh/financial-management-app.git
cd financial-management-app
```

### **2. Setup Database**
```sql
CREATE DATABASE FinancialManagement;
USE FinancialManagement;
-- Run: database/schema/FinancialManagement_Complete_Fixed.sql
```

Update `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=FinancialManagement
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### **3. Start AI Service** (Required)
```bash
cd ai-service
pip install -r requirements.txt

# Windows
.\start_service.ps1

# Manual
python main.py
```
AI Service: `http://localhost:8001`

### **4. Start Backend**
```bash
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```
Backend: `http://localhost:8080`

### **5. Start Frontend**
```bash
cd frontend
npm install
npm start
```
Frontend: `http://localhost:3000`

---

## 📱 Usage Guide

### **Basic Operations**

**Add Transaction:**
```
Transactions → "Add Transaction"
- Type: Income/Expense
- Amount: 500000
- Category: Food (AI auto-suggests)
- Wallet: Cash
- Description: "Starbucks coffee"
```

**AI Chat:**
```
Chat → Ask questions:

"Chi tiêu tháng này?" → AI analyzes spending
"Tạo báo cáo tổng hợp" → Auto-generate report
"Tiết kiệm 2 triệu/tháng" → Get savings plan
```

**Smart Budget:**
```
Budgets → "Smart Recommendations"
- Select analysis period: 1/3/6 months
- Click "Get Recommendations"
- AI analyzes patterns & suggests optimal budgets
- Click "Apply" to auto-create budgets
```

**Savings Roadmap:**
```
Goals → "💰 Savings Path"
- Target amount: 10,000,000 VND
- Purpose: House/Car/Travel
- AI generates detailed roadmap:
  + Required timeline
  + Monthly savings amount
  + Step-by-step actions
  + Optimization tips
```

---

## 🤖 AI Features Details

### **Transaction Classification API**
```bash
POST /api/ai/classify
{
  "text": "Mua cà phê Highlands 50000 VND"
}

Response:
{
  "category": "ăn uống",
  "confidence": 0.94
}
```

### **Smart Analytics API**
```bash
GET /api/ai/smart-analytics

Response:
{
  "healthScore": 75,
  "insights": ["Chi tiêu tăng 15%", ...],
  "recommendations": [...]
}
```

### **AI Endpoints**
| Endpoint | Description |
|----------|-------------|
| `/api/ai/classify` | Classify transaction |
| `/api/ai/advice` | Financial advice |
| `/api/ai/chat` | Chat with AI |
| `/api/ai/tips` | Personalized tips |
| `/api/ai/smart-analytics` | Smart analytics |
| `/api/ai/smart-budget` | Budget recommendations |
| `/api/ai/savings-tips` | Savings knowledge |
| `/api/ai/suggest-savings-path` | Savings roadmap |

See full documentation: [AI Service README](./ai-service/README.md)

---

## 🗄️ Database Schema

**Core Tables:**
- `Users` - User accounts
- `Transactions` - Income/expense records
- `Categories` - Spending categories
- `Wallets` - Money wallets
- `Budgets` - Monthly budgets
- `Goals` - Financial goals

**Relationships:**
```
User (1) ──→ (*) Transactions
User (1) ──→ (*) Categories
User (1) ──→ (*) Wallets
User (1) ──→ (*) Budgets
User (1) ──→ (*) Goals
```

---

## 🚨 Troubleshooting

### **AI Service won't start**
```bash
# Check Python version
python --version  # Must be >= 3.9

# Reinstall dependencies
cd ai-service
pip install -r requirements.txt --force-reinstall
```

### **Backend DB connection failed**
```sql
-- Test connection
sqlcmd -S localhost -U sa -P password

-- Check database
USE FinancialManagement;
SELECT COUNT(*) FROM Users;
```

### **Transaction PRIMARY KEY error**
```sql
-- Reset IDENTITY sequence
DBCC CHECKIDENT ('Transactions', RESEED, 0);
```

### **Email alerts not sending**
```properties
# Check Gmail App Password
spring.mail.username=your-email@gmail.com
spring.mail.password=app-password

# Enable alerts
notification.email.budget-alerts=true
```

---

## 📈 Performance

| Metric | Value |
|--------|-------|
| API Response | < 100ms |
| AI Classification | < 50ms |
| DB Queries | < 20ms |
| Frontend Load | < 2s |
| Concurrent Users | 100+ |

---

## 🤝 Contributing

1. Fork repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

---

## 📄 License

MIT License - See [LICENSE](LICENSE)

---

## 📞 Support

- 📧 Email: support@financeapp.com
- 🐛 Issues: [GitHub Issues](https://github.com/trtahhh/financial-management-app/issues)
- 📚 Docs: [Wiki](https://github.com/trtahhh/financial-management-app/wiki)

---

<div align="center">

**Made with ❤️ by Financial Management Team**

⭐ Star us on GitHub!

[🏠 Home](.) | [🤖 AI Service](./ai-service) | [📚 Docs](https://github.com/trtahhh/financial-management-app/wiki) | [🐛 Issues](https://github.com/trtahhh/financial-management-app/issues)

</div>
