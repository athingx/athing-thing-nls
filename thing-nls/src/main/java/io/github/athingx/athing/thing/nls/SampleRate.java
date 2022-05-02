package io.github.athingx.athing.thing.nls;

/**
 * 音频采样率
 */
public enum SampleRate {

    /**
     * 8000Hz
     */
    _8K(8000f),

    /**
     * 16000Hz
     */
    _16K(16000f);

    private final float value;

    SampleRate(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    public static SampleRate valueOf(float value) {
        if (value == _8K.value) {
            return _8K;
        }
        if (value == _16K.value) {
            return _16K;
        }
        return null;
    }

}
