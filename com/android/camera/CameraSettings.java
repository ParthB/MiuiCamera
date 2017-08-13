package com.android.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Rect;
import android.hardware.Camera.Area;
import android.hardware.Camera.Parameters;
import android.hardware.input.InputManager;
import android.media.CamcorderProfile;
import android.os.Build.VERSION;
import android.provider.Settings.System;
import android.support.v7.recyclerview.R;
import android.text.TextUtils;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;
import com.android.camera.effect.EffectController;
import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.hardware.QcomCameraProxy;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.preferences.ListPreference;
import com.android.camera.ui.V6ModulePicker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miui.reflect.Method;

public class CameraSettings {
    public static final int BOTTOM_CONTROL_HEIGHT = CameraAppImpl.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.bottom_control_height);
    public static final int LOCATION_DELAY_TIME;
    private static final int MMS_VIDEO_DURATION;
    public static final int SURFACE_LEFT_MARGIN_MDP_QUALITY_480P = CameraAppImpl.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.v6_surface_view_left_margin_mdp_render_quality_480p);
    public static final int SURFACE_LEFT_MARGIN_MDP_QUALITY_LOW = CameraAppImpl.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.v6_surface_view_left_margin_mdp_render_quality_low);
    public static final int TOP_CONTROL_HEIGHT = CameraAppImpl.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.bottom_control_upper_panel_height);
    public static final ChangeManager sCameraChangeManager = new ChangeManager();
    public static boolean sCroppedIfNeeded = false;
    private static boolean sEdgePhotoEnable = false;
    public static ArrayList<String> sRemindMode = new ArrayList();
    private static HashMap<String, String> sSceneToFlash = new HashMap(11);

    static {
        int i;
        if (CamcorderProfile.get(0) != null) {
            i = CamcorderProfile.get(0).duration;
        } else {
            i = 30;
        }
        MMS_VIDEO_DURATION = i;
        if (VERSION.SDK_INT < 24) {
            i = 0;
        } else {
            i = 600;
        }
        LOCATION_DELAY_TIME = i;
        sSceneToFlash.put("auto", null);
        sSceneToFlash.put("portrait", null);
        sSceneToFlash.put("landscape", "off");
        sSceneToFlash.put("sports", null);
        sSceneToFlash.put("night", "off");
        sSceneToFlash.put("night-portrait", "on");
        sSceneToFlash.put("beach", "off");
        sSceneToFlash.put("snow", "off");
        sSceneToFlash.put("sunset", "off");
        sSceneToFlash.put("fireworks", "off");
        sSceneToFlash.put("backlight", "off");
        sSceneToFlash.put("flowers", "off");
        sRemindMode.add("pref_camera_mode_settings_key");
        sRemindMode.add("pref_camera_magic_mirror_key");
        if (Device.isSupportGroupShot()) {
            sRemindMode.add("pref_camera_groupshot_mode_key");
        }
    }

    public static boolean isFrontCamera() {
        return CameraSettingPreferences.instance().isFrontCamera();
    }

    public static boolean isBackCamera() {
        return CameraSettingPreferences.instance().isBackCamera();
    }

    public static float getPreviewAspectRatio(int width, int height) {
        if (Math.abs((((double) width) / ((double) height)) - 1.3333333333333333d) <= Math.abs((((double) width) / ((double) height)) - 1.7777777777777777d)) {
            return 1.3333334f;
        }
        if (Math.abs((((double) width) / ((double) height)) - 1.7777777777777777d) > Math.abs((((double) width) / ((double) height)) - 2.0d)) {
            return 2.0f;
        }
        return 1.7777778f;
    }

    public static int getRenderAspectRatio(int width, int height) {
        if (V6ModulePicker.isCameraModule() && isSwitchOn("pref_camera_square_mode_key")) {
            return 2;
        }
        return getAspectRatio(width, height);
    }

    public static int getAspectRatio(int width, int height) {
        if (isNearRatio16_9(width, height)) {
            return 1;
        }
        if (isNearRatio18_9(width, height)) {
            return 3;
        }
        return 0;
    }

    public static boolean isAspectRatio4_3(int width, int height) {
        if (width < height) {
            int tmp = width;
            width = height;
            height = tmp;
        }
        if (Math.abs((((double) width) / ((double) height)) - 1.3333333333333333d) < 0.02d) {
            return true;
        }
        return false;
    }

    public static boolean isAspectRatio16_9(int width, int height) {
        if (width < height) {
            int tmp = width;
            width = height;
            height = tmp;
        }
        if (Math.abs((((double) width) / ((double) height)) - 1.7777777777777777d) < 0.02d) {
            return true;
        }
        return false;
    }

    public static boolean isAspectRatio18_9(int width, int height) {
        if (width < height) {
            int tmp = width;
            width = height;
            height = tmp;
        }
        if (Math.abs((((double) width) / ((double) height)) - 2.0d) < 0.02d) {
            return true;
        }
        return false;
    }

    public static int getStrictAspectRatio(int width, int height) {
        if (isAspectRatio16_9(width, height)) {
            return 1;
        }
        if (isAspectRatio4_3(width, height)) {
            return 0;
        }
        if (isAspectRatio1_1(width, height)) {
            return 2;
        }
        return -1;
    }

    public static boolean isNearAspectRatio(int width1, int height1, int width2, int height2) {
        return getAspectRatio(width1, height1) == getAspectRatio(width2, height2);
    }

    public static boolean isAspectRatio1_1(int width, int height) {
        return width == height;
    }

    public static boolean isNearRatio16_9(int width, int height) {
        if (width < height) {
            int tmp = width;
            width = height;
            height = tmp;
        }
        double ratio = ((double) width) / ((double) height);
        if ((Math.abs(ratio - 1.3333333333333333d) > Math.abs(ratio - 1.7777777777777777d) || Math.abs(ratio - 1.5d) < 0.02d) && Math.abs(ratio - 1.7777777777777777d) <= Math.abs(ratio - 2.0d)) {
            return true;
        }
        return false;
    }

    public static boolean isNearRatio18_9(int width, int height) {
        if (width < height) {
            int tmp = width;
            width = height;
            height = tmp;
        }
        double ratio = ((double) width) / ((double) height);
        if ((Math.abs(ratio - 1.3333333333333333d) > Math.abs(ratio - 1.7777777777777777d) || Math.abs(ratio - 1.5d) < 0.02d) && Math.abs(ratio - 1.7777777777777777d) > Math.abs(ratio - 2.0d)) {
            return true;
        }
        return false;
    }

    public static int getVideoQuality() {
        int maxHfrQuality = 6;
        if (isSwitchOn("pref_camera_stereo_mode_key")) {
            return 6;
        }
        int quality = getPreferVideoQuality();
        if (isSwitchOn("pref_video_speed_slow_key")) {
            if (!Device.isSupportFHDHFR()) {
                maxHfrQuality = 5;
            }
            if (quality > maxHfrQuality) {
                quality = maxHfrQuality;
            }
        }
        return quality;
    }

    public static int getPreferVideoQuality() {
        String defaultQuality = getString(getDefaultPreferenceId(R.string.pref_video_quality_default));
        int quality = Integer.parseInt(defaultQuality);
        if (CameraSettingPreferences.instance().contains("pref_video_quality_key")) {
            return Integer.parseInt(CameraSettingPreferences.instance().getString("pref_video_quality_key", defaultQuality));
        }
        if (!CamcorderProfile.hasProfile(getCameraId(), Integer.parseInt(defaultQuality))) {
            defaultQuality = Integer.toString(1);
        }
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.putString("pref_video_quality_key", defaultQuality);
        editor.apply();
        return Integer.parseInt(defaultQuality);
    }

    public static void setAutoExposure(CameraHardwareProxy proxy, Parameters parameters, String value) {
        if (value != null) {
            int weight;
            ArrayList<Area> meteringAreas = new ArrayList();
            Rect meteringRect = new Rect();
            if (value.equals(getString(R.string.pref_camera_autoexposure_value_spotmetering))) {
                meteringRect.left = -250;
                meteringRect.top = -250;
                meteringRect.right = 250;
                meteringRect.bottom = 250;
                weight = 1;
            } else if (value.equals(getString(R.string.pref_camera_autoexposure_value_centerweighted))) {
                meteringRect.left = 0;
                meteringRect.top = 0;
                meteringRect.right = 0;
                meteringRect.bottom = 0;
                weight = 0;
            } else {
                meteringRect.left = -1000;
                meteringRect.top = -1000;
                meteringRect.right = 1000;
                meteringRect.bottom = 1000;
                weight = 1;
            }
            meteringAreas.add(new Area(meteringRect, weight));
            proxy.setMeteringAreas(parameters, meteringAreas);
        }
    }

    public static String getString(int resId) {
        return CameraAppImpl.getAndroidContext().getString(resId);
    }

    public static void upgradeGlobalPreferences(CameraSettingPreferences combPref) {
        Editor editor = combPref.edit();
        if (combPref.getInt("pref_version_key", 1) < 1 && !combPref.getBoolean("pref_camera_first_use_hint_shown_key", true)) {
            editor.putBoolean("pref_camera_first_touch_toast_shown_key", false);
        }
        editor.putInt("pref_version_key", 1);
        editor.apply();
    }

    public static void upgradeLocalPreferences(SharedPreferences prefLocal) {
        Editor editor = prefLocal.edit();
        int version = prefLocal.getInt("pref_local_version_key", 0);
        if (version == 0) {
            version = 1;
        }
        editor.putInt("pref_local_version_key", version);
        editor.apply();
    }

    public static int readPreferredCameraId(SharedPreferences pref) {
        return Integer.parseInt(pref.getString("pref_camera_id_key", String.valueOf(CameraHolder.instance().getBackCameraId())));
    }

    public static int getCameraId() {
        return Integer.parseInt(CameraSettingPreferences.instance().getString("pref_camera_id_key", String.valueOf(CameraHolder.instance().getBackCameraId())));
    }

    public static void writePreferredCameraId(SharedPreferences pref, int cameraId) {
        Editor editor = pref.edit();
        editor.putString("pref_camera_id_key", Integer.toString(cameraId));
        editor.apply();
    }

    public static int readExposure(CameraSettingPreferences preferences) {
        String exposure = preferences.getString("pref_camera_exposure_key", "0");
        try {
            return Integer.parseInt(exposure);
        } catch (Exception e) {
            Log.e("CameraSettings", "Invalid exposure: " + exposure);
            return 0;
        }
    }

    public static void writeExposure(CameraSettingPreferences preferences, int value) {
        Editor editor = preferences.edit();
        editor.putString("pref_camera_exposure_key", Integer.toString(value));
        editor.apply();
    }

    public static void resetExposure() {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.remove("pref_camera_exposure_key");
        editor.apply();
    }

    public static int readZoom(CameraSettingPreferences preferences) {
        String zoom = preferences.getString("pref_camera_zoom_key", "0");
        try {
            return Integer.parseInt(zoom);
        } catch (Exception e) {
            Log.e("CameraSettings", "Invalid zoom: " + zoom);
            return 0;
        }
    }

    public static void writeZoom(CameraSettingPreferences preferences, int value) {
        Editor editor = preferences.edit();
        editor.putString("pref_camera_zoom_key", Integer.toString(value));
        editor.apply();
    }

    public static void resetZoom(CameraSettingPreferences preferences) {
        Editor editor = preferences.edit();
        editor.remove("pref_camera_zoom_key");
        editor.apply();
    }

    public static void restorePreferences(Context context, CameraSettingPreferences preferences) {
        int currentCameraId = readPreferredCameraId(preferences);
        Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
        upgradeGlobalPreferences(preferences);
        writePreferredCameraId(preferences, currentCameraId);
    }

    public static int get4kProfile() {
        if (Device.isSupportedVideoQuality4kUHD()) {
            return Integer.parseInt(getString(R.string.pref_video_quality_entry_value_4kuhd));
        }
        return -1;
    }

    public static boolean is4KHigherVideoQuality(int quality) {
        boolean z = false;
        if (!Device.isSupportedVideoQuality4kUHD()) {
            return false;
        }
        if (get4kProfile() <= quality) {
            z = true;
        }
        return z;
    }

    public static ArrayList<String> getSupportedVideoQuality(int cameraId) {
        ArrayList<String> supported = new ArrayList();
        int quality4k = get4kProfile();
        if (Device.isSupportedVideoQuality4kUHD() && CamcorderProfile.hasProfile(cameraId, quality4k)) {
            supported.add(Integer.toString(quality4k));
        }
        if (CamcorderProfile.hasProfile(cameraId, 6)) {
            supported.add(Integer.toString(6));
        }
        if (CamcorderProfile.hasProfile(cameraId, 5)) {
            supported.add(Integer.toString(5));
        }
        if (CamcorderProfile.hasProfile(cameraId, 4)) {
            supported.add(Integer.toString(4));
        }
        if (CamcorderProfile.hasProfile(cameraId, 11)) {
            supported.add(Integer.toString(11));
        }
        if (CamcorderProfile.hasProfile(cameraId, 10)) {
            supported.add(Integer.toString(10));
        }
        if (CamcorderProfile.hasProfile(cameraId, 9)) {
            supported.add(Integer.toString(9));
        }
        return supported;
    }

    public static long updateOpenCameraFailTimes() {
        Editor edit = CameraSettingPreferences.instance().edit();
        long openCameraFail = CameraSettingPreferences.instance().getLong("open_camera_fail_key", 0) + 1;
        edit.putLong("open_camera_fail_key", openCameraFail);
        edit.apply();
        return openCameraFail;
    }

    public static void resetOpenCameraFailTimes() {
        Editor edit = CameraSettingPreferences.instance().edit();
        edit.putLong("open_camera_fail_key", 0);
        edit.apply();
    }

    public static boolean isVideoPauseVisible() {
        CameraSettingPreferences preferences = CameraSettingPreferences.instance();
        if (!Device.isSupportedVideoPause() || !V6ModulePicker.isVideoModule()) {
            return false;
        }
        if (Device.isHFRVideoPauseSupported() || !"slow".equals(getVideoSpeed(preferences))) {
            return true;
        }
        return false;
    }

    public static boolean isVideoCaptureVisible() {
        CameraSettingPreferences preferences = CameraSettingPreferences.instance();
        if (!preferences.getBoolean("pref_video_captrue_ability_key", false) || ((Device.isMTKPlatform() && isSwitchOn("pref_camera_stereo_mode_key")) || (!Device.isHFRVideoCaptureSupported() && "slow".equals(getVideoSpeed(preferences))))) {
            return false;
        }
        if (!Device.isSupportedVideoQuality4kUHD()) {
            return true;
        }
        if ((Device.IS_MI4 || Device.IS_X5) && getVideoQuality() > 6) {
            return false;
        }
        return true;
    }

    public static int getDefaultPreferenceId(int prefId) {
        switch (prefId) {
            case R.bool.pref_camera_auto_chroma_flash_default:
                if (Device.IS_X5 || Device.IS_X7) {
                    return R.bool.pref_camera_auto_chroma_flash_virgo_default;
                }
            case R.string.pref_video_quality_default:
                if (CameraSettingPreferences.instance().isFrontCamera() && Device.isFrontVideoQualityShouldBe1080P()) {
                    return R.string.pref_mi_front_video_quality_default;
                }
            case R.string.pref_camera_antibanding_default:
                if (Util.isAntibanding60()) {
                    return R.string.pref_camera_antibanding_60;
                }
                break;
        }
        return prefId;
    }

    public static boolean isRecordLocation(SharedPreferences pref) {
        return pref.getBoolean("pref_camera_recordlocation_key", false) ? Device.isSupportedGPS() : false;
    }

    public static boolean isCameraSoundOpen(SharedPreferences pref) {
        return pref.getBoolean("pref_camerasound_key", true) || !Device.isSupportedMuteCameraSound();
    }

    public static boolean isTimeWaterMarkOpen(SharedPreferences pref) {
        return pref.getBoolean("pref_watermark_key", false);
    }

    public static void setDualCameraWaterMarkOpen(SharedPreferences pref, boolean open) {
        if (isSupportedOpticalZoom() && isBackCamera()) {
            pref.edit().putBoolean("pref_dualcamera_watermark", open).apply();
        }
    }

    public static boolean isDualCameraWaterMarkOpen(SharedPreferences pref) {
        if (isSupportedOpticalZoom() && isBackCamera()) {
            return pref.getBoolean("pref_dualcamera_watermark", CameraAppImpl.getAndroidContext().getResources().getBoolean(R.bool.pref_dualcamera_watermark_default));
        }
        return false;
    }

    public static boolean isProximityLockOpen(SharedPreferences pref) {
        return pref.getBoolean("pref_camera_proximity_lock_key", true);
    }

    public static boolean isFaceWaterMarkOpen(SharedPreferences pref) {
        return pref.getBoolean("pref_face_info_watermark_key", false);
    }

    public static String getFrontMirror(SharedPreferences pref) {
        return pref.getString("pref_front_mirror_key", getString(R.string.pref_front_mirror_default));
    }

    public static boolean isMovieSolidOn(SharedPreferences pref) {
        if (Device.isSupportedMovieSolid()) {
            return pref.getBoolean("pref_camera_movie_solid_key", Boolean.valueOf(getString(R.string.pref_camera_movie_solid_default)).booleanValue());
        }
        return false;
    }

    public static boolean isScanQRCode(Context context, SharedPreferences pref) {
        if (!pref.getBoolean("pref_scan_qrcode_key", Boolean.valueOf(getString(R.string.pref_scan_qrcode_default)).booleanValue()) || isFrontCamera()) {
            return false;
        }
        return isQRCodeReceiverAvailable(context);
    }

    public static boolean isQRCodeReceiverAvailable(Context context) {
        return Util.isPackageAvailable(context, "com.xiaomi.scanner");
    }

    public static boolean isBurstShootingEnable(SharedPreferences pref) {
        if (Device.isSupportedLongPressBurst()) {
            return "burst".equals(pref.getString("pref_camera_long_press_shutter_feature_key", getString(R.string.pref_camera_long_press_shutter_feature_default)));
        }
        return false;
    }

    private static void changeSurfaceViewFrameLayoutParams(ActivityBase activity, int index, int width, int height) {
        if (Device.isMDPRender() && V6ModulePicker.isVideoModule()) {
            FrameLayout previewPanel = activity.getUIController().getSurfaceViewFrame();
            LayoutParams p1 = (LayoutParams) previewPanel.getLayoutParams();
            if (index == 0) {
                int margin = (width <= 0 || height <= 0 || isAspectRatio4_3(width, height)) ? 0 : SURFACE_LEFT_MARGIN_MDP_QUALITY_LOW;
                p1.setMargins(margin, 0, margin, BOTTOM_CONTROL_HEIGHT);
            } else if (index == 2) {
                p1.setMargins(SURFACE_LEFT_MARGIN_MDP_QUALITY_480P, 0, SURFACE_LEFT_MARGIN_MDP_QUALITY_480P, 0);
            } else {
                p1.setMargins(0, 0, 0, 0);
            }
            previewPanel.requestLayout();
        }
    }

    private static void changePreviewPanelLayoutParams(ActivityBase activity, int index) {
        RelativeLayout previewPanel = activity.getUIController().getPreviewPanel();
        RelativeLayout.LayoutParams p1 = (RelativeLayout.LayoutParams) previewPanel.getLayoutParams();
        if (index == 0) {
            p1.setMargins(0, 0, 0, BOTTOM_CONTROL_HEIGHT);
            activity.getUIController().getBottomControlPanel().setBackgroundVisible(false);
        } else if (index == 1) {
            int navigationBarHeight;
            if (Util.checkDeviceHasNavigationBar(activity)) {
                navigationBarHeight = Util.getNavigationBarHeight(activity);
            } else {
                navigationBarHeight = 0;
            }
            p1.setMargins(0, 0, 0, navigationBarHeight);
            activity.getUIController().getBottomControlPanel().setBackgroundVisible(true);
        } else {
            p1.setMargins(0, 0, 0, 0);
            activity.getUIController().getBottomControlPanel().setBackgroundVisible(true);
        }
        previewPanel.requestLayout();
    }

    private static void changePreviewFrameLayoutParams(ActivityBase activity, int index) {
        RelativeLayout previewFrame = activity.getUIController().getPreviewFrame();
        RelativeLayout.LayoutParams p1 = (RelativeLayout.LayoutParams) previewFrame.getLayoutParams();
        if (index == 0) {
            p1.setMargins(0, (Util.sWindowHeight - BOTTOM_CONTROL_HEIGHT) - ((Util.sWindowWidth * 4) / 3), 0, 0);
        } else if (index == 1) {
            p1.setMargins(0, (Util.sWindowHeight - (Util.checkDeviceHasNavigationBar(activity) ? Util.getNavigationBarHeight(activity) : 0)) - ((Util.sWindowWidth * 16) / 9), 0, 0);
        } else {
            p1.setMargins(0, 0, 0, 0);
        }
        previewFrame.requestLayout();
    }

    private static void changeSettingStatusBarLayoutParams(ActivityBase activity, int index) {
        RelativeLayout statusBar = activity.getUIController().getSettingsStatusBar();
        RelativeLayout.LayoutParams p1 = (RelativeLayout.LayoutParams) statusBar.getLayoutParams();
        if (index == 0) {
            p1.setMargins(0, 0, 0, BOTTOM_CONTROL_HEIGHT);
        } else {
            p1.setMargins(0, 0, 0, 0);
        }
        statusBar.requestLayout();
    }

    public static int getUIStyleByPreview(int width, int height) {
        if (Device.isPad()) {
            return 0;
        }
        if (sCroppedIfNeeded) {
            return 1;
        }
        int index = 0;
        double ratio = ((double) width) / ((double) height);
        if (Device.isMDPRender() && Math.abs(ratio - 1.5d) < 0.02d) {
            index = 2;
        } else if (Math.abs(ratio - 1.3333333333333333d) > Math.abs(ratio - 1.7777777777777777d) || Math.abs(ratio - 1.5d) < 0.02d) {
            if (Math.abs(ratio - 1.7777777777777777d) > Math.abs(ratio - 2.0d)) {
                index = 3;
            } else {
                index = 1;
            }
        }
        return index;
    }

    public static void changeUIByPreviewSize(ActivityBase activity, int index) {
        changeUIByPreviewSize(activity, index, -1, -1);
    }

    public static void changeUIByPreviewSize(ActivityBase activity, int index, int width, int height) {
        if (!Device.isPad()) {
            changeSettingStatusBarLayoutParams(activity, index);
            changePreviewPanelLayoutParams(activity, index);
            changePreviewFrameLayoutParams(activity, index);
            changeSurfaceViewFrameLayoutParams(activity, index, width, height);
        }
    }

    public static int getExitText(String key) {
        if ("pref_camera_coloreffect_key".equals(key) || "pref_camera_shader_coloreffect_key".equals(key)) {
            return R.string.simple_mode_button_text_color_effect;
        }
        if ("pref_camera_hand_night_key".equals(key)) {
            return R.string.simple_mode_button_text_hand_night;
        }
        if ("pref_camera_panoramamode_key".equals(key)) {
            return R.string.simple_mode_button_text_panorama;
        }
        if ("pref_video_speed_key".equals(key)) {
            return R.string.simple_mode_button_text_slow_video;
        }
        if ("pref_camera_face_beauty_mode_key".equals(key)) {
            return R.string.simple_mode_button_text_face_beauty;
        }
        if ("pref_delay_capture_mode".equals(key)) {
            return R.string.simple_mode_button_text_delay_capture;
        }
        if ("pref_video_speed_fast_key".equals(key)) {
            return R.string.simple_mode_button_text_slow_video;
        }
        if ("pref_video_speed_slow_key".equals(key)) {
            return R.string.simple_mode_button_text_fast_video;
        }
        if ("pref_camera_ubifocus_key".equals(key)) {
            return R.string.simple_mode_button_text_ubifocus;
        }
        if ("pref_camera_manual_mode_key".equals(key)) {
            return R.string.simple_mode_button_text_manual;
        }
        if ("pref_camera_burst_shooting_key".equals(key)) {
            return R.string.burst_shoot_exit_button_text;
        }
        if ("pref_audio_focus_mode_key".equals(key)) {
            return R.string.audio_focus_exit_button_text;
        }
        if ("pref_camera_scenemode_setting_key".equals(key)) {
            return R.string.simple_mode_button_text_scene;
        }
        if ("pref_camera_gradienter_key".equals(key)) {
            return R.string.simple_mode_button_text_gradienter;
        }
        if ("pref_camera_tilt_shift_mode".equals(key)) {
            return R.string.simple_mode_button_text_tilt_shift;
        }
        if ("pref_camera_magic_mirror_key".equals(key)) {
            return R.string.simple_mode_button_text_magic_mirror;
        }
        if ("pref_audio_capture".equals(key)) {
            return R.string.simple_mode_button_text_audio_capture;
        }
        if ("pref_camera_stereo_mode_key".equals(key)) {
            return R.string.simple_mode_button_text_stereo_mode;
        }
        if ("pref_camera_square_mode_key".equals(key)) {
            return R.string.simple_mode_button_text_square;
        }
        if ("pref_camera_groupshot_mode_key".equals(key)) {
            return R.string.simple_mode_button_text_groupshot;
        }
        return -1;
    }

    public static void resetSettingsNoNeedToSave(CameraSettingPreferences preferences, int cameraId) {
        Object value;
        Editor editor = preferences.edit();
        editor.remove("pref_camera_exposure_key");
        editor.remove("pref_camera_coloreffect_key");
        editor.remove("pref_camera_shader_coloreffect_key");
        editor.remove("pref_camera_focus_mode_key");
        editor.remove("pref_camera_hand_night_key");
        editor.remove("pref_camera_scenemode_key");
        editor.remove("pref_camera_scenemode_setting_key");
        editor.remove("pref_video_speed_key");
        editor.remove("pref_video_hdr_key");
        editor.remove("pref_camera_face_beauty_key");
        editor.remove("pref_camera_face_beauty_mode_key");
        editor.remove("pref_camera_id_key");
        editor.remove("pref_delay_capture_mode");
        editor.remove("pref_delay_capture_key");
        editor.remove("pref_audio_capture");
        editor.remove("pref_video_speed_fast_key");
        editor.remove("pref_video_speed_slow_key");
        editor.remove("pref_camera_ubifocus_key");
        editor.remove("pref_camera_manual_mode_key");
        editor.remove("pref_camera_panoramamode_key");
        editor.remove("pref_camera_burst_shooting_key");
        editor.remove("pref_audio_focus_mode_key");
        editor.remove("pref_camera_gradienter_key");
        editor.remove("pref_camera_tilt_shift_mode");
        editor.remove("pref_camera_magic_mirror_key");
        editor.remove("pref_camera_stereo_mode_key");
        editor.remove("pref_camera_groupshot_mode_key");
        editor.remove("pref_camera_zoom_key");
        editor.remove("pref_camera_zoom_mode_key");
        editor.remove("pref_camera_portrait_mode_key");
        editor.remove("pref_camera_square_mode_key");
        Map<String, ?> preferenceMap = preferences.getAll();
        for (String key : Arrays.asList(new String[]{"pref_camerasound_key", "pref_scan_qrcode_key", "pref_watermark_key", "pref_camera_referenceline_key", "pref_camera_facedetection_key", "pref_camera_movie_solid_key"})) {
            value = preferenceMap.get(key);
            if (value != null && (value instanceof String)) {
                editor.remove(key);
                if (!"pref_camera_facedetection_key".equals(key) || !Device.isThirdDevice()) {
                    editor.putBoolean(key, "on".equalsIgnoreCase((String) value));
                }
            }
        }
        value = preferenceMap.get("pref_video_quality_key");
        if (value != null && (value instanceof String)) {
            if (is4KHigherVideoQuality(Integer.parseInt((String) value))) {
                editor.remove("pref_video_quality_key");
            } else if (!getSupportedVideoQuality(cameraId).contains(value)) {
                Log.d("CameraSettings", "Remove unsupported video quality " + value + " for camera " + cameraId);
                editor.remove("pref_video_quality_key");
            }
        }
        filterPreference(preferenceMap, "pref_camera_skin_beautify_key", editor, R.array.pref_camera_skin_beautify_entryvalues);
        filterPreference(preferenceMap, "pref_qc_camera_saturation_key", editor, R.array.pref_camera_saturation_entryvalues);
        filterPreference(preferenceMap, "pref_qc_camera_contrast_key", editor, R.array.pref_camera_contrast_entryvalues);
        filterPreference(preferenceMap, "pref_qc_camera_sharpness_key", editor, R.array.pref_camera_sharpness_entryvalues);
        filterPreference(preferenceMap, "pref_video_quality_key", editor, R.array.pref_video_quality_entryvalues);
        value = preferenceMap.get("pref_front_mirror_key");
        if (!(value == null || (value instanceof String))) {
            editor.remove("pref_front_mirror_key");
        }
        value = preferenceMap.get("pref_camera_restored_flashmode_key");
        if (value != null && (value instanceof String)) {
            editor.putString("pref_camera_flashmode_key", (String) value);
            editor.remove("pref_camera_restored_flashmode_key");
        }
        value = preferenceMap.get("pref_camera_hdr_key");
        if (!(value == null || "auto".equals(value) || "off".equals(value))) {
            editor.remove("pref_camera_hdr_key");
        }
        if (!Device.isSupportedManualFunction()) {
            editor.remove("pref_focus_position_key");
            editor.remove("pref_qc_camera_exposuretime_key");
        }
        if (preferenceMap.get("pref_camera_confirm_location_shown_key") == null) {
            Object location = preferenceMap.get("pref_camera_recordlocation_key");
            if (Device.isSupportedGPS() && (location == null || ((location instanceof Boolean) && !((Boolean) location).booleanValue()))) {
                editor.remove("pref_camera_first_use_hint_shown_key");
                editor.remove("pref_camera_recordlocation_key");
            }
            editor.putBoolean("pref_camera_confirm_location_shown_key", false);
        }
        editor.apply();
    }

    private static void filterPreference(Map<String, ?> preferenceMap, String key, Editor editor, int valuesId) {
        if (editor != null && !TextUtils.isEmpty(key) && valuesId != 0) {
            Object value = preferenceMap.get(key);
            if (value != null && !isStringValueContained(value, valuesId)) {
                editor.remove(key);
            }
        }
    }

    private static boolean isStringValueContained(Object checkedValue, int entryValueId) {
        return isStringValueContains(checkedValue, CameraAppImpl.getAndroidContext().getResources().getStringArray(entryValueId));
    }

    public static boolean isStringValueContains(Object checkedValue, CharSequence[] values) {
        if (values == null || checkedValue == null) {
            return false;
        }
        for (CharSequence beautyValue : values) {
            if (beautyValue.equals(checkedValue)) {
                return true;
            }
        }
        return false;
    }

    public static void resetPreference(String key) {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.remove(key);
        editor.apply();
    }

    public static boolean isSwitchOn(String key) {
        return "on".equals(CameraSettingPreferences.instance().getString(key, "off"));
    }

    public static void setKValue(int value) {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.putInt("pref_qc_manual_whitebalance_k_value_key", value);
        editor.apply();
    }

    public static int getKValue() {
        return CameraSettingPreferences.instance().getInt("pref_qc_manual_whitebalance_k_value_key", 5500);
    }

    public static void setSmartShutterPosition(String value) {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.putString("pref_key_camera_smart_shutter_position", value);
        editor.apply();
    }

    public static String getSmartShutterPosition() {
        return CameraSettingPreferences.instance().getString("pref_key_camera_smart_shutter_position", "");
    }

    public static void setFocusMode(String mode) {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.putString("pref_camera_focus_mode_key", mode);
        editor.apply();
    }

    public static String getFocusMode() {
        return CameraSettingPreferences.instance().getString("pref_camera_focus_mode_key", getString(R.string.pref_camera_focusmode_value_default));
    }

    public static void setFocusModeSwitching(boolean value) {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.putBoolean("pref_qc_focus_mode_switching_key", value);
        editor.apply();
    }

    public static boolean isAudioCaptureOpen() {
        return isSwitchOn("pref_audio_capture");
    }

    public static boolean isFocusModeSwitching() {
        return CameraSettingPreferences.instance().getBoolean("pref_qc_focus_mode_switching_key", false);
    }

    public static void updateFocusMode() {
        String oldFocusMode = getFocusMode();
        String focusMode = (!isSwitchOn("pref_camera_manual_mode_key") || getFocusPosition() == 1000) ? "continuous-picture" : "manual";
        if (!focusMode.equals(oldFocusMode)) {
            setFocusModeSwitching(true);
            setFocusMode(focusMode);
        }
    }

    public static void setFocusPosition(int value) {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.putString("pref_focus_position_key", String.valueOf(value));
        editor.apply();
    }

    public static void setShaderEffect(int value) {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.putString("pref_camera_shader_coloreffect_key", String.valueOf(value));
        editor.apply();
    }

    public static int getShaderEffect() {
        if (isSwitchOn("pref_camera_gradienter_key")) {
            return EffectController.sGradienterIndex;
        }
        if (isSwitchOn("pref_camera_tilt_shift_mode")) {
            String mode = CameraSettingPreferences.instance().getString("pref_camera_tilt_shift_key", getString(R.string.pref_camera_tilt_shift_default));
            if (mode.equals(getString(R.string.pref_camera_tilt_shift_entryvalue_circle))) {
                return EffectController.sGaussianIndex;
            }
            if (mode.equals(getString(R.string.pref_camera_tilt_shift_entryvalue_parallel))) {
                return EffectController.sTiltShiftIndex;
            }
        } else if (isSwitchOn("pref_camera_magic_mirror_key")) {
            return 0;
        }
        try {
            return Integer.parseInt(CameraSettingPreferences.instance().getString("pref_camera_shader_coloreffect_key", getString(R.string.pref_camera_shader_coloreffect_default)));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static List<Integer> getShaderEffectPositionList() {
        int i;
        String[] effectNames = EffectController.getInstance().getEntryNames();
        Map<String, Integer> effectNameIndexMap = new HashMap();
        for (i = 0; i < effectNames.length; i++) {
            effectNameIndexMap.put(effectNames[i], Integer.valueOf(i));
        }
        String[] savedOrder = CameraSettingPreferences.instance().getString("pref_camera_shader_coloreffect_order_key", "").split(", ");
        Map<String, Integer> savedNamePositionMap = new HashMap();
        int index = 0;
        for (i = 0; i < savedOrder.length; i++) {
            if (effectNameIndexMap.containsKey(savedOrder[i])) {
                int index2 = index + 1;
                savedNamePositionMap.put(savedOrder[i], Integer.valueOf(index));
                index = index2;
            }
        }
        savedOrder = new String[savedNamePositionMap.size()];
        for (String name : savedNamePositionMap.keySet()) {
            savedOrder[((Integer) savedNamePositionMap.get(name)).intValue()] = name;
        }
        Integer[] positions = new Integer[effectNames.length];
        int skip = 0;
        int offset = 0;
        for (i = 0; i < effectNames.length; i++) {
            if (savedNamePositionMap.containsKey(effectNames[i])) {
                positions[((Integer) effectNameIndexMap.get(savedOrder[offset])).intValue()] = Integer.valueOf(((Integer) savedNamePositionMap.get(savedOrder[offset])).intValue() + skip);
                offset++;
            } else {
                positions[i] = Integer.valueOf(i);
                skip++;
            }
        }
        return Arrays.asList(positions);
    }

    public static void saveShaderEffectPositionList(List<Integer> positionList) {
        int index = 0;
        int[] effectPositionIndexMap = new int[positionList.size()];
        for (Integer position : positionList) {
            int index2 = index + 1;
            effectPositionIndexMap[position.intValue()] = index;
            index = index2;
        }
        String[] effectNames = EffectController.getInstance().getEntryNames();
        StringBuilder orderBuilder = new StringBuilder();
        for (int i = 0; i < effectPositionIndexMap.length; i++) {
            orderBuilder.append(effectNames[effectPositionIndexMap[i]]);
            if (i < effectPositionIndexMap.length - 1) {
                orderBuilder.append(", ");
            }
        }
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.putString("pref_camera_shader_coloreffect_order_key", orderBuilder.toString());
        editor.apply();
    }

    public static int getFocusPosition() {
        return Integer.parseInt(CameraSettingPreferences.instance().getString("pref_focus_position_key", String.valueOf(1000)));
    }

    public static String getManualFocusName(Context context, int value) {
        if (value == 1000) {
            return context.getString(R.string.pref_camera_focusmode_entry_auto);
        }
        if (((double) value) >= 600.0d) {
            return context.getString(R.string.pref_camera_focusmode_entry_macro);
        }
        if (((double) value) >= 200.0d) {
            return context.getString(R.string.pref_camera_focusmode_entry_normal);
        }
        return context.getString(R.string.pref_camera_focusmode_entry_infinity);
    }

    public static String getSkinBeautifyHumanReadableValue(Context context, ListPreference preference) {
        int index = preference.findIndexOfValue(preference.getValue());
        int length = preference.getEntryValues().length;
        if (index > (length * 2) / 3) {
            return context.getString(R.string.pref_camera_beautify_entry_high);
        }
        if (index > length / 3) {
            return context.getString(R.string.pref_camera_beautify_entry_normal);
        }
        if (index > 0) {
            return context.getString(R.string.pref_camera_beautify_entry_low);
        }
        return context.getString(R.string.pref_camera_beautify_entry_close);
    }

    public static boolean isFaceBeautyOn(String value) {
        return (value == null || value.equals(getString(R.string.pref_face_beauty_close))) ? false : true;
    }

    public static String getFlashModeByScene(String sceneMode) {
        return (String) sSceneToFlash.get(sceneMode);
    }

    public static int getCountDownTimes() {
        return Integer.parseInt(CameraSettingPreferences.instance().getString("pref_delay_capture_key", getString(R.string.pref_camera_delay_capture_default)));
    }

    public static int getMinExposureTimes(Context context) {
        if (Device.IS_XIAOMI && Device.isQcomPlatform()) {
            return ((QcomCameraProxy) CameraHardwareProxy.getDeviceProxy()).getMinExposureTimeValue(CameraManager.instance().getStashParameters());
        }
        return 0;
    }

    public static String getFaceBeautifyValue() {
        String faceBeauty = getString(R.string.pref_face_beauty_close);
        if (!Device.isSupportedSkinBeautify()) {
            return faceBeauty;
        }
        String defaultBeauty = getString(R.string.pref_face_beauty_default);
        if (isFrontCamera()) {
            faceBeauty = CameraSettingPreferences.instance().getString("pref_camera_face_beauty_key", defaultBeauty);
            String beautySwitch = CameraSettingPreferences.instance().getString("pref_camera_face_beauty_switch_key", "pref_camera_face_beauty_key");
            if (beautySwitch.equals("pref_camera_face_beauty_advanced_key")) {
                return getString(R.string.pref_face_beauty_advanced);
            }
            if (beautySwitch.equals(getString(R.string.pref_face_beauty_close))) {
                return getString(R.string.pref_face_beauty_close);
            }
            return faceBeauty;
        } else if (isSwitchOn("pref_camera_face_beauty_mode_key")) {
            return CameraSettingPreferences.instance().getString("pref_camera_face_beauty_key", defaultBeauty);
        } else {
            return faceBeauty;
        }
    }

    public static String getBeautifyDetailValue(String key) {
        int resId = 0;
        if ("pref_skin_beautify_skin_color_key".equals(key)) {
            resId = R.string.pref_skin_beautify_color_default;
        } else if ("pref_skin_beautify_slim_face_key".equals(key)) {
            resId = R.string.pref_skin_beautify_slim_default;
        } else if ("pref_skin_beautify_skin_smooth_key".equals(key)) {
            resId = R.string.pref_skin_beautify_smooth_default;
        } else if ("pref_skin_beautify_enlarge_eye_key".equals(key)) {
            resId = R.string.pref_skin_beautify_eye_default;
        }
        String beautyValue = "0";
        if (resId == 0 || !Device.isSupportedSkinBeautify()) {
            return beautyValue;
        }
        if (!isFrontCamera() && !isSwitchOn("pref_camera_face_beauty_mode_key")) {
            return beautyValue;
        }
        return CameraSettingPreferences.instance().getString(key, getString(resId));
    }

    public static int getMaxExposureTimes(Context context) {
        if (Device.IS_XIAOMI && Device.isQcomPlatform()) {
            return ((QcomCameraProxy) CameraHardwareProxy.getDeviceProxy()).getMaxExposureTimeValue(CameraManager.instance().getStashParameters());
        }
        return 0;
    }

    public static int[] getMaxPreviewFpsRange(Parameters params) {
        List<int[]> frameRates = params.getSupportedPreviewFpsRange();
        if (frameRates == null || frameRates.size() <= 0) {
            return new int[0];
        }
        return (int[]) frameRates.get(frameRates.size() - 1);
    }

    public static void setPriorityStoragePreference(boolean externalPriority) {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.putBoolean("pref_priority_storage", externalPriority);
        editor.apply();
    }

    public static int[] getPhotoPreviewFpsRange(Parameters params) {
        List<int[]> frameRates = params.getSupportedPreviewFpsRange();
        if (frameRates == null || frameRates.isEmpty()) {
            Log.e("CameraSettings", "No supported frame rates returned!");
            return null;
        }
        int lowestMinRate = 400000;
        for (int[] rate : frameRates) {
            int[] rate2;
            int minFps = rate2[0];
            if (rate2[1] >= 30000 && minFps <= 30000 && minFps < lowestMinRate) {
                lowestMinRate = minFps;
            }
        }
        int resultIndex = -1;
        int highestMaxRate = 0;
        for (int i = 0; i < frameRates.size(); i++) {
            rate2 = (int[]) frameRates.get(i);
            minFps = rate2[0];
            int maxFps = rate2[1];
            if (minFps == lowestMinRate && highestMaxRate < maxFps) {
                highestMaxRate = maxFps;
                resultIndex = i;
            }
        }
        if (resultIndex >= 0) {
            return (int[]) frameRates.get(resultIndex);
        }
        Log.e("CameraSettings", "Can't find an appropriate frame rate range!");
        return null;
    }

    public static boolean isPressDownCapture() {
        String defaultValue = getString(R.string.pref_camera_long_press_shutter_feature_default);
        if (!Device.isSupportedFastCapture()) {
            return false;
        }
        if (isFrontCamera() || !"focus".equals(CameraSettingPreferences.instance().getString("pref_camera_long_press_shutter_feature_key", defaultValue))) {
            return true;
        }
        return false;
    }

    public static boolean isAsdPopupEnable() {
        return Device.isSupportedAsdFlash() ? CameraSettingPreferences.instance().getBoolean("pref_camera_asd_popup_key", Boolean.valueOf(getString(R.bool.pref_camera_asd_popup_default)).booleanValue()) : false;
    }

    public static boolean isAsdNightEnable() {
        return Device.isSupportedAsdNight() ? CameraSettingPreferences.instance().getBoolean("pref_camera_asd_night_key", Boolean.valueOf(getString(R.bool.pref_camera_asd_night_default)).booleanValue()) : false;
    }

    public static boolean isAsdMotionEnable() {
        return Device.isSupportedAsdMotion() ? CameraSettingPreferences.instance().getBoolean("pref_camera_asd_night_key", Boolean.valueOf(getString(R.bool.pref_camera_asd_night_default)).booleanValue()) : false;
    }

    public static boolean isNoCameraModeSelected(Context context) {
        for (String key : ((ActivityBase) context).getCurrentModule().getSupportedSettingKeys()) {
            if (isSwitchOn(key)) {
                return false;
            }
        }
        return true;
    }

    public static String getVideoSpeed(CameraSettingPreferences preference) {
        if ("on".equals(preference.getString("pref_video_speed_fast_key", "off"))) {
            return "fast";
        }
        if ("on".equals(preference.getString("pref_video_speed_slow_key", "off"))) {
            return "slow";
        }
        return "normal";
    }

    public static boolean isEdgePhotoEnable() {
        return Device.isSupportedEdgeTouch() ? sEdgePhotoEnable : false;
    }

    public static int getSystemEdgeMode(Context context) {
        if (Device.isSupportedEdgeTouch() && (((System.getInt(context.getContentResolver(), "edge_handgrip", 0) | System.getInt(context.getContentResolver(), "edge_handgrip_clean", 0)) | System.getInt(context.getContentResolver(), "edge_handgrip_back", 0)) | System.getInt(context.getContentResolver(), "edge_handgrip_screenshot", 0)) == 1) {
            return 2;
        }
        return 0;
    }

    public static void readEdgePhotoSetting(Context context) {
        boolean z = true;
        if (Device.isSupportedEdgeTouch()) {
            if (System.getInt(context.getContentResolver(), "edge_handgrip_photo", 0) != 1) {
                z = false;
            }
            sEdgePhotoEnable = z;
        }
    }

    public static void setEdgeMode(Context context, boolean enable) {
        int i = 1;
        if (context != null) {
            if (enable) {
                readEdgePhotoSetting(context);
            }
            if (isEdgePhotoEnable()) {
                InputManager im = (InputManager) context.getSystemService("input");
                Class<?>[] ownerClazz = new Class[]{InputManager.class};
                Method method = Util.getMethod(ownerClazz, "switchTouchEdgeMode", "(I)V");
                if (method != null) {
                    Class cls = ownerClazz[0];
                    Object[] objArr = new Object[1];
                    if (!enable) {
                        i = getSystemEdgeMode(context);
                    }
                    objArr[0] = Integer.valueOf(i);
                    method.invoke(cls, im, objArr);
                }
            }
        }
    }

    public static String getJpegQuality(CameraSettingPreferences preference, boolean burst) {
        String jpegQuality = preference.getString("pref_camera_jpegquality_key", getString(R.string.pref_camera_jpegquality_default));
        String maxQuality = "high";
        if (burst && Device.IS_HM3LTE) {
            maxQuality = "normal";
        } else if (burst && Device.IS_HONGMI) {
            maxQuality = "low";
        }
        if (JpegEncodingQualityMappings.getQualityNumber(jpegQuality) < JpegEncodingQualityMappings.getQualityNumber(maxQuality)) {
            return jpegQuality;
        }
        return maxQuality;
    }

    public static boolean isDualCameraHintShown(CameraSettingPreferences preference) {
        boolean z = true;
        if (!Device.IS_H3C) {
            return true;
        }
        int shownTimes = preference.getInt("pref_dual_camera_use_hint_shown_times_key", 0);
        Editor editor = preference.edit();
        editor.putInt("pref_dual_camera_use_hint_shown_times_key", shownTimes + 1);
        editor.apply();
        if (shownTimes >= 5) {
            z = false;
        }
        return z;
    }

    public static boolean isNeedFrontCameraFirstUseHint(CameraSettingPreferences preferences) {
        boolean defaultValue = Device.IS_A8 || Device.IS_D5;
        return preferences.getBoolean("pref_front_camera_first_use_hint_shown_key", defaultValue);
    }

    public static void cancelFrontCameraFirstUseHint(CameraSettingPreferences preferences) {
        Editor editor = preferences.edit();
        editor.putBoolean("pref_front_camera_first_use_hint_shown_key", false);
        editor.apply();
    }

    public static boolean isNeedRemind(String key) {
        if (sRemindMode.contains(key)) {
            return CameraSettingPreferences.instance().getBoolean(key + "_remind", true);
        }
        return false;
    }

    public static void cancelRemind(String key) {
        if (isNeedRemind(key)) {
            Editor editor = CameraSettingPreferences.instance().edit();
            editor.putBoolean(key + "_remind", false);
            editor.apply();
        }
    }

    public static String getMiuiSettingsKeyForStreetSnap(String snapValue) {
        if (snapValue.equals(getString(getDefaultPreferenceId(R.string.pref_camera_snap_value_take_picture)))) {
            return "Street-snap-picture";
        }
        if (snapValue.equals(getString(getDefaultPreferenceId(R.string.pref_camera_snap_value_take_movie)))) {
            return "Street-snap-movie";
        }
        return "none";
    }

    public static boolean isSwitchCameraZoomMode() {
        return !V6ModulePicker.isPanoramaModule() ? isSwitchOn("pref_camera_manual_mode_key") : true;
    }

    public static String getCameraZoomMode() {
        return CameraSettingPreferences.instance().getString("pref_camera_zoom_mode_key", getString(R.string.pref_camera_zoom_mode_default));
    }

    public static void resetCameraZoomMode() {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.remove("pref_camera_zoom_mode_key");
        editor.apply();
    }

    public static boolean isCameraPortraitWithFaceBeautyOptionVisible() {
        return CameraSettingPreferences.instance().getBoolean("pref_camera_portrait_with_facebeauty_key_visible", false);
    }

    public static void setCameraPortraitWithFaceBeautyOptionVisible(boolean visible) {
        Editor editor = CameraSettingPreferences.instance().edit();
        editor.putBoolean("pref_camera_portrait_with_facebeauty_key_visible", visible);
        editor.apply();
    }

    public static boolean isCameraPortraitWithFaceBeauty() {
        return CameraSettingPreferences.instance().getBoolean("pref_camera_portrait_with_facebeauty_key", CameraAppImpl.getAndroidContext().getResources().getBoolean(R.bool.pref_camera_portrait_with_facebeauty_default));
    }

    public static boolean isSupportedOpticalZoom() {
        return Device.isSupportedOpticalZoom() ? CameraHolder.instance().hasAuxCamera() : false;
    }

    public static boolean isSupportedPortrait() {
        return Device.isSupportedPortrait() ? CameraHolder.instance().hasAuxCamera() : false;
    }

    public static boolean isSupportedMetadata() {
        return !Device.isSupportedASD() ? isSupportedPortrait() : true;
    }
}
