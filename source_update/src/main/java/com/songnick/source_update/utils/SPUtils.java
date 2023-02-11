package com.songnick.source_update.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.songnick.source_update.data.SourceType;
import com.songnick.source_update.SourceUpdateSDK;

public final class SPUtils {

    private static final String KEY_CUR_DOWNLOAD_ID = "download_id";;

    private SharedPreferences sp = null;

    private SPUtils(){
        sp = SourceUpdateSDK.getApp().getSharedPreferences("source_update_sdk", Context.MODE_PRIVATE);
    }

    private static class Holder {
        static SPUtils instance = new SPUtils();
    }

    public static SPUtils getInstance(){

        return Holder.instance;
    }

    public void setCurDownloadId(long id){
        if(id >= 0){
            sp.edit().putLong(KEY_CUR_DOWNLOAD_ID, id).commit();
        }else {
            sp.edit().remove(KEY_CUR_DOWNLOAD_ID).commit();
        }
    }

    public long getCurDownloadId(){

        return sp.getLong(KEY_CUR_DOWNLOAD_ID, -1);
    }

    public void setNextCheckTime(SourceType sourceType, String versionCode, long time){
        String key = new StringBuilder().append(sourceType.ordinal()).append(versionCode).toString();
        sp.edit().putLong(key, time);
    }

    public long getNextCheckTime(SourceType sourceType, String versionCode){
        String key = new StringBuilder().append(sourceType.ordinal()).append(versionCode).toString();

        return sp.getLong(key, -1);
    }



}
