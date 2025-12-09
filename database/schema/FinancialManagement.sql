-- ========================================
-- FINANCIAL MANAGEMENT DATABASE SCHEMA
-- Unified and Complete Schema
-- ========================================

CREATE DATABASE FinancialManagement;
GO
USE FinancialManagement;
GO

-- ========================================
-- USERS & AUTHENTICATION
-- ========================================

CREATE TABLE Users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(100) UNIQUE NOT NULL,
    email NVARCHAR(255) UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    role NVARCHAR(50) DEFAULT 'USER',
    is_active BIT DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE User_Profile (
    user_id BIGINT PRIMARY KEY,
    full_name NVARCHAR(100),
    birthday DATE,
    gender NVARCHAR(10),
    phone NVARCHAR(20),
    address NVARCHAR(255),
    image_url NVARCHAR(500),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

-- ========================================
-- WALLETS
-- ========================================

CREATE TABLE Wallets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name NVARCHAR(100) NOT NULL,
    type NVARCHAR(50),
    balance DECIMAL(18,2) DEFAULT 0,
    is_active BIT DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

-- ========================================
-- CATEGORIES
-- ========================================

CREATE TABLE Categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    type VARCHAR(10) CHECK (type IN ('income','expense')) NOT NULL,
    color NVARCHAR(7) DEFAULT '#007bff',
    icon NVARCHAR(50),
    is_active BIT DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);

-- ========================================
-- TRANSACTIONS
-- ========================================

CREATE TABLE Transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(18,2) CHECK (amount > 0) NOT NULL,
    type VARCHAR(10) CHECK (type IN ('income','expense')) NOT NULL,
    note NVARCHAR(255),
    trans_date DATE NOT NULL,
    file_path NVARCHAR(500),
    tags NVARCHAR(255),
    status NVARCHAR(50) DEFAULT 'completed',
    is_deleted BIT DEFAULT 0,
    deleted_at DATETIME NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (wallet_id) REFERENCES Wallets(id) ON DELETE NO ACTION,
    FOREIGN KEY (category_id) REFERENCES Categories(id) ON DELETE NO ACTION
);

-- ========================================
-- BUDGETS
-- ========================================

CREATE TABLE Budgets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    month INT CHECK (month BETWEEN 1 AND 12),
    year INT,
    amount DECIMAL(18,2) NOT NULL,
    spent_amount DECIMAL(18,2) DEFAULT 0,
    is_deleted BIT DEFAULT 0,
    deleted_at DATETIME NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES Categories(id) ON DELETE NO ACTION
);

CREATE TABLE Shared_Budgets (
    user_id BIGINT NOT NULL,
    budget_id BIGINT NOT NULL,
    permission NVARCHAR(20) DEFAULT 'read',
    created_at DATETIME DEFAULT GETDATE(),
    PRIMARY KEY (user_id, budget_id),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (budget_id) REFERENCES Budgets(id) ON DELETE NO ACTION
);

-- ========================================
-- GOALS
-- ========================================

CREATE TABLE Goals (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(500),
    target_amount DECIMAL(18,2) CHECK (target_amount > 0) NOT NULL,
    current_amount DECIMAL(18,2) DEFAULT 0,
    due_date DATE,
    status NVARCHAR(20) DEFAULT 'active',
    completed_at DATETIME NULL,
    is_deleted BIT DEFAULT 0,
    deleted_at DATETIME NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

CREATE TABLE Goal_Categories (
    goal_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    PRIMARY KEY (goal_id, category_id),
    FOREIGN KEY (goal_id) REFERENCES Goals(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES Categories(id) ON DELETE NO ACTION
);

-- ========================================
-- RECURRING TRANSACTIONS
-- ========================================

CREATE TABLE Recurring_Transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(18,2) CHECK (amount > 0) NOT NULL,
    type VARCHAR(10) CHECK (type IN ('income','expense')) NOT NULL,
    note NVARCHAR(255),
    frequency VARCHAR(20) CHECK (frequency IN ('daily','weekly','monthly','yearly')),
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BIT DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (wallet_id) REFERENCES Wallets(id) ON DELETE NO ACTION,
    FOREIGN KEY (category_id) REFERENCES Categories(id) ON DELETE NO ACTION
);

-- ========================================
-- NOTIFICATIONS
-- ========================================

CREATE TABLE Notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NULL,
    budget_id BIGINT NULL,
    goal_id BIGINT NULL,
    transaction_id BIGINT NULL,
    category_id BIGINT NULL,
    message NVARCHAR(255) NOT NULL,
    type NVARCHAR(50),
    is_read BIT DEFAULT 0,
    month INT NULL,
    year INT NULL,
    is_deleted BIT DEFAULT 0,
    deleted_at DATETIME NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (wallet_id) REFERENCES Wallets(id) ON DELETE NO ACTION,
    FOREIGN KEY (budget_id) REFERENCES Budgets(id) ON DELETE NO ACTION,
    FOREIGN KEY (goal_id) REFERENCES Goals(id) ON DELETE NO ACTION,
    FOREIGN KEY (transaction_id) REFERENCES Transactions(id) ON DELETE NO ACTION,
    FOREIGN KEY (category_id) REFERENCES Categories(id) ON DELETE NO ACTION
);

-- ========================================
-- AI CHAT HISTORY
-- ========================================

CREATE TABLE AI_History (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    prompt NVARCHAR(MAX) NOT NULL,
    response NVARCHAR(MAX),
    model NVARCHAR(50),
    tokens_used INT,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

-- ========================================
-- INDEXES FOR PERFORMANCE
-- ========================================

-- Transaction indexes
CREATE INDEX IX_Transactions_user_date ON Transactions(user_id, trans_date);
CREATE INDEX IX_Transactions_wallet ON Transactions(wallet_id);
CREATE INDEX IX_Transactions_category ON Transactions(category_id);
CREATE INDEX IX_Transactions_type ON Transactions(type);
CREATE INDEX IX_Transactions_is_deleted ON Transactions(is_deleted);

-- Budget indexes
CREATE INDEX IX_Budgets_user_month_year ON Budgets(user_id, month, year);
CREATE INDEX IX_Budgets_category ON Budgets(category_id);
CREATE INDEX IX_Budgets_is_deleted ON Budgets(is_deleted);

-- Goal indexes
CREATE INDEX IX_Goals_user ON Goals(user_id);
CREATE INDEX IX_Goals_status ON Goals(status);
CREATE INDEX IX_Goals_is_deleted ON Goals(is_deleted);

-- Notification indexes
CREATE INDEX IX_Notifications_user ON Notifications(user_id);
CREATE INDEX IX_Notifications_is_read ON Notifications(is_read);
CREATE INDEX IX_Notifications_type ON Notifications(type);
CREATE INDEX IX_Notifications_is_deleted ON Notifications(is_deleted);

-- Wallet indexes
CREATE INDEX IX_Wallets_user ON Wallets(user_id);
CREATE INDEX IX_Wallets_is_active ON Wallets(is_active);

-- Category indexes
CREATE INDEX IX_Categories_type ON Categories(type);
CREATE INDEX IX_Categories_is_active ON Categories(is_active);

-- ========================================
-- DEFAULT DATA
-- ========================================

-- Insert default categories
INSERT INTO Categories (name, type, color, icon) VALUES
-- Income categories
(N'Lương', 'income', '#28a745', 'money-bill'),
(N'Thu nhập khác', 'income', '#20c997', 'gift'),
(N'Đầu tư', 'income', '#17a2b8', 'chart-line'),
(N'Kinh doanh', 'income', '#ffc107', 'store'),

-- Expense categories
(N'Ăn uống', 'expense', '#dc3545', 'utensils'),
(N'Giao thông', 'expense', '#fd7e14', 'car'),
(N'Giải trí', 'expense', '#e83e8c', 'gamepad'),
(N'Sức khỏe', 'expense', '#6f42c1', 'heartbeat'),
(N'Giáo dục', 'expense', '#20c997', 'graduation-cap'),
(N'Mua sắm', 'expense', '#fd7e14', 'shopping-cart'),
(N'Tiện ích', 'expense', '#6c757d', 'bolt'),
(N'Du lịch', 'expense', '#17a2b8', 'plane'),
(N'Thể thao', 'expense', '#28a745', 'dumbbell'),
(N'Khác', 'expense', '#6c757d', 'ellipsis-h');

-- Insert admin user
INSERT INTO Users (username, email, password_hash, role) VALUES 
('admin', 'admin@example.com', '$2a$10$jkBBeL2fMUDbood3P9isiOL4J0KjHb3PhXZf0cHLBAS4F1m4GVTo6', 'ADMIN');

INSERT INTO User_Profile (user_id, full_name, phone, address) VALUES 
(1, N'Administrator', '0123456789', N'Hà Nội, Việt Nam');

-- Insert regular user with full profile
INSERT INTO Users (username, email, password_hash, role) VALUES 
('user', 'user@example.com', '$2a$10$jkBBeL2fMUDbood3P9isiOL4J0KjHb3PhXZf0cHLBAS4F1m4GVTo6', 'USER');

INSERT INTO User_Profile (user_id, full_name, birthday, phone, address) VALUES 
(2, N'Nguyễn Văn An', '1995-03-15', '0987654321', N'123 Đường ABC, Quận 1, TP.HCM');

-- Insert wallets for regular user
INSERT INTO Wallets (user_id, name, type, balance) VALUES
(2, N'Tiền mặt', 'cash', 2500000),
(2, N'Vietcombank', 'bank', 15000000),
(2, N'Ví Momo', 'ewallet', 3000000),
(2, N'Ví ZaloPay', 'ewallet', 1500000);

-- Insert comprehensive transactions for regular user (last 3 months)
INSERT INTO Transactions (user_id, wallet_id, category_id, amount, type, note, trans_date) VALUES
-- Income transactions
(2, 2, 1, 25000000, 'income', N'Lương tháng 6', '2024-06-01'),
(2, 2, 1, 25000000, 'income', N'Lương tháng 7', '2024-07-01'),
(2, 2, 1, 25000000, 'income', N'Lương tháng 8', '2024-08-01'),
(2, 3, 2, 500000, 'income', N'Thưởng dự án', '2024-08-15'),
(2, 4, 3, 2000000, 'income', N'Lãi đầu tư chứng khoán', '2024-08-20'),

-- Food & Dining expenses
(2, 1, 5, 45000, 'expense', N'Bữa sáng', '2024-06-02'),
(2, 1, 5, 80000, 'expense', N'Bữa trưa công ty', '2024-06-03'),
(2, 1, 5, 120000, 'expense', N'Ăn tối với bạn', '2024-06-05'),
(2, 1, 5, 50000, 'expense', N'Cà phê sáng', '2024-06-07'),
(2, 1, 5, 150000, 'expense', N'Ăn buffet', '2024-06-10'),
(2, 1, 5, 60000, 'expense', N'Bữa trưa', '2024-06-12'),
(2, 1, 5, 90000, 'expense', N'Ăn tối gia đình', '2024-06-15'),
(2, 1, 5, 75000, 'expense', N'Bữa sáng', '2024-06-18'),
(2, 1, 5, 110000, 'expense', N'Ăn trưa', '2024-06-20'),
(2, 1, 5, 180000, 'expense', N'Ăn tối nhà hàng', '2024-06-25'),

-- Transportation expenses
(2, 1, 6, 15000, 'expense', N'Gửi xe', '2024-06-02'),
(2, 1, 6, 50000, 'expense', N'Xăng xe', '2024-06-05'),
(2, 1, 6, 20000, 'expense', N'Grab đi làm', '2024-06-08'),
(2, 1, 6, 30000, 'expense', N'Taxi về nhà', '2024-06-12'),
(2, 1, 6, 15000, 'expense', N'Gửi xe', '2024-06-15'),
(2, 1, 6, 80000, 'expense', N'Bảo dưỡng xe', '2024-06-20'),
(2, 1, 6, 25000, 'expense', N'Grab đi chơi', '2024-06-25'),

-- Entertainment expenses
(2, 3, 7, 200000, 'expense', N'Xem phim', '2024-06-08'),
(2, 3, 7, 150000, 'expense', N'Karaoke', '2024-06-15'),
(2, 3, 7, 300000, 'expense', N'Chơi game online', '2024-06-22'),
(2, 3, 7, 180000, 'expense', N'Xem phim IMAX', '2024-07-05'),
(2, 3, 7, 250000, 'expense', N'Karaoke với bạn', '2024-07-12'),
(2, 3, 7, 120000, 'expense', N'Chơi game', '2024-07-20'),

-- Health expenses
(2, 2, 8, 500000, 'expense', N'Khám sức khỏe', '2024-06-10'),
(2, 2, 8, 200000, 'expense', N'Mua thuốc', '2024-06-15'),
(2, 2, 8, 300000, 'expense', N'Khám răng', '2024-07-05'),
(2, 2, 8, 150000, 'expense', N'Thuốc bổ', '2024-07-20'),

-- Education expenses
(2, 2, 9, 2000000, 'expense', N'Khóa học tiếng Anh', '2024-06-01'),
(2, 2, 9, 1500000, 'expense', N'Khóa học lập trình', '2024-07-01'),
(2, 2, 9, 500000, 'expense', N'Mua sách', '2024-07-15'),

-- Shopping expenses
(2, 4, 10, 800000, 'expense', N'Mua quần áo', '2024-06-05'),
(2, 4, 10, 1200000, 'expense', N'Mua giày', '2024-06-15'),
(2, 4, 10, 500000, 'expense', N'Mua túi xách', '2024-07-10'),
(2, 4, 10, 300000, 'expense', N'Mua đồ điện tử', '2024-07-25'),

-- Utility expenses
(2, 2, 11, 500000, 'expense', N'Tiền điện', '2024-06-30'),
(2, 2, 11, 300000, 'expense', N'Tiền nước', '2024-06-30'),
(2, 2, 11, 200000, 'expense', N'Internet', '2024-06-30'),
(2, 2, 11, 550000, 'expense', N'Tiền điện', '2024-07-31'),
(2, 2, 11, 320000, 'expense', N'Tiền nước', '2024-07-31'),
(2, 2, 11, 200000, 'expense', N'Internet', '2024-07-31'),

-- Travel expenses
(2, 2, 12, 5000000, 'expense', N'Du lịch Đà Nẵng', '2024-06-20'),
(2, 2, 12, 3000000, 'expense', N'Du lịch Phú Quốc', '2024-07-25'),

-- Sports expenses
(2, 1, 13, 200000, 'expense', N'Phòng gym', '2024-06-01'),
(2, 1, 13, 150000, 'expense', N'Mua dụng cụ thể thao', '2024-06-10'),
(2, 1, 13, 200000, 'expense', N'Phòng gym', '2024-07-01'),
(2, 1, 13, 300000, 'expense', N'Chơi tennis', '2024-07-15'),

-- Other expenses
(2, 1, 14, 100000, 'expense', N'Chi phí khác', '2024-06-28'),
(2, 1, 14, 150000, 'expense', N'Chi phí khác', '2024-07-28');

-- Insert budgets for regular user
INSERT INTO Budgets (user_id, category_id, month, year, amount) VALUES
(2, 5, 8, 2024, 3000000),  -- Food budget
(2, 6, 8, 2024, 500000),    -- Transportation budget
(2, 7, 8, 2024, 800000),    -- Entertainment budget
(2, 8, 8, 2024, 1000000),   -- Health budget
(2, 9, 8, 2024, 2000000),   -- Education budget
(2, 10, 8, 2024, 1500000),  -- Shopping budget
(2, 11, 8, 2024, 800000),   -- Utility budget
(2, 12, 8, 2024, 3000000),  -- Travel budget
(2, 13, 8, 2024, 500000);   -- Sports budget

-- Insert goals for regular user
INSERT INTO Goals (user_id, name, description, target_amount, current_amount, due_date) VALUES
(2, N'Mua xe hơi', N'Tiết kiệm để mua xe hơi mới', 500000000, 50000000, '2025-12-31'),
(2, N'Du lịch châu Âu', N'Tiết kiệm cho chuyến du lịch châu Âu', 100000000, 20000000, '2025-06-30'),
(2, N'Đầu tư chứng khoán', N'Tiết kiệm để đầu tư chứng khoán', 50000000, 10000000, '2024-12-31');

-- Insert notifications for regular user
INSERT INTO Notifications (user_id, wallet_id, message, type, is_read) VALUES
(2, 1, N'Số dư ví Tiền mặt dưới 500k', 'LOW_BALANCE', 0),
(2, 3, N'Số dư ví Momo dưới 1 triệu', 'LOW_BALANCE', 0),
(2, NULL, N'Bạn đã vượt quá 80% ngân sách Ăn uống tháng này', 'BUDGET_ALERT', 0),
(2, NULL, N'Bạn đã vượt quá 90% ngân sách Giải trí tháng này', 'BUDGET_ALERT', 0);

-- Insert AI chat history for regular user
INSERT INTO AI_History (user_id, prompt, response, model) VALUES
(2, N'Làm thế nào để tiết kiệm tiền hiệu quả?', N'Để tiết kiệm tiền hiệu quả, bạn nên: 1) Lập kế hoạch chi tiêu hàng tháng, 2) Theo dõi chi tiêu thường xuyên, 3) Đặt mục tiêu tiết kiệm cụ thể, 4) Tự động chuyển tiền tiết kiệm, 5) Cắt giảm chi phí không cần thiết.', 'deepseek-r1-distill-llama-70b'),
(2, N'Có nên đầu tư vào chứng khoán không?', N'Đầu tư chứng khoán có thể mang lại lợi nhuận cao nhưng cũng có rủi ro. Bạn nên: 1) Tìm hiểu kỹ về thị trường, 2) Đầu tư dài hạn, 3) Đa dạng hóa danh mục, 4) Chỉ đầu tư số tiền có thể mất.', 'deepseek-r1-distill-llama-70b');

GO
