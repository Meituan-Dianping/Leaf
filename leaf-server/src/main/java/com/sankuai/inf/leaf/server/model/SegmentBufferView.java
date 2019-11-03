/*
 * Copyright 2016-2018 LEAF.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sankuai.inf.leaf.server.model;

public class SegmentBufferView {
    private String key;
    private long value0;
    private int step0;
    private long max0;

    private long value1;
    private int step1;
    private long max1;
    private int pos;
    private boolean nextReady;
    private boolean initOk;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getValue1() {
        return value1;
    }

    public void setValue1(long value1) {
        this.value1 = value1;
    }

    public int getStep1() {
        return step1;
    }

    public void setStep1(int step1) {
        this.step1 = step1;
    }

    public long getMax1() {
        return max1;
    }

    public void setMax1(long max1) {
        this.max1 = max1;
    }

    public long getValue0() {
        return value0;
    }

    public void setValue0(long value0) {
        this.value0 = value0;
    }

    public int getStep0() {
        return step0;
    }

    public void setStep0(int step0) {
        this.step0 = step0;
    }

    public long getMax0() {
        return max0;
    }

    public void setMax0(long max0) {
        this.max0 = max0;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public boolean isNextReady() {
        return nextReady;
    }

    public void setNextReady(boolean nextReady) {
        this.nextReady = nextReady;
    }

    public boolean isInitOk() {
        return initOk;
    }

    public void setInitOk(boolean initOk) {
        this.initOk = initOk;
    }
}
