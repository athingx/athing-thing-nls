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

package io.github.athingx.athing.thing.nls.aliyun.sdk.recognizer;

import com.alibaba.nls.client.protocol.SpeechResProtocol;

/**
 * @author zhishen.ml
 * @date 2017/11/24
 *
 * 一句话识别返回结果
 */
public class AliyunSpeechRecognizerResponse extends SpeechResProtocol {
    /**
     * 最终识别结果
     *
     * @return
     */
    public String getRecognizedText() {
        return (String)payload.get("result");
    }

    /**
     * 后处理之前的结果
     *
     * @return
     */
    public String getLexicalText() {
        return (String)payload.get("lexical_result");
    }

    /**
     * 置信度.(语音识别结果的置信度，范围0~1)
     *
     * @return
     */
    public Double getConfidence() {

        Object o=payload.get("confidence");
        if(o!=null){
            return Double.parseDouble(o.toString());
        }
        return null;
    }
}
