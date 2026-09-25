# Dragon Game Studio 0.6

Bản 0.6 tập trung vào hai khu vực làm việc trực quan hơn: **Map + kho tile** và **Thiết kế skill/effect**.

## Map + kho tile

- Tile map vẫn dùng ô logic 16×16 để tương thích engine 2D.
- Tile cắt từ ảnh được lưu riêng trong `tile-bank.png` thay vì gắn trực tiếp vào canvas.
- Màn tạo map chia hai phần: **kho tile bên trái** và **canvas bên phải**.
- Chạm tile trong kho để chọn, sau đó kéo trên canvas để tô.
- Có thể cắt một khối nhiều tile rồi đặt cả khối lên map.
- Dự án 0.5 dùng `tiles.png` vẫn mở được; 0.6 ưu tiên `tile-bank.png`.

## Skill + hiệu ứng

Tab **Skill** quản lý danh sách skill và cho chọn hiệu ứng đã tạo. Tab **Thiết kế skill** quản lý kho effect riêng trong thư mục `effects/`.

Quy trình: nhập PNG → kéo cắt vùng → thu/phóng → xoay 90° → lật ngang/dọc → nhập kích thước frame → nhập số frame → chạy thử animation → lưu hiệu ứng → chọn skill và gán effect.

Frame skill **không còn bắt buộc 16×16**. Có thể dùng 24×32, 48×64, 96×96, 128×64 hoặc kích thước khác miễn spritesheet chia đều theo kích thước frame đã khai báo.

Mỗi hiệu ứng lưu thành file riêng như `effects/eff_1.png`, `effects/eff_2.png` và metadata nằm trong `project.json`. Khi xuất `.dgproject`, kho tile và toàn bộ effect cũng được đóng gói theo dự án.

## GSC

Runtime GSC vẫn hỗ trợ `if/then/else`, `and/or`, HP/MP, item, biến, trạng thái, trọng lực, nhảy/bay, chiến đấu và các sự kiện gameplay. Xem `HUONG_DAN_ENGINE_GSC_0.6.txt` hoặc nút hướng dẫn trong tab Mã nguồn.

## Build APK

`BUILD_APK.sh` dùng Android SDK build-tools, platform android-34, JDK có `javac`, Python 3 và keystore `debug.keystore`. APK đầu ra là `DragonGameStudio-0.6-debug.apk`.