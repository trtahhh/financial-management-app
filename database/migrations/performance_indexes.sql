-- Performance Optimization: Comprehensive Database Indexing Strategy
-- SQL Server Compatible Version

-- ====================================
-- TRANSACTIONS TABLE INDEXES
-- ====================================

-- Composite index for user queries with date filtering
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transactions_user_date' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transactions_user_date 
    ON Transactions(user_id, trans_date DESC, is_deleted) 
    WHERE is_deleted = 0;
END
GO

-- Composite index for user and category queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transactions_user_category' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transactions_user_category 
    ON Transactions(user_id, category_id, trans_date DESC) 
    WHERE is_deleted = 0;
END
GO

-- Composite index for user and wallet queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transactions_user_wallet' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transactions_user_wallet 
    ON Transactions(user_id, wallet_id, trans_date DESC) 
    WHERE is_deleted = 0;
END
GO

-- Composite index for type and date range queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transactions_user_type_date' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transactions_user_type_date 
    ON Transactions(user_id, type, trans_date DESC) 
    WHERE is_deleted = 0;
END
GO

-- Index for amount-based queries (finding large transactions)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transactions_user_amount' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transactions_user_amount 
    ON Transactions(user_id, amount DESC, trans_date DESC) 
    WHERE is_deleted = 0;
END
GO

-- Index for recent transactions (created_at based)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_transactions_user_created' AND object_id = OBJECT_ID('Transactions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_transactions_user_created 
    ON Transactions(user_id, created_at DESC) 
    WHERE is_deleted = 0;
END
GO

-- ====================================
-- BUDGETS TABLE INDEXES
-- ====================================

-- Composite index for user, month, year queries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_budgets_user_period' AND object_id = OBJECT_ID('Budgets'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_budgets_user_period 
    ON Budgets(user_id, year DESC, month DESC, is_deleted) 
    WHERE is_deleted = 0;
END
GO

-- Composite index for user and category
CREATE INDEX IF NOT EXISTS idx_budgets_user_category 
ON Budgets(user_id, category_id, year DESC, month DESC) 
WHERE is_deleted = 0;

-- Index for active budgets
CREATE INDEX IF NOT EXISTS idx_budgets_active 
ON Budgets(user_id, is_deleted, year DESC, month DESC) 
WHERE is_deleted = 0;

-- ====================================
-- GOALS TABLE INDEXES
-- ====================================

-- Composite index for user and status
CREATE INDEX IF NOT EXISTS idx_goals_user_status 
ON Goals(user_id, status, is_deleted) 
WHERE is_deleted = 0;

-- Index for deadline-based queries
CREATE INDEX IF NOT EXISTS idx_goals_user_deadline 
ON Goals(user_id, deadline ASC, is_deleted) 
WHERE is_deleted = 0;

-- Index for goal progress tracking
CREATE INDEX IF NOT EXISTS idx_goals_user_progress 
ON Goals(user_id, current_amount, target_amount, is_deleted) 
WHERE is_deleted = 0;

-- ====================================
-- WALLETS TABLE INDEXES
-- ====================================

-- Index for active wallets
CREATE INDEX IF NOT EXISTS idx_wallets_user_active 
ON Wallets(user_id, is_active, is_deleted) 
WHERE is_deleted = 0;

-- Index for wallet type queries
CREATE INDEX IF NOT EXISTS idx_wallets_user_type 
ON Wallets(user_id, type, is_deleted) 
WHERE is_deleted = 0;

-- ====================================
-- CATEGORIES TABLE INDEXES
-- ====================================

-- Index for user categories
CREATE INDEX IF NOT EXISTS idx_categories_user_active 
ON Categories(user_id, is_active, is_deleted) 
WHERE is_deleted = 0;

-- Index for category type
CREATE INDEX IF NOT EXISTS idx_categories_user_type 
ON Categories(user_id, type, is_deleted) 
WHERE is_deleted = 0;

-- ====================================
-- NOTIFICATIONS TABLE INDEXES
-- ====================================

-- Composite index for unread notifications
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread 
ON Notifications(user_id, is_read, created_at DESC, is_deleted) 
WHERE is_deleted = 0 AND is_read = 0;

-- Index for notification type
CREATE INDEX IF NOT EXISTS idx_notifications_user_type 
ON Notifications(user_id, type, created_at DESC) 
WHERE is_deleted = 0;

-- Index for scheduled notifications
CREATE INDEX IF NOT EXISTS idx_notifications_scheduled 
ON Notifications(user_id, scheduled_for ASC, is_sent) 
WHERE is_deleted = 0 AND is_sent = 0;

-- ====================================
-- GAMIFICATION TABLE INDEXES
-- ====================================

-- Index for leaderboard queries
CREATE INDEX IF NOT EXISTS idx_user_gamification_points 
ON user_gamification(total_points DESC, level DESC);

-- Index for streak leaderboard
CREATE INDEX IF NOT EXISTS idx_user_gamification_streak 
ON user_gamification(current_streak DESC, longest_streak DESC);

-- Index for user achievements lookup
CREATE INDEX IF NOT EXISTS idx_user_achievements_user 
ON user_achievements(user_id, unlocked_at DESC);

-- Index for unnotified achievements
CREATE INDEX IF NOT EXISTS idx_user_achievements_unnotified 
ON user_achievements(user_id, is_notified) 
WHERE is_notified = 0;

-- Index for active challenges
CREATE INDEX IF NOT EXISTS idx_challenges_active 
ON challenges(is_active, start_date, end_date) 
WHERE is_active = 1;

-- Index for user challenges
CREATE INDEX IF NOT EXISTS idx_user_challenges_user 
ON user_challenges(user_id, is_completed, joined_at DESC);

-- ====================================
-- USERS TABLE INDEXES
-- ====================================

-- Index for email lookup (login)
CREATE INDEX IF NOT EXISTS idx_users_email 
ON Users(email);

-- Index for username lookup
CREATE INDEX IF NOT EXISTS idx_users_username 
ON Users(username);

-- Index for active users
CREATE INDEX IF NOT EXISTS idx_users_active 
ON Users(is_active, created_at DESC);

-- ====================================
-- COVERING INDEXES (Include commonly selected columns)
-- ====================================

-- Transaction summary queries
CREATE INDEX IF NOT EXISTS idx_transactions_summary 
ON Transactions(user_id, trans_date, type, category_id) 
INCLUDE (amount, description) 
WHERE is_deleted = 0;

-- Budget performance queries
CREATE INDEX IF NOT EXISTS idx_budgets_performance 
ON Budgets(user_id, year, month, category_id) 
INCLUDE (amount, spent) 
WHERE is_deleted = 0;

-- ====================================
-- STATISTICS UPDATE
-- ====================================

-- Update statistics for query optimizer
UPDATE STATISTICS Transactions;
UPDATE STATISTICS Budgets;
UPDATE STATISTICS Goals;
UPDATE STATISTICS Wallets;
UPDATE STATISTICS Categories;
UPDATE STATISTICS Notifications;
UPDATE STATISTICS Users;
UPDATE STATISTICS user_gamification;
UPDATE STATISTICS user_achievements;
UPDATE STATISTICS challenges;
UPDATE STATISTICS user_challenges;

-- ====================================
-- ANALYSIS RECOMMENDATIONS
-- ====================================

/*
Performance Monitoring Query:
SELECT 
    OBJECT_NAME(s.object_id) AS TableName,
    i.name AS IndexName,
    s.user_seeks,
    s.user_scans,
    s.user_lookups,
    s.user_updates
FROM sys.dm_db_index_usage_stats s
INNER JOIN sys.indexes i ON s.object_id = i.object_id AND s.index_id = i.index_id
WHERE database_id = DB_ID()
ORDER BY s.user_seeks + s.user_scans + s.user_lookups DESC;

Query to find missing indexes:
SELECT 
    migs.avg_user_impact * (migs.user_seeks + migs.user_scans) AS Impact,
    mid.statement AS TableName,
    mid.equality_columns,
    mid.inequality_columns,
    mid.included_columns
FROM sys.dm_db_missing_index_groups mig
INNER JOIN sys.dm_db_missing_index_group_stats migs ON mig.index_group_handle = migs.group_handle
INNER JOIN sys.dm_db_missing_index_details mid ON mig.index_handle = mid.index_handle
WHERE migs.avg_user_impact > 10
ORDER BY Impact DESC;
*/
