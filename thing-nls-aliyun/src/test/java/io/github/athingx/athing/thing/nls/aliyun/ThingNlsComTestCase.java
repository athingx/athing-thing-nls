package io.github.athingx.athing.thing.nls.aliyun;

import io.github.athingx.athing.thing.nls.ThingNlsCom;
import io.github.athingx.athing.thing.nls.asr.RecordingFuture;
import io.github.athingx.athing.thing.nls.asr.recognize.SpeechRecognizeOption;
import io.github.athingx.athing.thing.nls.asr.recognize.SpeechRecognizer;
import io.github.athingx.athing.thing.nls.asr.transcribe.SpeechTranscribeOption;
import io.github.athingx.athing.thing.nls.asr.transcribe.SpeechTranscriber;
import io.github.athingx.athing.thing.nls.aliyun.handler.SourceDataChannel;
import io.github.athingx.athing.thing.nls.aliyun.handler.TargetDataChannel;
import io.github.athingx.athing.thing.nls.synthesis.SpeechSynthesisOption;
import io.github.athingx.athing.thing.nls.synthesis.SpeechSynthesizer;
import org.junit.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class ThingNlsComTestCase extends ThingSupport {

    @Test
    public void test$print$mixer() {
        for (final Mixer.Info info : AudioSystem.getMixerInfo()) {
            System.out.println(info);
        }
    }

    @Test
    public void test$speech$mixer$synthesis() throws ExecutionException, InterruptedException, IOException {
        final Mixer.Info info = AudioSystem.getMixerInfo()[3];
        final Mixer mixer = AudioSystem.getMixer(info);
        final ThingNlsCom thingNls = thing.getUniqueThingCom(ThingNlsCom.class);
        try (final SpeechSynthesizer synthesizer = thingNls.openSpeechSynthesizer().get()) {
            synthesizer.synthesis(mixer, new SpeechSynthesisOption(), "白日依山尽，");
            synthesizer.synthesis(mixer, new SpeechSynthesisOption(), "黄河入海流，");
            synthesizer.synthesis(mixer, new SpeechSynthesisOption(), "欲穷千里目，");
            synthesizer.synthesis(mixer, new SpeechSynthesisOption(), "更上一层楼，").get();
        }
    }

    @Test
    public void test$speech$channel$synthesis() throws ExecutionException, InterruptedException, IOException, LineUnavailableException {
        final Mixer.Info info = AudioSystem.getMixerInfo()[3];
        final Mixer mixer = AudioSystem.getMixer(info);
        final ThingNlsCom thingNls = thing.getUniqueThingCom(ThingNlsCom.class);
        final AudioFormat format = new AudioFormat(
                8000f,
                16,
                1,
                true,
                false
        );

        final CountDownLatch latch = new CountDownLatch(2);
        try (final SpeechSynthesizer synthesizer1 = thingNls.openSpeechSynthesizer().get();
             final SpeechSynthesizer synthesizer2 = thingNls.openSpeechSynthesizer().get();
             final SourceDataChannel channel = new SourceDataChannel(AudioSystem.getSourceDataLine(format, mixer.getMixerInfo()))) {
            channel.getSourceDataLine().open(format);
            channel.getSourceDataLine().start();

            synthesizer1.synthesis(channel, new SpeechSynthesisOption(), "白日依山尽，");
            synthesizer1.synthesis(channel, new SpeechSynthesisOption(), "黄河入海流，");
            synthesizer1.synthesis(channel, new SpeechSynthesisOption(), "欲穷千里目，");
            synthesizer1.synthesis(channel, new SpeechSynthesisOption(), "更上一层楼，").onDone(future -> latch.countDown());

            synthesizer2.synthesis(channel, new SpeechSynthesisOption(), "离离原上草，");
            synthesizer2.synthesis(channel, new SpeechSynthesisOption(), "一岁一枯荣，");
            synthesizer2.synthesis(channel, new SpeechSynthesisOption(), "野火烧不尽，");
            synthesizer2.synthesis(channel, new SpeechSynthesisOption(), "春风吹又生，").onDone(future -> latch.countDown());

            latch.await();
            channel.getSourceDataLine().drain();
            channel.getSourceDataLine().stop();
        }
    }

    @Test
    public void test$speech$mixer$recognize$stop() throws ExecutionException, InterruptedException, IOException {

        final Mixer.Info info = AudioSystem.getMixerInfo()[6];
        final Mixer mixer = AudioSystem.getMixer(info);
        final ThingNlsCom thingNls = thing.getUniqueThingCom(ThingNlsCom.class);
        try (final SpeechRecognizer recognizer = thingNls.openSpeechRecognizer().get()) {
            for (int index = 0; index < 5; index++) {
                final RecordingFuture future = recognizer.recognize(mixer, new SpeechRecognizeOption(), sentence ->
                        System.out.printf("%s;%s%n", sentence.confidence(), sentence.text()));
                Thread.sleep(5 * 1000L);
                future.stopRecording();
                future.get();
            }
        }

    }

    @Test
    public void test$speech$channel$recognize$stop() throws ExecutionException, InterruptedException, IOException, LineUnavailableException {

        final ThingNlsCom thingNls = thing.getUniqueThingCom(ThingNlsCom.class);
        final AudioFormat format = new AudioFormat(
                8000f,
                16,
                1,
                true,
                false
        );

        try (final SpeechRecognizer recognizer = thingNls.openSpeechRecognizer().get();
             final TargetDataChannel channel = new TargetDataChannel(AudioSystem.getTargetDataLine(format))) {

            channel.getTargetDataLine().open(format);
            channel.getTargetDataLine().start();

            final RecordingFuture future = recognizer.recognize(channel, new SpeechRecognizeOption(), sentence ->
                    System.out.printf("%s;%s%n", sentence.confidence(), sentence.text()));
            Thread.sleep(5 * 1000L);
            future.stopRecording();
            future.get();
        }

    }

    @Test
    public void test$speech$mixer$recognize$silence() throws ExecutionException, InterruptedException, IOException {

        for (final Mixer.Info info : AudioSystem.getMixerInfo()) {
            System.out.println(info);
        }

        final Mixer.Info info = AudioSystem.getMixerInfo()[6];
        final Mixer mixer = AudioSystem.getMixer(info);
        final ThingNlsCom thingNls = thing.getUniqueThingCom(ThingNlsCom.class);
        final SpeechRecognizeOption option = new SpeechRecognizeOption();
        option.set("enable-itn", true);
        option.set("enable-voice-detection", true);
        option.set("max-start-silence", 10000L);
        option.set("max-end-silence", 1000L);
        try (final SpeechRecognizer recognizer = thingNls.openSpeechRecognizer().get()) {
            for (int index = 0; index < 5; index++) {
                recognizer.recognize(mixer, option, sentence ->
                        System.out.printf("%s;%s%n", sentence.confidence(), sentence.text())).get();
            }
        }

    }

    @Test
    public void test$speech$mixer$transcribe$stop() throws ExecutionException, InterruptedException, IOException {

        final Mixer.Info info = AudioSystem.getMixerInfo()[6];
        final Mixer mixer = AudioSystem.getMixer(info);
        final ThingNlsCom thingNls = thing.getUniqueThingCom(ThingNlsCom.class);

        try (final SpeechTranscriber transcriber = thingNls.openSpeechTranscriber().get()) {
            final RecordingFuture future = transcriber.transcribe(mixer, new SpeechTranscribeOption(), sentence -> {
                System.out.printf("%s;%s%n", sentence.confidence(), sentence.text());
            });
            Thread.sleep(60 * 1000L);
            future.stopRecording();
            future.get();
        }

    }

    @Test
    public void test$speech$channel$transcribe$stop() throws ExecutionException, InterruptedException, IOException, LineUnavailableException {

        final ThingNlsCom thingNls = thing.getUniqueThingCom(ThingNlsCom.class);
        final AudioFormat format = new AudioFormat(
                8000f,
                16,
                1,
                true,
                false
        );

        try (final SpeechTranscriber transcriber = thingNls.openSpeechTranscriber().get();
             final TargetDataChannel channel = new TargetDataChannel(AudioSystem.getTargetDataLine(format))) {

            channel.getTargetDataLine().open(format);
            channel.getTargetDataLine().start();

            final RecordingFuture future = transcriber.transcribe(channel, new SpeechTranscribeOption(), sentence ->
                    System.out.printf("%s;%s%n", sentence.confidence(), sentence.text()));
            Thread.sleep(60 * 1000L);
            future.stopRecording();
            future.get();
        }

    }

}
