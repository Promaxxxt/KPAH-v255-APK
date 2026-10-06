# KPAH Vibe Coding Agent Guide

## Mục tiêu
AI coding agent làm việc với KPAH v1.6.255 phải ưu tiên sửa có kiểm soát, build bằng CI và giữ khả năng rollback.

## Quy tắc an toàn
1. Không sửa trực tiếp KPAHtuoitho_drg.jar nếu chưa có checkpoint.
2. Ưu tiên branch riêng cho thay đổi lớn.
3. Không xóa workflow, patch hoặc script cũ chỉ để làm build xanh.
4. Sau thay đổi phải chạy kiểm tra phù hợp.
5. Khi build lỗi, đọc BUILD_ERROR.txt và xác định lỗi gốc trước khi sửa.
6. Với bytecode phải kiểm tra D8; với runtime phải kiểm tra smoke test.
7. Không commit API key, token, mật khẩu hoặc production keystore.
8. Nếu chưa chắc nguyên nhân, chỉ phân tích và không sửa lan.

## Quy trình
ANALYZE -> CHECKPOINT -> PATCH -> BUILD -> READ ERROR -> FIX -> RUNTIME TEST -> REPORT

## Phạm vi
- Build APK: .github/workflows/build-apk.yml
- D8/bytecode: scripts/apply_dexsafe_patches.py, scripts/isolate_d8_bad_classes.py, patches/
- Runtime/crash: patch_runtime_crash_dialog.py, patch_k70_safe_runtime.py, runtime-smoke*.yml
- Port/UI/touch: scripts/prepare_port.py

## Câu lệnh người dùng
- Chẩn đoán lỗi KPAH hiện tại
- Tạo checkpoint rồi sửa lỗi này
- Kiểm tra lỗi D8
- Kiểm tra crash runtime
- Sửa chức năng X rồi build APK
- Rollback thay đổi vừa làm
- Chỉ phân tích, chưa sửa
- Kiểm tra login/RMS
- Kiểm tra inventory/item/pet
- Kiểm tra timer/game time/client time

## Khi sửa thất bại
Giữ log, xác định commit trước thay đổi, rollback phần liên quan và chạy lại kiểm tra. Không reset toàn bộ repository nếu không cần thiết.

## Tiêu chuẩn hoàn thành
Báo rõ file đã sửa, nguyên nhân, commit/checkpoint, build, D8, runtime smoke và lỗi còn lại.
