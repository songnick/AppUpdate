package com.songnick.source_update;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;


import java.lang.ref.WeakReference;

public final class SourceUpdateSDK {


    private static Application sContext = null;
    private static SDKConfig sSDKConfig = null;

    public static void init(Application app, SDKConfig sdkConfig){
        sContext = app;
        sSDKConfig = sdkConfig;
    }

    public static Application getApp(){

        return sContext;
    }

    public static SDKConfig getSDKConfig(){

        return sSDKConfig;
    }

}
