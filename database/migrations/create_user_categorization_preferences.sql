-- Migration: Create user_categorization_preferences table for Layer 2 personalization
-- Purpose: Store user's categorization patterns to improve prediction accuracy
-- Layer 2 boosts confidence to 95% after 3+ uses of same pattern

CREATE TABLE user_categorization_preferences (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    description_pattern NVARCHAR(500) NOT NULL,
    category_id BIGINT NOT NULL,
    frequency INT NOT NULL DEFAULT 1,
    last_used DATETIME2 NOT NULL DEFAULT GETDATE(),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NULL,
    
    -- Foreign keys
    CONSTRAINT FK_user_cat_pref_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_user_cat_pref_category FOREIGN KEY (category_id) 
        REFERENCES categories(id) ON DELETE CASCADE,
    
    -- Unique constraint: one pattern per user
    CONSTRAINT UQ_user_description_pattern UNIQUE (user_id, description_pattern)
);

-- Index for fast lookups during categorization
CREATE INDEX IDX_user_cat_pref_lookup 
    ON user_categorization_preferences(user_id, description_pattern);

-- Index for analytics/cleanup queries
CREATE INDEX IDX_user_cat_pref_frequency 
    ON user_categorization_preferences(user_id, frequency DESC, last_used DESC);

GO

-- Verify table creation
SELECT 
    'Table created successfully' AS Status,
    COUNT(*) AS InitialRowCount
FROM user_categorization_preferences;

GO
