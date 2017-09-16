package com.google.zxing.qrcode.decoder;

import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.common.BitSource;
import com.google.zxing.common.CharacterSetECI;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.StringUtils;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

final class DecodedBitStreamParser {
    private static final char[] ALPHANUMERIC_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ' ', '$', '%', '*', '+', '-', '.', '/', ':'};

    private DecodedBitStreamParser() {
    }

    static DecoderResult decode(byte[] bytes, Version version, ErrorCorrectionLevel ecLevel, Map<DecodeHintType, ?> hints) throws FormatException {
        List list;
        String errorCorrectionLevel;
        BitSource bits = new BitSource(bytes);
        StringBuilder result = new StringBuilder(50);
        List<byte[]> byteSegments = new ArrayList(1);
        int symbolSequence = -1;
        int parityData = -1;
        CharacterSetECI currentCharacterSetECI = null;
        boolean fc1InEffect = false;
        Mode mode;
        do {
            try {
                if (bits.available() >= 4) {
                    mode = Mode.forBits(bits.readBits(4));
                } else {
                    mode = Mode.TERMINATOR;
                }
                if (mode != Mode.TERMINATOR) {
                    if (mode == Mode.FNC1_FIRST_POSITION || mode == Mode.FNC1_SECOND_POSITION) {
                        fc1InEffect = true;
                    } else if (mode != Mode.STRUCTURED_APPEND) {
                        if (mode == Mode.ECI) {
                            currentCharacterSetECI = CharacterSetECI.getCharacterSetECIByValue(parseECIValue(bits));
                            if (currentCharacterSetECI == null) {
                                throw FormatException.getFormatInstance();
                            }
                        } else if (mode != Mode.HANZI) {
                            int count = bits.readBits(mode.getCharacterCountBits(version));
                            if (mode == Mode.NUMERIC) {
                                decodeNumericSegment(bits, result, count);
                            } else if (mode == Mode.ALPHANUMERIC) {
                                decodeAlphanumericSegment(bits, result, count, fc1InEffect);
                            } else if (mode == Mode.BYTE) {
                                decodeByteSegment(bits, result, count, currentCharacterSetECI, byteSegments, hints);
                            } else if (mode != Mode.KANJI) {
                                throw FormatException.getFormatInstance();
                            } else {
                                decodeKanjiSegment(bits, result, count);
                            }
                        } else {
                            int subset = bits.readBits(4);
                            int countHanzi = bits.readBits(mode.getCharacterCountBits(version));
                            if (subset == 1) {
                                decodeHanziSegment(bits, result, countHanzi);
                            }
                        }
                    } else if (bits.available() >= 16) {
                        symbolSequence = bits.readBits(8);
                        parityData = bits.readBits(8);
                    } else {
                        throw FormatException.getFormatInstance();
                    }
                }
            } catch (IllegalArgumentException e) {
                throw FormatException.getFormatInstance();
            }
        } while (mode != Mode.TERMINATOR);
        String stringBuilder = result.toString();
        if (byteSegments.isEmpty()) {
            list = null;
        } else {
            list = byteSegments;
        }
        if (ecLevel != null) {
            errorCorrectionLevel = ecLevel.toString();
        } else {
            errorCorrectionLevel = null;
        }
        return new DecoderResult(bytes, stringBuilder, list, errorCorrectionLevel, symbolSequence, parityData);
    }

    private static void decodeHanziSegment(BitSource bits, StringBuilder result, int count) throws FormatException {
        if (count * 13 <= bits.available()) {
            byte[] buffer = new byte[(count * 2)];
            int offset = 0;
            while (count > 0) {
                int twoBytes = bits.readBits(13);
                int assembledTwoBytes = ((twoBytes / 96) << 8) | (twoBytes % 96);
                if (assembledTwoBytes >= 959) {
                    assembledTwoBytes += 42657;
                } else {
                    assembledTwoBytes += 41377;
                }
                buffer[offset] = (byte) ((byte) ((assembledTwoBytes >> 8) & 255));
                buffer[offset + 1] = (byte) ((byte) (assembledTwoBytes & 255));
                offset += 2;
                count--;
            }
            try {
                result.append(new String(buffer, "GB2312"));
                return;
            } catch (UnsupportedEncodingException e) {
                throw FormatException.getFormatInstance();
            }
        }
        throw FormatException.getFormatInstance();
    }

    private static void decodeKanjiSegment(BitSource bits, StringBuilder result, int count) throws FormatException {
        if (count * 13 <= bits.available()) {
            byte[] buffer = new byte[(count * 2)];
            int offset = 0;
            while (count > 0) {
                int twoBytes = bits.readBits(13);
                int assembledTwoBytes = ((twoBytes / 192) << 8) | (twoBytes % 192);
                if (assembledTwoBytes >= 7936) {
                    assembledTwoBytes += 49472;
                } else {
                    assembledTwoBytes += 33088;
                }
                buffer[offset] = (byte) ((byte) (assembledTwoBytes >> 8));
                buffer[offset + 1] = (byte) ((byte) assembledTwoBytes);
                offset += 2;
                count--;
            }
            try {
                result.append(new String(buffer, "SJIS"));
                return;
            } catch (UnsupportedEncodingException e) {
                throw FormatException.getFormatInstance();
            }
        }
        throw FormatException.getFormatInstance();
    }

    private static void decodeByteSegment(BitSource bits, StringBuilder result, int count, CharacterSetECI currentCharacterSetECI, Collection<byte[]> byteSegments, Map<DecodeHintType, ?> hints) throws FormatException {
        if (count * 8 <= bits.available()) {
            String encoding;
            byte[] readBytes = new byte[count];
            for (int i = 0; i < count; i++) {
                readBytes[i] = (byte) ((byte) bits.readBits(8));
            }
            if (currentCharacterSetECI != null) {
                encoding = currentCharacterSetECI.name();
            } else {
                encoding = StringUtils.guessEncoding(readBytes, hints);
            }
            try {
                result.append(new String(readBytes, encoding));
                byteSegments.add(readBytes);
                return;
            } catch (UnsupportedEncodingException e) {
                throw FormatException.getFormatInstance();
            }
        }
        throw FormatException.getFormatInstance();
    }

    private static char toAlphaNumericChar(int value) throws FormatException {
        if (value < ALPHANUMERIC_CHARS.length) {
            return ALPHANUMERIC_CHARS[value];
        }
        throw FormatException.getFormatInstance();
    }

    private static void decodeAlphanumericSegment(BitSource bits, StringBuilder result, int count, boolean fc1InEffect) throws FormatException {
        int start = result.length();
        while (count > 1) {
            if (bits.available() >= 11) {
                int nextTwoCharsBits = bits.readBits(11);
                result.append(toAlphaNumericChar(nextTwoCharsBits / 45));
                result.append(toAlphaNumericChar(nextTwoCharsBits % 45));
                count -= 2;
            } else {
                throw FormatException.getFormatInstance();
            }
        }
        if (count == 1) {
            if (bits.available() >= 6) {
                result.append(toAlphaNumericChar(bits.readBits(6)));
            } else {
                throw FormatException.getFormatInstance();
            }
        }
        if (fc1InEffect) {
            int i = start;
            while (i < result.length()) {
                if (result.charAt(i) == '%') {
                    if (i < result.length() - 1 && result.charAt(i + 1) == '%') {
                        result.deleteCharAt(i + 1);
                    } else {
                        result.setCharAt(i, '\u001d');
                    }
                }
                i++;
            }
        }
    }

    private static void decodeNumericSegment(BitSource bits, StringBuilder result, int count) throws FormatException {
        while (count >= 3) {
            if (bits.available() >= 10) {
                int threeDigitsBits = bits.readBits(10);
                if (threeDigitsBits < 1000) {
                    result.append(toAlphaNumericChar(threeDigitsBits / 100));
                    result.append(toAlphaNumericChar((threeDigitsBits / 10) % 10));
                    result.append(toAlphaNumericChar(threeDigitsBits % 10));
                    count -= 3;
                } else {
                    throw FormatException.getFormatInstance();
                }
            }
            throw FormatException.getFormatInstance();
        }
        if (count != 2) {
            if (count == 1) {
                if (bits.available() >= 4) {
                    int digitBits = bits.readBits(4);
                    if (digitBits < 10) {
                        result.append(toAlphaNumericChar(digitBits));
                        return;
                    }
                    throw FormatException.getFormatInstance();
                }
                throw FormatException.getFormatInstance();
            }
        } else if (bits.available() >= 7) {
            int twoDigitsBits = bits.readBits(7);
            if (twoDigitsBits < 100) {
                result.append(toAlphaNumericChar(twoDigitsBits / 10));
                result.append(toAlphaNumericChar(twoDigitsBits % 10));
                return;
            }
            throw FormatException.getFormatInstance();
        } else {
            throw FormatException.getFormatInstance();
        }
    }

    private static int parseECIValue(BitSource bits) throws FormatException {
        int firstByte = bits.readBits(8);
        if ((firstByte & 128) == 0) {
            return firstByte & 127;
        }
        if ((firstByte & 192) == 128) {
            return ((firstByte & 63) << 8) | bits.readBits(8);
        } else if ((firstByte & 224) != 192) {
            throw FormatException.getFormatInstance();
        } else {
            return ((firstByte & 31) << 16) | bits.readBits(16);
        }
    }
}
