-- Simplify Recurring_Transactions table
-- Remove start_date and end_date columns
-- Add next_execution column if not exists
-- User only needs to set: amount, type, note, category, wallet, frequency
-- System auto-calculates next_execution

USE FinancialManagement;
GO

-- Step 1: Add next_execution column if it doesn't exist
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Recurring_Transactions') AND name = 'next_execution')
BEGIN
    ALTER TABLE Recurring_Transactions ADD next_execution DATE;
    PRINT 'Added next_execution column';
END
GO

-- Step 2: Set default value for next_execution based on start_date or frequency
UPDATE Recurring_Transactions 
SET next_execution = CASE 
    WHEN start_date IS NOT NULL THEN start_date
    WHEN frequency = 'daily' THEN DATEADD(day, 1, GETDATE())
    WHEN frequency = 'weekly' THEN DATEADD(week, 1, GETDATE())
    WHEN frequency = 'monthly' THEN DATEADD(month, 1, GETDATE())
    WHEN frequency = 'yearly' THEN DATEADD(year, 1, GETDATE())
    ELSE DATEADD(month, 1, GETDATE())
END
WHERE next_execution IS NULL;
GO

-- Step 3: Make next_execution NOT NULL
ALTER TABLE Recurring_Transactions ALTER COLUMN next_execution DATE NOT NULL;
PRINT 'Made next_execution NOT NULL';
GO

-- Step 4: Drop start_date column if exists
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Recurring_Transactions') AND name = 'start_date')
BEGIN
    ALTER TABLE Recurring_Transactions DROP COLUMN start_date;
    PRINT 'Dropped start_date column';
END
GO

-- Step 5: Drop end_date column if exists
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Recurring_Transactions') AND name = 'end_date')
BEGIN
    ALTER TABLE Recurring_Transactions DROP COLUMN end_date;
    PRINT 'Dropped end_date column';
END
GO

PRINT '';
PRINT '========================================';
PRINT 'Recurring_Transactions table simplified!';
PRINT '========================================';
PRINT 'Users can now create recurring transactions with:';
PRINT '  ✓ amount, type, note';
PRINT '  ✓ categoryId, walletId';
PRINT '  ✓ frequency (daily/weekly/monthly/yearly)';
PRINT '  ✓ isActive (optional, default true)';
PRINT '';
PRINT 'System automatically calculates:';
PRINT '  → nextExecution = today + frequency';
PRINT '========================================';
GO
