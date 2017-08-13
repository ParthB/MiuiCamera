package com.android.zxing;

import com.google.zxing.LuminanceSource;

public final class YUVLuminanceSource extends LuminanceSource {
    private final int mDataHeight;
    private final int mDataWidth;
    private final int mLeft;
    private final int mTop;
    private final byte[] mYUVData;

    public YUVLuminanceSource(byte[] yuvData, int dataWidth, int dataHeight, int left, int top, int width, int height) {
        super(width, height);
        if (left + width > dataWidth || top + height > dataHeight) {
            throw new IllegalArgumentException("Crop rectangle does not fit within image data.");
        }
        this.mYUVData = yuvData;
        this.mDataWidth = dataWidth;
        this.mDataHeight = dataHeight;
        this.mLeft = left;
        this.mTop = top;
    }

    public byte[] getRow(int y, byte[] row) {
        if (y < 0 || y >= getHeight()) {
            throw new IllegalArgumentException("Requested row is outside the image: " + y);
        }
        int width = getWidth();
        if (row == null || row.length < width) {
            row = new byte[width];
        }
        System.arraycopy(this.mYUVData, ((this.mTop + y) * this.mDataWidth) + this.mLeft, row, 0, width);
        return row;
    }

    public byte[] getMatrix() {
        int width = getWidth();
        int height = getHeight();
        if (width == this.mDataWidth && height == this.mDataHeight) {
            return this.mYUVData;
        }
        int area = width * height;
        byte[] matrix = new byte[area];
        int inputOffset = (this.mTop * this.mDataWidth) + this.mLeft;
        if (width == this.mDataWidth) {
            System.arraycopy(this.mYUVData, inputOffset, matrix, 0, area);
            return matrix;
        }
        byte[] yuv = this.mYUVData;
        for (int y = 0; y < height; y++) {
            System.arraycopy(yuv, inputOffset, matrix, y * width, width);
            inputOffset += this.mDataWidth;
        }
        return matrix;
    }

    public boolean isCropSupported() {
        return true;
    }
}
