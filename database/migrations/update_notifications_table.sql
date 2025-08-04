-- Cập nhật bảng Notifications để phù hợp với entity
USE FinancialManagement;
GO

-- Thêm các cột còn thiếu
ALTER TABLE Notifications ADD 
    category_id INT NULL,
    is_deleted BIT DEFAULT 0,
    deleted_at DATETIME NULL,
    month INT NULL,
    year INT NULL;

-- Thêm foreign key cho category_id
ALTER TABLE Notifications ADD CONSTRAINT FK_Notifications_Categories 
    FOREIGN KEY (category_id) REFERENCES Categories(id);

-- Thêm index cho tìm kiếm nhanh
CREATE INDEX IX_Notif_category ON Notifications(category_id);
CREATE INDEX IX_Notif_month_year ON Notifications(month, year);

GO
