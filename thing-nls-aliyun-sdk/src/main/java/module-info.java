module io.github.athingx.athing.thing.nls.aliyun.sdk {

    //noinspection JavaRequiresAutoModule
    requires transitive nls.sdk.common;
    requires fastjson;
    requires org.slf4j;
    exports io.github.athingx.athing.thing.nls.aliyun.sdk.recognizer;
    exports io.github.athingx.athing.thing.nls.aliyun.sdk.transcriber;
    exports io.github.athingx.athing.thing.nls.aliyun.sdk.tts;

}