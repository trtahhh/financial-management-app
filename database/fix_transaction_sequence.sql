-- Script để fix vấn đề sequence của bảng Transactions
-- Chạy script này trong SQL Server Management Studio hoặc Azure Data Studio

USE FinancialManagement;
GO

-- 1. Kiểm tra dữ liệu hiện tại trong bảng Transactions
SELECT 'Current Transactions Data:' as Info;
SELECT TOP 10 * FROM Transactions ORDER BY id DESC;
GO

-- 2. Kiểm tra IDENTITY hiện tại
SELECT 'Current IDENTITY value:' as Info;
SELECT IDENT_CURRENT('Transactions') as CurrentIdentity;
GO

-- 3. Kiểm tra xem có ID = 52 không
SELECT 'Checking for ID = 52:' as Info;
SELECT * FROM Transactions WHERE id = 52;
GO

-- 4. Nếu có ID = 52, xóa nó đi để tránh xung đột
-- UNCOMMENT DÒNG DƯỚI NẾU BẠN CHẮC CHẮN MUỐN XÓA
-- DELETE FROM Transactions WHERE id = 52;
-- GO

-- 5. Reset IDENTITY về giá trị cao nhất + 1
SELECT 'Resetting IDENTITY...' as Info;
DECLARE @MaxID BIGINT;
SELECT @MaxID = ISNULL(MAX(id), 0) FROM Transactions;
DBCC CHECKIDENT ('Transactions', RESEED, @MaxID);
GO

-- 6. Kiểm tra IDENTITY mới
SELECT 'New IDENTITY value:' as Info;
SELECT IDENT_CURRENT('Transactions') as NewIdentity;
GO

-- 7. Kiểm tra lại dữ liệu
SELECT 'Final check - Top 10 transactions:' as Info;
SELECT TOP 10 * FROM Transactions ORDER BY id DESC;
GO

PRINT 'Script completed. Check the results above.';
GO
