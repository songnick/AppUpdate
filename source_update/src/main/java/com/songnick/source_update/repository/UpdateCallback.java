package com.songnick.source_update.repository;

public interface UpdateCallback<T> {
    void onFail(String message, int code);

    void onSuccess(T result);
}
