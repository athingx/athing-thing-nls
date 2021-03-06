package io.github.athingx.athing.thing.nls.aliyun.asr.transcribe;

import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import io.github.athingx.athing.thing.nls.SampleRate;
import io.github.athingx.athing.thing.nls.aliyun.ThingNlsConfig;
import io.github.athingx.athing.thing.nls.aliyun.asr.RecordingPromise;
import io.github.athingx.athing.thing.nls.aliyun.handler.TargetDataChannel;
import io.github.athingx.athing.thing.nls.aliyun.sdk.transcriber.AliyunSpeechTranscriber;
import io.github.athingx.athing.thing.nls.aliyun.sdk.transcriber.AliyunSpeechTranscriberListener;
import io.github.athingx.athing.thing.nls.aliyun.sdk.transcriber.AliyunSpeechTranscriberResponse;
import io.github.athingx.athing.thing.nls.aliyun.util.ArgumentUtils;
import io.github.athingx.athing.thing.nls.asr.RecordingFuture;
import io.github.athingx.athing.thing.nls.asr.Sentence;
import io.github.athingx.athing.thing.nls.asr.SentenceHandler;
import io.github.athingx.athing.thing.nls.asr.transcribe.SpeechTranscribeOption;
import io.github.athingx.athing.thing.nls.asr.transcribe.SpeechTranscriber;
import io.github.oldmanpushcart.jpromisor.FutureFunction;
import io.github.oldmanpushcart.jpromisor.Promise;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_REQUEST_CONFIRMED;

public class SpeechTranscriberImplByAliyun implements SpeechTranscriber {

    private final ThingNlsConfig config;
    private final Executor executor;
    private final String _string;

    private final ByteBuffer buffer = ByteBuffer.allocate(10240);
    private final AliyunSpeechTranscriber transcriber;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicReference<Process> processRef = new AtomicReference<>();

    public SpeechTranscriberImplByAliyun(NlsClient client, ThingNlsConfig config, Executor executor) throws Exception {
        this.config = config;
        this.executor = executor;
        this._string = "nls:/%s/transcriber".formatted(config.getAppKey());
        this.transcriber = new AliyunSpeechTranscriber(client, new InnerListener());
    }

    // ??????SampleRate
    private SampleRate getSampleRate(SpeechTranscribeOption option) {
        return ArgumentUtils.getByOrder(
                option.getSampleRate(),
                config.getSampleRate(),
                SampleRate._8K
        );
    }

    // ??????AudioFormat
    private AudioFormat getAudioFormat(SampleRate sampleRate) {
        return new AudioFormat(
                sampleRate.getValue(),
                16,
                1,
                true,
                false
        );
    }

    private void setupOption(SampleRate sampleRate) {
        transcriber.setAppKey(config.getAppKey());
        transcriber.setFormat(InputFormatEnum.PCM);
        transcriber.setSampleRate((int) sampleRate.getValue());
    }

    // ??????????????????
    private void setupExtOption(SpeechTranscribeOption option) {
        option.option("enable-itn", transcriber::setEnableITN);
    }

    // ??????????????????
    private void cleanExtOption() {
        transcriber.setEnableITN(null);
    }

    // ???????????????
    private boolean isStarted() {
        return transcriber.getState() == STATE_REQUEST_CONFIRMED;
    }

    // ??????????????????
    private TargetDataLine openTargetDataLine(Mixer mixer, AudioFormat format) throws LineUnavailableException {
        final TargetDataLine line = AudioSystem.getTargetDataLine(format, mixer.getMixerInfo());
        line.open(format);
        return line;
    }

    private void recordingLoop(RecordingPromise promise, ReadableByteChannel channel) throws Exception {
        promise.<RecordingPromise>promise().recording(() -> {
            while (!promise.isDone() && isStarted()) {

                buffer.clear();
                channel.read(buffer);
                buffer.flip();

                /*
                 * ????????????????????????????????????????????????????????????stop()??????
                 */
                synchronized (transcriber) {
                    if (isStarted()) {
                        transcriber.send(buffer.array(), buffer.arrayOffset(), buffer.remaining());
                    }
                }

            }
        });
    }

    @Override
    public RecordingFuture transcribe(Mixer mixer, SpeechTranscribeOption option, SentenceHandler handler) {
        return new InnerRecordingPromise().execute(promise -> promise.fulfill(executor, (FutureFunction.FutureExecutable) () -> {
            lock.lockInterruptibly();
            try {

                final SampleRate sampleRate = getSampleRate(option);
                final AudioFormat format = getAudioFormat(sampleRate);

                // ????????????
                setupOption(sampleRate);
                setupExtOption(option);

                // ??????????????????
                try (final TargetDataLine line = openTargetDataLine(mixer, format)) {

                    // ????????????
                    processRef.set(new Process(promise, handler));
                    line.start();
                    transcriber.start();

                    // ??????????????????
                    recordingLoop(promise.promise(), new TargetDataChannel(line));


                } finally {
                    processRef.set(null);
                    cleanExtOption();
                }


            } finally {
                lock.unlock();
            }
        })).future();
    }

    @Override
    public RecordingFuture transcribe(ReadableByteChannel channel, SpeechTranscribeOption option, SentenceHandler handler) {
        return new InnerRecordingPromise().execute(promise -> promise.fulfill(executor, (FutureFunction.FutureExecutable) () -> {
            lock.lockInterruptibly();
            try {

                final SampleRate sampleRate = getSampleRate(option);

                // ????????????
                setupOption(sampleRate);
                setupExtOption(option);

                // ??????????????????
                try {

                    // ????????????
                    processRef.set(new Process(promise, handler));
                    transcriber.start();

                    // ??????????????????
                    recordingLoop(promise.promise(), channel);

                } finally {
                    processRef.set(null);
                    cleanExtOption();
                }

            } finally {
                lock.unlock();
            }
        })).future();
    }

    @Override
    public String toString() {
        return _string;
    }

    @Override
    public void close() {
        transcriber.close();
    }

    /**
     * ??????Future??????????????????
     */
    class InnerRecordingPromise extends RecordingPromise {

        @Override
        public RecordingFuture stopRecording() {
            if (!isInRecording()) {
                return this;
            }

            /*
             * ???????????????????????????????????????????????????send()??????
             */
            synchronized (transcriber) {
                execute(p -> {
                    if (isStarted()) {
                        transcriber.stop();
                    }
                });
            }

            return this;
        }

    }

    /**
     * ?????????
     */
    record Process(Promise<Void> promise, SentenceHandler handler) {

    }

    /**
     * ???????????????????????????????????????
     */
    class InnerListener extends AliyunSpeechTranscriberListener {

        @Override
        public void onTranscriberStart(AliyunSpeechTranscriberResponse response) {

        }

        @Override
        public void onSentenceBegin(AliyunSpeechTranscriberResponse response) {

        }

        @Override
        public void onSentenceEnd(AliyunSpeechTranscriberResponse response) {
            final Process process = processRef.get();
            final Promise<Void> promise = process.promise;
            final SentenceHandler handler = process.handler;
            promise.execute(p -> handler.onSentence(
                    new Sentence(response.getTransSentenceText(), response.getConfidence())
            ));
        }

        @Override
        public void onTranscriptionResultChange(AliyunSpeechTranscriberResponse response) {

        }

        @Override
        public void onTranscriptionComplete(AliyunSpeechTranscriberResponse response) {
            processRef.get().promise.trySuccess();
        }

        @Override
        public void onFail(AliyunSpeechTranscriberResponse response) {
            processRef.get().promise.tryException(new Exception("task=%s;code=%s;reason=%s;".formatted(
                    response.getTaskId(),
                    response.getStatus(),
                    response.getStatusText()
            )));
        }

    }

}
