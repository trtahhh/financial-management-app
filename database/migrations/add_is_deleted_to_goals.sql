-- Thêm cột is_deleted vào bảng Goals
ALTER TABLE Goals 
ADD is_deleted BIT DEFAULT 0 NOT NULL;
