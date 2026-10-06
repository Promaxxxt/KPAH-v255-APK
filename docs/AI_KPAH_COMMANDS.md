# Bộ lệnh Vibe Coding KPAH

## Chẩn đoán
- Chẩn đoán lỗi KPAH hiện tại, chưa sửa.
- Kiểm tra pipeline build APK.
- Kiểm tra lỗi D8.
- Kiểm tra crash runtime.

## Sửa
- Tạo checkpoint rồi sửa lỗi này.
- Chỉ sửa phần login.
- Sửa nhưng giữ nguyên JAR gốc.
- Sửa và chạy kiểm tra sau sửa.

## Build
- Build APK KPAH.
- Chạy D8 diagnostic.
- Chạy runtime smoke test.
- Kiểm tra artifact APK mới nhất.

## An toàn
- Tạo checkpoint trước khi sửa.
- Nếu thất bại, rollback thay đổi vừa tạo.
- Không reset repository.
- Không xóa workflow đang hoạt động.
- Chỉ thay đổi file cần thiết.

## Tìm chức năng
- Tìm login/RMS.
- Tìm inventory.
- Tìm item/fish/pet.
- Tìm timer/cooking.
- Tìm game time/client time.
- Tìm touch/key/capsule.
- Tìm class gây crash.

## Chế độ không sửa
- Chỉ phân tích, không thay đổi file.
- Chỉ cho tôi nguyên nhân và file liên quan.
- So sánh hai commit và giải thích khác biệt.

## Báo cáo
AI phải nêu: file đã sửa, lý do, checkpoint/commit, build, D8, runtime smoke, APK artifact và lỗi còn lại.
