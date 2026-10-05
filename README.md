# VietBlog Backend API

> Nền tảng chia sẻ bài viết cộng đồng công nghệ Việt Nam. Hệ thống Backend được xây dựng theo kiến trúc **Clean Architecture** (Pragmatic approach) kết hợp **Domain-Driven Design (DDD lite)**.

## 🚀 Công nghệ sử dụng
- **Ngôn ngữ:** Java 17
- **Framework:** Spring Boot 3 (Spring Web, Spring Data JPA, Spring Security)
- **Cơ sở dữ liệu:** PostgreSQL 16+ (PostGIS & pgvector cho AI search)
- **Caching & Real-time:** Redis (chuẩn bị tích hợp)
- **Bảo mật:** JWT (JSON Web Token)
- **Build tool:** Gradle

## 🏗️ Kiến trúc Hệ thống (Clean Architecture)
Dự án được phân chia thành 4 lớp cốt lõi để đảm bảo nguyên tắc Dependency Rule:
1. **`domain` (Tầng Cốt lõi):** Chứa các Thực thể (Entities) và Exception nghiệp vụ thuần Java. Không chứa logic của Web Framework.
2. **`application` (Tầng Ứng dụng):** Chứa Use Cases (Services) và DTOs. Đóng vai trò điều phối (Orchestration).
3. **`presentation` (Tầng Giao tiếp):** Chứa Controllers, làm nhiệm vụ nhận/trả HTTP Request (RESTful API). Tuyệt đối không chứa logic nghiệp vụ.
4. **`infrastructure` (Tầng Hạ tầng):** Chứa cấu hình Security, kết nối Database (Repositories), và Global Exception Handler.

## 🛠️ Hướng dẫn cài đặt (Local Development)

### 1. Yêu cầu hệ thống
- JDK 17
- PostgreSQL 16
- Redis (Optional - for future features)

### 2. Cài đặt Database
Tạo một database trống trong PostgreSQL:
```sql
CREATE DATABASE vietblog;
```
Tài khoản mặc định trong `application.yml` là `postgres` / `postgres`. Hãy đổi lại nếu máy bạn cấu hình khác.

### 3. Chạy dự án
Mở terminal tại thư mục gốc của project (chứa file `build.gradle`):
```bash
# Build dự án
./gradlew build

# Chạy dự án
./gradlew bootRun
```
Server sẽ khởi chạy tại: `http://localhost:8080`

## 📁 Cấu trúc thư mục chi tiết
Vui lòng tham khảo file `docs/architecture.md` để hiểu sâu hơn về lý do và cách chia thư mục của dự án.

## 🧑‍💻 Git Flow
Dự án áp dụng mô hình Git Flow chuẩn:
- `main`: Nhánh production.
- `dev`: Nhánh tích hợp (integration).
- `feature/*`: Nhánh tính năng.

*Đồ án tốt nghiệp Đại học - Lê Thanh Tình*
