package Threads;

import android.content.Context;

import GlobalVar.GlobalVar;
import UpdateFlow.UpdateFlow;
import ViewCtrl.ViewCtrl;

public class TextUpdateThread {
    private Context sContext;
    private UpdateFlow mUpdateFlow;
    public TextUpdateThread(Context mContext){

        sContext = mContext;
        mUpdateFlow = new UpdateFlow(mContext);
    }



    public void Start(){
        new Thread(new Runnable() {

            @Override
            public void run() {

                mUpdateFlow.UpdateText();

            }
        }).start();

    }

}