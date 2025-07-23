CREATE DATABASE FinancialManagement;
GO
USE FinancialManagement;
GO

-- Bảng người dùng
CREATE TABLE Users (
    id              INT IDENTITY PRIMARY KEY,
    username        NVARCHAR(50) NOT NULL UNIQUE,
    email           NVARCHAR(255) NOT NULL UNIQUE,
    password_hash   NVARCHAR(255) NOT NULL,
    role            NVARCHAR(20) NOT NULL DEFAULT 'user', -- user, admin
    full_name       NVARCHAR(100),
    image_url       NVARCHAR(255),   -- Link ảnh đại diện
    created_at      DATETIME DEFAULT GETDATE(),
    is_deleted      BIT DEFAULT 0,
    deleted_at      DATETIME
);

-- Bảng hồ sơ người dùng (mở rộng, tùy ý)
CREATE TABLE User_Profile (
    user_id     INT PRIMARY KEY,
    birthday    DATE,
    gender      NVARCHAR(10),
    address     NVARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Bảng ví/nguồn tiền
CREATE TABLE Wallets (
    id          INT IDENTITY PRIMARY KEY,
    user_id     INT NOT NULL,
    name        NVARCHAR(100) NOT NULL,
    balance     DECIMAL(18,2) DEFAULT 0,
    type        NVARCHAR(50),    -- cash, bank, momo, ...
    note        NVARCHAR(255),
    created_at  DATETIME DEFAULT GETDATE(),
    is_deleted  BIT DEFAULT 0,
    deleted_at  DATETIME,
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Bảng danh mục (category thu/chi)
CREATE TABLE Categories (
    id          INT IDENTITY PRIMARY KEY,
    user_id     INT NULL, -- cho phép custom category riêng user
    name        NVARCHAR(100) NOT NULL,
    parent_id   INT NULL,
    icon        NVARCHAR(100),
    type        NVARCHAR(10) NOT NULL, -- income/expense
    is_default  BIT DEFAULT 0,
    created_at  DATETIME DEFAULT GETDATE(),
    is_deleted  BIT DEFAULT 0,
    deleted_at  DATETIME,
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (parent_id) REFERENCES Categories(id)
);

-- Bảng giao dịch thu/chi
CREATE TABLE Transactions (
    id              INT IDENTITY PRIMARY KEY,
    user_id         INT NOT NULL,
    wallet_id       INT NOT NULL,
    category_id     INT NOT NULL,
    amount          DECIMAL(18,2) NOT NULL,
    trans_type      NVARCHAR(10) NOT NULL, -- income/expense
    description     NVARCHAR(255),
    transaction_date DATE NOT NULL,
    created_at      DATETIME DEFAULT GETDATE(),
    is_deleted      BIT DEFAULT 0,
    deleted_at      DATETIME,
    FOREIGN KEY (user_id) REFERENCES Users(id),
    FOREIGN KEY (wallet_id) REFERENCES Wallets(id),
    FOREIGN KEY (category_id) REFERENCES Categories(id)
);

-- Bảng file đính kèm (cho từng giao dịch hoặc người dùng)
CREATE TABLE Files (
    id          INT IDENTITY PRIMARY KEY,
    user_id     INT NOT NULL,
    transaction_id INT NULL,
    file_url    NVARCHAR(255) NOT NULL,
    file_type   NVARCHAR(50),
    uploaded_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (transaction_id) REFERENCES Transactions(id),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Bảng ngân sách cá nhân
CREATE TABLE Budgets (
    id          INT IDENTITY PRIMARY KEY,
    user_id     INT NOT NULL,
    name        NVARCHAR(100) NOT NULL,
    total       DECIMAL(18,2) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    status      NVARCHAR(20) DEFAULT 'active',
    created_at  DATETIME DEFAULT GETDATE(),
    is_deleted  BIT DEFAULT 0,
    deleted_at  DATETIME,
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Bảng chia sẻ ngân sách
CREATE TABLE Shared_Budgets (
    budget_id   INT NOT NULL,
    user_id     INT NOT NULL,
    role        NVARCHAR(20),
    PRIMARY KEY (budget_id, user_id),
    FOREIGN KEY (budget_id) REFERENCES Budgets(id),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Bảng mục tiêu tài chính
CREATE TABLE Goals (
    id          INT IDENTITY PRIMARY KEY,
    user_id     INT NOT NULL,
    name        NVARCHAR(100) NOT NULL,
    target      DECIMAL(18,2) NOT NULL,
    [current]   DECIMAL(18,2) DEFAULT 0,
    due_date    DATE,
    status      NVARCHAR(20) DEFAULT 'active',
    created_at  DATETIME DEFAULT GETDATE(),
    is_deleted  BIT DEFAULT 0,
    deleted_at  DATETIME,
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Mối liên hệ nhiều-nhiều mục tiêu - danh mục
CREATE TABLE Goal_Categories (
    goal_id         INT NOT NULL,
    category_id     INT NOT NULL,
    PRIMARY KEY (goal_id, category_id),
    FOREIGN KEY (goal_id) REFERENCES Goals(id),
    FOREIGN KEY (category_id) REFERENCES Categories(id)
);

-- Lưu lịch sử AI/chat
CREATE TABLE AI_History (
    id          INT IDENTITY PRIMARY KEY,
    user_id     INT NOT NULL,
    content     NVARCHAR(MAX) NOT NULL,
    type        NVARCHAR(50),
    created_at  DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Thông báo & nhắc nhở (notification)
CREATE TABLE Notifications (
    id          INT IDENTITY PRIMARY KEY,
    user_id     INT NOT NULL,
    content     NVARCHAR(255) NOT NULL,
    source      NVARCHAR(50),    -- budgets, transactions, goals, ...
    source_id   INT,             -- id của bảng nguồn
    type        NVARCHAR(50),
    is_read     BIT DEFAULT 0,
    created_at  DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Bảng ngôn ngữ
CREATE TABLE Languages (
    code        NVARCHAR(10) PRIMARY KEY,
    name        NVARCHAR(50) NOT NULL
);

-- Bảng dịch thuật
CREATE TABLE Translations (
    id          INT IDENTITY PRIMARY KEY,
    lang_code   NVARCHAR(10) NOT NULL,
    key_name    NVARCHAR(100) NOT NULL,
    value       NVARCHAR(255) NOT NULL,
    FOREIGN KEY (lang_code) REFERENCES Languages(code)
);

-- ======================= Dữ liệu mẫu =======================

-- Ngôn ngữ hệ thống
INSERT INTO Languages (code, name) VALUES
('vi', N'Tiếng Việt'),
('en', N'English');

-- Danh mục mặc định (global, cho user_id NULL)
INSERT INTO Categories (user_id, name, type, is_default) VALUES
(NULL, N'Lương', 'income', 1),
(NULL, N'Ăn uống', 'expense', 1),
(NULL, N'Giải trí', 'expense', 1),
(NULL, N'Đi lại', 'expense', 1);

-- Admin & User mẫu (lưu ý: password_hash phải hash khi dùng thực tế)
INSERT INTO Users (username, email, password_hash, role, full_name, image_url) VALUES
('admin', 'admin@example.com', 'admin123', 'admin', N'Quản trị viên', 'https://randomuser.me/api/portraits/men/1.jpg'),
('user1', 'user1@example.com', 'user123', 'user', N'Người dùng 1', 'https://randomuser.me/api/portraits/women/2.jpg');

-- Thêm dữ liệu User_Profile mẫu
INSERT INTO User_Profile (user_id, birthday, gender, address) VALUES
(1, '1990-01-15', 'male', N'123 Đường ABC, Quận 1, TP.HCM'),
(2, '1995-06-20', 'female', N'456 Đường XYZ, Quận 3, TP.HCM');

-- Wallet mẫu
INSERT INTO Wallets (user_id, name, balance, type) VALUES
(1, N'Tiền mặt', 1000000, 'cash'),
(2, N'Tài khoản ngân hàng', 2000000, 'bank');
