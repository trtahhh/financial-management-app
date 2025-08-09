# 🚀 Financial Management App

A comprehensive personal finance management application built with Spring Boot backend and Node.js frontend.

## 🎯 Features

### ✅ Core Features
- **User Authentication**: JWT-based login/register system
- **Transaction Management**: CRUD operations for income/expense tracking
- **Wallet Management**: Multiple wallet support with balance tracking
- **Category Management**: Customizable expense/income categories
- **Budget Planning**: Monthly budget setting and tracking
- **Financial Goals**: Goal setting and progress tracking
- **AI Assistant**: ChatGPT integration for financial advice
- **Notifications**: Budget alerts and low balance warnings
- **Recurring Transactions**: Automated transaction scheduling
- **File Upload**: Receipt and document attachment support

### 📊 Analytics & Reporting
- **Dashboard**: Real-time financial overview
- **Charts**: Interactive expense analysis and trends
- **Statistics**: Monthly/yearly financial summaries
- **Multi-language Support**: Vietnamese and English

## 🛠️ Technology Stack

### Backend
- **Spring Boot 3.5.4**: Main framework
- **Spring Security**: Authentication & authorization
- **Spring Data JPA**: Database operations
- **SQL Server**: Database
- **JWT**: Token-based authentication
- **MapStruct**: Object mapping
- **Caffeine**: Caching
- **Spring AI**: OpenAI integration

### Frontend
- **Node.js**: Server runtime
- **Express.js**: Web framework
- **EJS**: Template engine
- **Chart.js**: Data visualization
- **Axios**: HTTP client

## 📋 Prerequisites

- **Java 17+**
- **Node.js 18+**
- **SQL Server 2019+** (or SQL Server Express)
- **Maven 3.6+**

## 🚀 Quick Start Guide

### **Step 1: Clone Repository**
```bash
git clone <repository-url>
cd financial-management-app
```

### **Step 2: Database Setup**

#### **Option A: Automatic Setup (Recommended)**
```bash
# Run the database setup script
scripts\setup-database.bat
```

#### **Option B: Manual Setup**
1. **Open SQL Server Management Studio**
2. **Run file:** `database/schema/FinancialManagement.sql`
3. **Verify database creation**

### **Step 3: Environment Configuration**

#### **Create `.env` file in `backend/` directory:**
```bash
# Database Configuration
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=FinancialManagement;encrypt=false;trustServerCertificate=true
DB_USERNAME=sa
DB_PASSWORD=your_sql_server_password

# JWT Configuration (Base64 format - already generated)
JWT_SECRET=pvrI7sWa6Jbj22rtj731qFr9BkW7Uq7KpSFyEdJm6zk=

# AI Configuration (Optional - for AI features)
OPENAI_API_KEY=your_openrouter_api_key_here
OPENAI_BASE_URL=https://openrouter.ai/api/v1/chat/completions
OPENAI_MODEL=deepseek/deepseek-r1-distill-llama-70b:free

# Server Configuration
SERVER_PORT=8080
LOGGING_LEVEL=INFO
```

### **Step 4: Start Backend**
```bash
cd backend
.\mvnw.cmd spring-boot:run
```

### **Step 5: Start Frontend**
```bash
cd frontend
npm install
npm start
```

### **Step 6: Access Application**
- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:8080
- **Health Check:** http://localhost:8080/actuator/health

## 🔐 Default Credentials

After setup, you can login with:

### **Admin User:**
- **Username:** `admin`
- **Password:** `123456`

### **Regular User (with full data):**
- **Username:** `user`
- **Password:** `123456`
- **Profile:** Nguyễn Văn An (1995-03-15)
- **Data includes:** 3 months of transactions, budgets, goals, notifications

## 🛠️ Troubleshooting

### **Common Issues & Solutions**

#### **1. Database Connection Error**
```bash
# Check SQL Server is running
sqlcmd -S localhost -E -Q "SELECT @@VERSION"

# Test connection with credentials
sqlcmd -S localhost -U sa -P your_password -Q "SELECT 1"
```

#### **2. JWT Secret Error**
- Ensure JWT_SECRET is Base64 format
- Restart backend after changing JWT_SECRET

#### **3. Port Conflicts**
- Change port in `backend/src/main/resources/application.properties`
- Default ports: Backend (8080), Frontend (3000)

#### **4. Node.js Dependencies**
```bash
cd frontend
npm cache clean --force
npm install
```

#### **5. Maven Dependencies**
```bash
cd backend
.\mvnw.cmd clean install
```

### **Database Verification**
```sql
USE FinancialManagement;
SELECT COUNT(*) as user_count FROM Users;
SELECT COUNT(*) as category_count FROM Categories;
```

## 📊 Database Schema Overview

### **Main Tables:**
- **Users** - User information and authentication
- **User_Profile** - Detailed user profiles
- **Wallets** - Financial wallets/accounts
- **Categories** - Income/expense categories
- **Transactions** - Financial transactions
- **Budgets** - Monthly budget planning
- **Goals** - Financial goals and targets
- **Notifications** - System notifications
- **AI_History** - AI chat history

### **Key Relationships:**
- Users → Wallets (1:N)
- Users → Transactions (1:N)
- Categories → Transactions (1:N)
- Users → Budgets (1:N)
- Users → Goals (1:N)

## 🎯 Available Features

### **Core Features:**
- ✅ User Authentication (JWT)
- ✅ Transaction Management
- ✅ Wallet Management
- ✅ Category Management
- ✅ Budget Planning
- ✅ Goal Setting
- ✅ Statistics & Reports
- ✅ AI Chat Assistant
- ✅ File Upload
- ✅ Notifications

### **Advanced Features:**
- ✅ Recurring Transactions
- ✅ Shared Budgets
- ✅ Multi-language Support
- ✅ Data Export/Import
- ✅ Real-time Notifications

## 📁 Project Structure

```
financial-management-app/
├── backend/                 # Spring Boot application
│   ├── src/main/java/
│   │   └── com/example/finance/
│   │       ├── config/      # Configuration classes
│   │       ├── controller/  # REST controllers
│   │       ├── dto/         # Data transfer objects
│   │       ├── entity/      # JPA entities
│   │       ├── repository/  # Data access layer
│   │       ├── security/    # Security components
│   │       └── service/     # Business logic
│   └── src/main/resources/
│       └── application.properties
├── frontend/               # Node.js application
│   ├── public/            # Static assets
│   ├── views/             # EJS templates
│   └── server.js          # Express server
├── database/              # Database scripts
│   ├── schema/            # Database schema
│   └── migrations/        # Database migrations
└── scripts/               # Setup scripts
    └── setup-database.bat # Database setup script
```

## 🔐 Security Features

- **JWT Authentication**: Secure token-based auth
- **Password Encryption**: BCrypt hashing
- **Input Validation**: Comprehensive data validation
- **SQL Injection Prevention**: Parameterized queries
- **CORS Configuration**: Proper cross-origin settings
- **Environment Variables**: No hardcoded secrets

## 📁 Project Structure

```
financial-management-app/
├── backend/                 # Spring Boot application
│   ├── src/main/java/
│   │   └── com/example/finance/
│   │       ├── config/      # Configuration classes
│   │       ├── controller/  # REST controllers
│   │       ├── dto/         # Data transfer objects
│   │       ├── entity/      # JPA entities
│   │       ├── repository/  # Data access layer
│   │       ├── security/    # Security components
│   │       └── service/     # Business logic
│   └── src/main/resources/
│       └── application.properties
├── frontend/               # Node.js application
│   ├── public/            # Static assets
│   ├── views/             # EJS templates
│   └── server.js          # Express server
└── database/              # Database scripts
    ├── schema/            # Database schema
    └── migrations/        # Database migrations
```

## 🚀 API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login

### Transactions
- `GET /api/transactions` - List transactions
- `POST /api/transactions` - Create transaction
- `PUT /api/transactions/{id}` - Update transaction
- `DELETE /api/transactions/{id}` - Delete transaction

### Analytics
- `GET /api/dashboard/data` - Dashboard statistics
- `GET /api/transactions/stats-by-category` - Category statistics

### AI Assistant
- `POST /api/chat/message` - AI chat
- `POST /api/ai/chat` - AI financial advice

## 🔧 Development

### Adding New Features
1. **Backend**: Entity → Repository → Service → Controller
2. **Frontend**: Route → Controller → View → JavaScript
3. **Database**: Update schema and run migrations

### Code Quality
- Follow Spring Boot best practices
- Use proper exception handling
- Implement comprehensive validation
- Write unit tests for critical components

## 🐛 Troubleshooting

### Common Issues
1. **Database Connection**: Check SQL Server is running
2. **JWT Issues**: Verify JWT_SECRET is set correctly
3. **CORS Errors**: Check CORS configuration
4. **AI Integration**: Verify OpenAI API key

### Logs
- Backend logs: Check console output
- Frontend logs: Check browser console
- Database logs: Check SQL Server logs

## 📈 Performance Optimization

- **Caching**: Caffeine cache for frequently accessed data
- **Database Indexes**: Optimized queries with proper indexing
- **Connection Pooling**: HikariCP for database connections
- **Lazy Loading**: JPA lazy loading for relationships

## 🔮 Future Enhancements

- [ ] Mobile app (React Native)
- [ ] Advanced analytics and predictions
- [ ] Investment tracking
- [ ] Bill reminders and auto-payments
- [ ] Expense splitting with friends
- [ ] Export to PDF/Excel
- [ ] Push notifications
- [ ] Multi-currency support

## 📄 License

This project is licensed under the MIT License.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📞 Support

For support and questions, please create an issue in the repository.
