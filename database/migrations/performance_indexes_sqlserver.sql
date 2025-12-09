-- Performance Optimization: Comprehensive Database Indexing Strategy
-- SQL Server Compatible Version
-- Created: 2025-11-05

USE FinancialManagement;
GO

PRINT 'Starting index creation process...';
GO

-- ====================================
-- TRANSACTIONS TABLE INDEXES
-- ====================================
PRINT 'Creating Transactions table indexes...';
GO

-- Composite index for user queries with date filtering
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transactions_user_date' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transactions_user_date 
    ON Transactions(user_id, trans_date DESC, is_deleted) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_transactions_user_date';
END
GO

-- Composite index for user and category queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transactions_user_category' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transactions_user_category 
    ON Transactions(user_id, category_id, trans_date DESC) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_transactions_user_category';
END
GO

-- Composite index for user and wallet queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transactions_user_wallet' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transactions_user_wallet 
    ON Transactions(user_id, wallet_id, trans_date DESC) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_transactions_user_wallet';
END
GO

-- Composite index for type and date range queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transactions_user_type_date' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transactions_user_type_date 
    ON Transactions(user_id, type, trans_date DESC) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_transactions_user_type_date';
END
GO

-- Index for amount-based queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transactions_user_amount' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transactions_user_amount 
    ON Transactions(user_id, amount DESC, trans_date DESC) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_transactions_user_amount';
END
GO

-- ====================================
-- BUDGETS TABLE INDEXES
-- ====================================
PRINT 'Creating Budgets table indexes...';
GO

-- Composite index for user, month, year queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_budgets_user_period' AND object_id = OBJECT_ID('Budgets'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_budgets_user_period 
    ON Budgets(user_id, year DESC, month DESC, is_deleted) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_budgets_user_period';
END
GO

-- Composite index for user and category
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_budgets_user_category' AND object_id = OBJECT_ID('Budgets'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_budgets_user_category 
    ON Budgets(user_id, category_id, year DESC, month DESC) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_budgets_user_category';
END
GO

-- ====================================
-- GOALS TABLE INDEXES
-- ====================================
PRINT 'Creating Goals table indexes...';
GO

-- Composite index for user and status
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_goals_user_status' AND object_id = OBJECT_ID('Goals'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_goals_user_status 
    ON Goals(user_id, status, is_deleted) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_goals_user_status';
END
GO

-- Index for deadline-based queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_goals_user_deadline' AND object_id = OBJECT_ID('Goals'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_goals_user_deadline 
    ON Goals(user_id, deadline ASC, is_deleted) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_goals_user_deadline';
END
GO

-- ====================================
-- WALLETS TABLE INDEXES
-- ====================================
PRINT 'Creating Wallets table indexes...';
GO

-- Index for active wallets
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_wallets_user_active' AND object_id = OBJECT_ID('Wallets'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_wallets_user_active 
    ON Wallets(user_id, is_deleted) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_wallets_user_active';
END
GO

-- Index for wallet type queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_wallets_user_type' AND object_id = OBJECT_ID('Wallets'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_wallets_user_type 
    ON Wallets(user_id, wallet_type, is_deleted) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_wallets_user_type';
END
GO

-- ====================================
-- CATEGORIES TABLE INDEXES
-- ====================================
PRINT 'Creating Categories table indexes...';
GO

-- Index for active categories
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_categories_user_active' AND object_id = OBJECT_ID('Categories'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_categories_user_active 
    ON Categories(user_id, is_deleted) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_categories_user_active';
END
GO

-- Index for category type
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_categories_user_type' AND object_id = OBJECT_ID('Categories'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_categories_user_type 
    ON Categories(user_id, category_type, is_deleted) 
    WHERE is_deleted = 0;
    PRINT 'Created idx_categories_user_type';
END
GO

-- ====================================
-- NOTIFICATIONS TABLE INDEXES
-- ====================================
PRINT 'Creating Notifications table indexes...';
GO

-- Index for unread notifications
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_notifications_user_unread' AND object_id = OBJECT_ID('Notifications'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_notifications_user_unread 
    ON Notifications(user_id, is_read, created_at DESC);
    PRINT 'Created idx_notifications_user_unread';
END
GO

-- Index for notification type
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_notifications_user_type' AND object_id = OBJECT_ID('Notifications'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_notifications_user_type 
    ON Notifications(user_id, type, created_at DESC);
    PRINT 'Created idx_notifications_user_type';
END
GO

-- ====================================
-- GAMIFICATION TABLE INDEXES
-- ====================================
PRINT 'Creating Gamification table indexes...';
GO

-- Index for leaderboard (points ranking)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_gamification_points' AND object_id = OBJECT_ID('UserGamification'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_user_gamification_points 
    ON UserGamification(total_points DESC);
    PRINT 'Created idx_user_gamification_points';
END
GO

-- Index for streak tracking
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_gamification_streak' AND object_id = OBJECT_ID('UserGamification'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_user_gamification_streak 
    ON UserGamification(current_streak DESC, last_activity_date DESC);
    PRINT 'Created idx_user_gamification_streak';
END
GO

-- Index for user achievements
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_achievements_user' AND object_id = OBJECT_ID('UserAchievements'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_user_achievements_user 
    ON UserAchievements(user_id, unlocked_at DESC);
    PRINT 'Created idx_user_achievements_user';
END
GO

-- Index for active challenges
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_challenges_active' AND object_id = OBJECT_ID('Challenges'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_challenges_active 
    ON Challenges(is_active, start_date, end_date);
    PRINT 'Created idx_challenges_active';
END
GO

-- Index for user challenges
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_challenges_user' AND object_id = OBJECT_ID('UserChallenges'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_user_challenges_user 
    ON UserChallenges(user_id, is_completed, joined_at DESC);
    PRINT 'Created idx_user_challenges_user';
END
GO

-- ====================================
-- USERS TABLE INDEXES
-- ====================================
PRINT 'Creating Users table indexes...';
GO

-- Index for email lookups
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_users_email' AND object_id = OBJECT_ID('Users'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX idx_users_email 
    ON Users(email);
    PRINT 'Created idx_users_email';
END
GO

-- Index for username lookups
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_users_username' AND object_id = OBJECT_ID('Users'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX idx_users_username 
    ON Users(username);
    PRINT 'Created idx_users_username';
END
GO

-- ====================================
-- UPDATE STATISTICS
-- ====================================
PRINT 'Updating statistics...';
GO

UPDATE STATISTICS Transactions WITH FULLSCAN;
UPDATE STATISTICS Budgets WITH FULLSCAN;
UPDATE STATISTICS Goals WITH FULLSCAN;
UPDATE STATISTICS Wallets WITH FULLSCAN;
UPDATE STATISTICS Categories WITH FULLSCAN;
UPDATE STATISTICS Notifications WITH FULLSCAN;
UPDATE STATISTICS UserGamification WITH FULLSCAN;
UPDATE STATISTICS UserAchievements WITH FULLSCAN;
UPDATE STATISTICS Challenges WITH FULLSCAN;
UPDATE STATISTICS UserChallenges WITH FULLSCAN;
UPDATE STATISTICS Users WITH FULLSCAN;
GO

PRINT 'Index creation completed successfully!';
PRINT 'Total indexes created: Check sys.indexes for complete list';
GO

-- Verify indexes created
SELECT 
    OBJECT_NAME(object_id) AS TableName,
    name AS IndexName,
    type_desc AS IndexType,
    is_unique AS IsUnique
FROM sys.indexes
WHERE OBJECT_NAME(object_id) IN ('Transactions', 'Budgets', 'Goals', 'Wallets', 'Categories', 
                                  'Notifications', 'UserGamification', 'UserAchievements', 
                                  'Challenges', 'UserChallenges', 'Users')
    AND name LIKE 'idx_%'
ORDER BY OBJECT_NAME(object_id), name;
GO
