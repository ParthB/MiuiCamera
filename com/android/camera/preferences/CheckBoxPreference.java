package com.android.camera.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class CheckBoxPreference extends android.preference.CheckBoxPreference {
    public CheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CheckBoxPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 16842895);
    }

    public CheckBoxPreference(Context context) {
        this(context, null);
    }

    protected void onBindView(View view) {
        ((TextView) view.findViewById(16908310)).setSingleLine(false);
        super.onBindView(view);
    }
}
