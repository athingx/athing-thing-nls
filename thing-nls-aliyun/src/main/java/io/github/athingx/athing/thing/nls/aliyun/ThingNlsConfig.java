package io.github.athingx.athing.thing.nls.aliyun;

import io.github.athingx.athing.thing.nls.SampleRate;

/**
 * 语音识别配置
 */
public class ThingNlsConfig {


    private String accessKeyId;
    private String accessKeySecret;
    private String appKey;

    private String remote = "wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1";
    private SampleRate sampleRate = SampleRate._8K;

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public SampleRate getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(SampleRate sampleRate) {
        this.sampleRate = sampleRate;
    }

}
