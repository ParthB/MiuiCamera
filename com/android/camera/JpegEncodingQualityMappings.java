package com.android.camera;

import android.support.v7.recyclerview.R;
import java.util.HashMap;

public class JpegEncodingQualityMappings {
    private static HashMap<String, Integer> mHashMap = new HashMap();

    static {
        mHashMap.put("low", Integer.valueOf(67));
        mHashMap.put("normal", Integer.valueOf(87));
        mHashMap.put("high", Integer.valueOf(CameraAppImpl.getAndroidContext().getResources().getInteger(R.integer.high_jpeg_quality)));
    }

    public static int getQualityNumber(String jpegQuality) {
        Integer quality = (Integer) mHashMap.get(jpegQuality);
        if (quality == null) {
            return 87;
        }
        return quality.intValue();
    }
}
