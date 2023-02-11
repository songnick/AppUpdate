package com.songnick.source_update.update;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.songnick.source_update.SourceUpdateSDK;
import com.songnick.source_update.data.ResourceUpdateInfo;
import com.songnick.source_update.repository.UpdateCallback;
import com.songnick.source_update.utils.MD5Utils;
import com.songnick.source_update.utils.SPUtils;

import java.io.File;

public class SourceDownloader {

    private static final String TAG = "SourceDownloader";

    private static final String DIR_UPDATE_SOURCE = "update_source";

    private ResourceUpdateInfo mUpdateInfo = null;
    private UpdateCallback<File> callback;
    private Handler mHandler = null;

    public SourceDownloader(ResourceUpdateInfo info ){
        this.mUpdateInfo = info;
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        SourceUpdateSDK.getApp().registerReceiver(new DownloadReceiver(), filter);
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static String getDownloadPath(ResourceUpdateInfo.ResourceInfo info){
        String name = null;
        if (info != null && info.resourceURL != null){
            String[] strings = info.resourceURL.split("/");
            name = strings[strings.length - 1];
        }
        return SourceUpdateSDK.getApp().getPackageName() + "/" + DIR_UPDATE_SOURCE + "/" + name;
    }

    /***
     * 启动下载流程
     * */
    public long starDownload(UpdateCallback<File> callback){
        this.callback = callback;
        DownloadManager downloadManager = (DownloadManager) SourceUpdateSDK.getApp().getSystemService(Context.DOWNLOAD_SERVICE);
        //current is downloading
        ResourceUpdateInfo info = this.mUpdateInfo;
        Uri uri = Uri.parse(info.getResourceInfo().resourceURL);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        String[] strings = info.getResourceInfo().resourceURL.split("/");
        String downloadDir = getDownloadPath(info.getResourceInfo()) ;
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE | DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadDir)
                .setAllowedOverRoaming(false);
        long downloadId = downloadManager.enqueue(request);
        SPUtils.getInstance().setCurDownloadId(downloadId);
        return downloadId;
    }

    public class DownloadReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("DownloadReceiver", " action : " + intent.getAction());
            if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)){
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                DownloadManager downloadManager =
                        (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                Uri uri = downloadManager.getUriForDownloadedFile(id);
                if (uri == null){
                    callback.onFail("文件下载异常", -1);
                    return;
                }
                new Thread(() -> {
                    String path = getPath(context, uri);
                    try {
                        if (path != null){
                            File file = new File(path);
                            if (file != null && file.exists()){
                                mHandler.post(() -> {callback.onSuccess(file);});
                                return;
                            }
                        }
                        mHandler.post(() -> callback.onFail("文件下载异常", -1));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    private String getPath(Context context, Uri uri){
        String path = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Downloads.DATA}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (columnIndex >=0 ) {
                        path = cursor.getString(columnIndex);
                    }
                }
                cursor.close();
            }
        }
        return path;
    }

}
