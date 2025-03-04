# Xây dựng hệ thống hỏi đáp và tìm kiếm luật
## Giới thiệu
Hệ thống hỏi đáp và tìm kiếm luật là một ứng dụng web giúp người dùng tra cứu văn bản pháp luật, đặt câu hỏi với chatbot và tìm kiếm thông tin luật pháp một cách nhanh chóng và chính xác. Hệ thống sử dụng công nghệ trí tuệ nhân tạo để hỗ trợ tìm kiếm và phân tích câu hỏi, giúp người dùng tiếp cận thông tin một cách dễ dàng.
## Công nghệ sử dụng
### 1. Ngôn ngữ lập trình
- Backend: Java (Spring Boot)
- Frontend: HTML, CSS, JavaScript
### 2. Framework và thư viện
- Backend: Spring Boot, Spring Data JPA, Spring Security
- Frontend: Thymeleaf
- Tìm kiếm: Elasticsearch
### 3. Cơ sở dữ liệu
- MySQL: Quản lý dữ liệu văn bản pháp luật và người dùng
- Elasticsearch: Lưu trữ và tìm kiếm nhanh văn bản pháp luật
- 
### 4. Triển khai
- Máy chủ ứng dụng: Tomcat (Spring Boot tích hợp)
- Công cụ quản lý mã nguồn: GitLab
- CI/CD: GitLab CI/CD

## Cài đặt và chạy hệ thống
### Yêu cầu hệ thống
- JDK 17+
- MySQL 8.0+
- Elasticsearch 8.x
- Thymleaf
- Spring boot
- Maven 3.8+

### Hướng dẫn cài đặt
### 1. Clone repository:
```
git clone https://gitlab.duthu.net/S52100059/hoi-dap-tim-kiem-luat.git
cd hoi-dap-tim-kiem-luat
```
### 2. Cấu hình database:
- Tạo database law_qa trong MySQL.
- Cập nhật file application.properties với thông tin kết nối MySQL và Elasticsearch (password)

### 3. Tải Ollama
- [https://ollama.com/download]
- Pull mô hình ollama
```
ollama pull nomic-embed-text
```

### 4. Tải elastic
- [https://github.com/elastic/elasticsearch]
- Sau khi chạy giải nén file
- Chạy bin/elasticsearch.bat
- Nếu có set password nhập mật khẩu vào application.properties

### 5. Chạy dự án
-  http://localhost:8080/ vào trang chủ
-  Có thể đăng nhập bằng Google, Facebook, hoặc đăng ký sau đó đăng nhập để vào trang user
-  Vào trang admin thì nhập tên đăng nhập: admin@gmail.com và mật khẩu là admin
-  Chức năng nạp tiền có thể dùng những tài khoản test của VNPAY để nạp [https://sandbox.vnpayment.vn/apis/vnpay-demo/]
## Chức năng chính

- Hỏi đáp với chatbot: Người dùng có thể đặt câu hỏi về luật và nhận câu trả lời từ AI.

- Tìm kiếm văn bản pháp luật: Hỗ trợ tìm kiếm theo từ khóa và tìm kiếm ngữ nghĩa với Elasticsearch và OllamaEmbedding và OpenAI.

- Quản lý tài khoản: Đăng ký, đăng nhập, nạp tiền, quản lý thuê bao chatbot.

- Lưu và chia sẻ kết quả: Người dùng có thể lưu lại tìm kiếm và chia sẻ với người khác.

- Báo cáo và thống kê: Hệ thống cung cấp báo cáo về lịch sử sử dụng và chi tiêu của người dùng.

## Đóng góp

Chúng tôi luôn hoan nghênh các đóng góp từ cộng đồng để phát triển hệ thống tốt hơn. Nếu bạn muốn đóng góp, hãy gửi pull request hoặc báo lỗi qua GitLab Issues.

