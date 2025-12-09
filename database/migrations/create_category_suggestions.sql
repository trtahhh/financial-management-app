-- ========================================
-- CATEGORY SUGGESTIONS SYSTEM
-- AI-generated category suggestions awaiting user approval
-- ========================================

USE FinancialManagement;
GO

-- Table: Category_Suggestions
CREATE TABLE Category_Suggestions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    -- Suggested category info
    suggested_name NVARCHAR(100) NOT NULL,
    suggested_type VARCHAR(10) CHECK (suggested_type IN ('income','expense')) NOT NULL,
    suggested_color NVARCHAR(7) DEFAULT '#6c757d',
    suggested_icon NVARCHAR(50) DEFAULT 'fa-tag',
    
    -- AI reasoning
    confidence_score DECIMAL(5,2) CHECK (confidence_score >= 0 AND confidence_score <= 1),
    reasoning NVARCHAR(500), -- Why AI suggests this category
    sample_descriptions NVARCHAR(1000), -- Example transactions that triggered this
    transaction_count INT DEFAULT 1, -- How many transactions match this pattern
    
    -- Approval status
    status NVARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected', 'merged')),
    approved_at DATETIME2,
    rejected_reason NVARCHAR(255),
    
    -- If approved, the created category ID
    created_category_id BIGINT,
    
    -- If merged with existing category
    merged_with_category_id BIGINT,
    
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (created_category_id) REFERENCES Categories(id) ON DELETE SET NULL,
    FOREIGN KEY (merged_with_category_id) REFERENCES Categories(id) ON DELETE SET NULL
);

-- Index for fast retrieval
CREATE INDEX IX_CategorySuggestions_UserId_Status ON Category_Suggestions(user_id, status);
CREATE INDEX IX_CategorySuggestions_Status ON Category_Suggestions(status);
CREATE INDEX IX_CategorySuggestions_CreatedAt ON Category_Suggestions(created_at DESC);

-- Add user_id to Categories for custom categories
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'Categories') AND name = 'user_id')
BEGIN
    ALTER TABLE Categories ADD user_id BIGINT NULL;
    ALTER TABLE Categories ADD CONSTRAINT FK_Categories_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE;
    
    -- NULL user_id = system default categories
    -- NOT NULL user_id = user-created custom categories
END
GO

-- Add is_custom flag
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'Categories') AND name = 'is_custom')
BEGIN
    ALTER TABLE Categories ADD is_custom BIT DEFAULT 0;
END
GO

PRINT 'Category Suggestions system created successfully';
GO
