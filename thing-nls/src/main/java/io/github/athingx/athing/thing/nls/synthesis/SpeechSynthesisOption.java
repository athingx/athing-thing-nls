package io.github.athingx.athing.thing.nls.synthesis;

import io.github.athingx.athing.thing.nls.SpeechOption;

/**
 * 语音合成选项
 */
public class SpeechSynthesisOption extends SpeechOption {

    /**
     * 音量：0 ~ 100，默认值50
     */
    private int volume = 50;

    /**
     * 语速：-500 ~ +500，默认值0
     */
    private int speechRate = 0;

    /**
     * 语调：-500 ~ +500，默认值0
     */
    private int pitchRate = 0;

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public int getSpeechRate() {
        return speechRate;
    }

    public void setSpeechRate(int speechRate) {
        this.speechRate = speechRate;
    }

    public int getPitchRate() {
        return pitchRate;
    }

    public void setPitchRate(int pitchRate) {
        this.pitchRate = pitchRate;
    }

}
