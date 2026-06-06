# TriggerBot File I/O - Troubleshooting Guide (Windows)

## 🔍 Vấn đề Đã Phát Hiện

### Root Cause
`isEnabled()` luôn trả về `false` vì:

1. **Windows CRLF Issue** (Chính yếu)
   - Khi `split("|")` trên Windows, nếu file chứa `\r\n`, các part sẽ giữ ký tự whitespace
   - Ví dụ: `"true\r|-1\r|100"` → split → `["true\r", "-1\r", "100"]`
   - Khi so sánh: `"true\r".equalsIgnoreCase("true")` → **FALSE** ✗

2. **Encoding không rõ ràng**
   - Không chỉ định UTF-8 tường minh → có thể bị ảnh hưởng bởi hệ thống
   - BOM (Byte Order Mark) có thể được thêm vào file

3. **Race Condition**
   - File được ghi nhưng chưa sync/flush, Agent đọc ngay → đọc dữ liệu cũ hoặc không đầy đủ
   - Nhất là trên Windows, file buffering có thể delay write

4. **Thiếu Logging**
   - `catch (Exception e) {}` im lặng, không biết lỗi gì xảy ra
   - Rất khó debug

---

## ✅ Sửa Chữa

### 1. TriggerBotBridge.java - Cập Nhật
**Các thay đổi chính:**
```java
// ✓ Trim TỪNG PART sau split (không chỉ trim toàn chuỗi)
String enabledStr = parts[0].trim(); // ← Quan trọng!
boolean enabled = "true".equalsIgnoreCase(enabledStr);

// ✓ UTF-8 explicit
Files.writeString(FILE, config, StandardCharsets.UTF_8, ...);
Files.readString(FILE, StandardCharsets.UTF_8);

// ✓ Sleep sau write trên Windows (force OS flush)
if (IS_WINDOWS) Thread.sleep(10);

// ✓ Chi tiết logging
System.out.println("[TriggerBotBridge] ✓ Config written: ...");
System.err.println("[TriggerBotBridge] ✗ Write failed: ...");
```

### 2. TriggerBotDebugger.java - Công Cụ Mới
Diagnostic tool để verify file read/write chain hoàn toàn.

---

## 🧪 Hướng Dẫn Debug

### Step 1: Enable Verbose Logging
Thêm vào JVM arguments trong Minecraft launcher:
```
-Dtriggerbot.debug
```

hoặc run từ command line:
```bash
java -Dtriggerbot.debug -cp target/classes me.sentaihex.client.SentaiHex
```

### Step 2: Run Diagnostics
```bash
# Compile trước (nếu chưa)
mvn clean compile

# Chạy diagnostic tool
mvn exec:java -Dexec.mainClass="me.sentaihex.client.util.TriggerBotDebugger"
```

**Output sẽ hiện:**
```
╔════════════════════════════════════════════════════════════════╗
║          TriggerBot Diagnostics                                 ║
╚════════════════════════════════════════════════════════════════╝

[1] File Location:
    Path: C:\Users\YourName\.sentaihex\triggerbot.txt
    Exists: true
    Size: 14 bytes

[2] Raw Bytes (hex):
    74 72 75 65 7C 2D 31 7C 31 30 30 0D 0A

[3] Content (UTF-8):
    Length: 14 chars
    Raw: true|-1|100\r\n
    Display: [true|-1|100
    ]

[4] After trim():
    Length: 12 chars
    Raw: true|-1|100

[5] After split('|'):
    Parts count: 3
    Part[0]: raw=true, trimmed=true, length=4 -> 4
    Part[1]: raw=-1, trimmed=-1, length=2 -> 2
    Part[2]: raw=100, trimmed=100, length=3 -> 3

[6] Parsing Test:
    enabled: 'true' -> true ✓
    slot: '-1' -> -1 ✓
    delay: '100' -> 100 ✓

[7] TriggerBotBridge Result:
    isEnabled(): true ✓
    getWeaponSlot(): -1 ✓
    getDelayMs(): 100 ✓

[8] Environment:
    OS: Windows 10
    Java: 21.0.1
    File.encoding: UTF-8
    Line separator: \r\n
```

### Step 3: Test Scenarios

**Scenario A: File có CRLF (Windows default)**
```
Raw bytes: 74 72 75 65 7C 2D 31 7C 31 30 30 0D 0A
(61 bytes: "true|-1|100\r\n")

Khi đọc:
✓ trim() bỏ \r\n
✓ split() tách thành ["true", "-1", "100"]
✓ trim() trên từng part (không cần nhưng safe)
✓ parse OK → isEnabled() = true
```

**Scenario B: File có dòng thừa (sai ghi)**
```
Raw: "true|-1|100\r\n-1\r\n"
Sau split('|'): ["true", "-1", "100\r\n-1\r\n"]
Sau split trên part[0]: "true" ✓ OK
Nhưng part[2] sẽ "100\r\n-1\r\n" → parse delay fail → default 50
```

**Scenario C: UTF-8 BOM (sai encoding)**
```
Raw: BF BB BF 74 72 75 65 7C ...
(EF BB BF = UTF-8 BOM)

Sau trim(): bỏ BOM ✓ OK
Sau split(): ["true", "-1", ...] ✓ OK
```

---

## 📋 Verification Checklist

- [ ] **File path**: `~/.sentaihex/triggerbot.txt` exists
- [ ] **File content**: Exactly `true|-1|100` (or enabled/slot/delay values)
- [ ] **No extra whitespace**: No leading/trailing spaces
- [ ] **isEnabled() returns true**: When content = "true|..."
- [ ] **Parsing works**: All three fields (enabled, slot, delay)
- [ ] **Agent reads correctly**: TriggerBot loop recognizes enabled state
- [ ] **No race conditions**: Works after rapid enable/disable toggle

---

## 🔧 Nếu Vấn Đề Vẫn Tiếp Diễn

### Check 1: File Location
Xác nhận file được tạo ở đúng nơi:
```bash
# Windows Command Prompt
dir %USERPROFILE%\.sentaihex\
# hoặc
cd %USERPROFILE%\.sentaihex\ && type triggerbot.txt
```

### Check 2: Log Output
Khi chạy Minecraft với `-Dtriggerbot.debug`, kiểm tra console có thấy:
```
[TriggerBotBridge] DEBUG: ...
[TriggerBotBridge] ✓ Config written: ...
```

### Check 3: Agent Reading
Trong Agent console, TriggerBot loop phải show:
```
[TriggerBot] loop started
[TriggerBot] Attack executed!
```

(không phải `[TriggerBot] Disabled`)

### Check 4: Manual Test
```bash
# Clear file
del %USERPROFILE%\.sentaihex\triggerbot.txt

# Run diagnostic
mvn exec:java -Dexec.mainClass="me.sentaihex.client.util.TriggerBotDebugger"
```

---

## 📝 Notes

- **UTF-8**: Không BOM, platform-agnostic
- **Windows**: CRLF handled by trim() on each part
- **Caching**: 100ms cache để tránh read file quá thường xuyên
- **Atomicity**: writeString() atomic, nhưng thêm sleep trên Windows để safe

---

## 🎯 Summary

**Lỗi gốc**: Windows CRLF không được trim trên từng part sau split
**Sửa chữa**: Thêm `.trim()` sau lấy từng part, UTF-8 explicit, logging chi tiết
**Verify**: Dùng TriggerBotDebugger tool để kiểm tra full chain
