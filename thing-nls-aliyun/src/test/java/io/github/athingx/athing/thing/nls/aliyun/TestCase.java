package io.github.athingx.athing.thing.nls.aliyun;

import io.github.athingx.athing.thing.nls.SampleRate;
import org.junit.Test;

import javax.sound.sampled.*;

public class TestCase {

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

    @Test
    public void test() throws LineUnavailableException {
        final Mixer.Info info = AudioSystem.getMixerInfo()[6];
        final Mixer mixer = AudioSystem.getMixer(info);
        final AudioFormat format = new AudioFormat(
                8000f,
                16,
                1,
                true,
                false
        );

        try(final TargetDataLine line1 = AudioSystem.getTargetDataLine(format, mixer.getMixerInfo())) {
            line1.open(format);
            try(final TargetDataLine line2 = AudioSystem.getTargetDataLine(format, mixer.getMixerInfo())) {
                line2.open(format);
            }
        }
    }

}
