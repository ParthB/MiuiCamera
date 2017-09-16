package com.google.zxing.common;

public final class BitSource {
    private int bitOffset;
    private int byteOffset;
    private final byte[] bytes;

    public BitSource(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getBitOffset() {
        return this.bitOffset;
    }

    public int getByteOffset() {
        return this.byteOffset;
    }

    public int readBits(int numBits) {
        if (numBits >= 1 && numBits <= 32 && numBits <= available()) {
            int bitsToNotRead;
            int result = 0;
            if (this.bitOffset > 0) {
                int toRead;
                int bitsLeft = 8 - this.bitOffset;
                if (numBits >= bitsLeft) {
                    toRead = bitsLeft;
                } else {
                    toRead = numBits;
                }
                bitsToNotRead = bitsLeft - toRead;
                result = (this.bytes[this.byteOffset] & ((255 >> (8 - toRead)) << bitsToNotRead)) >> bitsToNotRead;
                numBits -= toRead;
                this.bitOffset += toRead;
                if (this.bitOffset == 8) {
                    this.bitOffset = 0;
                    this.byteOffset++;
                }
            }
            if (numBits <= 0) {
                return result;
            }
            while (numBits >= 8) {
                result = (result << 8) | (this.bytes[this.byteOffset] & 255);
                this.byteOffset++;
                numBits -= 8;
            }
            if (numBits <= 0) {
                return result;
            }
            bitsToNotRead = 8 - numBits;
            result = (result << numBits) | ((this.bytes[this.byteOffset] & ((255 >> bitsToNotRead) << bitsToNotRead)) >> bitsToNotRead);
            this.bitOffset += numBits;
            return result;
        }
        throw new IllegalArgumentException(String.valueOf(numBits));
    }

    public int available() {
        return ((this.bytes.length - this.byteOffset) * 8) - this.bitOffset;
    }
}
