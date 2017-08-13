package com.android.camera.effect.renders;

import android.opengl.GLES20;
import com.android.gallery3d.ui.GLCanvas;
import java.nio.IntBuffer;

public class InstagramRiseEffectRender extends RGBTransEffectRender {
    private static final int[] sCurveBLut = new int[]{34, 34, 35, 35, 36, 36, 37, 37, 38, 38, 39, 40, 40, 41, 41, 42, 42, 43, 44, 44, 45, 45, 46, 47, 47, 48, 48, 49, 50, 50, 51, 52, 53, 53, 54, 55, 55, 56, 57, 58, 58, 59, 60, 61, 62, 62, 63, 64, 65, 66, 66, 67, 68, 69, 70, 71, 72, 72, 73, 74, 75, 76, 77, 78, 79, 79, 80, 81, 82, 83, 84, 85, 86, 86, 87, 88, 89, 90, 91, 92, 93, 93, 94, 95, 96, 97, 98, 99, 100, 100, 101, 102, 103, 104, 105, 106, 107, 107, 108, 109, 110, 111, 112, 113, 113, 114, 115, 116, 117, 118, 119, 119, 120, 121, 122, 123, 124, 124, 125, 126, 127, 128, 129, 129, 130, 131, 132, 133, 134, 134, 135, 136, 137, 138, 138, 139, 140, 141, 142, 142, 143, 144, 145, 146, 146, 147, 148, 149, 150, 150, 151, 152, 153, 154, 154, 155, 156, 157, 158, 159, 159, 160, 161, 162, 163, 163, 164, 165, 166, 167, 168, 169, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 179, 180, 181, 182, 183, 184, 185, 186, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 200, 201, 202, 203, 204, 205, 207, 208, 209, 210, 211, 212, 213, 215, 216, 217, 218, 219, 220, 221, 222, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255};
    private static final int[] sCurveGLut = new int[]{23, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 226, 227, 228, 229, 230, 231, 232, 233, 233, 234, 235, 236, 237, 237, 238, 239, 239, 240, 241, 241, 242, 243, 243, 244, 244, 245, 245, 246, 247, 247, 248, 248, 249, 249, 250, 250, 251, 251, 252, 252, 253, 253, 254, 254, 255};
    private static IntBuffer sCurveRGBLutBuffer;
    private static final int[] sCurveRLut = new int[]{35, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 93, 94, 95, 96, 97, 98, 99, 100, 101, 101, 102, 103, 104, 105, 106, 107, 108, 108, 109, 110, 111, 112, 113, 114, 115, 115, 116, 117, 118, 119, 120, 121, 121, 122, 123, 124, 125, 126, 126, 127, 128, 129, 130, 131, 131, 132, 133, 134, 135, 136, 136, 137, 138, 139, 140, 141, 141, 142, 143, 144, 145, 146, 147, 148, 148, 149, 150, 151, 152, 153, 154, 155, 156, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 238, 239, 240, 241, 242, 242, 243, 244, 244, 245, 245, 246, 246, 247, 247, 248, 248, 249, 249, 249, 250, 250, 251, 251, 251, 252, 252, 252, 252, 253, 253, 253, 253, 254, 254, 254, 255};
    private static final int[] sSelfRGBLut = new int[]{1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 6, 7, 8, 8, 9, 9, 10, 11, 11, 12, 13, 14, 15, 16, 16, 18, 19, 20, 21, 22, 23, 24, 26, 27, 28, 30, 31, 33, 34, 36, 37, 39, 41, 42, 44, 46, 47, 49, 51, 52, 54, 56, 58, 59, 61, 63, 65, 67, 68, 70, 72, 74, 75, 77, 79, 81, 83, 84, 86, 88, 90, 91, 93, 95, 96, 98, 100, 102, 103, 105, 107, 108, 110, 111, 113, 114, 116, 118, 119, 121, 122, 123, 125, 126, 128, 129, 130, 132, 133, 134, 135, 137, 138, 139, 140, 142, 143, 144, 145, 146, 147, 148, 149, 150, 152, 153, 154, 155, 156, 157, 158, 159, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 168, 169, 170, 171, 172, 172, 173, 174, 175, 176, 176, 177, 178, 179, 179, 180, 181, 181, 182, 183, 184, 184, 185, 186, 186, 187, 188, 188, 189, 189, 190, 191, 191, 192, 193, 193, 194, 194, 195, 195, 196, 197, 197, 198, 198, 199, 199, 200, 200, 201, 201, 202, 203, 203, 203, 204, 204, 205, 205, 206, 206, 207, 207, 208, 208, 209, 209, 210, 210, 211, 211, 211, 212, 212, 213, 213, 214, 214, 215, 215, 216, 216, 217, 217, 218, 218, 218, 219, 219, 220, 220, 221, 222, 222, 223, 223, 224, 224, 225, 225, 226, 226, 227, 227, 228, 228, 229, 230, 230, 231, 231, 232, 232, 233, 233, 234, 235};
    private static IntBuffer sSelfRGBLutBuffer;
    protected float[] mCenPos = new float[]{0.57f, 0.5814f};
    protected float mCon = -7.0f;
    protected float[] mGCenPos = new float[]{0.5f, 0.64f};
    protected float mRadius = 0.91f;
    protected float mStd = 0.236f;
    protected int mUniformCenPosH;
    protected int mUniformConH;
    protected int mUniformGCenPosH;
    protected int mUniformRadiusH;
    protected int mUniformStdH;
    protected int mUniformWHH;

    private static IntBuffer getCurveRGBLutBuffer() {
        if (sCurveRGBLutBuffer == null) {
            int[] rgbLut = new int[sCurveRLut.length];
            for (int i = 0; i < rgbLut.length; i++) {
                rgbLut[i] = (((sCurveBLut[i] << 16) | -16777216) | (sCurveGLut[i] << 8)) | sCurveRLut[i];
            }
            sCurveRGBLutBuffer = IntBuffer.wrap(rgbLut);
        }
        sCurveRGBLutBuffer.rewind();
        return sCurveRGBLutBuffer;
    }

    private static IntBuffer getSelfRGBLutBuffer() {
        if (sSelfRGBLutBuffer == null) {
            for (int i = 0; i < sSelfRGBLut.length; i++) {
                sSelfRGBLut[i] = (((sSelfRGBLut[i] << 16) | -16777216) | (sSelfRGBLut[i] << 8)) | sSelfRGBLut[i];
            }
            sSelfRGBLutBuffer = IntBuffer.wrap(sSelfRGBLut);
        }
        sSelfRGBLutBuffer.rewind();
        return sSelfRGBLutBuffer;
    }

    public static Render create(GLCanvas canvas, int id) {
        CurveEffectRender firstPassRender = new CurveEffectRender(canvas, id);
        firstPassRender.setRGBTransLutBuffer(getCurveRGBLutBuffer());
        InstagramRiseEffectRender secondPassRender = new InstagramRiseEffectRender(canvas, id);
        secondPassRender.setRGBTransLutBuffer(getSelfRGBLutBuffer());
        return new PipeRenderPair(canvas, firstPassRender, secondPassRender, false);
    }

    protected InstagramRiseEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    protected void initShader() {
        super.initShader();
        this.mUniformConH = GLES20.glGetUniformLocation(this.mProgram, "uCon");
        this.mUniformCenPosH = GLES20.glGetUniformLocation(this.mProgram, "uCenPos");
        this.mUniformGCenPosH = GLES20.glGetUniformLocation(this.mProgram, "uGCenPos");
        this.mUniformRadiusH = GLES20.glGetUniformLocation(this.mProgram, "uRadius");
        this.mUniformStdH = GLES20.glGetUniformLocation(this.mProgram, "uStd");
        this.mUniformWHH = GLES20.glGetUniformLocation(this.mProgram, "uWH");
    }

    protected void initShaderValue(boolean isSnapShot) {
        super.initShaderValue(isSnapShot);
        GLES20.glUniform1f(this.mUniformConH, this.mCon);
        GLES20.glUniform2f(this.mUniformCenPosH, this.mCenPos[0], this.mCenPos[1]);
        GLES20.glUniform2f(this.mUniformGCenPosH, this.mGCenPos[0], this.mGCenPos[1]);
        GLES20.glUniform1f(this.mUniformRadiusH, this.mRadius);
        GLES20.glUniform1f(this.mUniformStdH, this.mStd);
        GLES20.glUniform2f(this.mUniformWHH, ((float) this.mPreviewWidth) * 1.0f, ((float) this.mPreviewWidth) * 1.0f);
    }

    public String getFragShaderString() {
        return "precision mediump int; \nprecision mediump float; \nuniform sampler2D sTexture; \nuniform sampler2D sRGBLut; \nvarying vec2 vTexCoord; \nuniform float uAlpha; \nuniform float uCon; \nuniform vec2  uCenPos; \nuniform vec2  uGCenPos; \nuniform float uRadius; \nuniform float uStd; \nuniform vec2  uWH; \nvec3 CProcess(vec3 color) { \n    vec3 dstcolor; \n    float cValue = uCon/100.0 + 1.0; \n    dstcolor = clamp((color - 0.5) *cValue + 0.5,0.0,1.0); \n    return dstcolor; \n} \nfloat WJianbianProcess() { \n    float disx,disy,dis,f1,f2,f,pf = uWH.x / uWH.y,x,y; \n    f1 = max(uCenPos.x,1.0 - uCenPos.x);f2 = max(uCenPos.y,1.0 - uCenPos.y); \n    if (pf < 1.0) { \n        disx = (vTexCoord.x - uCenPos.x) * (vTexCoord.x - uCenPos.x); \n        if (vTexCoord.y/pf < uCenPos.y) { \n            y = vTexCoord.y; \n        } else if ((1.0 - vTexCoord.y)/pf < (1.0 - uCenPos.y)) { \n            y = pf - (1.0 - vTexCoord.y); \n        } else { \n            y = uCenPos.y * pf; \n        } \n        disy = (y/pf - uCenPos.y) * (y/pf - uCenPos.y); \n    } else { \n        disy = (vTexCoord.y - uCenPos.y) * (vTexCoord.y - uCenPos.y); \n        if (vTexCoord.x * pf < uCenPos.x) { \n            x = vTexCoord.x; \n        } else if ((1.0 - vTexCoord.x)*pf < (1.0 - uCenPos.x)) { \n            x = 1.0/pf - (1.0 - vTexCoord.x); \n        } else { \n            x = uCenPos.x / pf; \n        } \n        disx = (x * pf - uCenPos.x) * (x * pf - uCenPos.x); \n    } \n    dis = disx + disy; \n    f1 = sqrt(dis)/(sqrt(f1 * f1 + f2 * f2) * uRadius); \n    if (f1 > 1.0) { \n        f = 0.4; \n    } else { \n        f2 = 0.9908 * pow(f1,3.0) -1.4934 * pow(f1,2.0) -0.4974 * f1 + 1.0; \n        f = 0.6 * f2 + 0.4; \n    } \n    return f; \n} \nfloat WEraserProcess() { \n    float disx,disy,dis,f1,f2,f,pf = uWH.x / uWH.y,x,y,std1; \n    f1 = max(uGCenPos.x,1.0 - uGCenPos.x);f2 = max(uGCenPos.y,1.0 - uGCenPos.y); \n    std1 = 2.0 * uStd * uStd * (f1 * f1 + f2 * f2); \n    if (pf < 1.0) { \n        disx = (vTexCoord.x - uGCenPos.x) * (vTexCoord.x - uGCenPos.x); \n        if (vTexCoord.y /pf < uCenPos.y) { \n            y = vTexCoord.y; \n        } else if ((1.0 - vTexCoord.y)/pf < (1.0 - uCenPos.y)) { \n            y = pf - (1.0 - vTexCoord.y); \n        } else { \n            y = uCenPos.y * pf; \n        } \n        disy = (y/pf - uGCenPos.y) * (y/pf - uGCenPos.y); \n    } else { \n        disy = (vTexCoord.y - uCenPos.y) * (vTexCoord.y - uCenPos.y); \n        if (vTexCoord.x * pf < uCenPos.x) {  \n            x = vTexCoord.x; \n        } else if ((1.0 - vTexCoord.x)*pf < (1.0 - uCenPos.x)) { \n            x = 1.0/pf - (1.0 - vTexCoord.x); \n        } else { \n            x = uCenPos.x / pf; \n        } \n        disx =  (x * pf - uCenPos.x) * (x * pf - uCenPos.x); \n    } \n    dis = disx + disy; \n    f = exp(-1.0 * (disx + disy)/std1); \n    return f; \n} \nfloat BlendOverlayF(float base, float blend) { \n    return (base < 0.5 ? (2.0 * base * blend) : (1.0 - 2.0 * (1.0 - base) * (1.0 - blend))); \n} \nvec3 BlendOverlay(vec3 base, vec3 blend) { \n    vec3 destColor; \n    destColor.r = BlendOverlayF(base.r, blend.r); \n    destColor.g = BlendOverlayF(base.g, blend.g); \n    destColor.b = BlendOverlayF(base.b, blend.b); \n    return destColor; \n} \nvoid main() { \n    vec3 color = texture2D(sTexture, vTexCoord).rgb; \n    int index = int(color.r * 255.0); \n    float index1 = float(index) / 256.0; \n    color.r = texture2D(sRGBLut,vec2(index1,0.0)).r; \n    index = int(color.g * 255.0); \n    index1 = float(index) / 256.0; \n    color.g = texture2D(sRGBLut,vec2(index1,0.0)).r; \n    index = int(color.b * 255.0); \n    index1 = float(index) / 256.0; \n    color.b = texture2D(sRGBLut,vec2(index1,0.0)).r; \n    vec3 oricolor = vec3(color.r,color.g,color.b); \n    oricolor = CProcess(oricolor); \n    float f1 = WJianbianProcess(); \n    float f2 = WEraserProcess(); \n    float f = (1.0 - f2) * f1 + f2; \n    f = (1.0 - f2) * f + f2; \n    f = 1.0 - f; \n    vec3 dstcolor = BlendOverlay(oricolor,vec3(0.0,0.0,0.0)); \n    dstcolor = dstcolor * f + oricolor * (1.0 - f); \n    gl_FragColor = vec4(dstcolor.rgb,1.0) * uAlpha; \n} \n";
    }
}
