package io.github.athingx.athing.thing.nls.aliyun.handler;

import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * 音频输入数据通道
 */
public class TargetDataChannel implements ReadableByteChannel {

    private final byte[] data;
    private final TargetDataLine line;

    public TargetDataChannel(TargetDataLine line, int length) {
        this.line = line;
        this.data = new byte[length];
    }

    public TargetDataChannel(TargetDataLine line) {
        this(line, 10240);
    }

    public TargetDataLine getTargetDataLine() {
        return line;
    }

    @Override
    public synchronized int read(ByteBuffer buffer) {
        final int length = line.read(data, 0, Math.min(buffer.remaining(), data.length));
        buffer.put(data, 0, length);
        return length;
    }

    @Override
    public boolean isOpen() {
        return line.isOpen();
    }

    @Override
    public void close() {
        line.close();
    }

}
