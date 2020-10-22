package com.sankuai.inf.leaf.common;

/**
 * @author mickle
 */
public class CheckVO {
    private long timestamp;
    private int workId;

    public CheckVO(long timestamp, int workId) {
        this.timestamp = timestamp;
        this.workId = workId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getWorkId() {
        return workId;
    }

    public void setWorkId(int workId) {
        this.workId = workId;
    }

  @Override
  public String toString() {
    return "CheckVO{" +
      "timestamp=" + timestamp +
      ", workId=" + workId +
      '}';
  }
}
