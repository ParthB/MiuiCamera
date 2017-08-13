package com.android.camera.preferences;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import com.android.camera.CameraSettings;

public class QRCodePreference extends CheckBoxPreference {
    public QRCodePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public QRCodePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public QRCodePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public QRCodePreference(Context context) {
        super(context);
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        View checkBoxView = view.findViewById(16908289);
        if (checkBoxView != null) {
            checkBoxView.setEnabled(CameraSettings.isQRCodeReceiverAvailable(getContext()));
        }
    }

    protected boolean callChangeListener(Object newValue) {
        if (!Boolean.TRUE.equals(newValue) || CameraSettings.isQRCodeReceiverAvailable(getContext())) {
            return super.callChangeListener(newValue);
        }
        return false;
    }
}
