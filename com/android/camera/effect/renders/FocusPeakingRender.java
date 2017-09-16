package com.android.camera.effect.renders;

import android.os.SystemProperties;
import com.android.camera.effect.draw_mode.DrawAttribute;
import com.android.gallery3d.ui.GLCanvas;

public class FocusPeakingRender extends RenderGroup {
    static final float DEFAULT_THRESHOLD = (((float) SystemProperties.getInt("camera_peaking_mf_threshold", 15)) / 100.0f);
    public FocusPeakingFirstPassRender mFirstPassRender;
    public PipeRenderPair mFocusPeakingRender;
    public NoneEffectRender mNoneEffectRender;
    public FocusPeakingSecondPassRender mSecondPassRender;

    public FocusPeakingRender(GLCanvas canvas) {
        super(canvas);
        this.mNoneEffectRender = new NoneEffectRender(canvas);
        this.mFirstPassRender = new FocusPeakingFirstPassRender(canvas);
        this.mSecondPassRender = new FocusPeakingSecondPassRender(canvas);
        this.mFocusPeakingRender = new PipeRenderPair(canvas, this.mFirstPassRender, this.mSecondPassRender, false);
        addRender(this.mNoneEffectRender);
        addRender(this.mFocusPeakingRender);
    }

    public FocusPeakingRender(GLCanvas canvas, int id) {
        this(canvas);
        this.mId = id;
    }

    public boolean draw(DrawAttribute attri) {
        return this.mNoneEffectRender.draw(attri) | this.mFocusPeakingRender.draw(attri);
    }
}
