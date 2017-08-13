package com.android.camera.groupshot;

import com.android.camera.Log;

public class GroupShot {
    private static final String TAG = GroupShot.class.getSimpleName();
    private int mHeight;
    private int mMaxImageNum;
    private long mNative = 0;
    private boolean mStart;
    private int mWidth;

    private final native int attach(long j, byte[] bArr);

    private final native int clearImages(long j);

    private final native long createNativeObject();

    private final native void deleteNativeObject(long j);

    private final native int end(long j);

    private final native int getImageAndSaveJpeg(long j, String str);

    private final native int initializeNativeObject(long j, int i, int i2, int i3, int i4, int i5, int i6, int i7);

    private final native int saveInputImages(long j, String str);

    private final native int setBaseImage(long j, int i);

    private final native int setBestFace(long j);

    private final native int start(long j, int i);

    static {
        try {
            System.loadLibrary("morpho_groupshot");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "can't loadLibrary, " + e.getMessage());
        }
    }

    public int initialize(int maxImageNum, int maxFaceNum, int width, int height, int max_width, int max_height) {
        if (!this.mStart && this.mWidth == width && this.mHeight == height && this.mMaxImageNum == maxImageNum) {
            return 0;
        }
        if (this.mStart) {
            if (this.mWidth == 0 && this.mHeight == 0) {
                if (this.mMaxImageNum != 0) {
                }
            }
            clearImages();
            finish();
        }
        if (this.mNative == 0) {
            this.mNative = createNativeObject();
            if (this.mNative == 0) {
                return -1;
            }
        }
        Log.v(TAG, String.format("initialize imagenum=%d, width=%d, height=%d, mStart=%b, mWidth=%d, mHeight=%d, mMaxImageNum=%d", new Object[]{Integer.valueOf(maxImageNum), Integer.valueOf(width), Integer.valueOf(height), Boolean.valueOf(this.mStart), Integer.valueOf(this.mWidth), Integer.valueOf(this.mHeight), Integer.valueOf(this.mMaxImageNum)}));
        initializeNativeObject(this.mNative, maxImageNum, maxFaceNum, width, height, max_width, max_height, 0);
        this.mMaxImageNum = maxImageNum;
        this.mWidth = width;
        this.mHeight = height;
        this.mStart = false;
        return 0;
    }

    public boolean isUsed() {
        return this.mStart;
    }

    public void finish() {
        if (this.mNative != 0) {
            Log.v(TAG, String.format("finish mNative=%x", new Object[]{Long.valueOf(this.mNative)}));
            deleteNativeObject(this.mNative);
            this.mWidth = 0;
            this.mHeight = 0;
            this.mMaxImageNum = 0;
            this.mStart = false;
            this.mNative = 0;
        }
    }

    public int clearImages() {
        Log.v(TAG, String.format("clearImages mNative=%x", new Object[]{Long.valueOf(this.mNative)}));
        if (this.mNative == 0) {
            return -1;
        }
        return clearImages(this.mNative);
    }

    public int attach_start(int withDelay) {
        Log.v(TAG, String.format("GroupShot attach start mNative=%x", new Object[]{Long.valueOf(this.mNative)}));
        if (this.mNative == 0) {
            return -1;
        }
        this.mStart = true;
        return start(this.mNative, withDelay);
    }

    public int attach(byte[] src) {
        Log.v(TAG, String.format("GroupShot attach mNative=%x", new Object[]{Long.valueOf(this.mNative)}));
        if (this.mNative == 0) {
            return -1;
        }
        return attach(this.mNative, src);
    }

    public int attach_end() {
        Log.v(TAG, String.format("GroupShot attach end, mNative=%x", new Object[]{Long.valueOf(this.mNative)}));
        if (this.mNative == 0) {
            return -1;
        }
        return end(this.mNative);
    }

    public int setBaseImage(int index) {
        if (this.mNative == 0) {
            return -1;
        }
        return setBaseImage(this.mNative, index);
    }

    public int getImageAndSaveJpeg(String filename) {
        if (this.mNative == 0) {
            return -1;
        }
        Log.v(TAG, String.format("GroupShot getImageAndSaveJpeg, mNative=%x filename=%s", new Object[]{Long.valueOf(this.mNative), filename}));
        return getImageAndSaveJpeg(this.mNative, filename);
    }

    public int saveInputImages(String filepath) {
        if (this.mNative == 0) {
            return -1;
        }
        return saveInputImages(this.mNative, filepath);
    }

    public int setBestFace() {
        if (this.mNative == 0) {
            return -1;
        }
        return setBestFace(this.mNative);
    }
}
