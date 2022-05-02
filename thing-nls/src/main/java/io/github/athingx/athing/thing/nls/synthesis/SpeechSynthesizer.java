package io.github.athingx.athing.thing.nls.synthesis;

import io.github.oldmanpushcart.jpromisor.ListenableFuture;

import javax.sound.sampled.Mixer;
import java.io.Closeable;
import java.nio.channels.WritableByteChannel;

/**
 * 语音合成器
 */
public interface SpeechSynthesizer extends Closeable {

    /**
     * 语音合成
     *
     * @param mixer  混音器
     * @param option 合成选项
     * @param text   文本
     * @return 合成Future
     */
    ListenableFuture<Void> synthesis(Mixer mixer, SpeechSynthesisOption option, String text);

    /**
     * 语音合成
     *
     * @param channel 混音器通道
     * @param option  合成选项
     * @param text    文本
     * @return 合成Future
     */
    ListenableFuture<Void> synthesis(WritableByteChannel channel, SpeechSynthesisOption option, String text);

}
