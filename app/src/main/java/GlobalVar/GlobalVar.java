package GlobalVar;

import java.io.FileInputStream;
import java.io.IOException;

public class GlobalVar {
    public static final int UPDATE_START  = 0;
    public static final int UPDATE_SUCCESS  = 1;
    public static final int UPDATE_CHECK_FAILED  = -1;
    public static final int UPDATE_RMTMP_FAILED  = -2;
    public static final int UPDATE_DEC_FAILED  = -3;
    public static final int UPDATE_RMMEDIA_FAILED  = -4;
    public static final int UPDATE_UNZIP_FAILED  = -5;
    public static final int UPDATE_COPY_FAILED  = -6;
    public static final int UPDATE_INSTALL_APP_FAILED  = -7;
    public static final int UPDATE_RMDOWNLOAD_FAILED  = -8;
    public static final int UPDATE_COPYOTADATA_FAILED  = -9;
    public static final String DOWNLOAD_PATH = "/mnt/download/update/";
    public static final String UPDATE_FILE_PATH = "/mnt/download/update/update.zip";
    public static final String UPDATE_MD5_PATH = "/mnt/download/update/info.txt";
    public static final String DEC_UPDATE_FILE_PATH = "/data/tmp/dec_update.zip";
    public static final String OTA_DATA_PROP = "otaprop.properties";
    public static final String OTA_DATA_XML = "OTA_Service.xml";
    public static final String DATA_DOWNLOAD_PATH = "/data/download/";

    public static final String MEDIA_PATH = "/data/Media/";
    public static final String TMP_PATH = "/data/tmp/";
    public static final String DATA_PATH = "/data/";
    public static final String RESOURCE_MEDIA_PATH = "/data/tmp/Resource/Media/";
    public static final String RESOURCE_GAME_PATH =  "/data/game.apk";

    private static final String AES_KEY_FILE = "/system/bin/aes_key.bin";
    private static String Key = null;
    private static String Iv = null;
    private static boolean keyLoaded = false;

    public static synchronized String getKey() {
        if (!keyLoaded) {
            loadKeyFromFile();
        }
        return Key;
    }

    public static synchronized String getIv() {
        if (!keyLoaded) {
            loadKeyFromFile();
        }
        return Iv;
    }

    private static void loadKeyFromFile() {
        keyLoaded = true;
        java.io.File file = new java.io.File(AES_KEY_FILE);
        if (!file.exists() || !file.canRead()) {
            android.util.Log.e("GlobalVar", "AES key file not found or not readable: " + AES_KEY_FILE);
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[32];
            int bytesRead = fis.read(data);
            if (bytesRead >= 32) {
                Key = new String(data, 0, 16, "UTF-8");
                Iv = new String(data, 16, 16, "UTF-8");
            } else {
                android.util.Log.e("GlobalVar", "AES key file too short: " + bytesRead + " bytes");
            }
        } catch (IOException e) {
            android.util.Log.e("GlobalVar", "Failed to read AES key file", e);
        }
    }
    public static int UpdateStatus;

    public static long UpdateFileLen = 0;

    public static final String RelForLauncherFilePath = "/data/media/RelForLauncher.txt";

    private static final Object lock = new Object();;

    public static void SetUpdateStatus(int status) {
        synchronized (lock) {
            UpdateStatus = status;
        }

    }

    public static int GetUpdateStatus() {
        synchronized (lock) {
            return UpdateStatus;
        }

    }

}
