package com.android.camera.effect.renders;

import com.android.gallery3d.ui.GLCanvas;
import java.nio.IntBuffer;

public class InstagramHudsonEffectRender extends RGBTransEffectRender {
    private static final int[] sCurveBLut = new int[]{0, 0, 0, 0, 0, 1, 2, 3, 5, 6, 9, 11, 13, 16, 19, 22, 25, 28, 31, 35, 38, 41, 44, 47, 50, 53, 55, 58, 61, 63, 66, 68, 70, 72, 74, 76, 78, 80, 82, 83, 85, 87, 88, 90, 91, 92, 94, 95, 96, 97, 98, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 130, 131, 132, 134, 135, 136, 138, 139, 140, 142, 143, 145, 146, 148, 149, 151, 152, 154, 155, 157, 158, 160, 161, 162, 164, 165, 167, 168, 169, 171, 172, 173, 174, 175, 177, 178, 179, 180, 181, 182, 183, 184, 185, 185, 186, 187, 188, 189, 190, 190, 191, 192, 192, 193, 194, 194, 195, 196, 196, 197, 197, 198, 198, 199, 199, 200, 200, 201, 201, 202, 202, 203, 203, 203, 204, 204, 205, 205, 205, 206, 206, 207, 207, 207, 208, 208, 208, 209, 209, 210, 210, 210, 211, 211, 212, 212, 212, 213, 213, 214, 214, 215, 215, 216, 216, 216, 217, 217, 218, 218, 219, 219, 220, 220, 221, 221, 222, 222, 223, 223, 224, 224, 225, 226, 226, 227, 227, 228, 228, 229, 230, 230, 231, 231, 232, 233, 233, 234, 235, 236, 237, 237, 238, 239, 240, 241, 242, 243, 243, 244, 245, 246, 247, 248, 248, 249, 250, 250, 251, 252, 252, 252, 253, 253, 253, 254, 254, 254, 254, 254, 255, 255, 255, 255, 255};
    private static final int[] sCurveGLut = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 16, 17, 19, 20, 21, 23, 24, 26, 27, 29, 31, 32, 34, 36, 38, 40, 42, 44, 45, 47, 49, 51, 53, 55, 57, 59, 60, 62, 64, 65, 67, 68, 70, 71, 73, 74, 75, 76, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 91, 92, 93, 94, 95, 96, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 109, 110, 111, 113, 114, 115, 117, 119, 120, 122, 124, 126, 127, 129, 131, 133, 135, 136, 138, 140, 141, 143, 144, 146, 147, 148, 150, 151, 152, 153, 155, 156, 157, 158, 159, 160, 161, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 176, 177, 178, 179, 180, 181, 182, 183, 183, 184, 185, 186, 187, 187, 188, 189, 190, 190, 191, 192, 192, 193, 194, 194, 195, 196, 196, 197, 197, 198, 198, 199, 200, 200, 200, 201, 201, 202, 202, 203, 203, 204, 204, 204, 205, 205, 206, 206, 207, 207, 207, 208, 208, 209, 209, 210, 211, 211, 212, 213, 213, 214, 215, 216, 216, 217, 218, 219, 220, 221, 222, 223, 223, 224, 225, 226, 227, 228, 228, 229, 230, 230, 231, 231, 232, 232, 233, 233, 234, 234, 235, 235, 235, 236, 236, 237, 237, 238, 238, 239, 239, 240, 240, 241, 242, 242, 243, 244, 245, 246, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255};
    private static IntBuffer sCurveRGBLutBuffer;
    private static final int[] sCurveRLut = new int[]{31, 32, 33, 34, 35, 36, 37, 37, 38, 39, 40, 41, 42, 43, 44, 45, 45, 46, 47, 48, 49, 49, 50, 51, 52, 52, 53, 54, 55, 55, 56, 57, 57, 58, 59, 60, 60, 61, 62, 63, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 77, 78, 79, 80, 81, 83, 84, 85, 86, 88, 89, 90, 91, 92, 94, 95, 96, 97, 98, 100, 101, 102, 103, 104, 106, 107, 108, 109, 110, 111, 113, 114, 115, 116, 117, 118, 120, 121, 122, 123, 124, 125, 127, 128, 129, 130, 132, 133, 134, 135, 137, 138, 139, 141, 142, 143, 145, 146, 148, 149, 150, 152, 153, 155, 156, 157, 159, 160, 161, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 172, 173, 174, 175, 175, 176, 177, 177, 178, 179, 179, 180, 180, 181, 182, 182, 183, 183, 184, 185, 185, 186, 187, 188, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 198, 199, 200, 201, 202, 203, 204, 204, 205, 206, 206, 207, 207, 208, 208, 209, 209, 210, 210, 211, 211, 212, 212, 213, 213, 214, 214, 215, 215, 216, 216, 217, 217, 218, 218, 219, 219, 220, 221, 221, 222, 222, 223, 224, 224, 225, 225, 226, 227, 227, 228, 229, 229, 230, 231, 231, 232, 233, 233, 234, 235, 236, 237, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 254, 255, 255, 255, 255, 255, 255, 255};
    private static final int[] sSelfRGBLut = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 14, 14, 15, 16, 16, 17, 17, 18, 19, 20, 20, 21, 22, 22, 23, 24, 25, 25, 26, 27, 28, 29, 29, 30, 31, 32, 33, 34, 35, 35, 36, 37, 38, 39, 40, 41, 42, 43, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 137, 138, 139, 140, 141, 142, 143, 144, 145, 147, 148, 149, 150, 151, 153, 154, 155, 156, 157, 159, 160, 161, 162, 164, 165, 166, 168, 169, 170, 172, 173, 174, 176, 177, 178, 180, 181, 183, 184, 186, 187, 189, 190, 192, 193, 195, 196, 198, 199, 201, 202, 204, 205, 207, 209, 210, 212, 213, 215, 217, 218, 220, 221, 223, 225, 226, 228, 230, 231, 233, 234, 236, 238, 239, 241};
    private static IntBuffer sSelfRGBLutBuffer;

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
        InstagramHudsonEffectRender secondPassRender = new InstagramHudsonEffectRender(canvas, id);
        secondPassRender.setRGBTransLutBuffer(getSelfRGBLutBuffer());
        return new PipeRenderPair(canvas, firstPassRender, secondPassRender, false);
    }

    protected InstagramHudsonEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    public String getFragShaderString() {
        return "precision mediump int; \nprecision mediump float; \nuniform sampler2D sTexture; \nuniform sampler2D sRGBLut; \nvarying vec2 vTexCoord; \nuniform float uAlpha; \nvec3 adjust_contrast(vec3 color, float f_contrast) { \n    float ff = log(1.0 - f_contrast) / log(0.5); \n    float r = color.r, g = color.g, b = color.b; \n    if (r < 0.5) { \n        r = pow(2.0 * r, ff) / 2.0; \n    } else { \n        r = 1.0 - pow(2.0 * (1.0 - r), ff) / 2.0; \n    } \n    if (g < 0.5) { \n        g = pow(2.0 * g, ff) / 2.0; \n    } else { \n        g = 1.0 - pow(2.0 * (1.0 - g), ff) / 2.0; \n    } \n    if (b < 0.5) { \n        b = pow(2.0 * b, ff) / 2.0; \n    } else { \n        b = 1.0 - pow(2.0 * (1.0 - b), ff) / 2.0; \n    } \n    return vec3(r, g, b); \n} \nvec3 adjust_light(vec3 color, float f_light, float ff_ll) { \n    color.r = pow(color.r, ff_ll); \n    color.r = (color.r * f_light) / (2.0 * color.r * f_light - color.r - f_light + 1.0); \n    color.r = pow(color.r, 1.0 / ff_ll); \n    color.g = pow(color.g, ff_ll); \n    color.g = (color.g * f_light) / (2.0 * color.g * f_light - color.g - f_light + 1.0); \n    color.g = pow(color.g, 1.0 / ff_ll); \n    color.b = pow(color.b, ff_ll); \n    color.b = (color.b * f_light) / (2.0 * color.b * f_light - color.b - f_light + 1.0); \n    color.b = pow(color.b, 1.0 / ff_ll); \n    color = clamp(color, 0.0, 1.0); \n    return color; \n} \nvoid main() { \n    vec3 color = texture2D(sTexture, vTexCoord).rgb; \n    vec3 index; \n    index.r = float(int(color.r * 255.0)) / 256.0; \n    index.g = float(int(color.g * 255.0)) / 256.0; \n    index.b = float(int(color.b * 255.0)) / 256.0; \n    color.r = texture2D(sRGBLut, vec2(index.r, 0.0)).r; \n    color.g = texture2D(sRGBLut, vec2(index.g, 0.0)).g; \n    color.b = texture2D(sRGBLut, vec2(index.b, 0.0)).b; \n    float msk_value = exp(-(pow(vTexCoord[0]-0.5, 2.0) + pow(vTexCoord[1]-0.5, 2.0))/0.2); \n    vec3 combined_back = msk_value * vec3(158.0/255.0, 167.0/255.0, 201.0/255.0); \n    combined_back += (1.0-msk_value) * vec3(100.0/255.0, 100.0/255.0, 100.0/255.0); \n    vec3 tmp = vec3(float(color.r < 0.5), float(color.g < 0.5), float(color.b < 0.5)); \n    color = 0.25 * color + 0.75 * (2.0 * color * combined_back * tmp + (1.0 - 2.0*(1.0-color)*(1.0-combined_back)) * (1.0-tmp)); \n    color = clamp(color, 0.0, 1.0); \n    float bri = 0.5 + 20.0 / 155.0; \n    float ff_ll = 3.8; \n    color = adjust_light(color, bri, ff_ll); \n    color = adjust_contrast(color, 0.55); \n    color = clamp(color, 0.0, 1.0); \n    gl_FragColor = vec4(color, 1.0) * uAlpha; \n}";
    }
}
