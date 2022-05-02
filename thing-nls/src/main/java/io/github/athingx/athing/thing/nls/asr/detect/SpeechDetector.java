package io.github.athingx.athing.thing.nls.asr.detect;

import io.github.oldmanpushcart.jpromisor.ListenableFuture;

import javax.sound.sampled.Mixer;
import java.io.Closeable;
import java.nio.channels.ReadableByteChannel;

public interface SpeechDetector extends Closeable {

    /**
     * 唤醒检测
     *
     * @param mixer  混音器
     * @param option 检测选项
     * @return 唤醒检测Future
     */
    ListenableFuture<Void> detectWakeUp(Mixer mixer, SpeechDetectWakeUpOption option);

    /**
     * 唤醒检测
     *
     * @param channel 混音器通道
     * @param option  检测选项
     * @return 唤醒检测Future
     */
    ListenableFuture<Void> detectWakeUp(ReadableByteChannel channel, SpeechDetectWakeUpOption option);

}
