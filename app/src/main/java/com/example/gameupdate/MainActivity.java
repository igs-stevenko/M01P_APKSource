package com.example.gameupdate;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import GlobalVar.GlobalVar;
import Threads.ProgressThread;
import Threads.TextUpdateThread;
import Threads.UpdateThread;
import ViewCtrl.ViewCtrl;

public class MainActivity extends AppCompatActivity {

    /* Git 測試用 */

    private void init() {
        ViewCtrl.progressBar = findViewById(R.id.progressBar);
        ViewCtrl.ProgressText = (TextView) findViewById(R.id.IGSText);
        ViewCtrl.TitleText = (TextView) findViewById(R.id.Title);
        ViewCtrl.PromptText = (TextView) findViewById(R.id.PromptText);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        View decorView = getWindow().getDecorView();;
        decorView.setSystemUiVisibility(uiOptions);

        init();

        ViewCtrl.ProgressShow();
        new UpdateThread(this).Start();
        new ProgressThread(this).Start();
        new TextUpdateThread(this).Start();

        Log.d(TAGS, "GameUpdate V1.7");
    }
    static String TAGS = "## [KO] MainActivity";
}