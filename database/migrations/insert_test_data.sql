-- Insert test data for financial management app
USE FinancialManagement;
GO

-- 1. Create test user if not exists
IF NOT EXISTS (SELECT 1 FROM Users WHERE id = 1)
BEGIN
    SET IDENTITY_INSERT Users ON;
    INSERT INTO Users (id, username, email, password_hash, role, created_at) 
    VALUES (1, 'testuser', 'test@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'USER', GETDATE());
    SET IDENTITY_INSERT Users OFF;
    
    -- Create user profile
    INSERT INTO User_Profile (user_id, full_name, phone, address, birthday)
    VALUES (1, 'Test User', '0123456789', '123 Test Street', '1990-01-01');
END

-- 2. Create default wallet for user
IF NOT EXISTS (SELECT 1 FROM Wallets WHERE user_id = 1)
BEGIN
    INSERT INTO Wallets (user_id, name, type, balance, created_at)
    VALUES (1, 'Ví mặc định', 'default', 1000000.00, GETDATE());
END

-- 3. Create test categories if not exist
IF NOT EXISTS (SELECT 1 FROM Categories WHERE user_id = 1)
BEGIN
    INSERT INTO Categories (user_id, name, type, color, created_at) VALUES
    (1, 'Ăn uống', 'CHI', '#FF6B6B', GETDATE()),
    (1, 'Xăng xe', 'CHI', '#4ECDC4', GETDATE()),
    (1, 'Học phí', 'CHI', '#45B7D1', GETDATE()),
    (1, 'Lương', 'THU', '#96CEB4', GETDATE()),
    (1, 'Thưởng', 'THU', '#FECA57', GETDATE());
END

-- 4. Create test transactions for July 2025 (tháng 7)
DECLARE @walletId INT = (SELECT TOP 1 id FROM Wallets WHERE user_id = 1);
DECLARE @categoryAnUong INT = (SELECT TOP 1 id FROM Categories WHERE name = 'Ăn uống' AND user_id = 1);
DECLARE @categoryXangXe INT = (SELECT TOP 1 id FROM Categories WHERE name = 'Xăng xe' AND user_id = 1);
DECLARE @categoryLuong INT = (SELECT TOP 1 id FROM Categories WHERE name = 'Lương' AND user_id = 1);
DECLARE @categoryThuong INT = (SELECT TOP 1 id FROM Categories WHERE name = 'Thưởng' AND user_id = 1);

-- Clear existing test transactions
DELETE FROM Transactions WHERE user_id = 1;

-- Insert July 2025 transactions
INSERT INTO Transactions (user_id, wallet_id, category_id, amount, type, note, trans_date, created_at, status, is_deleted) VALUES
-- Thu nhập tháng 7/2025 (tổng: 10,000,000 VND)
(1, @walletId, @categoryLuong, 8000000.00, 'THU', 'Lương tháng 7', '2025-07-01', GETDATE(), 'cleared', 0),
(1, @walletId, @categoryThuong, 2000000.00, 'THU', 'Thưởng dự án', '2025-07-15', GETDATE(), 'cleared', 0),

-- Chi tiêu tháng 7/2025 (tổng: 3,500,000 VND)
(1, @walletId, @categoryAnUong, 1500000.00, 'CHI', 'Chi tiêu ăn uống tháng 7', '2025-07-03', GETDATE(), 'cleared', 0),
(1, @walletId, @categoryXangXe, 800000.00, 'CHI', 'Xăng xe tháng 7', '2025-07-10', GETDATE(), 'cleared', 0),
(1, @walletId, @categoryAnUong, 1200000.00, 'CHI', 'Ăn uống cuối tháng', '2025-07-25', GETDATE(), 'cleared', 0),

-- Thêm một vài giao dịch tháng 8 để test
(1, @walletId, @categoryAnUong, 500000.00, 'CHI', 'Ăn uống đầu tháng 8', '2025-08-01', GETDATE(), 'cleared', 0),
(1, @walletId, @categoryLuong, 8000000.00, 'THU', 'Lương tháng 8', '2025-08-01', GETDATE(), 'cleared', 0);

-- 5. Create test budgets for categories
DELETE FROM Budgets WHERE user_id = 1;

INSERT INTO Budgets (user_id, category_id, amount, month, year, created_at, is_deleted) VALUES
(1, @categoryAnUong, 2000000.00, 7, 2025, GETDATE(), 0),  -- Budget 2M cho ăn uống tháng 7
(1, @categoryXangXe, 1000000.00, 7, 2025, GETDATE(), 0),  -- Budget 1M cho xăng xe tháng 7
(1, @categoryAnUong, 2000000.00, 8, 2025, GETDATE(), 0),  -- Budget 2M cho ăn uống tháng 8
(1, @categoryXangXe, 1000000.00, 8, 2025, GETDATE(), 0);  -- Budget 1M cho xăng xe tháng 8

-- 6. Create test goals
DELETE FROM Goals WHERE user_id = 1;

INSERT INTO Goals (user_id, name, target_amount, current_amount, deadline, status, created_at, is_deleted) VALUES
(1, 'Mua xe máy', 50000000.00, 15000000.00, '2025-12-31', 'ACTIVE', GETDATE(), 0),
(1, 'Du lịch Đà Lạt', 10000000.00, 3000000.00, '2025-10-31', 'ACTIVE', GETDATE(), 0);

-- Verify the data
SELECT 'TRANSACTIONS' as DataType, COUNT(*) as Count FROM Transactions WHERE user_id = 1 AND YEAR(trans_date) = 2025
UNION ALL
SELECT 'BUDGETS', COUNT(*) FROM Budgets WHERE user_id = 1 AND year = 2025
UNION ALL
SELECT 'GOALS', COUNT(*) FROM Goals WHERE user_id = 1
UNION ALL
SELECT 'CATEGORIES', COUNT(*) FROM Categories WHERE user_id = 1
UNION ALL
SELECT 'WALLETS', COUNT(*) FROM Wallets WHERE user_id = 1;

-- Show July 2025 summary
SELECT 
    'Tháng 7/2025 - Thu nhập' as Description,
    SUM(amount) as Amount
FROM Transactions 
WHERE user_id = 1 AND type = 'THU' AND MONTH(trans_date) = 7 AND YEAR(trans_date) = 2025 AND is_deleted = 0

UNION ALL

SELECT 
    'Tháng 7/2025 - Chi tiêu' as Description,
    SUM(amount) as Amount
FROM Transactions 
WHERE user_id = 1 AND type = 'CHI' AND MONTH(trans_date) = 7 AND YEAR(trans_date) = 2025 AND is_deleted = 0;

PRINT 'Test data inserted successfully! Tháng 7/2025 có thu nhập 10,000,000 VND và chi tiêu 3,500,000 VND';
