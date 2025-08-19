package GlobalVar;

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
    public static final String DOWNLOAD_PATH = "/data/download/update/";
    public static final String UPDATE_FILE_PATH = "/data/download/update/update.zip";
    public static final String UPDATE_MD5_PATH = "/data/download/update/info.txt";
    public static final String DEC_UPDATE_FILE_PATH = "/data/tmp/dec_update.zip";
    public static final String MEDIA_PATH = "/data/Media/";
    public static final String TMP_PATH = "/data/tmp/";
    public static final String DATA_PATH = "/data/";
    public static final String RESOURCE_MEDIA_PATH = "/data/tmp/Resource/Media/";
    public static final String RESOURCE_GAME_PATH =  "/data/game.apk";

    public static final String Key = "f2a8b0e7c9d34105";
    public static final String Iv = "7f3e9d0a1b5c8e2f";
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
