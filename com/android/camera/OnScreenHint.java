package com.android.camera;

import android.app.Activity;
import android.support.v7.recyclerview.R;
import android.view.ViewGroup;
import android.widget.TextView;

public class OnScreenHint {
    private ViewGroup mHintView;

    public OnScreenHint(ViewGroup view) {
        this.mHintView = view;
    }

    public void show() {
        Util.fadeIn(this.mHintView);
    }

    public int getHintViewVisibility() {
        return this.mHintView.getVisibility();
    }

    public void cancel() {
        Util.fadeOut(this.mHintView);
    }

    public static OnScreenHint makeText(Activity activity, CharSequence text) {
        ViewGroup hintView = (ViewGroup) activity.findViewById(R.id.on_screen_hint);
        OnScreenHint result = new OnScreenHint(hintView);
        ((TextView) hintView.findViewById(R.id.message)).setText(text);
        return result;
    }

    public void setText(CharSequence s) {
        if (this.mHintView == null) {
            throw new RuntimeException("This OnScreenHint was not created with OnScreenHint.makeText()");
        }
        TextView tv = (TextView) this.mHintView.findViewById(R.id.message);
        if (tv == null) {
            throw new RuntimeException("This OnScreenHint was not created with OnScreenHint.makeText()");
        }
        tv.setText(s);
    }
}
