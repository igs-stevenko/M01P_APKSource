package model;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * JNI wrapper for RK OTP Key decryption
 * Uses hardware-stored AES key that cannot be extracted
 */
public class CryptoNative {
    private static final String TAG = "CryptoNative";
    
    // OTP Key IDs
    public static final int KEY0 = 0;
    public static final int KEY1 = 1;
    public static final int KEY2 = 2;
    public static final int KEY3 = 3;
    
    // Encrypted key file path (AES key encrypted by OTP key)
    private static final String ENCRYPTED_KEY_FILE = "/system/bin/aes_enc.bin";
    // IV file path for software AES decryption
    private static final String IV_FILE = "/system/bin/aes_iv.bin";
    
    private static boolean libraryLoaded = false;
    
    static {
        try {
            System.loadLibrary("rkcrypto_jni");
            libraryLoaded = true;
            Log.i(TAG, "librkcrypto_jni loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load librkcrypto_jni: " + e.getMessage());
            libraryLoaded = false;
        }
    }
    
    // Native methods
    private static native int init();
    private static native void deinit();
    private static native int isKeyWritten(int keyId);
    private static native int decryptChunk(int keyId, byte[] iv, byte[] input, byte[] output, int length);
    private static native int decryptFile(int keyId, byte[] iv, String inputPath, String outputPath);
    
    /**
     * Unwrap (decrypt) the AES key using OTP key
     * This is the KEK (Key Encryption Key) approach:
     * 1. Read encrypted key from file (16 bytes)
     * 2. Decrypt it using OTP KEY2 (single OTP call)
     * 3. Return the plaintext key for software AES decryption
     * 
     * @return 16-byte plaintext AES key, or null on error
     */
    public static byte[] unwrapAesKey() {
        if (!libraryLoaded) {
            Log.e(TAG, "Library not loaded");
            return null;
        }
        
        // Read encrypted key file
        File file = new File(ENCRYPTED_KEY_FILE);
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "Encrypted key file not found: " + ENCRYPTED_KEY_FILE);
            return null;
        }
        
        byte[] encryptedKey = new byte[16];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = fis.read(encryptedKey);
            if (bytesRead != 16) {
                Log.e(TAG, "Encrypted key file wrong size: " + bytesRead);
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read encrypted key file", e);
            return null;
        }
        
        // Initialize crypto
        int ret = init();
        if (ret != 0) {
            Log.e(TAG, "rk_crypto_init failed: " + ret);
            return null;
        }
        
        // Check KEY2 is written
        int keyStatus = isKeyWritten(KEY2);
        if (keyStatus != 1) {
            Log.e(TAG, "OTP KEY2 not written, status: " + keyStatus);
            deinit();
            return null;
        }
        
        // Decrypt the key using OTP KEY2 (zero IV, same as encryption)
        byte[] zeroIv = new byte[16];
        byte[] plaintextKey = new byte[16];
        
        ret = decryptChunk(KEY2, zeroIv, encryptedKey, plaintextKey, 16);
        deinit();
        
        if (ret != 0) {
            Log.e(TAG, "Failed to unwrap AES key: " + ret);
            return null;
        }
        
        Log.i(TAG, "AES key unwrapped successfully");
        return plaintextKey;
    }
    
    /**
     * Check if native library is available
     */
    public static boolean isAvailable() {
        return libraryLoaded;
    }
    
    /**
     * Initialize the crypto library
     * Must be called before any decryption
     */
    public static int initialize() {
        if (!libraryLoaded) {
            Log.e(TAG, "Library not loaded");
            return -1;
        }
        return init();
    }
    
    /**
     * Cleanup crypto library resources
     */
    public static void cleanup() {
        if (libraryLoaded) {
            deinit();
        }
    }
    
    /**
     * Check if the specified OTP key slot has been written
     * @param keyId KEY0-KEY3
     * @return 1 if written, 0 if empty, negative on error
     */
    public static int checkKeyWritten(int keyId) {
        if (!libraryLoaded) return -1;
        return isKeyWritten(keyId);
    }
    
    /**
     * Load IV from file
     * @return 16-byte IV array, or null on error
     */
    public static byte[] loadIv() {
        return loadIvFromFile(IV_FILE);
    }
    
    /**
     * Load IV from specified file
     * @param path Path to IV file (16 bytes)
     * @return 16-byte IV array, or null on error
     */
    public static byte[] loadIvFromFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "IV file not found or not readable: " + path);
            return null;
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] iv = new byte[16];
            int bytesRead = fis.read(iv);
            if (bytesRead != 16) {
                Log.e(TAG, "IV file wrong size: " + bytesRead + " bytes");
                return null;
            }
            return iv;
        } catch (IOException e) {
            Log.e(TAG, "Failed to read IV file", e);
            return null;
        }
    }
    
    /**
     * Decrypt a file using OTP key
     * Handles large files (3GB+) via streaming
     * 
     * @param keyId OTP key slot (KEY0-KEY3)
     * @param iv 16-byte initialization vector
     * @param inputPath Path to encrypted file
     * @param outputPath Path to write decrypted file
     * @return 0 on success, negative on error
     */
    public static int decryptFileWithOtpKey(int keyId, byte[] iv, String inputPath, String outputPath) {
        if (!libraryLoaded) {
            Log.e(TAG, "Library not loaded");
            return -1;
        }
        
        if (iv == null || iv.length != 16) {
            Log.e(TAG, "Invalid IV");
            return -2;
        }
        
        Log.i(TAG, "Decrypting: " + inputPath + " -> " + outputPath);
        int ret = decryptFile(keyId, iv, inputPath, outputPath);
        
        if (ret == 0) {
            Log.i(TAG, "Decryption successful");
        } else {
            Log.e(TAG, "Decryption failed: " + ret);
        }
        
        return ret;
    }
    
    /**
     * Convenience method: decrypt using KEY0 and default IV file
     */
    public static int decryptFileDefault(String inputPath, String outputPath) {
        byte[] iv = loadIv();
        if (iv == null) {
            return -1;
        }
        return decryptFileWithOtpKey(KEY0, iv, inputPath, outputPath);
    }
}
