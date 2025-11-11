-- Create Security Tables for Financial Management System
-- Created: 2025-11-05

USE FinancialManagement;
GO

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

PRINT 'Creating Security tables...';
GO

-- ====================================
-- TWO_FACTOR_AUTH TABLE
-- ====================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'TwoFactorAuth') AND type in (N'U'))
BEGIN
    CREATE TABLE TwoFactorAuth (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL UNIQUE,
        secret_key NVARCHAR(255) NOT NULL,
        is_enabled BIT DEFAULT 0,
        backup_codes NVARCHAR(MAX),
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_TwoFactorAuth_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
    );
    PRINT '✓ Created TwoFactorAuth table';
END
GO

-- ====================================
-- USER_SESSIONS TABLE
-- ====================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'UserSessions') AND type in (N'U'))
BEGIN
    CREATE TABLE UserSessions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        session_token NVARCHAR(500) NOT NULL UNIQUE,
        ip_address NVARCHAR(50),
        user_agent NVARCHAR(500),
        device_info NVARCHAR(200),
        last_activity DATETIME DEFAULT GETDATE(),
        expires_at DATETIME NOT NULL,
        is_active BIT DEFAULT 1,
        created_at DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_UserSessions_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
    );
    PRINT '✓ Created UserSessions table';
END
GO

-- ====================================
-- AUDIT_LOGS TABLE
-- ====================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'AuditLogs') AND type in (N'U'))
BEGIN
    CREATE TABLE AuditLogs (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT,
        action NVARCHAR(100) NOT NULL,
        entity_type NVARCHAR(50),
        entity_id BIGINT,
        old_value NVARCHAR(MAX),
        new_value NVARCHAR(MAX),
        ip_address NVARCHAR(50),
        user_agent NVARCHAR(500),
        status NVARCHAR(20),
        error_message NVARCHAR(MAX),
        created_at DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_AuditLogs_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE SET NULL
    );
    PRINT '✓ Created AuditLogs table';
END
GO

-- ====================================
-- LOGIN_ATTEMPTS TABLE
-- ====================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'LoginAttempts') AND type in (N'U'))
BEGIN
    CREATE TABLE LoginAttempts (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        username NVARCHAR(100) NOT NULL,
        ip_address NVARCHAR(50) NOT NULL,
        attempt_time DATETIME DEFAULT GETDATE(),
        success BIT DEFAULT 0,
        failure_reason NVARCHAR(200),
        user_agent NVARCHAR(500),
        INDEX idx_login_attempts_username_time (username, attempt_time DESC),
        INDEX idx_login_attempts_ip_time (ip_address, attempt_time DESC)
    );
    PRINT '✓ Created LoginAttempts table';
END
GO

-- ====================================
-- INDEXES FOR SECURITY TABLES
-- ====================================
PRINT 'Creating indexes for Security tables...';
GO

-- TwoFactorAuth indexes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_2fa_user' AND object_id = OBJECT_ID('TwoFactorAuth'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_2fa_user ON TwoFactorAuth(user_id, is_enabled);
    PRINT '✓ Created idx_2fa_user';
END
GO

-- UserSessions indexes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_sessions_user_active' AND object_id = OBJECT_ID('UserSessions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_sessions_user_active ON UserSessions(user_id, is_active, last_activity DESC);
    PRINT '✓ Created idx_sessions_user_active';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_sessions_token' AND object_id = OBJECT_ID('UserSessions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_sessions_token ON UserSessions(session_token) WHERE is_active = 1;
    PRINT '✓ Created idx_sessions_token';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_sessions_expires' AND object_id = OBJECT_ID('UserSessions'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_sessions_expires ON UserSessions(expires_at, is_active);
    PRINT '✓ Created idx_sessions_expires';
END
GO

-- AuditLogs indexes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_audit_user_action' AND object_id = OBJECT_ID('AuditLogs'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_audit_user_action ON AuditLogs(user_id, action, created_at DESC);
    PRINT '✓ Created idx_audit_user_action';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_audit_entity' AND object_id = OBJECT_ID('AuditLogs'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_audit_entity ON AuditLogs(entity_type, entity_id, created_at DESC);
    PRINT '✓ Created idx_audit_entity';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_audit_created' AND object_id = OBJECT_ID('AuditLogs'))
BEGIN
    CREATE NONCLUSTERED INDEX idx_audit_created ON AuditLogs(created_at DESC);
    PRINT '✓ Created idx_audit_created';
END
GO

PRINT '';
PRINT '========================================';
PRINT 'Security tables created successfully!';
PRINT '========================================';
GO

-- Verify tables
SELECT 
    name AS TableName,
    create_date AS CreatedDate
FROM sys.tables
WHERE name IN ('TwoFactorAuth', 'UserSessions', 'AuditLogs', 'LoginAttempts')
ORDER BY name;
GO
