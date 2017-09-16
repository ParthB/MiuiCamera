package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class TopPopupParent extends FrameLayout implements V6FunctionUI {
    public TopPopupParent(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onPreviewPageShown(boolean shown) {
        if (!shown) {
            dismissAllPopup(true);
        }
    }

    public void dismissAllPopup(boolean animate) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if ((v instanceof V6AbstractSettingPopup) && v.getVisibility() == 0) {
                dismissPopup((V6AbstractSettingPopup) v, animate);
            }
        }
    }

    private void dismissPopupExcept(View view, boolean animate) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (view != v && v.getVisibility() == 0 && (v instanceof V6AbstractSettingPopup)) {
                dismissPopup((V6AbstractSettingPopup) v, animate);
            }
        }
    }

    public void dismissAllPopupExceptSkinBeauty(boolean animate) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if ((v instanceof V6AbstractSettingPopup) && v.getVisibility() == 0) {
                V6AbstractSettingPopup popup = (V6AbstractSettingPopup) v;
                if (!"pref_camera_face_beauty_switch_key".equals(popup.getKey())) {
                    dismissPopup(popup, animate);
                }
            }
        }
    }

    public void dismissPopup(V6AbstractSettingPopup mPopup, boolean animate) {
        mPopup.dismiss(animate);
    }

    public void showPopup(V6AbstractSettingPopup mPopup, boolean animate) {
        dismissPopupExcept(mPopup, animate);
        mPopup.show(animate);
    }

    public void onCreate() {
    }

    public void onCameraOpen() {
        dismissAllPopup(false);
    }

    public void onResume() {
    }

    public void onPause() {
        dismissAllPopup(false);
    }

    public void enableControls(boolean enabled) {
    }

    public void setMessageDispacher(MessageDispacher p) {
    }
}
