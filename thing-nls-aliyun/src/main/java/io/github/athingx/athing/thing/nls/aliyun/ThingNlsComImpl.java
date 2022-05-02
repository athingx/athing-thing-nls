package io.github.athingx.athing.thing.nls.aliyun;

import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.NlsClient;
import io.github.athingx.athing.standard.thing.Thing;
import io.github.athingx.athing.standard.thing.ThingLifeCycle;
import io.github.athingx.athing.standard.thing.boot.ThInject;
import io.github.athingx.athing.standard.thing.op.executor.ThingExecutor;
import io.github.athingx.athing.thing.nls.ThingNlsCom;
import io.github.athingx.athing.thing.nls.aliyun.asr.recognize.SpeechRecognizerImplByAliyun;
import io.github.athingx.athing.thing.nls.aliyun.asr.transcribe.SpeechTranscriberImplByAliyun;
import io.github.athingx.athing.thing.nls.aliyun.synthesis.SpeechSynthesizerImplByAliyun;
import io.github.athingx.athing.thing.nls.asr.detect.SpeechDetector;
import io.github.athingx.athing.thing.nls.asr.recognize.SpeechRecognizer;
import io.github.athingx.athing.thing.nls.asr.transcribe.SpeechTranscriber;
import io.github.athingx.athing.thing.nls.synthesis.SpeechSynthesizer;
import io.github.oldmanpushcart.jpromisor.ListenableFuture;
import io.github.oldmanpushcart.jpromisor.Promisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ThingNlsComImpl implements ThingNlsCom, ThingLifeCycle {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final NlsClient client;
    private final ThingNlsConfig config;
    private final AccessToken token;
    private final String _string;

    @ThInject
    private Thing thing;

    public ThingNlsComImpl(ThingNlsConfig config) {
        this.client = new NlsClient(config.getRemote());
        this.config = config;
        this.token = new AccessToken(config.getAccessKeyId(), config.getAccessKeySecret());
        this._string = String.format("nls:/%s", config.getAppKey());
    }

    @Override
    public ListenableFuture<SpeechDetector> openSpeechDetector() {
        return null;
    }

    @Override
    public ListenableFuture<SpeechRecognizer> openSpeechRecognizer() {
        final ThingExecutor executor = thing.getThingOp().getThingExecutor();
        return new Promisor().fulfill(executor, () -> {
            return new SpeechRecognizerImplByAliyun(client, config, executor);
        });
    }

    @Override
    public ListenableFuture<SpeechTranscriber> openSpeechTranscriber() {
        final ThingExecutor executor = thing.getThingOp().getThingExecutor();
        return new Promisor().fulfill(executor, () -> {
            return new SpeechTranscriberImplByAliyun(client, config, executor);
        });
    }

    @Override
    public ListenableFuture<SpeechSynthesizer> openSpeechSynthesizer() {
        final ThingExecutor executor = thing.getThingOp().getThingExecutor();
        return new Promisor().fulfill(executor, () -> {
            return new SpeechSynthesizerImplByAliyun(client, config, executor);
        });
    }


    private static long next(long expire, long delay) {
        return Math.max(expire, delay);
    }

    private void flush() {

        final ThingExecutor executor = thing.getThingOp().getThingExecutor();
        final long delay = TimeUnit.SECONDS.toMillis(10);
        final Runnable flusher = new Runnable() {
            @Override
            public void run() {

                // 刷新TOKEN
                try {
                    token.apply();
                    client.setToken(token.getToken());
                    final long next = next(token.getExpireTime(), delay);
                    logger.info("{} flush token success, will flush after {} ms", ThingNlsComImpl.this, next);
                    executor.submit(next, MILLISECONDS, this);
                }

                // 刷新失败
                catch (Throwable cause) {
                    logger.warn("{} flush token failure, will retry after {} ms", ThingNlsComImpl.this, delay, cause);
                    executor.submit(delay, MILLISECONDS, this);
                }

            }
        };

        flusher.run();

    }

    @Override
    public void onLoaded(Thing thing) {
        flush();
    }

    @Override
    public String toString() {
        return _string;
    }

}
