package com.google.zxing.maxicode.decoder;

import com.google.zxing.common.DecoderResult;
import java.text.DecimalFormat;
import java.text.NumberFormat;

final class DecodedBitStreamParser {
    private static final NumberFormat NINE_DIGITS = new DecimalFormat("000000000");
    private static final String[] SETS = new String[]{"\nABCDEFGHIJKLMNOPQRSTUVWXYZ￺\u001c\u001d\u001e￻ ￼\"#$%&'()*+,-./0123456789:￱￲￳￴￸", "`abcdefghijklmnopqrstuvwxyz￺\u001c\u001d\u001e￻{￼}~;<=>?[\\]^_ ,./:@!|￼￵￶￼￰￲￳￴￷", "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚ￺\u001c\u001d\u001eÛÜÝÞßª¬±²³µ¹º¼½¾￷ ￹￳￴￸", "àáâãäåæçèéêëìíîïðñòóôõö÷øùú￺\u001c\u001d\u001e￻ûüýþÿ¡¨«¯°´·¸»¿￷ ￲￹￴￸", "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000b\f\r\u000e\u000f\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001a￺￼￼\u001b￻\u001c\u001d\u001e\u001f ¢£¤¥¦§©­®¶￷ ￲￳￹￸", "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000b\f\r\u000e\u000f\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f !\"#$%&'()*+,-./0123456789:;<=>?"};
    private static final NumberFormat THREE_DIGITS = new DecimalFormat("000");

    private DecodedBitStreamParser() {
    }

    static DecoderResult decode(byte[] bytes, int mode) {
        StringBuilder result = new StringBuilder(144);
        switch (mode) {
            case 2:
            case 3:
                String postcode;
                if (mode != 2) {
                    postcode = getPostCode3(bytes);
                } else {
                    postcode = new DecimalFormat("0000000000".substring(0, getPostCode2Length(bytes))).format((long) getPostCode2(bytes));
                }
                String country = THREE_DIGITS.format((long) getCountry(bytes));
                String service = THREE_DIGITS.format((long) getServiceClass(bytes));
                result.append(getMessage(bytes, 10, 84));
                if (!result.toString().startsWith("[)>\u001e01\u001d")) {
                    result.insert(0, new StringBuilder(String.valueOf(postcode)).append('\u001d').append(country).append('\u001d').append(service).append('\u001d').toString());
                    break;
                }
                result.insert(9, new StringBuilder(String.valueOf(postcode)).append('\u001d').append(country).append('\u001d').append(service).append('\u001d').toString());
                break;
            case 4:
                result.append(getMessage(bytes, 1, 93));
                break;
            case 5:
                result.append(getMessage(bytes, 1, 77));
                break;
        }
        return new DecoderResult(bytes, result.toString(), null, String.valueOf(mode));
    }

    private static int getBit(int bit, byte[] bytes) {
        bit--;
        if ((bytes[bit / 6] & (1 << (5 - (bit % 6)))) != 0) {
            return 1;
        }
        return 0;
    }

    private static int getInt(byte[] bytes, byte[] x) {
        if (x.length != 0) {
            int val = 0;
            for (int i = 0; i < x.length; i++) {
                val += getBit(x[i], bytes) << ((x.length - i) - 1);
            }
            return val;
        }
        throw new IllegalArgumentException();
    }

    private static int getCountry(byte[] bytes) {
        return getInt(bytes, new byte[]{(byte) 53, (byte) 54, (byte) 43, (byte) 44, (byte) 45, (byte) 46, (byte) 47, (byte) 48, (byte) 37, (byte) 38});
    }

    private static int getServiceClass(byte[] bytes) {
        return getInt(bytes, new byte[]{(byte) 55, (byte) 56, (byte) 57, (byte) 58, (byte) 59, (byte) 60, (byte) 49, (byte) 50, (byte) 51, (byte) 52});
    }

    private static int getPostCode2Length(byte[] bytes) {
        return getInt(bytes, new byte[]{(byte) 39, (byte) 40, (byte) 41, (byte) 42, (byte) 31, (byte) 32});
    }

    private static int getPostCode2(byte[] bytes) {
        return getInt(bytes, new byte[]{(byte) 33, (byte) 34, (byte) 35, (byte) 36, (byte) 25, (byte) 26, (byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 19, (byte) 20, (byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 13, (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 1, (byte) 2});
    }

    private static String getPostCode3(byte[] bytes) {
        r0 = new char[6];
        r0[0] = (char) SETS[0].charAt(getInt(bytes, new byte[]{(byte) 39, (byte) 40, (byte) 41, (byte) 42, (byte) 31, (byte) 32}));
        r0[1] = (char) SETS[0].charAt(getInt(bytes, new byte[]{(byte) 33, (byte) 34, (byte) 35, (byte) 36, (byte) 25, (byte) 26}));
        r0[2] = (char) SETS[0].charAt(getInt(bytes, new byte[]{(byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 19, (byte) 20}));
        r0[3] = (char) SETS[0].charAt(getInt(bytes, new byte[]{(byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 13, (byte) 14}));
        r0[4] = (char) SETS[0].charAt(getInt(bytes, new byte[]{(byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 7, (byte) 8}));
        r0[5] = (char) SETS[0].charAt(getInt(bytes, new byte[]{(byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 1, (byte) 2}));
        return String.valueOf(r0);
    }

    private static String getMessage(byte[] bytes, int start, int len) {
        StringBuilder sb = new StringBuilder();
        int shift = -1;
        int set = 0;
        int lastset = 0;
        int i = start;
        while (i < start + len) {
            int shift2;
            char c = SETS[set].charAt(bytes[i]);
            switch (c) {
                case '￰':
                case '￱':
                case '￲':
                case '￳':
                case '￴':
                    lastset = set;
                    set = c - 65520;
                    shift2 = 1;
                    break;
                case '￵':
                    lastset = set;
                    set = 0;
                    shift2 = 2;
                    break;
                case '￶':
                    lastset = set;
                    set = 0;
                    shift2 = 3;
                    break;
                case '￷':
                    set = 0;
                    shift2 = -1;
                    break;
                case '￸':
                    set = 1;
                    shift2 = -1;
                    break;
                case '￹':
                    shift2 = -1;
                    break;
                case '￻':
                    i++;
                    i++;
                    i++;
                    i++;
                    i++;
                    sb.append(NINE_DIGITS.format((long) (((((bytes[i] << 24) + (bytes[i] << 18)) + (bytes[i] << 12)) + (bytes[i] << 6)) + bytes[i])));
                    shift2 = shift;
                    break;
                default:
                    sb.append(c);
                    shift2 = shift;
                    break;
            }
            shift = shift2 - 1;
            if (shift2 == 0) {
                set = lastset;
            }
            i++;
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '￼') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
