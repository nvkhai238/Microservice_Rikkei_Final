# **ĐẶC TẢ YÊU CẦU PHẦN MỀM**

## **Hệ thống Thương mại điện tử đa kênh theo kiến trúc Microservices**

**Mã tài liệu:** SRS-OMNI-ECOM-001  
**Phiên bản:** 1.0  
**Trạng thái:** Bản cơ sở phục vụ phân tích, thiết kế, phát triển và nghiệm thu  
**Tiêu chuẩn cấu trúc:** IEEE 830-1998

---

# **1\. Giới thiệu**

## **1.1. Mục đích**

Tài liệu này đặc tả đầy đủ các yêu cầu phần mềm cho Hệ thống Thương mại điện tử đa kênh theo kiến trúc Microservices, sau đây gọi là **Hệ thống**. Tài liệu là cơ sở thống nhất giữa chủ nhiệm sản phẩm, giảng viên, nhóm phát triển, nhóm kiểm thử, nhóm vận hành và các bên tích hợp về:

* Phạm vi và ranh giới của sản phẩm;  
* Các tác nhân, quy trình nghiệp vụ và quy tắc xử lý;  
* Yêu cầu chức năng, dữ liệu, giao diện và tích hợp;  
* Yêu cầu chất lượng, bảo mật, hiệu năng, khả dụng và vận hành;  
* Tiêu chí nghiệm thu và khả năng truy vết yêu cầu.

Tài liệu mô tả hệ thống cần làm gì và các ràng buộc phải tuân thủ. Thiết kế kỹ thuật chi tiết, mã nguồn, đặc tả OpenAPI và lược đồ cơ sở dữ liệu vật lý là các sản phẩm riêng được dẫn xuất từ SRS này.

## **1.2. Phạm vi sản phẩm**

Hệ thống cung cấp nền tảng bán lẻ trực tuyến phục vụ khách hàng trên Web và ứng dụng di động thông qua một điểm truy cập API thống nhất. Phạm vi phiên bản 1.0 bao gồm:

* Đăng ký, đăng nhập, quản lý hồ sơ, địa chỉ và phân quyền người dùng;  
* Quản trị danh mục, thương hiệu, sản phẩm, biến thể SKU, giá và hình ảnh;  
* Duyệt, tìm kiếm, lọc và xem thông tin sản phẩm;  
* Quản lý giỏ hàng và tạo đơn hàng;  
* Kiểm tra, giữ chỗ, trừ và hoàn trả tồn kho an toàn khi có truy cập đồng thời;  
* Thanh toán trực tuyến, nhận webhook, theo dõi giao dịch và hoàn tiền;  
* Quản lý vòng đời đơn hàng;  
* Gửi email, SMS hoặc thông báo đẩy theo sự kiện;  
* Chịu tải trong kịch bản Flash Sale và duy trì tính đúng đắn của tồn kho.

Khái niệm “đa kênh” trong phiên bản này nghĩa là các kênh Web và Mobile dùng chung năng lực nghiệp vụ qua API Gateway. Đồng bộ điểm bán vật lý, sàn thương mại điện tử bên thứ ba hoặc mạng xã hội chưa thuộc phạm vi trừ khi được bổ sung bằng change request.

## **1.3. Đối tượng sử dụng tài liệu**

| Đối tượng | Mục đích sử dụng |
| ----- | ----- |
| Chủ nhiệm sản phẩm/Giảng viên | Xác nhận phạm vi, ưu tiên và tiêu chí đánh giá |
| Business Analyst | Quản lý yêu cầu, quy tắc nghiệp vụ và truy vết |
| Kiến trúc sư/Trưởng nhóm kỹ thuật | Thiết kế kiến trúc, API, sự kiện và dữ liệu |
| Lập trình viên | Hiện thực chức năng theo mã yêu cầu |
| Kiểm thử viên | Xây dựng test plan, test case và tiêu chí nghiệm thu |
| Đội tích hợp | Tích hợp cổng thanh toán, email, SMS và push notification |

## **1.4. Định nghĩa, từ viết tắt và thuật ngữ**

| Thuật ngữ | Định nghĩa |
| :---: | ----- |
| API Gateway | Điểm vào duy nhất, xác thực token, giới hạn lưu lượng và định tuyến yêu cầu |
| SKU | Đơn vị lưu kho đại diện cho một biến thể sản phẩm có thể bán |
| RBAC | Kiểm soát truy cập dựa trên vai trò |
| JWT | Token có chữ ký dùng để truyền thông tin định danh và quyền truy cập |
| Access Token | Token ngắn hạn dùng để truy cập tài nguyên bảo vệ |
| Refresh Token | Token dùng để xin cấp Access Token mới |
| Idempotency | Thuộc tính bảo đảm gửi lặp cùng một yêu cầu không tạo nhiều hiệu ứng nghiệp vụ |
| Saga | Mẫu điều phối giao dịch phân tán bằng các bước cục bộ và hành động bù |
| Outbox Pattern | Lưu thay đổi nghiệp vụ và sự kiện chờ phát trong cùng giao dịch cơ sở dữ liệu |
| Eventual Consistency | Các bản sao dữ liệu có thể lệch tạm thời nhưng cuối cùng hội tụ |
| Reservation | Bản ghi giữ một lượng tồn kho trong thời hạn nhất định cho đơn hàng |
| Overselling | Bán số lượng lớn hơn tồn kho khả dụng |
| Webhook | HTTP callback từ hệ thống bên ngoài về Hệ thống |
| PII | Dữ liệu nhận dạng cá nhân |
| DLQ | Hàng đợi chứa thông điệp xử lý thất bại sau số lần retry cho phép |
| Correlation ID | Mã liên kết log/trace xuyên suốt một yêu cầu hoặc quy trình phân tán |
| TBD | Nội dung chưa được quyết định, cần xác nhận |

## **1.5. Tài liệu tham chiếu**

1. IEEE Std 830-1998, *IEEE Recommended Practice for Software Requirements Specifications*.  
2. Đề tài 1, *Hệ thống Thương mại điện tử Đa kênh chuẩn doanh nghiệp*.  
3. OWASP Application Security Verification Standard, phiên bản áp dụng tại thời điểm triển khai.  
4. OpenAPI Specification, phiên bản được nhóm dự án lựa chọn.  
5. Tài liệu kỹ thuật chính thức của cổng thanh toán được lựa chọn.

Nếu có mâu thuẫn, thứ tự ưu tiên là: change request đã phê duyệt, SRS đã phê duyệt, hợp đồng API, tài liệu thiết kế, sau cùng là tài liệu đề tài ban đầu.

## **1.6. Tổng quan tài liệu**

Phần 2 mô tả bối cảnh, tác nhân, ràng buộc và giả định. Phần 3 đặc tả giao diện ngoài. Phần 4 mô tả các yêu cầu chức năng. Phần 5 trình bày yêu cầu dữ liệu. Phần 6 quy định các thuộc tính chất lượng. Phần 7 nêu quy tắc nghiệp vụ. Phần 8 mô tả tiêu chí nghiệm thu và phần 9 cung cấp ma trận truy vết.

---

# **2\. Mô tả tổng thể**

## **2.1. Bối cảnh sản phẩm**

Hệ thống là một nền tảng phân tán, trong đó mỗi dịch vụ sở hữu miền nghiệp vụ và kho dữ liệu riêng. Client không truy cập trực tiếp các dịch vụ nội bộ mà đi qua API Gateway. Giao tiếp đồng bộ dùng HTTPS/REST hoặc gRPC; giao tiếp bất đồng bộ dùng message broker. Không dịch vụ nào được phép truy vấn trực tiếp cơ sở dữ liệu thuộc dịch vụ khác.

### **![][image1]**

### **2.1.1. Phân rã dịch vụ**

| Thành phần | Trách nhiệm chính | Kho dữ liệu dự kiến |
| ----- | ----- | ----- |
| API Gateway | Điểm vào, xác thực token, rate limiting, routing, correlation ID | Cấu hình/Redis nếu cần |
| Identity and User Service | Tài khoản, hồ sơ, địa chỉ, RBAC, token | PostgreSQL hoặc MySQL |
| Product Catalog Service | Danh mục, thương hiệu, sản phẩm, SKU, hình ảnh, tìm kiếm | MongoDB và Elasticsearch |
| Order and Inventory Service | Giỏ hàng, đơn hàng, trạng thái, tồn kho, reservation | PostgreSQL và Redis |
| Payment and Billing Service | Payment intent, webhook, hoàn tiền, đối soát | PostgreSQL |
| Notification Service | Tiêu thụ sự kiện, gửi thông báo, lưu nhật ký | MongoDB/Redis theo thiết kế |
| Message Broker | Truyền sự kiện bền vững và tách rời dịch vụ | RabbitMQ hoặc Kafka |

## **2.2. Chức năng sản phẩm ở mức cao**

1. Quản lý định danh, phiên đăng nhập và quyền truy cập.  
2. Quản lý hồ sơ khách hàng và địa chỉ giao hàng.  
3. Quản lý catalog và chỉ mục tìm kiếm.  
4. Cho phép khách hàng duyệt, tìm kiếm và chọn SKU.  
5. Quản lý giỏ hàng theo khách hàng.  
6. Tạo đơn và chụp snapshot hàng hóa tại thời điểm mua.  
7. Giữ và cập nhật tồn kho không oversell trong tải đồng thời cao.  
8. Khởi tạo, xác nhận và hoàn tiền giao dịch thanh toán.  
9. Điều phối quy trình phân tán bằng sự kiện và hành động bù.  
10. Thông báo các thay đổi quan trọng cho khách hàng.  
11. Cho phép nhân viên quản trị sản phẩm, kho và đơn hàng.

## **2.3. Nhóm người dùng và đặc điểm**

| Tác nhân | Mô tả | Quyền chính |
| ----- | ----- | ----- |
| Khách vãng lai | Người chưa đăng nhập | Xem, tìm kiếm sản phẩm; đăng ký; đăng nhập |
| Khách hàng | Người mua đã xác thực | Quản lý hồ sơ, giỏ hàng, đặt hàng, thanh toán, theo dõi và hủy đơn hợp lệ |
| Nhân viên catalog | Quản trị nội dung bán hàng | CRUD danh mục, thương hiệu, sản phẩm, SKU, giá, hình ảnh |
| Nhân viên kho | Quản lý số lượng tồn | Nhập/điều chỉnh tồn kho, xem reservation và lịch sử biến động |
| Nhân viên vận hành đơn | Xử lý đơn | Xác nhận, chuyển trạng thái, xem lịch sử đơn |
| Nhân viên tài chính | Theo dõi giao dịch | Xem giao dịch, yêu cầu hoàn tiền, đối soát |
| Quản trị viên | Quản trị toàn hệ thống | Quản lý vai trò, quyền và các chức năng quản trị |
| Hệ thống cổng thanh toán | Tác nhân máy | Nhận yêu cầu thanh toán và gửi webhook |
| Nhà cung cấp thông báo | Tác nhân máy | Nhận yêu cầu gửi email, SMS hoặc push |

## **2.4. Môi trường vận hành**

* Client Web trên các phiên bản ổn định còn được hỗ trợ của Chrome, Edge, Firefox và Safari.  
* Client Mobile trên Android/iOS; phạm vi phiên bản hệ điều hành là TBD.  
* Các ứng dụng khách giao tiếp với hệ thống qua HTTPS.  
* PostgreSQL/MySQL, MongoDB, Elasticsearch, Redis và RabbitMQ/Kafka được lựa chọn theo quyết định kiến trúc đã phê duyệt.

## **2.5. Ràng buộc thiết kế**

* CON-01: Hệ thống PHẢI được phân tách thành tối thiểu bốn microservice có thể build và chạy độc lập.  
* CON-02: Mỗi dịch vụ PHẢI sở hữu dữ liệu của mình; cấm truy vấn chéo trực tiếp giữa các database.  
* CON-03: API công khai PHẢI đi qua API Gateway và dùng HTTPS.  
* CON-04: Ít nhất một luồng nghiệp vụ PHẢI sử dụng message broker theo mô hình bất đồng bộ.  
* CON-05: Quy trình đặt hàng–tồn kho–thanh toán PHẢI xử lý giao dịch phân tán bằng Saga và hành động bù.  
* CON-06: Dịch vụ phát domain event PHẢI dùng Transactional Outbox hoặc cơ chế tương đương để tránh mất sự kiện.  
* CON-09: Bí mật PHẢI được quản lý bằng cơ chế bảo vệ phù hợp và không được ghi cứng trong mã nguồn.

## **2.6. Giả định và phụ thuộc**

* A-01: Khách hàng phải đăng nhập trước khi checkout.  
* A-02: Phiên bản 1.0 hỗ trợ một loại tiền tệ mặc định là VND; hỗ trợ đa tiền tệ là ngoài phạm vi.  
* A-03: Phí vận chuyển và thuế được cung cấp bởi cấu hình/quy tắc nội bộ đơn giản; tích hợp hãng vận chuyển và máy tính thuế chuyên dụng là TBD.  
* A-04: Một cổng thanh toán sandbox sẽ được chọn trong VNPay, MoMo, ZaloPay hoặc Stripe cho bản demo.  
* A-05: Email là kênh thông báo bắt buộc; SMS và push phụ thuộc nhà cung cấp và cấu hình.  
* A-06: Tính nhất quán mạnh áp dụng trong ranh giới một dịch vụ; giữa các dịch vụ chấp nhận nhất quán cuối cùng có kiểm soát.  
* A-07: Sản phẩm vật lý và quy trình giao hàng cơ bản là trọng tâm; sản phẩm số, subscription, marketplace nhiều người bán ngoài phạm vi.  
* A-08: Thời gian giữ kho mặc định là 15 phút nhưng PHẢI cấu hình được; giá trị cuối cùng là TBD.

## **2.7. Ngoài phạm vi phiên bản 1.0**

* Quản lý cửa hàng vật lý/POS và chuyển kho đa địa điểm nâng cao;  
* Đồng bộ listing với sàn thương mại điện tử hoặc mạng xã hội;  
* Chương trình khách hàng thân thiết, voucher, coupon và promotion engine phức tạp;  
* Marketplace nhiều nhà bán, chia hoa hồng và thanh toán cho nhà bán;  
* Đấu giá, subscription, sản phẩm số;  
* Hệ thống quản lý vận tải hoặc tối ưu tuyến giao hàng;  
* AI recommendation, dynamic pricing và phân tích dữ liệu nâng cao;  
* Đa ngôn ngữ, đa tiền tệ và đa quốc gia;  
* Quy trình trả hàng sau giao hàng, trừ phần hoàn tiền cơ bản được mô tả tại mục thanh toán.

---

# **3\. Yêu cầu giao diện ngoài**

## **3.1. Giao diện người dùng**

### **3.1.1. Giao diện khách hàng**

* UI-01: Web/Mobile PHẢI cung cấp các màn hình đăng ký, đăng nhập, quên/đặt lại mật khẩu, hồ sơ và sổ địa chỉ.  
* UI-02: Giao diện catalog PHẢI hỗ trợ danh sách sản phẩm, tìm kiếm, lọc, sắp xếp, phân trang và trang chi tiết sản phẩm/SKU.  
* UI-03: Giao diện giỏ hàng PHẢI hiển thị SKU, số lượng, đơn giá, thành tiền, tạm tính và cảnh báo khi sản phẩm không còn hợp lệ hoặc không đủ tồn.  
* UI-04: Checkout PHẢI cho phép xác nhận địa chỉ, thông tin liên hệ, các khoản tiền, phương thức thanh toán và nội dung đơn trước khi đặt.  
* UI-05: Trang kết quả thanh toán PHẢI phân biệt trạng thái đang xử lý, thành công, thất bại và hết hạn; không được coi redirect phía client là bằng chứng thanh toán cuối cùng.  
* UI-06: Khách hàng PHẢI xem được danh sách đơn, chi tiết đơn, dòng thời gian trạng thái và giao dịch liên quan.  
* UI-07: Hành động không thể hoàn tác hoặc có ảnh hưởng tài chính PHẢI có bước xác nhận.  
* UI-08: Thông báo lỗi PHẢI dễ hiểu, không làm lộ stack trace, thông tin hạ tầng hoặc bí mật.  
* UI-09: Giao diện NÊN đáp ứng WCAG 2.1 mức AA cho các luồng chính.  
* UI-10: Ngày giờ hiển thị theo múi giờ người dùng hoặc mặc định Asia/Ho\_Chi\_Minh; dữ liệu trao đổi giữa hệ thống dùng ISO 8601\.

### **3.1.2. Giao diện quản trị**

* UI-ADM-01: Giao diện quản trị PHẢI yêu cầu tài khoản có vai trò/quyền phù hợp.  
* UI-ADM-02: Danh sách dữ liệu PHẢI hỗ trợ tìm kiếm, lọc, sắp xếp, phân trang và chỉ báo trạng thái.  
* UI-ADM-03: Biểu mẫu PHẢI kiểm tra dữ liệu phía client và server, đồng thời chỉ rõ trường không hợp lệ.  
* UI-ADM-04: Các thay đổi sản phẩm, giá, tồn kho, quyền và hoàn tiền PHẢI hiển thị kết quả rõ ràng và được ghi audit.

## **3.2. Giao diện phần cứng**

Hệ thống không yêu cầu thiết bị phần cứng chuyên dụng và không giao tiếp trực tiếp với thiết bị ngoại vi.

## **3.3. Giao diện phần mềm**

| Hệ thống/Thành phần | Hướng giao tiếp | Giao thức/Định dạng | Yêu cầu chính |
| ----- | ----- | ----- | ----- |
| Web/Mobile ↔ API Gateway | Hai chiều | HTTPS, JSON | JWT, versioning, rate limit |
| Gateway ↔ Microservices | Hai chiều | HTTP/REST hoặc gRPC | Timeout, correlation ID, retry có kiểm soát |
| Order ↔ Product Catalog | Hai chiều | gRPC/HTTP2 dự kiến | Tra cứu SKU; mục tiêu nội bộ dưới 5 ms trong điều kiện chuẩn |
| Services ↔ Broker | Hai chiều | AMQP/Kafka protocol | Durable message, ack, retry, DLQ |
| Payment ↔ Payment Provider | Hai chiều | HTTPS/JSON/form tùy provider | Chữ ký, idempotency, webhook |
| Notification ↔ Provider | Đi | HTTPS/SMTP/provider SDK | Retry, timeout, tracking ID |

## 

## 

## **3.4. Giao diện truyền thông**

* COM-01: Giao tiếp công khai PHẢI dùng TLS 1.2 trở lên; TLS 1.3 được ưu tiên.  
* COM-02: API JSON PHẢI dùng UTF-8 và media type phù hợp.  
* COM-03: API PHẢI được version hóa, tối thiểu theo đường dẫn hoặc header.  
* COM-04: Mọi phản hồi API PHẢI có mã HTTP đúng ngữ nghĩa và cấu trúc lỗi thống nhất gồm tối thiểu **code**, **message**, **correlationId**; **details** chỉ dùng khi an toàn.  
* COM-05: Mỗi request PHẢI có hoặc được cấp **correlationId**, sau đó truyền qua các lời gọi đồng bộ và metadata sự kiện.  
* COM-06: Client PHẢI đặt timeout; retry chỉ áp dụng với lỗi tạm thời và thao tác an toàn/idempotent, dùng exponential backoff có jitter.  
* COM-07: Event PHẢI có tối thiểu **eventId**, **eventType**, **eventVersion**, **occurredAt**, **correlationId**, **producer** và payload.  
* COM-08: Consumer PHẢI xử lý trùng lặp an toàn theo **eventId** hoặc business key.  
* COM-09: Thông điệp thất bại sau ngưỡng retry PHẢI chuyển vào DLQ và tạo cảnh báo vận hành.

---

# **4\. Yêu cầu chức năng cụ thể**

## **4.1. Quản lý định danh và người dùng**

### **4.1.1. Đăng ký và kích hoạt tài khoản**

* **FR-IAM-001:** Hệ thống PHẢI cho phép khách vãng lai đăng ký bằng email hoặc số điện thoại theo cấu hình.  
* **FR-IAM-002:** Hệ thống PHẢI kiểm tra định dạng, tính bắt buộc và tính duy nhất của định danh đăng nhập đã chuẩn hóa.  
* **FR-IAM-003:** Hệ thống PHẢI áp dụng chính sách mật khẩu được cấu hình và không lưu mật khẩu dạng rõ hoặc có thể giải mã.  
* **FR-IAM-004:** Hệ thống PHẢI tạo tài khoản ở trạng thái phù hợp và gửi yêu cầu xác minh nếu chính sách bật xác minh.  
* **FR-IAM-005:** Token xác minh PHẢI có thời hạn, dùng một lần và không lưu dạng rõ.  
* **FR-IAM-006:** Hệ thống PHẢI ghi audit cho việc đăng ký, kích hoạt và thay đổi định danh quan trọng.

### **4.1.2. Xác thực và phiên**

* **FR-IAM-007:** Người dùng PHẢI có thể đăng nhập bằng thông tin hợp lệ và nhận Access Token cùng Refresh Token.  
* **FR-IAM-008:** Hệ thống PHẢI từ chối thông tin xác thực sai bằng thông báo không tiết lộ tài khoản có tồn tại hay không.  
* **FR-IAM-009:** Hệ thống PHẢI giới hạn tốc độ và có cơ chế giảm thiểu brute force đối với đăng nhập, refresh và quên mật khẩu.  
* **FR-IAM-010:** Access Token PHẢI có thời hạn ngắn và chứa tối thiểu subject, thời điểm cấp, hết hạn và thông tin quyền/role cần thiết.  
* **FR-IAM-011:** Refresh Token PHẢI có thể bị thu hồi, xoay vòng khi sử dụng và phát hiện reuse theo chính sách.  
* **FR-IAM-012:** Người dùng PHẢI có thể đăng xuất phiên hiện tại; quản trị viên hoặc người dùng PHẢI có thể thu hồi toàn bộ phiên khi có rủi ro.  
* **FR-IAM-013:** Hệ thống PHẢI hỗ trợ quy trình quên và đặt lại mật khẩu bằng token có thời hạn, dùng một lần.

### **4.1.3. Hồ sơ, địa chỉ và phân quyền**

* **FR-IAM-014:** Khách hàng PHẢI xem và cập nhật họ tên, số điện thoại và các thuộc tính hồ sơ được phép.  
* **FR-IAM-015:** Khách hàng PHẢI có thể thêm, sửa, xóa và chọn địa chỉ mặc định.  
* **FR-IAM-016:** Hệ thống PHẢI xác thực cấu trúc địa chỉ và từ chối thao tác với địa chỉ không thuộc người dùng.  
* **FR-IAM-017:** Hệ thống PHẢI hỗ trợ RBAC với vai trò tối thiểu Customer, CatalogStaff, InventoryStaff, OrderStaff, FinanceStaff và Administrator.  
* **FR-IAM-018:** Mỗi endpoint bảo vệ PHẢI kiểm tra cả xác thực và quyền truy cập phía server.  
* **FR-IAM-019:** Chỉ người có quyền quản trị IAM mới được gán/thu hồi vai trò; mọi thay đổi PHẢI được audit.

## **4.2. Quản lý catalog sản phẩm**

### **4.2.1. Danh mục và thương hiệu**

* **FR-CAT-001:** Nhân viên có quyền PHẢI tạo, xem, sửa, kích hoạt/ngừng kích hoạt danh mục.  
* **FR-CAT-002:** Hệ thống PHẢI hỗ trợ cấu trúc danh mục phân cấp và ngăn chu trình trong cây danh mục.  
* **FR-CAT-003:** Nhân viên có quyền PHẢI tạo, xem, sửa, kích hoạt/ngừng kích hoạt thương hiệu.  
* **FR-CAT-004:** Không được xóa cứng danh mục/thương hiệu đang được sản phẩm tham chiếu; Hệ thống PHẢI từ chối hoặc chuyển sang ngừng hoạt động theo quy tắc được chọn.

### **4.2.2. Sản phẩm, SKU, giá và hình ảnh**

* **FR-CAT-005:** Nhân viên catalog PHẢI tạo và cập nhật sản phẩm với tên, mô tả, danh mục, thương hiệu, trạng thái và metadata cần thiết.  
* **FR-CAT-006:** Mỗi sản phẩm PHẢI có một hoặc nhiều SKU; mã SKU PHẢI duy nhất toàn hệ thống.  
* **FR-CAT-007:** Mỗi SKU PHẢI có tập thuộc tính biến thể, giá bán, đơn vị tiền tệ và trạng thái bán.  
* **FR-CAT-008:** Giá PHẢI là số không âm, lưu bằng kiểu số thập phân hoặc đơn vị nhỏ nhất; không dùng floating-point nhị phân cho tính tiền.  
* **FR-CAT-009:** Hệ thống PHẢI cho phép gắn nhiều hình ảnh cho sản phẩm/SKU, chỉ định ảnh chính và kiểm tra loại/kích thước tệp theo cấu hình.  
* **FR-CAT-010:** Sản phẩm chỉ xuất hiện trên kênh bán khi sản phẩm và SKU tương ứng đều ở trạng thái được phép công bố.  
* **FR-CAT-011:** Mọi thay đổi catalog quan trọng PHẢI ghi người thay đổi, thời điểm và giá trị trước/sau hoặc thông tin tương đương.  
* **FR-CAT-012:** Khi sản phẩm/SKU/giá thay đổi, Hệ thống PHẢI cập nhật chỉ mục tìm kiếm theo cơ chế có thể retry và quan sát được.

### **4.2.3. Duyệt và tìm kiếm**

* **FR-CAT-013:** Người dùng PHẢI xem được danh sách sản phẩm đang công bố với phân trang ổn định.  
* **FR-CAT-014:** Người dùng PHẢI tìm kiếm full-text theo tối thiểu tên, mô tả, mã SKU, thương hiệu và danh mục phù hợp.  
* **FR-CAT-015:** Người dùng PHẢI lọc theo danh mục, thương hiệu, khoảng giá, trạng thái còn hàng và thuộc tính biến thể khả dụng.  
* **FR-CAT-016:** Người dùng PHẢI sắp xếp theo mức liên quan, giá và thời điểm cập nhật/tạo theo thiết kế UI.  
* **FR-CAT-017:** Trang chi tiết PHẢI trả về thông tin sản phẩm, các SKU đang bán, giá hiện hành, hình ảnh và chỉ báo còn hàng.  
* **FR-CAT-018:** Nếu chỉ mục tìm kiếm chưa đồng bộ, nguồn dữ liệu catalog vẫn là nguồn sự thật; hệ thống PHẢI có cơ chế tái lập chỉ mục.

## **4.3. Quản lý giỏ hàng**

* **FR-CART-001:** Khách hàng đã xác thực PHẢI có một giỏ hàng hoạt động trên các kênh dùng chung tài khoản.  
* **FR-CART-002:** Khách hàng PHẢI thêm SKU đang bán vào giỏ với số lượng nguyên dương.  
* **FR-CART-003:** Khách hàng PHẢI cập nhật số lượng hoặc xóa dòng hàng khỏi giỏ.  
* **FR-CART-004:** Hệ thống PHẢI từ chối số lượng vượt giới hạn đặt hàng theo SKU hoặc cấu hình.  
* **FR-CART-005:** Việc thêm vào giỏ KHÔNG đồng nghĩa giữ tồn kho trừ khi chính sách được thay đổi rõ ràng.  
* **FR-CART-006:** Khi hiển thị giỏ, Hệ thống PHẢI đánh giá lại trạng thái bán, giá hiện hành và khả năng đáp ứng tồn kho.  
* **FR-CART-007:** Hệ thống PHẢI thông báo các thay đổi về giá, trạng thái hoặc tồn kho trước khi checkout.  
* **FR-CART-008:** Tổng tiền hiển thị trong giỏ là ước tính; tổng tiền được chốt trong đơn theo quy tắc tại thời điểm tạo đơn.

## **4.4. Quản lý tồn kho**

### **4.4.1. Số lượng và biến động tồn**

* **FR-INV-001:** Hệ thống PHẢI quản lý tồn kho theo SKU và địa điểm kho logic; phiên bản tối thiểu có một kho mặc định.  
* **FR-INV-002:** Với mỗi SKU/kho, Hệ thống PHẢI quản lý tối thiểu onHand, reserved và available, trong đó available \= onHand \- reserved theo mô hình được phê duyệt.  
* **FR-INV-003:** Nhân viên kho có quyền PHẢI có thể nhập hoặc điều chỉnh tồn với lý do bắt buộc.  
* **FR-INV-004:** Mọi biến động tồn PHẢI tạo bản ghi ledger bất biến gồm SKU, kho, loại biến động, số lượng, tham chiếu nghiệp vụ, người/hệ thống thực hiện và thời điểm.  
* **FR-INV-005:** Hệ thống PHẢI từ chối thao tác làm onHand, reserved hoặc available âm, trừ cơ chế điều chỉnh đặc biệt đã phê duyệt và audit.

### **4.4.2. Giữ và giải phóng tồn**

* **FR-INV-006:** Khi tạo đơn, Hệ thống PHẢI kiểm tra và giữ tồn cho toàn bộ dòng hàng như một thao tác nghiệp vụ nhất quán.  
* **FR-INV-007:** Reservation PHẢI có mã duy nhất, order ID, SKU, số lượng, trạng thái và thời điểm hết hạn.  
* **FR-INV-008:** Yêu cầu giữ tồn lặp với cùng khóa idempotency/order ID PHẢI trả cùng kết quả logic và không giữ kho hai lần.  
* **FR-INV-009:** Hệ thống PHẢI bảo đảm không oversell khi nhiều yêu cầu đồng thời mua cùng SKU bằng atomic conditional update, database lock hoặc distributed lock đã kiểm chứng.  
* **FR-INV-010:** Nếu không đủ tồn ở bất kỳ dòng nào, Hệ thống PHẢI không hoàn tất reservation một phần, hoặc PHẢI bù toàn bộ phần đã giữ trước khi trả thất bại.  
* **FR-INV-011:** Reservation chưa được thanh toán PHẢI tự động hết hạn sau thời gian cấu hình và giải phóng đúng số lượng.  
* **FR-INV-012:** Khi thanh toán được xác nhận, Hệ thống PHẢI chuyển reservation thành trừ tồn chính thức đúng một lần.  
* **FR-INV-013:** Khi đơn bị hủy trước khi trừ tồn, reservation PHẢI được giải phóng đúng một lần.  
* **FR-INV-014:** Khi cần bù sau khi đã trừ tồn, Hệ thống PHẢI tạo biến động hoàn tồn có tham chiếu tới đơn/giao dịch gốc.  
* **FR-INV-015:** Job xử lý reservation hết hạn PHẢI an toàn khi chạy lặp hoặc chạy đồng thời trên nhiều instance.

## **4.5. Quản lý đơn hàng**

### **4.5.1. Tạo đơn**

* **FR-ORD-001:** Chỉ khách hàng đã xác thực mới được tạo đơn trong phiên bản 1.0.  
* **FR-ORD-002:** Yêu cầu tạo đơn PHẢI có Idempotency-Key; gửi lặp cùng khóa và cùng payload PHẢI không tạo thêm đơn.  
* **FR-ORD-003:** Hệ thống PHẢI kiểm tra quyền sở hữu địa chỉ, giỏ hàng không rỗng, trạng thái SKU, giá và tồn kho trước khi chấp nhận đơn.  
* **FR-ORD-004:** Đơn hàng PHẢI lưu snapshot tối thiểu gồm mã/tên SKU, tên sản phẩm, thuộc tính biến thể, đơn giá, số lượng, giảm giá nếu có và thành tiền tại thời điểm mua.  
* **FR-ORD-005:** Đơn PHẢI lưu snapshot địa chỉ giao hàng và thông tin liên hệ để thay đổi hồ sơ sau đó không làm biến đổi đơn lịch sử.  
* **FR-ORD-006:** Hệ thống PHẢI tính tổng tiền xác định, bao gồm tạm tính, giảm giá, thuế, phí vận chuyển và tổng thanh toán; phiên bản 1.0 có thể đặt các khoản chưa áp dụng bằng 0\.  
* **FR-ORD-007:** Hệ thống PHẢI tạo mã đơn duy nhất, không chứa PII và phù hợp để hỗ trợ tra cứu.  
* **FR-ORD-008:** Sau khi lưu đơn và reservation thành công, Order Service PHẢI phát **OrderCreated** thông qua Outbox.  
* **FR-ORD-009:** Nếu giữ kho thất bại, đơn PHẢI chuyển sang trạng thái thất bại/hủy phù hợp và không được yêu cầu thanh toán.

### **4.5.2. Trạng thái và vòng đời đơn**

Các trạng thái nghiệp vụ tối thiểu:

| Trạng thái | Ý nghĩa |
| ----- | ----- |
| PENDING | Đơn đã tạo, đang chờ hoàn tất bước tiền xử lý/thanh toán |
| CONFIRMED | Thanh toán hoặc điều kiện xác nhận đã hoàn tất |
| SHIPPING | Đơn đã bàn giao để giao hàng |
| COMPLETED | Đơn đã hoàn tất |
| CANCELLED | Đơn đã hủy |

* **FR-ORD-010:** Hệ thống PHẢI kiểm soát chuyển trạng thái theo state machine, không cho phép bước chuyển không hợp lệ.  
* **FR-ORD-011:** Mỗi chuyển trạng thái PHẢI ghi trạng thái cũ, trạng thái mới, lý do, tác nhân và thời điểm.  
* **FR-ORD-012:** Khách hàng PHẢI xem được các đơn thuộc chính mình và không được truy cập đơn của người khác.  
* **FR-ORD-013:** Nhân viên vận hành có quyền PHẢI tìm kiếm và lọc đơn theo mã, khách hàng, trạng thái và khoảng thời gian.  
* **FR-ORD-014:** Nhân viên vận hành PHẢI có thể chuyển đơn qua các trạng thái hợp lệ theo quyền.  
* **FR-ORD-015:** Mọi thay đổi trạng thái thành công PHẢI phát domain event tương ứng thông qua Outbox.

### **4.5.3. Hủy đơn**

* **FR-ORD-016:** Khách hàng PHẢI có thể yêu cầu hủy khi đơn ở trạng thái cho phép; ma trận trạng thái cuối cùng là TBD.  
* **FR-ORD-017:** Hệ thống PHẢI lưu lý do hủy và tác nhân yêu cầu hủy.  
* **FR-ORD-018:** Hủy đơn PHẢI kích hoạt giải phóng/hoàn tồn và yêu cầu hoàn tiền nếu thanh toán đã thành công.  
* **FR-ORD-019:** Đơn chỉ chuyển CANCELLED khi các bước bắt buộc của Saga hủy đạt trạng thái được định nghĩa; các bước bất đồng bộ PHẢI được thể hiện là đang xử lý.  
* **FR-ORD-020:** Yêu cầu hủy lặp PHẢI idempotent.

## **4.6. Thanh toán và lập hóa đơn**

### **4.6.1. Khởi tạo và xác nhận thanh toán**

* **FR-PAY-001:** Payment Service PHẢI tạo một payment intent gắn với đơn, số tiền, tiền tệ, provider và trạng thái.  
* **FR-PAY-002:** Số tiền thanh toán PHẢI lấy từ snapshot tổng tiền của đơn, không tin cậy số tiền do client gửi.  
* **FR-PAY-003:** Mỗi yêu cầu khởi tạo thanh toán PHẢI idempotent theo đơn và lần thanh toán hợp lệ.  
* **FR-PAY-004:** Hệ thống PHẢI hỗ trợ ít nhất một cổng thanh toán trong môi trường sandbox và cô lập adapter theo provider.  
* **FR-PAY-005:** Hệ thống PHẢI lưu mã tham chiếu phía provider, không lưu dữ liệu thẻ nhạy cảm nếu không cần thiết.  
* **FR-PAY-006:** Hệ thống PHẢI xử lý các trạng thái tối thiểu **PENDING**, **SUCCEEDED**, **FAILED**, **EXPIRED**, **REFUND\_PENDING**, **REFUNDED**, **PARTIALLY\_REFUNDED** nếu provider hỗ trợ.

### **4.6.2. Webhook**

* **FR-PAY-007:** Endpoint webhook PHẢI xác minh chữ ký/xác thực theo tài liệu provider trước khi xử lý.  
* **FR-PAY-008:** Webhook PHẢI kiểm tra timestamp/replay protection nếu provider hỗ trợ.  
* **FR-PAY-009:** Webhook trùng PHẢI được xử lý idempotent và không phát sinh ghi nhận thanh toán hoặc event trùng về mặt nghiệp vụ.  
* **FR-PAY-010:** Hệ thống PHẢI đối chiếu order reference, payment reference, số tiền và tiền tệ trước khi xác nhận thành công.  
* **FR-PAY-011:** Trạng thái cuối cùng từ webhook/provider là nguồn xác nhận thanh toán; redirect phía client không được tự quyết định thanh toán thành công.  
* **FR-PAY-012:** Sau khi ghi nhận kết quả, Payment Service PHẢI phát event thích hợp qua Outbox.  
* **FR-PAY-013:** Webhook hợp lệ PHẢI được phản hồi trong thời gian phù hợp với provider; xử lý dài PHẢI chuyển sang bất đồng bộ.

### **4.6.3. Hoàn tiền và đối soát**

* **FR-PAY-014:** Nhân viên tài chính có quyền PHẢI có thể yêu cầu hoàn tiền toàn phần hoặc một phần nếu provider và chính sách hỗ trợ.  
* **FR-PAY-015:** Tổng tiền hoàn lũy kế PHẢI không vượt quá số tiền đã thanh toán thành công.  
* **FR-PAY-016:** Yêu cầu hoàn tiền PHẢI idempotent, có lý do, người yêu cầu và audit trail.  
* **FR-PAY-017:** Kết quả hoàn tiền PHẢI cập nhật trạng thái giao dịch và phát event cho Order/Notification.  
* **FR-PAY-018:** Hệ thống PHẢI có tác vụ đối soát định kỳ giữa giao dịch nội bộ và dữ liệu provider, tạo báo cáo sai lệch để xử lý.  
* **FR-PAY-019:** Không được sửa/xóa trực tiếp bản ghi giao dịch tài chính đã hoàn tất; điều chỉnh phải qua giao dịch bù có liên kết.

## **4.7. Điều phối Saga và xử lý sự kiện**

### **4.7.1. Luồng đặt hàng chuẩn**

![][image2]

* **FR-SAGA-001:** Mỗi Saga instance PHẢI có định danh, trạng thái, correlation ID và lịch sử bước xử lý.  
* **FR-SAGA-002:** Luồng thành công PHẢI bảo đảm đơn, tồn kho và thanh toán hội tụ về trạng thái nhất quán.  
* **FR-SAGA-003:** Khi thanh toán thất bại/hết hạn, Hệ thống PHẢI giải phóng reservation và chuyển đơn sang trạng thái phù hợp.  
* **FR-SAGA-004:** Khi xác nhận đơn thất bại sau thanh toán thành công và không thể phục hồi trong ngưỡng retry, Hệ thống PHẢI khởi tạo hoàn tiền hoặc đưa vào hàng đợi can thiệp thủ công có cảnh báo.  
* **FR-SAGA-005:** Hành động bù PHẢI idempotent và có audit trail.  
* **FR-SAGA-006:** Dịch vụ không sẵn sàng tạm thời PHẢI được retry theo chính sách; lỗi nghiệp vụ không retry vô hạn.  
* **FR-SAGA-007:** Sự kiện đến sai thứ tự PHẢI được từ chối, trì hoãn hoặc hợp nhất an toàn dựa trên version/state transition.  
* **FR-SAGA-008:** Outbox publisher PHẢI đánh dấu sự kiện đã phát theo cách an toàn và có thể phát lại khi broker lỗi.  
* **FR-SAGA-009:** Consumer PHẢI lưu bằng chứng đã xử lý hoặc áp dụng thao tác idempotent để chịu được at-least-once delivery.  
* **FR-SAGA-010:** Nhân viên vận hành PHẢI có khả năng quan sát Saga lỗi, retry hoặc kích hoạt xử lý bù theo quyền; thao tác thủ công PHẢI audit.

## **4.8. Dịch vụ thông báo**

* **FR-NOT-001:** Notification Service PHẢI tiêu thụ các event được cấu hình như đăng ký tài khoản, tạo đơn, xác nhận thanh toán, đổi trạng thái đơn, hủy và hoàn tiền.  
* **FR-NOT-002:** Hệ thống PHẢI hỗ trợ email; SMS và push là kênh tùy cấu hình.  
* **FR-NOT-003:** Template PHẢI được định danh theo loại thông báo, kênh, ngôn ngữ và phiên bản nếu áp dụng.  
* **FR-NOT-004:** Nội dung thông báo PHẢI lấy từ dữ liệu event/snapshot cần thiết, không truy vấn trực tiếp database của dịch vụ khác.  
* **FR-NOT-005:** Mỗi lần gửi PHẢI có trạng thái **QUEUED**, **SENT**, **DELIVERED** nếu provider hỗ trợ, hoặc **FAILED**, cùng provider message ID.  
* **FR-NOT-006:** Lỗi tạm thời PHẢI retry với backoff; lỗi vĩnh viễn hoặc vượt ngưỡng PHẢI vào DLQ/cảnh báo.  
* **FR-NOT-007:** Xử lý event trùng PHẢI không gửi lặp ngoài chính sách cho phép.  
* **FR-NOT-008:** Lỗi Notification Service KHÔNG được làm thất bại việc đặt hàng hoặc xác nhận thanh toán đã hoàn tất.  
* **FR-NOT-009:** PII trong log thông báo PHẢI được che hoặc loại bỏ; nội dung thông báo nhạy cảm phải tuân thủ chính sách lưu giữ.

## **4.9. API Gateway**

* **FR-GW-001:** Gateway PHẢI là điểm vào công khai duy nhất cho API nghiệp vụ.  
* **FR-GW-002:** Gateway PHẢI định tuyến theo version/path đến đúng upstream và trả lỗi chuẩn khi upstream không khả dụng.  
* **FR-GW-003:** Gateway PHẢI xác minh tính hợp lệ, chữ ký và thời hạn JWT với endpoint bảo vệ.  
* **FR-GW-004:** Gateway PHẢI truyền danh tính/quyền theo cơ chế chống giả mạo; service đích vẫn PHẢI kiểm tra authorization.  
* **FR-GW-005:** Gateway PHẢI áp dụng rate limit theo IP, client hoặc user cho endpoint nhạy cảm; ngưỡng là cấu hình.  
* **FR-GW-006:** Gateway PHẢI giới hạn kích thước request, header và thời gian kết nối theo cấu hình.  
* **FR-GW-007:** Gateway PHẢI cấp/truyền correlation ID và ghi access log có cấu trúc.  
* **FR-GW-008:** Gateway PHẢI hỗ trợ CORS allowlist; không dùng wildcard với credential trong production.

## **4.10. Quản trị và audit**

* **FR-ADM-001:** Hệ thống PHẢI cung cấp chức năng quản trị theo quyền cho catalog, tồn kho, đơn hàng, thanh toán và người dùng.  
* **FR-ADM-002:** Tác vụ quản trị nhạy cảm PHẢI ghi audit gồm actor, action, target, before/after hoặc delta, timestamp, IP/correlation ID và kết quả.  
* **FR-ADM-003:** Audit log PHẢI chống sửa đổi thông thường và không cho người dùng nghiệp vụ xóa.  
* **FR-ADM-004:** Truy vấn danh sách lớn PHẢI phân trang và có giới hạn kích thước trang.  
* **FR-ADM-005:** Export dữ liệu, nếu triển khai, PHẢI kiểm tra quyền và ghi audit; phạm vi export chi tiết là TBD.

---

# **5\. Yêu cầu dữ liệu**

## **5.1. Các thực thể logic chính**

| Miền | Thực thể chính | Thuộc tính tối thiểu |
| ----- | ----- | ----- |
| Identity | User | userId, login identifier, password hash, status, timestamps |
| Identity | Role/Permission | roleId, permission code, assignments |
| Identity | Address | addressId, ownerId, recipient, phone, address fields, default flag |
| Catalog | Category/Brand | id, name, slug/code, status, hierarchy |
| Catalog | Product | productId, name, description, category, brand, status, images |
| Catalog | SKU | skuId/code, productId, attributes, price, currency, status |
| Cart | Cart/CartItem | cartId, customerId, skuId, quantity, timestamps |
| Inventory | Stock | warehouseId, skuId, onHand, reserved, version |
| Inventory | Reservation | reservationId, orderId, skuId, quantity, status, expiresAt |
| Inventory | StockMovement | movementId, skuId, quantity, type, reference, timestamp |
| Order | Order | orderId/code, customerId, status, totals, address snapshot, timestamps |
| Order | OrderItem | product/SKU snapshot, quantity, unit price, line total |
| Payment | Payment | paymentId, orderId, amount, currency, provider, status, providerRef |
| Payment | Refund | refundId, paymentId, amount, reason, status, providerRef |
| Notification | NotificationLog | notificationId, eventId, channel, recipient reference, status, providerRef |
| Platform | OutboxEvent | eventId, aggregate, type, version, payload, status, timestamps |
| Platform | AuditLog | auditId, actor, action, target, delta, timestamp, correlationId |

## 

## 

## **5.2. Sở hữu và nhất quán dữ liệu**

* **DR-001:** Mỗi thực thể PHẢI có một service là nguồn sự thật duy nhất.  
* **DR-002:** Service khác chỉ được dùng API, event hoặc read model đã đồng ý; cấm foreign key và join xuyên database service.  
* **DR-003:** Các bản snapshot trong Order là dữ liệu lịch sử của Order, không bị cập nhật khi Catalog thay đổi.  
* **DR-004:** ID chia sẻ giữa service PHẢI là opaque identifier; ưu tiên UUID/ULID hoặc định dạng không va chạm.  
* **DR-005:** Giao dịch tài chính, biến động tồn kho, lịch sử trạng thái và audit PHẢI giữ lịch sử bất biến; sửa sai bằng bản ghi bù.  
* **DR-006:** Mọi thời điểm lưu trữ PHẢI dùng UTC; chuyển múi giờ chỉ tại biên hiển thị/báo cáo.  
* **DR-007:** Số tiền PHẢI lưu bằng decimal với precision rõ ràng hoặc số nguyên đơn vị nhỏ nhất, kèm currency.  
* **DR-008:** Thao tác cập nhật cạnh tranh PHẢI dùng version/locking/conditional write phù hợp để ngăn lost update.

## **5.3. Kiểm tra và chất lượng dữ liệu**

* **DR-009:** Dữ liệu đầu vào PHẢI được kiểm tra kiểu, độ dài, phạm vi, enum, định dạng và quan hệ nghiệp vụ tại server.  
* **DR-010:** Mọi chuỗi đầu vào PHẢI được xử lý an toàn trước injection và output encoding theo ngữ cảnh.  
* **DR-011:** Email, số điện thoại, slug và mã SKU PHẢI được chuẩn hóa trước khi kiểm tra duy nhất theo quy tắc được công bố.  
* **DR-012:** API không được chấp nhận trường không xác định ở các thao tác nhạy cảm nếu có nguy cơ mass assignment.  
* **DR-013:** Elasticsearch là chỉ mục dẫn xuất, không phải nguồn sự thật; PHẢI có job/reprocess để tái đồng bộ.

## **5.4. Lưu giữ, sao lưu và xóa dữ liệu**

* **DR-014:** Chính sách lưu giữ dữ liệu theo loại phải được phê duyệt trước production; thời hạn cụ thể là TBD.  
* **DR-015:** Dữ liệu giao dịch và audit PHẢI được lưu theo yêu cầu pháp lý/nghiệp vụ áp dụng; không xóa chỉ vì người dùng xóa hồ sơ nếu còn nghĩa vụ lưu giữ.  
* **DR-016:** Khi có yêu cầu xóa/ẩn danh dữ liệu cá nhân hợp lệ, Hệ thống PHẢI xử lý PII nhưng bảo toàn tính toàn vẹn giao dịch theo chính sách.

## **5.5. Hợp đồng sự kiện tối thiểu**

| Event | Producer | Consumer chính | Dữ liệu nghiệp vụ tối thiểu |
| ----- | ----- | ----- | ----- |
| OrderCreated | Order | Payment, Notification | orderId, customerRef, amount, currency, reservation expiry |
| PaymentSucceeded | Payment | Order, Notification | paymentId, orderId, amount, currency, providerRef |
| PaymentFailed | Payment | Order, Notification | paymentId, orderId, failure category |
| OrderConfirmed | Order | Notification | orderId, status, confirmedAt |
| OrderStatusChanged | Order | Notification | orderId, oldStatus, newStatus, occurredAt |
| OrderCancelled | Order | Payment, Notification | orderId, reason, refundRequired |
| RefundCompleted | Payment | Order, Notification | refundId, paymentId, orderId, amount |
| ProductChanged | Catalog | Search indexer/cache | productId, version, change type |

Event payload không được chứa bí mật và chỉ chứa PII tối thiểu cần thiết. Mỗi event phải tuân thủ COM-07 và có chiến lược tương thích phiên bản.

---

# **6\. Yêu cầu phi chức năng**

## **6.1. Hiệu năng và khả năng mở rộng**

* NFR-PERF-001: Ở tải bình thường và dữ liệu kiểm thử chuẩn, 95% API đọc catalog PHẢI phản hồi trong ≤ 500 ms và 99% trong ≤ 1.000 ms, đo tại API Gateway, không tính thời gian tải tệp ảnh từ CDN/object storage.  
* NFR-PERF-002: Ở tải bình thường, 95% API ghi thông thường PHẢI phản hồi trong ≤ 1.000 ms và 99% trong ≤ 2.000 ms, không tính thời gian xử lý bất đồng bộ của bên thứ ba.  
* NFR-PERF-003: API tạo đơn PHẢI phản hồi xác nhận tiếp nhận/kết quả ban đầu trong p95 ≤ 2.000 ms ở tải mục tiêu, không chờ gửi thông báo.  
* NFR-PERF-004: Lời gọi nội bộ Order–Catalog có mục tiêu p95 \< 5 ms trong cùng mạng, không tính thời gian client/gateway; mục tiêu phải được kiểm chứng và điều chỉnh theo hạ tầng thực tế.  
* NFR-PERF-005: Hệ thống PHẢI vượt qua bài kiểm thử 500 request/giây trong tối thiểu 15 phút; mục tiêu nâng cao 1.000 request/giây. Workload mix và dữ liệu kiểm thử phải được ghi trong test plan.  
* NFR-PERF-006: Trong bài kiểm thử tranh chấp 500–1.000 yêu cầu mua SKU có tồn kho giới hạn, số đơn xác nhận không được vượt tồn khả dụng và số lượng tồn không được âm.  
* NFR-PERF-007: Các service stateless PHẢI cho phép chạy nhiều instance; Order, Inventory và Payment PHẢI scale độc lập trong giới hạn kho dữ liệu.  
* NFR-PERF-008: Search, danh sách và export PHẢI giới hạn số bản ghi mỗi request; không cho phép query không giới hạn.

## **6.2. Khả dụng, tin cậy và phục hồi**

* NFR-AVL-001: Mục tiêu khả dụng ban đầu cho API nghiệp vụ là ≥ 99,5% mỗi tháng, không tính bảo trì đã thông báo; SLA production cuối cùng là TBD.  
* NFR-AVL-002: Lỗi Notification Service không được ngăn tạo đơn/thanh toán; thông báo phải xử lý bù sau khi dịch vụ phục hồi.  
* NFR-AVL-003: Hệ thống PHẢI dùng timeout, retry có giới hạn và circuit breaker/bulkhead khi thích hợp để tránh lỗi dây chuyền.  
* NFR-AVL-004: Message broker PHẢI bảo đảm lưu giữ bền vững các sự kiện nghiệp vụ quan trọng cho đến khi được xử lý thành công.  
* NFR-AVL-005: Không được mất đơn, giao dịch thanh toán đã xác nhận hoặc biến động tồn sau khi API báo thành công.  
* NFR-AVL-006: Các tác vụ nền PHẢI có thể tiếp tục sau khi khởi động lại mà không tạo hiệu ứng trùng.  
* NFR-AVL-007: Khi một dịch vụ độc lập tạm thời không khả dụng, chức năng không phụ thuộc trực tiếp NÊN tiếp tục hoạt động hoặc suy giảm có kiểm soát.

## **6.3. Bảo mật**

* NFR-SEC-001: Toàn bộ giao tiếp công khai PHẢI dùng HTTPS.  
* NFR-SEC-002: Mật khẩu PHẢI băm bằng Argon2id, bcrypt hoặc thuật toán được ngành chấp nhận với tham số đủ mạnh tại thời điểm triển khai.  
* NFR-SEC-003: Token, API key, khóa ký, mật khẩu database và credential provider PHẢI được quản lý như secret, xoay vòng được và không xuất hiện trong log.  
* NFR-SEC-004: Hệ thống PHẢI thực thi least privilege cho user, service account, database account và hạ tầng.  
* NFR-SEC-005: Mọi endpoint PHẢI được đánh giá rủi ro theo OWASP API Security Top 10 hiện hành và có biện pháp chống broken object/function level authorization.  
* NFR-SEC-006: Hệ thống PHẢI chống injection, XSS, CSRF trong ngữ cảnh cookie, SSRF, path traversal, insecure deserialization và mass assignment phù hợp bề mặt tấn công.  
* NFR-SEC-007: API công khai PHẢI rate limit; endpoint login, reset password, payment và webhook phải có chính sách riêng.  
* NFR-SEC-008: PII và dữ liệu tài chính nhạy cảm PHẢI được mã hóa khi truyền; mã hóa khi lưu áp dụng theo phân loại dữ liệu và khả năng nền tảng.  
* NFR-SEC-009: Log PHẢI che password, token, authorization header, OTP, full card data và PII không cần thiết.  
* NFR-SEC-010: Upload hình ảnh PHẢI kiểm tra MIME thực, phần mở rộng, dung lượng, tên tệp, nội dung nguy hiểm và lưu ngoài vùng thực thi.  
* NFR-SEC-011: Thư viện và thành phần phụ thuộc PHẢI được kiểm tra lỗ hổng; lỗ hổng nghiêm trọng/cao phải được xử lý hoặc chấp nhận rủi ro trước phát hành.  
* NFR-SEC-012: Hệ thống PHẢI có audit cho đăng nhập bất thường, thay đổi quyền, tồn kho, giá, trạng thái đơn và hoàn tiền.  
* NFR-SEC-013: Webhook payment PHẢI tuân thủ FR-PAY-007 đến FR-PAY-011 và chỉ được truy cập qua đường dẫn/cơ chế được bảo vệ phù hợp.

## **6.4. Khả năng bảo trì và chất lượng mã**

* NFR-MNT-001: Mỗi service PHẢI có repository/module boundary rõ, hướng dẫn build/run, cấu hình mẫu và ownership.  
* NFR-MNT-002: API công khai PHẢI có OpenAPI; gRPC PHẢI có .proto; event PHẢI có schema/version được quản lý.  
* NFR-MNT-003: Thay đổi hợp đồng không tương thích PHẢI tạo version mới hoặc có kế hoạch migration được phê duyệt.  
* NFR-MNT-004: Business logic trọng yếu về tồn kho, tiền, trạng thái và idempotency PHẢI có unit test tự động.  
* NFR-MNT-005: Mã nguồn PHẢI vượt qua lint, unit test, integration/contract test phù hợp và kiểm tra bảo mật trước khi phát hành.  
* NFR-MNT-006: Database migration PHẢI được version hóa và có phương án xử lý khi nâng cấp không thành công.  
* NFR-MNT-007: Cấu hình theo môi trường PHẢI tách khỏi mã; image giống nhau được dùng xuyên các môi trường với cấu hình khác nhau.  
* NFR-MNT-008: Mã nguồn NÊN tuân thủ giới hạn bounded context; không dùng shared database/schema làm cơ chế tích hợp.

## **6.5. Khả năng sử dụng và tương thích**

* NFR-USA-001: Các luồng mua hàng chính PHẢI hoạt động trên kích thước màn hình desktop và mobile mục tiêu.  
* NFR-USA-002: Phản hồi thao tác PHẢI thể hiện trạng thái đang xử lý để giảm gửi lặp; nút tạo đơn/thanh toán phải chống double-click ở UI và idempotent ở server.  
* NFR-USA-003: Nội dung tiền tệ, ngày giờ, số lượng và trạng thái PHẢI trình bày nhất quán.  
* NFR-USA-004: API lỗi PHẢI đủ rõ cho client ánh xạ sang thông báo người dùng nhưng không tiết lộ nội bộ.  
* NFR-COMP-001: API PHẢI duy trì tương thích ngược trong cùng major version đối với trường hợp đã công bố.  
* NFR-COMP-002: Consumer PHẢI bỏ qua trường event chưa biết khi an toàn; producer không được xóa/đổi nghĩa trường bắt buộc trong cùng version.

---

# **7\. Quy tắc nghiệp vụ**

* BR-001: SKU là đơn vị bán và quản lý tồn kho; Product chỉ là thực thể nhóm/mô tả.  
* BR-002: Giá và thông tin hàng hóa trong Order là snapshot, không thay đổi theo Catalog sau khi đặt.  
* BR-003: Thêm sản phẩm vào giỏ không giữ tồn kho.  
* BR-004: Chỉ số available không được âm và đơn xác nhận không được vượt tồn khả dụng.  
* BR-005: Một reservation chỉ được commit hoặc release đúng một lần về mặt nghiệp vụ.  
* BR-006: Đơn chỉ được xác nhận sau khi điều kiện thanh toán của phương thức tương ứng thành công.  
* BR-007: Với thanh toán online, webhook/provider verification là nguồn sự thật; kết quả redirect client chỉ có tính hiển thị.  
* BR-008: Số tiền thanh toán phải bằng tổng tiền được chốt trên đơn, trừ trường hợp partial payment được bổ sung bằng change request.  
* BR-009: Tổng tiền hoàn không vượt tổng tiền đã thu.  
* BR-010: Mọi thao tác tài chính, tồn kho và trạng thái đơn phải idempotent khi được retry.  
* BR-011: Trạng thái đơn chỉ chuyển theo ma trận hợp lệ; không sửa trực tiếp để bỏ qua quy trình Saga.  
* BR-012: Lỗi gửi thông báo không làm đảo ngược giao dịch mua hàng đã thành công.  
* BR-013: Người dùng chỉ truy cập dữ liệu sở hữu của mình, trừ vai trò nhân viên có quyền nghiệp vụ tương ứng.  
* BR-014: Các thay đổi giá, tồn kho, quyền và hoàn tiền phải có audit.  
* BR-015: Xóa dữ liệu nghiệp vụ đã được tham chiếu ưu tiên soft delete/ngừng hoạt động thay vì xóa cứng.

## **7.1. Ma trận chuyển trạng thái đơn tối thiểu**

| Từ trạng thái | Sang trạng thái | Điều kiện/Tác nhân |
| ----- | ----- | ----- |
| PENDING | CONFIRMED | Thanh toán/điều kiện xác nhận thành công và commit tồn kho |
| PENDING | CANCELLED | Thanh toán thất bại/hết hạn, khách hủy hợp lệ hoặc lỗi không thể phục hồi |
| CONFIRMED | SHIPPING | Nhân viên vận hành có quyền, đơn sẵn sàng giao |
| CONFIRMED | CANCELLED | Chỉ khi chính sách cho phép; phải hoàn tiền/hoàn tồn phù hợp |
| SHIPPING | COMPLETED | Xác nhận giao hoàn tất |
| SHIPPING | CANCELLED | Không mặc định; cần chính sách ngoại lệ được phê duyệt |
| COMPLETED | — | Trạng thái kết thúc trong phạm vi v1.0 |
| CANCELLED | — | Trạng thái kết thúc; hoàn tiền có thể tiếp tục ở payment sub-state |

---

# **8\. Kịch bản sử dụng và tiêu chí nghiệm thu**

## **8.1. UC-01 Đăng ký và đăng nhập**

**Tác nhân chính:** Khách vãng lai  
**Tiền điều kiện:** Người dùng chưa có tài khoản hoạt động với cùng định danh.  
**Luồng chính:**

1. Người dùng gửi dữ liệu đăng ký hợp lệ.  
2. Hệ thống tạo tài khoản và thực hiện xác minh nếu được bật.  
3. Người dùng đăng nhập.  
4. Hệ thống xác thực và cấp token.

**Luồng thay thế:** Định danh đã tồn tại; dữ liệu sai; mật khẩu không đạt chính sách; quá giới hạn thử.  
**Hậu điều kiện:** Có tài khoản hợp lệ và phiên đăng nhập, hoặc không có thay đổi ngoài audit/rate-limit state khi thất bại.  
**Truy vết:** FR-IAM-001 đến FR-IAM-013, NFR-SEC-002, NFR-SEC-007.

## **8.2. UC-02 Tìm kiếm và xem sản phẩm**

**Tác nhân chính:** Khách vãng lai/Khách hàng  
**Luồng chính:**

1. Người dùng nhập từ khóa và/hoặc bộ lọc.  
2. Hệ thống trả danh sách phân trang.  
3. Người dùng chọn sản phẩm.  
4. Hệ thống hiển thị thông tin, SKU, giá và chỉ báo tồn.

**Tiêu chí nghiệm thu:** Chỉ sản phẩm/SKU được công bố xuất hiện; bộ lọc và sắp xếp đúng; phân trang ổn định; không dùng Elasticsearch làm nguồn sửa dữ liệu.  
**Truy vết:** FR-CAT-013 đến FR-CAT-018, NFR-PERF-001.

## **8.3. UC-03 Đặt hàng và thanh toán thành công**

**Tác nhân chính:** Khách hàng  
**Tiền điều kiện:** Đã đăng nhập; giỏ có SKU hợp lệ; địa chỉ hợp lệ; đủ tồn.  
**Luồng chính:**

1. Khách hàng xác nhận checkout và gửi yêu cầu cùng Idempotency-Key.  
2. Hệ thống kiểm tra dữ liệu, chụp snapshot và giữ tồn.  
3. Hệ thống tạo đơn PENDING và payment intent.  
4. Khách hàng thanh toán tại provider.  
5. Provider gửi webhook hợp lệ.  
6. Payment ghi nhận thành công và phát PaymentSucceeded.  
7. Order commit tồn kho, chuyển CONFIRMED và phát event.  
8. Notification gửi xác nhận.

**Hậu điều kiện:** Chỉ có một đơn/giao dịch logic; tồn kho trừ đúng; đơn được xác nhận và có đầy đủ lịch sử nghiệp vụ.  
**Truy vết:** FR-ORD-001 đến FR-ORD-015, FR-INV-006 đến FR-INV-015, FR-PAY-001 đến FR-PAY-013, FR-SAGA-001 đến FR-SAGA-010.

## **8.4. UC-04 Thanh toán thất bại hoặc hết hạn**

**Luồng chính:**

1. Đơn và reservation đã được tạo.  
2. Payment báo thất bại hoặc hết thời hạn giữ kho.  
3. Hệ thống đánh dấu giao dịch thất bại/hết hạn.  
4. Order chuyển trạng thái phù hợp.  
5. Reservation được giải phóng đúng một lần.  
6. Khách hàng được thông báo bất đồng bộ.

**Tiêu chí nghiệm thu:** Không trừ tồn vĩnh viễn; retry/event trùng không cộng trả kho nhiều lần; trạng thái cuối hội tụ.  
**Truy vết:** FR-INV-011, FR-INV-013, FR-PAY-006, FR-SAGA-003, FR-NOT-001.

## **8.5. UC-05 Cạnh tranh Flash Sale**

**Tiền điều kiện:** Một SKU có tồn khả dụng xác định, ví dụ 1 đơn vị.  
**Luồng kiểm thử:** Phát đồng thời ít nhất 100 yêu cầu hợp lệ mua SKU đó; bài tải tổng thể thực hiện theo NFR-PERF-005.  
**Tiêu chí nghiệm thu:**

* Không có tồn âm;  
* Tổng số lượng reservation/đơn thành công không vượt tồn ban đầu;  
* Yêu cầu thua cuộc nhận kết quả hết hàng/xung đột rõ ràng;  
* Không có deadlock kéo dài hoặc dữ liệu reservation mồ côi;  
* Event và thao tác retry không tạo đơn/trừ kho trùng.

**Truy vết:** FR-INV-006 đến FR-INV-015, FR-ORD-002, NFR-PERF-005, NFR-PERF-006.

## **8.6. UC-06 Hủy đơn đã thanh toán**

**Tiền điều kiện:** Đơn ở trạng thái cho phép hủy và có payment thành công.  
**Luồng chính:**

1. Tác nhân có quyền gửi yêu cầu hủy kèm lý do.  
2. Hệ thống xác minh trạng thái và ghi nhận Saga hủy.  
3. Payment thực hiện hoàn tiền idempotent.  
4. Inventory hoàn/giải phóng tồn phù hợp.  
5. Order chuyển CANCELLED khi đạt điều kiện.  
6. Notification gửi kết quả.

**Tiêu chí nghiệm thu:** Không hoàn vượt số tiền; không hoàn/trả kho hai lần; lỗi tạm thời có retry; lỗi kéo dài có cảnh báo và khả năng can thiệp.  
**Truy vết:** FR-ORD-016 đến FR-ORD-020, FR-PAY-014 đến FR-PAY-019, FR-SAGA-004 đến FR-SAGA-010.

## **8.7. UC-07 Dịch vụ thông báo bị dừng**

**Luồng kiểm thử:** Dừng Notification Service, tạo và thanh toán đơn thành công, sau đó khởi động lại service.  
**Tiêu chí nghiệm thu:** Đơn vẫn được xác nhận; sự kiện còn bền vững; sau phục hồi thông báo được gửi một lần theo chính sách; không cần sửa dữ liệu thủ công.  
**Truy vết:** FR-NOT-006 đến FR-NOT-008, NFR-AVL-003, NFR-AVL-005, NFR-AVL-007.

## **8.8. Điều kiện nghiệm thu phát hành**

Một bản phát hành được chấp nhận khi:

1. Tất cả yêu cầu bắt buộc trong phạm vi phát hành có test case và kết quả đạt, hoặc có waiver được phê duyệt.  
2. Không còn lỗi Critical/Blocker; lỗi High còn lại phải có đánh giá và chấp nhận rủi ro.  
3. Unit, integration, contract, end-to-end, security và regression test đạt.  
4. Kịch bản Flash Sale chứng minh không oversell và đạt ngưỡng tải được thống nhất.  
5. Kịch bản tắt Notification Service và phục hồi event đạt.  
6. HTTPS, phân quyền, quản lý bí mật và bảo vệ dữ liệu được kiểm chứng.  
7. OpenAPI, event schema và tài liệu hướng dẫn sử dụng được cập nhật.

---

# **9\. Ma trận truy vết yêu cầu**

| Mục tiêu nguồn | Yêu cầu SRS chính | Kịch bản xác minh |
| ----- | ----- | ----- |
| Quản lý người dùng, địa chỉ, RBAC, JWT | FR-IAM-001…019, FR-GW-003…004 | UC-01, security/API test |
| Quản lý catalog, SKU, hình ảnh, tìm kiếm | FR-CAT-001…018 | UC-02, catalog/search test |
| Đặt hàng và vòng đời đơn | FR-CART-001…008, FR-ORD-001…020 | UC-03, UC-04, UC-06 |
| Tồn kho thời gian thực, chống race condition | FR-INV-001…015, NFR-PERF-006 | UC-03, UC-05 |
| Tích hợp thanh toán, webhook, hoàn tiền, đối soát | FR-PAY-001…019 | UC-03, UC-04, UC-06 |
| Event-driven với RabbitMQ/Kafka | COM-07…009, FR-SAGA-001…010 | UC-03, UC-04, UC-07 |
| Gửi email/SMS/push | FR-NOT-001…009 | UC-03, UC-07 |
| Saga và compensating transaction | FR-SAGA-001…010 | UC-04, UC-06 |
| Outbox Pattern | CON-06, FR-ORD-008/015, FR-PAY-012, FR-SAGA-008 | Integration/failure injection test |
| Database per service | CON-02, DR-001…004 | Architecture review, integration test |
| 500–1.000 request/giây | NFR-PERF-005…006 | k6/JMeter load test report |
| Một service lỗi không ảnh hưởng service độc lập | NFR-AVL-002/007 | UC-07, failure test |

---

# **10\. Các vấn đề cần quyết định**

| Mã | Vấn đề | Lựa chọn/Thông tin cần chốt | Chủ trì dự kiến |
| ----- | ----- | ----- | ----- |
| TBD-01 | Message broker | RabbitMQ hay Kafka; topology, retention, partitioning | Kiến trúc sư |
| TBD-02 | Cổng thanh toán | VNPay, MoMo, ZaloPay hay Stripe; sandbox/production | Product/Tài chính |
| TBD-03 | Thời gian giữ kho | Mặc định đề xuất 15 phút; quy tắc gia hạn/hết hạn | Product/Operations |
| TBD-04 | Chính sách hủy | Trạng thái nào khách/nhân viên được hủy; cutoff giao hàng | Product |
| TBD-05 | Thuế và vận chuyển | Công thức, nguồn dữ liệu, miễn phí, vùng giao | Product/Tài chính |
| TBD-06 | SLA sản phẩm | Mức khả dụng và độ trễ chính thức | Product/Kiến trúc sư |
| TBD-07 | Retention | Thời hạn giữ account, order, payment và audit | Legal/Product |
| TBD-08 | Kênh định danh | Email, số điện thoại hoặc cả hai; xác minh bắt buộc | Product/Security |
| TBD-09 | Mobile support | Phiên bản Android/iOS tối thiểu | Product/Mobile |
| TBD-10 | Kho hàng | Một kho hay đa kho trong phiên bản nghiệm thu | Product/Inventory |
| TBD-11 | Chính sách refund | Toàn phần/một phần, quyền phê duyệt, thời hạn | Tài chính/Product |
| TBD-12 | Dữ liệu tải | Quy mô catalog, user, order và workload mix chuẩn | QA/Product |

Không TBD nào được phép làm mơ hồ yêu cầu an toàn dữ liệu, chống oversell, xác thực webhook, idempotency hoặc quyền truy cập. Các nội dung đó là bắt buộc ở mọi lựa chọn thiết kế.

---

# **Phụ lục A. Mẫu phản hồi API**

## **A.1. Phản hồi thành công**

| {  "code": "SUCCESS",  "message": "Request completed successfully",  "data": {},  "correlationId": "01J..."} |
| :---- |

## **A.2. Phản hồi lỗi**

| {  "code": "INVENTORY\_INSUFFICIENT",  "message": "The requested quantity is not available",  "details": \[    {      "field": "items\[0\].quantity",      "reason": "AVAILABLE\_QUANTITY\_EXCEEDED"    }  \],  "correlationId": "01J..."} |
| :---- |

Mẫu trên là quy ước logic; mã HTTP vẫn phải phản ánh đúng kết quả. details không được chứa stack trace, câu SQL, secret hoặc dữ liệu nhạy cảm.

---

# **Phụ lục B. Danh mục mã lỗi tối thiểu**

| HTTP | Mã ứng dụng ví dụ | Ý nghĩa |
| ----- | ----- | ----- |
| 400 | VALIDATION\_ERROR | Dữ liệu đầu vào không hợp lệ |
| 401 | AUTHENTICATION\_REQUIRED | Thiếu hoặc token không hợp lệ |
| 403 | ACCESS\_DENIED | Đã xác thực nhưng không có quyền |
| 404 | RESOURCE\_NOT\_FOUND | Không tìm thấy hoặc không được phép tiết lộ tài nguyên |
| 409 | STATE\_CONFLICT | Xung đột phiên bản/trạng thái |
| 409 | INVENTORY\_INSUFFICIENT | Không đủ tồn kho |
| 409 | IDEMPOTENCY\_KEY\_REUSED | Cùng khóa nhưng payload khác |
| 422 | BUSINESS\_RULE\_VIOLATION | Vi phạm quy tắc nghiệp vụ |
| 429 | RATE\_LIMIT\_EXCEEDED | Vượt giới hạn yêu cầu |
| 500 | INTERNAL\_ERROR | Lỗi nội bộ đã được che thông tin |
| 502/503 | UPSTREAM\_UNAVAILABLE | Dịch vụ phụ thuộc không khả dụng |
| 504 | UPSTREAM\_TIMEOUT | Dịch vụ phụ thuộc quá thời gian |

---

# **Phụ lục C. Chiến lược kiểm thử tối thiểu**

| Loại kiểm thử | Trọng tâm |
| ----- | ----- |
| Unit test | Tính tiền, state machine, reservation, idempotency, chữ ký webhook, refund limit |
| Integration test | Database transaction, Redis locking/atomic operation, broker, Outbox, search index |
| Contract test | REST/gRPC/event compatibility giữa producer và consumer |
| End-to-end test | Đăng ký, catalog, giỏ, đặt hàng, thanh toán, hủy, thông báo |
| Concurrency test | Cùng SKU, cùng idempotency key, webhook/event trùng và sai thứ tự |
| Load test | 500–1.000 request/giây theo workload mix được chốt |
| Resilience test | Kill/restart service, broker/provider timeout, retry, DLQ, phục hồi |
| Security test | AuthN/AuthZ, injection, rate limit, secret exposure, upload, webhook replay |

---

# **Phụ lục D. Tiêu chí chất lượng của SRS**

Tài liệu này được xây dựng để đáp ứng các đặc tính SRS theo IEEE 830:

* **Đúng:** Mỗi yêu cầu bám theo đề tài hoặc được ghi rõ là giả định/TBD.  
* **Không mơ hồ:** Dùng từ khóa bắt buộc và tiêu chí đo được khi có thể.  
* **Đầy đủ:** Bao phủ chức năng, giao diện, dữ liệu, chất lượng, ràng buộc và nghiệm thu.  
* **Nhất quán:** Trạng thái, thuật ngữ, nguồn sự thật và ranh giới service được dùng thống nhất.  
* **Xếp hạng:** Yêu cầu bắt buộc dùng PHẢI; nội dung chưa quyết định được ghi TBD.  
* **Có thể kiểm chứng:** Yêu cầu có mã và gắn với kiểm thử/review/demonstration.  
* **Có thể sửa đổi:** Cấu trúc phân cấp, mã yêu cầu duy nhất và lịch sử phiên bản hỗ trợ kiểm soát thay đổi.  
* **Có thể truy vết:** Ma trận tại mục 9 liên kết yêu cầu nguồn, yêu cầu chi tiết và phương pháp xác minh.

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAloAAACpCAYAAADzwpN0AAA7A0lEQVR4Xu2dh3vT1sOFv7/KTuLsBDIghF323mW0ZY+2v1L2hpZSKNCyN4RN2RuyyN7DibedFih7n8/3OnY8EnBiO7Hj8z7PeSRdybIt29LrK+ne/wMhhBBCCAkI/+deQAghhBBC/ANFixBCCCEkQFC0CCGEEEICBEWLENIt+OReQAghQQBFixBCCCEkQFC0CCGEEEICBEWLEEIIISRAULQIIYQQQgIERYsQQgghJEBQtAghhBBCAgRFiwQxvGGfEEJIaEPRCmkoIoQQQkgwQ9EixM+8e/ceb16/xetXb8Irr9/g/fv3+PSJfwAIIcQORYsQP/Lhwwec/E0Po/5jWObx48eULUIIcYKiRYgfEaJ1apsBTU0Iy9TV1eHt27cULUIIaYaiRYgfEbU54SxaZWVleP36NUWLEEKaoWiR4CbEjtcUrTK8evWKokUIIc1QtIiP8IDqDEWLokUIIc5QtAjxI91BtCytlHkbihYhhLhC0SLEj3QH0fIlFC1CCHGFokWIH+moaB1YOgMNTtM9lQkey7inrKDaMR6d1B9Npjeo0HzwWK492TlzIDStlHsbihYhhLhC0SLEj/hDtK6cueYkWh9xP88kx4vzG2FobMLVO/VoMr9B7zgFCorNqCvTIL9Aj+ys3/Dz8UJY9E/RoLetKzenruV5LO+RdT7HZfrM+YeO6bL8MmydMYCiRQghfoSiRYgf8VW0ovvPk9NStHQazP7lFhrUz6CM6YfesSo5T319pxz2S4xwPD4hbTK02Sfw+8MXcjoiOkkOJ/xS4vI8FeVGREb1keM3amy1XycLXyMjKUqOs0aLEEL8S/CIFvfLpBvgq2iNX3pCTgvR0j3YiRX7c5CdU42cPLVDtAxF5+Xwc6J1Yc1o5BaXwOT0HLMH90BBqRHRyhQ5bRetA9bHxEUo5DhFixBC/EvwiBYh3QBfRWvhhAGIS89EpMJ26nBi/15IjlVizeFKD9E68PMsxMUky3EhWk1Nb6BUKHHi/j9osnyEQtEiYiIb543DsD5JGNAzDgazq2gVndoGZWQ0EmOiKFqEEOJHKFqEdJDLly+7F3VYtPwdXY0aQ+f84VEe6FC0CCHEFYoWIR1EiJZCocDZs2cdZcEiWrfu6zzKOiMULUIIcYWiRUgbVFVV4datW21m8+bNUrTsuXfvXtCIVleFokUIIa5QtAhxQ6PRoLS01L3YA3uNlkjv3r1lGUWLokUIIc5QtAhx48aNG+5FrSJEKzMzE+/evXOUUbQoWoQQ4gxFixAnXr58Ca1W617cKtXV1e5FPomWQhnZPP4MRVrP+Z/LsZUzZM3apSrPeW1l2opzHmW+hqJFCCGuULQIceLff/+FyWRyL/Yav4qW5R0e5jQ45heUWmzjlvewmN4hJ7/lgvfYniNc1lWUVwmjWYx/Qq36Je4/rMWd+zVyXk1pLfSNT9GgfSen66r0KCx7LMfNumcoqXzisq72hKJFCCGuULQIcSKYREupsDUi2tT0FqPn7pXjCcpkNDVUoMCtxqvywSVE95oox6MGLJbD8kt7ZP+HCUOXyOld30+CEC9V31ly+vtdBVg9qrdjHbrKCtTqP8rxRQeKXNbvbShahBDiCkWLECeCQrQsT1EsREr/L/qlxMNSm4OJP+zF6dPXcfrMbSlaxlYeb1bXIH36IfSesty2rDVCtIbMtXXZ06RvQmneRWQ9eimnhWj17RnreHzx3wdwsvlx90ufeqzfm1C0CCHEFYoWIU50pWhdWD8ZKlW0oyYrKjIKAzPSYbIAY3sq0D89CeO/P96qaEVYl42MUOJ69Uf8NG4AYpPSrY+PdBUtuVyGY1yIlkVnQELPXohR2fo6FOvJ6JmAeqPn6/MmFC1Cgg3+FrsaihYhTnSlaHWHULQIIcQVihYhTlC0fAtFixBCXKFoEeIERcu3ULQIIcSVdomW2HmKA4looDHcIt73x48f3TcJ6WZQtHwLRYsQQlzxWrTEjvNOlsljxxouOf6rGi9evOABpJtD0fItFK3ORWznDx8+yO9dOIZ/fkko0C7RunGipYHEcMuhjVX4559/+MPu5lC0fAtFq3MR37ebp/4N24jvGvfJJNjxWrTEl/n6ca3HjjVccmBdhTwAi3+PpPtC0fIt5eXleP36NUWrExDbWIiGxeL5OYRLjEYj3r59y+8bCWooWl6GohUe+EO0diyqw8G1mrBMVVUV3rx5wwNfJyD2yaJvznAWLY1Gw+8bCXooWl6GohUe+Cpa4vthsR75RIfTJSUlYZWKigro9Xp58wgJPBQtoLGxkaJFgh6Klpfxh2iJA7C/QgKDr6IldvhCNMQpHXEQFMNwiv00Dg98gYeiRdEioQFFy8tQtMIDX0XLjtjxi99MOIUHu85FbHOKFkWLBD9dJlr5Z3Y3j3/EwO8vyvG0uOZOdZtjKLuG1X+33rltYlI/j7JAxh+iNSJRAbMUJSOUqmGYl6mAQtFWYqCyDpMSYqGMTpRylaxSID46gqIVQPwlWoQEGooWRYuEBl0mWk2W17hY/Qnq/FtSLJrMLxHR9yeYDK+wY8sfqKp9JkXrx0OPcP5Gjcfj+/RNQk1zx7fX71VizfpdsvNdi9aIq1lnrI+p8niMLwmEaMnaqcozWLAn21FTpXm41zGuUsbKoaGhEKWVDzB41Q3WaAUYihYJFbwRLUujFstXb8PNh97tu0ePmetR5k3OHTqE3Xv/9ihvK1c2z0WD2bO8vaFokVCg60TLGqVqIL7uE4eKc5twftuPyKt5g0FTViM3pxYX9m+Bzl6jZX6DxN4THI+7u3c1mkwvkT5po5yeufaCHEap+kJfdLp5uY9IG7HI4zk7mh1L87F//3788ccf2LFjR4cyzCpaK1euwqqVK6BQDUFDQwMaCo9hzh+3bOPWVN3c6RiPUkRg3NhxUCmUcjr3+hkk9x4MtVrtsW6RM2fOuH9spJ1QtEio4I1oGfJP2cZNz3Dq0TNU1r/Bzb/vyLK8O/mwOC17+XoFIlWDUVfWAHNzmdZkG16/dBt1uo+oLyrEpdtVjnKRoSnRLs9ZXVSN85fz5Hh5biEu36u2vZbGf3H1RokcP7EgA1XNf5Tz7+VDrX8vx02NTTh3pRDnHv7jss62QtEioUCXitbIaAX6TVsnxxVWmRA/+kk/20XJ9dRhbOpXjvIMlQKDBw2VNWHiMXbRilUkO4kWkDhwusdzdjRdWaPlnNo7e7Dhttp91Q6ysrJw+/Zt9+IQpnN3oBQt0nEC+11VqVSydwo73oqWwfgONQ9OI7+8DhExg2X5gB5Rctg3KRJNlg9QRiZbh++tfwAHY/tEFRqa13m14o28XEGMPyp5jCa9GaWNNimSsbyDIr7lT7DIg/x/YDG/xZE7FpiNz1Cus5XfuVqAJoMR4xYfdIhW6fEVaDQD93YvkWcjogYthcX4HLuuaDzeS2uhaJFQoEtFS5d3AmcLXsnxjPHL5PCr1Fhs/m0/Zk0cL0Wr98g5mDIiE1nZz+R8Q10Dhi/cJ8fv7F2HP7NfIrnvOOze+ReW7rwnRWv9tkOYOSQNpx62fn1XR9LlomXWYtp3S9ArKQYV+s+fOvz111/di4iXULRIsCJES/y5nDhxopz2VrS2bNmJv47chrm+DtN32GqalKrhcrhz7kiUV9dj0fZ7clrUaLmK1ksoosa0rNNdtJqsoqVq+RMsziSM7NMDs75bhNWHS1xEa/2cCZgzewHSxv3oEK1v+qXaHmd+ikc1ZqRO2GQdf43dl7071lC0SCjQpaLlr9hrtESca7T8GX+IlnvtlC8hgcFb0dq9ezfi4uKQlJTEMJ0S9xtmdDqdV6JlH3cWrYn9e6C4/jmilKJG6z0irMPGhqeyRuvmb1Px5xUN9vwwVtZo9bH+uWtsfIG53262StArrDxWjooK2x9fkUPLpuJ8thmnf18JY70WY74/DHVVPf63O9u6/DvsOFeNUvVHRKaOQfWD80geNh/3t07C+uPl0OacQm7lcywcnm59HW8w5487OPzXHuhNnzzeS2uhaJFQoFuIVo3aVismY3Ia92MoWuGBN6I1ZcoU9yJCAo69Rqtv375y2psarc+lvMS179qq+pZ9Z3WlyWVeSbHOcT1XY7UZWv1Hl/kVpTqoNe9s0+a38houg/aNnK61rkvvdOG7xWSrEVPX2K7DqirTQWP4YJtnsQmWYsAKl/W3FYoWCQW6hWh1RvwhWmKn6K+QwOCNaIlW0AnpbGJjY12mfRWtYE1BxUuPsrZC0SKhAEXLy/hDtEjw441oPXnyxL2IkE6nu4pWe0LRIqEARcvLULTCA4oWCRUoWhQtEhpQtLxMdxItsVN6//697JdOROyowiHivYp+CD/3GVK0SLDSkeYdunsoWiQUoGh5me4kWuI9aOrfh2Wythvw/Plz+X1ubedM0SLBiv1i+AULFsjpgIqW5S32/Hncs/wzuXWz3KMs0KFokVCgXaJ147gOJ37Vh2UObqiUd/uJ7RDKiB3S69evPXZY4ZKzu0zQarWyRq81KFqB5/79+xg3bhzGjBnDtCNKpdKleQez2Rwg0foARco0l7LSgjrHHYHVtS9RUVSPO3eaxcryHnrdC1RUNMlps/ENHmTXOR5bXPGklefwTyhaJBTwWrTEF/m///6DWq1GWVmZvPMqXFJeXi7f99OnTylaIR4hWuKzFKLV2s6ZohVYxG9IHBi7gsePH3c4wYC9RktE7IsDWaP19aCemLr0hBw/v3I8isvN2DSjP65UWCUsIgrlFSZ8nZkMo5i/YTaMumeIVaTg0aU9mLUmC5UVRliswhYR1QvlRVWIjLe1SO/vULRIKNAu0RLXt4jTLmJnKQ424RLxfsX7Ftf4hDric3z1KjBtjYVCKFpdS2JiontRp+HeFl17EgwI0Tpx4oRjOpCiJaNuwJxN1xGvUqJXrwyZtcfqoRrxm22+pg7zdhYhJb63nI63itY3aQoYml9P7bX9SEq1Pa5X736e6/dDKFokFPBatJwRX+pwS3dBvBeKVtuiVVRU1OZpRTsUrY4zf/5896JOw12e2pNgJJCitWPnOWgrajHxf8dw4scRUBuA7Kzdcp5DtKz5rk8UHlTZGiYVopV7+nfMWnUWTZaPMFveISJOSNgH3KrxfA5/hKJFQoEOiRYJHXJyclymKVqfF62MjAz3Ig8oWh1n4cKF7kWdhrs8JUcqMH7USCgi06Apv40emcMwetQILN933TrfjJikNHz77RQs3nTMfVVBQSBFS3Sd8+BhrWO6oUoHk9mzWxyjoZXGRXXP8DC73jGdkxe4m6goWuTLdP13g6LVTXn06JEcius5IiMj5QXgAoqWCStXrsSECRMwevRojBo1SkYIQJNYgASUrhQtTfYBpA6eIn8TpmbREsLVL0EpRWveH5fktCJlKrZPjA/rGq2WeMpVMIWiRUIBilY3Zd26dVK2nO9S2rt3rx9Eq2XH++vGrVi26hc8yNPI6Z27Trosa9Y9xb5t23E/gP9o25sv1Wh9Cbuwko7RlaL148AE6KxWMj4lzlGjlRStRKnaKEXL9jtRIq9Gh5k9bBJG0QruULRIKEDR6uY4i1Zubq7PorV9/ngsP2HrjFYRnW7b2WWfgNYMKCNVLcuaX+Gb7Y9cHlueXYCTZ3PkePGNC5i85hhMzfPuXLpuW+bOVTk06f6Tw7r8+2gwAVnHzqO42naaQq2zdWib87DGZf3eRIjWmjVrMG3aNEycOFHWbHmTsWPHyqG42+tLiAOgEDlx80i45UvtzHWlaFkM9Rg//TuU1mkdomUxGxETEetSoyWizj2C66UNcjz7Ybb7qoICihZFi4QGFK1ujhAs5zu9fBOtT4hJmoTI5IFyWhGdBo3mJX7/NlNOO4tW/cMrjgNAZZUFJuv4vcrXaDL8i6h+P8FYXYEFe/Lk/B7RsXLYNykKBnUjKnSfsDBTgbP5L5AUEe1Y55Y5I1Cn/4j0sT/J6dEL9rXyGj8fX2u0voRY58G1Gtw59yQsI0T0c7LVpaJlqsOQ4aMwqFc8irQWjB82RIpU3d29qK7Mwdqjt1xqsaaPG4rMzEwcu1rgvqqggKJF0SKhAUWrm2OvzWpoaJDTvohW5d2/seuyBpunpaJc+wmKqETs35+FW7lGOd9ZtDSFdx23eW+f2RM5GqB/SgwWLFkGRcTAFtGyildk8gBMmTpDRsjc97sfoueozUgaOBcZMw6hyfQcSf3HY+qIAcire4P5w5JgNL1Bgab9148EWrSEZJzaZvB43nCJ+J59rhmUrhSt3IMLMWnGPKTHR0Jt9Lyz8HMJRihaFC0SGlC0ujGiYVLnU4ciz54967BoKZRxcmjRNSFu1GrHqUN7XE4dWhNpfb7rd6vRPzoC+VqraM09iLybN6BUpKBJZ0FU5mwUVLzC8LQY3MtVY94qWwOJQ1Ki5LDk7/1yWHHtGErKzZg8IBFXS15YH2uS78X99XkTX0Tr1KlTWL58uXuxC+L0WTiLlmjcV3zv2tq2XSla7vLUngQjFC3rHzqNhqJFgh6KVjdl7ty58qDnLFlXrlzxqUarI6msMDtNf4TW2DLPbHwNg8k2btQ8dZQbtJ4t11dXWg941qFG+1ZOR6bN8VjGm/giWoIvHXTFesNZtESvEeL71da27UrRcr+erD0JRsQ2FqJVUfgalWEacXMKRYsEOxStbo4QrF9++cUx3dmiFYjkFFo8yryNEK2srCwcPXoUhw8fxpEjR2TWr1+Pfv36OW25thE9BbQFRSt4Rau7IbaxOE2r0+lQWVkpt31paWlYRLzX6upq2ZPDlxoYJqSroWh1c6qqqlymu4No+ZIv1WgNHjzYvciDz3XRQ9GiaHUm4vSh2N7iJgTxByCcIi6DEKIZ6v3Pku4PRSvMoGh9XrREJ+Kfu5hbQNFqOxStzkdsayEb4Zi2vmeEBBMdEi3x5Q63dBfEe6FotS1a3nQq/bn5/hKtdT/9iKkzFiC3uvXP6vr+NSisf+9R3pHMGdIL5lbKOxKKFiGEuNIu0RI7T3H7ujiYhFvE+xb/oEIdilbwi5b+wV7UGUSjrJ8wJLb1uysv7ViC/Np3HuUdyeQUBUWLEEIChNeiJXacd7JMHjvWcMmJX9XyDp+2DiChAkUr+EUrQ9kiVwXn98nGXqOSv5LTG6b1xd3ylzi65mspWgOmrZflP49KhrHoCjKn/eGyrtxsvRz2mn8Z2vwbqNEBlTezsO6SHj1U8TCYP2FYfOeJ1ooVK9yLCCGkW+O1aInanJsnbDvtcMyhjdX4559/Qr5WSxwARTtHFQXhmZO/GWSjmuKW/dZkIBhEa1pCi2hd37tGNmvRd9IqOR2rSJBDe43WiFk/4+dla2WEaK2+9sJlXcnRKqzdegQxX22SoiXKTLWlmLGnHCmjFsvpzqzRevDggXsRIYR0a9olWtePB0/nwJ2dA+sq5AH2c92LhApCMoxGI2pqalBRUSHb2wqHiPdaX18vhVl8jq3JQDCIVlPTB/QZ+R02r9uItCHzZJldtM5tW4yZSzZj2rDeUrQSo+Ox/Y+DGDtiSquiFRmVhqP7D0HVY6KHaE3pm4QNvx2Wff4FWrROnDiBn376yaWMEELCAYqWl+kuoiUOgOKzFLVaz58/l7eFh0vE7eAvXryQdxW6i4Cd4BAtwGL+YJXh1i92N5tcy01u0+4xmUVXRa13V9TWc3Q0hYWF1mGTY3uLiO9ZqNcEE0JIR6FoeZnuIlp27MIVjmlLsgTBIlqhmrZqtAghJFyhaHkZf4iWe/9pvoQEBoqWb6FoEUKIKxQtL0PRCg8oWr6lM0RLnP4NlfiyvyCEdA86WbQ+Ys3+Yjn+1/IFrcx/gXJDy3TJmS04mPsKvROiW1nWKZpG1BhFu0OtzPNT/CFaIxIVMEtRMkKpGoZ5mS0dPnsmBirrMCU5HsoIlZSr6AgFEmMiviha4rMiHYOi5Vs6Q7Tc/3QEc8T3gRAS3nSqaFmML6FIHi3H4xQtt7BXldkPTC9QWPcMNeqXcrq+7qlsQyg1umXZ8lKDvN3deb0bvh2OxEn75XhD/VOUOS2jMXxAaanv7X8FQrTkzrjyDBbsyXbsmDUP9zrGVcpYOTQ0FKK0Mhv9f7romPc5Vq1a5V5EvMQb0RId+AouX77sNsd/onVi926PslBIZ4iWXlOPwqISGM2eYuNt6mtrPcoCEYoWIaRTRash/yyGjRgKi+kVBg8ZJct6x0WgqLwJqiTRIOML7L9UgnN7f8Pgqb8ga8M3uFbTIlprvxuOh9nVGN0v2WW9iUMWI00V2bw+FXKyK5AeHQWDdXrA7J14eL8Q/WYf9Xg97Ym/RMtRY+WNaCmikf0gGzMGp1qF04LVMwdj+e9Zcl5brF27FkVFRe7FxEu8Ea3t27fLoRCtiIgI1FoP2nb8JVqTMyI9/lC0J18nKWFqpTzQ6QzROrlmOK5cvohRvaJxPqfaQ268yfbNWzzKfI2uKg9qg2sZRSu4Ecc1e28nYhhO8fXMRzhuO/t7be+261TRWj5AYf0X+hwbxsfDbHwMjeUT+kxZI+ctHBoHrfkFyvW2ZRWRySg9usxJtD4hQtEDe/edlHGs1/waBy4W4+65Pag12kRLlDfc+B277z7H1N2VcjouovWuTLyNv0SrIzVazqm7uxdrb6rdV038hDeiJUhISMDWrVsd4iyES7TP5W/RGhCllNMJyhjc+mUybtd8QOmZLWiyvEVMT+ufFct7xEXGoD7/Forq36L0wjYpWHbR0lcVYfLS4zDWVOCb9Zetr1UJnUU8x2vcr/sAS2MdGk2ez9/R5OTkQK1WQ6fTBSwnVg2T7cCJRFr/jJxaOwU/bNmLBFUM9NYypfXzWLZohvxcFswYg5+OPUJ94VVs/f03xEQmWP/wnbH+0UmTj0/PHIKVS6ahWGNbn8iBVdMwZf5SJMVGwajXoN+0ZdZhI0bO+wUDrPuiVRt/Ra+EKDQ8uoDEHhmYOSYTWdlVOPnHGvy++0/oDFqMnLkY0ydPhV6v93j93uTp06fuXzniZ8Rv9caJfz2+w+GQyiJb8z7ieNaRP0Vi29278NRjveEQg+4jnjx50i7Z6lTRioywSVBk9GA5XHPzBZKj4mG07vgj4/tB1GhdyP0H+df/RsbEjQ7Ryoi3HTRmD0+Xjzt39JJjnetHJ6K25onMdxuvOESrf2IU9Nb1Dlx8Dk2mV+g1Y5/H62lPuly0zA0YMnI8kuOiUC82GAkI3oqWQNRo2UUrKipK7rj8L1q27/OUVNsfhcxJq9E/KQaaolz0GToFM2fNsWa+FC3xG9HVVEFjbhGtO7+Ow53aD1Yh+4DYgfOhjIhyPMeAaRuwemxvj+f2JY8ePZLb7/Hjx3JnFIicWD1cPodIlCIOiqhEOZ59dAnO5TdYRStBTitix8FkbIBi+BbrtAHDh3yF1PgI2zxVuhyeLdPJ4dYbdU7rjMDoUaMxpG8K7tWbMC0zFmd/7ItqrfVxCRly3qihfaEpuIh15/Khr76Pr7dfQ2PpXdTpm9eRmI6HVZoObwetVouUlBT3rxzxI6LmNZxFS3xP2+oh40uIbRfOotXY2Ci3nbd0qmh1RuyiZY+9RsvX+EO03GunfAkJDO0VLSFYoibLTqBEa1qzaJ3a+C3+fmTbwSVHR+Lu3UJsPFrqIVr7vuuDvWfzYDY8Q+pXs/HXTxOx64rORbS2LRiOpAmufSP6ms46dVhfW4WlE3tj3dF7mNwzCnU6E2b3jUK19d+VEC3xG1HEjofF1ChF6/c5/WTZkpGJtnlW0RLDc+UGOdx6s97x2xqRqkK9zoyy4hJbmbYUyVO3y3GlMl6exq8tfQRt4SWsO28Vy7qHmLr9KvRV2ShWG2A0WawxY3FfBV698+3UIa+3DByi79pwFi2NRoM3b964b5YvIn7b4o7acBatqqoque283c91O9Ha9etOl+lj9x57LNORULTCg/aIVmv99vlLtNqM5YPLdG1t2zs7de0TObSY3qFB69kC/MMT21BQ+8aj3Jd0hmiV51zGyZMn8aiszvZ7MBvxx9YtuJFXJadPnjxtG2Zdk/NOXs6Xw41btlsFrVxeRH/y1Dm5TKXWLIf5tTbhssWMk0cP4NKtXEeZzn7hvb4ee3ZsRdbfd2BqrEJOZaNVZtW4UVAj52/bshnXitTY/cc2nL1y1+drtMT29BaxzcMl/oCipZE9hLQXsf3DXbTEDVFhLVqBCkUrPGiPaLVGwEXLj5m55qxHma/pDNFy/y0Ec3wVLXEK8UuIfbN4nnBLRy5KdoaiRdHqSChaAYw/RIsEP+EkWoEIRcs14vvgC18SLdlBvOEjTIZPYRnRl2ZHL+imaFG0OhKKVgBD0QoPKFq+pTXRunjxotMW8h33W66DOd7uiNvic6Il1i129hZ5F2l4Rtwl2tELuilaFK2OhKIVwFC0wgOKlm9xFi3R1IP9rkzSMT4nWmKfLGQhnEXLfkG3twc8ZzoqWoc2b3SMT5uzzWO+c6ZPXSyHmxZNQ3rvSdi+dC4MZs/lWouxpgxbLvje2HZr6QzRsjTqMXXqLFt+8K4dy/unDkA05ZQ6dKFL+cLpMzBqzFQ5XnHhL0f5n9fF5/cBUydMwjdztzjKLYbnGDVyDCZ/vQB1us/0GqOtx5oTDZ7lnwlFK4ChaIUHwSpaFt0/sq2u6PSxHvPaypnNtp18Z0aIljiAidfq3KWUr6fQwhWK1ucjbrNvzwHPmY6K1oqxSVj4R44cVyTPlEOL5SPMlk9yfFqS0ipT4uD+CWazKPuEUfN2ybuIzbLcth6TqfnGFuvjHOPmN4gZulIua7Eua2+02GyfL8qdH9vBdIZometqoDG0vF+LRWwP2+t2fj8u483bLbLXdEdZ7dUN2H/vP8f0gkFptvGqO8gcuxYbJg9AvZu8WvSPMWieVcjMb6GIzJTrdJ7fJD4v8VyNNfjxYB1MTp/LlxJw0bpxXIcLe8zBmb9aKfNjDq6vtH4wZp8uviTBT7CK1vb5trbn7Fn7/RJMnWUTqY1Lv8efq3/E7nXLUaizzjc+w7SVf2P69NlyfuGtqxgxcjwOXKmHobYGk8ZNwsrf/X8hvIi9RkuczhkyZIhLjdakSZPcthb5EqJNLfGdFKxZs8ZlHkWr60Sr7OFVzNmWLUVLW1mB3Jo3MFn/DA2evcOlVwZF7BA5HLtI1NQAcwbFyG7lIuJsbUI2aN+iqFTcHfwJPUVTLpZ3iBm2Ss7TFV3F8uvPEBPZU073i7c19TJANPJtfolh33W8aZbOEi3b7z9CymFUD9u2SOhnq5lSRCZIsRTjY/rEyeGZTWKf5ipaYnpQWhwWrj5ke3xUDNYcq8PwtASkqazbxipVsdbnKFW/dTxGiFbm1+tw/cxRpI5ZhYbrzbVdegMadGZEDPnF+tzvpGjNWH8N+tI7WLCrzOM9tJaAi5Zorbiurg4lJSWym5dwSXFxsXzf4v1TtNqDd1/CYCJYRUtEtHh+p8K6M6kvhtb671D8Q5y1NR8TBti7pHqP5EErsG/RBNn9lNhpWYyvkDrhZ8c6InvMkY+rvfSrx/r9EfdrtLZt2+Zy6vDAgQOOceIdv/zyixyqVCrExcU5ti1Fq+tES2sGdkxPlaKVf9LWu0lT00fr9HgvROsdBkxsOc2VGh+JLTtPI0X0gNKKaCWNs61/yYQMmK2f9ZgFtsa3+0xc6fHavE1niZZzjVZM5iQ5TBs2Tw5Fg9z1t44jNn0MBvVOkGWti5YthrKrqDa+hWLwBqgyJiJm9Db8PsMmrCK94yMc444areZpF9GybsPck79b90sxjhqtJtNjTFhxzeM5W0tARUus8O3bt/IuD9HasWikMVwi3q943+L9e7thSWgSzKIl8lWcAqWn1uLs2VsyDwqeOokWMCVFheTUyXJc7LQM6los3v7AMT9hwq+Ox7qv2x9xFy13fv75Z/ci8gVOnDgh72AUomWvIdy8eTNFq6lrRUsIgRAtg1qDmyXPUZf/EJOWncKy/hEo19hOh7UuWtY/PDG2Wqrrt+qgsv45Eu3jpcWIRonfQ5E0xnowf+MQrR4qm4QkNQ9DSbTyioyoKDehqu5lq6L184SBskuwUX2SZJldtBSxfVvWZXmDKvVr3MrajdLiIvxwpBo7vx+HG2UvUHjhT+iraqA1fkJKfEtjzO6iZSq+gHrDJ5z5bYkUrcLa17h7ZAvMDUEmWgKxUhHxAw+3eLtBSWgTrKJ1fMMcKJVKxGeOl9MK63iP+GgYrePOomUxPkepxjZu22kBo9MiMSAjGRN/PIpFmQrEJfaU63J/Dn/kS6KVnp7uXkS8xFm0YmNj5X7Ja9EytHwnBw2e6znfTxHfK5HU8es85jkn46cW+fcl3orWsGHD3Is6LFrdIe0Rra+//tpl2lvR6q4JuGgR0t0JVtEKlXxJtObMmeNeRLxEiFZ0dLSs3RK0R7QqT6/Dg3pxGuc9FKoUR3lxeYtoPMyul0NdvQmllbYeNUoL6lBc1tK7Rk52FbKO3pHj2joLahtbrosRUUT3ksN1w2yncSqrnyEnt1aOa+r/cZxS690sWpWV/8hhQ1XLb6aq6l/k51S7rLeteCtadkH977//HGUULe9ES3QzJrbd7du35TRFi6JFiE9QtHzLl0RLkJiYKK87KigoYNoR0R6Z87TowDs3V3QT5Pk5uOfrfinoMexbnFg6DsdWj0aT+T8MmHdczhOdcu/84Ws5Li5a7jFhF1zu0jLqUWsAYiJs/W1GKBKRe2YXzGKe6SXixm52LCsOyOm90hAVI2TuPSJUtjvEpicq0WD9WVWeWIxT+W+kaEVE2E4XpStj5PNe2r0MD6veWGUt1eP1t5W8vDxUVFTIvueqq6vbjPMdsKLGTRDuopWfny+3nfu2ck9kZKTLtmuyroCiRdEipMNQtHyLN6JFOsaYMWNcpttTo9UzOgIrJqQgNT4JNTnXoMnPwsQVx3Dy1FWcyroplxmWkYRDN/QovHYeKmW0vPB65KxVeHDrPop1wLiMWJy9VoD0ib/iwA9D5WNFzl6pcTyPvUbLUGeVH8N7qFK+l9PRiuZT1TX3seJwrbwTbWjPBBgt4jHpjnU16D5A1XOAx+tvK+2t0RJZsmSJLAt30WpvjZbI4sWLWaNF0SLENyhavoWiFTjEqUNx0LM39+C9aL2Hos/3aMi/j3m7C2DW/YNi9VtExveR80/lv0LZrasw6/9D31m/4VbFKxxfOwcFDe9wsfAF8k9swaNGq+j1z0DOzQeo0nxAQ3EBzmQ/hkFtQL2h5bnsojV/cBLMTS2idX3daBy8YcSoHtGoNnySNVpmrREpE7fj+1E95cXkJ/celMsGUrSeP3/uKKNotU+0RA2qgKJF0SLEJyhavoWiFRjEd9L5FI6IuBPaO9HyT24cPweTGegVH+0xr6virWi11vBroERrxYzhSO/VG9dqPee1Fefr5joj7REtcde9MwERLU2tvMFHNGFzs/SV5/zWYrBgwPSdjukFQzM8lwlAKFqE+AhFy7dQtPzP+fPnZTMzzncdbt26tR01Wv6Jtt6CC2euQmv0vhXtQMdb0WqNQIlWdIJr48LqsmrojLbxRyVNuH3xGq6cyJLToqsYdaUeDx7abhgw6V/g2PGrjsceO3rdY/3+SHtEy51AidagpXfleGRMDxRml+Pe39fQaN1udQVlyCuztQx/5YGtS6LD+04h72EliitfyOn8W/cxd0iLaJ06+1AOjQ0m6/f1Aw4d9a7pBm9C0SLERyhavqW8vFzuvL3dAZHPI2RKbFOBEK2///7bZV5nilYwJhhFS1NZhqTYSNmn4dTeUais/hd5Fw7grweP0WPkD3IZs8aI4vq3+H5wnJxWJvSDsfwhak22dWgryrHw12uosT42PsL7mwO8TTCKVvLQhVi3ciV+3FOJkUm2Gy+Kzv1pm296hsjB6zAgNlbeOLH+lBpNZgvGLL8hrz8Uy8gaLdGF0aCFcrvNHNoThac34vBt252t/gpFixAf8Ydo7f6fGqf/MIZlxA6IouU/xHdR7NAFojseZyhawSla9sSP2IoUZQRKSvUyGuNHDJrV0iND8vT96DfZdsemEK2ay7sd8ypvn8SmI/nNjzV6rNvXBKNo2Wu0ROyidWWnvVHWD1DEjEHhxQO4cP6ArQ/IZtGKVNhEVIiWRdeEzJlbHNtciNaVMtcmSHwNRYsQH/FVtESn4+KaBnEAqKmp8bhVujunvr5e9gfKDqT9R2vXFtmhaAWnaMVG2k7v5jdAtloeG6lEdHy6bA7DWbSOLB7pGBeiJYZJMRFIjo2R43PHDUZijxRsy6r0eA5fEyqiJbs1UopT5ipUaN/Lsl6pzdutWbRu71uPiCiVddvZ+oI8vH4pevTsgSGz/6BoERKM+Cpa4ocnOlQW1ymJnZG4yykcIt6rOHDZdz7e7oDI56FofT7BKFqhkKATLX+lE34LFC1CfMRX0bJj77YpnOL8nol/oGh9PhStjqXbilYnhKJFiI/4S7QI8QefEy2xkxeycGanwRpj2OXsLiN0Ol27DnjOULQoWh0JRYsQH6FokWDiS6IldvaiVqe0tBSFhYVhk6KiInk3pugORpyq7wgULYpWR0LRIsRHKFokmPiSaInTh2KHH07XA4rYrwkUN154e7Bzh6JF0epIKFqE+AhFiwQTnxMtO3bhEsNwinjP9vffEShaFK2OhKJFiI9QtEgw4Y1okY5B0aJodSQULUJ8hKJFggmKVuAQonVkg1bmcJhFvGe9Xi9lob0IuRDb7q+lDWG57Q6v16K2tpaiRUhHoWiRYIKiFTjEgVI0sCsuqi8pKUFxcXFYRLxXUSMj9nXiRgJvZcEZse3EfrKqqspj/d059m1nNBrb1TAzRYsQJyhaJJigaAUOIRj2xoVFDU04RZwy9OVGgnDeduI92wXV2+1H0SLECYoWCSYoWoHF+cL6cIq3gvAluO28g6JFiBMULRJMULQICX0oWoQ4QdEiwQRFi5DQh6JFiBMUreBHXB8RKhGnGnyBokVI6EPRIsQJilbwY7FYQibtuTOpNShahIQ+FC1CnKBoBT/uMhPMoWgRQihahDjhT9Gy39EULuksTq4ZLiXmzzVzsWr/NQ+58SbFebkeZb5GU34PaoNrma+idfnyZfciQkiIQdEixAl/iNaHDx/k9TmiUb9wytu3b31qm8db7KJlsZgRoYxH9vE1GDXrB6iSMmS5UpWIEf16IKV3BvqmJWL3nTrU55/HrNnfQJXQFw13d0OhSpfL9umZiNEDk9BgapGjO4fWILbPMMTE94BZ34AR83+BxWxEv6nLMHtwOsZMnYmomHhoCy+hX3oP9EpU4HZZA35dMgPzFi2C3rpsQp+vkBTf02fRysjIcC8ihIQYFC1CnPBVtIRknNpmgNHwKSzz5MkTKZpfQnRhkZub26EcWzlMdh8iolLEQBERJ8dv752DC/lqKBUJcloROxZ6bS0UwzZZp3XYsGoZxvZR2eap0uQwq6hRDrder3WsM1qhwvZt27F69mjcrdFjaGI07m2fhGzruLLvLDnvt7VzUZ93DmvO5KCx7Ba+/v0q6opuobpRrEOH+AGTUd2gRVFRkcfr9yZ//fUXRo4c6b7ZCCEhCEWLECd8FS1RkyVEy70j0nBJTU2NrN1qq1br6dOnHerI1hl7jdahFZMw+5ez6B8VAZN1euO4BBRrLFK0xHxF7HhYTI1QDN+Crd/Yars2Tetpm9dco3Wu3CCHW2/WO2q0+sYpHeMiptqHiBr4o622LDrTUS5qtNadfwRT3UNM3X4V2or7qNGbHfMXZSrw8p1vNVqEkNCHokWIE76KljhVFM6iJfqNEyLVlmglJia6F7Wbq399jzFjxuDIlXwpNGajFsO+GoxVB+7K6bFjptiGk36GxazFmO8PwWLUYPDgoSjIuQq9yYIxE2bJZW7WGOXwSE6Dk1wZMWH0SCzZsN9RVtd8atGsfoQRQ4dg2g9boC+/i723y2BqKMSq4w/k/CHW17HxbDGGDxuC6f/7xedTh4SQ0IeiRYgTFC3fUlZWJvsCa0u05s2b517UblqEKPhD0SKEULQIcYKi5Vu+JFoLFy50L2o37jITzKFoEUIoWoQ4QdHyLZ0hWi9fvgyZeHNjACGke0PRIsQJipZv6QzRIoSQUIKiRYgTXSFa48dOxMjRk7Bu598e87oihjot6gye5d6EokUIIa5QtAhxoitEKzKxjxzW3/oNmoZ/sX7VepTWvkKTzoyc8pdy3pHDV3D8ch1O7duHO3l6/LV1O/LKn8l5JTmF+G3XRVis4xeOX8SR3btx6YEZTZZ3iIrOwIHj9xzP1VCpxcrlG3Ezzyjn51X+hzVrd8Bg/iTnn96/H4ePnEVNgERr+fLl7kXER8TpSfG9c+/QurtHvGdfO+0mpDOgaBHiRFeIVkR8Ogoe1WNEzwiYNI9lWboqSg7j+023Dt9g7JJjiFTEyLKv0mLlMDVagTtHtuDoDb1Vmj5g0rrLGK1Synn9IpUwW4fR8eNcnstQo5HDOQOS0WR4jIQxG2Fu1CJl1mGUXDuGSs17aEqLAyZa7FLGvwjJun3mcdhGXAdH2SLBDkWLECe6QrTsNVoi88ekQ28djktVyOmLO37E/wbEwCSWUyTKsvF9k+VwcIoKG6f0dJGi0aoIOfxfpgKGJk/RUimUcl37v+ktRStt/jmrpL2G4qtN2De3n1zGUNPgV9ESB8Lq6mrMnTvXaUsRXxHbWLRZZrF4fg7hEqPRKLt+akvsCQkGKFqEONHVorXm6yH4fd95RFmFySwOoKZnUCROsS3XimjpauqRlDkGmzZsQYXmo4doRSojsWrVesf6B0Qr8df+U+iRpPIQLW1VFb5ZtAErFszpsGiJGqtr167h+vXrjty5cwfPnj1z31TER4TAihqdcBYtjUbz2Z4ICAkGKFqEOCEOXmq12r24VVo7DdYR0XKPxWK7Xsqem1VvPZZxj/tjPjfPfdrbed6ktRotEhgoWkBjYyNFiwQ9FC1C3EhPT3cvahUhWgqFAr1793aU+UO0nFNVa7vgPVRC0eo8KFoULRIaULQIcUPstPv27YsHDx64z3LBLlr2iJ2+v0Ur1OKraLm3rN7d8+TJE/dN4DUULYoWCQ0oWoR0EGfRKiwslGUULU/REh1Ne4u7iHT3dJVomWsqkJo81KM81ELRIqEARYuQDiJEKyoqyqU/O4qWq2jFxcVJEfUWi9mEgdN/lhJSeHEHTubUIDY21pFx86e5TC/YegbR0dGIiYmFXsiLtgyxMTFIHbnAITNT0pNhaB6/XGmQw9ieg1yE5/T68ShuNDmmx8dGekhRW/lm43mPsoV/3nSMz96dI4cl59fK1xobGyenc3Z93S7Rcr9Jw1vRMmv/w47dJx3T/a3bKmXKNhSrP3gs+6XM+flPj7LWYlRXy+ZF3Ms/l3kDEtr9GIoWCQUoWoR0kJKSEveidovW0eUzkZgQh4Q+tmYY5o9Ok8ORPxySw4iUsXKYqrDdTfilWIwvkdAzA4pIW1tb3mTBkFg0Gj3LOxK7aNXX10OpVDpq/LxFiFbGpB+kiBSc/w3HHtbI8SmZyS4io4ge5hgfveayHMb06I+5Y1JgMrtKjzbnT2w6W4KCQ/MwePp6WHSFGP/TIdtzFBTL4el1Y5FbVoWK2kY5PTZaiZqyImiMzesxG1BUUulYZ2NdheN5Zq63iVZ1da0cmnU1qNaZm5c1ISqmhxwvPrMSp3Ma5PjkZXuQvX1iu0RLpVJhx44djmmvRMv4FPPXn5bjLRLz5RsenNeZu22aYzx1+AKPZVuLvuIRjK2UKxW2Zkv8FYoWCQUoWoT4kY6IlhhumDlCtuyekGBrwsFFtMpvWst7wVByAYf3/yZrRMQ8Y3mBdTweMdG2xk1Fyi5vhcnpILl4RC/ExdvWeXjNPCQlJuHHNduw/qJ4je8QkzFPPqfVJ1D3KBtx1nUPmfUbmrQmJMTHI2XAZI/X/LkI0frzzz8RERHhcv3arVu3ZLZu3eq+yVwQouX8OG9Ea8Ds33H13GGM+vkMzEadVfAUKKo3uiyv7DkEsZEqfJsZhXl9E6G3ClBk7FA5b+aQNClaokbLbGjA5qwcKVpi3vh4hXXb6LHxRr2cTpy5D9/1i7aKqRknt8xGfo0OM9edRUyEyvFcm8fbxEpky+zhKL+2HYfvVUvR2rT/PLL2rMChu9UdEi37dhHXEHojWjP7prpMr5s0GNX6D2h4dBXf/lmBjDgFqtTPsXq6dXsaniFx2CLc2r0YOy7qHY+5u2UytNpXcjwivg8aag3oM2YZ8k6fRKPmBb5KU6HJ9BKKmP6oL69G39l7pGhdyjHhwYH/udRSCdES6zLkn8B3m25Ap32CvXee4tH5s2isN0OZOMX6Op5g1YFs1BSXoqDu87VuFC0SClC0CPEjHRGtpctWITOlF7RmYF6bNVrx0Bedxh+3n8vplae06J8UKcdTolxrCYZmpmLx5jNoMv6Lu2WPUVPzGNGZS7Fv2QzHMjERPXDv6DY8qH6NBV/FStGKSR3hmB8V0Vs+rqrggWxA1f11txV7jZaQgP79+3vUaP3333+4evVqm9FqNeg9YbF1qEVO1hYculMmxyf3SZZDexTRQx3jI1eed5knkq4SB/SW6WXje2LWn3dRcG0fopRx0KpLoRowAwcPHpQ5vno08mo0ctlxqw/KFvbF+P/6KlBfl48KjW09yuiRSFVGyfHs09uw/1YJlDFJSI2zlYmkDVvYPK5BpDIZZ7OOI7r/DOSdXIbj96ocy93ZOt66jWsc7120P+Y8dE9kZKRjewqR9Ua05gywfZ/s6Zds+86I9Oi3QIqWGM85sglm3VMM+e53VN89ji2nah3LtVajFZMxDvdP7kFKxmD0jIuRotVj6g40Wd5D0fd7R42WWatHnVNtqb1G6+f+UdA0v+5h32zHwzMHMXT4GEQoVFK08mreocn8Cn8X2L7vbYWiRUIBihYhfqQjoiWGpqpHSJ7zt9ei9dOhRhjq1fI6pYvZFo/1nv+hP/Iq1ag1t5Q5i5bmwREkDfpOjttE6xOS+rXUXimjhnis05u4X6MlWi4XguAtvpw6FNm6cBxS09KQPGSWy/LGmmwYm0/17b5jW+fmGQMwbPhQxGZ8LWu0EnukIDkxDnqT7dShWGapVbR04vkiY/BV/96oNJhRcHEnklIHITIiAWaL/dShGaqeA3Fz63SrMNue5+qvk3Ey23Y6cemUfi6nDkU6UqMlPm/RErrAG9GymJ5j+PTNUlrWn2q0vqa5WHtKjd0/TsLR/FcuotVkfotFW69hx65TLutQX9qEBhNkA7rOopUUbevuaWCPaK9Fq5dVtNRiXWXXMW31BRiry3C68DXSYhXWZZukiFG0SHeDokWIH+mIaIkaisyBI+W0XbRS4lXQmD9h1qB0eSqwNdHa9b9vkdgzzSpFLbUW6ryrcn1JfWyC1jclQcpDrdFVtEQulbyWQ3uN1pH1i9E7LR2DJv6M2wd/gdIqSJGqBI/X/Lm4i1Z7cZajcEh7RGvRokUu096IlohR9x9yc9WOaU21DnW69x7LicjeCKzClTx2pUt5fm6NPLXtvnxhga3vzPbkkXVdYqhv/Be5j1p+KzXqLzfM6x6KFgkFKFqE+JH2ipYvmTOsF74aNhyRyhSPeV0Vilb70h7Rcsdb0WpPaqvMKC03e5QHayhaJBSgaBHiRzpTtIIxFK32JdhEK9RC0SKhAEWLED9C0fJNtIj3ULQoWiQ0oGgR4kcoWhStzoKiRdEioQFFixA/QtGiaHUWFC2KFgkNKFqE+BGKFkWrs6BoUbRIaEDRIsSPULQoWp0FRQvQaDQULRL0ULQI8SNCtP5a2oCs7cawTFVVlWyklAe+wCO2sZCMuro65OfnIycnJ6xSUFAgO9p+9+4dv28kqKFoEeJHRC3D48eP5T/t2tpa2cVKuEStVssmC4Rs8sDXOYjv24sXL2QzEf/++2/YRPzGnj59KkVTbANCghmKFiF+RAiG+IctTp+J0zriIBguEe9XdA8jDnwUrc5BbGexvUU+fPgQNrG/X37PSChA0SKEEEJIpxMumkzRIoQQQggJEBQtQgghhJAAQdEihBBCCAkQFC1/Ey4nnQkhhAQHPO4ENRQtQgghhJAAQdEihBBCCAkQFC1CCCGEkABB0SKEEEIICRAULUIIIYSQAEHRIoQQQggJEP8Pr1PgykIsQM0AAAAASUVORK5CYII=>

[image2]: <data:image/png;base64,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>