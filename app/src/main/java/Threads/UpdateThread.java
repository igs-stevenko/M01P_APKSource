package Threads;

import android.content.Context;

import java.io.IOException;

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
                try {
                    mUpdateFlow.GameUpdate();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    static String TAGS = "## [KO] UpdateThread";
}
