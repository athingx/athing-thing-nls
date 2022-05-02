package io.github.athingx.athing.thing.nls.aliyun;

import io.github.athingx.athing.thing.nls.ThingNlsCom;
import io.github.athingx.athing.thing.nls.asr.RecordingFuture;
import io.github.athingx.athing.thing.nls.asr.recognize.SpeechRecognizeOption;
import io.github.athingx.athing.thing.nls.asr.recognize.SpeechRecognizer;
import io.github.athingx.athing.thing.nls.asr.transcribe.SpeechTranscribeOption;
import io.github.athingx.athing.thing.nls.asr.transcribe.SpeechTranscriber;
import io.github.athingx.athing.thing.nls.synthesis.SpeechSynthesisOption;
import io.github.athingx.athing.thing.nls.synthesis.SpeechSynthesizer;
import org.junit.Test;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ThingNlsComTestCase extends ThingSupport {

    @Test
    public void test$speech$synthesis() throws ExecutionException, InterruptedException, IOException {

        for (final Mixer.Info info : AudioSystem.getMixerInfo()) {
            System.out.println(info);
        }

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
    public void test$speech$recognize$stop() throws ExecutionException, InterruptedException, IOException {

        for (final Mixer.Info info : AudioSystem.getMixerInfo()) {
            System.out.println(info);
        }

        final Mixer.Info info = AudioSystem.getMixerInfo()[6];
        final Mixer mixer = AudioSystem.getMixer(info);
        final ThingNlsCom thingNls = thing.getUniqueThingCom(ThingNlsCom.class);
        try (final SpeechRecognizer recognizer = thingNls.openSpeechRecognizer().get()) {
            for(int index=0;index<5;index++) {
                final RecordingFuture future = recognizer.recognize(mixer, new SpeechRecognizeOption(), sentence ->
                        System.out.printf("%s;%s%n", sentence.confidence(), sentence.text()));
                Thread.sleep(5 * 1000L);
                future.stopRecording();
                future.get();
            }
        }

    }

    @Test
    public void test$speech$recognize$multi$stop() throws ExecutionException, InterruptedException, IOException {

        final Mixer.Info info = AudioSystem.getMixerInfo()[6];
        final Mixer mixer = AudioSystem.getMixer(info);
        final ThingNlsCom thingNls = thing.getUniqueThingCom(ThingNlsCom.class);
        try (final SpeechRecognizer recognizer1 = thingNls.openSpeechRecognizer().get();
             final SpeechRecognizer recognizer2 = thingNls.openSpeechRecognizer().get()) {
            final RecordingFuture future1 = recognizer1.recognize(mixer, new SpeechRecognizeOption(), sentence ->
                    System.out.printf("future1: %s;%s%n", sentence.confidence(), sentence.text()));
            final RecordingFuture future2 = recognizer2.recognize(mixer, new SpeechRecognizeOption(), sentence ->
                    System.out.printf("future2: %s;%s%n", sentence.confidence(), sentence.text()));
            Thread.sleep(5 * 1000L);
            future1.stopRecording();
            future2.stopRecording();
            future1.get();
            future2.get();
        }

    }

    @Test
    public void test$speech$recognize$silence() throws ExecutionException, InterruptedException, IOException {

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
            for(int index=0;index<5;index++) {
                recognizer.recognize(mixer, option, sentence ->
                        System.out.printf("%s;%s%n", sentence.confidence(), sentence.text())).get();
            }
        }

    }

    @Test
    public void test$speech$transcribe() throws ExecutionException, InterruptedException, IOException {

        final Mixer.Info info = AudioSystem.getMixerInfo()[6];
        final Mixer mixer = AudioSystem.getMixer(info);
        final ThingNlsCom thingNls = thing.getUniqueThingCom(ThingNlsCom.class);

        try(final SpeechTranscriber transcriber = thingNls.openSpeechTranscriber().get()) {
            final RecordingFuture future = transcriber.transcribe(mixer, new SpeechTranscribeOption(), sentence -> {
                System.out.printf("%s;%s%n", sentence.confidence(), sentence.text());
            });
            Thread.sleep(60 * 1000L);
            future.stopRecording();
            future.get();
        }

    }

}
