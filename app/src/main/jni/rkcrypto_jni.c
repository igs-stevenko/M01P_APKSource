/*
 * JNI wrapper for librkcrypto OTP Key cipher
 * Provides AES decryption using OTP-stored key
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <android/log.h>

#include "rkcrypto_core.h"
#include "rkcrypto_otp_key.h"
#include "rkcrypto_common.h"

#define LOG_TAG "RKCryptoJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define CHUNK_SIZE (1 * 1024 * 1024)  // 1MB chunks
#define REINIT_THRESHOLD (256 * 1024 * 1024)  // Reinit TEE every 256MB to avoid timeout

static int g_initialized = 0;
static size_t g_bytes_since_reinit = 0;

/*
 * Remove PKCS7 padding from decrypted data
 * Returns the actual data length after removing padding, or -1 on error
 */
static int pkcs7_unpad(uint8_t *data, int len)
{
    if (len < 16) return -1;
    
    uint8_t pad_byte = data[len - 1];
    
    // Padding byte must be 1-16
    if (pad_byte == 0 || pad_byte > 16) {
        LOGE("Invalid PKCS7 padding byte: 0x%02x", pad_byte);
        return -1;
    }
    
    // Verify all padding bytes are correct
    for (int i = 0; i < pad_byte; i++) {
        if (data[len - 1 - i] != pad_byte) {
            LOGE("Invalid PKCS7 padding at position %d", i);
            return -1;
        }
    }
    
    return len - pad_byte;
}

/*
 * Initialize librkcrypto
 */
JNIEXPORT jint JNICALL
Java_model_CryptoNative_init(JNIEnv *env, jclass clazz) {
    if (g_initialized) {
        LOGI("Already initialized");
        return 0;
    }
    
    int ret = rk_crypto_init();
    if (ret != 0) {
        LOGE("rk_crypto_init failed: 0x%08x", ret);
        return ret;
    }
    
    g_initialized = 1;
    LOGI("rk_crypto_init success");
    return 0;
}

/*
 * Deinitialize librkcrypto
 */
JNIEXPORT void JNICALL
Java_model_CryptoNative_deinit(JNIEnv *env, jclass clazz) {
    if (g_initialized) {
        rk_crypto_deinit();
        g_initialized = 0;
        LOGI("rk_crypto_deinit done");
    }
}

/*
 * Check if OTP key is written
 */
JNIEXPORT jint JNICALL
Java_model_CryptoNative_isKeyWritten(JNIEnv *env, jclass clazz, jint keyId) {
    uint8_t is_written = 0;
    int ret = rk_oem_otp_key_is_written((enum RK_OEM_OTP_KEYID)keyId, &is_written);
    if (ret != 0) {
        LOGE("rk_oem_otp_key_is_written failed: 0x%08x", ret);
        return -1;
    }
    return is_written ? 1 : 0;
}

/*
 * Decrypt a chunk of data using OTP key
 * Returns 0 on success, negative on error
 */
JNIEXPORT jint JNICALL
Java_model_CryptoNative_decryptChunk(JNIEnv *env, jclass clazz,
                                      jint keyId,
                                      jbyteArray ivArray,
                                      jbyteArray inputArray,
                                      jbyteArray outputArray,
                                      jint length) {
    if (!g_initialized) {
        LOGE("Not initialized");
        return -1;
    }
    
    jbyte *iv = (*env)->GetByteArrayElements(env, ivArray, NULL);
    jbyte *input = (*env)->GetByteArrayElements(env, inputArray, NULL);
    jbyte *output = (*env)->GetByteArrayElements(env, outputArray, NULL);
    
    if (!iv || !input || !output) {
        LOGE("Failed to get byte array elements");
        if (iv) (*env)->ReleaseByteArrayElements(env, ivArray, iv, JNI_ABORT);
        if (input) (*env)->ReleaseByteArrayElements(env, inputArray, input, JNI_ABORT);
        if (output) (*env)->ReleaseByteArrayElements(env, outputArray, output, JNI_ABORT);
        return -2;
    }
    
    rk_cipher_config config;
    memset(&config, 0, sizeof(config));
    config.algo = RK_ALGO_AES;
    config.mode = RK_CIPHER_MODE_CBC;
    config.operation = RK_OP_CIPHER_DEC;
    config.key_len = 16;  // AES-128
    config.reserved = NULL;
    memcpy(config.iv, iv, 16);
    
    int ret = rk_oem_otp_key_cipher_virt(
        (enum RK_OEM_OTP_KEYID)keyId,
        &config,
        (uint8_t *)input,
        (uint8_t *)output,
        length
    );
    
    (*env)->ReleaseByteArrayElements(env, ivArray, iv, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, inputArray, input, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, outputArray, output, 0);  // Copy back
    
    if (ret != 0) {
        LOGE("rk_oem_otp_key_cipher_virt failed: 0x%08x", ret);
        return ret;
    }
    
    return 0;
}

/*
 * Decrypt entire file using OTP key (streaming, handles large files)
 * Automatically removes PKCS7 padding from the last block
 * Returns 0 on success, negative on error
 */
JNIEXPORT jint JNICALL
Java_model_CryptoNative_decryptFile(JNIEnv *env, jclass clazz,
                                     jint keyId,
                                     jbyteArray ivArray,
                                     jstring inputPath,
                                     jstring outputPath) {
    if (!g_initialized) {
        LOGE("Not initialized");
        return -1;
    }
    
    const char *inPath = (*env)->GetStringUTFChars(env, inputPath, NULL);
    const char *outPath = (*env)->GetStringUTFChars(env, outputPath, NULL);
    jbyte *initialIv = (*env)->GetByteArrayElements(env, ivArray, NULL);
    
    if (!inPath || !outPath || !initialIv) {
        LOGE("Failed to get parameters");
        if (inPath) (*env)->ReleaseStringUTFChars(env, inputPath, inPath);
        if (outPath) (*env)->ReleaseStringUTFChars(env, outputPath, outPath);
        if (initialIv) (*env)->ReleaseByteArrayElements(env, ivArray, initialIv, JNI_ABORT);
        return -2;
    }
    
    FILE *fin = fopen(inPath, "rb");
    if (!fin) {
        LOGE("Cannot open input file: %s", inPath);
        (*env)->ReleaseStringUTFChars(env, inputPath, inPath);
        (*env)->ReleaseStringUTFChars(env, outputPath, outPath);
        (*env)->ReleaseByteArrayElements(env, ivArray, initialIv, JNI_ABORT);
        return -3;
    }
    
    // Get file size
    fseek(fin, 0, SEEK_END);
    long file_size = ftell(fin);
    fseek(fin, 0, SEEK_SET);
    
    FILE *fout = fopen(outPath, "wb");
    if (!fout) {
        LOGE("Cannot open output file: %s", outPath);
        fclose(fin);
        (*env)->ReleaseStringUTFChars(env, inputPath, inPath);
        (*env)->ReleaseStringUTFChars(env, outputPath, outPath);
        (*env)->ReleaseByteArrayElements(env, ivArray, initialIv, JNI_ABORT);
        return -4;
    }
    
    uint8_t *chunk_in = malloc(CHUNK_SIZE);
    uint8_t *chunk_out = malloc(CHUNK_SIZE);
    uint8_t iv[16];
    memcpy(iv, initialIv, 16);
    
    if (!chunk_in || !chunk_out) {
        LOGE("Failed to allocate buffers");
        if (chunk_in) free(chunk_in);
        if (chunk_out) free(chunk_out);
        fclose(fin);
        fclose(fout);
        (*env)->ReleaseStringUTFChars(env, inputPath, inPath);
        (*env)->ReleaseStringUTFChars(env, outputPath, outPath);
        (*env)->ReleaseByteArrayElements(env, ivArray, initialIv, JNI_ABORT);
        return -5;
    }
    
    rk_cipher_config config;
    memset(&config, 0, sizeof(config));
    config.algo = RK_ALGO_AES;
    config.mode = RK_CIPHER_MODE_CBC;
    config.operation = RK_OP_CIPHER_DEC;
    config.key_len = 16;
    config.reserved = NULL;
    
    int ret = 0;
    size_t bytes_read;
    size_t total_read = 0;
    g_bytes_since_reinit = 0;  // Reset counter for this file
    
    while ((bytes_read = fread(chunk_in, 1, CHUNK_SIZE, fin)) > 0) {
        // Ensure 16-byte alignment for AES
        if (bytes_read % 16 != 0) {
            LOGE("Data not 16-byte aligned: %zu bytes", bytes_read);
            ret = -6;
            break;
        }
        
        // Reinitialize TEE session periodically to avoid timeout on large files
        if (g_bytes_since_reinit >= REINIT_THRESHOLD) {
            LOGI("Reinitializing TEE session at offset %zu", total_read);
            rk_crypto_deinit();
            ret = rk_crypto_init();
            if (ret != 0) {
                LOGE("rk_crypto_init failed during reinit: 0x%08x", ret);
                break;
            }
            g_bytes_since_reinit = 0;
        }
        
        memcpy(config.iv, iv, 16);
        
        ret = rk_oem_otp_key_cipher_virt(
            (enum RK_OEM_OTP_KEYID)keyId,
            &config,
            chunk_in,
            chunk_out,
            bytes_read
        );
        
        // If failed, try reinit and retry up to 3 times
        if (ret != 0) {
            for (int retry = 0; retry < 3 && ret != 0; retry++) {
                LOGI("Decrypt failed (0x%08x), reinit and retry %d/3 at offset %zu", 
                     ret, retry + 1, total_read);
                rk_crypto_deinit();
                
                // Small delay to let TEE stabilize after deinit
                usleep(100000);  // 100ms
                
                ret = rk_crypto_init();
                if (ret != 0) {
                    LOGE("rk_crypto_init failed during retry: 0x%08x", ret);
                    continue;
                }
                g_bytes_since_reinit = 0;
                
                // Clear output buffer before retry
                memset(chunk_out, 0, bytes_read);
                
                // Retry the decrypt
                memcpy(config.iv, iv, 16);
                ret = rk_oem_otp_key_cipher_virt(
                    (enum RK_OEM_OTP_KEYID)keyId,
                    &config,
                    chunk_in,
                    chunk_out,
                    bytes_read
                );
                
                if (ret == 0) {
                    LOGI("Retry %d/3 succeeded at offset %zu", retry + 1, total_read);
                }
            }
        }
        
        if (ret != 0) {
            LOGE("Decrypt failed at offset %zu: 0x%08x", total_read, ret);
            break;
        }
        
        // CBC mode: next IV = last ciphertext block
        memcpy(iv, chunk_in + bytes_read - 16, 16);
        
        total_read += bytes_read;
        g_bytes_since_reinit += bytes_read;  // Track bytes for reinit threshold
        
        // Check if this is the last chunk
        if (total_read >= (size_t)file_size) {
            // Last chunk - remove PKCS7 padding
            int unpadded_len = pkcs7_unpad(chunk_out, bytes_read);
            if (unpadded_len < 0) {
                LOGE("PKCS7 unpadding failed, writing raw data");
                fwrite(chunk_out, 1, bytes_read, fout);
            } else {
                LOGI("Removed %zu bytes of PKCS7 padding", bytes_read - unpadded_len);
                fwrite(chunk_out, 1, unpadded_len, fout);
            }
        } else {
            // Not last chunk - write full data
            fwrite(chunk_out, 1, bytes_read, fout);
        }
    }
    
    LOGI("Decrypted %zu bytes (input size: %ld)", total_read, file_size);
    
    free(chunk_in);
    free(chunk_out);
    fclose(fin);
    fclose(fout);
    (*env)->ReleaseStringUTFChars(env, inputPath, inPath);
    (*env)->ReleaseStringUTFChars(env, outputPath, outPath);
    (*env)->ReleaseByteArrayElements(env, ivArray, initialIv, JNI_ABORT);
    
    return ret;
}
