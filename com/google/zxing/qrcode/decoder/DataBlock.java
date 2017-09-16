package com.google.zxing.qrcode.decoder;

import com.google.zxing.qrcode.decoder.Version.ECB;
import com.google.zxing.qrcode.decoder.Version.ECBlocks;

final class DataBlock {
    private final byte[] codewords;
    private final int numDataCodewords;

    private DataBlock(int numDataCodewords, byte[] codewords) {
        this.numDataCodewords = numDataCodewords;
        this.codewords = codewords;
    }

    static DataBlock[] getDataBlocks(byte[] rawCodewords, Version version, ErrorCorrectionLevel ecLevel) {
        if (rawCodewords.length == version.getTotalCodewords()) {
            ECB ecBlock;
            int i;
            int j;
            int rawCodewordsOffset;
            ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
            int totalBlocks = 0;
            ECB[] ecBlockArray = ecBlocks.getECBlocks();
            for (ECB ecBlock2 : ecBlockArray) {
                totalBlocks += ecBlock2.getCount();
            }
            DataBlock[] result = new DataBlock[totalBlocks];
            int numResultBlocks = 0;
            int length = ecBlockArray.length;
            int i2 = 0;
            while (i2 < length) {
                ecBlock2 = ecBlockArray[i2];
                i = 0;
                int numResultBlocks2 = numResultBlocks;
                while (i < ecBlock2.getCount()) {
                    int numDataCodewords = ecBlock2.getDataCodewords();
                    numResultBlocks = numResultBlocks2 + 1;
                    result[numResultBlocks2] = new DataBlock(numDataCodewords, new byte[(ecBlocks.getECCodewordsPerBlock() + numDataCodewords)]);
                    i++;
                    numResultBlocks2 = numResultBlocks;
                }
                i2++;
                numResultBlocks = numResultBlocks2;
            }
            int shorterBlocksTotalCodewords = result[0].codewords.length;
            int longerBlocksStartAt = result.length - 1;
            while (longerBlocksStartAt >= 0 && result[longerBlocksStartAt].codewords.length != shorterBlocksTotalCodewords) {
                longerBlocksStartAt--;
            }
            longerBlocksStartAt++;
            int shorterBlocksNumDataCodewords = shorterBlocksTotalCodewords - ecBlocks.getECCodewordsPerBlock();
            int rawCodewordsOffset2 = 0;
            i = 0;
            while (i < shorterBlocksNumDataCodewords) {
                j = 0;
                rawCodewordsOffset = rawCodewordsOffset2;
                while (j < numResultBlocks) {
                    rawCodewordsOffset2 = rawCodewordsOffset + 1;
                    result[j].codewords[i] = (byte) rawCodewords[rawCodewordsOffset];
                    j++;
                    rawCodewordsOffset = rawCodewordsOffset2;
                }
                i++;
                rawCodewordsOffset2 = rawCodewordsOffset;
            }
            j = longerBlocksStartAt;
            rawCodewordsOffset = rawCodewordsOffset2;
            while (j < numResultBlocks) {
                rawCodewordsOffset2 = rawCodewordsOffset + 1;
                result[j].codewords[shorterBlocksNumDataCodewords] = (byte) rawCodewords[rawCodewordsOffset];
                j++;
                rawCodewordsOffset = rawCodewordsOffset2;
            }
            int max = result[0].codewords.length;
            i = shorterBlocksNumDataCodewords;
            rawCodewordsOffset2 = rawCodewordsOffset;
            while (i < max) {
                j = 0;
                rawCodewordsOffset = rawCodewordsOffset2;
                while (j < numResultBlocks) {
                    int iOffset;
                    if (j >= longerBlocksStartAt) {
                        iOffset = i + 1;
                    } else {
                        iOffset = i;
                    }
                    rawCodewordsOffset2 = rawCodewordsOffset + 1;
                    result[j].codewords[iOffset] = (byte) rawCodewords[rawCodewordsOffset];
                    j++;
                    rawCodewordsOffset = rawCodewordsOffset2;
                }
                i++;
                rawCodewordsOffset2 = rawCodewordsOffset;
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    int getNumDataCodewords() {
        return this.numDataCodewords;
    }

    byte[] getCodewords() {
        return this.codewords;
    }
}
