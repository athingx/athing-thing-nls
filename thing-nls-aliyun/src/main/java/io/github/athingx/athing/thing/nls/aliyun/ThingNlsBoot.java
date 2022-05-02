package io.github.athingx.athing.thing.nls.aliyun;

import io.github.athingx.athing.standard.component.ThingCom;
import io.github.athingx.athing.standard.thing.boot.ThingBoot;
import io.github.athingx.athing.standard.thing.boot.ThingBootArgument;
import io.github.athingx.athing.thing.nls.SampleRate;

import java.util.Objects;
import java.util.Properties;

import static io.github.athingx.athing.standard.thing.boot.ThingBootArgument.Converter.cFloat;
import static io.github.athingx.athing.standard.thing.boot.ThingBootArgument.Converter.cString;

public class ThingNlsBoot implements ThingBoot {

    public static final String ACCESS_KEY_ID = "access-key-id";
    public static final String ACCESS_KEY_SECRET = "access-key-secret";
    public static final String REMOTE = "remote";
    public static final String APP_KEY = "app-key";
    public static final String SAMPLE_RATE = "sample-rate";
    public static final String SNOWBOY_COMMON_RES = "snowboy-common-res";
    public static final String SNOWBOY_PERSON_RES = "snowboy-person-res";

    @Override
    public ThingCom[] boot(String productId, String thingId, ThingBootArgument argument) {
        final ThingNlsConfig config = new ThingNlsConfig();
        config.setAccessKeyId(Objects.requireNonNull(argument.getArgument(ACCESS_KEY_ID, cString)));
        config.setAccessKeySecret(Objects.requireNonNull(argument.getArgument(ACCESS_KEY_SECRET, cString)));
        config.setAppKey(Objects.requireNonNull(argument.getArgument(APP_KEY, cString)));
        config.setRemote(argument.getArgument(REMOTE, cString, "wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1"));
        config.setSampleRate(SampleRate.valueOf(argument.getArgument(SAMPLE_RATE, cFloat, 16000f)));
        config.setSnowboyCommonResPath(argument.getArgument(SNOWBOY_COMMON_RES, cString, "/snowboy/common.res"));
        config.setSnowboyPersonResPath(argument.getArgument(SNOWBOY_PERSON_RES, cString, "/snowboy/xiaokun.pmdl"));
        return new ThingCom[]{
                new ThingNlsComImpl(config)
        };
    }

    @Override
    public Properties getProperties() {
        return new Properties() {{
            put(PROP_GROUP, "io.github.athingx.athing.thing.nls");
            put(PROP_ARTIFACT, "thing-nls-aliyun");
        }};
    }

}
