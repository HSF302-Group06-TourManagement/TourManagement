# <span style="color:#0d6efd">Mô Tả Dự Án TourMangement</span>

> <span style="color:#495057">Dự án là một ứng dụng web dùng để quản lý thông tin các tour du lịch. Hệ thống hỗ trợ xem danh sách tour, xem chi tiết, thêm mới, cập nhật, xóa tour, tìm kiếm theo điểm đến, thanh toán thử nghiệm qua VNPay Sandbox và hỗ trợ tư vấn tour bằng AI chatbot.</span>

---

## <span style="color:#198754">1. Mục Đích</span>

Ứng dụng giúp quản lý danh sách tour du lịch trong hệ thống. Mỗi tour có các thông tin chính như:

- Tên tour
- Điểm đến
- Sức chứa
- Thời lượng
- Ngày bắt đầu
- Giá
- Trạng thái hoạt động

Người dùng có thể theo dõi toàn bộ tour hiện có, xem chi tiết từng tour, thêm tour mới, sửa thông tin tour đã có và xóa tour không còn cần quản lý.

---

## <span style="color:#198754">2. Các Màn Hình Chính</span>

### <span style="color:#0d6efd">Trang giới thiệu</span>

Trang giới thiệu hiển thị giao diện tổng quan về hệ thống tour. Trang này có phần giới thiệu, danh sách tour nổi bật nếu có dữ liệu và một số nội dung điểm đến du lịch đang hiển thị sẵn.

### <span style="color:#0d6efd">Trang danh sách tour</span>

Trang danh sách tour là màn hình quản lý chính. Người dùng có thể xem các tour theo dạng bảng.

<span style="color:#6f42c1"><strong>Thông tin hiển thị trên danh sách gồm:</strong></span>

- Số thứ tự
- Tên tour
- Điểm đến
- Sức chứa
- Thời lượng
- Ngày bắt đầu
- Giá
- Trạng thái
- Các thao tác

Danh sách tour được sắp xếp theo tên tour tăng dần. Cột số thứ tự chỉ dùng để hiển thị trên giao diện, không phải mã tour trong cơ sở dữ liệu.

Người dùng có thể tìm kiếm tour theo điểm đến. Khi nhập từ khóa vào ô tìm kiếm, hệ thống lọc các tour có điểm đến phù hợp. Nếu không nhập từ khóa, hệ thống hiển thị toàn bộ danh sách tour.

### <span style="color:#0d6efd">Trang chi tiết tour</span>

Trang chi tiết hiển thị thông tin đầy đủ của một tour cụ thể.

<span style="color:#6f42c1"><strong>Tại trang này, người dùng có thể:</strong></span>

- Xem chi tiết tour
- Chuyển sang trang cập nhật tour
- Chuyển sang trang thanh toán tour
- Xóa tour thông qua hộp thoại xác nhận
- Quay lại danh sách tour

### <span style="color:#0d6efd">Trang thêm tour</span>

Trang thêm tour cho phép người dùng tạo mới một tour.

Người dùng cần nhập đầy đủ các thông tin bắt buộc. Nếu dữ liệu không hợp lệ, hệ thống hiển thị thông báo lỗi màu đỏ bên dưới trường nhập tương ứng.

Sau khi thêm thành công, hệ thống quay về trang danh sách tour và hiển thị thông báo thêm thành công.

### <span style="color:#0d6efd">Trang cập nhật tour</span>

Trang cập nhật tour cho phép người dùng sửa thông tin của một tour đã tồn tại.

Thông tin cũ của tour được hiển thị sẵn trên form. Người dùng có thể thay đổi các thông tin cần thiết và lưu lại.

Nếu cập nhật thành công, hệ thống quay về trang danh sách tour và hiển thị thông báo cập nhật thành công. Nếu bấm hủy, hệ thống quay về trang danh sách tour.

### <span style="color:#0d6efd">Trang thanh toán tour</span>

Trang thanh toán cho phép người dùng nhập thông tin khách hàng và số lượng đặt tour trước khi chuyển sang bước thanh toán.

<span style="color:#6f42c1"><strong>Thông tin thanh toán gồm:</strong></span>

- Tên khách hàng
- Email
- Số điện thoại
- Số lượng
- Thông tin tóm tắt tour

Nếu thông tin nhập không hợp lệ, hệ thống hiển thị lỗi ngay trên form thanh toán. Khi thông tin hợp lệ và cấu hình VNPay đầy đủ, hệ thống tạo URL thanh toán và chuyển người dùng sang VNPay Sandbox.

### <span style="color:#0d6efd">Trang kết quả thanh toán</span>

Sau khi thanh toán trên VNPay Sandbox, người dùng được chuyển về trang kết quả thanh toán của hệ thống.

Trang này hiển thị:

- Trạng thái thanh toán
- Mã giao dịch
- Mã phản hồi
- Số tiền
- Trạng thái kiểm tra chữ ký

Nếu VNPay trả về mã thành công và chữ ký hợp lệ, hệ thống hiển thị thanh toán thành công. Nếu giao dịch thất bại hoặc chữ ký không hợp lệ, hệ thống hiển thị thông báo thất bại.

---

## <span style="color:#198754">3. Thanh Toán VNPay Sandbox</span>

Hệ thống có tích hợp luồng thanh toán qua VNPay Sandbox để mô phỏng quá trình thanh toán tour.

<span style="color:#6f42c1"><strong>Luồng thanh toán hiện tại:</strong></span>

1. Người dùng mở trang chi tiết tour.
2. Người dùng chọn thanh toán.
3. Hệ thống hiển thị form nhập thông tin khách hàng.
4. Người dùng gửi form thanh toán.
5. Hệ thống tạo URL thanh toán VNPay Sandbox.
6. Người dùng được chuyển sang trang VNPay.
7. Sau khi hoàn tất thanh toán, VNPay chuyển người dùng về trang kết quả thanh toán của hệ thống.

Nếu chưa cấu hình đủ thông tin VNPay, hệ thống có thể chạy ở chế độ demo để không làm gián đoạn luồng kiểm thử giao diện.

Các thông tin nhạy cảm của VNPay như mã website và chuỗi bí mật không được lưu trực tiếp trong mã nguồn. Hệ thống đọc các giá trị này từ biến môi trường hoặc file cấu hình cục bộ không đưa lên GitHub.

---

## <span style="color:#198754">4. AI Chatbot Tư Vấn Tour</span>

Hệ thống có thêm AI chatbot hiển thị trên giao diện để hỗ trợ người dùng hỏi nhanh thông tin tour.

<span style="color:#6f42c1"><strong>Chatbot có thể hỗ trợ:</strong></span>

- Gợi ý tour theo điểm đến
- Tìm tour giá rẻ nhất
- Gợi ý tour có thời lượng ngắn
- Liệt kê các tour đang có trong hệ thống
- Trả lời dựa trên dữ liệu tour hiện có trong cơ sở dữ liệu

Chatbot ưu tiên sử dụng Google Gemini API nếu hệ thống có cấu hình API key. Nếu không có API key hoặc quá trình gọi AI bên ngoài thất bại, chatbot vẫn có thể trả lời bằng dữ liệu tour trong database để ứng dụng không bị gián đoạn.

Chatbot chỉ sử dụng dữ liệu tour hiện có của hệ thống để tư vấn, không tự tạo thêm tour không tồn tại trong cơ sở dữ liệu.

---

## <span style="color:#198754">5. Thông Tin Một Tour</span>

Mỗi tour hiện tại gồm các thông tin:

| Thông tin | Ý nghĩa |
| --- | --- |
| Tên tour | Tên của tour du lịch |
| Điểm đến | Địa điểm hoặc khu vực tour đi đến |
| Sức chứa | Số lượng khách tối đa |
| Thời lượng | Số ngày của tour |
| Ngày bắt đầu | Ngày tour khởi hành |
| Giá | Giá tour |
| Trạng thái | Tình trạng hoạt động của tour |

<span style="color:#6f42c1"><strong>Trạng thái tour hiện có ba giá trị:</strong></span>

- <span style="color:#198754"><strong>Active</strong></span>
- <span style="color:#dc3545"><strong>Inactive</strong></span>
- <span style="color:#ffc107"><strong>Draft</strong></span>

Trên trang danh sách, trạng thái <span style="color:#dc3545"><strong>Inactive</strong></span> được hiển thị màu đỏ để người dùng dễ nhận biết.

---

## <span style="color:#198754">6. Quy Tắc Kiểm Tra Dữ Liệu</span>

Hệ thống kiểm tra dữ liệu khi thêm, cập nhật tour và nhập thông tin thanh toán.

### <span style="color:#0d6efd">Tên tour</span>

- Bắt buộc nhập
- Độ dài từ 1 đến 200 ký tự

### <span style="color:#0d6efd">Điểm đến</span>

- Bắt buộc nhập
- Độ dài từ 1 đến 200 ký tự

### <span style="color:#0d6efd">Sức chứa</span>

- Bắt buộc nhập
- Phải là số
- Giá trị từ 1 đến 1000

### <span style="color:#0d6efd">Thời lượng</span>

- Bắt buộc nhập
- Phải là số
- Giá trị từ 1 đến 300

### <span style="color:#0d6efd">Ngày bắt đầu</span>

- Bắt buộc nhập
- Định dạng ngày là `dd/MM/yyyy`
- Phải là ngày trong tương lai
- Phải trước ngày hiện tại cộng 360 ngày

### <span style="color:#0d6efd">Giá</span>

- Bắt buộc nhập
- Phải là số
- Giá trị từ 0.1 đến 100000

### <span style="color:#0d6efd">Trạng thái</span>

- Bắt buộc chọn
- Chỉ chấp nhận `Active`, `Inactive` hoặc `Draft`

### <span style="color:#0d6efd">Thông tin thanh toán</span>

- Tên khách hàng bắt buộc nhập
- Email bắt buộc nhập và phải đúng định dạng
- Số điện thoại bắt buộc nhập
- Số lượng bắt buộc nhập và phải nằm trong khoảng hợp lệ

---

## <span style="color:#198754">7. Cách Nhập Ngày Bắt Đầu</span>

Trường ngày bắt đầu là ô nhập văn bản, không phải lịch chọn ngày của trình duyệt.

Người dùng nhập ngày theo định dạng:

```text
dd/MM/yyyy
```

Hệ thống có hỗ trợ tự động chèn dấu gạch chéo khi nhập ngày.

<span style="color:#6f42c1"><strong>Ví dụ:</strong></span>

- Nhập `01012027` sẽ được hỗ trợ thành `01/01/2027`
- Nhập `1/1/2027` rồi rời khỏi ô nhập sẽ thành `01/01/2027`

Nếu ngày không đúng định dạng hoặc không nằm trong khoảng hợp lệ, hệ thống hiển thị lỗi bên dưới trường ngày bắt đầu.

---

## <span style="color:#198754">8. Xóa Tour</span>

Khi người dùng bấm xóa tour, hệ thống không xóa ngay lập tức. Một hộp thoại xác nhận sẽ hiện ra để người dùng quyết định có tiếp tục xóa hay không.

Nếu người dùng xác nhận xóa, tour bị xóa khỏi danh sách và hệ thống quay về trang danh sách tour kèm thông báo xóa thành công.

---

## <span style="color:#198754">9. Thông Báo Sau Khi Thao Tác</span>

Sau các thao tác thành công, hệ thống hiển thị thông báo trên trang danh sách:

- <span style="color:#198754"><strong>Thêm tour thành công</strong></span>
- <span style="color:#198754"><strong>Cập nhật tour thành công</strong></span>
- <span style="color:#198754"><strong>Xóa tour thành công</strong></span>

Sau thanh toán, hệ thống hiển thị kết quả tại trang kết quả thanh toán riêng.
