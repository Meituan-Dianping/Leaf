package com.sankuai.inf.leaf.segment.model;

import java.util.concurrent.atomic.LongAdder;

public class Segment {
    private LongAdder value = new LongAdder();
    private volatile long max;
    private volatile int step;
    private SegmentBuffer buffer;

    public Segment(SegmentBuffer buffer) {
        this.buffer = buffer;
    }

    public LongAdder getValue() {
        return value;
    }

    public void setValue(LongAdder value) {
        this.value = value;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public SegmentBuffer getBuffer() {
        return buffer;
    }

    public long getIdle() {
        return this.getMax() - getValue().sum();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Segment(");
        sb.append("value:");
        sb.append(value);
        sb.append(",max:");
        sb.append(max);
        sb.append(",step:");
        sb.append(step);
        sb.append(")");
        return sb.toString();
    }

    // get then increment
    public long getAndIncrement(){
        long ret = value.sum();
        value.increment();
        return ret;
    }

    public void reset(long value) {
        this.value.reset();
        this.value.add(value);
    }
}
