package UpdateMethod;

import android.content.Context;
import android.util.Log;

import java.io.File;

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

        return rtn;
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

    public int UnzipGameFile() {

        int rtn = 0;

        rtn = FileControl.Unzip(GlobalVar.DEC_UPDATE_FILE_PATH, GlobalVar.TMP_PATH);
        if(rtn < 0) {
            return -1;
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

        rtn = Crypto.decryptAESCBCFile(Source, Target, GlobalVar.Key, GlobalVar.Iv);
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
