package com.sankuai.inf.leaf.common;

public class Result {

    private long id;
    private long size = 1;
    private Status status;

    public Result() {
    }

    public Result(long id, Status status) {
        this.id = id;
        this.status = status;
    }

    public Result(long id, long size, Status status) {
        this(id, status);
        this.size = size;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Result{");
        sb.append("id=").append(id);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }
}
