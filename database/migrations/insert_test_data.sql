-- Insert test data for financial management app
USE FinancialManagement;
GO

-- 1. Create test users if not exists
IF NOT EXISTS (SELECT 1 FROM Users WHERE id = 1)
BEGIN
    SET IDENTITY_INSERT Users ON;
    INSERT INTO Users (id, username, email, password_hash, role, created_at) 
    VALUES (1, 'admin', 'admin@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', GETDATE());
    SET IDENTITY_INSERT Users OFF;
    
    -- Create user profile for admin
    INSERT INTO User_Profile (user_id, full_name, phone, address, birthday)
    VALUES (1, 'Admin User', '0123456789', '123 Admin Street', '1990-01-01');
END

IF NOT EXISTS (SELECT 1 FROM Users WHERE id = 4)
BEGIN
    SET IDENTITY_INSERT Users ON;
    INSERT INTO Users (id, username, email, password_hash, role, created_at) 
    VALUES (4, 'testuser', 'test@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', GETDATE());
    SET IDENTITY_INSERT Users OFF;
    
    -- Create user profile for testuser
    INSERT INTO User_Profile (user_id, full_name, phone, address, birthday)
    VALUES (4, 'Test User', '0987654321', '456 Test Street', '1995-01-01');
END

-- 2. Create default wallets for users
IF NOT EXISTS (SELECT 1 FROM Wallets WHERE user_id = 1)
BEGIN
    INSERT INTO Wallets (user_id, name, type, balance, created_at)
    VALUES (1, 'Ví mặc định', 'default', 1000000.00, GETDATE());
END

IF NOT EXISTS (SELECT 1 FROM Wallets WHERE user_id = 4)
BEGIN
    INSERT INTO Wallets (user_id, name, type, balance, created_at)
    VALUES (4, 'Ví mặc định', 'default', 500000.00, GETDATE());
END

-- 3. Create test categories if not exist (system categories)
IF NOT EXISTS (SELECT 1 FROM Categories WHERE name = 'Ăn uống')
BEGIN
    INSERT INTO Categories (name, type) VALUES ('Ăn uống', 'expense');
END

IF NOT EXISTS (SELECT 1 FROM Categories WHERE name = 'Xăng xe')
BEGIN
    INSERT INTO Categories (name, type) VALUES ('Xăng xe', 'expense');
END

IF NOT EXISTS (SELECT 1 FROM Categories WHERE name = 'Học phí')
BEGIN
    INSERT INTO Categories (name, type) VALUES ('Học phí', 'expense');
END

IF NOT EXISTS (SELECT 1 FROM Categories WHERE name = 'Lương')
BEGIN
    INSERT INTO Categories (name, type) VALUES ('Lương', 'income');
END

IF NOT EXISTS (SELECT 1 FROM Categories WHERE name = 'Thưởng')
BEGIN
    INSERT INTO Categories (name, type) VALUES ('Thưởng', 'income');
END

-- 4. Create test transactions for July 2025 (tháng 7)
DECLARE @walletId1 INT = (SELECT TOP 1 id FROM Wallets WHERE user_id = 1);
DECLARE @walletId4 INT = (SELECT TOP 1 id FROM Wallets WHERE user_id = 4);
DECLARE @categoryAnUong INT = (SELECT TOP 1 id FROM Categories WHERE name = 'Ăn uống');
DECLARE @categoryXangXe INT = (SELECT TOP 1 id FROM Categories WHERE name = 'Xăng xe');
DECLARE @categoryLuong INT = (SELECT TOP 1 id FROM Categories WHERE name = 'Lương');
DECLARE @categoryThuong INT = (SELECT TOP 1 id FROM Categories WHERE name = 'Thưởng');

-- Clear existing test transactions
DELETE FROM Transactions WHERE user_id IN (1, 4);

-- Insert July 2025 transactions for admin (user_id = 1)
INSERT INTO Transactions (user_id, wallet_id, category_id, amount, type, note, trans_date, created_at, status, is_deleted) VALUES
-- Thu nhập tháng 7/2025 (tổng: 10,000,000 VND)
(1, @walletId1, @categoryLuong, 8000000.00, 'income', 'Lương tháng 7', '2025-07-01', GETDATE(), 'cleared', 0),
(1, @walletId1, @categoryThuong, 2000000.00, 'income', 'Thưởng dự án', '2025-07-15', GETDATE(), 'cleared', 0),

-- Chi tiêu tháng 7/2025 (tổng: 3,500,000 VND)
(1, @walletId1, @categoryAnUong, 1500000.00, 'expense', 'Chi tiêu ăn uống tháng 7', '2025-07-03', GETDATE(), 'cleared', 0),
(1, @walletId1, @categoryXangXe, 800000.00, 'expense', 'Xăng xe tháng 7', '2025-07-10', GETDATE(), 'cleared', 0),
(1, @walletId1, @categoryAnUong, 1200000.00, 'expense', 'Ăn uống cuối tháng', '2025-07-25', GETDATE(), 'cleared', 0);

-- Insert July 2025 transactions for testuser (user_id = 4)
INSERT INTO Transactions (user_id, wallet_id, category_id, amount, type, note, trans_date, created_at, status, is_deleted) VALUES
-- Thu nhập tháng 7/2025 (tổng: 5,000,000 VND)
(4, @walletId4, @categoryLuong, 5000000.00, 'income', 'Lương tháng 7', '2025-07-01', GETDATE(), 'cleared', 0),

-- Chi tiêu tháng 7/2025 (tổng: 2,000,000 VND)
(4, @walletId4, @categoryAnUong, 1000000.00, 'expense', 'Chi tiêu ăn uống tháng 7', '2025-07-05', GETDATE(), 'cleared', 0),
(4, @walletId4, @categoryXangXe, 1000000.00, 'expense', 'Xăng xe tháng 7', '2025-07-15', GETDATE(), 'cleared', 0);

-- 5. Create test budgets for categories
DELETE FROM Budgets WHERE user_id IN (1, 4);

INSERT INTO Budgets (user_id, category_id, amount, month, year, created_at, is_deleted) VALUES
(1, @categoryAnUong, 2000000.00, 7, 2025, GETDATE(), 0),  -- Budget 2M cho ăn uống tháng 7
(1, @categoryXangXe, 1000000.00, 7, 2025, GETDATE(), 0),  -- Budget 1M cho xăng xe tháng 7
(4, @categoryAnUong, 1500000.00, 7, 2025, GETDATE(), 0),  -- Budget 1.5M cho ăn uống tháng 7
(4, @categoryXangXe, 800000.00, 7, 2025, GETDATE(), 0);  -- Budget 800K cho xăng xe tháng 7

-- 6. Create test goals
DELETE FROM Goals WHERE user_id IN (1, 4);

INSERT INTO Goals (user_id, name, target_amount, current_amount, due_date, status, created_at, is_deleted) VALUES
(1, 'Mua xe máy', 50000000.00, 15000000.00, '2025-12-31', 'ACTIVE', GETDATE(), 0),
(1, 'Du lịch Đà Lạt', 10000000.00, 3000000.00, '2025-10-31', 'ACTIVE', GETDATE(), 0),
(4, 'Mua laptop', 20000000.00, 5000000.00, '2025-12-31', 'ACTIVE', GETDATE(), 0);

-- Verify the data
SELECT 'TRANSACTIONS' as DataType, COUNT(*) as Count FROM Transactions WHERE user_id IN (1, 4) AND YEAR(trans_date) = 2025
UNION ALL
SELECT 'BUDGETS', COUNT(*) FROM Budgets WHERE user_id IN (1, 4) AND year = 2025
UNION ALL
SELECT 'GOALS', COUNT(*) FROM Goals WHERE user_id IN (1, 4)
UNION ALL
SELECT 'CATEGORIES', COUNT(*) FROM Categories
UNION ALL
SELECT 'WALLETS', COUNT(*) FROM Wallets WHERE user_id IN (1, 4);

-- Show July 2025 summary for both users
SELECT 
    'Admin - Tháng 7/2025 - Thu nhập' as Description,
    SUM(amount) as Amount
FROM Transactions 
WHERE user_id = 1 AND type = 'income' AND MONTH(trans_date) = 7 AND YEAR(trans_date) = 2025 AND is_deleted = 0

UNION ALL

SELECT 
    'Admin - Tháng 7/2025 - Chi tiêu' as Description,
    SUM(amount) as Amount
FROM Transactions 
WHERE user_id = 1 AND type = 'expense' AND MONTH(trans_date) = 7 AND YEAR(trans_date) = 2025 AND is_deleted = 0

UNION ALL

SELECT 
    'TestUser - Tháng 7/2025 - Thu nhập' as Description,
    SUM(amount) as Amount
FROM Transactions 
WHERE user_id = 4 AND type = 'income' AND MONTH(trans_date) = 7 AND YEAR(trans_date) = 2025 AND is_deleted = 0

UNION ALL

SELECT 
    'TestUser - Tháng 7/2025 - Chi tiêu' as Description,
    SUM(amount) as Amount
FROM Transactions 
WHERE user_id = 4 AND type = 'expense' AND MONTH(trans_date) = 7 AND YEAR(trans_date) = 2025 AND is_deleted = 0;

PRINT 'Test data inserted successfully!';
PRINT 'Admin (user_id=1): Tháng 7/2025 có thu nhập 10,000,000 VND và chi tiêu 3,500,000 VND';
PRINT 'TestUser (user_id=4): Tháng 7/2025 có thu nhập 5,000,000 VND và chi tiêu 2,000,000 VND';
