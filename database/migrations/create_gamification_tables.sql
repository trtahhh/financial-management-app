-- Create Gamification Tables for Financial Management System
-- Created: 2025-11-05

USE FinancialManagement;
GO

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

PRINT 'Creating Gamification tables...';
GO

-- ====================================
-- ACHIEVEMENTS TABLE
-- ====================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'Achievements') AND type in (N'U'))
BEGIN
    CREATE TABLE Achievements (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(100) NOT NULL,
        description NVARCHAR(500),
        icon NVARCHAR(100),
        badge_type NVARCHAR(50),
        points INT DEFAULT 0,
        requirement_type NVARCHAR(50),
        requirement_value INT,
        is_active BIT DEFAULT 1,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE()
    );
    PRINT '✓ Created Achievements table';
END
GO

-- ====================================
-- USER_GAMIFICATION TABLE
-- ====================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'UserGamification') AND type in (N'U'))
BEGIN
    CREATE TABLE UserGamification (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        total_points INT DEFAULT 0,
        current_level INT DEFAULT 1,
        current_streak INT DEFAULT 0,
        longest_streak INT DEFAULT 0,
        last_activity_date DATE,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_UserGamification_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
    );
    PRINT '✓ Created UserGamification table';
END
GO

-- ====================================
-- USER_ACHIEVEMENTS TABLE
-- ====================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'UserAchievements') AND type in (N'U'))
BEGIN
    CREATE TABLE UserAchievements (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        achievement_id BIGINT NOT NULL,
        unlocked_at DATETIME DEFAULT GETDATE(),
        progress INT DEFAULT 0,
        is_notified BIT DEFAULT 0,
        created_at DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_UserAchievements_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
        CONSTRAINT FK_UserAchievements_Achievement FOREIGN KEY (achievement_id) REFERENCES Achievements(id) ON DELETE CASCADE,
        CONSTRAINT UQ_UserAchievements UNIQUE (user_id, achievement_id)
    );
    PRINT '✓ Created UserAchievements table';
END
GO

-- ====================================
-- CHALLENGES TABLE
-- ====================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'Challenges') AND type in (N'U'))
BEGIN
    CREATE TABLE Challenges (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(100) NOT NULL,
        description NVARCHAR(500),
        challenge_type NVARCHAR(50),
        target_value DECIMAL(15,2),
        reward_points INT DEFAULT 0,
        start_date DATE,
        end_date DATE,
        is_active BIT DEFAULT 1,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE()
    );
    PRINT '✓ Created Challenges table';
END
GO

-- ====================================
-- USER_CHALLENGES TABLE
-- ====================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'UserChallenges') AND type in (N'U'))
BEGIN
    CREATE TABLE UserChallenges (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        challenge_id BIGINT NOT NULL,
        current_progress DECIMAL(15,2) DEFAULT 0,
        is_completed BIT DEFAULT 0,
        completed_at DATETIME,
        joined_at DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_UserChallenges_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
        CONSTRAINT FK_UserChallenges_Challenge FOREIGN KEY (challenge_id) REFERENCES Challenges(id) ON DELETE CASCADE,
        CONSTRAINT UQ_UserChallenges UNIQUE (user_id, challenge_id)
    );
    PRINT '✓ Created UserChallenges table';
END
GO

-- ====================================
-- INDEXES FOR GAMIFICATION TABLES
-- ====================================
PRINT 'Creating indexes for Gamification tables...';
GO

-- UserGamification indexes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_gamification_user' AND object_id = OBJECT_ID('UserGamification'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_user_gamification_user ON UserGamification(user_id);
    PRINT '✓ Created idx_user_gamification_user';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_gamification_points' AND object_id = OBJECT_ID('UserGamification'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_user_gamification_points ON UserGamification(total_points DESC);
    PRINT '✓ Created idx_user_gamification_points';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_gamification_streak' AND object_id = OBJECT_ID('UserGamification'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_user_gamification_streak ON UserGamification(current_streak DESC, last_activity_date DESC);
    PRINT '✓ Created idx_user_gamification_streak';
END
GO

-- UserAchievements indexes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_achievements_user' AND object_id = OBJECT_ID('UserAchievements'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_user_achievements_user ON UserAchievements(user_id, unlocked_at DESC);
    PRINT '✓ Created idx_user_achievements_user';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_achievements_notified' AND object_id = OBJECT_ID('UserAchievements'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_user_achievements_notified ON UserAchievements(user_id, is_notified);
    PRINT '✓ Created idx_user_achievements_notified';
END
GO

-- Challenges indexes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_challenges_active' AND object_id = OBJECT_ID('Challenges'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_challenges_active ON Challenges(is_active, start_date, end_date);
    PRINT '✓ Created idx_challenges_active';
END
GO

-- UserChallenges indexes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_challenges_user' AND object_id = OBJECT_ID('UserChallenges'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_user_challenges_user ON UserChallenges(user_id, is_completed, joined_at DESC);
    PRINT '✓ Created idx_user_challenges_user';
END
GO

-- ====================================
-- SEED DEFAULT ACHIEVEMENTS
-- ====================================
PRINT 'Seeding default achievements...';
GO

IF NOT EXISTS (SELECT * FROM Achievements WHERE name = 'First Transaction')
BEGIN
    INSERT INTO Achievements (name, description, icon, badge_type, points, requirement_type, requirement_value, is_active)
    VALUES 
    ('First Transaction', 'Complete your first transaction', '🎯', 'BRONZE', 10, 'TRANSACTION_COUNT', 1, 1),
    ('Transaction Master', 'Complete 100 transactions', '⭐', 'GOLD', 100, 'TRANSACTION_COUNT', 100, 1),
    ('Budget Creator', 'Create your first budget', '💰', 'BRONZE', 15, 'BUDGET_COUNT', 1, 1),
    ('Budget Expert', 'Manage 10 budgets successfully', '👑', 'GOLD', 150, 'BUDGET_COUNT', 10, 1),
    ('Goal Setter', 'Create your first savings goal', '🎯', 'BRONZE', 20, 'GOAL_COUNT', 1, 1),
    ('Goal Achiever', 'Complete 5 savings goals', '🏆', 'GOLD', 200, 'GOAL_COMPLETED', 5, 1),
    ('Streak Starter', 'Maintain a 7-day streak', '🔥', 'SILVER', 50, 'STREAK_DAYS', 7, 1),
    ('Streak Legend', 'Maintain a 30-day streak', '⚡', 'PLATINUM', 300, 'STREAK_DAYS', 30, 1);
    
    PRINT '✓ Seeded 8 default achievements';
END
GO

PRINT '';
PRINT '========================================';
PRINT 'Gamification tables created successfully!';
PRINT '========================================';
GO

-- Verify tables
SELECT 
    name AS TableName,
    create_date AS CreatedDate
FROM sys.tables
WHERE name IN ('Achievements', 'UserGamification', 'UserAchievements', 'Challenges', 'UserChallenges')
ORDER BY name;
GO
