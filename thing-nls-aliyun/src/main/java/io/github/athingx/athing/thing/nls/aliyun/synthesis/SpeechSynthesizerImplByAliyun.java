package io.github.athingx.athing.thing.nls.aliyun.synthesis;

import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import io.github.athingx.athing.thing.nls.SampleRate;
import io.github.athingx.athing.thing.nls.aliyun.ThingNlsConfig;
import io.github.athingx.athing.thing.nls.aliyun.sdk.tts.AliyunSpeechSynthesizer;
import io.github.athingx.athing.thing.nls.aliyun.sdk.tts.AliyunSpeechSynthesizerListener;
import io.github.athingx.athing.thing.nls.aliyun.sdk.tts.AliyunSpeechSynthesizerResponse;
import io.github.athingx.athing.thing.nls.aliyun.util.ArgumentUtils;
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

    // 获取SampleRate
    private SampleRate getSampleRate(SpeechSynthesisOption option) {
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
    private void setupExtOption(SpeechSynthesisOption option) {
        option.option("method", synthesizer::setMethod);
        option.option("voice", synthesizer::setVoice);
    }

    // 清理扩展参数
    private void cleanExtOption() {
        synthesizer.setMethod(null);
        synthesizer.setVoice(null);
    }

    // 打开音频线路
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

                        // 设置参数
                        synthesizer.setFormat(OutputFormatEnum.PCM);
                        synthesizer.setAppKey(config.getAppKey());
                        synthesizer.setSampleRate((int) sampleRate.getValue());
                        synthesizer.setSpeechRate(option.getSpeechRate());
                        synthesizer.setPitchRate(option.getPitchRate());
                        synthesizer.setVolume(option.getVolume());
                        synthesizer.setText(text);
                        setupExtOption(option);


                        // 打开音频线路
                        try (final SourceDataLine line = openSourceDataLine(mixer, format)) {

                            // 资源准备
                            processRef.set(new Process(promise, line));
                            line.start();
                            synthesizer.start();

                            // 等待语音合成结束
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
    public ListenableFuture<Void> synthesis(WritableByteChannel channel, SpeechSynthesisOption option, String text) {
        return null;
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
     * 处理器
     */
    record Process(Promise<Void> promise, SourceDataLine line) {

    }

    /**
     * 语音合成监听器（内部实现）
     */
    class InnerListener extends AliyunSpeechSynthesizerListener {

        private final byte[] data = new byte[10240];

        @Override
        public void onComplete(AliyunSpeechSynthesizerResponse response) {
            final Process process = processRef.get();
            process.line.drain();
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
            final SourceDataLine source = process.line;
            final Promise<Void> promise = process.promise;
            try {
                while (buffer.hasRemaining()) {
                    final int length = Math.min(data.length, buffer.remaining());
                    buffer.get(data, 0, length);
                    int offset = 0;
                    do {
                        offset += source.write(data, offset, length - offset);
                    } while (offset < length);
                }
            } catch (Exception cause) {
                promise.tryException(cause);
            }
        }

    }

}
