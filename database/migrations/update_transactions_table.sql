-- Cập nhật bảng Transactions để thêm các cột còn thiếu
USE FinancialManagement;
GO

-- Kiểm tra và thêm cột is_deleted nếu chưa có
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'transactions' AND COLUMN_NAME = 'is_deleted')
BEGIN
    ALTER TABLE Transactions ADD is_deleted BIT DEFAULT 0;
END

-- Kiểm tra và thêm cột deleted_at nếu chưa có
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'transactions' AND COLUMN_NAME = 'deleted_at')
BEGIN
    ALTER TABLE Transactions ADD deleted_at DATETIME NULL;
END

-- Kiểm tra và thêm cột status nếu chưa có
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'transactions' AND COLUMN_NAME = 'status')
BEGIN
    ALTER TABLE Transactions ADD status NVARCHAR(50) NULL;
END

-- Kiểm tra và thêm cột tags nếu chưa có
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'transactions' AND COLUMN_NAME = 'tags')
BEGIN
    ALTER TABLE Transactions ADD tags NVARCHAR(255) NULL;
END

-- Kiểm tra và thêm cột file_path nếu chưa có
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'transactions' AND COLUMN_NAME = 'file_path')
BEGIN
    ALTER TABLE Transactions ADD file_path NVARCHAR(500) NULL;
END

-- Kiểm tra và thêm cột updated_at nếu chưa có
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'transactions' AND COLUMN_NAME = 'updated_at')
BEGIN
    ALTER TABLE Transactions ADD updated_at DATETIME DEFAULT GETDATE();
END

-- Cập nhật dữ liệu cho các bản ghi cũ
UPDATE Transactions SET is_deleted = 0 WHERE is_deleted IS NULL;
UPDATE Transactions SET updated_at = created_at WHERE updated_at IS NULL;

GO
