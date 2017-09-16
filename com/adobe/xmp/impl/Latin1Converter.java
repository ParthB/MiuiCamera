package com.adobe.xmp.impl;

import java.io.UnsupportedEncodingException;

public class Latin1Converter {
    private Latin1Converter() {
    }

    public static ByteBuffer convert(ByteBuffer buffer) {
        if (!"UTF-8".equals(buffer.getEncoding())) {
            return buffer;
        }
        byte[] readAheadBuffer = new byte[8];
        int readAhead = 0;
        int expectedBytes = 0;
        ByteBuffer out = new ByteBuffer((buffer.length() * 4) / 3);
        int state = 0;
        int i = 0;
        while (i < buffer.length()) {
            int b = buffer.charAt(i);
            int readAhead2;
            switch (state) {
                case 11:
                    if (expectedBytes > 0 && (b & 192) == 128) {
                        readAhead2 = readAhead + 1;
                        readAheadBuffer[readAhead] = (byte) b;
                        expectedBytes--;
                        if (expectedBytes != 0) {
                            readAhead = readAhead2;
                            break;
                        }
                        out.append(readAheadBuffer, 0, readAhead2);
                        readAhead = 0;
                        state = 0;
                        break;
                    }
                    out.append(convertToUTF8(readAheadBuffer[0]));
                    i -= readAhead;
                    readAhead = 0;
                    state = 0;
                    break;
                    break;
                default:
                    if (b >= 127) {
                        if (b < 192) {
                            out.append(convertToUTF8((byte) b));
                            break;
                        }
                        expectedBytes = -1;
                        int test = b;
                        while (expectedBytes < 8 && (test & 128) == 128) {
                            expectedBytes++;
                            test <<= 1;
                        }
                        readAhead2 = readAhead + 1;
                        readAheadBuffer[readAhead] = (byte) b;
                        state = 11;
                        readAhead = readAhead2;
                        break;
                    }
                    out.append((byte) b);
                    break;
                    break;
            }
            i++;
        }
        if (state == 11) {
            for (int j = 0; j < readAhead; j++) {
                out.append(convertToUTF8(readAheadBuffer[j]));
            }
        }
        return out;
    }

    private static byte[] convertToUTF8(byte ch) {
        int c = ch & 255;
        if (c >= 128) {
            if (c == 129 || c == 141 || c == 143 || c == 144 || c == 157) {
                try {
                    return new byte[]{(byte) 32};
                } catch (UnsupportedEncodingException e) {
                }
            } else {
                return new String(new byte[]{ch}, "cp1252").getBytes("UTF-8");
            }
        }
        return new byte[]{ch};
    }
}
