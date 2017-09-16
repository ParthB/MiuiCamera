package com.android.camera.steganography;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BitmapEncoder {
    public static byte[] createHeader(long size) {
        int i;
        int i2 = 0;
        byte[] header = new byte[12];
        header[0] = (byte) 91;
        int i3 = 1 + 1;
        header[1] = (byte) 91;
        byte[] longToBytes = longToBytes(size);
        int length = longToBytes.length;
        while (i2 < length) {
            i = i3 + 1;
            header[i3] = longToBytes[i2];
            i2++;
            i3 = i;
        }
        i = i3 + 1;
        header[i3] = (byte) 93;
        i3 = i + 1;
        header[i] = (byte) 93;
        return header;
    }

    private static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }

    public static Bitmap encode(Bitmap inBitmap, byte[] bytes) {
        byte[] header = createHeader((long) bytes.length);
        if (bytes.length % 24 != 0) {
            bytes = Arrays.copyOf(bytes, bytes.length + (24 - (bytes.length % 24)));
        }
        return encodeByteArrayIntoBitmap(inBitmap, header, bytes);
    }

    private static Bitmap encodeByteArrayIntoBitmap(Bitmap inBitmap, byte[] header, byte[] bytes) {
        Bitmap outBitmap = inBitmap.copy(Config.ARGB_8888, true);
        int x = 0;
        int y = 0;
        int width = inBitmap.getWidth();
        int height = inBitmap.getHeight();
        int bufferPos = 0;
        int[] buffer = new int[]{0, 0, 0};
        for (int i = 0; i < header.length + bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                if (i < header.length) {
                    buffer[bufferPos] = (header[i] >> j) & 1;
                } else {
                    buffer[bufferPos] = (bytes[i - header.length] >> j) & 1;
                }
                if (bufferPos == 2) {
                    int color = inBitmap.getPixel(x, y);
                    int r = Color.red(color);
                    int g = Color.green(color);
                    int b = Color.blue(color);
                    if (r % 2 == 1 - buffer[0]) {
                        r++;
                    }
                    if (g % 2 == 1 - buffer[1]) {
                        g++;
                    }
                    if (b % 2 == 1 - buffer[2]) {
                        b++;
                    }
                    if (r == 256) {
                        r = 254;
                    }
                    if (g == 256) {
                        g = 254;
                    }
                    if (b == 256) {
                        b = 254;
                    }
                    outBitmap.setPixel(x, y, Color.argb(255, r, g, b));
                    x++;
                    if (x == width) {
                        x = 0;
                        y++;
                    }
                    bufferPos = 0;
                } else {
                    bufferPos++;
                }
            }
        }
        return outBitmap;
    }
}
