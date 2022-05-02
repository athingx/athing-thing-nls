package io.github.athingx.athing.thing.nls.asr;

import io.github.oldmanpushcart.jpromisor.ListenableFuture;

/**
 * 录音Future
 */
public interface RecordingFuture extends ListenableFuture<Void> {

    /**
     * 停止录音
     */
    RecordingFuture stopRecording();

}
