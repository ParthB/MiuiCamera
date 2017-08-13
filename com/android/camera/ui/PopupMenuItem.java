package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class PopupMenuItem extends RelativeLayout implements Rotatable {
    private final float DISABLED_ALPHA = 0.4f;

    public PopupMenuItem(Context context) {
        super(context);
    }

    public PopupMenuItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public PopupMenuItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setOrientation(int orientation, boolean animation) {
        orientation = -orientation;
        int rotation = (int) getRotation();
        int deltaR = (orientation >= 0 ? orientation % 360 : (orientation % 360) + 360) - (rotation >= 0 ? rotation % 360 : (rotation % 360) + 360);
        if (deltaR == 0) {
            animate().cancel();
            return;
        }
        if (Math.abs(deltaR) > 180) {
            if (deltaR >= 0) {
                deltaR -= 360;
            } else {
                deltaR += 360;
            }
        }
        if (animation) {
            animate().withLayer().rotation((float) (rotation + deltaR)).setDuration((long) ((Math.abs(deltaR) * 1000) / 270));
        } else {
            animate().withLayer().rotation((float) (rotation + deltaR)).setDuration(0);
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            setAlpha(1.0f);
        } else {
            setAlpha(0.4f);
        }
    }
}
