package com.google.android.apps.photos.api.signature;

final class HexConvert {
    private static final char[] HEX_DIGITS_ARRAY = "0123456789ABCDEF".toCharArray();

    private HexConvert() {
    }

    static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        char[] hexChars = new char[(bytes.length * 2)];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = HEX_DIGITS_ARRAY[v >>> 4];
            hexChars[(j * 2) + 1] = HEX_DIGITS_ARRAY[v & 15];
        }
        return new String(hexChars);
    }
}
