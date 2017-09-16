package com.android.camera.effect.renders;

import android.opengl.GLES20;
import com.android.gallery3d.ui.GLCanvas;

public class GradienterSnapshotEffectRender extends PixelEffectRender {
    private int mUniformAngle;
    private int mUniformTexSize;

    public GradienterSnapshotEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    protected void initShader() {
        super.initShader();
        this.mUniformAngle = GLES20.glGetUniformLocation(this.mProgram, "sAngle");
        this.mUniformTexSize = GLES20.glGetUniformLocation(this.mProgram, "vTexSize");
    }

    protected void initShaderValue(boolean isSnapShot) {
        super.initShaderValue(isSnapShot);
        if (this.mSnapshotOriginWidth != 0 && this.mSnapshotOriginHeight != 0) {
            GLES20.glUniform2f(this.mUniformTexSize, (float) this.mSnapshotOriginWidth, (float) this.mSnapshotOriginHeight);
            GLES20.glUniform1f(this.mUniformAngle, getRotation());
        }
    }

    public String getFragShaderString() {
        return "precision highp float; \nvarying vec2 vTexCoord; \nuniform sampler2D sTexture; \nuniform float sAngle; \nuniform vec2 vTexSize; \nvoid main() \n{ \n  float pf = min(vTexSize.s,vTexSize.t) / max(vTexSize.s,vTexSize.t);\n  float mOrigin = atan(pf); \n  float rorate_angle = sAngle; \n  float fangle = radians(abs(rorate_angle)) + mOrigin;\n  float s = sin(mOrigin) / sin(fangle);\n  float center_x = 0.5 * vTexSize.s; \n  float center_y = 0.5 * vTexSize.t; \n  float tx = center_x - 0.5 * vTexSize.s * s; \n  float ty = center_y - 0.5 * vTexSize.t * s; \n  float cosangle = cos(radians(rorate_angle)); \n  float sinangle = sin(radians(rorate_angle)); \n  float x = s * (vTexCoord.s * vTexSize.s) + tx; \n  float y = s * (vTexCoord.t * vTexSize.t) + ty; \n  float  x1 = x - center_x; \n  float  y1 = y - center_y; \n  x = cosangle * x1 + sinangle * y1 + center_x; \n  y = -1.0 * sinangle * x1 + cosangle * y1 + center_y; \n  x = x / vTexSize.s; \n  y = y / vTexSize.t; \n  x = clamp(x,0.0,1.0); \n  y = clamp(y,0.0,1.0); \n  gl_FragColor = texture2D(sTexture, vec2(x,y)); \n} \n";
    }

    private float getRotation() {
        float rotation = this.mShootRotation - ((float) this.mOrientation);
        if (rotation > 180.0f) {
            return rotation - 360.0f;
        }
        return rotation;
    }
}
