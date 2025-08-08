# Financial Management App

A comprehensive personal finance management application built with Spring Boot backend and Node.js frontend.

## 🚀 Features

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

- Java 17+
- Node.js 18+
- SQL Server 2019+
- Maven 3.6+

## 🔧 Installation & Setup

### 1. Database Setup
```sql
-- Run the schema file
-- database/schema/FinancialManagement.sql
```

### 2. Environment Configuration
Copy `backend/env.example` to `backend/.env` and configure:
```bash
# Database
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=FinancialManagement
DB_USERNAME=sa
DB_PASSWORD=your_secure_password

# JWT (generate a secure 32+ character key)
JWT_SECRET=your_secure_jwt_secret_here

# AI Configuration
OPENAI_API_KEY=your_openai_api_key
```

### 3. Backend Setup
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 4. Frontend Setup
```bash
cd frontend
npm install
npm run dev
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
