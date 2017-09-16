package com.android.camera.effect.draw_mode;

import android.location.Location;
import android.net.Uri;
import com.android.camera.effect.EffectController.EffectRectAttribute;
import com.android.gallery3d.exif.ExifInterface;

public class DrawJPEGAttribute extends DrawAttribute {
    public EffectRectAttribute mAttribute;
    public byte[] mData;
    public long mDate;
    public int mEffectIndex;
    public ExifInterface mExif;
    public boolean mFinalImage;
    public int mHeight;
    public int mJpegOrientation;
    public Location mLoc;
    public boolean mMirror;
    public int mOrientation;
    public boolean mPortrait;
    public int mPreviewHeight;
    public int mPreviewWidth;
    public float mShootRotation;
    public String mTitle;
    public Uri mUri;
    public int mWidth;

    public DrawJPEGAttribute(byte[] data, int previewW, int previewH, int w, int h, int effectIndex, EffectRectAttribute attribute, Location loc, String title, long date, int orientation, int jpegOrientation, float shootRotation, boolean mirror, boolean portrait) {
        this.mPreviewWidth = previewW;
        this.mPreviewHeight = previewH;
        this.mWidth = w;
        this.mHeight = h;
        this.mData = data;
        this.mDate = date;
        this.mEffectIndex = effectIndex;
        this.mAttribute = attribute;
        this.mLoc = loc;
        this.mTitle = title;
        this.mOrientation = orientation;
        this.mJpegOrientation = jpegOrientation;
        this.mShootRotation = shootRotation;
        this.mMirror = mirror;
        this.mTarget = 9;
        this.mFinalImage = true;
        this.mPortrait = portrait;
    }

    protected void finalize() throws Throwable {
        super.finalize();
    }
}
