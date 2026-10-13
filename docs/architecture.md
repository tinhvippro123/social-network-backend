# Pragmatic Clean Architecture

Tài liệu này giải thích lý do lựa chọn và cách phân bố kiến trúc của dự án VietBlog Backend.

## 1. Triết lý thiết kế (Design Philosophy)
Dự án áp dụng mô hình **Pragmatic Clean Architecture** (Kiến trúc Sạch thực dụng) thay vì Strict Clean Architecture. 

**Lý do thỏa hiệp (Trade-offs):**
- Trong Strict Clean Architecture, `Domain Entity` (thực thể nghiệp vụ) và `JPA Entity` (thực thể Database) phải tách biệt hoàn toàn thông qua các Port và Mapper. Điều này làm x3 khối lượng code (Boilerplate).
- Với quy mô và quỹ thời gian của Đồ án Đại học (8 tuần), rủi ro thay đổi Framework hoặc Database cốt lõi (PostgreSQL sang MongoDB) là 0%. 
- Do đó, dự án gắn trực tiếp Annotations của Spring Data JPA (`@Entity`) vào tầng Domain. Việc này vi phạm nhẹ quy tắc "Độc lập Framework" của Clean Architecture nhưng mang lại **tốc độ phát triển cực cao**, tránh Over-engineering (YAGNI).

## 2. Tổ chức Package (Package-by-Layer)

Mã nguồn được chia thành 4 lớp độc lập:

### 2.1. Lớp `domain` (Core)
- **Vị trí:** Trong cùng của kiến trúc.
- **Nhiệm vụ:** Chứa các khái niệm cốt lõi của hệ thống.
- **Bao gồm:**
  - `Entities`: Post, User, Comment...
  - `Exceptions`: Các ngoại lệ thuần Java (`DomainException`, `ResourceNotFoundException`). Hoàn toàn không dính líu đến Spring Web (`@ResponseStatus`).
  - `Service (Domain Service)`: Tính toán các thuật toán phức tạp (VD: Thuật toán Trending Score) mà không gọi đến Database.

### 2.2. Lớp `application` (Use Cases)
- **Vị trí:** Bao bọc lớp Domain.
- **Nhiệm vụ:** Điều phối các chức năng (Orchestrator).
- **Bao gồm:**
  - `service`: Gọi Repository lấy dữ liệu, xử lý nghiệp vụ trung gian, gọi Domain Service tính toán, rồi lưu lại.
  - `dto`: Định dạng dữ liệu Input/Output.

### 2.3. Lớp `presentation` (Interface Adapters)
- **Nhiệm vụ:** Tương tác với người dùng hoặc hệ thống bên ngoài (REST API).
- **Quy tắc:** Controller tuyệt đối không chứa vòng lặp hay logic if/else nghiệp vụ. Controller cực kỳ "mỏng", chỉ nhận JSON và truyền thẳng vào Application Service.

### 2.4. Lớp `infrastructure` (Frameworks & Drivers)
- **Vị trí:** Ngoài cùng của hệ thống.
- **Nhiệm vụ:** Kết nối chi tiết kỹ thuật (Database, Security, HTTP Handling).
- **Bao gồm:**
  - `repository`: Chứa các Spring Data JPA Interfaces. (Đóng vai trò như các Port Out).
  - `security`: Cấu hình JWT, CORS.
  - `exception`: Chứa `GlobalExceptionHandler` (`@RestControllerAdvice`). Lớp này đón các Exception từ tầng Domain và tự động map thành HTTP Status Code. Điều này giúp giữ tầng Domain "sạch" khỏi HTTP.

## 3. Khác biệt với MVC truyền thống
Khác biệt lớn nhất với MVC truyền thống là **Luồng phụ thuộc (Dependency Rule)**:
- Trong MVC, Controller gọi Service, Service gọi trực tiếp các câu lệnh SQL ở Repository.
- Trong dự án này, Application Service chỉ phối hợp logic, mọi quyết định tính toán (Ví dụ: Một bài viết có hợp lệ hay không) được giao cho Domain xử lý. Các ngoại lệ được định nghĩa ở Domain thay vì bị buộc chặt vào Spring Web.
