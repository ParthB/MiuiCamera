package com.google.zxing.datamatrix.decoder;

final class DataBlock {
    private final byte[] codewords;
    private final int numDataCodewords;

    private DataBlock(int numDataCodewords, byte[] codewords) {
        this.numDataCodewords = numDataCodewords;
        this.codewords = codewords;
    }

    static DataBlock[] getDataBlocks(byte[] rawCodewords, Version version) {
        ECB ecBlock;
        int i;
        int j;
        int rawCodewordsOffset;
        boolean specialVersion;
        int numLongerBlocks;
        ECBlocks ecBlocks = version.getECBlocks();
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
                result[numResultBlocks2] = new DataBlock(numDataCodewords, new byte[(ecBlocks.getECCodewords() + numDataCodewords)]);
                i++;
                numResultBlocks2 = numResultBlocks;
            }
            i2++;
            numResultBlocks = numResultBlocks2;
        }
        int longerBlocksNumDataCodewords = result[0].codewords.length - ecBlocks.getECCodewords();
        int shorterBlocksNumDataCodewords = longerBlocksNumDataCodewords - 1;
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
        if (version.getVersionNumber() != 24) {
            specialVersion = false;
        } else {
            specialVersion = true;
        }
        if (specialVersion) {
            numLongerBlocks = 8;
        } else {
            numLongerBlocks = numResultBlocks;
        }
        j = 0;
        rawCodewordsOffset = rawCodewordsOffset2;
        while (j < numLongerBlocks) {
            rawCodewordsOffset2 = rawCodewordsOffset + 1;
            result[j].codewords[longerBlocksNumDataCodewords - 1] = (byte) rawCodewords[rawCodewordsOffset];
            j++;
            rawCodewordsOffset = rawCodewordsOffset2;
        }
        int max = result[0].codewords.length;
        i = longerBlocksNumDataCodewords;
        rawCodewordsOffset2 = rawCodewordsOffset;
        while (i < max) {
            j = 0;
            rawCodewordsOffset = rawCodewordsOffset2;
            while (j < numResultBlocks) {
                int iOffset = (specialVersion && j > 7) ? i - 1 : i;
                rawCodewordsOffset2 = rawCodewordsOffset + 1;
                result[j].codewords[iOffset] = (byte) rawCodewords[rawCodewordsOffset];
                j++;
                rawCodewordsOffset = rawCodewordsOffset2;
            }
            i++;
            rawCodewordsOffset2 = rawCodewordsOffset;
        }
        if (rawCodewordsOffset2 == rawCodewords.length) {
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
