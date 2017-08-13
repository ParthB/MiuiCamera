package com.android.camera.effect.renders;

import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.support.v7.recyclerview.R;
import com.android.camera.CameraAppImpl;
import com.android.camera.effect.EffectController;
import com.android.camera.effect.EffectController.EffectRectAttribute;
import com.android.gallery3d.ui.GLCanvas;

public abstract class RegionEffectRender extends ConvolutionEffectRender {
    private EffectRectAttribute mAttribute;
    private int mThresholdHeight;
    private int mThresholdWidth;
    protected int mUniformEffectParameterH;
    protected int mUniformEffectRectH;
    protected int mUniformInvertRectH;

    private void init() {
        Resources resources = CameraAppImpl.getAndroidContext().getResources();
        this.mThresholdWidth = resources.getDimensionPixelSize(R.dimen.effect_item_width);
        this.mThresholdHeight = resources.getDimensionPixelSize(R.dimen.effect_item_height);
    }

    public RegionEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
        init();
    }

    protected void initShader() {
        super.initShader();
        this.mUniformEffectRectH = GLES20.glGetUniformLocation(this.mProgram, "uEffectRect");
        this.mUniformInvertRectH = GLES20.glGetUniformLocation(this.mProgram, "uInvertRect");
        this.mUniformEffectParameterH = GLES20.glGetUniformLocation(this.mProgram, "uEffectArray");
    }

    protected void initShaderValue(boolean isSnapShot) {
        super.initShaderValue(isSnapShot);
        initEffectRect(isSnapShot);
    }

    public void setEffectRangeAttribute(EffectRectAttribute attribute) {
        this.mAttribute = attribute;
        setEffectRectF(attribute.mRectF);
    }

    private void setEffectRectF(RectF rect) {
        if (rect != null) {
            this.mPreviewEffectRect[0] = rect.left;
            this.mPreviewEffectRect[1] = rect.top;
            this.mPreviewEffectRect[2] = rect.right;
            this.mPreviewEffectRect[3] = rect.bottom;
            return;
        }
        this.mPreviewEffectRect[0] = 0.0f;
        this.mPreviewEffectRect[1] = 0.0f;
        this.mPreviewEffectRect[2] = 1.0f;
        this.mPreviewEffectRect[3] = 1.0f;
    }

    protected int getInvertFlag(boolean isSnapShot) {
        if (isSnapShot) {
            return this.mAttribute.mInvertFlag;
        }
        return EffectController.getInstance().getInvertFlag();
    }

    protected void initEffectRect(boolean isSnapShot) {
        GLES20.glUniform4fv(this.mUniformEffectRectH, 1, getEffectRect(isSnapShot), 0);
        GLES20.glUniform1i(this.mUniformInvertRectH, getInvertFlag(isSnapShot));
        GLES20.glUniform1fv(this.mUniformEffectParameterH, 5, getEffectArray(isSnapShot), 0);
    }

    private float[] getEffectArray(boolean isSnapShot) {
        if (isSnapShot) {
            float[] points = new float[]{this.mAttribute.mPoint1.x, this.mAttribute.mPoint1.y, this.mAttribute.mPoint2.x, this.mAttribute.mPoint2.y, this.mAttribute.mRangeWidth};
            getChangeMatrix().mapPoints(points, 0, points, 0, 2);
            return points;
        } else if (EffectController.getInstance().getEffect(true) != this.mId) {
            return new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        } else {
            EffectRectAttribute attribute = EffectController.getInstance().getEffectAttribute();
            return new float[]{attribute.mPoint1.x, attribute.mPoint1.y, attribute.mPoint2.x, attribute.mPoint2.y, attribute.mRangeWidth};
        }
    }

    protected float[] getEffectRect(boolean isSnapShot) {
        RectF rectF;
        if (isSnapShot) {
            if (!this.mMirror) {
                rectF = new RectF(this.mPreviewEffectRect[0], this.mPreviewEffectRect[1], this.mPreviewEffectRect[2], this.mPreviewEffectRect[3]);
            } else if (this.mOrientation % 180 == 0) {
                rectF = new RectF(1.0f - this.mPreviewEffectRect[0], this.mPreviewEffectRect[1], 1.0f - this.mPreviewEffectRect[2], this.mPreviewEffectRect[3]);
            } else {
                rectF = new RectF(this.mPreviewEffectRect[0], 1.0f - this.mPreviewEffectRect[1], this.mPreviewEffectRect[2], 1.0f - this.mPreviewEffectRect[3]);
            }
            getChangeMatrix().mapRect(rectF);
            this.mSnapshotEffectRect[0] = rectF.left;
            this.mSnapshotEffectRect[1] = rectF.top;
            this.mSnapshotEffectRect[2] = rectF.right;
            this.mSnapshotEffectRect[3] = rectF.bottom;
            return this.mSnapshotEffectRect;
        }
        rectF = EffectController.getInstance().getEffectRectF();
        if (EffectController.getInstance().getEffect(true) != this.mId || this.mPreviewWidth <= this.mThresholdWidth || this.mPreviewHeight <= this.mThresholdHeight) {
            setEffectRectF(null);
        } else {
            setEffectRectF(rectF);
        }
        return this.mPreviewEffectRect;
    }

    private Matrix getChangeMatrix() {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate((float) (this.mOrientation - this.mJpegOrientation));
        matrix.preTranslate(-0.5f, -0.5f);
        matrix.postTranslate(0.5f, 0.5f);
        return matrix;
    }
}
