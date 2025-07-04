package Threads;

import android.content.Context;

import UpdateFlow.UpdateFlow;
import UpdateMethod.UpdateMethod;

public class UpdateThread {
    private Context sContext;
    private UpdateFlow mUpdateFlow;
    public UpdateThread(Context mContext) {
        sContext = mContext;
        mUpdateFlow = new UpdateFlow(mContext);
    }


    public void Start() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                mUpdateFlow.GameUpdate();
            }
        }).start();
    }

    static String TAGS = "## [KO] UpdateThread";
}
