USE FinancialManagement
GO

SELECT * FROM Categories;

SELECT * FROM Goal_Categories;

SELECT * FROM Goals;

SELECT * FROM Wallets;

SELECT * FROM Transactions;

SELECT * FROM Users;

SELECT * FROM Budgets;

SELECT * FROM Notifications;

SELECT * FROM Shared_Budgets;

UPDATE Budgets
SET is_deleted = 0
WHERE is_deleted = 1;

ALTER TABLE transactions ADD file_path NVARCHAR(255) NULL;

ALTER TABLE Goal_Categories
    ADD CONSTRAINT FK_Goal FOREIGN KEY (goal_id) REFERENCES Goals(id) ON DELETE CASCADE;
ALTER TABLE Goal_Categories
    ADD CONSTRAINT FK_Category FOREIGN KEY (category_id) REFERENCES Categories(id) ON DELETE CASCADE;

ALTER TABLE Goals
ADD completed_at DATETIME NULL,
    status NVARCHAR(20) NULL;  -- Hoặc NOT NULL DEFAULT 'active' nếu muốn mặc định

