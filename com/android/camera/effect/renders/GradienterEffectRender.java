package com.android.camera.effect.renders;

import android.opengl.GLES20;
import com.android.camera.Log;
import com.android.camera.effect.EffectController;
import com.android.gallery3d.ui.GLCanvas;

public class GradienterEffectRender extends PixelEffectRender {
    private boolean mKeepZero;
    private float mLastRotation = -1.0f;
    private int mShiftTimes;
    private int mUniformAngle;
    private int mUniformTexSize;
    private boolean mZero;

    public GradienterEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    protected void initShader() {
        super.initShader();
        this.mUniformAngle = GLES20.glGetUniformLocation(this.mProgram, "sAngle");
        this.mUniformTexSize = GLES20.glGetUniformLocation(this.mProgram, "vTexSize");
    }

    protected void initShaderValue(boolean isSnapShot) {
        super.initShaderValue(isSnapShot);
        GLES20.glUniform2f(this.mUniformTexSize, (float) this.mPreviewWidth, (float) this.mPreviewHeight);
        GLES20.glUniform1f(this.mUniformAngle, getRotation());
    }

    public String getFragShaderString() {
        return "precision highp float; \nvarying vec2 vTexCoord; \nuniform sampler2D sTexture; \nuniform float sAngle; \nuniform vec2 vTexSize; \nuniform float uAlpha; \nvoid main() \n{ \n  vec4 color = texture2D(sTexture, vTexCoord); \n  if (sAngle != 0.0) { \n    float pf = min(vTexSize.s,vTexSize.t) / max(vTexSize.s,vTexSize.t); \n    float mOrigin = atan(pf); \n    float rorate_angle = -sAngle; \n    float fangle = radians(abs(rorate_angle)) + mOrigin; \n    float s = sin(mOrigin) / sin(fangle);\n    s = 1.0 / s; \n    float center_x = 0.5 * vTexSize.s; \n    float center_y = 0.5 * vTexSize.t; \n    float cosangle = cos(radians(rorate_angle)); \n    float sinangle = sin(radians(rorate_angle)); \n    float x = vTexCoord.s * vTexSize.s; \n    float y = vTexCoord.t * vTexSize.t; \n    float  x1 = x - center_x; \n    float  y1 = y - center_y; \n    x = cosangle * x1 + sinangle * y1 + center_x; \n    y = -1.0 * sinangle * x1 + cosangle * y1 + center_y; \n    x = s * (x - center_x) + center_x; \n     y = s * (y - center_y) + center_y; \n     float dis = min( min(y,vTexSize.t - y),min(x, vTexSize.s - x)); \n    float ap = abs(dis) / s /1.0; \n    float a = sqrt(exp(-1.0 * ap * ap)); \n    float yy = float(int(y + 0.5)); \n    float xx = float(int(x + 0.5)); \n    if (yy <= 0.0 || yy >= vTexSize.t || xx <= 0.0 || xx >= vTexSize.s) { \n        color = mix(color * 0.4,vec4(1.0,1.0,1.0,1.0),a); \n    } else { \n        color = mix(color,vec4(1.0,1.0,1.0,1.0),a); \n    } \n    color = clamp(color,0.0,1.0); \n  } \n  gl_FragColor = color*uAlpha; \n} \n";
    }

    private float getRotation() {
        float deviceRotation = EffectController.getInstance().getDeviceRotation();
        if (deviceRotation < 0.0f) {
            this.mLastRotation = -1.0f;
            return 0.0f;
        }
        filteRotation(deviceRotation);
        float rotation = this.mLastRotation - ((float) EffectController.getInstance().getOrientation());
        if (rotation > 180.0f) {
            rotation -= 360.0f;
        }
        boolean isZero = Math.abs(rotation) < 0.5f;
        if (isZero != this.mKeepZero) {
            this.mKeepZero = isZero;
            this.mShiftTimes = 1;
        } else {
            if (this.mShiftTimes < 5) {
                this.mShiftTimes++;
            }
            if (this.mShiftTimes == 5) {
                this.mZero = this.mKeepZero;
            }
        }
        if (this.mZero) {
            rotation = 0.0f;
        }
        return rotation;
    }

    private void filteRotation(float deviceRotation) {
        if (this.mLastRotation != -1.0f) {
            if (Math.abs(deviceRotation - this.mLastRotation) > 180.0f) {
                if (deviceRotation < this.mLastRotation) {
                    deviceRotation += 360.0f;
                } else {
                    this.mLastRotation += 360.0f;
                }
            }
            this.mLastRotation = (this.mLastRotation * 0.7f) + (0.3f * deviceRotation);
        } else {
            this.mLastRotation = deviceRotation;
        }
        while (this.mLastRotation >= 360.0f) {
            this.mLastRotation -= 360.0f;
        }
        Log.v("GradienterEffectRender", "filteRotation deviceRotation=" + deviceRotation + " mLastRotation=" + this.mLastRotation);
    }
}
