package io.github.athingx.athing.thing.nls.aliyun.synthesis;

import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import io.github.athingx.athing.thing.nls.SampleRate;
import io.github.athingx.athing.thing.nls.aliyun.ThingNlsConfig;
import io.github.athingx.athing.thing.nls.aliyun.sdk.tts.AliyunSpeechSynthesizer;
import io.github.athingx.athing.thing.nls.aliyun.sdk.tts.AliyunSpeechSynthesizerListener;
import io.github.athingx.athing.thing.nls.aliyun.sdk.tts.AliyunSpeechSynthesizerResponse;
import io.github.athingx.athing.thing.nls.aliyun.util.ArgumentUtils;
import io.github.athingx.athing.thing.nls.aliyun.handler.SourceDataChannel;
import io.github.athingx.athing.thing.nls.synthesis.SpeechSynthesisOption;
import io.github.athingx.athing.thing.nls.synthesis.SpeechSynthesizer;
import io.github.oldmanpushcart.jpromisor.FutureFunction;
import io.github.oldmanpushcart.jpromisor.ListenableFuture;
import io.github.oldmanpushcart.jpromisor.Promise;
import io.github.oldmanpushcart.jpromisor.Promisor;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class SpeechSynthesizerImplByAliyun implements SpeechSynthesizer {

    private final ThingNlsConfig config;
    private final Executor executor;
    private final String _string;

    private final AliyunSpeechSynthesizer synthesizer;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicReference<Process> processRef = new AtomicReference<>();


    public SpeechSynthesizerImplByAliyun(NlsClient client, ThingNlsConfig config, Executor executor) throws Exception {
        this.config = config;
        this.executor = executor;
        this._string = String.format("nls:/%s/synthesizer", config.getAppKey());
        this.synthesizer = new AliyunSpeechSynthesizer(client, new InnerListener());
    }

    // ??????SampleRate
    private SampleRate getSampleRate(SpeechSynthesisOption option) {
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

    // ????????????
    private void setupOption(SpeechSynthesisOption option, SampleRate sampleRate) {
        synthesizer.setFormat(OutputFormatEnum.PCM);
        synthesizer.setAppKey(config.getAppKey());
        synthesizer.setSampleRate((int) sampleRate.getValue());
        synthesizer.setSpeechRate(option.getSpeechRate());
        synthesizer.setPitchRate(option.getPitchRate());
        synthesizer.setVolume(option.getVolume());
    }

    // ??????????????????
    private void setupExtOption(SpeechSynthesisOption option) {
        option.option("method", synthesizer::setMethod);
        option.option("voice", synthesizer::setVoice);
    }

    // ??????????????????
    private void cleanExtOption() {
        synthesizer.setMethod(null);
        synthesizer.setVoice(null);
    }

    // ??????????????????
    private SourceDataLine openSourceDataLine(Mixer mixer, AudioFormat format) throws LineUnavailableException {
        final SourceDataLine line = AudioSystem.getSourceDataLine(format, mixer.getMixerInfo());
        line.open(format);
        return line;
    }

    @Override
    public ListenableFuture<Void> synthesis(Mixer mixer, SpeechSynthesisOption option, String text) {
        return new Promisor().promise(promise ->
                promise.fulfill(executor, (FutureFunction.FutureExecutable) () -> {
                    lock.lockInterruptibly();
                    try {

                        final SampleRate sampleRate = getSampleRate(option);
                        final AudioFormat format = getAudioFormat(sampleRate);

                        // ????????????
                        setupOption(option, sampleRate);
                        setupExtOption(option);
                        synthesizer.setText(text);


                        // ??????????????????
                        try (final SourceDataChannel channel = new SourceDataChannel(openSourceDataLine(mixer, format))) {

                            // ????????????
                            processRef.set(new Process(promise, channel));
                            channel.getSourceDataLine().start();
                            synthesizer.start();

                            // ????????????????????????
                            synthesizer.waitForComplete();
                            channel.getSourceDataLine().drain();
                            promise.trySuccess();

                        } finally {
                            processRef.set(null);
                            cleanExtOption();
                        }

                    } finally {
                        lock.unlock();
                    }
                }));
    }

    @Override
    public ListenableFuture<Void> synthesis(WritableByteChannel channel, SpeechSynthesisOption option, String text) {
        return new Promisor().promise(promise ->
                promise.fulfill(executor, (FutureFunction.FutureExecutable) () -> {
                    lock.lockInterruptibly();
                    try {

                        final SampleRate sampleRate = getSampleRate(option);

                        // ????????????
                        setupOption(option, sampleRate);
                        setupExtOption(option);
                        synthesizer.setText(text);

                        // ??????????????????
                        try {

                            // ????????????
                            processRef.set(new Process(promise, channel));
                            synthesizer.start();

                            // ????????????????????????
                            synthesizer.waitForComplete();
                            promise.trySuccess();

                        } finally {
                            processRef.set(null);
                            cleanExtOption();
                        }

                    } finally {
                        lock.unlock();
                    }
                }));
    }

    @Override
    public void close() {
        synthesizer.close();
    }

    @Override
    public String toString() {
        return _string;
    }

    /**
     * ?????????
     */
    record Process(Promise<Void> promise, WritableByteChannel channel) {

    }

    /**
     * ???????????????????????????????????????
     */
    class InnerListener extends AliyunSpeechSynthesizerListener {

        @Override
        public void onComplete(AliyunSpeechSynthesizerResponse response) {
            final Process process = processRef.get();
            process.promise.trySuccess();
        }

        @Override
        public void onFail(AliyunSpeechSynthesizerResponse response) {
            processRef.get().promise.tryException(new Exception("task=%s;code=%s;reason=%s;".formatted(
                    response.getTaskId(),
                    response.getStatus(),
                    response.getStatusText()
            )));
        }

        @Override
        public void onMessage(ByteBuffer buffer) {
            final Process process = processRef.get();
            final WritableByteChannel channel = process.channel;
            final Promise<Void> promise = process.promise;
            try {
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
            } catch (Exception cause) {
                promise.tryException(cause);
            }
        }

    }

}
