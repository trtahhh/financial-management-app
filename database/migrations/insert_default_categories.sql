-- Insert default categories 
-- Kiểm tra và tạo các danh mục mặc định

-- Xóa categories cũ nếu có (để tránh duplicate)
DELETE FROM Categories;

-- Thêm categories mặc định
INSERT INTO Categories (name, type) VALUES
('Ăn uống', 'expense'),
('Di chuyển', 'expense'),
('Mua sắm', 'expense'),
('Giải trí', 'expense'),
('Học tập', 'expense'),
('Y tế', 'expense'),
('Hóa đơn', 'expense'),
('Lương', 'income'),
('Thưởng', 'income'),
('Đầu tư', 'income'),
('Khác', 'expense');

-- Kiểm tra kết quả
SELECT * FROM Categories ORDER BY type, name;
