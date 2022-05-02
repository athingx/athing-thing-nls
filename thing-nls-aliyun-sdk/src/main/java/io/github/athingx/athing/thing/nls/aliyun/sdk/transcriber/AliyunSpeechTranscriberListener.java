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

import com.alibaba.fastjson.JSON;
import com.alibaba.nls.client.protocol.Constant;
import com.alibaba.nls.client.transport.ConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * @author zhishen.ml
 * @date 2018/05/24
 *
 */
public abstract class AliyunSpeechTranscriberListener implements ConnectionListener {
    Logger logger = LoggerFactory.getLogger(AliyunSpeechTranscriberListener.class);
    private AliyunSpeechTranscriber transcriber;

    public void setSpeechTranscriber(AliyunSpeechTranscriber transcriber) {
        this.transcriber = transcriber;
    }

    public AliyunSpeechTranscriber getSpeechTranscriber(){
        return transcriber;
    }

    /**
     * 服务端准备好了进行识别
     *
     * @param response
     */
    abstract public void onTranscriberStart(AliyunSpeechTranscriberResponse response) ;

    /**
     * 服务端检测到了一句话的开始
     *
     * @param response
     */
    abstract public void onSentenceBegin(AliyunSpeechTranscriberResponse response) ;

    /**
     * 服务端检测到了一句话的结束
     *
     * @param response
     */
    abstract public void onSentenceEnd(AliyunSpeechTranscriberResponse response) ;

    /**
     * 语音识别过程中返回的结果
     *
     * @param response
     */
    abstract public void onTranscriptionResultChange(AliyunSpeechTranscriberResponse response) ;

    /**
     * 识别结束后返回的最终结果
     *
     * @param response
     */
    abstract public void onTranscriptionComplete(AliyunSpeechTranscriberResponse response) ;

    /**
     * nlp 识别结果
     * @param response
     */
    public void onSentenceSemantics(AliyunSpeechTranscriberResponse response){
        //选择实现
    }

    /**
     * 失败处理
     * @param response
     */
    abstract public void onFail(AliyunSpeechTranscriberResponse response) ;


    @Override
    public void onOpen() {
        logger.debug("connection is ok");
    }

    @Override
    public void onClose(int closeCode, String reason) {
        if (transcriber != null) {
            transcriber.markClosed();
        }
        logger.info("connection is closed due to {},code:{}", reason, closeCode);
    }

    @Override
    public void onMessage(String message) {
        if (message == null || message.trim().length() == 0) {
            return;
        }
        logger.debug("on message:{}", message);
        AliyunSpeechTranscriberResponse response = JSON.parseObject(message, AliyunSpeechTranscriberResponse.class);
        if (isTranscriptionStarted(response)) {
            onTranscriberStart(response);
            transcriber.markTranscriberReady();
        } else if (isSentenceBegin(response)) {
            onSentenceBegin(response);
        } else if (isSentenceEnd(response)) {
            onSentenceEnd(response);
        }else if (isNlpResult(response)) {
            onSentenceSemantics(response);
        } else if (isTranscriptionResultChanged(response)) {
            onTranscriptionResultChange(response);
        } else if (isTranscriptionCompleted(response)) {
            onTranscriptionComplete(response);
            transcriber.markTranscriberComplete();
        } else if (isTaskFailed(response)) {
            onFail(response);
            transcriber.markFail();
        } else {
            logger.error("can not process this message: {}", message);
        }
    }

    @Override
    public void onMessage(ByteBuffer message) {

    }

    private boolean isTranscriptionStarted(AliyunSpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_TRANSCRIPTION_STARTED)) {
            return true;
        }
        return false;
    }

    private boolean isSentenceBegin(AliyunSpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_SENTENCE_BEGIN)) {
            return true;
        }
        return false;
    }

    private boolean isSentenceEnd(AliyunSpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_SENTENCE_END)) {
            return true;
        }
        return false;
    }

    private boolean isNlpResult(AliyunSpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_NLP_RESULT)) {
            return true;
        }
        return false;
    }

    private boolean isTranscriptionResultChanged(AliyunSpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_TRANSCRIPTION_RESULT_CHANGE)) {
            return true;
        }
        return false;
    }

    private boolean isTranscriptionCompleted(AliyunSpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_ASR_TRANSCRIPTION_COMPLETE)) {
            return true;
        }
        return false;
    }

    private boolean isTaskFailed(AliyunSpeechTranscriberResponse response) {
        String name = response.getName();
        if (name.equals(Constant.VALUE_NAME_TASK_FAILE)) {
            return true;
        }
        return false;
    }

}
