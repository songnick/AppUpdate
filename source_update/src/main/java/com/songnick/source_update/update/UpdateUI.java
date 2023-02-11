package com.songnick.source_update.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.songnick.source_update.R;
import com.songnick.source_update.SourceUpdateSDK;
import com.songnick.source_update.data.ResourceUpdateInfo;

import java.lang.ref.WeakReference;

public class UpdateUI {

    private static final String TAG = "UpdateUI";

    private AlertDialog tipDialog = null;
    private AlertDialog progressDialog = null;
    private Handler handler = null;
    private WeakReference<Activity> activityWeakReference = null;
    private long currentId = -1;

    public UpdateUI(WeakReference<Activity> activityWeakReference){
         this.activityWeakReference = activityWeakReference;
        handler = new android.os.Handler(Looper.getMainLooper());
    }

    public void showUpdateDialog(ResourceUpdateInfo info, View.OnClickListener positiveClickListener, View.OnClickListener negativeClickListener ){
        AlertDialog dialog = new AlertDialog.Builder(activityWeakReference.get())
                .setTitle(info.getTitle()).setMessage(info.getDesc())
                .setCancelable(info.isForceUpdate()).setPositiveButton("立即更新", null)
                .setNegativeButton("暂时不更新", (dialogInterface, i) -> {
                    dismissUpdateTip();
                    if (negativeClickListener != null){
                        negativeClickListener.onClick(null);
                    }

                })
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            positiveClickListener.onClick(view);
            dismissUpdateTip();
        });
        tipDialog = dialog;
    }

    private void dismissUpdateTip(){
        if(tipDialog != null && tipDialog.isShowing()){
            tipDialog.dismiss();
            tipDialog = null;
        }
    }

    public void showProgress(long downloadingId){
        if (tipDialog != null && tipDialog.isShowing()){
            tipDialog.dismiss();
            tipDialog = null;
        }
        currentId = downloadingId;
        AlertDialog dialog = new AlertDialog.Builder(activityWeakReference.get()).setView(R.layout.sdk_view_progress).show();
        progressDialog = dialog;
        progressDialog.show();
        handler.postDelayed(updateProgress, 1000);
    }

    public void hideProgress(){
        if (progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
    }

    public void showInstallDialog(boolean forceUpdate, DialogInterface.OnClickListener listener){
        if (progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        AlertDialog dialog = new AlertDialog.Builder(activityWeakReference.get())
                .setTitle(R.string.sdk_update_alert_title_download)
                .setMessage(R.string.sdk_update_alert_title_download_content)
                .setCancelable(forceUpdate)
                .setPositiveButton(
                        R.string.sdk_update_alert_ok_install, listener
                ).create();
            dialog.show();
    }

    private Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (progressDialog != null){
                ProgressBar progressBar = progressDialog.findViewById(R.id.sdk_progress);
                DownloadManager downloadManager = (DownloadManager) SourceUpdateSDK.getApp().getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query().setFilterById(currentId);
                Cursor cursor = downloadManager.query(query);
                cursor.moveToFirst();
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_RUNNING
                        || status == DownloadManager.STATUS_PENDING
                ){
                    int soFar = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int totalSize = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    int progress = (int)(soFar*1.0f/totalSize);
                    progressBar.setProgress(progress);
                    Log.i(TAG, " status:  " + status +  "progress: " + progress + soFar + "////" + totalSize);
                    handler.postDelayed(updateProgress, 1000);
                }else{
                    hideProgress();
                    if (activityWeakReference.get() != null){
                        Toast.makeText(activityWeakReference.get(), "下载出现异常", Toast.LENGTH_LONG).show();
                    }
                }
                Log.i(TAG, " status:  " + status);

            }
        }
    };


}
