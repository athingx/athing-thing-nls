package io.github.athingx.athing.thing.nls.aliyun.handler;

import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * 音频输出数据通道
 */
public class SourceDataChannel implements WritableByteChannel {

    private final byte[] data;
    private final SourceDataLine line;

    public SourceDataChannel(SourceDataLine line) {
        this(line, 10240);
    }

    public SourceDataChannel(SourceDataLine line, int length) {
        this.line = line;
        this.data = new byte[length];
    }

    public SourceDataLine getSourceDataLine() {
        return line;
    }

    @Override
    public synchronized int write(ByteBuffer buffer) throws IOException {
        try {
            final int length = Math.min(data.length, buffer.remaining());
            buffer.get(data, 0, length);
            int offset = 0;
            do {
                offset += line.write(data, offset, length - offset);
            } while (offset < length);
            return length;
        } catch (Exception cause) {
            throw new IOException(cause);
        }
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
