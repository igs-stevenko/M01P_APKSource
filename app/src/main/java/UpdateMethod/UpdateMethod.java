package UpdateMethod;

import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import model.ApkControl;
import model.Crypto;
import model.FileControl;
import GlobalVar.GlobalVar;
public class UpdateMethod {

    private Context sContext;
    private ApkControl mApkControl;

    public UpdateMethod(Context mContext) {
        sContext = mContext;
        mApkControl = new ApkControl(mContext);
    }

    public void GetUpdateFileLen(){
        GlobalVar.UpdateFileLen = FileControl.GetFileSize(GlobalVar.UPDATE_FILE_PATH);
    }


    public int CheckGameFile() {

        //Log.d(TAGS, "CheckGameFile");

        int rtn = 0;
        boolean UpdateFileExist;
        boolean Md5FileExist;

        UpdateFileExist = FileControl.IsFileExist(GlobalVar.UPDATE_FILE_PATH);
        Md5FileExist = FileControl.IsFileExist(GlobalVar.UPDATE_MD5_PATH);

        if (UpdateFileExist == false || Md5FileExist == false) {
            return -1;
        }

        if(CheckFile(GlobalVar.UPDATE_FILE_PATH, GlobalVar.UPDATE_MD5_PATH) < 0) {
            return -2;
        }

        return rtn;

    }

    public int RemoveTmp() {

        //Log.d(TAGS, "RemoveTmp");

        int rtn = 0;

        rtn = FileControl.RemoveFolder(GlobalVar.TMP_PATH);
        if(rtn < 0){
            return -1;
        }

        return rtn;
    }

    public int DecrpytGameFile() {

        //Log.d(TAGS, "DecrpytGameFile");

        int rtn = 0;

        rtn = DecryptFile(GlobalVar.UPDATE_FILE_PATH, GlobalVar.DEC_UPDATE_FILE_PATH);
        if(rtn < 0) {
            return -1;
        }

        // MD5 校驗
        String calcMd5 = calcFileMd5(GlobalVar.DEC_UPDATE_FILE_PATH);
        if (calcMd5 == null) {
            Log.e(TAGS, "Failed to calculate MD5 of decrypted file");
            return -1;
        }

        String expectedMd5 = readExpectedMd5("/mnt/download/update/PF.txt");
        if (expectedMd5 == null) {
            Log.e(TAGS, "Failed to read expected MD5 from PF.txt");
            return -1;
        }

        Log.d(TAGS, "Calculated MD5: " + calcMd5);
        Log.d(TAGS, "Expected MD5:   " + expectedMd5);

        if (!calcMd5.equalsIgnoreCase(expectedMd5)) {
            Log.e(TAGS, "MD5 mismatch! Decryption verification failed.");
            return -1;
        }

        Log.d(TAGS, "MD5 verification passed.");
        return rtn;
    }

    private String calcFileMd5(String filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (FileInputStream fis = new FileInputStream(filePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.e(TAGS, "calcFileMd5 error", e);
            return null;
        }
    }

    private String readExpectedMd5(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            String content = new String(data, "UTF-8").trim();
            // 支援 md5sum 格式 "hash  filename"，只取前 32 字元
            if (content.length() >= 32) {
                return content.substring(0, 32);
            }
            return content;
        } catch (IOException e) {
            Log.e(TAGS, "readExpectedMd5 error", e);
            return null;
        }
    }

    public int RemoveMedia() {

        int rtn = 0;

        //Log.d(TAGS, "RemoveMedia");

        rtn = FileControl.RemoveFolder(GlobalVar.MEDIA_PATH);
        if(rtn < 0){
            return -1;
        }

        return rtn;
    }

    public int UnzipGameFile() throws IOException {

        int rtn = 0;
        int mode = OsConstants.S_IRWXU | OsConstants.S_IRWXG | OsConstants.S_IRWXO;

        rtn = FileControl.UnzipWithoutFirstName(GlobalVar.DEC_UPDATE_FILE_PATH, GlobalVar.DATA_PATH);
        if(rtn < 0) {
            Log.d(TAGS, "UnzipWithoutFirstName failed");
            return -1;
        }

        Log.d(TAGS, "chmod start");

        try {
            Os.chmod(GlobalVar.MEDIA_PATH, mode);
        } catch (ErrnoException e) {
            Log.d(TAGS, "chmod failed");
            return -2;
        }

        Log.d(TAGS, "UnzipGameFile");

        return 0;
    }



    public int CopyToMedia() {

        int rtn = 0;

        rtn = FileControl.CopyAllDir(GlobalVar.RESOURCE_MEDIA_PATH, GlobalVar.MEDIA_PATH);
        if(rtn < 0) {
            return -1;
        }

        return rtn;
    }

    public int CopyOTAData() {

        int rtn = 0;
        boolean FileExist;
        FileExist = FileControl.IsFileExist(GlobalVar.DOWNLOAD_PATH + GlobalVar.OTA_DATA_PROP);
        if (FileExist == true) {
            rtn = FileControl.CopyFile(GlobalVar.DOWNLOAD_PATH + GlobalVar.OTA_DATA_PROP, GlobalVar.DATA_DOWNLOAD_PATH + GlobalVar.OTA_DATA_PROP);
            if (rtn < 0) {
                return -1;
            }
        }

        FileExist = FileControl.IsFileExist(GlobalVar.DOWNLOAD_PATH + GlobalVar.OTA_DATA_XML);
        if (FileExist == true) {
            rtn = FileControl.CopyFile(GlobalVar.DOWNLOAD_PATH + GlobalVar.OTA_DATA_XML, GlobalVar.DATA_DOWNLOAD_PATH + GlobalVar.OTA_DATA_XML);
            if (rtn < 0) {
                return -1;
            }
        }


        return rtn;
    }

    public int ReinstallApp(String Source) {

        String InstallPkgName = mApkControl.GetApkPkgName(Source);
        if(InstallPkgName == null){
            return -1;
        }

        Log.d(TAGS, "InstallPkgName : " + InstallPkgName);

        int rtn = 0;

        Sleep(1000);

        rtn = mApkControl.install_app(InstallPkgName, Source);
        if(rtn != 0){
            return -2;
        }

        while(mApkControl.isAppInstalled(InstallPkgName) != true){
            Sleep(1000);
        }

        FileControl.RemoveFile(Source);

        /* 看RelForLauncher.txt是否存在，若存在則刪掉，重新建立一個 */
        if(FileControl.IsFileExist(GlobalVar.RelForLauncherFilePath) == true) {
            FileControl.RemoveFile(GlobalVar.RelForLauncherFilePath);
        }

        Sleep(1000);

        /* 寫入APP的Package Name到RelForLauncher.txt */
        rtn = FileControl.WriteStringToFile(InstallPkgName,GlobalVar.RelForLauncherFilePath);
        if(rtn != 0){
            return -3;
        }

        return rtn;
    }

    public int RemoveDownload(){

        int rtn = 0;

        //Log.d(TAGS, "RemoveDownload");

        rtn = FileControl.RemoveFolder(GlobalVar.DOWNLOAD_PATH);
        if(rtn < 0){
            return -1;
        }

        return rtn;
    }

    private int CheckFile(String SourceFile, String MD5File) {

        int rtn = 0;

        String Md5String = FileControl.CalculateMD5(SourceFile);
        String Md5InInfo = FileControl.ReadStringFromFile(MD5File);
        if (Md5String.equals(Md5InInfo.toString()) == true) {
            //Log.d(TAGS, "Same Md5 : " + Md5String);
            rtn = 0;
        } else {
            //Log.d(TAGS, "Different Md5 ");
            rtn = -1;
        }
        return rtn;
    }

    public int DecryptFile(String Source, String Target) {

        int rtn = 0;

        String key = GlobalVar.getKey();
        String iv = GlobalVar.getIv();
        if (key == null || iv == null) {
            Log.d(TAGS, "AES key or IV not available, cannot decrypt");
            return -1;
        }

        Log.d(TAGS, "AES Key: " + key);
        Log.d(TAGS, "AES IV: " + iv);

        rtn = Crypto.decryptAESCBCFile(Source, Target, key, iv);
        if(rtn < 0) {
            return -1;
        }
        return rtn;

    }



    private void Sleep(int mTime) {
        try {
            Thread.sleep(mTime);
        } catch (Exception e) {

        }
    }

    static String TAGS = "## [KO] UpdateMethod";
}
