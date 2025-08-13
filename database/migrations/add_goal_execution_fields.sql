-- Migration: Thêm các field để theo dõi việc thực hiện mục tiêu
-- Date: 2025-08-13

USE FinancialManagement;
GO

-- Thêm các column mới vào bảng Goals
ALTER TABLE Goals 
ADD is_executed BIT DEFAULT 0;
GO

ALTER TABLE Goals 
ADD executed_at DATETIME2 NULL;
GO

ALTER TABLE Goals 
ADD executed_transaction_id BIGINT NULL;
GO

-- Thêm comment để giải thích ý nghĩa
EXEC sp_addextendedproperty 
    @name = N'MS_Description',
    @value = N'Đánh dấu mục tiêu đã được thực hiện (trừ tiền và tạo giao dịch)',
    @level0type = N'SCHEMA',
    @level0name = N'dbo',
    @level1type = N'TABLE',
    @level1name = N'Goals',
    @level2type = N'COLUMN',
    @level2name = N'is_executed';
GO

EXEC sp_addextendedproperty 
    @name = N'MS_Description',
    @value = N'Thời gian mục tiêu được thực hiện',
    @level0type = N'SCHEMA',
    @level0name = N'dbo',
    @level1type = N'TABLE',
    @level1name = N'Goals',
    @level2type = N'COLUMN',
    @level2name = N'executed_at';
GO

EXEC sp_addextendedproperty 
    @name = N'MS_Description',
    @value = N'ID của giao dịch được tạo khi thực hiện mục tiêu',
    @level0type = N'SCHEMA',
    @level0name = N'dbo',
    @level1type = N'TABLE',
    @level1name = N'Goals',
    @level2type = N'COLUMN',
    @level2name = N'executed_transaction_id';
GO

-- Tạo index để tối ưu query
CREATE INDEX IX_Goals_IsExecuted ON Goals(is_executed);
GO

CREATE INDEX IX_Goals_ExecutedTransactionId ON Goals(executed_transaction_id);
GO

PRINT 'Migration completed: Added goal execution fields to Goals table';
GO
