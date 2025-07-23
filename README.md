# Financial Management App

Ứng dụng quản lý tài chính cá nhân với Spring Boot backend và Node.js frontend.

## Cấu trúc Database

### Bảng Users
- `id`: ID chính
- `username`: Tên đăng nhập (unique)
- `email`: Email (unique)
- `password_hash`: Mật khẩu đã hash
- `role`: Vai trò (user/admin)
- `full_name`: Họ tên đầy đủ
- `image_url`: URL ảnh đại diện
- `created_at`: Ngày tạo
- `is_deleted`: Đánh dấu xóa
- `deleted_at`: Ngày xóa

### Bảng User_Profile
- `user_id`: ID user (FK)
- `birthday`: Ngày sinh
- `gender`: Giới tính (male/female/other)
- `address`: Địa chỉ

## API Endpoints

### Profile API
- `GET /api/profile`: Lấy thông tin profile
- `PUT /api/profile`: Cập nhật profile
- `DELETE /api/profile`: Xóa tài khoản (soft delete)

## Tính năng Profile

1. **Hiển thị thông tin đầy đủ**:
   - Email, Username, Họ tên
   - Ảnh đại diện URL
   - Ngày sinh, Giới tính, Địa chỉ
   - Vai trò (readonly)

2. **Chỉnh sửa thông tin**:
   - Nút "Chỉnh sửa" để bật chế độ edit
   - Nút "Cập nhật" để lưu thay đổi
   - Nút "Hủy" để khôi phục giá trị ban đầu

3. **Validation**:
   - Kiểm tra định dạng ngày sinh (yyyy-MM-dd)
   - Xử lý lỗi và hiển thị thông báo

## Cách chạy

### Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```

### Frontend (Node.js)
```bash
cd frontend
npm install
npm start
```

### Database
Chạy script SQL trong `database/schema/FinancialManagement.sql`

## Tài khoản mẫu
- Admin: admin@example.com / admin123
- User: user1@example.com / user123
