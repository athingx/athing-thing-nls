/*
 * Copyright 2015 Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.athingx.athing.thing.nls.aliyun.sdk.tts;

import com.alibaba.nls.client.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_CLOSED;
import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.STATE_REQUEST_CONFIRMED;

/**
 * @author zhishen.ml
 * @date 2017/11/28
 * <p>
 * 语音合成
 */
public class AliyunSpeechSynthesizer extends SpeechReqProtocol implements Closeable {
    static Logger logger = LoggerFactory.getLogger(AliyunSpeechSynthesizer.class);
    private AliyunSpeechSynthesizerListener listener;
    private CountDownLatch completeLatch;
    private boolean isLongText;

    /**
     * 如果没有设置format,默认为pcm
     */
    private static final String DEFAULT_FORMAT = "pcm";
    /**
     * 如果没有设置sampleRate,默认为16000
     */
    private static final Integer DEFAULT_SAMPLE_RATE = 16000;

    private static final Integer DEFAULT_VOICE_VOLUME = 50;

    public AliyunSpeechSynthesizer(NlsClient client, AliyunSpeechSynthesizerListener listener) throws Exception {
        conn = client.connect(listener);
        this.listener = listener;
        afterConnection(listener);

    }

    public AliyunSpeechSynthesizer(NlsClient client, String token, AliyunSpeechSynthesizerListener listener) throws Exception {
        this.conn = client.connect(token, listener);
        this.listener = listener;
        afterConnection(listener);
    }

    protected void afterConnection(AliyunSpeechSynthesizerListener listener) {
        payload = new HashMap<String, Object>();
        header.put(Constant.PROP_NAMESPACE, TTSConstant.VALUE_NAMESPACE_TTS);
        header.put(Constant.PROP_NAME, TTSConstant.VALUE_NAME_TTS_START);
        payload.put(TTSConstant.PROP_TTS_FORMAT, DEFAULT_FORMAT);
        payload.put(TTSConstant.PROP_TTS_SAMPLE_RATE, DEFAULT_SAMPLE_RATE);
        payload.put(TTSConstant.PROP_TTS_VOLUME, DEFAULT_VOICE_VOLUME);
        listener.setSpeechSynthesizer(this);
        state = State.STATE_CONNECTED;

    }

    /**
     * 发音人
     *
     * @param voice
     */
    public void setVoice(String voice) {
        if (null == voice) {
            payload.remove(TTSConstant.PROP_TTS_VOICE);
        } else {
            payload.put(TTSConstant.PROP_TTS_VOICE, voice);
        }

    }

    /**
     * 待合成文本
     *
     * @param text
     */
    public void setText(String text) {
        isLongText = false;
        header.put(Constant.PROP_NAMESPACE, TTSConstant.VALUE_NAMESPACE_TTS);
        payload.put(TTSConstant.PROP_TTS_TEXT, text);
    }

    public void setLongText(String text) {
        isLongText = true;
        header.put(Constant.PROP_NAMESPACE, TTSConstant.VALUE_NAMESPACE_LONG_TTS);
        payload.put(TTSConstant.PROP_TTS_TEXT, text);
    }

    /**
     * 合成语音的编码格式,支持的格式：pcm，wav，mp3 默认是pcm
     *
     * @param format
     */
    public void setFormat(OutputFormatEnum format) {
        payload.put(Constant.PROP_ASR_FORMAT, format.getName());
    }

    /**
     * 合成语音的采样率,可选默认16000
     *
     * @param sampleRate
     */
    public void setSampleRate(SampleRateEnum sampleRate) {
        payload.put(TTSConstant.PROP_TTS_SAMPLE_RATE, sampleRate.value);
    }

    /**
     * 个别场景需要设置除'SampleRateEnum'之外的采样率,如32000,采用此方法进行设置
     *
     * @param sampleRate
     */
    public void setSampleRate(int sampleRate) {
        payload.put(TTSConstant.PROP_TTS_SAMPLE_RATE, sampleRate);
    }

    /**
     * 音量，范围是0~100，可选，默认50
     *
     * @param volume
     */
    public void setVolume(int volume) {
        payload.put(TTSConstant.PROP_TTS_VOLUME, volume);
    }

    /**
     * 语速，范围是-500~500，可选，默认是0
     *
     * @param speechRate
     */
    public void setSpeechRate(int speechRate) {
        payload.put(TTSConstant.PROP_TTS_SPEECH_RATE, speechRate);
    }

    /**
     * 语调，范围是-500~500，可选，默认是0
     *
     * @param pitchRate
     */
    public void setPitchRate(int pitchRate) {
        payload.put(TTSConstant.PROP_TTS_PITCH_RATE, pitchRate);
    }

    /**
     * 语音合成方法 默认为0
     * <p>
     * 0 统计参数合成
     * 基于统计参数的语音合成，优点是能适应的韵律特征的范围较宽，合成器比特率低，资源占用小，性能高，音质适中
     * 1 波形拼接合成
     * 基于高质量音库提取学习合成，资源占用相对较高，音质较好，更加贴近真实发音，但没有参数合成稳定
     *
     * @param method
     */
    public void setMethod(Integer method) {
        if (null == method) {
            payload.remove(TTSConstant.PROP_TTS_METHOD);
        } else {
            payload.put(TTSConstant.PROP_TTS_METHOD, method);
        }
    }

    /**
     * 开始语音合成
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        super.start();
        state = STATE_REQUEST_CONFIRMED;
        completeLatch = new CountDownLatch(1);
        listener.setCompleteLatch(completeLatch);
    }

    /**
     * 等待语音合成结束
     *
     * @param seconds 最多等待多久就按超时处理
     * @throws Exception
     */
    public void waitForComplete(int seconds) throws Exception {
        completeLatch.await(seconds, TimeUnit.SECONDS);
    }

    /**
     * 等待语音合成结束
     *
     * @throws Exception
     */
    public void waitForComplete() throws Exception {
        completeLatch.await();

    }

    /**
     * 关闭连接
     */
    @Override
    public void close() {
        conn.close();
    }

    void markClosed() {
        state = STATE_CLOSED;
        if (completeLatch != null) {
            completeLatch.countDown();
        }
    }
}
