package com.songnick.source_update;

public class SDKConfig {

    /***业务对应的key**/
    private String appKey = null;

    private String channel = null;

    public String getAppKey() {
        return appKey;
    }

    public String getChannel() {
        return channel;
    }

    private SDKConfig(){

    }

    public static final class SDKConfigBuilder {
        private String appKey;
        private String channel;

        public SDKConfigBuilder() {
        }

        public SDKConfigBuilder withAppKey(String appKey) {
            this.appKey = appKey;
            return this;
        }

        public SDKConfigBuilder withChannel(String channel) {
            this.channel = channel;
            return this;
        }

        public SDKConfig build() {
            SDKConfig sDKConfig = new SDKConfig();
            sDKConfig.appKey = this.appKey;
            sDKConfig.channel = this.channel;
            return sDKConfig;
        }
    }
}
