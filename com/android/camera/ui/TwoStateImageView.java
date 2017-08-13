package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TwoStateImageView extends ImageView {
    private final float DISABLED_ALPHA;
    private boolean mFilterEnabled;
    private boolean mFilterInPressState;

    public TwoStateImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.DISABLED_ALPHA = 0.4f;
        this.mFilterEnabled = false;
        this.mFilterInPressState = true;
    }

    public TwoStateImageView(Context context) {
        this(context, null);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!this.mFilterEnabled) {
            return;
        }
        if (enabled) {
            setAlpha(1.0f);
        } else {
            setAlpha(0.4f);
        }
    }

    public void setPressed(boolean pressed) {
        if (this.mFilterInPressState) {
            if (!this.mFilterInPressState) {
                return;
            }
            if (!isEnabled() && pressed) {
                return;
            }
        }
        super.setPressed(pressed);
    }

    public void enableFilter(boolean enabled) {
        this.mFilterEnabled = enabled;
    }

    public void enablePressFilter(boolean enabled) {
        this.mFilterInPressState = enabled;
    }
}
