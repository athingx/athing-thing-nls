package io.github.athingx.athing.thing.nls.asr;

/**
 * 句子处理器
 */
public interface SentenceHandler {

    /**
     * 处理句子
     *
     * @param sentence 句子
     */
    void onSentence(Sentence sentence);

}
