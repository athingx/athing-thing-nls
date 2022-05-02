package io.github.athingx.athing.thing.nls.aliyun.asr.detect;

import io.github.athingx.athing.thing.nls.SampleRate;
import io.github.athingx.athing.thing.nls.aliyun.ThingNlsConfig;
import io.github.athingx.athing.thing.nls.aliyun.asr.detect.snowboy.Snowboy;
import io.github.athingx.athing.thing.nls.aliyun.util.ArgumentUtils;
import io.github.athingx.athing.thing.nls.asr.detect.SpeechDetectWakeUpOption;
import io.github.athingx.athing.thing.nls.asr.detect.SpeechDetector;
import io.github.oldmanpushcart.jpromisor.FutureFunction;
import io.github.oldmanpushcart.jpromisor.ListenableFuture;
import io.github.oldmanpushcart.jpromisor.Promise;
import io.github.oldmanpushcart.jpromisor.Promisor;

import javax.sound.sampled.*;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

public class SpeechDetectorImplBySnowboy implements SpeechDetector {

    private final ThingNlsConfig config;
    private final Executor executor;
    private final String _string;

    private final byte[] data = new byte[10240];
    private final Snowboy snowboy;
    private final ReentrantLock lock = new ReentrantLock();

    public SpeechDetectorImplBySnowboy(ThingNlsConfig config, Executor executor) {
        this.config = config;
        this.executor = executor;
        this._string = "nls:/%s/detector".formatted(config.getAppKey());
        this.snowboy = new Snowboy();
    }

    // 打开音频线路
    private TargetDataLine openTargetDataLine(Mixer mixer, AudioFormat format) throws LineUnavailableException {
        final TargetDataLine line = AudioSystem.getTargetDataLine(format, mixer.getMixerInfo());
        line.open(format);
        return line;
    }

    // 获取SampleRate
    private SampleRate getSampleRate(SpeechDetectWakeUpOption option) {
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

    @Override
    public ListenableFuture<Void> detectWakeUp(Mixer mixer, SpeechDetectWakeUpOption option) {
        return new Promisor().<Void>promise().execute(promise -> promise.fulfill(executor, (FutureFunction.FutureExecutable) () -> {
            lock.lockInterruptibly();
            try {
                final SampleRate sampleRate = getSampleRate(option);
                final AudioFormat format = getAudioFormat(sampleRate);
                try (final TargetDataLine line = openTargetDataLine(mixer, format)) {

                    line.start();
                    while (!promise.isDone()) {
                        final int size = line.read(data, 0, data.length);
                        if (snowboy.detect(data, 0, size)) {
                            break;
                        }
                    }

                }
            } finally {
                lock.unlock();
            }
        }));
    }

    @Override
    public void close() {
        snowboy.close();
    }

}
