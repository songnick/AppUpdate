package com.songnick.appupdate;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.songnick.source_update.CheckUpdateManager;
import com.songnick.source_update.UpdateAction;
import com.songnick.source_update.data.SourceType;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //
//        CheckUpdateManager.instance().checkUpdate(SourceType.PROGRAM, "00002", new UpdateAction() {
//            @Override
//            public void performUpdate(String sourcePath) {
//
//            }
//        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //apk升级检测
//        CheckUpdateManager.instance().checkAppUpdate();
    }
}