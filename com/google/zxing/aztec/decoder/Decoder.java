package com.google.zxing.aztec.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.aztec.AztecDetectorResult;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;
import java.util.Arrays;

public final class Decoder {
    private static /* synthetic */ int[] $SWITCH_TABLE$com$google$zxing$aztec$decoder$Decoder$Table;
    private static final String[] DIGIT_TABLE = new String[]{"CTRL_PS", " ", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ",", ".", "CTRL_UL", "CTRL_US"};
    private static final String[] LOWER_TABLE = new String[]{"CTRL_PS", " ", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "CTRL_US", "CTRL_ML", "CTRL_DL", "CTRL_BS"};
    private static final String[] MIXED_TABLE = new String[]{"CTRL_PS", " ", "\u0001", "\u0002", "\u0003", "\u0004", "\u0005", "\u0006", "\u0007", "\b", "\t", "\n", "\u000b", "\f", "\r", "\u001b", "\u001c", "\u001d", "\u001e", "\u001f", "@", "\\", "^", "_", "`", "|", "~", "", "CTRL_LL", "CTRL_UL", "CTRL_PL", "CTRL_BS"};
    private static final String[] PUNCT_TABLE = new String[]{"", "\r", "\r\n", ". ", ", ", ": ", "!", "\"", "#", "$", "%", "&", "'", "(", ")", "*", "+", ",", "-", ".", "/", ":", ";", "<", "=", ">", "?", "[", "]", "{", "}", "CTRL_UL"};
    private static final String[] UPPER_TABLE = new String[]{"CTRL_PS", " ", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "CTRL_LL", "CTRL_ML", "CTRL_DL", "CTRL_BS"};
    private AztecDetectorResult ddata;

    private enum Table {
        UPPER,
        LOWER,
        MIXED,
        DIGIT,
        PUNCT,
        BINARY
    }

    static /* synthetic */ int[] $SWITCH_TABLE$com$google$zxing$aztec$decoder$Decoder$Table() {
        int[] iArr = $SWITCH_TABLE$com$google$zxing$aztec$decoder$Decoder$Table;
        if (iArr != null) {
            return iArr;
        }
        iArr = new int[Table.values().length];
        try {
            iArr[Table.BINARY.ordinal()] = 6;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Table.DIGIT.ordinal()] = 4;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Table.LOWER.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Table.MIXED.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[Table.PUNCT.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[Table.UPPER.ordinal()] = 1;
        } catch (NoSuchFieldError e6) {
        }
        $SWITCH_TABLE$com$google$zxing$aztec$decoder$Decoder$Table = iArr;
        return iArr;
    }

    public DecoderResult decode(AztecDetectorResult detectorResult) throws FormatException {
        this.ddata = detectorResult;
        return new DecoderResult(null, getEncodedData(correctBits(extractBits(detectorResult.getBits()))), null, null);
    }

    private static String getEncodedData(boolean[] correctedBits) {
        int endIndex = correctedBits.length;
        Table latchTable = Table.UPPER;
        Table shiftTable = Table.UPPER;
        StringBuilder result = new StringBuilder(20);
        int index = 0;
        while (index < endIndex) {
            if (shiftTable == Table.BINARY) {
                if (endIndex - index < 5) {
                    break;
                }
                int length = readCode(correctedBits, index, 5);
                index += 5;
                if (length == 0) {
                    if (endIndex - index < 11) {
                        break;
                    }
                    length = readCode(correctedBits, index, 11) + 31;
                    index += 11;
                }
                for (int charCount = 0; charCount < length; charCount++) {
                    if (endIndex - index < 8) {
                        index = endIndex;
                        break;
                    }
                    result.append((char) readCode(correctedBits, index, 8));
                    index += 8;
                }
                shiftTable = latchTable;
            } else {
                int size;
                if (shiftTable != Table.DIGIT) {
                    size = 5;
                } else {
                    size = 4;
                }
                if (endIndex - index < size) {
                    break;
                }
                int code = readCode(correctedBits, index, size);
                index += size;
                String str = getCharacter(shiftTable, code);
                if (str.startsWith("CTRL_")) {
                    shiftTable = getTable(str.charAt(5));
                    if (str.charAt(6) == 'L') {
                        latchTable = shiftTable;
                    }
                } else {
                    result.append(str);
                    shiftTable = latchTable;
                }
            }
        }
        return result.toString();
    }

    private static Table getTable(char t) {
        switch (t) {
            case 'B':
                return Table.BINARY;
            case 'D':
                return Table.DIGIT;
            case 'L':
                return Table.LOWER;
            case 'M':
                return Table.MIXED;
            case 'P':
                return Table.PUNCT;
            default:
                return Table.UPPER;
        }
    }

    private static String getCharacter(Table table, int code) {
        switch ($SWITCH_TABLE$com$google$zxing$aztec$decoder$Decoder$Table()[table.ordinal()]) {
            case 1:
                return UPPER_TABLE[code];
            case 2:
                return LOWER_TABLE[code];
            case 3:
                return MIXED_TABLE[code];
            case 4:
                return DIGIT_TABLE[code];
            case 5:
                return PUNCT_TABLE[code];
            default:
                throw new IllegalStateException("Bad table");
        }
    }

    private boolean[] correctBits(boolean[] rawbits) throws FormatException {
        int codewordSize;
        GenericGF gf;
        if (this.ddata.getNbLayers() <= 2) {
            codewordSize = 6;
            gf = GenericGF.AZTEC_DATA_6;
        } else if (this.ddata.getNbLayers() <= 8) {
            codewordSize = 8;
            gf = GenericGF.AZTEC_DATA_8;
        } else if (this.ddata.getNbLayers() > 22) {
            codewordSize = 12;
            gf = GenericGF.AZTEC_DATA_12;
        } else {
            codewordSize = 10;
            gf = GenericGF.AZTEC_DATA_10;
        }
        int numDataCodewords = this.ddata.getNbDatablocks();
        int numCodewords = rawbits.length / codewordSize;
        if (numCodewords >= numDataCodewords) {
            int offset = rawbits.length % codewordSize;
            int numECCodewords = numCodewords - numDataCodewords;
            int[] dataWords = new int[numCodewords];
            int i = 0;
            while (i < numCodewords) {
                dataWords[i] = readCode(rawbits, offset, codewordSize);
                i++;
                offset += codewordSize;
            }
            try {
                int dataWord;
                new ReedSolomonDecoder(gf).decode(dataWords, numECCodewords);
                int mask = (1 << codewordSize) - 1;
                int stuffedBits = 0;
                for (i = 0; i < numDataCodewords; i++) {
                    dataWord = dataWords[i];
                    if (dataWord == 0 || dataWord == mask) {
                        throw FormatException.getFormatInstance();
                    }
                    if (dataWord == 1 || dataWord == mask - 1) {
                        stuffedBits++;
                    }
                }
                boolean[] correctedBits = new boolean[((numDataCodewords * codewordSize) - stuffedBits)];
                int index = 0;
                for (i = 0; i < numDataCodewords; i++) {
                    dataWord = dataWords[i];
                    boolean z;
                    if (dataWord == 1 || dataWord == mask - 1) {
                        int i2 = (index + codewordSize) - 1;
                        if (dataWord <= 1) {
                            z = false;
                        } else {
                            z = true;
                        }
                        Arrays.fill(correctedBits, index, i2, z);
                        index += codewordSize - 1;
                    } else {
                        int bit = codewordSize - 1;
                        int index2 = index;
                        while (bit >= 0) {
                            index = index2 + 1;
                            if (((1 << bit) & dataWord) == 0) {
                                z = false;
                            } else {
                                z = true;
                            }
                            correctedBits[index2] = z;
                            bit--;
                            index2 = index;
                        }
                        index = index2;
                    }
                }
                return correctedBits;
            } catch (ReedSolomonException e) {
                throw FormatException.getFormatInstance();
            }
        }
        throw FormatException.getFormatInstance();
    }

    boolean[] extractBits(BitMatrix matrix) {
        int i;
        boolean compact = this.ddata.isCompact();
        int layers = this.ddata.getNbLayers();
        int baseMatrixSize = !compact ? (layers * 4) + 14 : (layers * 4) + 11;
        int[] alignmentMap = new int[baseMatrixSize];
        boolean[] rawbits = new boolean[totalBitsInLayer(layers, compact)];
        if (compact) {
            for (i = 0; i < alignmentMap.length; i++) {
                alignmentMap[i] = i;
            }
        } else {
            int origCenter = baseMatrixSize / 2;
            int center = ((baseMatrixSize + 1) + ((((baseMatrixSize / 2) - 1) / 15) * 2)) / 2;
            for (i = 0; i < origCenter; i++) {
                int newOffset = i + (i / 15);
                alignmentMap[(origCenter - i) - 1] = (center - newOffset) - 1;
                alignmentMap[origCenter + i] = (center + newOffset) + 1;
            }
        }
        int rowOffset = 0;
        for (i = 0; i < layers; i++) {
            int rowSize = !compact ? ((layers - i) * 4) + 12 : ((layers - i) * 4) + 9;
            int low = i * 2;
            int high = (baseMatrixSize - 1) - low;
            for (int j = 0; j < rowSize; j++) {
                int columnOffset = j * 2;
                for (int k = 0; k < 2; k++) {
                    rawbits[(rowOffset + columnOffset) + k] = matrix.get(alignmentMap[low + k], alignmentMap[low + j]);
                    rawbits[(((rowSize * 2) + rowOffset) + columnOffset) + k] = matrix.get(alignmentMap[low + j], alignmentMap[high - k]);
                    rawbits[(((rowSize * 4) + rowOffset) + columnOffset) + k] = matrix.get(alignmentMap[high - k], alignmentMap[high - j]);
                    rawbits[(((rowSize * 6) + rowOffset) + columnOffset) + k] = matrix.get(alignmentMap[high - j], alignmentMap[low + k]);
                }
            }
            rowOffset += rowSize * 8;
        }
        return rawbits;
    }

    private static int readCode(boolean[] rawbits, int startIndex, int length) {
        int res = 0;
        for (int i = startIndex; i < startIndex + length; i++) {
            res <<= 1;
            if (rawbits[i]) {
                res |= 1;
            }
        }
        return res;
    }

    private static int totalBitsInLayer(int layers, boolean compact) {
        return ((!compact ? 112 : 88) + (layers * 16)) * layers;
    }
}
