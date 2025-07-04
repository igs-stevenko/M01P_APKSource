package Threads;

import android.content.Context;

import UpdateFlow.UpdateFlow;
import ViewCtrl.ViewCtrl;
import GlobalVar.GlobalVar;

public class ProgressThread {

    private Context sContext;
    UpdateFlow mUpdateFlow;
    public ProgressThread(Context mContext){
        sContext = mContext;
        mUpdateFlow = new UpdateFlow(mContext);
    }

    public void Start(){
        new Thread(new Runnable() {

            @Override
            public void run() {

                mUpdateFlow.UpdateProgress();

            }
        }).start();
    }

    private void Sleep(int mTime) {
        try {
            Thread.sleep(mTime);
        }catch(Exception e) {

        }
    }

    static String TAGS = "## [KO] ProgressThread";
}
