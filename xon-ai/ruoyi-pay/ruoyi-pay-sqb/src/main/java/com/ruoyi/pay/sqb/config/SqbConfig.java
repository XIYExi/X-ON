package com.ruoyi.pay.sqb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pay.sqb", name = "enabled", havingValue = "true")
public class SqbConfig {
    @Value("${pay.sqb.apiDomain}")
    private String apiDomain;

    @Value("${pay.sqb.terminalSn}")
    private String terminalSn;

    @Value("${pay.sqb.terminalKey}")
    private String terminalKey;

    @Value("${pay.sqb.appId}")
    private String appId;

    @Value("${pay.sqb.vendorSn}")
    private String vendorSn;

    @Value("${pay.sqb.vendorKey}")
    private String vendorKey;

    @Value("${pay.sqb.notifyUrl}")
    private String defaultNotifyUrl;

    @Value("${pay.sqb.notifyBaseUrl}")
    private String defaultNotifyBaseUrl;

    @Value("${pay.sqb.proxy}")
    private String proxyPath;

    public String getDefaultNotifyUrl() {
        return defaultNotifyUrl;
    }

    public void setDefaultNotifyUrl(String defaultNotifyUrl) {
        this.defaultNotifyUrl = defaultNotifyUrl;
    }

    public String getDefaultNotifyBaseUrl() {
        return defaultNotifyBaseUrl;
    }

    public void setDefaultNotifyBaseUrl(String defaultNotifyBaseUrl) {
        this.defaultNotifyBaseUrl = defaultNotifyBaseUrl;
    }

    public String getProxyPath() {
        return proxyPath;
    }

    public void setProxyPath(String proxyPath) {
        this.proxyPath = proxyPath;
    }

    public String getApiDomain() {
        return apiDomain;
    }

    public void setApiDomain(String apiDomain) {
        this.apiDomain = apiDomain;
    }

    public String getTerminalSn() {
        return terminalSn;
    }

    public void setTerminalSn(String terminalSn) {
        this.terminalSn = terminalSn;
    }

    public String getTerminalKey() {
        return terminalKey;
    }

    public void setTerminalKey(String terminalKey) {
        this.terminalKey = terminalKey;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getVendorSn() {
        return vendorSn;
    }

    public void setVendorSn(String vendorSn) {
        this.vendorSn = vendorSn;
    }

    public String getVendorKey() {
        return vendorKey;
    }

    public void setVendorKey(String vendorKey) {
        this.vendorKey = vendorKey;
    }
}
