package com.songnick.source_update;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.songnick.source_update.data.ResourceUpdateInfo;
import com.songnick.source_update.data.SourceType;
import com.songnick.source_update.repository.ApiServer;
import com.songnick.source_update.repository.UpdateCallback;
import com.songnick.source_update.update.SourceDownloader;
import com.songnick.source_update.update.UpdateUI;
import com.songnick.source_update.utils.MD5Utils;
import com.songnick.source_update.utils.SPUtils;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;


import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CheckUpdateManager {

    private static final String TAG = "CheckUpdateManager";

    private WeakReference<Activity> acReference = null;
    private WeakReference<UpdateAction> callbackReference = null;
    private static volatile CheckUpdateManager instance = null;
    private ExecutorService executor = null;
    private SourceType sourceType = null;
    private String versionCode = null;
    private boolean needCheck = false;
    private Handler handler = null;

    private CheckUpdateManager(){
        init();
    }

    public static CheckUpdateManager instance(){
        if (instance == null){
            synchronized (CheckUpdateManager.class){
                if (instance == null){
                    instance = new CheckUpdateManager();
                }
            }
        }
        return instance;
    }

    private void init(){
        executor = Executors.newSingleThreadExecutor();
        SourceUpdateSDK.getApp().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
                acReference = new WeakReference<>(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {

            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                //交互页面可能会销毁，所以在onCreate方法获取的activity会被销毁
                acReference = new WeakReference<>(activity);
                if (needCheck && !TextUtils.isEmpty(versionCode) && sourceType != null){
                    checkUpdate(sourceType, versionCode, callbackReference.get());
                    needCheck = false;
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        });
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * 检测App升级接口，文件下载完成直接调用apk安装逻辑
     * */
    public void checkAppUpdate(){
        int appCode = -1;
        try {
            Application app = SourceUpdateSDK.getApp();
            PackageManager packageManager = app.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(app.getPackageName(), 0);
            appCode = packageInfo.versionCode;
        }catch (Exception e){
            e.printStackTrace();
        }
        Log.i(TAG, " code : " + appCode);
        if (appCode > 0){
            checkUpdate(SourceType.APK, String.valueOf(appCode), new UpdateAction() {
                @Override
                public void performUpdate(String sourcePath) {
                    //执行安装
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(Uri.fromFile(new File(sourcePath)), "application/vnd.android.package-archive");
                    SourceUpdateSDK.getApp().startActivity(intent);
                }
            });
        }
    }

    /***
     * 检查是否存在需要资源升级的接口
     * @param sourceType 检测的资源类型{@link SourceType}
     * @param versionCode 对应资源当前版本号(int类型)
     * */
    public void checkUpdate(SourceType sourceType,@NonNull String versionCode, UpdateAction callback){
        //sp读取数据会有性能问题，需要优化
        if (SPUtils.getInstance().getNextCheckTime(sourceType,versionCode) > System.currentTimeMillis()){
            Log.i(TAG, "未到检测时间");
            return;
        }
        callbackReference = new WeakReference<>(callback);
        //应用未处于用户交互页面，无法进行弹窗显示
        if (acReference == null){
            this.sourceType = sourceType;
            this.versionCode = versionCode;
            needCheck = true;
            return;
        }
        needCheck = false;

        Map<String, String> params = createBaseParams();
        params.put("sourceType", String.valueOf(sourceType.ordinal()));
        if(!TextUtils.isEmpty(versionCode)){
            params.put("versionCode", versionCode);
        }
        ApiServer.instance().checkUpdate(params, new UpdateCallback<ResourceUpdateInfo>() {
            @Override
            public void onFail(String message, int code) {
                Log.i(TAG, " 服务端请求异常: " + message + " code: " + code);
                if (callbackReference.get() != null){
                    callbackReference.get().updateError(message, code);
                }
//                callback.onFail(message, code);
            }

            @Override
            public void onSuccess(ResourceUpdateInfo result) {
                if (result != null){
                    //校验本地是否存在已下载的资源升级文件
                    ResourceUpdateInfo.ResourceInfo info = result.getResourceInfo();
                    if (info != null && info.resourceURL != null){
                        UpdateUI updateUI = new UpdateUI(acReference);
                        updateUI.showUpdateDialog(result, view -> {
                            String downloadPath = SourceDownloader.getDownloadPath(info);
                            if (new File(downloadPath).exists()){
                                checkAndShowInstallDialog(downloadPath, updateUI, result);
                            }else{
                                startDownload(result, updateUI);
                            }
                        }, view -> {
                            executor.execute(() -> {
                                //存储再次检测的时间
                                long time = System.currentTimeMillis() + result.getUpdateInterval();
                                SPUtils.getInstance()
                                        .setNextCheckTime(sourceType, versionCode, time);
                            });
                        });
                    }
                }


            }
        });
    }

    @SuppressLint("WrongConstant")
    private void startDownload(ResourceUpdateInfo result, UpdateUI updateUI) {
        if (AndPermission.hasPermissions(acReference.get(), Permission.Group.STORAGE)){
            start(result, updateUI);
        }else{
            AndPermission.with(acReference.get())
                    .runtime()
                    .permission(Permission.Group.STORAGE)
                    .onGranted(permissions -> {
                        start(result, updateUI);
                    })
                    .onDenied(permissions -> {
                        // Storage permission are not allowed.
                    })
                    .start();
        }
    }

    private void checkAndShowInstallDialog(String path, UpdateUI updateUI, ResourceUpdateInfo info) {
        executor.execute(() -> {
            try {
                if (path != null) {
                    File file = new File(path);
                    if (file != null && file.exists()) {
                        String md5 = MD5Utils.getMD5OfFile(file);
                        String resourceMD5 = info.getResourceInfo().resourceMD5;
                        if (!TextUtils.isEmpty(md5) && md5.equals(resourceMD5)) {
                            showInstallDialog(updateUI, info, path);
                            return;
                        }
                        showToast(acReference.get().getResources().getString(R.string.sdk_update_md5_error));
                    } else {
                        showToast("文件下载异常");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void showInstallDialog(UpdateUI updateUI, ResourceUpdateInfo info, String path){
        handler.post(() -> {
            updateUI.showInstallDialog(info.isForceUpdate(), (dialogInterface, i) -> {
                if (callbackReference != null && callbackReference.get() != null) {
                    callbackReference.get().performUpdate(path);
                }
            });
        });
    }

    private void showToast(String message){
        if (acReference != null && acReference.get() != null){
            handler.post(() -> {Toast.makeText(acReference.get(),message, Toast.LENGTH_LONG).show();});
        }
    }

    private void start(ResourceUpdateInfo info, UpdateUI updateUI){
        SourceDownloader downloader = new SourceDownloader(info);
        long downloadId = downloader.starDownload(new UpdateCallback<File>() {
            @Override
            public void onFail(String message, int code) {
                updateUI.hideProgress();
                Toast.makeText(acReference.get(), message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSuccess(File result) {
                checkAndShowInstallDialog(result.getAbsolutePath(), updateUI, info);
            }
        });
        updateUI.showProgress(downloadId);
    }

    private Map<String, String> createBaseParams(){
        Map<String, String> params = new HashMap<>();
        Application app = SourceUpdateSDK.getApp();
        params.put("appId", app.getPackageName());
        params.put("platform", "Android");
        params.put("countryCode", app.getResources().getConfiguration().locale.getCountry());
        params.put("appKey", SourceUpdateSDK.getSDKConfig().getAppKey());
        params.put("channel", SourceUpdateSDK.getSDKConfig().getChannel());
        params.put("userId", SourceUpdateSDK.getSDKConfig().getUserId());
        return params;
    }

    /***
     * 调用方主动释放资源
     * */
    public synchronized void release(){
        if (!executor.isShutdown()){
            executor.shutdownNow();
        }
        executor = null;
        acReference.clear();
        acReference = null;
        instance = null;
    }
}
