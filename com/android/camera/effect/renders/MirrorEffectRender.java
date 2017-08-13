package com.android.camera.effect.renders;

import android.opengl.GLES20;
import com.android.camera.effect.EffectController;
import com.android.gallery3d.ui.GLCanvas;

public class MirrorEffectRender extends ConvolutionEffectRender {
    private int mUniformDirectionH;

    public MirrorEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    protected void initShader() {
        super.initShader();
        this.mUniformDirectionH = GLES20.glGetUniformLocation(this.mProgram, "uDir");
    }

    protected void initShaderValue(boolean isSnapShot) {
        super.initShaderValue(isSnapShot);
        GLES20.glUniform1i(this.mUniformDirectionH, getDirection(isSnapShot));
    }

    public String getFragShaderString() {
        return "precision mediump float;  \nvarying vec2 vTexCoord;  \nuniform sampler2D sTexture;  \nuniform int uDir; \nuniform float uAlpha; \nuniform vec2 uStep;  \nvoid main()               \n{                         \n    if (uDir == 0)    \n    { \n          gl_FragColor=texture2D(sTexture, vec2(vTexCoord.s>0.5 ? (1.0-vTexCoord.s) : vTexCoord.s, vTexCoord.t));\n    } \n    else if (uDir == 1)   \n    { \n          gl_FragColor=texture2D(sTexture, vec2(vTexCoord.s, vTexCoord.t<0.5 ? (1.0-vTexCoord.t) : vTexCoord.t));\n    } \n    else if (uDir == 2)   \n    { \n          gl_FragColor=texture2D(sTexture, vec2(vTexCoord.s<0.5 ? (1.0-vTexCoord.s) : vTexCoord.s, vTexCoord.t));\n    } \n    else if (uDir == 3)   \n    { \n          gl_FragColor=texture2D(sTexture, vec2(vTexCoord.s, vTexCoord.t>0.5 ? (1.0-vTexCoord.t) : vTexCoord.t));\n    } \n    gl_FragColor = gl_FragColor*uAlpha; \n}";
    }

    private int getDirection(boolean isSnapShot) {
        int orientation = isSnapShot ? this.mJpegOrientation : EffectController.getInstance().getOrientation();
        if (isSnapShot && this.mMirror) {
            orientation = (orientation + 180) % 360;
        }
        if (orientation == 270) {
            return 3;
        }
        if (orientation == 180) {
            return 2;
        }
        if (orientation == 90) {
            return 1;
        }
        return 0;
    }
}
