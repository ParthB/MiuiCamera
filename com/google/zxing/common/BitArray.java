package com.google.zxing.common;

import java.util.Arrays;

public final class BitArray implements Cloneable {
    private int[] bits;
    private int size;

    public BitArray() {
        this.size = 0;
        this.bits = new int[1];
    }

    public BitArray(int size) {
        this.size = size;
        this.bits = makeArray(size);
    }

    BitArray(int[] bits, int size) {
        this.bits = bits;
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }

    public boolean get(int i) {
        return (this.bits[i / 32] & (1 << (i & 31))) != 0;
    }

    public void set(int i) {
        int[] iArr = this.bits;
        int i2 = i / 32;
        iArr[i2] = iArr[i2] | (1 << (i & 31));
    }

    public int getNextSet(int from) {
        if (from >= this.size) {
            return this.size;
        }
        int bitsOffset = from / 32;
        int currentBits = this.bits[bitsOffset] & (((1 << (from & 31)) - 1) ^ -1);
        while (currentBits == 0) {
            bitsOffset++;
            if (bitsOffset == this.bits.length) {
                return this.size;
            }
            currentBits = this.bits[bitsOffset];
        }
        int result = (bitsOffset * 32) + Integer.numberOfTrailingZeros(currentBits);
        if (result > this.size) {
            result = this.size;
        }
        return result;
    }

    public int getNextUnset(int from) {
        if (from >= this.size) {
            return this.size;
        }
        int bitsOffset = from / 32;
        int currentBits = (this.bits[bitsOffset] ^ -1) & (((1 << (from & 31)) - 1) ^ -1);
        while (currentBits == 0) {
            bitsOffset++;
            if (bitsOffset == this.bits.length) {
                return this.size;
            }
            currentBits = this.bits[bitsOffset] ^ -1;
        }
        int result = (bitsOffset * 32) + Integer.numberOfTrailingZeros(currentBits);
        if (result > this.size) {
            result = this.size;
        }
        return result;
    }

    public void setBulk(int i, int newBits) {
        this.bits[i / 32] = newBits;
    }

    public void clear() {
        int max = this.bits.length;
        for (int i = 0; i < max; i++) {
            this.bits[i] = 0;
        }
    }

    public boolean isRange(int start, int end, boolean value) {
        if (end < start) {
            throw new IllegalArgumentException();
        } else if (end == start) {
            return true;
        } else {
            end--;
            int firstInt = start / 32;
            int lastInt = end / 32;
            for (int i = firstInt; i <= lastInt; i++) {
                int firstBit;
                int lastBit;
                int mask;
                if (i <= firstInt) {
                    firstBit = start & 31;
                } else {
                    firstBit = 0;
                }
                if (i >= lastInt) {
                    lastBit = end & 31;
                } else {
                    lastBit = 31;
                }
                if (firstBit == 0 && lastBit == 31) {
                    mask = -1;
                } else {
                    mask = 0;
                    for (int j = firstBit; j <= lastBit; j++) {
                        mask |= 1 << j;
                    }
                }
                int i2 = this.bits[i] & mask;
                if (!value) {
                    mask = 0;
                }
                if (i2 != mask) {
                    return false;
                }
            }
            return true;
        }
    }

    public int[] getBitArray() {
        return this.bits;
    }

    public void reverse() {
        int i;
        int[] newBits = new int[this.bits.length];
        int len = (this.size - 1) / 32;
        int oldBitsLen = len + 1;
        for (i = 0; i < oldBitsLen; i++) {
            long x = (long) this.bits[i];
            x = ((x >> 1) & 1431655765) | ((1431655765 & x) << 1);
            x = ((x >> 2) & 858993459) | ((858993459 & x) << 2);
            x = ((x >> 4) & 252645135) | ((252645135 & x) << 4);
            x = ((x >> 8) & 16711935) | ((16711935 & x) << 8);
            newBits[len - i] = (int) (((x >> 16) & 65535) | ((65535 & x) << 16));
        }
        if (this.size != oldBitsLen * 32) {
            int leftOffset = (oldBitsLen * 32) - this.size;
            int mask = 1;
            for (i = 0; i < 31 - leftOffset; i++) {
                mask = (mask << 1) | 1;
            }
            int currentInt = (newBits[0] >> leftOffset) & mask;
            for (i = 1; i < oldBitsLen; i++) {
                int nextInt = newBits[i];
                newBits[i - 1] = currentInt | (nextInt << (32 - leftOffset));
                currentInt = (nextInt >> leftOffset) & mask;
            }
            newBits[oldBitsLen - 1] = currentInt;
        }
        this.bits = newBits;
    }

    private static int[] makeArray(int size) {
        return new int[((size + 31) / 32)];
    }

    public boolean equals(Object o) {
        if (!(o instanceof BitArray)) {
            return false;
        }
        BitArray other = (BitArray) o;
        if (this.size == other.size && Arrays.equals(this.bits, other.bits)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (this.size * 31) + Arrays.hashCode(this.bits);
    }

    public String toString() {
        StringBuilder result = new StringBuilder(this.size);
        for (int i = 0; i < this.size; i++) {
            char c;
            if ((i & 7) == 0) {
                result.append(' ');
            }
            if (get(i)) {
                c = 'X';
            } else {
                c = '.';
            }
            result.append(c);
        }
        return result.toString();
    }

    public BitArray clone() {
        return new BitArray((int[]) this.bits.clone(), this.size);
    }
}
