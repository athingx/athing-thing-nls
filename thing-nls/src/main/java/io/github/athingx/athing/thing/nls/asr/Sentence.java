package io.github.athingx.athing.thing.nls.asr;

/**
 * 句子
 *
 * @param text       文本
 * @param confidence 置信度，[0.00-1.00]，越接近1越可信
 */
public record Sentence(String text, double confidence) {
}
