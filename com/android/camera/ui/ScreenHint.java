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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.android.camera.ActivityBase;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.LocationManager;
import com.android.camera.OnScreenHint;
import com.android.camera.RotateDialogController;
import com.android.camera.permission.PermissionManager;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.storage.Storage;
import miui.view.animation.CubicEaseInOutInterpolator;

public class ScreenHint {
    private static final CubicEaseInOutInterpolator sCubicEaseInOutInterpolator = new CubicEaseInOutInterpolator();
    private final Activity mActivity;
    private boolean mIsShowingFrontCameraFirstUseHint;
    private boolean mIsShowingPortraitUseHint;
    private OnScreenHint mStorageHint;
    private long mStorageSpace;
    private AnimatorListener mUseHintAnimatorListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            final View hintView = (View) ScreenHint.this.mUseHintHideAnimator.getTarget();
            if (animation == ScreenHint.this.mUseHintShowAnimator) {
                hintView.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        if (hintView == ((ActivityBase) ScreenHint.this.mActivity).getUIController().getPortraitUseHintView()) {
                            ScreenHint.this.dismissPortraitUseHint();
                        } else if (hintView == ((ActivityBase) ScreenHint.this.mActivity).getUIController().getFrontCameraHintView()) {
                            ScreenHint.this.dismissFrontCameraFirstUseHint();
                        }
                    }
                });
            } else if (animation == ScreenHint.this.mUseHintHideAnimator) {
                hintView.setAlpha(1.0f);
                hintView.setVisibility(8);
            }
        }
    };
    private ObjectAnimator mUseHintHideAnimator;
    private AnimatorSet mUseHintShowAnimator;

    public ScreenHint(Activity activity) {
        this.mActivity = activity;
    }

    private void createUseHintAnimator(ViewGroup hintView) {
        Object childAt;
        ObjectAnimator.ofInt(hintView.getBackground(), "alpha", new int[]{0, 255}).setDuration(300);
        if (hintView.getChildCount() > 0) {
            childAt = hintView.getChildAt(0);
        } else {
            ViewGroup viewGroup = hintView;
        }
        Animator useHintLayoutShowAnimator = ObjectAnimator.ofFloat(childAt, "alpha", new float[]{0.0f, 1.0f});
        useHintLayoutShowAnimator.setStartDelay(50);
        useHintLayoutShowAnimator.setDuration(250);
        this.mUseHintShowAnimator = new AnimatorSet();
        this.mUseHintShowAnimator.playTogether(new Animator[]{useHintBackgroundShowAnimator, useHintLayoutShowAnimator});
        this.mUseHintShowAnimator.setInterpolator(sCubicEaseInOutInterpolator);
        this.mUseHintShowAnimator.addListener(this.mUseHintAnimatorListener);
        this.mUseHintHideAnimator = ObjectAnimator.ofFloat(hintView, "alpha", new float[]{1.0f, 0.0f});
        this.mUseHintHideAnimator.setDuration(400);
        this.mUseHintHideAnimator.setInterpolator(sCubicEaseInOutInterpolator);
        this.mUseHintHideAnimator.addListener(this.mUseHintAnimatorListener);
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
        if (((ActivityBase) this.mActivity).getUIController().getPortraitButton().isVisible()) {
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
        createUseHintAnimator((ViewGroup) ((ActivityBase) this.mActivity).getUIController().getPortraitUseHintView());
        ((ActivityBase) this.mActivity).getUIController().getPortraitUseHintView().findViewById(R.id.portrait_use_hint_layout).setAlpha(0.0f);
        ((ActivityBase) this.mActivity).getUIController().getPortraitUseHintView().setVisibility(0);
        this.mUseHintShowAnimator.start();
        this.mIsShowingPortraitUseHint = true;
    }

    public boolean isShowingPortraitUseHint() {
        return this.mIsShowingPortraitUseHint;
    }

    public void dismissPortraitUseHint() {
        ((ActivityBase) this.mActivity).getUIController().getPortraitUseHintView().setOnClickListener(null);
        this.mUseHintHideAnimator.start();
        this.mIsShowingPortraitUseHint = false;
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
            } else if (firstPortraitHint && ((ActivityBase) this.mActivity).getUIController().getPortraitButton().isVisible()) {
                showPortraitUseHint();
            }
        }
    }

    public void showFrontCameraFirstUseHint() {
        View hintView = ((ActivityBase) this.mActivity).getUIController().getFrontCameraHintView();
        createUseHintAnimator((ViewGroup) hintView);
        hintView.findViewById(R.id.front_camera_hint_layout).setAlpha(0.0f);
        hintView.setVisibility(0);
        this.mUseHintShowAnimator.start();
        ((AnimationDrawable) hintView.findViewById(R.id.front_camera_hint_animation).getBackground()).start();
        this.mIsShowingFrontCameraFirstUseHint = true;
    }

    public boolean isShowingFrontCameraFirstUseHint() {
        return this.mIsShowingFrontCameraFirstUseHint;
    }

    public void dismissFrontCameraFirstUseHint() {
        ((ActivityBase) this.mActivity).getUIController().getFrontCameraHintView().setOnClickListener(null);
        this.mUseHintHideAnimator.start();
        this.mIsShowingFrontCameraFirstUseHint = false;
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
