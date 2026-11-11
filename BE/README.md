# THEFILM Backend

Backend đặt vé và quản lý chuỗi rạp, xây dựng bằng Java 21 và Spring Boot theo Layered Architecture.

## Kiến trúc hiện tại

Luồng xử lý của API:

Controller -> Service -> Repository -> Oracle Database

- Controller: nhận HTTP request, kiểm tra đầu vào, trả response.
- Service: interface khai báo nghiệp vụ; các lớp service.impl xử lý quy tắc, trạng thái và transaction.
- Repository: dùng Spring Data JPA / JpaRepository cho truy vấn và lưu dữ liệu; truy vấn đặc thù nằm trong repository JPA.
- Entity: mô hình dữ liệu lưu trữ.
- DTO: hợp đồng request/response cho Angular.
- Config/Security: cấu hình Oracle, CORS, Keycloak JWT và môi trường chạy.
- Exception: chuyển lỗi thành response thống nhất, không để lộ thông tin kết nối hoặc SQL.

Đây là một ứng dụng Spring Boot đơn khối có phân lớp, dùng Spring Data JPA; các nghiệp vụ tiếp theo sẽ được thêm vào đúng các lớp này.

## Phần đã dựng

- Health API.
- Danh sách, lọc và chi tiết phim đã được duyệt.
- Danh sách rạp đang hoạt động.
- Suất chiếu đang mở bán theo rạp, phim và ngày.
- Phân trang danh sách phim.
- Response JSON thống nhất và xử lý lỗi tập trung.
- Cấu hình JWT Resource Server đọc access token do Keycloak phát hành; public catalog mở đọc, các API nghiệp vụ khác yêu cầu token.
- Profile local dùng H2 và dữ liệu demo; profile oracle dùng Oracle, Flyway và migration hiện có.
- Migrations V001-V004 được đóng gói từ thư mục database/oracle/migrations; không tạo bản sao migration trong BE.

## Enum nghiệp vụ

Các mã trạng thái/loại giao dịch cố định trong schema được khai báo ở src/main/java/vn/thefilm/backend/enums. Giá trị enum viết đúng mã lưu trong Oracle; entity dùng @Enumerated(EnumType.STRING). YesNoFlag dùng AttributeConverter để lưu Y/N thay cho tên enum. Các danh mục cấu hình được bằng dữ liệu (loại phòng, thể loại phim, phương thức thanh toán, nhóm thuế) không đóng cứng thành enum.
## Chạy local nhanh

Yêu cầu JDK 21 và Maven 3.9+.

Từ thư mục C:\DATN\TheFilm\BE:

    mvn spring-boot:run

Mặc định chạy profile local với H2 trong bộ nhớ. Dữ liệu demo được tạo lại mỗi lần khởi động. API chạy ở cổng 8080; H2 console ở /h2-console.

Các API public:

    GET http://localhost:8080/api/v1/health
    GET http://localhost:8080/api/v1/public/films
    GET http://localhost:8080/api/v1/public/films?status=NOW_SHOWING
    GET http://localhost:8080/api/v1/public/films?status=COMING_SOON&keyword=mua
    GET http://localhost:8080/api/v1/public/films/1
    GET http://localhost:8080/api/v1/public/cinemas
    GET http://localhost:8080/api/v1/public/cinemas?province=TP.%20H%E1%BB%93%20Ch%C3%AD%20Minh
    GET http://localhost:8080/api/v1/public/screenings?date=2026-11-01
    GET http://localhost:8080/api/v1/public/screenings?cinemaId=1&filmId=1&date=2026-11-01

Ngày bỏ trống ở API suất chiếu sẽ lấy ngày hiện tại theo múi giờ Asia/Ho_Chi_Minh. Các suất demo được sinh tương đối theo thời điểm backend khởi động.

Các repository dùng entity ánh xạ trực tiếp các bảng Oracle đang cần cho catalog. Tắt Open EntityManager in View; service nạp rõ quan hệ cần trả về trước khi đóng transaction. Hibernate chỉ validate schema, không tự sửa hoặc tạo bảng.

Response thành công có dạng:

    {
      "success": true,
      "data": {},
      "error": null,
      "timestamp": "..."
    }

## Dùng Oracle và Keycloak

Profile Oracle tự chạy migration V001-V004. Chỉ trỏ đến schema mới, sạch và đã cấp quyền chạy DDL; không dùng schema cũ CINEMA_DB vì nó có bộ bảng khác và migration này không chuyển đổi dữ liệu cũ.

Trong PowerShell, khai báo thông tin kết nối qua biến môi trường trước khi chạy:

    $env:SPRING_PROFILES_ACTIVE = 'oracle'
    $env:SPRING_DATASOURCE_URL = 'jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
    $env:SPRING_DATASOURCE_USERNAME = 'THEFILM_APP'
    $env:SPRING_DATASOURCE_PASSWORD = '<mật khẩu schema>'
    $env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI = 'http://localhost:8081/realms/thefilm'
    $env:APP_CORS_ALLOWED_ORIGINS = 'http://localhost:4200,http://localhost:4300'
    mvn spring-boot:run

Không commit mật khẩu. Không chạy profile Oracle tới database có dữ liệu cần giữ trước khi rà soát migration. Các địa chỉ Keycloak trong ví dụ chỉ là giá trị local; phải thay bằng issuer thật của realm.

Keycloak cần phát role realm hoặc role client THEFILM_API vào claim roles. Spring chuyển các role đó thành ROLE_<TEN_ROLE>. Môi trường local hiện mở API để dựng giao diện; không dùng profile local khi triển khai nhóm hoặc production.

## Lưu ý nghiệp vụ

- Status phim NOW_SHOWING/COMING_SOON được suy ra từ suất chiếu trong database; APPROVED là phim được duyệt nhưng chưa có suất phù hợp.
- Danh sách suất chiếu chỉ trả trạng thái ON_SALE. API đang trả số ghế trống và tổng ghế không bị chặn.
- Chưa triển khai giữ ghế, thanh toán, vé, nhân viên, CSKH, hóa đơn/thuế giả lập, F&B hoặc báo cáo trong phần đầu này. Những luồng này cần được làm ở Service/Repository với transaction và khóa dữ liệu tương ứng, không tạo API giả.
- Schema local H2 chỉ phục vụ xem API catalog. Schema đầy đủ và nguồn migration Oracle vẫn nằm trong thư mục database/oracle.
## Lệnh chạy BE local
mvn spring-boot:run -Dspring-boot.run.profiles=oracle,devauth
