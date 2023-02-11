package com.songnick.source_update;

/**
 * 执行升级回调接口
 * */
public interface UpdateAction {
    /***
     * 点击执行按钮
     * @param sourcePath 待升级的资源文件路径
     * */
    void performUpdate(String sourcePath);

    /***
     * 升级过程中出现异常
     * @param message
     * @param code
     * */
    default void updateError(String message, int code) {

    }
}
