package ViewCtrl;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ViewCtrl {

    public static TextView ProgressText;
    public static TextView TitleText;
    public static TextView PromptText;
    public static ProgressBar progressBar;
    private static Handler mHandler;

    private static final String CURRENT_PERCENT = "CURRENT_PERCENT";

    public static void SetupProgressBar(int mProgress){
        Bundle bundle = new Bundle();
        Message msg = new Message();
        msg.what = 1;
        bundle.putInt(CURRENT_PERCENT, mProgress);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    public static void SetupTitleTextView(String mString){
        Bundle bundle = new Bundle();
        Message msg = new Message();
        msg.what = 2;
        bundle.putString(CURRENT_PERCENT, mString);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    public static void SetupPromptTextView(String mString){
        Bundle bundle = new Bundle();
        Message msg = new Message();
        msg.what = 3;
        bundle.putString(CURRENT_PERCENT, mString);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    public static void ProgressShow() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int progress = 0;
                String text = "";
                super.handleMessage(msg);
                Bundle bundle = msg.getData();
                switch (msg.what) {
                    case 1:
                        progress = bundle.getInt(CURRENT_PERCENT);
                        ProgressText.setText("" + progress + "%");
                        progressBar.setProgress(progress);
                        break;
                    case 2:
                        text = bundle.getString(CURRENT_PERCENT);
                        TitleText.setText(text);
                        break;
                    case 3:
                        text = bundle.getString(CURRENT_PERCENT);
                        PromptText.setText(text);
                        break;

                    default:
                        break;
                }
            }
        };
    }
}

