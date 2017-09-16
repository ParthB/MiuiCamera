package com.android.camera.effect.renders;

import android.opengl.GLES20;
import com.android.gallery3d.ui.GLCanvas;
import java.nio.IntBuffer;

public class BlackWhiteEffectRender extends RGBTransEffectRender {
    private static final int[] sRGBLut = new int[]{45, 45, 45, 45, 45, 45, 46, 46, 46, 46, 46, 47, 47, 47, 47, 47, 48, 48, 48, 48, 48, 49, 49, 49, 49, 50, 50, 50, 50, 51, 51, 51, 52, 52, 52, 52, 53, 53, 53, 54, 54, 54, 55, 55, 56, 56, 56, 57, 57, 58, 58, 59, 59, 60, 60, 61, 61, 62, 62, 63, 63, 64, 64, 65, 66, 66, 67, 67, 68, 69, 69, 70, 71, 71, 72, 73, 74, 74, 75, 76, 77, 78, 78, 79, 80, 81, 82, 83, 84, 85, 86, 86, 87, 88, 89, 90, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 103, 104, 105, 106, 107, 109, 110, 111, 112, 114, 115, 116, 117, 119, 120, 121, 123, 124, 125, 127, 128, 129, 131, 132, 133, 134, 136, 137, 138, 140, 141, 142, 144, 145, 147, 148, 149, 151, 152, 153, 155, 156, 157, 159, 160, 161, 163, 164, 166, 167, 168, 170, 171, 173, 174, 175, 177, 178, 179, 181, 182, 184, 185, 186, 188, 189, 190, 192, 193, 194, 195, 196, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 211, 212, 213, 213, 214, 214, 215, 215, 216, 216, 217, 217, 217, 218, 218, 218, 219, 219, 219, 219, 220, 220, 220, 220, 221, 221, 221, 221, 222, 222, 222, 223, 223, 223, 223, 224, 224, 224, 225, 225, 226, 226, 226, 227, 227, 228, 228, 229, 229, 230, 230, 231, 231, 231, 232, 232, 233, 233, 234, 234, 235, 235, 236, 237};
    private static IntBuffer sRGBLutBuffer;
    protected float mCon = 45.0f;
    protected int mUniformConH;

    private static IntBuffer getRGBLutBuffer() {
        if (sRGBLutBuffer == null) {
            for (int i = 0; i < sRGBLut.length; i++) {
                sRGBLut[i] = (((sRGBLut[i] << 16) | -16777216) | (sRGBLut[i] << 8)) | sRGBLut[i];
            }
            sRGBLutBuffer = IntBuffer.wrap(sRGBLut);
        }
        sRGBLutBuffer.rewind();
        return sRGBLutBuffer;
    }

    public BlackWhiteEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
        setRGBTransLutBuffer(getRGBLutBuffer());
    }

    protected void initShader() {
        super.initShader();
        this.mUniformConH = GLES20.glGetUniformLocation(this.mProgram, "uCon");
    }

    protected void initShaderValue(boolean isSnapShot) {
        super.initShaderValue(isSnapShot);
        GLES20.glUniform1f(this.mUniformConH, this.mCon);
    }

    public String getFragShaderString() {
        return "precision mediump int; \nprecision mediump float; \nuniform sampler2D sTexture; \nuniform sampler2D sRGBLut; \nvarying vec2 vTexCoord; \nuniform float uAlpha; \nuniform float uCon; \nfloat removeColor(vec3 color) { \n   float maxColor = max(color.r, max(color.g, color.b)); \n   float minColor = min(color.r, min(color.g, color.b)); \n   return (maxColor + minColor) / 2.0; \n} \nfloat CProcess(float color) { \n    float cValue = uCon / 100.0 + 1.0; \n    float dstf = clamp((color - 0.5) * cValue + 0.5, 0.0, 1.0); \n    return dstf; \n} \nvoid main() { \n    vec3 color = texture2D(sTexture, vTexCoord).rgb; \n    float gray = removeColor(color); \n    gray = CProcess(gray); \n    gray = float(int(gray * 255.0)) / 256.0; \n    gray = texture2D(sRGBLut, vec2(gray, 0.0)).r; \n    gl_FragColor = vec4(gray, gray, gray, 1.0) * uAlpha; \n}";
    }
}
