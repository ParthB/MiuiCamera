package com.android.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.AnimationDrawable;
import android.support.v7.recyclerview.R;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.PopupWindow;
import com.android.camera.ActivityBase;
import com.android.camera.Camera;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.LocationManager;
import com.android.camera.OnScreenHint;
import com.android.camera.RotateDialogController;
import com.android.camera.aosp_porting.animation.CubicEaseInOutInterpolator;
import com.android.camera.permission.PermissionManager;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.storage.Storage;

public class ScreenHint {
    private static final CubicEaseInOutInterpolator sCubicEaseInOutInterpolator = new CubicEaseInOutInterpolator();
    private final Activity mActivity;
    private PopupWindow mFrontCameraFirstUseHintPopup;
    private AnimatorListener mPortraitUseHintAnimatorListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            if (animation == ScreenHint.this.mPortraitUseHintShowAnimator) {
                ((ActivityBase) ScreenHint.this.mActivity).getUIController().getPortraitUseHintView().setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        ((ActivityBase) ScreenHint.this.mActivity).getUIController().getPortraitUseHintView().setOnClickListener(null);
                        ScreenHint.this.mPortraitUseHintHideAnimator.start();
                    }
                });
            } else if (animation == ScreenHint.this.mPortraitUseHintHideAnimator) {
                ((ActivityBase) ScreenHint.this.mActivity).getUIController().getPortraitUseHintView().setAlpha(1.0f);
                ((ActivityBase) ScreenHint.this.mActivity).getUIController().getPortraitUseHintView().setVisibility(8);
            }
        }
    };
    private Animator mPortraitUseHintHideAnimator;
    private AnimatorSet mPortraitUseHintShowAnimator;
    private OnScreenHint mStorageHint;
    private long mStorageSpace;

    public ScreenHint(Activity activity) {
        this.mActivity = activity;
    }

    private void initPortraitUseHintAnimator() {
        if (this.mPortraitUseHintShowAnimator == null) {
            ObjectAnimator.ofInt(((ActivityBase) this.mActivity).getUIController().getPortraitUseHintView().getBackground(), "alpha", new int[]{0, 216}).setDuration(300);
            Animator portraitUseHintLayoutShowAnimator = ObjectAnimator.ofFloat(((ActivityBase) this.mActivity).getUIController().getPortraitUseHintView().findViewById(R.id.portrait_use_hint_layout), "alpha", new float[]{0.0f, 1.0f});
            portraitUseHintLayoutShowAnimator.setStartDelay(50);
            portraitUseHintLayoutShowAnimator.setDuration(250);
            this.mPortraitUseHintShowAnimator = new AnimatorSet();
            this.mPortraitUseHintShowAnimator.playTogether(new Animator[]{portraitUseHintBackgroundShowAnimator, portraitUseHintLayoutShowAnimator});
            this.mPortraitUseHintShowAnimator.setInterpolator(sCubicEaseInOutInterpolator);
            this.mPortraitUseHintShowAnimator.addListener(this.mPortraitUseHintAnimatorListener);
        }
        if (this.mPortraitUseHintHideAnimator == null) {
            this.mPortraitUseHintHideAnimator = ObjectAnimator.ofFloat(((ActivityBase) this.mActivity).getUIController().getPortraitUseHintView(), "alpha", new float[]{1.0f, 0.0f});
            this.mPortraitUseHintHideAnimator.setDuration(400);
            this.mPortraitUseHintHideAnimator.setInterpolator(sCubicEaseInOutInterpolator);
            this.mPortraitUseHintHideAnimator.addListener(this.mPortraitUseHintAnimatorListener);
        }
    }

    public long getStorageSpace() {
        return Storage.getAvailableSpace();
    }

    public void updateHint() {
        Storage.switchStoragePathIfNeeded();
        this.mStorageSpace = Storage.getAvailableSpace();
        CharSequence message = null;
        if (this.mStorageSpace == -1) {
            message = this.mActivity.getString(R.string.no_storage);
        } else if (this.mStorageSpace == -2) {
            message = this.mActivity.getString(R.string.preparing_sd);
        } else if (this.mStorageSpace == -3) {
            message = this.mActivity.getString(R.string.access_sd_fail);
        } else if (this.mStorageSpace < 52428800) {
            if (Storage.isPhoneStoragePriority()) {
                message = this.mActivity.getString(R.string.spaceIsLow_content_primary_storage_priority);
            } else {
                message = this.mActivity.getString(R.string.spaceIsLow_content_external_storage_priority);
            }
        }
        if (message != null) {
            if (this.mStorageHint == null) {
                this.mStorageHint = OnScreenHint.makeText(this.mActivity, message);
            } else {
                this.mStorageHint.setText(message);
            }
            this.mStorageHint.show();
        } else if (this.mStorageHint != null) {
            this.mStorageHint.cancel();
            this.mStorageHint = null;
        }
    }

    public boolean isScreenHintVisible() {
        return this.mStorageHint != null && this.mStorageHint.getHintViewVisibility() == 0;
    }

    public void cancelHint() {
        if (this.mStorageHint != null) {
            this.mStorageHint.cancel();
            this.mStorageHint = null;
        }
    }

    private void recordLocation(boolean recorded) {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.putBoolean("pref_camera_recordlocation_key", recorded);
        editor.apply();
        LocationManager.instance().recordLocation(recorded);
        if (V6ModulePicker.isCameraModule() && CameraSettings.isSupportedPortrait() && CameraSettings.isBackCamera()) {
            showPortraitUseHint();
        }
    }

    public void showObjectTrackHint(CameraSettingPreferences preferences) {
        Editor editor = preferences.edit();
        editor.putBoolean("pref_camera_first_tap_screen_hint_shown_key", false);
        editor.apply();
        RotateTextToast.getInstance(this.mActivity).show(R.string.object_track_enable_toast, 0);
    }

    private void showPortraitUseHint() {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.putBoolean("pref_camera_first_portrait_use_hint_shown_key", false);
        editor.apply();
        initPortraitUseHintAnimator();
        ((ActivityBase) this.mActivity).getUIController().getPortraitUseHintView().findViewById(R.id.portrait_use_hint_layout).setAlpha(0.0f);
        ((ActivityBase) this.mActivity).getUIController().getPortraitUseHintView().setVisibility(0);
        this.mPortraitUseHintShowAnimator.start();
    }

    public void showFirstUseHint() {
        CameraSettingPreferences preferences = CameraSettingPreferences.instance();
        boolean firstLocation = preferences.getBoolean("pref_camera_first_use_hint_shown_key", true);
        if (PermissionManager.checkCameraLocationPermissions()) {
            Editor editor = preferences.edit();
            editor.putBoolean("pref_camera_first_use_hint_shown_key", false);
            editor.putBoolean("pref_camera_confirm_location_shown_key", false);
            editor.apply();
        } else {
            firstLocation = false;
        }
        boolean firstPortraitHint = preferences.getBoolean("pref_camera_first_portrait_use_hint_shown_key", CameraSettings.isSupportedPortrait());
        if (firstLocation || firstPortraitHint) {
            boolean containsRecordLocation = preferences.contains("pref_camera_recordlocation_key");
            if (Device.isSupportedGPS() && !containsRecordLocation && firstLocation) {
                RotateDialogController.showSystemChoiceDialog(this.mActivity, this.mActivity.getString(R.string.confirm_location_title), this.mActivity.getString(R.string.confirm_location_message), this.mActivity.getString(R.string.confirm_location_alert), this.mActivity.getString(R.string.start_capture), new Runnable() {
                    public void run() {
                        ScreenHint.this.recordLocation(true);
                    }
                }, new Runnable() {
                    public void run() {
                        ScreenHint.this.recordLocation(false);
                    }
                });
            } else if (firstPortraitHint && V6ModulePicker.isCameraModule() && CameraSettings.isBackCamera()) {
                showPortraitUseHint();
            }
        }
    }

    public void showFrontCameraFirstUseHintPopup() {
        if (this.mFrontCameraFirstUseHintPopup == null) {
            View popupView = View.inflate(this.mActivity, R.layout.front_camera_hint_popup, null);
            this.mFrontCameraFirstUseHintPopup = new PopupWindow(popupView, -2, -2, true);
            this.mFrontCameraFirstUseHintPopup.setTouchInterceptor(new OnTouchListener() {
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    int x = (int) motionEvent.getX();
                    int y = (int) motionEvent.getY();
                    if (x < 0 || x >= view.getWidth() || y < 0 || y >= view.getHeight()) {
                        return true;
                    }
                    return false;
                }
            });
            popupView.findViewById(R.id.front_camera_hint_text_confirm).setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    ScreenHint.this.mFrontCameraFirstUseHintPopup.dismiss();
                }
            });
            ((AnimationDrawable) popupView.findViewById(R.id.front_camera_hint_animation).getBackground()).start();
            this.mFrontCameraFirstUseHintPopup.showAtLocation(((Camera) this.mActivity).getUIController().getGLView(), 49, 0, this.mActivity.getResources().getDimensionPixelSize(R.dimen.front_camera_hint_popup_margin));
        }
    }

    public boolean isShowingFrontCameraFirstUseHintPopup() {
        return this.mFrontCameraFirstUseHintPopup != null ? this.mFrontCameraFirstUseHintPopup.isShowing() : false;
    }

    public void dismissFrontCameraFirstUseHintPopup() {
        if (this.mFrontCameraFirstUseHintPopup != null) {
            this.mFrontCameraFirstUseHintPopup.dismiss();
            this.mFrontCameraFirstUseHintPopup = null;
        }
    }

    public void showConfirmMessage(int title, int message) {
        RotateDialogController.showSystemAlertDialog(this.mActivity, this.mActivity.getString(title), this.mActivity.getString(message), this.mActivity.getString(17039370), null, null, null);
    }

    public void hideToast() {
        RotateTextToast toast = RotateTextToast.getInstance();
        if (toast != null) {
            toast.show(0, 0);
        }
    }
}
