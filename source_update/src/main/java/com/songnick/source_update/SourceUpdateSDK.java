package com.songnick.source_update;

import android.app.Application;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class SourceUpdateSDK {


    private  Application mContext = null;
    private  SDKConfig mSDKConfig = null;
    private  ThreadPoolExecutor mExecutor = null;

    public static class Holder{
        static SourceUpdateSDK instance = new SourceUpdateSDK();
    }

    public static SourceUpdateSDK getInstance(){
        return Holder.instance;
    }

    public void init(Application app, SDKConfig sdkConfig){
        mContext = app;
        mSDKConfig = sdkConfig;
    }

    public Application getApp(){

        return mContext;
    }

    public SDKConfig getSDKConfig(){

        return mSDKConfig;
    }

    /***
     * 统一线程池管理
     * @param runnable
     * */
    public void execute(Runnable runnable){
        if (mExecutor == null){
            mExecutor = new ThreadPoolExecutor(
                    5,
                    10,
                    1,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue(10)
            );
        }
        mExecutor.execute(runnable);
    }

    /****
     * 主动释放初始化使用的资源
     * */
    public void release(){
        if (mExecutor != null){
            mExecutor.shutdown();
            mExecutor = null;
        }
    }

}
