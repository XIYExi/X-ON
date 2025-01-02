package com.ruoyi.oauth.wx.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WxMiniAppConstant {
    @Value("${oauth.wx.miniapp.appId}")
    private String appId;

    @Value("${oauth.wx.miniapp.appSecret}")
    private String appSecret;

    @Value("${oauth.wx.miniapp.url}")
    private String url;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

}
