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

    // 获取SampleRate
    private SampleRate getSampleRate(SpeechTranscribeOption option) {
        return ArgumentUtils.getByOrder(
                option.getSampleRate(),
                config.getSampleRate(),
                SampleRate._8K
        );
    }

    // 获取AudioFormat
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

    // 设置扩展参数
    private void setupExtOption(SpeechTranscribeOption option) {
        option.option("enable-itn", transcriber::setEnableITN);
    }

    // 清理扩展参数
    private void cleanExtOption() {
        transcriber.setEnableITN(null);
    }

    // 是否已开始
    private boolean isStarted() {
        return transcriber.getState() == STATE_REQUEST_CONFIRMED;
    }

    // 打开音频线路
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
                 * 这里需要加一把锁，因为外边随时可能会进行stop()操作
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

                // 设置参数
                setupOption(sampleRate);
                setupExtOption(option);

                // 语音识别开始
                try (final TargetDataLine line = openTargetDataLine(mixer, format)) {

                    // 资源准备
                    processRef.set(new Process(promise, handler));
                    line.start();
                    transcriber.start();

                    // 语音识别开始
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

                // 设置参数
                setupOption(sampleRate);
                setupExtOption(option);

                // 语音识别开始
                try {

                    // 资源准备
                    processRef.set(new Process(promise, handler));
                    transcriber.start();

                    // 语音识别开始
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
     * 录音Future（内部实现）
     */
    class InnerRecordingPromise extends RecordingPromise {

        @Override
        public RecordingFuture stopRecording() {
            if (!isInRecording()) {
                return this;
            }

            /*
             * 这里需要加一把锁，因为此时可能正在send()数据
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
     * 处理器
     */
    record Process(Promise<Void> promise, SentenceHandler handler) {

    }

    /**
     * 语音识别监听器（内部实现）
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
