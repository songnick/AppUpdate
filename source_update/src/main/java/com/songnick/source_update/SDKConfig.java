package com.songnick.source_update;

public class SDKConfig {

    /***业务对应的key**/
    private String appKey = null;

    private String channel = null;

    private String userId = null;

    public String getAppKey() {
        return appKey;
    }

    public String getChannel() {
        return channel;
    }

    public String getUserId() {
        return userId;
    }

    private SDKConfig(){

    }

    public static final class SDKConfigBuilder {
        private String appKey;
        private String channel;
        private String userId;

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

        public SDKConfigBuilder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public SDKConfig build() {
            SDKConfig sDKConfig = new SDKConfig();
            sDKConfig.appKey = this.appKey;
            sDKConfig.channel = this.channel;
            sDKConfig.userId = this.userId;
            return sDKConfig;
        }
    }
}
