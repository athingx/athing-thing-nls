package io.github.athingx.athing.thing.nls.asr.recognize;

import io.github.athingx.athing.thing.nls.asr.RecordingFuture;
import io.github.athingx.athing.thing.nls.asr.SentenceHandler;

import javax.sound.sampled.Mixer;
import java.io.Closeable;
import java.nio.channels.ReadableByteChannel;

/**
 * 语音识别器
 */
public interface SpeechRecognizer extends Closeable {

    /**
     * 识别短句
     *
     * @param mixer   混音器
     * @param option  识别选项
     * @param handler 识别处理器
     * @return 识别Future
     */
    RecordingFuture recognize(Mixer mixer, SpeechRecognizeOption option, SentenceHandler handler);

    /**
     * 识别短句
     *
     * @param channel 混音器通道
     * @param option  识别选项
     * @param handler 识别处理器
     * @return 识别Future
     */
    RecordingFuture recognize(ReadableByteChannel channel, SpeechRecognizeOption option, SentenceHandler handler);

}
