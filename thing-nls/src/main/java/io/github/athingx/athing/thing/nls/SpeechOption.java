package io.github.athingx.athing.thing.nls;

import java.util.Properties;
import java.util.function.Consumer;

/**
 * 语音选项
 */
public class SpeechOption {

    /**
     * 扩展属性
     */
    private final Properties properties = new Properties();

    /**
     * 语音采样率
     */
    private SampleRate sampleRate;

    public void set(String key, Object value) {
        properties.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) properties.get(key);
    }

    public <T> void option(String key, Consumer<T> fn) {
        if (properties.containsKey(key)) {
            fn.accept(get(key));
        }
    }

    public SampleRate getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(SampleRate sampleRate) {
        this.sampleRate = sampleRate;
    }

}
