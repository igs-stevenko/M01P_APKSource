package model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

public class Crypto {
    private static final String TAG = "Crypto";

    /**
     * 使用 KEK (Key Encryption Key) 架構解密檔案 (推薦方式)
     * 1. 用 OTP KEY2 解密 aes_enc.bin 取得明文 AES key (僅 1 次 OTP 呼叫)
     * 2. 用明文 key 做軟體 AES 解密 (無 TEE 限制，適合大檔案)
     * 
     * @param inputFile  加密檔案路徑
     * @param outputFile 解密輸出路徑
     * @return 0 成功, 負數失敗
     */
    public static int decryptWithKEK(String inputFile, String outputFile) {
        // Step 1: 用 OTP KEY2 解密 AES key
        byte[] aesKey = CryptoNative.unwrapAesKey();
        if (aesKey == null) {
            Log.e(TAG, "Failed to unwrap AES key from OTP");
            return -110;
        }
        Log.i(TAG, "AES key unwrapped from OTP successfully");
        
        // Step 2: 讀取 IV
        byte[] iv = CryptoNative.loadIv();
        if (iv == null) {
            Log.e(TAG, "Failed to load IV");
            return -111;
        }
        
        // Step 3: 用軟體 AES 解密檔案
        return decryptAESCBCFileWithBytes(inputFile, outputFile, aesKey, iv);
    }
    
    /**
     * 軟體 AES-CBC 解密 (使用 byte[] key 和 iv)
     */
    public static int decryptAESCBCFileWithBytes(String inputFile, String outputFile, byte[] keyBytes, byte[] ivBytes) {
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException e) {
            return -2;
        } catch (NoSuchPaddingException e) {
            return -3;
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        } catch (InvalidAlgorithmParameterException e) {
            return -4;
        } catch (InvalidKeyException e) {
            return -5;
        }

        try (InputStream fileInputStream = new FileInputStream(inputFile);
             CipherInputStream cipherInputStream = new CipherInputStream(fileInputStream, cipher);
             OutputStream fileOutputStream = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            return -6;
        } catch (IOException e) {
            return -7;
        }

        return 0;
    }

    /**
     * 使用 OTP 硬體金鑰解密檔案 (舊方式，大檔案可能有 TEE 穩定性問題)
     * Key 存在 OTP，無法被讀取，更安全
     * 
     * @param inputFile  加密檔案路徑
     * @param outputFile 解密輸出路徑
     * @param iv         16 bytes IV (可從檔案讀取)
     * @return 0 成功, 負數失敗
     */
    public static int decryptWithOtpKey(String inputFile, String outputFile, byte[] iv) {
        if (!CryptoNative.isAvailable()) {
            Log.e(TAG, "CryptoNative not available, falling back to software decryption");
            return -100;
        }
        
        // 初始化
        int ret = CryptoNative.initialize();
        if (ret != 0) {
            Log.e(TAG, "CryptoNative initialize failed: " + ret);
            return -101;
        }
        
        // 檢查 KEY1 是否已寫入
        int keyStatus = CryptoNative.checkKeyWritten(CryptoNative.KEY1);
        if (keyStatus != 1) {
            Log.e(TAG, "OTP KEY1 not written, status: " + keyStatus);
            CryptoNative.cleanup();
            return -102;
        }
        
        // 解密
        ret = CryptoNative.decryptFileWithOtpKey(CryptoNative.KEY1, iv, inputFile, outputFile);
        
        // 清理
        CryptoNative.cleanup();
        
        return ret;
    }

    /**
     * 使用 OTP 硬體金鑰解密檔案 (使用預設 IV 檔案)
     * 
     * @param inputFile  加密檔案路徑
     * @param outputFile 解密輸出路徑
     * @return 0 成功, 負數失敗
     */
    public static int decryptWithOtpKey(String inputFile, String outputFile) {
        byte[] iv = CryptoNative.loadIv();
        if (iv == null) {
            Log.e(TAG, "Failed to load IV");
            return -103;
        }
        return decryptWithOtpKey(inputFile, outputFile, iv);
    }

    /**
     * 原本的軟體解密方式 (保留相容性，但不建議使用)
     * Key 存在檔案系統，有安全風險
     */
    public static int decryptAESCBCFile(String inputFile, String outputFile, String key, String iv) {

        byte[] keyBytes = new byte[0];
        try {
            keyBytes = key.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return -1;
        }
        byte[] ivBytes = new byte[0];
        try {
            ivBytes = iv.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return -1;
        }

        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException e) {
            return -2;
        } catch (NoSuchPaddingException e) {
            return -3;
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        } catch (InvalidAlgorithmParameterException e) {
            return -4;
        } catch (InvalidKeyException e) {
            return -5;
        }

        try (InputStream fileInputStream = new FileInputStream(inputFile);
             CipherInputStream cipherInputStream = new CipherInputStream(fileInputStream, cipher);
             OutputStream fileOutputStream = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            return -6;
        } catch (IOException e) {
            return -7;
        }

        return 0;
    }

}
