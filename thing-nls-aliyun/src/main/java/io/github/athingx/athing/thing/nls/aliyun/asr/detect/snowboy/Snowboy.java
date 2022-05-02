package io.github.athingx.athing.thing.nls.aliyun.asr.detect.snowboy;

import io.github.athingx.athing.thing.nls.aliyun.util.SystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static io.github.athingx.athing.thing.nls.aliyun.util.SystemUtils.*;

public class Snowboy implements AutoCloseable {

    private final static Map<String, File> resourceFileCache = new HashMap<>();
    private final static File SNOWBOY_LIB_FILE;

    static {

        // 加载关键资源
        try {
            SNOWBOY_LIB_FILE = loadingSnowboyLibFile();
        } catch (IOException cause) {
            throw new RuntimeException("snowboy loading resource error!", cause);
        }

        // 加载动态库
        System.load(SNOWBOY_LIB_FILE.getAbsolutePath());

    }

    /**
     * 加载lib文件
     * linux_aarch64
     *
     * @return snowboy的库
     * @throws IOException 加载失败
     */
    private static File loadingSnowboyLibFile() throws IOException {

        // linux_arm_64
        if (isOsLinux() && isArchArm() && isBit64()) {
            return loadingResFile(
                    "/snowboy/libs/linux_aarch_64_libsnowboy-detect-java.so",
                    "snowboy",
                    "so"
            );
        }

        // 其他操作系统&CPU架构暂不支持
        throw new UnsupportedOperationException(String.format("unsupported OS:%s & ARCH:%s & BITS:%s",
                SystemUtils.getOs(),
                SystemUtils.getArch(),
                SystemUtils.getBits()
        ));

    }



    /**
     * 从资源中加载文件
     *
     * @param resourceName 资源名称
     * @param prefix       文件前缀
     * @param suffix       文件后缀
     * @return 资源文件
     * @throws IOException 加载失败
     */
    private static synchronized File loadingResFile(String resourceName, String prefix, String suffix) throws IOException {

        if (resourceFileCache.containsKey(resourceName)) {
            return resourceFileCache.get(resourceName);
        }

        try (final InputStream input = Snowboy.class.getResourceAsStream(resourceName)) {
            if (null == input) {
                throw new IOException(String.format("resource: %s not existed!", resourceName));
            }
            final File resFile = File.createTempFile(prefix, suffix);
            resFile.deleteOnExit();
            try (final FileOutputStream output = new FileOutputStream(resFile)) {
                final byte[] buffer = new byte[1024];
                int size;
                while ((size = input.read(buffer)) != -1) {
                    output.write(buffer, 0, size);
                }
                output.flush();
            }
            resourceFileCache.put(resourceName, resFile);
            return resFile;
        }
    }


    // -----

    private final SnowboyDetect detect;

    public Snowboy(String commonResPath, String personResPath) throws IOException {
        this.detect = new SnowboyDetect(
                loadingResFile(commonResPath, "common", "res").getAbsolutePath(),
                loadingResFile(personResPath, "person", "pmdl").getAbsolutePath()
        );
        detect.SetSensitivity("0.5");
        detect.SetAudioGain(1);
        detect.ApplyFrontend(false);
    }

    /**
     * 检测音频数据是否包含唤醒词
     *
     * @param data   音频数据
     * @param offset offset
     * @param length length
     * @return TRUE | FALSE
     */
    public boolean detect(byte[] data, int offset, int length) {
        final short[] buffer = new short[(length - offset) / (Short.BYTES / Byte.BYTES)];
        if (buffer.length == 0) {
            return false;
        }
        int size = 0;
        for (int index = offset; index < data.length - 1; index += 2) {
            if (size == buffer.length - 1) {
                break;
            }
            buffer[size++] = (short) ((data[index + 1] << 8) + (data[index]));
        }
        return detect.RunDetection(buffer, size) > 0;
    }

    @Override
    public void close() {
        detect.delete();
    }

}
