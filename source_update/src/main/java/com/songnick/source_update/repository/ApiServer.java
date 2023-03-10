package com.songnick.source_update.repository;

import android.os.Handler;
import android.os.Looper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.songnick.source_update.SourceUpdateSDK;
import com.songnick.source_update.data.ResourceUpdateInfo;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiServer {

    private static final long DEFAULT_TIME_OUT = 10*1000L;

    private OkHttpClient mClient = null;
    private volatile static ApiServer instance = null;

    private ApiServer(){
        init();
    }

    public static ApiServer instance(){
        if (instance == null){
            synchronized (ApiServer.class){
                if (instance == null){
                    instance = new ApiServer();
                }
            }
        }
        return instance;
    }

    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    public void checkUpdate(Map<String, String> params, UpdateCallback<ResourceUpdateInfo> callback){
        HttpUrl.Builder builder = HttpUrl.get("http://www.10jqka.com.cn/api/app/checkUpdate").newBuilder();
        Iterator<String> iterator = params.keySet().iterator();
        while (iterator.hasNext()){
            String key = iterator.next();
            builder.addQueryParameter(key, params.get(key));
        }
        HttpUrl httpUrl = builder.build();
        Request request = new Request.Builder().url(httpUrl).build();
        SourceUpdateSDK.getInstance().execute(() -> {
            Response response = null;
            try {
                response = mClient.newCall(request).execute();
                int code = response.code();
                if (code != 200){
                    callback.onFail(response.message(), code);
                }
                String bodyStr = response.body().toString();
                //??????????????????
//                String bodyStr = testResult;
                JSONObject jsonObject = JSON.parseObject(bodyStr);
                String data = jsonObject.getString("data");
                if (data != null){
                    try {
                        ResourceUpdateInfo updateInfo = JSON.parseObject(data, ResourceUpdateInfo.class);
                        mMainHandler.post(() -> {
                            callback.onSuccess(updateInfo);
                        });
                    }catch (Exception e){
                        e.printStackTrace();
                        mMainHandler.post(() -> {
                            callback.onFail("????????????", -1);
                        });
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                mMainHandler.post(() -> {
                    callback.onFail("????????????", -1);
                });
                if(response != null){
                    response.close();
                }
            }
        });
    }

    private void init(){
        mClient = new OkHttpClient.Builder()
                .readTimeout(DEFAULT_TIME_OUT, TimeUnit.MILLISECONDS)
                .writeTimeout(DEFAULT_TIME_OUT, TimeUnit.MILLISECONDS)
                .connectTimeout(DEFAULT_TIME_OUT, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();

    }


    private static final String testResult = "{\n" +
            "    \"code\":200,\n" +
            "    \"message\":\"xxxxx\",\n" +
            "    \"data\":{\n" +
            "        \"lastVersion\":\"3.0.1\",\n" +
            "        \"forceUpdate\":false,\n" +
            "        \"title\":\"????????????\",\n" +
            "        \"desc\":\"???????????????????????????\",\n" +
            "        \"resourceInfo\":{\n" +
            "            \"resourceURL\":\"https://github.com/songnick/AppUpdate/archive/refs/heads/main.zip\",\n" +
            "            \"resourceMD5\":\"E0E0F2C83C8768AF141212DBDE89DF46\"\n" +
            "        }\n" +
            "    }\n" +
            "}";
}
