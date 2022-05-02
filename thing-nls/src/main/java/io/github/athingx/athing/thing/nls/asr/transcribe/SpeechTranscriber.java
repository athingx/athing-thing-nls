package io.github.athingx.athing.thing.nls.asr.transcribe;

import io.github.athingx.athing.thing.nls.asr.RecordingFuture;
import io.github.athingx.athing.thing.nls.asr.SentenceHandler;

import javax.sound.sampled.Mixer;
import java.io.Closeable;

/**
 * 语音转录器
 */
public interface SpeechTranscriber extends Closeable {

    /**
     * 语音转录
     *
     * @param mixer   混音器
     * @param option  转录选项
     * @param handler 转录处理器
     * @return 转录Future
     */
    RecordingFuture transcribe(Mixer mixer, SpeechTranscribeOption option, SentenceHandler handler);

}
