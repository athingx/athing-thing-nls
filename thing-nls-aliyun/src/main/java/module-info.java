module io.github.athingx.athing.thing.nls.aliyun {

    requires io.github.athingx.athing.thing.nls;
    requires io.github.athingx.athing.standard.thing;
    requires io.github.athingx.athing.thing.nls.aliyun.sdk;
    requires nls.sdk.common;
    requires org.slf4j;
    provides io.github.athingx.athing.standard.thing.boot.ThingBoot
            with io.github.athingx.athing.thing.nls.aliyun.ThingNlsBoot;

}