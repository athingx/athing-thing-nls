package io.github.athingx.athing.thing.nls.aliyun.asr;

import io.github.athingx.athing.thing.nls.asr.RecordingFuture;
import io.github.oldmanpushcart.jpromisor.impl.NotifiableFuture;

abstract public class RecordingPromise extends NotifiableFuture<Void> implements RecordingFuture {

    private volatile boolean isInRecording = false;

    public void recording(RecordingAction action) throws Exception {

        isInRecording = true;
        try {
            action.action();
        } finally {
            isInRecording = false;
        }

    }

    public boolean isInRecording() {
        return isInRecording;
    }

    @FunctionalInterface
    public interface RecordingAction {

        void action() throws Exception;

    }

}
