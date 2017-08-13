package com.android.camera.effect.renders;

import com.android.gallery3d.ui.GLCanvas;

public class BigFaceEffectRender extends PixelEffectRender {
    public BigFaceEffectRender(GLCanvas canvas, int id) {
        super(canvas, id);
    }

    public String getFragShaderString() {
        return "precision mediump float; \nuniform sampler2D sTexture; \nvarying vec2 vTexCoord; \nuniform float uAlpha; \nvec4 bigface() { \n    float trans_center_x = 0.5; \n    float trans_center_y = 0.5; \n    float cut_radius = 0.6; \n    float amplify_rate = 100.0; \n    float dist_x = vTexCoord[0] - trans_center_x; \n    float dist_y = vTexCoord[1] - trans_center_y; \n    float radius = sqrt(pow(dist_y*amplify_rate, 2.0) + pow(dist_x*amplify_rate, 2.0)); \n    float sin_angle = dist_y * amplify_rate / radius; \n    float cos_angle = dist_x * amplify_rate / radius; \n    radius = radius / amplify_rate; \n    float new_radius = pow(radius/cut_radius, 1.4) * cut_radius; \n    if(radius > cut_radius) { \n        new_radius = radius; \n    } \n    vec2 newCoord = vec2(trans_center_x + new_radius*cos_angle, trans_center_y + new_radius*sin_angle); \n    if(radius > cut_radius) { \n        newCoord = vTexCoord; \n    } \n    if (newCoord.x > 1.0 || newCoord.x < 0.0 || newCoord.y > 1.0 || newCoord.y < 0.0) { \n        return vec4(0.0, 0.0, 0.0, 1.0); \n    } else { \n        return texture2D(sTexture, newCoord); \n    } \n} \nvoid main() { \n    gl_FragColor = vec4(bigface().rgb, 1.0) * uAlpha; \n}";
    }
}
