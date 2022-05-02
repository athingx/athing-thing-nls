open module io.github.athingx.athing.thing.nls {

    requires transitive java.desktop;
    requires transitive io.github.athingx.athing.standard.component;

    exports io.github.athingx.athing.thing.nls.asr;
    exports io.github.athingx.athing.thing.nls.synthesis;
    exports io.github.athingx.athing.thing.nls;
    exports io.github.athingx.athing.thing.nls.asr.recognize;
    exports io.github.athingx.athing.thing.nls.asr.transcribe;
    exports io.github.athingx.athing.thing.nls.asr.detect;
}