package com.android.camera.panorama;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import com.android.camera.Log;
import java.nio.ByteBuffer;

public class MorphoPanoramaGP {
    private static final String TAG = MorphoPanoramaGP.class.getSimpleName();
    private long mNative = 0;

    public static class InitParam {
        public double angle_of_view_degree;
        public int direction;
        public int draw_cur_image;
        public int dst_img_height;
        public int dst_img_width;
        public String format;
        public int output_rotation;
        public int preview_height;
        public int preview_img_height;
        public int preview_img_width;
        public int preview_shrink_ratio;
        public int preview_width;
        public int still_height;
        public int still_width;
        public int use_threshold;
    }

    private final native long createNativeObject();

    private final native void deleteNativeObject(long j);

    private final native int nativeAttachPreview(long j, byte[] bArr, int i, int[] iArr, byte[] bArr2, int[] iArr2, Bitmap bitmap);

    private final native int nativeAttachStillImageExt(long j, ByteBuffer byteBuffer, int i, ByteBuffer byteBuffer2);

    private final native int nativeAttachStillImageRaw(long j, ByteBuffer byteBuffer, int i, ByteBuffer byteBuffer2);

    private static final native int nativeCalcImageSize(InitParam initParam, double d);

    private final native int nativeEnd(long j);

    private final native int nativeFinish(long j);

    private final native int nativeGetBoundingRect(long j, int[] iArr);

    private final native int nativeGetClippingRect(long j, int[] iArr);

    private final native int nativeGetCurrentDirection(long j, int[] iArr);

    private final native int nativeGetGuidancePos(long j, int[] iArr);

    private final native int nativeGetMoveSpeed(long j, int[] iArr);

    private final native int nativeInitialize(long j, InitParam initParam, int[] iArr);

    private final native int nativeSaveOutputJpeg(long j, String str, int i, int i2, int i3, int i4, int i5, int[] iArr);

    private final native int nativeSetJpegForCopyingExif(long j, ByteBuffer byteBuffer);

    private final native int nativeSetMotionlessThreshold(long j, int i);

    private final native int nativeSetUseSensorAssist(long j, int i, int i2);

    private final native int nativeSetUseSensorThreshold(long j, int i);

    private final native int nativeStart(long j);

    static {
        try {
            System.loadLibrary("morpho_panorama");
            Log.e(TAG, "loadLibrary done");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "can't loadLibrary " + e.getMessage());
        }
    }

    public static int calcImageSize(InitParam param, double goal_angle) {
        return nativeCalcImageSize(param, goal_angle);
    }

    public MorphoPanoramaGP() {
        long ret = createNativeObject();
        if (ret != 0) {
            this.mNative = ret;
        } else {
            this.mNative = 0;
        }
    }

    public int initialize(InitParam param, int[] buffer_size) {
        if (this.mNative != 0) {
            return nativeInitialize(this.mNative, param, buffer_size);
        }
        return -2147483646;
    }

    public int finish() {
        if (this.mNative == 0) {
            return -2147483646;
        }
        int ret = nativeFinish(this.mNative);
        deleteNativeObject(this.mNative);
        this.mNative = 0;
        return ret;
    }

    public int start() {
        if (this.mNative != 0) {
            return nativeStart(this.mNative);
        }
        return -2147483646;
    }

    public int attachPreview(byte[] input_image, int use_image, int[] image_id, byte[] motion_data, int[] status, Bitmap preview_image) {
        if (this.mNative != 0) {
            return nativeAttachPreview(this.mNative, input_image, use_image, image_id, motion_data, status, preview_image);
        }
        return -2147483646;
    }

    public int attachStillImageExt(ByteBuffer input_image, int image_id, ByteBuffer motion_data) {
        if (this.mNative != 0) {
            return nativeAttachStillImageExt(this.mNative, input_image, image_id, motion_data);
        }
        return -2147483646;
    }

    public int attachStillImageRaw(ByteBuffer input_image, int image_id, ByteBuffer motion_data) {
        if (this.mNative != 0) {
            return nativeAttachStillImageRaw(this.mNative, input_image, image_id, motion_data);
        }
        return -2147483646;
    }

    public int attachSetJpegForCopyingExif(ByteBuffer input_image) {
        if (this.mNative != 0) {
            return nativeSetJpegForCopyingExif(this.mNative, input_image);
        }
        return -2147483646;
    }

    public int end() {
        if (this.mNative != 0) {
            return nativeEnd(this.mNative);
        }
        return -2147483646;
    }

    public int getBoundingRect(Rect rect) {
        int ret;
        int[] rect_info = new int[4];
        if (this.mNative != 0) {
            ret = nativeGetBoundingRect(this.mNative, rect_info);
            if (ret == 0) {
                rect.set(rect_info[0], rect_info[1], rect_info[2], rect_info[3]);
            }
        } else {
            ret = -2147483646;
        }
        if (ret != 0) {
            rect.set(0, 0, 0, 0);
        }
        return ret;
    }

    public int getClippingRect(Rect rect) {
        int ret;
        int[] rect_info = new int[4];
        if (this.mNative != 0) {
            ret = nativeGetClippingRect(this.mNative, rect_info);
            if (ret == 0) {
                rect.set(rect_info[0], rect_info[1], rect_info[2], rect_info[3]);
            }
        } else {
            ret = -2147483646;
        }
        if (ret != 0) {
            rect.set(0, 0, 0, 0);
        }
        return ret;
    }

    public int setMotionlessThreshold(int motionless_threshold) {
        if (this.mNative != 0) {
            return nativeSetMotionlessThreshold(this.mNative, motionless_threshold);
        }
        return -2147483646;
    }

    public int getMoveSpeed(int[] movespeed) {
        if (this.mNative != 0) {
            return nativeGetMoveSpeed(this.mNative, movespeed);
        }
        return -2147483646;
    }

    public int getCurrentDirection(int[] direction) {
        if (this.mNative != 0) {
            return nativeGetCurrentDirection(this.mNative, direction);
        }
        return -2147483646;
    }

    public int setUseSensorAssist(int use_case, int enable) {
        if (this.mNative != 0) {
            return nativeSetUseSensorAssist(this.mNative, use_case, enable);
        }
        return -2147483646;
    }

    public int setUseSensorThreshold(int threshold) {
        if (this.mNative != 0) {
            return nativeSetUseSensorThreshold(this.mNative, threshold);
        }
        return -2147483646;
    }

    public int getGuidancePos(Point attached, Point guide) {
        int[] pos = new int[4];
        if (this.mNative == 0) {
            return -2147483646;
        }
        int ret = nativeGetGuidancePos(this.mNative, pos);
        attached.set(pos[0], pos[1]);
        guide.set(pos[2], pos[3]);
        return ret;
    }

    public int saveOutputJpeg(String path, Rect rect, int orientation, int[] progress) {
        if (this.mNative == 0) {
            return -2147483646;
        }
        return nativeSaveOutputJpeg(this.mNative, path, rect.left, rect.top, rect.right, rect.bottom, orientation, progress);
    }
}
