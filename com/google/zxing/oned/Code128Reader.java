package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Code128Reader extends OneDReader {
    static final int[][] CODE_PATTERNS;

    static {
        r0 = new int[107][];
        r0[0] = new int[]{2, 1, 2, 2, 2, 2};
        r0[1] = new int[]{2, 2, 2, 1, 2, 2};
        r0[2] = new int[]{2, 2, 2, 2, 2, 1};
        r0[3] = new int[]{1, 2, 1, 2, 2, 3};
        r0[4] = new int[]{1, 2, 1, 3, 2, 2};
        r0[5] = new int[]{1, 3, 1, 2, 2, 2};
        r0[6] = new int[]{1, 2, 2, 2, 1, 3};
        r0[7] = new int[]{1, 2, 2, 3, 1, 2};
        r0[8] = new int[]{1, 3, 2, 2, 1, 2};
        r0[9] = new int[]{2, 2, 1, 2, 1, 3};
        r0[10] = new int[]{2, 2, 1, 3, 1, 2};
        r0[11] = new int[]{2, 3, 1, 2, 1, 2};
        r0[12] = new int[]{1, 1, 2, 2, 3, 2};
        r0[13] = new int[]{1, 2, 2, 1, 3, 2};
        r0[14] = new int[]{1, 2, 2, 2, 3, 1};
        r0[15] = new int[]{1, 1, 3, 2, 2, 2};
        r0[16] = new int[]{1, 2, 3, 1, 2, 2};
        r0[17] = new int[]{1, 2, 3, 2, 2, 1};
        r0[18] = new int[]{2, 2, 3, 2, 1, 1};
        r0[19] = new int[]{2, 2, 1, 1, 3, 2};
        r0[20] = new int[]{2, 2, 1, 2, 3, 1};
        r0[21] = new int[]{2, 1, 3, 2, 1, 2};
        r0[22] = new int[]{2, 2, 3, 1, 1, 2};
        r0[23] = new int[]{3, 1, 2, 1, 3, 1};
        r0[24] = new int[]{3, 1, 1, 2, 2, 2};
        r0[25] = new int[]{3, 2, 1, 1, 2, 2};
        r0[26] = new int[]{3, 2, 1, 2, 2, 1};
        r0[27] = new int[]{3, 1, 2, 2, 1, 2};
        r0[28] = new int[]{3, 2, 2, 1, 1, 2};
        r0[29] = new int[]{3, 2, 2, 2, 1, 1};
        r0[30] = new int[]{2, 1, 2, 1, 2, 3};
        r0[31] = new int[]{2, 1, 2, 3, 2, 1};
        r0[32] = new int[]{2, 3, 2, 1, 2, 1};
        r0[33] = new int[]{1, 1, 1, 3, 2, 3};
        r0[34] = new int[]{1, 3, 1, 1, 2, 3};
        r0[35] = new int[]{1, 3, 1, 3, 2, 1};
        r0[36] = new int[]{1, 1, 2, 3, 1, 3};
        r0[37] = new int[]{1, 3, 2, 1, 1, 3};
        r0[38] = new int[]{1, 3, 2, 3, 1, 1};
        r0[39] = new int[]{2, 1, 1, 3, 1, 3};
        r0[40] = new int[]{2, 3, 1, 1, 1, 3};
        r0[41] = new int[]{2, 3, 1, 3, 1, 1};
        r0[42] = new int[]{1, 1, 2, 1, 3, 3};
        r0[43] = new int[]{1, 1, 2, 3, 3, 1};
        r0[44] = new int[]{1, 3, 2, 1, 3, 1};
        r0[45] = new int[]{1, 1, 3, 1, 2, 3};
        r0[46] = new int[]{1, 1, 3, 3, 2, 1};
        r0[47] = new int[]{1, 3, 3, 1, 2, 1};
        r0[48] = new int[]{3, 1, 3, 1, 2, 1};
        r0[49] = new int[]{2, 1, 1, 3, 3, 1};
        r0[50] = new int[]{2, 3, 1, 1, 3, 1};
        r0[51] = new int[]{2, 1, 3, 1, 1, 3};
        r0[52] = new int[]{2, 1, 3, 3, 1, 1};
        r0[53] = new int[]{2, 1, 3, 1, 3, 1};
        r0[54] = new int[]{3, 1, 1, 1, 2, 3};
        r0[55] = new int[]{3, 1, 1, 3, 2, 1};
        r0[56] = new int[]{3, 3, 1, 1, 2, 1};
        r0[57] = new int[]{3, 1, 2, 1, 1, 3};
        r0[58] = new int[]{3, 1, 2, 3, 1, 1};
        r0[59] = new int[]{3, 3, 2, 1, 1, 1};
        r0[60] = new int[]{3, 1, 4, 1, 1, 1};
        r0[61] = new int[]{2, 2, 1, 4, 1, 1};
        r0[62] = new int[]{4, 3, 1, 1, 1, 1};
        r0[63] = new int[]{1, 1, 1, 2, 2, 4};
        r0[64] = new int[]{1, 1, 1, 4, 2, 2};
        r0[65] = new int[]{1, 2, 1, 1, 2, 4};
        r0[66] = new int[]{1, 2, 1, 4, 2, 1};
        r0[67] = new int[]{1, 4, 1, 1, 2, 2};
        r0[68] = new int[]{1, 4, 1, 2, 2, 1};
        r0[69] = new int[]{1, 1, 2, 2, 1, 4};
        r0[70] = new int[]{1, 1, 2, 4, 1, 2};
        r0[71] = new int[]{1, 2, 2, 1, 1, 4};
        r0[72] = new int[]{1, 2, 2, 4, 1, 1};
        r0[73] = new int[]{1, 4, 2, 1, 1, 2};
        r0[74] = new int[]{1, 4, 2, 2, 1, 1};
        r0[75] = new int[]{2, 4, 1, 2, 1, 1};
        r0[76] = new int[]{2, 2, 1, 1, 1, 4};
        r0[77] = new int[]{4, 1, 3, 1, 1, 1};
        r0[78] = new int[]{2, 4, 1, 1, 1, 2};
        r0[79] = new int[]{1, 3, 4, 1, 1, 1};
        r0[80] = new int[]{1, 1, 1, 2, 4, 2};
        r0[81] = new int[]{1, 2, 1, 1, 4, 2};
        r0[82] = new int[]{1, 2, 1, 2, 4, 1};
        r0[83] = new int[]{1, 1, 4, 2, 1, 2};
        r0[84] = new int[]{1, 2, 4, 1, 1, 2};
        r0[85] = new int[]{1, 2, 4, 2, 1, 1};
        r0[86] = new int[]{4, 1, 1, 2, 1, 2};
        r0[87] = new int[]{4, 2, 1, 1, 1, 2};
        r0[88] = new int[]{4, 2, 1, 2, 1, 1};
        r0[89] = new int[]{2, 1, 2, 1, 4, 1};
        r0[90] = new int[]{2, 1, 4, 1, 2, 1};
        r0[91] = new int[]{4, 1, 2, 1, 2, 1};
        r0[92] = new int[]{1, 1, 1, 1, 4, 3};
        r0[93] = new int[]{1, 1, 1, 3, 4, 1};
        r0[94] = new int[]{1, 3, 1, 1, 4, 1};
        r0[95] = new int[]{1, 1, 4, 1, 1, 3};
        r0[96] = new int[]{1, 1, 4, 3, 1, 1};
        r0[97] = new int[]{4, 1, 1, 1, 1, 3};
        r0[98] = new int[]{4, 1, 1, 3, 1, 1};
        r0[99] = new int[]{1, 1, 3, 1, 4, 1};
        r0[100] = new int[]{1, 1, 4, 1, 3, 1};
        r0[101] = new int[]{3, 1, 1, 1, 4, 1};
        r0[102] = new int[]{4, 1, 1, 1, 3, 1};
        r0[103] = new int[]{2, 1, 1, 4, 1, 2};
        r0[104] = new int[]{2, 1, 1, 2, 1, 4};
        r0[105] = new int[]{2, 1, 1, 2, 3, 2};
        r0[106] = new int[]{2, 3, 3, 1, 1, 1, 2};
        CODE_PATTERNS = r0;
    }

    private static int[] findStartPattern(BitArray row) throws NotFoundException {
        int width = row.getSize();
        int rowOffset = row.getNextSet(0);
        int counterPosition = 0;
        int[] counters = new int[6];
        int patternStart = rowOffset;
        int isWhite = 0;
        int patternLength = counters.length;
        int i = rowOffset;
        while (i < width) {
            if ((row.get(i) ^ isWhite) == 0) {
                if (counterPosition != patternLength - 1) {
                    counterPosition++;
                } else {
                    float bestVariance = 0.25f;
                    int bestMatch = -1;
                    for (int startCode = 103; startCode <= 105; startCode++) {
                        float variance = OneDReader.patternMatchVariance(counters, CODE_PATTERNS[startCode], 0.7f);
                        if (variance < bestVariance) {
                            bestVariance = variance;
                            bestMatch = startCode;
                        }
                    }
                    if (bestMatch >= 0 && row.isRange(Math.max(0, patternStart - ((i - patternStart) / 2)), patternStart, false)) {
                        return new int[]{patternStart, i, bestMatch};
                    }
                    patternStart += counters[0] + counters[1];
                    System.arraycopy(counters, 2, counters, 0, patternLength - 2);
                    counters[patternLength - 2] = 0;
                    counters[patternLength - 1] = 0;
                    counterPosition--;
                }
                counters[counterPosition] = 1;
                if (isWhite == 0) {
                    isWhite = 1;
                } else {
                    isWhite = 0;
                }
            } else {
                counters[counterPosition] = counters[counterPosition] + 1;
            }
            i++;
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private static int decodeCode(BitArray row, int[] counters, int rowOffset) throws NotFoundException {
        OneDReader.recordPattern(row, rowOffset, counters);
        float bestVariance = 0.25f;
        int bestMatch = -1;
        for (int d = 0; d < CODE_PATTERNS.length; d++) {
            float variance = OneDReader.patternMatchVariance(counters, CODE_PATTERNS[d], 0.7f);
            if (variance < bestVariance) {
                bestVariance = variance;
                bestMatch = d;
            }
        }
        if (bestMatch >= 0) {
            return bestMatch;
        }
        throw NotFoundException.getNotFoundInstance();
    }

    public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> hints) throws NotFoundException, FormatException, ChecksumException {
        int codeSet;
        boolean convertFNC1 = hints != null && hints.containsKey(DecodeHintType.ASSUME_GS1);
        int[] startPatternInfo = findStartPattern(row);
        int startCode = startPatternInfo[2];
        List<Byte> arrayList = new ArrayList(20);
        arrayList.add(Byte.valueOf((byte) startCode));
        switch (startCode) {
            case 103:
                codeSet = 101;
                break;
            case 104:
                codeSet = 100;
                break;
            case 105:
                codeSet = 99;
                break;
            default:
                throw FormatException.getFormatInstance();
        }
        boolean done = false;
        boolean isNextShifted = false;
        StringBuilder stringBuilder = new StringBuilder(20);
        int lastStart = startPatternInfo[0];
        int nextStart = startPatternInfo[1];
        int[] counters = new int[6];
        int lastCode = 0;
        int code = 0;
        int checksumTotal = startCode;
        int multiplier = 0;
        boolean lastCharacterWasPrintable = true;
        boolean upperMode = false;
        boolean shiftUpperMode = false;
        while (!done) {
            boolean unshift = isNextShifted;
            isNextShifted = false;
            lastCode = code;
            code = decodeCode(row, counters, nextStart);
            arrayList.add(Byte.valueOf((byte) code));
            if (code != 106) {
                lastCharacterWasPrintable = true;
            }
            if (code != 106) {
                multiplier++;
                checksumTotal += multiplier * code;
            }
            lastStart = nextStart;
            for (int counter : counters) {
                nextStart += counter;
            }
            switch (code) {
                case 103:
                case 104:
                case 105:
                    throw FormatException.getFormatInstance();
                default:
                    switch (codeSet) {
                        case 99:
                            if (code < 100) {
                                if (code < 10) {
                                    stringBuilder.append('0');
                                }
                                stringBuilder.append(code);
                                break;
                            }
                            if (code != 106) {
                                lastCharacterWasPrintable = false;
                            }
                            switch (code) {
                                case 100:
                                    codeSet = 100;
                                    break;
                                case 101:
                                    codeSet = 101;
                                    break;
                                case 102:
                                    if (convertFNC1) {
                                        if (stringBuilder.length() == 0) {
                                            stringBuilder.append("]C1");
                                            break;
                                        }
                                        stringBuilder.append('\u001d');
                                        break;
                                    }
                                    break;
                                case 103:
                                case 104:
                                case 105:
                                    break;
                                case 106:
                                    done = true;
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case 100:
                            if (code < 96) {
                                if (shiftUpperMode != upperMode) {
                                    stringBuilder.append((char) ((code + 32) + 128));
                                } else {
                                    stringBuilder.append((char) (code + 32));
                                }
                                shiftUpperMode = false;
                                break;
                            }
                            if (code != 106) {
                                lastCharacterWasPrintable = false;
                            }
                            switch (code) {
                                case 96:
                                case 97:
                                case 103:
                                case 104:
                                case 105:
                                    break;
                                case 98:
                                    isNextShifted = true;
                                    codeSet = 101;
                                    break;
                                case 99:
                                    codeSet = 99;
                                    break;
                                case 100:
                                    if (upperMode || !shiftUpperMode) {
                                        if (!upperMode || !shiftUpperMode) {
                                            shiftUpperMode = true;
                                            break;
                                        }
                                        upperMode = false;
                                        shiftUpperMode = false;
                                        break;
                                    }
                                    upperMode = true;
                                    shiftUpperMode = false;
                                    break;
                                    break;
                                case 101:
                                    codeSet = 101;
                                    break;
                                case 102:
                                    if (convertFNC1) {
                                        if (stringBuilder.length() == 0) {
                                            stringBuilder.append("]C1");
                                            break;
                                        }
                                        stringBuilder.append('\u001d');
                                        break;
                                    }
                                    break;
                                case 106:
                                    done = true;
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case 101:
                            if (code >= 64) {
                                if (code < 96) {
                                    if (shiftUpperMode != upperMode) {
                                        stringBuilder.append((char) (code + 64));
                                    } else {
                                        stringBuilder.append((char) (code - 64));
                                    }
                                    shiftUpperMode = false;
                                    break;
                                }
                                if (code != 106) {
                                    lastCharacterWasPrintable = false;
                                }
                                switch (code) {
                                    case 96:
                                    case 97:
                                    case 103:
                                    case 104:
                                    case 105:
                                        break;
                                    case 98:
                                        isNextShifted = true;
                                        codeSet = 100;
                                        break;
                                    case 99:
                                        codeSet = 99;
                                        break;
                                    case 100:
                                        codeSet = 100;
                                        break;
                                    case 101:
                                        if (upperMode || !shiftUpperMode) {
                                            if (!upperMode || !shiftUpperMode) {
                                                shiftUpperMode = true;
                                                break;
                                            }
                                            upperMode = false;
                                            shiftUpperMode = false;
                                            break;
                                        }
                                        upperMode = true;
                                        shiftUpperMode = false;
                                        break;
                                        break;
                                    case 102:
                                        if (convertFNC1) {
                                            if (stringBuilder.length() == 0) {
                                                stringBuilder.append("]C1");
                                                break;
                                            }
                                            stringBuilder.append('\u001d');
                                            break;
                                        }
                                        break;
                                    case 106:
                                        done = true;
                                        break;
                                    default:
                                        break;
                                }
                            }
                            if (shiftUpperMode != upperMode) {
                                stringBuilder.append((char) ((code + 32) + 128));
                            } else {
                                stringBuilder.append((char) (code + 32));
                            }
                            shiftUpperMode = false;
                            break;
                            break;
                    }
                    if (unshift) {
                        if (codeSet != 101) {
                            codeSet = 101;
                        } else {
                            codeSet = 100;
                        }
                    }
                    break;
            }
        }
        int lastPatternSize = nextStart - lastStart;
        nextStart = row.getNextUnset(nextStart);
        if (!row.isRange(nextStart, Math.min(row.getSize(), ((nextStart - lastStart) / 2) + nextStart), false)) {
            throw NotFoundException.getNotFoundInstance();
        } else if ((checksumTotal - (multiplier * lastCode)) % 103 == lastCode) {
            int resultLength = stringBuilder.length();
            if (resultLength != 0) {
                if (resultLength > 0 && lastCharacterWasPrintable) {
                    if (codeSet != 99) {
                        stringBuilder.delete(resultLength - 1, resultLength);
                    } else {
                        stringBuilder.delete(resultLength - 2, resultLength);
                    }
                }
                float left = ((float) (startPatternInfo[1] + startPatternInfo[0])) / 2.0f;
                float right = ((float) lastStart) + (((float) lastPatternSize) / 2.0f);
                int rawCodesSize = arrayList.size();
                byte[] rawBytes = new byte[rawCodesSize];
                for (int i = 0; i < rawCodesSize; i++) {
                    rawBytes[i] = (byte) ((Byte) arrayList.get(i)).byteValue();
                }
                return new Result(stringBuilder.toString(), rawBytes, new ResultPoint[]{new ResultPoint(left, (float) rowNumber), new ResultPoint(right, (float) rowNumber)}, BarcodeFormat.CODE_128);
            }
            throw NotFoundException.getNotFoundInstance();
        } else {
            throw ChecksumException.getChecksumInstance();
        }
    }
}
