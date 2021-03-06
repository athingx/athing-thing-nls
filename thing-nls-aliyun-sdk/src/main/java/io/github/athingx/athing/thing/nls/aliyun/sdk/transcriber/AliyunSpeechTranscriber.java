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

package io.github.athingx.athing.thing.nls.aliyun.sdk.transcriber;

import com.alibaba.nls.client.protocol.*;
import com.alibaba.nls.client.transport.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.alibaba.nls.client.protocol.SpeechReqProtocol.State.*;

/**
 * @author zhishen.ml
 * @date 2018/05/24
 * <p>
 * 实时语音转写器，支持长语音,用于设置及发送识别请求,处理识别结果回调
 * 非线程安全
 */
public class AliyunSpeechTranscriber extends SpeechReqProtocol implements Closeable {
    static Logger logger = LoggerFactory.getLogger(AliyunSpeechTranscriber.class);

    private CountDownLatch completeLatch;
    private CountDownLatch readyLatch;
    protected long lastSendTime = -1;

    protected AliyunSpeechTranscriberListener speechTranscriberListener;

    public AliyunSpeechTranscriberListener getSpeechTranscriberListener() {
        return this.speechTranscriberListener;
    }

    /**
     * 如果没有设置format,默认为pcm
     */
    private static final String DEFAULT_FORMAT = "pcm";
    /**
     * 如果没有设置sampleRate,默认为16000
     */
    private static final Integer DEFAULT_SAMPLE_RATE = 16000;

    public String getFormat() {
        return (String) payload.get(Constant.PROP_ASR_FORMAT);
    }

    /**
     * 输入音频格式
     *
     * @param format pcm  opu opus speex
     */
    public void setFormat(InputFormatEnum format) {
        payload.put(Constant.PROP_ASR_FORMAT, format.getName());
    }

    public Integer getSampleRate() {
        return (Integer) payload.get(Constant.PROP_ASR_SAMPLE_RATE);
    }

    /**
     * 输入音频采样率 8000 16000
     *
     * @param sampleRate
     */
    public void setSampleRate(SampleRateEnum sampleRate) {
        payload.put(Constant.PROP_ASR_SAMPLE_RATE, sampleRate.value);
    }

    public void setSampleRate(int value) {
        payload.put(Constant.PROP_ASR_SAMPLE_RATE, value);
    }

    /**
     * 是否返回句子的中间识别结果，默认为false
     *
     * @param isEnable
     */
    public void setEnableIntermediateResult(boolean isEnable) {
        payload.put(Constant.PROP_ASR_ENABLE_INTERMEDIATE_RESULT, isEnable);
    }

    /**
     * 是否在识别结果中添加标点，默认为false
     *
     * @param isEnable
     */
    public void setEnablePunctuation(boolean isEnable) {
        payload.put(Constant.PROP_ASR_ENABLE_PUNCTUATION_PREDICTION, isEnable);
    }

    /**
     * 设置开启ITN(Inverse Text Normalization）,开启后汉字数字将转为阿拉伯数字输出,默认关闭
     *
     * @param enableITN
     */
    public void setEnableITN(Boolean enableITN) {
        payload.put(Constant.PROP_ASR_ENABLE_ITN, enableITN);
    }

    public AliyunSpeechTranscriber(NlsClient client, AliyunSpeechTranscriberListener listener) throws Exception {
        this.conn = client.connect(listener);
        afterConnection(listener);
    }

    public AliyunSpeechTranscriber(NlsClient client, String token, AliyunSpeechTranscriberListener listener) throws Exception {
        Connection conn = client.connect(token, listener);
        this.conn = conn;
        afterConnection(listener);
    }

    protected void afterConnection(AliyunSpeechTranscriberListener listener) {
        payload = new HashMap<String, Object>();
        header.put(Constant.PROP_NAMESPACE, Constant.VALUE_NAMESPACE_ASR_TRANSCRIPTION);
        header.put(Constant.PROP_NAME, Constant.VALUE_NAME_ASR_TRANSCRIPTION_START);
        payload.put(Constant.PROP_ASR_FORMAT, DEFAULT_FORMAT);
        payload.put(Constant.PROP_ASR_SAMPLE_RATE, DEFAULT_SAMPLE_RATE);
        listener.setSpeechTranscriber(this);
        speechTranscriberListener = listener;
        state = STATE_CONNECTED;
    }

    /**
     * 自己控制发送，需要控制发送速率
     *
     * @param data
     */
    public void send(byte[] data, int offset, int length) {
        long sendInterval;
        if (lastSendTime != -1 && (sendInterval = (System.currentTimeMillis() - lastSendTime)) > 5000) {
            logger.warn("too large binary send interval: {} million second", sendInterval);
        }
        state.checkSend();
        try {
            conn.sendBinary(Arrays.copyOfRange(data, offset, length));
            lastSendTime = System.currentTimeMillis();
        } catch (Exception e) {
            logger.error("fail to send binary,current_task_id:{},state:{}", currentTaskId, state, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 实时采集音频流
     *
     * @param ins
     */
    public void send(InputStream ins) {
        state.checkSend();
        try {
            byte[] bytes = new byte[8000];
            int len = 0;
            long sendInterval;
            while ((len = ins.read(bytes)) > 0) {
                if (lastSendTime != -1 && (sendInterval = (System.currentTimeMillis() - lastSendTime)) > 5000) {
                    logger.warn("too large binary send interval: {} million second", sendInterval);
                }
                conn.sendBinary(Arrays.copyOfRange(bytes, 0, len));
                lastSendTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            logger.error("fail to send binary,current_task_id:{},state:{}", currentTaskId, state, e);
            throw new RuntimeException(e);

        }
    }

    /**
     * 语音数据来自文件，发送时需要控制速率，使单位时间内发送的数据大小接近单位时间原始语音数据存储的大小
     * <ul>
     * <li><对于8k pcm 编码数据，建议每发送3200字节 sleep 200 ms/li>
     * <li>对于16k pcm 编码数据，建议每发送6400字节 sleep 200 ms/li>
     * <li>对于其它编码格式的数据，用户根据压缩比，自行估算，比如压缩比为10:1的 16k opus ，需要每发送6400/10=640 sleep 200ms/li>
     * </ul>
     *
     * @param ins           离线音频文件流
     * @param batchSize     每次发送到服务端的数据大小
     * @param sleepInterval 数据发送的间隔，即用于控制发送数据的速率，每次发送batchSize大小的数据后需要sleep的时间
     */
    public void send(InputStream ins, int batchSize, int sleepInterval) {
        state.checkSend();
        try {
            byte[] bytes = new byte[batchSize];
            int len = 0;
            long sendInterval;
            while ((len = ins.read(bytes)) > 0) {
                if (lastSendTime != -1 && (sendInterval = (System.currentTimeMillis() - lastSendTime)) > 5000) {
                    logger.warn("too large binary send interval: {} million second", sendInterval);
                }
                conn.sendBinary(Arrays.copyOfRange(bytes, 0, len));
                lastSendTime = System.currentTimeMillis();
                Thread.sleep(sleepInterval);
            }
        } catch (Exception e) {
            logger.error("fail to send binary,current_task_id:{},state:{}", currentTaskId, state, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 服务端准备好了进行语音转写
     */
    void markTranscriberReady() {
        state = STATE_REQUEST_CONFIRMED;
        if (readyLatch != null) {
            readyLatch.countDown();
        }
    }

    /**
     * 服务端停止了语音转写
     */
    void markTranscriberComplete() {
        state = STATE_COMPLETE;
        if (completeLatch != null) {
            completeLatch.countDown();
        }
    }

    /**
     * 服务端返回错误
     */
    void markFail() {
        state = STATE_FAIL;
        if (readyLatch != null) {
            readyLatch.countDown();
        }
        if (completeLatch != null) {
            completeLatch.countDown();
        }
    }

    /**
     * 内部调用方法
     */
    void markClosed() {
        state = STATE_CLOSED;
        if (readyLatch != null) {
            readyLatch.countDown();
        }
        if (completeLatch != null) {
            completeLatch.countDown();
        }
    }

    /**
     * 开始语音转写：发送语音转写请求，同步接收服务端确认
     *
     * @throws Exception
     */
    public void start() throws Exception {
        super.start();
        completeLatch = new CountDownLatch(1);
        readyLatch = new CountDownLatch(1);
        boolean result = readyLatch.await(10, TimeUnit.SECONDS);
        if (!result) {
            String msg = String.format("timeout after 10 seconds waiting for start confirmation.task_id:%s,state:%s",
                    currentTaskId, state);
            logger.error(msg);
            throw new Exception(msg);
        }
    }

    /**
     * 结束语音识别: 发送结束识别通知，接收服务端确认
     *
     * @throws Exception
     */
    public void stop() throws Exception {
        state.checkStop();
        SpeechReqProtocol req = new SpeechReqProtocol();
        req.header.put(Constant.PROP_TASK_ID, currentTaskId);
        req.header.put(Constant.PROP_NAMESPACE, Constant.VALUE_NAMESPACE_ASR_TRANSCRIPTION);
        req.header.put(Constant.PROP_NAME, Constant.VALUE_NAME_ASR_TRANSCRIPTION_STOP);
        req.setAppKey(getAppKey());
        conn.sendText(req.serialize());
        state = STATE_STOP_SENT;
        boolean result = completeLatch.await(10, TimeUnit.SECONDS);
        if (!result) {
            String msg = String.format("timeout after 10 seconds waiting for complete confirmation.task_id:%s,state:%s",
                    currentTaskId, state);
            logger.error(msg);
            throw new Exception(msg);
        }
    }

    /**
     * 关闭连接
     */
    @Override
    public void close() {
        conn.close();
    }

}