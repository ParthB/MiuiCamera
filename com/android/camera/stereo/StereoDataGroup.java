package com.android.camera.stereo;

public class StereoDataGroup {
    private byte[] mClearImage;
    private byte[] mDepthMap;
    private byte[] mJpsData;
    private byte[] mLdcData;
    private byte[] mMaskAndConfigData;
    private byte[] mOriginalJpegData;
    private String mPictureName;

    public StereoDataGroup(String pictureName, byte[] originalJpegData, byte[] jpsData, byte[] maskData, byte[] depthData, byte[] clearImage, byte[] ldcData) {
        this.mPictureName = pictureName;
        this.mOriginalJpegData = originalJpegData;
        this.mJpsData = jpsData;
        this.mMaskAndConfigData = maskData;
        this.mDepthMap = depthData;
        this.mClearImage = clearImage;
        this.mLdcData = ldcData;
    }

    public String getPictureName() {
        return this.mPictureName;
    }

    public byte[] getJpsData() {
        return this.mJpsData;
    }

    public byte[] getMaskAndConfigData() {
        return this.mMaskAndConfigData;
    }

    public byte[] getDepthMap() {
        return this.mDepthMap;
    }

    public byte[] getClearImage() {
        return this.mClearImage;
    }

    public byte[] getLdcData() {
        return this.mLdcData;
    }

    public byte[] getOriginalJpegData() {
        return this.mOriginalJpegData;
    }
}
