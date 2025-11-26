-- Migration: Add Gamification Tables
-- Date: 2025-11-26
-- Description: Add user_gamification, achievements, user_achievements, challenges, user_challenges tables

-- Create user_gamification table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[user_gamification]') AND type in (N'U'))
BEGIN
    CREATE TABLE user_gamification (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        level INT DEFAULT 1,
        total_points INT DEFAULT 0,
        current_streak INT DEFAULT 0,
        longest_streak INT DEFAULT 0,
        transaction_count INT DEFAULT 0,
        budget_count INT DEFAULT 0,
        goal_count INT DEFAULT 0,
        total_savings DECIMAL(18,2) DEFAULT 0,
        last_activity_date DATETIME,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE(),
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
    
    CREATE INDEX idx_user_gamification_user_id ON user_gamification(user_id);
    PRINT 'Created table: user_gamification';
END
ELSE
    PRINT 'Table user_gamification already exists';

-- Create achievements table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[achievements]') AND type in (N'U'))
BEGIN
    CREATE TABLE achievements (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(100) NOT NULL,
        description NVARCHAR(500),
        icon NVARCHAR(50),
        points INT DEFAULT 0,
        requirement_type NVARCHAR(50),
        requirement_value INT,
        is_active BIT DEFAULT 1,
        created_at DATETIME DEFAULT GETDATE()
    );
    
    PRINT 'Created table: achievements';
    
    -- Insert default achievements
    INSERT INTO achievements (name, description, icon, points, requirement_type, requirement_value) VALUES
    ('First Transaction', 'Giao dịch đầu tiên', '🎯', 10, 'TRANSACTION_COUNT', 1),
    ('10 Transactions', '10 giao dịch', '📊', 50, 'TRANSACTION_COUNT', 10),
    ('100 Transactions', '100 giao dịch', '💯', 200, 'TRANSACTION_COUNT', 100),
    ('First Budget', 'Ngân sách đầu tiên', '💰', 20, 'BUDGET_COUNT', 1),
    ('Budget Master', '5 ngân sách', '👑', 100, 'BUDGET_COUNT', 5),
    ('First Goal', 'Mục tiêu đầu tiên', '🎯', 20, 'GOAL_COUNT', 1),
    ('Goal Achiever', 'Hoàn thành mục tiêu', '🏆', 150, 'GOAL_COMPLETED', 1),
    ('Week Streak', '7 ngày liên tiếp', '🔥', 50, 'STREAK', 7),
    ('Month Streak', '30 ngày liên tiếp', '⚡', 200, 'STREAK', 30),
    ('Saver', 'Tiết kiệm 1 triệu', '💎', 100, 'TOTAL_SAVINGS', 1000000);
    
    PRINT 'Inserted default achievements';
END
ELSE
    PRINT 'Table achievements already exists';

-- Create user_achievements table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[user_achievements]') AND type in (N'U'))
BEGIN
    CREATE TABLE user_achievements (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        achievement_id BIGINT NOT NULL,
        unlocked_at DATETIME DEFAULT GETDATE(),
        notified BIT DEFAULT 0,
        progress DECIMAL(5,2) DEFAULT 0,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE,
        UNIQUE(user_id, achievement_id)
    );
    
    CREATE INDEX idx_user_achievements_user_id ON user_achievements(user_id);
    CREATE INDEX idx_user_achievements_notified ON user_achievements(notified);
    PRINT 'Created table: user_achievements';
END
ELSE
    PRINT 'Table user_achievements already exists';

-- Create challenges table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[challenges]') AND type in (N'U'))
BEGIN
    CREATE TABLE challenges (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(100) NOT NULL,
        description NVARCHAR(500),
        challenge_type NVARCHAR(50),
        target_value INT,
        reward_points INT DEFAULT 0,
        start_date DATETIME,
        end_date DATETIME,
        is_active BIT DEFAULT 1,
        created_at DATETIME DEFAULT GETDATE()
    );
    
    PRINT 'Created table: challenges';
    
    -- Insert sample challenges
    INSERT INTO challenges (name, description, challenge_type, target_value, reward_points, start_date, end_date) VALUES
    ('Giao dịch hàng ngày', 'Ghi chép giao dịch mỗi ngày trong tuần', 'DAILY_TRANSACTION', 7, 100, GETDATE(), DATEADD(day, 7, GETDATE())),
    ('Tiết kiệm master', 'Tiết kiệm 5 triệu trong tháng', 'MONTHLY_SAVINGS', 5000000, 300, GETDATE(), DATEADD(month, 1, GETDATE())),
    ('Ngân sách chặt chẽ', 'Không vượt ngân sách trong tháng', 'BUDGET_ADHERENCE', 1, 200, GETDATE(), DATEADD(month, 1, GETDATE()));
    
    PRINT 'Inserted sample challenges';
END
ELSE
    PRINT 'Table challenges already exists';

-- Create user_challenges table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[user_challenges]') AND type in (N'U'))
BEGIN
    CREATE TABLE user_challenges (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        challenge_id BIGINT NOT NULL,
        joined_at DATETIME DEFAULT GETDATE(),
        completed_at DATETIME,
        progress DECIMAL(5,2) DEFAULT 0,
        is_completed BIT DEFAULT 0,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE,
        UNIQUE(user_id, challenge_id)
    );
    
    CREATE INDEX idx_user_challenges_user_id ON user_challenges(user_id);
    CREATE INDEX idx_user_challenges_completed ON user_challenges(is_completed);
    PRINT 'Created table: user_challenges';
END
ELSE
    PRINT 'Table user_challenges already exists';

PRINT 'Gamification tables migration completed successfully!';
