# Vibe Coding cho KPAH v1.6.255

## Mục đích
Tài liệu này bổ sung quy trình Vibe Coding cho repository KPAH: AI đọc project, phân tích lỗi, tạo checkpoint, sửa từng phần, giao GitHub Actions build APK, đọc log và kiểm tra runtime.

## Cài trên Windows
Cài Git, JDK 17 và Python 3. Clone repository:

~~~bash
git clone https://github.com/Promaxxxt/KPAH-v255-APK.git
cd KPAH-v255-APK
git status
java -version
python --version
~~~

Không bắt buộc cài toàn bộ Android SDK để AI đọc/sửa project; build APK có thể giao GitHub Actions.

## GitHub Codespaces
Mở repository -> Code -> Codespaces -> Create codespace on main.

Trong terminal:

~~~bash
git status
java -version
python3 --version
~~~

Đọc AGENTS.md trước khi giao việc cho AI. Dùng branch riêng cho thay đổi lớn.

## Android/Termux
Có thể làm việc theo mô hình:

Android -> Termux -> Ubuntu/PRoot -> Git -> JDK 17 + Python -> AI coding CLI -> GitHub -> GitHub Actions.

Termux cơ bản:

~~~bash
pkg update
pkg install git python openssh
~~~

Nếu dùng Ubuntu/PRoot, cài JDK/Git/Python bên trong Ubuntu.

## Quy trình sửa
Ví dụ lỗi login sau khi thoát game:
1. Chỉ phân tích trước.
2. Tìm class/script login/RMS/persistence.
3. Tạo branch.
4. Tạo checkpoint.
5. Sửa tối thiểu.
6. Trigger build.
7. Nếu D8 lỗi, đọc D8_BAD_CLASSES.txt và BUILD_ERROR.txt.
8. Nếu runtime lỗi, đọc log smoke test.
9. Chỉ sửa tiếp khi nguyên nhân mới rõ.

## Checkpoint
~~~bash
git status
git add -A
git commit -m "checkpoint before <change>"
~~~

## D8 và runtime
Với thay đổi bytecode, build APK thành công chưa đủ. Phải kiểm tra D8 và nếu có thể chạy runtime smoke.

Kiểm tra runtime-smoke.yml, runtime-smoke-x86.yml và runtime-smoke-artifact.yml khi thay đổi liên quan crash, launch, touch hoặc Android compatibility.

## Các nhóm chức năng có thể giao AI
- login/RMS/persistence
- inventory/item/pet
- fish/item balance
- cooking/timer
- game time/client time
- UI/touch/hotkey/capsule
- crash khi thoát và vào lại
- D8 malformed bytecode
- Android compatibility

## Lưu ý
KPAH hiện là JAR/bytecode. Không nên tự động decompile và thay thế toàn bộ project bằng source giả định. Quy trình an toàn là:

JAR -> phân tích class/method -> patch có kiểm soát -> D8 -> APK -> runtime test.

## Secret
Không commit API key, token, mật khẩu hoặc production signing key. Dùng GitHub Secrets/biến môi trường khi cần.
