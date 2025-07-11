- Setup Database:
	Mở file FinancialManagement.sql tại đường dẫn "financial-management-app\database\schema\FinancialManagement.sql" rồi chạy script này trong SMSS để khởi tạo database.

- Khởi động dự án:
	Mở Git Bash tại thư mục chính của dự án và chạy từng lệnh:
		cd backend
		./mvnw spring-boot:run
		cd ../frontend
		npm install
		npm run dev

- Thêm tính năng mới
	Khi thêm bảng hoặc thay đổi cấu trúc database, hãy cập nhật file ".sql" tại thư mục "database/schema" rồi chạy lại script.

- Ở backend, thêm hoặc cập nhật các lớp theo thứ tự:
	Entity → Repository → Service → Controller và thêm DTO hoặc Mapper nếu cần.

- Ở frontend, thêm route vào "routes", viết hàm gọi API trong "controllers", cuối cùng tạo giao diện HTML (EJS), CSS, JavaScript trong "views" và "public".

- Kiểm tra & commit:
	Sử dụng Postman và trình duyệt để test từng bước, đảm bảo code hoạt động ổn định trước khi commit và push lên GitHub.
