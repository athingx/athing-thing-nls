package io.github.athingx.athing.thing.nls.aliyun.asr.recognize;

import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import io.github.athingx.athing.thing.nls.SampleRate;
import io.github.athingx.athing.thing.nls.aliyun.ThingNlsConfig;
import io.github.athingx.athing.thing.nls.aliyun.asr.RecordingPromise;
import io.github.athingx.athing.thing.nls.aliyun.sdk.recognizer.AliyunSpeechRecognizer;
import io.github.athingx.athing.thing.nls.aliyun.sdk.recognizer.AliyunSpeechRecognizerListener;
import io.github.athingx.athing.thing.nls.aliyun.sdk.recognizer.AliyunSpeechRecognizerResponse;
import io.github.athingx.athing.thing.nls.aliyun.util.ArgumentUtils;
import io.github.athingx.athing.thing.nls.asr.RecordingFuture;
import io.github.athingx.athing.thing.nls.asr.Sentence;
import io.github.athingx.athing.thing.nls.asr.SentenceHandler;
import io.github.athingx.athing.thing.nls.asr.recognize.SpeechRecognizeOption;
import io.github.athingx.athing.thing.nls.asr.recognize.SpeechRecognizer;
import io.github.oldmanpushcart.jpromisor.FutureFunction;
import io.github.oldmanpushcart.jpromisor.Promise;

import javax.sound.sampled.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_REQUEST_CONFIRMED;

public class SpeechRecognizerImplByAliyun implements SpeechRecognizer {

    private final ThingNlsConfig config;
    private final Executor executor;
    private final String _string;

    private final byte[] data = new byte[10240];
    private final AliyunSpeechRecognizer recognizer;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicReference<Process> processRef = new AtomicReference<>();

    public SpeechRecognizerImplByAliyun(NlsClient client, ThingNlsConfig config, Executor executor) throws Exception {
        this.config = config;
        this.executor = executor;
        this._string = "nls:/%s/recognizer".formatted(config.getAppKey());
        this.recognizer = new AliyunSpeechRecognizer(client, new InnerListener());
    }

    // 获取SampleRate
    private SampleRate getSampleRate(SpeechRecognizeOption option) {
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

    // 设置扩展参数
    private void setupExtOption(SpeechRecognizeOption option) {
        option.option("enable-itn", recognizer::setEnableITN);
        option.option("enable-voice-detection", recognizer::setEnableVoiceDetection);
        if (recognizer.isEnableVoiceDetection()) {
            option.option("max-start-silence", recognizer::setMaxStartSilence);
            option.option("max-end-silence", recognizer::setMaxEndSilence);
        }
    }

    // 清理扩展参数
    private void cleanExtOption() {
        recognizer.setEnableITN(null);
        recognizer.setEnableVoiceDetection(null);
        recognizer.setMaxStartSilence(null);
        recognizer.setMaxEndSilence(null);
    }

    // 打开音频线路
    private TargetDataLine openTargetDataLine(Mixer mixer, AudioFormat format) throws LineUnavailableException {
        final TargetDataLine line = AudioSystem.getTargetDataLine(format, mixer.getMixerInfo());
        line.open(format);
        return line;
    }

    // 是否已开始
    private boolean isStarted() {
        return recognizer.getState() == STATE_REQUEST_CONFIRMED;
    }

    private boolean isTimeLimit(long start, long limit) {
        return System.currentTimeMillis() - start >= limit;
    }

    @Override
    public RecordingFuture recognize(Mixer mixer, SpeechRecognizeOption option, SentenceHandler handler) {
        return new InnerRecordingPromise().execute(promise -> promise.fulfill(executor, (FutureFunction.FutureExecutable) () -> {

            lock.lockInterruptibly();
            try {

                final SampleRate sampleRate = getSampleRate(option);
                final AudioFormat format = getAudioFormat(sampleRate);

                // 设置参数
                recognizer.setAppKey(config.getAppKey());
                recognizer.setFormat(InputFormatEnum.PCM);
                recognizer.setSampleRate((int) sampleRate.getValue());
                setupExtOption(option);

                // 打开音频线路
                try (final TargetDataLine line = openTargetDataLine(mixer, format)) {

                    // 资源准备
                    processRef.set(new Process(promise, handler));
                    line.start();
                    recognizer.start();

                    // 语音识别开始
                    promise.<RecordingPromise>promise().recording(() -> {

                        final long start = System.currentTimeMillis();
                        final long limit = Math.min(option.getSpeechTimeLimit(), 60000);
                        while (!promise.isDone() && isStarted()) {

                            // 如果已经到达录制时长限制，则停止录制
                            if (isTimeLimit(start, limit)) {
                                promise.<RecordingPromise>promise().stopRecording();
                                break;
                            }

                            final int size = line.read(data, 0, data.length);

                            /*
                             * 这里需要加一把锁，因为外边随时可能会进行stop()操作
                             */
                            synchronized (recognizer) {
                                if (isStarted()) {
                                    recognizer.send(data, 0, size);
                                }
                            }

                        }
                    });

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
    public void close() {
        recognizer.close();
    }

    @Override
    public String toString() {
        return _string;
    }

    /**
     * 处理器
     */
    record Process(Promise<Void> promise, SentenceHandler handler) {

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
            synchronized (recognizer) {
                execute(p -> {
                    if (isStarted()) {
                        recognizer.stop();
                    }
                });
            }

            return this;
        }

    }

    /**
     * 语音识别监听器（内部实现）
     */
    class InnerListener extends AliyunSpeechRecognizerListener {

        @Override
        public void onRecognitionResultChanged(AliyunSpeechRecognizerResponse response) {

        }

        @Override
        public void onRecognitionCompleted(AliyunSpeechRecognizerResponse response) {
            final Process process = processRef.get();
            final Promise<Void> promise = process.promise;
            final SentenceHandler handler = process.handler;
            promise.fulfill((FutureFunction.FutureExecutable) () ->
                    handler.onSentence(new Sentence(response.getRecognizedText(), 1d)));
        }

        @Override
        public void onStarted(AliyunSpeechRecognizerResponse response) {

        }

        @Override
        public void onFail(AliyunSpeechRecognizerResponse response) {
            processRef.get().promise.tryException(new Exception("task=%s;code=%s;reason=%s;".formatted(
                    response.getTaskId(),
                    response.getStatus(),
                    response.getStatusText()
            )));
        }

    }

}
