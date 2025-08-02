-- Insert test user data
-- Kiểm tra xem có user nào không
SELECT * FROM Users;

-- Nếu không có user, tạo user test
IF NOT EXISTS (SELECT 1 FROM Users WHERE id = 1)
BEGIN
    INSERT INTO Users (id, username, email, password, full_name, role, created_at, updated_at) 
    VALUES (1, 'testuser', 'test@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Test User', 'USER', GETDATE(), GETDATE());
    
    -- Tạo user profile tương ứng
    INSERT INTO User_Profile (user_id, phone, gender, address, birthday)
    VALUES (1, '0123456789', 'Nam', '123 Test Street', '1990-01-01');
END

-- Kiểm tra lại
SELECT u.*, up.* 
FROM Users u 
LEFT JOIN User_Profile up ON u.id = up.user_id 
WHERE u.id = 1;
