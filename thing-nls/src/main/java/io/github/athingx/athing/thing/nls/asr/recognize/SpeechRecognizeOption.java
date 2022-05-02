package io.github.athingx.athing.thing.nls.asr.recognize;

import io.github.athingx.athing.thing.nls.SpeechOption;

/**
 * 语音识别选项
 */
public class SpeechRecognizeOption extends SpeechOption {

    /**
     * 语音时长限制（默认60秒）
     */
    private long speechTimeLimit = 60000L;

    public long getSpeechTimeLimit() {
        return speechTimeLimit;
    }

    public void setSpeechTimeLimit(long speechTimeLimit) {
        this.speechTimeLimit = speechTimeLimit;
    }

}
