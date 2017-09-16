package com.google.zxing.pdf417.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.common.CharacterSetECI;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.pdf417.PDF417ResultMetadata;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;

final class DecodedBitStreamParser {
    private static /* synthetic */ int[] $SWITCH_TABLE$com$google$zxing$pdf417$decoder$DecodedBitStreamParser$Mode;
    private static final Charset DEFAULT_ENCODING = Charset.forName("ISO-8859-1");
    private static final BigInteger[] EXP900 = new BigInteger[16];
    private static final char[] MIXED_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '&', '\r', '\t', ',', ':', '#', '-', '.', '$', '/', '+', '%', '*', '=', '^'};
    private static final char[] PUNCT_CHARS = new char[]{';', '<', '>', '@', '[', '\\', ']', '_', '`', '~', '!', '\r', '\t', ',', ':', '\n', '-', '.', '$', '/', '\"', '|', '*', '(', ')', '?', '{', '}', '\''};

    private enum Mode {
        ALPHA,
        LOWER,
        MIXED,
        PUNCT,
        ALPHA_SHIFT,
        PUNCT_SHIFT
    }

    static /* synthetic */ int[] $SWITCH_TABLE$com$google$zxing$pdf417$decoder$DecodedBitStreamParser$Mode() {
        int[] iArr = $SWITCH_TABLE$com$google$zxing$pdf417$decoder$DecodedBitStreamParser$Mode;
        if (iArr != null) {
            return iArr;
        }
        iArr = new int[Mode.values().length];
        try {
            iArr[Mode.ALPHA.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Mode.ALPHA_SHIFT.ordinal()] = 5;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Mode.LOWER.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Mode.MIXED.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[Mode.PUNCT.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[Mode.PUNCT_SHIFT.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        $SWITCH_TABLE$com$google$zxing$pdf417$decoder$DecodedBitStreamParser$Mode = iArr;
        return iArr;
    }

    static {
        EXP900[0] = BigInteger.ONE;
        BigInteger nineHundred = BigInteger.valueOf(900);
        EXP900[1] = nineHundred;
        for (int i = 2; i < EXP900.length; i++) {
            EXP900[i] = EXP900[i - 1].multiply(nineHundred);
        }
    }

    private DecodedBitStreamParser() {
    }

    static DecoderResult decode(int[] codewords, String ecLevel) throws FormatException {
        StringBuilder result = new StringBuilder(codewords.length * 2);
        Charset encoding = DEFAULT_ENCODING;
        int code = codewords[1];
        PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();
        int codeIndex = 2;
        while (codeIndex < codewords[0]) {
            int codeIndex2;
            switch (code) {
                case 900:
                    codeIndex2 = textCompaction(codewords, codeIndex, result);
                    break;
                case 901:
                case 924:
                    codeIndex2 = byteCompaction(code, codewords, encoding, codeIndex, result);
                    break;
                case 902:
                    codeIndex2 = numericCompaction(codewords, codeIndex, result);
                    break;
                case 913:
                    codeIndex2 = codeIndex + 1;
                    result.append((char) codewords[codeIndex]);
                    break;
                case 922:
                case 923:
                    throw FormatException.getFormatInstance();
                case 925:
                    codeIndex2 = codeIndex + 1;
                    break;
                case 926:
                    codeIndex2 = codeIndex + 2;
                    break;
                case 927:
                    codeIndex2 = codeIndex + 1;
                    encoding = Charset.forName(CharacterSetECI.getCharacterSetECIByValue(codewords[codeIndex]).name());
                    break;
                case 928:
                    codeIndex2 = decodeMacroBlock(codewords, codeIndex, resultMetadata);
                    break;
                default:
                    codeIndex2 = textCompaction(codewords, codeIndex - 1, result);
                    break;
            }
            if (codeIndex2 >= codewords.length) {
                throw FormatException.getFormatInstance();
            }
            codeIndex = codeIndex2 + 1;
            code = codewords[codeIndex2];
        }
        if (result.length() != 0) {
            DecoderResult decoderResult = new DecoderResult(null, result.toString(), null, ecLevel);
            decoderResult.setOther(resultMetadata);
            return decoderResult;
        }
        throw FormatException.getFormatInstance();
    }

    private static int decodeMacroBlock(int[] codewords, int codeIndex, PDF417ResultMetadata resultMetadata) throws FormatException {
        if (codeIndex + 2 <= codewords[0]) {
            int[] segmentIndexArray = new int[2];
            int i = 0;
            while (i < 2) {
                segmentIndexArray[i] = codewords[codeIndex];
                i++;
                codeIndex++;
            }
            resultMetadata.setSegmentIndex(Integer.parseInt(decodeBase900toBase10(segmentIndexArray, 2)));
            StringBuilder fileId = new StringBuilder();
            codeIndex = textCompaction(codewords, codeIndex, fileId);
            resultMetadata.setFileId(fileId.toString());
            if (codewords[codeIndex] == 923) {
                codeIndex++;
                int[] additionalOptionCodeWords = new int[(codewords[0] - codeIndex)];
                boolean end = false;
                int additionalOptionCodeWordsIndex = 0;
                int codeIndex2 = codeIndex;
                while (codeIndex2 < codewords[0] && !end) {
                    codeIndex = codeIndex2 + 1;
                    int code = codewords[codeIndex2];
                    if (code >= 900) {
                        switch (code) {
                            case 922:
                                resultMetadata.setLastSegment(true);
                                end = true;
                                codeIndex2 = codeIndex + 1;
                                break;
                            default:
                                throw FormatException.getFormatInstance();
                        }
                    }
                    int additionalOptionCodeWordsIndex2 = additionalOptionCodeWordsIndex + 1;
                    additionalOptionCodeWords[additionalOptionCodeWordsIndex] = code;
                    additionalOptionCodeWordsIndex = additionalOptionCodeWordsIndex2;
                    codeIndex2 = codeIndex;
                }
                resultMetadata.setOptionalData(Arrays.copyOf(additionalOptionCodeWords, additionalOptionCodeWordsIndex));
                return codeIndex2;
            } else if (codewords[codeIndex] != 922) {
                return codeIndex;
            } else {
                resultMetadata.setLastSegment(true);
                return codeIndex + 1;
            }
        }
        throw FormatException.getFormatInstance();
    }

    private static int textCompaction(int[] codewords, int codeIndex, StringBuilder result) {
        int[] textCompactionData = new int[((codewords[0] - codeIndex) * 2)];
        int[] byteCompactionData = new int[((codewords[0] - codeIndex) * 2)];
        boolean end = false;
        int index = 0;
        int codeIndex2 = codeIndex;
        while (codeIndex2 < codewords[0] && !end) {
            codeIndex = codeIndex2 + 1;
            int code = codewords[codeIndex2];
            if (code >= 900) {
                switch (code) {
                    case 900:
                        int index2 = index + 1;
                        textCompactionData[index] = 900;
                        index = index2;
                        codeIndex2 = codeIndex;
                        break;
                    case 901:
                    case 902:
                    case 922:
                    case 923:
                    case 924:
                    case 928:
                        end = true;
                        codeIndex2 = codeIndex - 1;
                        break;
                    case 913:
                        textCompactionData[index] = 913;
                        codeIndex2 = codeIndex + 1;
                        byteCompactionData[index] = codewords[codeIndex];
                        index++;
                        break;
                    default:
                        codeIndex2 = codeIndex;
                        break;
                }
            }
            textCompactionData[index] = code / 30;
            textCompactionData[index + 1] = code % 30;
            index += 2;
            codeIndex2 = codeIndex;
        }
        decodeTextCompaction(textCompactionData, byteCompactionData, index, result);
        return codeIndex2;
    }

    private static void decodeTextCompaction(int[] textCompactionData, int[] byteCompactionData, int length, StringBuilder result) {
        Mode subMode = Mode.ALPHA;
        Mode priorToShiftMode = Mode.ALPHA;
        for (int i = 0; i < length; i++) {
            int subModeCh = textCompactionData[i];
            char ch = '\u0000';
            switch ($SWITCH_TABLE$com$google$zxing$pdf417$decoder$DecodedBitStreamParser$Mode()[subMode.ordinal()]) {
                case 1:
                    if (subModeCh >= 26) {
                        if (subModeCh != 26) {
                            if (subModeCh != 27) {
                                if (subModeCh != 28) {
                                    if (subModeCh != 29) {
                                        if (subModeCh != 913) {
                                            if (subModeCh == 900) {
                                                subMode = Mode.ALPHA;
                                                break;
                                            }
                                        }
                                        result.append((char) byteCompactionData[i]);
                                        break;
                                    }
                                    priorToShiftMode = subMode;
                                    subMode = Mode.PUNCT_SHIFT;
                                    break;
                                }
                                subMode = Mode.MIXED;
                                break;
                            }
                            subMode = Mode.LOWER;
                            break;
                        }
                        ch = ' ';
                        break;
                    }
                    ch = (char) (subModeCh + 65);
                    break;
                    break;
                case 2:
                    if (subModeCh >= 26) {
                        if (subModeCh != 26) {
                            if (subModeCh != 27) {
                                if (subModeCh != 28) {
                                    if (subModeCh != 29) {
                                        if (subModeCh != 913) {
                                            if (subModeCh == 900) {
                                                subMode = Mode.ALPHA;
                                                break;
                                            }
                                        }
                                        result.append((char) byteCompactionData[i]);
                                        break;
                                    }
                                    priorToShiftMode = subMode;
                                    subMode = Mode.PUNCT_SHIFT;
                                    break;
                                }
                                subMode = Mode.MIXED;
                                break;
                            }
                            priorToShiftMode = subMode;
                            subMode = Mode.ALPHA_SHIFT;
                            break;
                        }
                        ch = ' ';
                        break;
                    }
                    ch = (char) (subModeCh + 97);
                    break;
                    break;
                case 3:
                    if (subModeCh >= 25) {
                        if (subModeCh != 25) {
                            if (subModeCh != 26) {
                                if (subModeCh != 27) {
                                    if (subModeCh != 28) {
                                        if (subModeCh != 29) {
                                            if (subModeCh != 913) {
                                                if (subModeCh == 900) {
                                                    subMode = Mode.ALPHA;
                                                    break;
                                                }
                                            }
                                            result.append((char) byteCompactionData[i]);
                                            break;
                                        }
                                        priorToShiftMode = subMode;
                                        subMode = Mode.PUNCT_SHIFT;
                                        break;
                                    }
                                    subMode = Mode.ALPHA;
                                    break;
                                }
                                subMode = Mode.LOWER;
                                break;
                            }
                            ch = ' ';
                            break;
                        }
                        subMode = Mode.PUNCT;
                        break;
                    }
                    ch = MIXED_CHARS[subModeCh];
                    break;
                    break;
                case 4:
                    if (subModeCh >= 29) {
                        if (subModeCh != 29) {
                            if (subModeCh != 913) {
                                if (subModeCh == 900) {
                                    subMode = Mode.ALPHA;
                                    break;
                                }
                            }
                            result.append((char) byteCompactionData[i]);
                            break;
                        }
                        subMode = Mode.ALPHA;
                        break;
                    }
                    ch = PUNCT_CHARS[subModeCh];
                    break;
                    break;
                case 5:
                    subMode = priorToShiftMode;
                    if (subModeCh >= 26) {
                        if (subModeCh != 26) {
                            if (subModeCh == 900) {
                                subMode = Mode.ALPHA;
                                break;
                            }
                        }
                        ch = ' ';
                        break;
                    }
                    ch = (char) (subModeCh + 65);
                    break;
                    break;
                case 6:
                    subMode = priorToShiftMode;
                    if (subModeCh >= 29) {
                        if (subModeCh != 29) {
                            if (subModeCh != 913) {
                                if (subModeCh == 900) {
                                    subMode = Mode.ALPHA;
                                    break;
                                }
                            }
                            result.append((char) byteCompactionData[i]);
                            break;
                        }
                        subMode = Mode.ALPHA;
                        break;
                    }
                    ch = PUNCT_CHARS[subModeCh];
                    break;
                    break;
            }
            if (ch != '\u0000') {
                result.append(ch);
            }
        }
    }

    private static int byteCompaction(int mode, int[] codewords, Charset encoding, int codeIndex, StringBuilder result) {
        ByteArrayOutputStream decodedBytes = new ByteArrayOutputStream();
        long value;
        boolean end;
        int codeIndex2;
        int count;
        int j;
        if (mode == 901) {
            value = 0;
            int[] byteCompactedCodewords = new int[6];
            end = false;
            codeIndex2 = codeIndex + 1;
            int nextCode = codewords[codeIndex];
            int count2 = 0;
            while (codeIndex2 < codewords[0] && !end) {
                count = count2 + 1;
                byteCompactedCodewords[count2] = nextCode;
                value = (900 * value) + ((long) nextCode);
                codeIndex = codeIndex2 + 1;
                nextCode = codewords[codeIndex2];
                if (nextCode == 900 || nextCode == 901 || nextCode == 902 || nextCode == 924 || nextCode == 928 || nextCode == 923 || nextCode == 922) {
                    end = true;
                    count2 = count;
                    codeIndex2 = codeIndex - 1;
                } else if (count % 5 == 0 && count > 0) {
                    for (j = 0; j < 6; j++) {
                        decodedBytes.write((byte) ((int) (value >> ((5 - j) * 8))));
                    }
                    value = 0;
                    count2 = 0;
                    codeIndex2 = codeIndex;
                } else {
                    count2 = count;
                    codeIndex2 = codeIndex;
                }
            }
            if (codeIndex2 == codewords[0] && nextCode < 900) {
                count = count2 + 1;
                byteCompactedCodewords[count2] = nextCode;
            } else {
                count = count2;
            }
            for (int i = 0; i < count; i++) {
                decodedBytes.write((byte) byteCompactedCodewords[i]);
            }
            codeIndex = codeIndex2;
        } else if (mode == 924) {
            count = 0;
            value = 0;
            end = false;
            codeIndex2 = codeIndex;
            while (codeIndex2 < codewords[0]) {
                if (end) {
                    codeIndex = codeIndex2;
                    break;
                }
                codeIndex = codeIndex2 + 1;
                int code = codewords[codeIndex2];
                if (code < 900) {
                    count++;
                    value = (900 * value) + ((long) code);
                } else if (code == 900 || code == 901 || code == 902 || code == 924 || code == 928 || code == 923 || code == 922) {
                    codeIndex--;
                    end = true;
                }
                if (count % 5 == 0 && count > 0) {
                    for (j = 0; j < 6; j++) {
                        decodedBytes.write((byte) ((int) (value >> ((5 - j) * 8))));
                    }
                    value = 0;
                    count = 0;
                    codeIndex2 = codeIndex;
                } else {
                    codeIndex2 = codeIndex;
                }
            }
            codeIndex = codeIndex2;
        }
        result.append(new String(decodedBytes.toByteArray(), encoding));
        return codeIndex;
    }

    private static int numericCompaction(int[] codewords, int codeIndex, StringBuilder result) throws FormatException {
        int count = 0;
        boolean end = false;
        int[] numericCodewords = new int[15];
        int codeIndex2 = codeIndex;
        while (codeIndex2 < codewords[0] && !end) {
            codeIndex = codeIndex2 + 1;
            int code = codewords[codeIndex2];
            if (codeIndex == codewords[0]) {
                end = true;
            }
            if (code < 900) {
                numericCodewords[count] = code;
                count++;
            } else if (code == 900 || code == 901 || code == 924 || code == 928 || code == 923 || code == 922) {
                codeIndex--;
                end = true;
            }
            if (count % 15 != 0 && code != 902 && !end) {
                codeIndex2 = codeIndex;
            } else if (count <= 0) {
                codeIndex2 = codeIndex;
            } else {
                result.append(decodeBase900toBase10(numericCodewords, count));
                count = 0;
                codeIndex2 = codeIndex;
            }
        }
        return codeIndex2;
    }

    private static String decodeBase900toBase10(int[] codewords, int count) throws FormatException {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < count; i++) {
            result = result.add(EXP900[(count - i) - 1].multiply(BigInteger.valueOf((long) codewords[i])));
        }
        String resultString = result.toString();
        if (resultString.charAt(0) == '1') {
            return resultString.substring(1);
        }
        throw FormatException.getFormatInstance();
    }
}
