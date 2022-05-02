package io.github.athingx.athing.thing.nls.aliyun.util;

public class ArgumentUtils {

    public static <T> T getByOrder(T... values) {
        if (null != values) {
            for (final T value : values) {
                if (null != value) {
                    return value;
                }
            }
        }
        return null;
    }

}
