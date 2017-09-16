package com.android.camera.steganography;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

public class SteganographyUtils {
    private static String TAG = "Whet_SteganographyUtils";

    public static Bitmap encodeWatermark(Bitmap bmp, String watermark) {
        Bitmap ret = null;
        if (bmp == null || TextUtils.isEmpty(watermark)) {
            return ret;
        }
        try {
            ret = Steg.withInput(bmp).encode(watermark).intoBitmap();
        } catch (Exception e) {
            Log.w(TAG, "encodeWatermark Exception e:" + e.getMessage());
        }
        return ret;
    }
}
