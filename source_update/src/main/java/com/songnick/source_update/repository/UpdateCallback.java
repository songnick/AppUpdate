package com.songnick.source_update.repository;

public interface UpdateCallback<T> {
    /***
     * 错误异常信息
     * @param message
     * @param code
     * */
    void onFail(String message, int code);

    /**
     * 请求成功
     * @param result
     * */
    void onSuccess(T result);
}
