# Hướng dẫn Test Profile Creation

## Vấn đề đã khắc phục:
1. **Xử lý kiểu dữ liệu birthday**: Sửa lỗi parse String thành LocalDate
2. **Validation dữ liệu**: Chỉ lưu các trường có giá trị
3. **Logging**: Thêm log để debug
4. **Test endpoint**: Tạo endpoint test riêng

## Cách test:

### 1. Sử dụng file test-profile.http:
```bash
# Test với đầy đủ thông tin
POST http://localhost:8080/api/auth/test-profile
{
  "fullName": "Nguyễn Văn A",
  "phone": "0123456789",
  "birthday": "1990-01-01",
  "gender": "Nam",
  "address": "Hà Nội, Việt Nam"
}
```

### 2. Test đăng ký tài khoản mới:
```bash
POST http://localhost:8080/api/auth/register
{
  "username": "testuser",
  "password": "password123",
  "email": "test@example.com",
  "fullName": "Nguyễn Văn Test",
  "phone": "0123456789",
  "birthday": "1990-01-01",
  "gender": "Nam",
  "address": "Hà Nội"
}
```

## Kiểm tra kết quả:

1. **Logs**: Xem console logs để kiểm tra quá trình tạo profile
2. **Database**: Kiểm tra bảng User_Profile
3. **Response**: Kiểm tra response từ API

## Các trường được lưu:
- ✅ fullName: Tên đầy đủ
- ✅ phone: Số điện thoại  
- ✅ birthday: Ngày sinh (format: yyyy-MM-dd)
- ✅ gender: Giới tính
- ✅ address: Địa chỉ
- ✅ imageUrl: URL ảnh đại diện

## Troubleshooting:

### Nếu birthday không lưu được:
- Kiểm tra format date: phải là "yyyy-MM-dd"
- Kiểm tra logs để xem lỗi parse

### Nếu các trường khác không lưu:
- Kiểm tra dữ liệu đầu vào có null/empty không
- Kiểm tra logs để xem quá trình lưu
- Kiểm tra database schema
