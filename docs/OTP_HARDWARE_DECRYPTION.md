# RK3576 OTP 硬體解密整合方案

## 概述

將 GameUpdate APK 的 AES 解密方式從軟體解密改為使用 RK3576 OTP 硬體金鑰，提升安全性。

### 安全架構

```
┌─────────────────────────────────────────────────────────────┐
│                        Normal World                          │
│  ┌─────────────┐    ┌──────────────┐    ┌────────────────┐  │
│  │ GameUpdate  │───▶│ librkcrypto  │───▶│   /dev/crypto  │  │
│  │    APK      │    │    (JNI)     │    │                │  │
│  └─────────────┘    └──────────────┘    └───────┬────────┘  │
└─────────────────────────────────────────────────┼───────────┘
                                                  │
┌─────────────────────────────────────────────────┼───────────┐
│                        Secure World (TEE)       │           │
│                                                 ▼           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   OP-TEE                              │  │
│  │   ┌──────────┐        ┌────────────────────────┐     │  │
│  │   │ OTP Key  │───────▶│   Crypto Hardware      │     │  │
│  │   │  (KEY1)  │        │   (AES Engine)         │     │  │
│  │   └──────────┘        └────────────────────────┘     │  │
│  │        ↑                                              │  │
│  │   Key 永不離開 Secure World                           │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 安全優勢

| 項目 | 原本（軟體解密） | 現在（OTP 硬體解密） |
|------|------------------|----------------------|
| Key 存放位置 | `/system/bin/aes_key.bin` | OTP (一次性寫入) |
| Key 可被讀取 | ✅ 可以 | ❌ 無法 |
| 記憶體中有 Key | ✅ 有 | ❌ 沒有 |
| Root 可提取 Key | ✅ 可以 | ❌ 無法 |

---

## Key 與 IV 說明

### 原始值

```
完整字串: dfa7393ebbf9185d50e9744e9cfdf518 (32 chars)
├── Key: dfa7393ebbf9185d (前 16 chars)
└── IV:  50e9744e9cfdf518 (後 16 chars)
```

### 轉換為 Bytes（ASCII 編碼）

字串中的每個字元轉換為其 ASCII byte 值：

**Key bytes:**
```
字串: d    f    a    7    3    9    3    e    b    b    f    9    1    8    5    d
Hex:  0x64 0x66 0x61 0x37 0x33 0x39 0x33 0x65 0x62 0x62 0x66 0x39 0x31 0x38 0x35 0x64
```

**IV bytes:**
```
字串: 5    0    e    9    7    4    4    e    9    c    f    d    f    5    1    8
Hex:  0x35 0x30 0x65 0x39 0x37 0x34 0x34 0x65 0x39 0x63 0x66 0x64 0x66 0x35 0x31 0x38
```

### OTP 寫入狀態

| Slot | 狀態 | 內容 | 備註 |
|------|------|------|------|
| KEY0 | WRITTEN | `df a7 39 3e...` | ❌ 錯誤的 key（已無法使用） |
| KEY1 | WRITTEN | `64 66 61 37...` | ✅ 正確的 key（目前使用） |
| KEY2 | EMPTY | - | 可用 |
| KEY3 | EMPTY | - | 可用 |

---

## 檔案修改清單

### 新增檔案

```
app/src/main/
├── jni/
│   ├── rkcrypto_jni.c      # JNI C 程式碼（含 PKCS7 unpadding）
│   ├── Android.mk          # NDK 編譯設定
│   ├── Application.mk      # ABI 設定 (arm64-v8a)
│   ├── include/            # librkcrypto headers
│   │   ├── rkcrypto_core.h
│   │   ├── rkcrypto_otp_key.h
│   │   └── rkcrypto_common.h
│   └── libs/arm64-v8a/
│       └── librkcrypto.so  # 預編譯函式庫
│
└── java/model/
    └── CryptoNative.java   # JNI Java 橋接類別
```

### 修改檔案

| 檔案 | 修改內容 |
|------|----------|
| `model/Crypto.java` | 新增 `decryptWithOtpKey()` 方法 |
| `UpdateMethod/UpdateMethod.java` | `DecryptFile()` 優先使用 OTP 解密 |
| `app/build.gradle` | 加入 NDK 編譯支援 |

---

## 解密流程

```
DecryptFile() 呼叫
        │
        ▼
┌───────────────────┐
│ 嘗試 OTP 硬體解密  │
│ (CryptoNative)    │
└─────────┬─────────┘
          │
          ▼
    ┌───────────┐     失敗
    │  成功？   │────────────┐
    └─────┬─────┘            │
          │ 成功              ▼
          │           ┌──────────────────┐
          │           │ Fallback 軟體解密 │
          │           │ (原本的 Java 方式) │
          │           └────────┬─────────┘
          │                    │
          ▼                    ▼
    ┌─────────────────────────────┐
    │      MD5 驗證解密結果        │
    └─────────────────────────────┘
```

### 分塊解密（處理大檔案）

```
game.apk.enc (可達 3GB)
        │
        ▼
┌─────────────────────────────────────┐
│  每次讀取 1MB chunk                  │
│  ┌─────┐ ┌─────┐ ┌─────┐           │
│  │ 1MB │ │ 1MB │ │ 1MB │ │ ... │   │
│  └──┬──┘ └──┬──┘ └──┬──┘           │
│     │       │       │               │
│     ▼       ▼       ▼               │
│  解密    解密    解密               │
│  (OTP)   (OTP)   (OTP)              │
│     │       │       │               │
│     ▼       ▼       ▼               │
│  寫出    寫出    寫出+Unpad         │
└─────────────────────────────────────┘
        │
        ▼
game.apk (解密完成，已移除 PKCS7 padding)
```

---

## 設備端設定

### 1. 建立 IV 檔案

```bash
adb root
adb remount
adb shell "echo -n '50e9744e9cfdf518' > /system/bin/aes_iv.bin"
adb shell "chmod 644 /system/bin/aes_iv.bin"
```

### 2. 設定 crypto 設備權限（開發測試用）

```bash
adb shell "chmod 666 /dev/crypto"
```

### 3. 永久設定（量產用）

在 `device/rockchip/rk3576/ueventd.rk3576.rc` 加入：

```
/dev/crypto  0666  root  root
```

---

## API 使用說明

### Java 層

```java
// 使用 OTP 解密檔案
int result = Crypto.decryptWithOtpKey(encryptedFile, decryptedFile);

// 回傳值
//  0: 成功
// -101: 初始化失敗 (librkcrypto)
// -102: KEY1 未寫入
// -103: IV 檔案讀取失敗
// -104: 解密失敗
```

### JNI 層

```java
// 初始化
CryptoNative.initialize();  // 呼叫 rk_crypto_init()

// 檢查 Key 狀態
int status = CryptoNative.checkKeyWritten(CryptoNative.KEY1);
// 1: 已寫入, 0: 空, -1: 錯誤

// 解密檔案
int ret = CryptoNative.decryptFileWithOtpKey(
    CryptoNative.KEY1,  // 使用 KEY1
    ivBytes,            // 16 bytes IV
    inputPath,          // 加密檔案路徑
    outputPath          // 輸出檔案路徑
);

// 清理
CryptoNative.cleanup();  // 呼叫 rk_crypto_deinit()
```

---

## 驗證測試

### OpenSSL 加密指令（正確格式）

```bash
# Key 和 IV 需要用 ASCII hex 表示
# Key: "dfa7393ebbf9185d" → 64666137333933656262663931383564
# IV:  "50e9744e9cfdf518" → 35306539373434653963666466353138

openssl enc -aes-128-cbc \
    -K 64666137333933656262663931383564 \
    -iv 35306539373434653963666466353138 \
    -in plaintext.bin \
    -out encrypted.bin
```

### 設備端測試工具

```bash
# 顯示正確的 Key 和 IV
adb shell "LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/otp_key_test show"

# 測試加解密
adb shell "LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/otp_key_test test 1"
```

---

## 錯誤排查

| 錯誤碼 | 原因 | 解決方案 |
|--------|------|----------|
| -101 | `rk_crypto_init()` 失敗 | 檢查 `/dev/crypto` 權限 |
| -102 | OTP KEY1 未寫入 | 執行 `otp_key_test write 1` |
| -103 | IV 檔案不存在 | 建立 `/system/bin/aes_iv.bin` |
| -104 | 解密失敗 | 檢查 Key/IV 是否正確 |
| MD5 不符 | Padding 問題 | 確認 JNI 有 PKCS7 unpadding |

### Logcat 過濾

```bash
adb logcat -s RKCryptoJNI:* CryptoNative:* Crypto:* UpdateMethod:*
```

---

## 注意事項

1. **OTP 寫入不可逆** — KEY0 已寫入錯誤值，無法修改，改用 KEY1
2. **Key 無法讀出** — 這是設計特性，只能做加密運算
3. **IV 不需保密** — IV 存放在檔案即可，只有 Key 需要保護
4. **權限問題** — APK 需要存取 `/dev/crypto`，需設定適當權限
5. **PKCS7 Padding** — OpenSSL 預設使用 PKCS7，JNI 層需做 unpadding

---

## 版本資訊

- librkcrypto API 版本: 1.2.3
- 目標平台: RK3576 Android 14
- ABI: arm64-v8a
- AES 模式: AES-128-CBC
