CREATE DATABASE FinancialManagement;
GO
USE FinancialManagement;
GO

CREATE TABLE Users (
    id INT IDENTITY PRIMARY KEY,
    username NVARCHAR(100) UNIQUE NOT NULL,
    email NVARCHAR(255),
    password_hash NVARCHAR(255) NOT NULL,
    role NVARCHAR(50) DEFAULT 'USER',
    created_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE User_Profile (
    user_id INT PRIMARY KEY,
    full_name NVARCHAR(100),
    birthday DATE,
    phone NVARCHAR(20),
    address NVARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

CREATE TABLE Languages (
    id INT IDENTITY PRIMARY KEY,
    code NVARCHAR(10) UNIQUE,
    name NVARCHAR(50)
);

CREATE TABLE Translations (
    id INT IDENTITY PRIMARY KEY,
    language_id INT NOT NULL,
    translation_key NVARCHAR(100) NOT NULL,
    translation_value NVARCHAR(255) NOT NULL,
    FOREIGN KEY (language_id) REFERENCES Languages(id),
    CONSTRAINT UQ_Translations_LangKey UNIQUE (language_id, translation_key)
);

CREATE TABLE Wallets (
    id INT IDENTITY PRIMARY KEY,
    user_id INT NOT NULL,
    name NVARCHAR(100),
    type NVARCHAR(50),
    balance DECIMAL(18,2) DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

CREATE TABLE Categories (
    id INT IDENTITY PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    type VARCHAR(10) CHECK (type IN ('income','expense')) NOT NULL
);

CREATE TABLE Transactions (
    id INT IDENTITY PRIMARY KEY,
    user_id INT NOT NULL,
    wallet_id INT NOT NULL,
    category_id INT NOT NULL,
    amount DECIMAL(18,2) CHECK (amount>0) NOT NULL,
    type VARCHAR(10) CHECK (type IN ('income','expense')) NOT NULL,
    note NVARCHAR(255),
    trans_date DATE NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (wallet_id) REFERENCES Wallets(id),
    FOREIGN KEY (category_id) REFERENCES Categories(id)
);

CREATE TABLE Files (
    id INT IDENTITY PRIMARY KEY,
    transaction_id INT NOT NULL,
    file_name NVARCHAR(255),
    file_url NVARCHAR(500),
    uploaded_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (transaction_id) REFERENCES Transactions(id)
);

CREATE TABLE Budgets (
    id INT IDENTITY PRIMARY KEY,
    user_id INT NOT NULL,
    category_id INT NOT NULL,
    month INT CHECK (month BETWEEN 1 AND 12),
    year INT,
    amount DECIMAL(18,2) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (category_id) REFERENCES Categories(id)
);

CREATE TABLE Shared_Budgets (
    user_id INT NOT NULL,
    budget_id INT NOT NULL,
    PRIMARY KEY (user_id, budget_id),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (budget_id) REFERENCES Budgets(id)
);

CREATE TABLE Goals (
    id INT IDENTITY PRIMARY KEY,
    user_id INT NOT NULL,
    name NVARCHAR(255),
    target_amount DECIMAL(18,2) CHECK (target_amount>0) NOT NULL,
    current_amount DECIMAL(18,2) DEFAULT 0,
    due_date DATE,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

CREATE TABLE Goal_Categories (
    goal_id INT NOT NULL,
    category_id INT NOT NULL,
    PRIMARY KEY (goal_id, category_id),
    FOREIGN KEY (goal_id) REFERENCES Goals(id),
    FOREIGN KEY (category_id) REFERENCES Categories(id)
);

CREATE TABLE AI_History (
    id INT IDENTITY PRIMARY KEY,
    user_id INT NOT NULL,
    prompt NVARCHAR(MAX) NOT NULL,
    response NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

CREATE TABLE Notifications (
    id INT IDENTITY PRIMARY KEY,
    user_id INT NOT NULL,
    wallet_id INT NULL,
    budget_id INT NULL,
    goal_id INT NULL,
    transaction_id INT NULL,
    message NVARCHAR(255) NOT NULL,
    type NVARCHAR(50),
    is_read BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (wallet_id) REFERENCES Wallets(id),
    FOREIGN KEY (budget_id) REFERENCES Budgets(id),
    FOREIGN KEY (goal_id) REFERENCES Goals(id),
    FOREIGN KEY (transaction_id) REFERENCES Transactions(id)
);

CREATE INDEX IX_Trans_user_date   ON Transactions(user_id, trans_date);
CREATE INDEX IX_Trans_wallet      ON Transactions(wallet_id);
CREATE INDEX IX_Trans_category    ON Transactions(category_id);
CREATE INDEX IX_Budgets_month     ON Budgets(user_id, month, year);
CREATE INDEX IX_Notif_user        ON Notifications(user_id);

INSERT INTO Languages (code,name) VALUES ('vi',N'Tiếng Việt'), ('en','English');
INSERT INTO Translations (language_id,translation_key,translation_value) VALUES (1,'DASHBOARD',N'Tổng quan'), (2,'DASHBOARD','Dashboard');

INSERT INTO Users (username,email,password_hash,role) VALUES ('testuser','test@example.com','hashed_pwd','USER');
INSERT INTO User_Profile (user_id,full_name,birthday,phone,address) VALUES (1,N'Nguyễn Văn A','2002-05-10','0123456789',N'123 Lê Lợi, TP.HCM');

INSERT INTO Wallets (user_id,name,type,balance) VALUES
(1,N'Tiền mặt','cash',1500000),
(1,N'Ngân hàng','bank',5000000),
(1,N'Ví Momo','ewallet',1200000);

INSERT INTO Categories (name,type) VALUES
(N'Lương','income'),(N'Thu nhập khác','income'),
(N'Ăn uống','expense'),(N'Giải trí','expense'),
(N'Giao thông','expense'),(N'Sức khỏe','expense'),
(N'Giáo dục','expense'),(N'Mua sắm','expense');

INSERT INTO Budgets (user_id,category_id,month,year,amount) VALUES
(1,3,7,2025,2000000),
(1,4,7,2025,500000);

INSERT INTO Goals (user_id,name,target_amount,current_amount,due_date) VALUES
(1,N'Mua laptop',20000000,5000000,'2026-06-30');

INSERT INTO Goal_Categories (goal_id,category_id) VALUES (1,8);

INSERT INTO Transactions (user_id,wallet_id,category_id,amount,type,note,trans_date) VALUES
(1,2,1,10000000,'income',N'Nhận lương tháng 7','2025-07-01'),
(1,1,3,  50000,'expense',N'Bữa trưa','2025-07-03'),
(1,3,4, 100000,'expense',N'Xem phim','2025-07-05'),
(1,2,7, 500000,'expense',N'Khoá học online','2025-07-10'),
(1,1,5,  20000,'expense',N'Gửi xe','2025-07-15'),
(1,3,2,1000000,'income',N'Bán đồ cũ','2025-07-20');

INSERT INTO Notifications (user_id,wallet_id,message,type,is_read) VALUES
(1,1,N'Số dư ví Tiền mặt dưới 100k','LOW_BALANCE',0);
GO
