package com.google.zxing;

public abstract class LuminanceSource {
    private final int height;
    private final int width;

    public abstract byte[] getMatrix();

    public abstract byte[] getRow(int i, byte[] bArr);

    protected LuminanceSource(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public final int getWidth() {
        return this.width;
    }

    public final int getHeight() {
        return this.height;
    }

    public boolean isRotateSupported() {
        return false;
    }

    public LuminanceSource rotateCounterClockwise() {
        throw new UnsupportedOperationException("This luminance source does not support rotation by 90 degrees.");
    }

    public final String toString() {
        byte[] row = new byte[this.width];
        StringBuilder result = new StringBuilder(this.height * (this.width + 1));
        for (int y = 0; y < this.height; y++) {
            row = getRow(y, row);
            for (int x = 0; x < this.width; x++) {
                char c;
                int luminance = row[x] & 255;
                if (luminance < 64) {
                    c = '#';
                } else if (luminance < 128) {
                    c = '+';
                } else if (luminance >= 192) {
                    c = ' ';
                } else {
                    c = '.';
                }
                result.append(c);
            }
            result.append('\n');
        }
        return result.toString();
    }
}
