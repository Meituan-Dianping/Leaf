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
