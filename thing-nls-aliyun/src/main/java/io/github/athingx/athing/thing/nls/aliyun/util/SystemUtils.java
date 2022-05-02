package io.github.athingx.athing.thing.nls.aliyun.util;

import java.util.Arrays;
import java.util.Objects;

/**
 * 系统工具类
 */
public class SystemUtils {

    private static final String OS = System.getProperty("os.name");
    private static final String ARCH = System.getProperty("os.arch");
    private static final String BITS = System.getProperty("sun.arch.data.model");


    private static boolean isInLowerCasePrefixes(String string, String... prefixes) {
        return Arrays.stream(prefixes).anyMatch(prefix -> string.toLowerCase().startsWith(prefix));
    }

    public static String getArch() {
        return ARCH;
    }

    public static boolean isArchX86() {
        return isInLowerCasePrefixes(getArch(), "amd64", "x86");
    }

    public static boolean isArchArm() {
        return isInLowerCasePrefixes(getArch(), "aarch");
    }

    public static String getBits() {
        return BITS;
    }

    public static boolean isBit64() {
        return Objects.equals("64", getBits());
    }

    public static boolean isBit32() {
        return Objects.equals("32", getBits());
    }

    public static String getOs() {
        return OS;
    }

    public static boolean isOsWindows() {
        return isInLowerCasePrefixes(getOs(), "windows");
    }

    public static boolean isOsMacOsX() {
        return isInLowerCasePrefixes(getOs(), "os");
    }

    public static boolean isOsLinux() {
        return isInLowerCasePrefixes(getOs(), "linux");
    }

}
