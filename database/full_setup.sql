-- Full Database Schema
USE FinancialManagement;

-- Users & Authentication
CREATE TABLE Users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(100) UNIQUE NOT NULL,
    email NVARCHAR(255) UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    role NVARCHAR(50) DEFAULT 'USER',
    is_active BIT DEFAULT 1,
    email_verified BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

CREATE TABLE User_Profile (
    user_id BIGINT PRIMARY KEY,
    full_name NVARCHAR(100),
    birthday DATE,
    phone NVARCHAR(20),
    address NVARCHAR(255),
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

-- Wallets
CREATE TABLE Wallets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name NVARCHAR(100) NOT NULL,
    type NVARCHAR(50),
    balance DECIMAL(18,2) DEFAULT 0,
    initial_balance DECIMAL(18,2) DEFAULT 0,
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

-- Categories
CREATE TABLE Categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    type VARCHAR(10) CHECK (type IN ('income','expense')) NOT NULL,
    color NVARCHAR(7) DEFAULT '#007bff',
    icon NVARCHAR(50),
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE()
);

-- Transactions
CREATE TABLE Transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    type VARCHAR(10) CHECK (type IN ('income','expense')) NOT NULL,
    note NVARCHAR(255),
    trans_date DATE NOT NULL,
    file_path NVARCHAR(500),
    status NVARCHAR(50) DEFAULT 'completed',
    is_deleted BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (wallet_id) REFERENCES Wallets(id),
    FOREIGN KEY (category_id) REFERENCES Categories(id)
);

-- Budgets
CREATE TABLE Budgets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    month INT CHECK (month BETWEEN 1 AND 12),
    year INT,
    amount DECIMAL(18,2) NOT NULL,
    spent_amount DECIMAL(18,2) DEFAULT 0,
    is_deleted BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES Categories(id)
);

-- Goals
CREATE TABLE Goals (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(500),
    target_amount DECIMAL(18,2) NOT NULL,
    current_amount DECIMAL(18,2) DEFAULT 0,
    due_date DATE,
    status NVARCHAR(20) DEFAULT 'active',
    is_deleted BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

-- Recurring Transactions
CREATE TABLE Recurring_Transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    type VARCHAR(10) CHECK (type IN ('income','expense')) NOT NULL,
    note NVARCHAR(255),
    frequency VARCHAR(20),
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (wallet_id) REFERENCES Wallets(id),
    FOREIGN KEY (category_id) REFERENCES Categories(id)
);

-- Notifications
CREATE TABLE Notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NULL,
    budget_id BIGINT NULL,
    goal_id BIGINT NULL,
    message NVARCHAR(255) NOT NULL,
    type NVARCHAR(50),
    is_read BIT DEFAULT 0,
    is_deleted BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (wallet_id) REFERENCES Wallets(id),
    FOREIGN KEY (budget_id) REFERENCES Budgets(id),
    FOREIGN KEY (goal_id) REFERENCES Goals(id)
);

-- AI Chat History
CREATE TABLE AI_History (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    prompt NVARCHAR(MAX) NOT NULL,
    response NVARCHAR(MAX),
    model NVARCHAR(50),
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

-- Insert Categories
INSERT INTO Categories (name, type, color, icon) VALUES
(N'Lương', 'income', '#28a745', 'money-bill'),
(N'Thu nhập khác', 'income', '#20c997', 'gift'),
(N'Đầu tư', 'income', '#17a2b8', 'chart-line'),
(N'Kinh doanh', 'income', '#ffc107', 'store'),
(N'Ăn uống', 'expense', '#dc3545', 'utensils'),
(N'Giao thông', 'expense', '#fd7e14', 'car'),
(N'Giải trí', 'expense', '#e83e8c', 'gamepad'),
(N'Sức khỏe', 'expense', '#6f42c1', 'heartbeat'),
(N'Giáo dục', 'expense', '#20c997', 'graduation-cap'),
(N'Mua sắm', 'expense', '#fd7e14', 'shopping-cart'),
(N'Tiện ích', 'expense', '#6c757d', 'bolt'),
(N'Vay nợ', 'expense', '#dc3545', 'credit-card'),
(N'Quà tặng', 'expense', '#e83e8c', 'gift'),
(N'Khác', 'expense', '#6c757d', 'ellipsis-h');

-- Insert Users
INSERT INTO Users (username, email, password_hash, role, email_verified) VALUES
('admin', 'admin@example.com', '$2a$10$jkBBeL2fMUDbood3P9isiOL4J0KjHb3PhXZf0cHLBAS4F1m4GVTo6', 'ADMIN', 1),
('user', 'user@example.com', '$2a$10$jkBBeL2fMUDbood3P9isiOL4J0KjHb3PhXZf0cHLBAS4F1m4GVTo6', 'USER', 1);

INSERT INTO User_Profile (user_id, full_name, phone, address) VALUES
(1, N'Administrator', '0123456789', N'Hà Nội, Việt Nam'),
(2, N'Nguyễn Văn User', '0987654321', N'TP.HCM, Việt Nam');

-- Insert Wallets
INSERT INTO Wallets (user_id, name, type, balance, initial_balance) VALUES
(2, N'Tiền mặt', 'cash', 2500000, 2500000),
(2, N'Vietcombank', 'bank', 15000000, 15000000),
(2, N'Ví Momo', 'ewallet', 3000000, 3000000);

-- Insert Budgets
INSERT INTO Budgets (user_id, category_id, month, year, amount) VALUES
(2, 5, 12, 2024, 3000000),
(2, 6, 12, 2024, 500000),
(2, 7, 12, 2024, 800000);

-- Insert Goals
INSERT INTO Goals (user_id, name, description, target_amount, current_amount, due_date) VALUES
(2, N'Mua xe hơi', N'Tiết kiệm mua xe', 500000000, 50000000, '2025-12-31'),
(2, N'Du lịch châu Âu', N'Tiết kiệm du lịch', 100000000, 20000000, '2025-06-30');

SELECT 'Database setup completed successfully!' AS Status;
SELECT 'Tables: ' + CAST(COUNT(*) AS VARCHAR(10)) AS Info FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'
UNION ALL SELECT 'Users: ' + CAST(COUNT(*) AS VARCHAR(10)) FROM Users
UNION ALL SELECT 'Categories: ' + CAST(COUNT(*) AS VARCHAR(10)) FROM Categories
UNION ALL SELECT 'Wallets: ' + CAST(COUNT(*) AS VARCHAR(10)) FROM Wallets
UNION ALL SELECT 'Budgets: ' + CAST(COUNT(*) AS VARCHAR(10)) FROM Budgets
UNION ALL SELECT 'Goals: ' + CAST(COUNT(*) AS VARCHAR(10)) FROM Goals;
