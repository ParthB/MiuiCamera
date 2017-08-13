package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class TwoStateTextView extends TextView {
    private final float DISABLED_ALPHA;
    private boolean mFilterEnabled;

    public TwoStateTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.DISABLED_ALPHA = 0.4f;
        this.mFilterEnabled = true;
    }

    public TwoStateTextView(Context context) {
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
}
