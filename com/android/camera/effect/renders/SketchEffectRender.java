package com.android.camera.effect.renders;

import com.android.gallery3d.ui.GLCanvas;

public class SketchEffectRender extends ConvolutionEffectRender {
    public SketchEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    public String getFragShaderString() {
        return "precision mediump float; \nuniform vec2 uStep; \nuniform sampler2D sTexture; \nvarying vec2 vTexCoord; \nvec4 rgb2gray(vec4 color) { \n  float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114)); \n  return vec4(gray, gray, gray, 1.0); \n} \nvoid main() \n{ \n  vec4 sample0, sample1, sample2, sample3, sample5, sample6, sample7, sample8; \n  sample0 = texture2D(sTexture, vTexCoord + vec2(-uStep.x, -uStep.y)); \n  sample1 = texture2D(sTexture, vTexCoord + vec2(0.0, -uStep.y)); \n  sample2 = texture2D(sTexture, vTexCoord + vec2(uStep.x, -uStep.y)); \n  sample3 = texture2D(sTexture, vTexCoord + vec2(-uStep.x, 0.0)); \n  gl_FragColor = texture2D(sTexture, vTexCoord); \n  sample5 = texture2D(sTexture, vTexCoord + vec2(uStep.x, 0.0)); \n  sample6 = texture2D(sTexture, vTexCoord + vec2(-uStep.x, uStep.y)); \n  sample7 = texture2D(sTexture, vTexCoord + vec2(0.0, uStep.y)); \n  sample8 = texture2D(sTexture, vTexCoord + vec2(uStep.x, uStep.y)); \n  vec4 sample = sqrt((gl_FragColor-sample0)*(gl_FragColor-sample0)+(gl_FragColor-sample1)*(gl_FragColor-sample1)) \n      +sqrt((gl_FragColor-sample2)*(gl_FragColor-sample2)+(gl_FragColor-sample3)*(gl_FragColor-sample3)) \n      +sqrt((gl_FragColor-sample5)*(gl_FragColor-sample5)+(gl_FragColor-sample6)*(gl_FragColor-sample6)) \n      +sqrt((gl_FragColor-sample7)*(gl_FragColor-sample7)+(gl_FragColor-sample8)*(gl_FragColor-sample8)); \n  sample = sample * 2.0; \n  sample = clamp(sample, 0.0, 1.0); \n  gl_FragColor = rgb2gray(1.0 - sample); \n} ";
    }
}
