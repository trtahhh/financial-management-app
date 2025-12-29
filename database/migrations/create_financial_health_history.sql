-- Create Financial Health History table
USE FinancialManagement;

CREATE TABLE financial_health_history (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    score INT NOT NULL,
    rating NVARCHAR(20),
    month INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    year INT NOT NULL,
    record_date DATE NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IX_FinancialHealthHistory_User ON financial_health_history(user_id);
CREATE INDEX IX_FinancialHealthHistory_Date ON financial_health_history(user_id, year, month);
CREATE UNIQUE INDEX UQ_FinancialHealthHistory_UserMonth ON financial_health_history(user_id, year, month);

PRINT 'Financial Health History table created successfully';
