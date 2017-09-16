package com.android.camera;

public class Exif {
    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int getOrientation(byte[] r15) {
        /*
        r14 = 8;
        r3 = 1;
        r13 = 4;
        r12 = 2;
        r9 = 0;
        if (r15 != 0) goto L_0x0009;
    L_0x0008:
        return r9;
    L_0x0009:
        r5 = 0;
        r2 = 0;
    L_0x000b:
        r10 = r5 + 3;
        r11 = r15.length;
        if (r10 >= r11) goto L_0x0025;
    L_0x0010:
        r6 = r5 + 1;
        r10 = r15[r5];
        r10 = r10 & 255;
        r11 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        if (r10 != r11) goto L_0x0024;
    L_0x001a:
        r10 = r15[r6];
        r4 = r10 & 255;
        r10 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        if (r4 != r10) goto L_0x003f;
    L_0x0022:
        r5 = r6;
        goto L_0x000b;
    L_0x0024:
        r5 = r6;
    L_0x0025:
        if (r2 <= r14) goto L_0x00e0;
    L_0x0027:
        r8 = pack(r15, r5, r13, r9);
        r10 = 1229531648; // 0x49492a00 float:823968.0 double:6.074693478E-315;
        if (r8 == r10) goto L_0x0085;
    L_0x0030:
        r10 = 1296891946; // 0x4d4d002a float:2.14958752E8 double:6.40749757E-315;
        if (r8 == r10) goto L_0x0085;
    L_0x0035:
        r10 = "CameraExif";
        r11 = "Invalid byte order";
        android.util.Log.e(r10, r11);
        return r9;
    L_0x003f:
        r5 = r6 + 1;
        r10 = 216; // 0xd8 float:3.03E-43 double:1.067E-321;
        if (r4 == r10) goto L_0x000b;
    L_0x0045:
        if (r4 == r3) goto L_0x000b;
    L_0x0047:
        r10 = 217; // 0xd9 float:3.04E-43 double:1.07E-321;
        if (r4 == r10) goto L_0x0025;
    L_0x004b:
        r10 = 218; // 0xda float:3.05E-43 double:1.077E-321;
        if (r4 == r10) goto L_0x0025;
    L_0x004f:
        r2 = pack(r15, r5, r12, r9);
        if (r2 < r12) goto L_0x005a;
    L_0x0055:
        r10 = r5 + r2;
        r11 = r15.length;
        if (r10 <= r11) goto L_0x0064;
    L_0x005a:
        r10 = "CameraExif";
        r11 = "Invalid length";
        android.util.Log.e(r10, r11);
        return r9;
    L_0x0064:
        r10 = 225; // 0xe1 float:3.15E-43 double:1.11E-321;
        if (r4 != r10) goto L_0x0082;
    L_0x0068:
        if (r2 < r14) goto L_0x0082;
    L_0x006a:
        r10 = r5 + 2;
        r10 = pack(r15, r10, r13, r9);
        r11 = 1165519206; // 0x45786966 float:3974.5874 double:5.758429993E-315;
        if (r10 != r11) goto L_0x0082;
    L_0x0075:
        r10 = r5 + 6;
        r10 = pack(r15, r10, r12, r9);
        if (r10 != 0) goto L_0x0082;
    L_0x007d:
        r5 = r5 + 8;
        r2 = r2 + -8;
        goto L_0x0025;
    L_0x0082:
        r5 = r5 + r2;
        r2 = 0;
        goto L_0x000b;
    L_0x0085:
        r10 = 1229531648; // 0x49492a00 float:823968.0 double:6.074693478E-315;
        if (r8 != r10) goto L_0x00a2;
    L_0x008a:
        r10 = r5 + 4;
        r10 = pack(r15, r10, r13, r3);
        r0 = r10 + 2;
        r10 = 10;
        if (r0 < r10) goto L_0x0098;
    L_0x0096:
        if (r0 <= r2) goto L_0x00a4;
    L_0x0098:
        r10 = "CameraExif";
        r11 = "Invalid offset";
        android.util.Log.e(r10, r11);
        return r9;
    L_0x00a2:
        r3 = r9;
        goto L_0x008a;
    L_0x00a4:
        r5 = r5 + r0;
        r2 = r2 - r0;
        r10 = r5 + -2;
        r0 = pack(r15, r10, r12, r3);
        r1 = r0;
    L_0x00ad:
        r0 = r1 + -1;
        if (r1 <= 0) goto L_0x00e0;
    L_0x00b1:
        r10 = 12;
        if (r2 < r10) goto L_0x00e0;
    L_0x00b5:
        r8 = pack(r15, r5, r12, r3);
        r10 = 274; // 0x112 float:3.84E-43 double:1.354E-321;
        if (r8 != r10) goto L_0x00da;
    L_0x00bd:
        r10 = r5 + 8;
        r7 = pack(r15, r10, r12, r3);
        switch(r7) {
            case 1: goto L_0x00d0;
            case 2: goto L_0x00c6;
            case 3: goto L_0x00d1;
            case 4: goto L_0x00c6;
            case 5: goto L_0x00c6;
            case 6: goto L_0x00d4;
            case 7: goto L_0x00c6;
            case 8: goto L_0x00d7;
            default: goto L_0x00c6;
        };
    L_0x00c6:
        r10 = "CameraExif";
        r11 = "Unsupported orientation";
        android.util.Log.i(r10, r11);
        return r9;
    L_0x00d0:
        return r9;
    L_0x00d1:
        r9 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
        return r9;
    L_0x00d4:
        r9 = 90;
        return r9;
    L_0x00d7:
        r9 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        return r9;
    L_0x00da:
        r5 = r5 + 12;
        r2 = r2 + -12;
        r1 = r0;
        goto L_0x00ad;
    L_0x00e0:
        r10 = "CameraExif";
        r11 = "Orientation not found";
        android.util.Log.i(r10, r11);
        return r9;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.Exif.getOrientation(byte[]):int");
    }

    private static int pack(byte[] bytes, int offset, int length, boolean littleEndian) {
        int step = 1;
        if (littleEndian) {
            offset += length - 1;
            step = -1;
        }
        int value = 0;
        int length2 = length;
        while (true) {
            length = length2 - 1;
            if (length2 <= 0) {
                return value;
            }
            value = (value << 8) | (bytes[offset] & 255);
            offset += step;
            length2 = length;
        }
    }
}
