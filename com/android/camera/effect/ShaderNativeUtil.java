package com.android.camera.effect;

import android.util.Log;

public class ShaderNativeUtil {
    private static native byte[] getJpegPicture(int i, int i2, int i3, int i4, int i5);

    private static native int[] initJpegTexture(byte[] bArr, int i, int i2);

    static {
        try {
            System.loadLibrary("CameraEffectJNI");
        } catch (Throwable e) {
            Log.e("Camera", "ShaderNativeUtil load CameraEffectJNI.so failed.", e);
        }
    }

    public static int[] initTexture(byte[] data, int texId, int downScale) {
        return initJpegTexture(data, texId, downScale);
    }

    public static byte[] getPicture(int w, int h, int quality) {
        return getJpegPicture(0, 0, w, h, quality);
    }
}
