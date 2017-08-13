package com.android.gallery3d.ui;

import android.os.Build;

public class Utils {
    private static final boolean IS_DEBUG_BUILD;
    private static long[] sCrcTable = new long[256];

    static {
        boolean z;
        if (Build.TYPE.equals("eng")) {
            z = true;
        } else {
            z = Build.TYPE.equals("userdebug");
        }
        IS_DEBUG_BUILD = z;
        for (int i = 0; i < 256; i++) {
            long part = (long) i;
            for (int j = 0; j < 8; j++) {
                part = (part >> 1) ^ ((((int) part) & 1) != 0 ? -7661587058870466123L : 0);
            }
            sCrcTable[i] = part;
        }
    }

    public static void assertTrue(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    public static int nextPowerOf2(int n) {
        if (n <= 0 || n > 1073741824) {
            throw new IllegalArgumentException();
        }
        n--;
        n |= n >> 16;
        n |= n >> 8;
        n |= n >> 4;
        n |= n >> 2;
        return (n | (n >> 1)) + 1;
    }
}
