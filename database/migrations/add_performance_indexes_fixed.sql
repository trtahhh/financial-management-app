-- =====================================================
-- DATABASE PERFORMANCE INDEXES (FIXED)
-- Created: 30/11/2025
-- Purpose: Optimize query performance
-- =====================================================

SET QUOTED_IDENTIFIER ON;
GO

-- =====================================================
-- TRANSACTION TABLE INDEXES
-- =====================================================

-- Index for user transactions lookup (most common query)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transaction_user_date' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transaction_user_date
    ON Transactions(user_id, trans_date DESC)
    INCLUDE (amount, type, note, category_id, wallet_id, status);
END
GO

-- Index for category-based queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transaction_user_category' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transaction_user_category
    ON Transactions(user_id, category_id)
    INCLUDE (amount, trans_date, type);
END
GO

-- Index for wallet transactions
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transaction_wallet' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transaction_wallet
    ON Transactions(wallet_id, trans_date DESC);
END
GO

-- Index for date range queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transaction_date_type' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transaction_date_type
    ON Transactions(trans_date, type, user_id);
END
GO

-- Index for soft delete filtering
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transaction_deleted' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transaction_deleted
    ON Transactions(user_id, is_deleted)
    INCLUDE (trans_date);
END
GO

-- =====================================================
-- BUDGET TABLE INDEXES
-- =====================================================

-- Index for user budget lookup by month/year
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_budget_user_period' AND object_id = OBJECT_ID('Budgets'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_budget_user_period
    ON Budgets(user_id, year, month);
END
GO

-- Index for category budget lookup
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_budget_category' AND object_id = OBJECT_ID('Budgets'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_budget_category
    ON Budgets(user_id, category_id, year, month)
    INCLUDE (amount);
END
GO

-- =====================================================
-- WALLET TABLE INDEXES
-- =====================================================

-- Index for user wallets
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_wallet_user' AND object_id = OBJECT_ID('Wallets'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_wallet_user
    ON Wallets(user_id)
    INCLUDE (name, type, balance);
END
GO

-- =====================================================
-- GOAL TABLE INDEXES
-- =====================================================

-- Index for user goals
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_goal_user' AND object_id = OBJECT_ID('Goals'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_goal_user
    ON Goals(user_id)
    INCLUDE (name, target_amount, current_amount);
END
GO

-- =====================================================
-- RECURRING TRANSACTION INDEXES
-- =====================================================

-- Index for recurring transactions
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_recurring_user' AND object_id = OBJECT_ID('Recurring_Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_recurring_user
    ON Recurring_Transactions(user_id);
END
GO

-- =====================================================
-- GAMIFICATION INDEXES
-- =====================================================

-- Index for user gamification data
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_gamification_user' AND object_id = OBJECT_ID('user_gamification'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_gamification_user
    ON user_gamification(user_id);
END
GO

-- Index for user achievements
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_achievement_user' AND object_id = OBJECT_ID('user_achievements'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_achievement_user
    ON user_achievements(user_id);
END
GO

-- Index for user challenges
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_challenge_user' AND object_id = OBJECT_ID('user_challenges'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_challenge_user
    ON user_challenges(user_id);
END
GO

-- =====================================================
-- USER TABLE INDEXES
-- =====================================================

-- Index for email lookup (login)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_email' AND object_id = OBJECT_ID('Users'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX idx_user_email
    ON Users(email)
    WHERE email IS NOT NULL;
END
GO

-- Index for username lookup
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_username' AND object_id = OBJECT_ID('Users'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX idx_user_username
    ON Users(username)
    WHERE username IS NOT NULL;
END
GO

-- =====================================================
-- CATEGORY TABLE INDEXES
-- =====================================================

-- Index for category name lookup
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_category_name' AND object_id = OBJECT_ID('Categories'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_category_name
    ON Categories(name);
END
GO

-- =====================================================
-- UPDATE STATISTICS
-- =====================================================

UPDATE STATISTICS Transactions WITH FULLSCAN;
UPDATE STATISTICS Budgets WITH FULLSCAN;
UPDATE STATISTICS Wallets WITH FULLSCAN;
UPDATE STATISTICS Goals WITH FULLSCAN;
UPDATE STATISTICS Users WITH FULLSCAN;
UPDATE STATISTICS Categories WITH FULLSCAN;
GO

PRINT 'Performance indexes created successfully!';
GO
