package com.google.zxing.pdf417;

public final class PDF417ResultMetadata {
    private String fileId;
    private boolean lastSegment;
    private int[] optionalData;
    private int segmentIndex;

    public void setSegmentIndex(int segmentIndex) {
        this.segmentIndex = segmentIndex;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setOptionalData(int[] optionalData) {
        this.optionalData = optionalData;
    }

    public void setLastSegment(boolean lastSegment) {
        this.lastSegment = lastSegment;
    }
}
