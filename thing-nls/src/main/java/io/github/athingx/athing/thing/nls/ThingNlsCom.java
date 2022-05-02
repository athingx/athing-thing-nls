package io.github.athingx.athing.thing.nls;

import io.github.athingx.athing.standard.component.ThingCom;
import io.github.athingx.athing.thing.nls.asr.detect.SpeechDetector;
import io.github.athingx.athing.thing.nls.asr.recognize.SpeechRecognizer;
import io.github.athingx.athing.thing.nls.asr.transcribe.SpeechTranscriber;
import io.github.athingx.athing.thing.nls.synthesis.SpeechSynthesizer;
import io.github.oldmanpushcart.jpromisor.ListenableFuture;

public interface ThingNlsCom extends ThingCom {

    ListenableFuture<SpeechDetector> openSpeechDetector();

    ListenableFuture<SpeechRecognizer> openSpeechRecognizer();

    ListenableFuture<SpeechTranscriber> openSpeechTranscriber();

    ListenableFuture<SpeechSynthesizer> openSpeechSynthesizer();


}
