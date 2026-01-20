package UpdateFlow;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import GlobalVar.GlobalVar;
import UpdateMethod.UpdateMethod;
import ViewCtrl.ViewCtrl;
import model.FileControl;

public class UpdateFlow {

    private Context sContext;
    private UpdateMethod mUpdateMethod;
    private int UpdateStatus;
    public UpdateFlow(Context mContext){
        sContext = mContext;
        mUpdateMethod = new UpdateMethod(mContext);
    }

    public void UpdateText() {

        int toggle = 0;

        while(true) {

            if (GlobalVar.GetUpdateStatus() == GlobalVar.UPDATE_START) {

                if (toggle == 0) {
                    toggle = 1;
                    ViewCtrl.SetupTitleTextView("Updating.");
                } else if (toggle == 1) {
                    toggle = 2;
                    ViewCtrl.SetupTitleTextView("Updating..");
                } else if (toggle == 2) {
                    toggle = 0;
                    ViewCtrl.SetupTitleTextView("Updating...");
                }
            } else if (GlobalVar.GetUpdateStatus() == GlobalVar.UPDATE_SUCCESS) {
                ViewCtrl.SetupTitleTextView("Update Finish !!");
                ViewCtrl.SetupPromptTextView("Update Finish, Reboot Now");
                break;
            } else if(GlobalVar.GetUpdateStatus() < 0){
                ViewCtrl.SetupTitleTextView("Update Failed !!" + GlobalVar.GetUpdateStatus());
                ViewCtrl.SetupPromptTextView("Update failed. Reboot this machine to try again. &" + GlobalVar.GetUpdateStatus());
                break;
            }

            Sleep(500);
        }
    }

    public void UpdateProgress(){
        int i = 0;
        long TotalSpent = 0;
        long SleepTime = 0;

        /* 等待取到檔案長度 */
        while(GlobalVar.UpdateFileLen == 0){
            Sleep(500);
        }

        /* 算出總共需要幾秒 */
        TotalSpent = GlobalVar.UpdateFileLen / 2500000;
        SleepTime = (TotalSpent*1000) / 100;
        if(SleepTime < 500) SleepTime = 100;

        Log.d(TAGS, "SleepTime = " + SleepTime);

        while (true) {

            Sleep((int)SleepTime);
            if(GlobalVar.GetUpdateStatus() == GlobalVar.UPDATE_SUCCESS){
                i = 100;
                ViewCtrl.SetupProgressBar(100);
                break;

            }else if(GlobalVar.GetUpdateStatus() < 0){
                break;
            }else if(i < 100) {
                ViewCtrl.SetupProgressBar(i);
            }
            else if(i >= 100) {
                i = 100;
                if(GlobalVar.GetUpdateStatus() == GlobalVar.UPDATE_SUCCESS) {
                    ViewCtrl.SetupProgressBar(100);
                    break;
                }
            }
            i++;
        }
    }
    public void GameUpdate() throws IOException {

        int rtn = 0;

        mUpdateMethod.GetUpdateFileLen();

        GlobalVar.SetUpdateStatus(GlobalVar.UPDATE_START);

        Log.d(TAGS, "CheckGameFile Start");
        rtn = mUpdateMethod.CheckGameFile();
        if(rtn < 0){
            GlobalVar.SetUpdateStatus(GlobalVar.UPDATE_CHECK_FAILED);
            return;
        }

        Log.d(TAGS, "RemoveTmp Start");
        rtn = mUpdateMethod.RemoveTmp();
        if(rtn < 0){
            GlobalVar.SetUpdateStatus(GlobalVar.UPDATE_RMTMP_FAILED);
            return;
        }

        Log.d(TAGS, "DecrpytGameFile Start");
        rtn = mUpdateMethod.DecrpytGameFile();
        if(rtn < 0){
            GlobalVar.SetUpdateStatus(GlobalVar.UPDATE_DEC_FAILED);
            return;
        }

        Log.d(TAGS, "RemoveMedia Start");
        rtn = mUpdateMethod.RemoveMedia();
        if(rtn < 0){
            GlobalVar.SetUpdateStatus(GlobalVar.UPDATE_RMMEDIA_FAILED);
            return;
        }


        Log.d(TAGS, "UnzipGameFile Start");
        rtn = mUpdateMethod.UnzipGameFile();
        if(rtn < 0){
            GlobalVar.SetUpdateStatus(GlobalVar.UPDATE_UNZIP_FAILED);
            return;
        }

        /*
        Log.d(TAGS, "CopyToMedia Start");
        rtn = mUpdateMethod.CopyToMedia();
        if(rtn < 0){
            GlobalVar.SetUpdateStatus(GlobalVar.UPDATE_COPY_FAILED);
            return;
        }
        */

        rtn = mUpdateMethod.CopyOTAData();
        if(rtn < 0){
            GlobalVar.SetUpdateStatus(GlobalVar.UPDATE_COPYOTADATA_FAILED);
            return;
        }

        Log.d(TAGS, "ReinstallApp Start");
        rtn = mUpdateMethod.ReinstallApp(GlobalVar.RESOURCE_GAME_PATH);
        if(rtn < 0){
            Log.d(TAGS, "UPDATE_INSTALL_APP_FAILED");
            GlobalVar.SetUpdateStatus(GlobalVar.UPDATE_INSTALL_APP_FAILED);
            return;
        }

        Log.d(TAGS, "RemoveTmp Start");
        rtn = mUpdateMethod.RemoveTmp();
        if(rtn < 0){
            GlobalVar.SetUpdateStatus(GlobalVar.UPDATE_RMTMP_FAILED);
            return;
        }

        Log.d(TAGS, "RemoveDownload Start");
        rtn = mUpdateMethod.RemoveDownload();
        if(rtn < 0){
            Log.d(TAGS, "UPDATE_RMDOWNLOAD_FAILED");
            GlobalVar.SetUpdateStatus(GlobalVar.UPDATE_RMDOWNLOAD_FAILED);
            return;
        }

        GlobalVar.SetUpdateStatus(GlobalVar.UPDATE_SUCCESS);
        Log.d(TAGS, "UPDATE_SUCCESS");
        Sleep(3000);
        Log.d(TAGS, "GetUpdateStatus() = " + GlobalVar.GetUpdateStatus());
    }
    private void Sleep(int mTime) {
        try {
            Thread.sleep(mTime);
        }catch(Exception e) {

        }
    }


    static String TAGS = "## [KO] UpdateFlow";
}
