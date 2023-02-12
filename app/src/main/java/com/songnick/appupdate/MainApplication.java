package com.songnick.appupdate;

import android.app.Application;

import com.songnick.source_update.CheckUpdateManager;
import com.songnick.source_update.SDKConfig;
import com.songnick.source_update.SourceUpdateSDK;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SDKConfig sdkConfig = new SDKConfig.SDKConfigBuilder()
                                .withAppKey("Android")
                                .withChannel("Official")
                                .withUserId("xxxxxx")
                                .build();
        SourceUpdateSDK.getInstance().init(this, sdkConfig);
        //apk升级检测
        CheckUpdateManager.instance().checkAppUpdate();
    }
}
