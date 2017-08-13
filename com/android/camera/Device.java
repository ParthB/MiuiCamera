package com.android.camera;

import android.os.Build.VERSION;
import java.util.ArrayList;
import miui.os.Build;
import miui.util.FeatureParser;

public class Device {
    public static final boolean IS_A1 = "gemini".equals(Build.DEVICE);
    public static final boolean IS_A10 = "aqua".equals(Build.DEVICE);
    public static final boolean IS_A12 = "land".equals(Build.DEVICE);
    public static final boolean IS_A13 = "santoni".equals(Build.DEVICE);
    public static final boolean IS_A1Pro = "gold".equals(Build.DEVICE);
    public static final boolean IS_A4 = "scorpio".equals(Build.DEVICE);
    public static final boolean IS_A7 = "capricorn".equals(Build.DEVICE);
    public static final boolean IS_A8 = "lithium".equals(Build.DEVICE);
    public static final boolean IS_A9 = "ido".equals(Build.DEVICE);
    public static final boolean IS_B3 = "hydrogen".equals(Build.DEVICE);
    public static final boolean IS_B3_PRO = "helium".equals(Build.DEVICE);
    public static final boolean IS_B5 = Build.DEVICE.startsWith("mark");
    public static final boolean IS_B6 = Build.DEVICE.startsWith("nike");
    public static final boolean IS_B7 = "natrium".equals(Build.DEVICE);
    public static final boolean IS_C1 = "sagit".equals(Build.DEVICE);
    public static final boolean IS_C2 = "centaur".equals(Build.DEVICE);
    public static final boolean IS_C2Q = "achilles".equals(Build.DEVICE);
    public static final boolean IS_C3A = "rolex".equals(Build.DEVICE);
    public static final boolean IS_C5 = Build.DEVICE.startsWith("prada");
    public static final boolean IS_C6 = Build.DEVICE.startsWith("mido");
    public static final boolean IS_C8 = "jason".equals(Build.DEVICE);
    public static final boolean IS_CM = Build.IS_CM_CUSTOMIZATION;
    public static final boolean IS_CM_TEST = Build.IS_CM_CUSTOMIZATION_TEST;
    public static final boolean IS_D2 = "tiffany".equals(Build.DEVICE);
    public static final boolean IS_D3 = "ulysse".equals(Build.DEVICE);
    public static final boolean IS_D4 = "oxygen".equals(Build.DEVICE);
    public static final boolean IS_D5 = "chiron".equals(Build.DEVICE);
    public static final boolean IS_D6S = "ugg".equals(Build.DEVICE);
    public static final boolean IS_E7 = "vince".equals(Build.DEVICE);
    public static final boolean IS_H2XLTE;
    public static final boolean IS_H2X_LC = Build.IS_HONGMI_TWOX_LC;
    public static final boolean IS_H3C = "omega".equals(Build.DEVICE);
    public static final boolean IS_HM;
    public static final boolean IS_HM2;
    public static final boolean IS_HM2A = Build.IS_HONGMI_TWO_A;
    public static final boolean IS_HM2S = Build.IS_HONGMI_TWO_S;
    public static final boolean IS_HM2S_LTE = Build.IS_HONGMI_TWOS_LTE_MTK;
    public static final boolean IS_HM3 = Build.IS_HONGMI_THREE;
    public static final boolean IS_HM3A = "kenzo".equals(Build.DEVICE);
    public static final boolean IS_HM3B = "kate".equals(Build.DEVICE);
    public static final boolean IS_HM3LTE = "dior".equals(Build.DEVICE);
    public static final boolean IS_HM3X = "gucci".equals(Build.DEVICE);
    public static final boolean IS_HM3Y = "hermes".equals(Build.DEVICE);
    public static final boolean IS_HM3Z = "hennessy".equals(Build.DEVICE);
    public static final boolean IS_HONGMI = FeatureParser.getBoolean("is_hongmi", false);
    public static final boolean IS_MI2 = Build.IS_MITWO;
    public static final boolean IS_MI2A = Build.IS_MI2A;
    public static final boolean IS_MI3;
    public static final boolean IS_MI3TD = "pisces".equals(Build.DEVICE);
    public static final boolean IS_MI3W = ("cancro".equals(Build.DEVICE) ? Build.MODEL.startsWith("MI 3") : false);
    public static final boolean IS_MI4 = Build.IS_MIFOUR;
    public static final boolean IS_NEXUS5 = "hammerhead".equals(Build.DEVICE);
    public static final boolean IS_PAD1 = Build.IS_MIPAD;
    public static final boolean IS_STABLE = Build.IS_STABLE_VERSION;
    public static final boolean IS_X11 = "libra".equals(Build.DEVICE);
    public static final boolean IS_X5 = Build.IS_MIFIVE;
    public static final boolean IS_X7 = "leo".equals(Build.DEVICE);
    public static final boolean IS_X9 = "ferrari".equals(Build.DEVICE);
    public static final boolean IS_XIAOMI = FeatureParser.getBoolean("is_xiaomi", false);
    public static final String MODULE = Build.MODEL;
    private static ArrayList<String> sFpNavEventNameList;

    static {
        boolean z;
        boolean z2 = true;
        if (IS_MI3W) {
            z = true;
        } else {
            z = IS_MI3TD;
        }
        IS_MI3 = z;
        z = (!Build.IS_HONGMI_TWO || Build.IS_HONGMI_TWO_A || Build.IS_HONGMI_TWO_S) ? false : true;
        IS_HM = z;
        if (IS_HM) {
            z = true;
        } else {
            z = IS_HM2S;
        }
        IS_HM2 = z;
        if (!Build.IS_HONGMI_TWOX) {
            z2 = "HM2014816".equals(Build.DEVICE);
        }
        IS_H2XLTE = z2;
    }

    public static int getBurstShootCount() {
        return FeatureParser.getInteger("burst_shoot_count", 20);
    }

    public static boolean isSupportedMovieSolid() {
        return FeatureParser.getBoolean("support_camera_movie_solid", false);
    }

    public static boolean isSupportedDynamicEffectPopup() {
        return !FeatureParser.getBoolean("is_camera_use_still_effect_image", false);
    }

    public static boolean isNeedForceRecycleEffectPopup() {
        return (IS_H2X_LC || IS_MI3TD) ? true : IS_C8;
    }

    public static boolean isSupportedShaderEffect() {
        return FeatureParser.getBoolean("support_camera_shader_effect", false);
    }

    public static boolean isSupportedMuteCameraSound() {
        return !IS_CM;
    }

    public static boolean isSupportedLongPressBurst() {
        return FeatureParser.getBoolean("support_camera_burst_shoot", false);
    }

    public static boolean isSupportedSkinBeautify() {
        return FeatureParser.getBoolean("support_camera_skin_beauty", false);
    }

    public static boolean isSupportedIntelligentBeautify() {
        return FeatureParser.getBoolean("support_camera_age_detection", false);
    }

    public static boolean isSupportedGPS() {
        return FeatureParser.getBoolean("support_camera_record_location", false);
    }

    public static boolean is18x9RatioScreen() {
        return !IS_D5 ? IS_E7 : true;
    }

    public static boolean isSupportedTimeWaterMark() {
        return FeatureParser.getBoolean("support_camera_water_mark", false);
    }

    public static boolean isSupportedNewStyleTimeWaterMark() {
        return FeatureParser.getBoolean("support_camera_new_style_time_water_mark", false);
    }

    public static boolean isSupportedFaceInfoWaterMark() {
        return FeatureParser.getBoolean("support_camera_face_info_water_mark", false);
    }

    public static boolean isSupportedVideoPause() {
        return FeatureParser.getBoolean("support_camera_video_pause", false);
    }

    public static boolean adjustScreenLight() {
        return !IS_CM_TEST ? FeatureParser.getBoolean("support_camera_boost_brightness", false) : false;
    }

    public static boolean isLowerEffectSize() {
        return FeatureParser.getBoolean("is_lower_size_effect", false);
    }

    public static boolean isQcomPlatform() {
        return "qcom".equals(FeatureParser.getString("vendor"));
    }

    public static boolean isNvPlatform() {
        return "nvidia".equals(FeatureParser.getString("vendor"));
    }

    public static boolean isLCPlatform() {
        return "leadcore".equals(FeatureParser.getString("vendor"));
    }

    public static boolean isMTKPlatform() {
        return "mediatek".equals(FeatureParser.getString("vendor"));
    }

    public static boolean isSupportedSecondaryStorage() {
        return FeatureParser.getBoolean("support_dual_sd_card", false);
    }

    public static boolean isResetToCCAFAfterCapture() {
        return false;
    }

    public static boolean isSupportedAoHDR() {
        return FeatureParser.getBoolean("support_camera_aohdr", false);
    }

    public static boolean isSupportedHFR() {
        return FeatureParser.getBoolean("support_camera_hfr", false);
    }

    public static boolean isSupportedChromaFlash() {
        return FeatureParser.getBoolean("support_chroma_flash", false);
    }

    public static boolean isSupportedObjectTrack() {
        return FeatureParser.getBoolean("support_object_track", false);
    }

    public static boolean isSupportedVideoQuality4kUHD() {
        return FeatureParser.getBoolean("support_camera_4k_quality", false);
    }

    public static boolean isSupportedUbiFocus() {
        return FeatureParser.getBoolean("support_camera_ubifocus", false);
    }

    public static boolean isSupportedAsdFlash() {
        if ((FeatureParser.getInteger("camera_supported_asd", 0) & 1) != 0) {
            return true;
        }
        return false;
    }

    public static boolean isSupportedASD() {
        if ((FeatureParser.getInteger("camera_supported_asd", 0) & 15) != 0) {
            return true;
        }
        return false;
    }

    public static boolean isSupportedAsdHdr() {
        if ((FeatureParser.getInteger("camera_supported_asd", 0) & 2) != 0) {
            return true;
        }
        return false;
    }

    public static boolean isSupportedAsdMotion() {
        if ((FeatureParser.getInteger("camera_supported_asd", 0) & 4) != 0) {
            return true;
        }
        return false;
    }

    public static boolean isSupportedTeleAsdNight() {
        return IS_D2 ? isSupportedAsdNight() : false;
    }

    public static boolean isSupportedAsdNight() {
        if ((FeatureParser.getInteger("camera_supported_asd", 0) & 8) != 0) {
            return true;
        }
        return false;
    }

    public static boolean isSupportedManualFunction() {
        return FeatureParser.getBoolean("support_camera_manual_function", false);
    }

    public static boolean isSupportedAudioFocus() {
        return FeatureParser.getBoolean("support_camera_audio_focus", false);
    }

    public static boolean isUsedMorphoLib() {
        return FeatureParser.getBoolean("is_camera_use_morpho_lib", false);
    }

    public static boolean isFrontVideoQualityShouldBe1080P() {
        return (IS_HM2A || IS_HM3LTE || IS_H2X_LC || IS_H2XLTE || IS_MI3W || IS_HM3X || IS_HM3 || IS_HM || IS_HM2S || IS_HM2S_LTE || IS_MI2 || IS_MI2A || IS_MI3) ? false : true;
    }

    public static boolean isSupportedFastCapture() {
        return false;
    }

    public static boolean isSupportedTorchCapture() {
        return FeatureParser.getBoolean("support_camera_torch_capture", false);
    }

    public static boolean isFloatExposureTime() {
        return isQcomPlatform() && 21 <= VERSION.SDK_INT;
    }

    public static boolean isHDRFreeze() {
        return FeatureParser.getBoolean("is_camera_freeze_after_hdr_capture", false);
    }

    public static boolean isFaceDetectNeedRotation() {
        return FeatureParser.getBoolean("is_camera_face_detection_need_orientation", false);
    }

    public static boolean isThirdDevice() {
        return (IS_XIAOMI || IS_HONGMI) ? false : true;
    }

    public static boolean isCaptureStopFaceDetection() {
        return !IS_HM3Y ? IS_HM3Z : true;
    }

    public static boolean isHoldBlurBackground() {
        return FeatureParser.getBoolean("is_camera_hold_blur_background", false);
    }

    public static boolean isSupportedPeakingMF() {
        return FeatureParser.getBoolean("support_camera_peaking_mf", false);
    }

    public static boolean isVideoSnapshotSizeLimited() {
        return (IS_HM2A || IS_HM3LTE || IS_H2X_LC || IS_H2XLTE || IS_MI3W || IS_HM3X || IS_HM3 || IS_HM || IS_HM2S || IS_HM2S_LTE || IS_MI2 || IS_MI2A || IS_MI3 || IS_MI4 || IS_X5 || IS_X9) ? false : true;
    }

    public static boolean isSurfaceSizeLimited() {
        return IS_X7;
    }

    public static boolean shouldRestartPreviewAfterZslSwitch() {
        return IS_MI2 && !IS_MI2A;
    }

    public static boolean isSupportGradienter() {
        return FeatureParser.getBoolean("support_camera_gradienter", false);
    }

    public static boolean isLowPowerQRScan() {
        return FeatureParser.getBoolean("is_camera_lower_qrscan_frequency", false);
    }

    public static boolean isSubthreadFrameListerner() {
        return FeatureParser.getBoolean("is_camera_preview_with_subthread_looper", false);
    }

    public static boolean isMDPRender() {
        return false;
    }

    public static boolean isEffectWatermarkFilted() {
        return FeatureParser.getBoolean("is_camera_app_water_mark", false);
    }

    public static boolean isSupportedEdgeTouch() {
        return FeatureParser.getBoolean("support_edge_handgrip", false);
    }

    public static boolean isSupportedTiltShift() {
        return FeatureParser.getBoolean("support_camera_tilt_shift", false);
    }

    public static boolean isSupportedQuickSnap() {
        return FeatureParser.getBoolean("support_camera_quick_snap", false);
    }

    public static boolean isPad() {
        return FeatureParser.getBoolean("is_pad", false);
    }

    public static String getContinuousShotCallbackClass() {
        return FeatureParser.getString("camera_continuous_shot_callback_class");
    }

    public static String getContinuousShotCallbackSetter() {
        return FeatureParser.getString("camera_continuous_shot_callback_setter");
    }

    public static boolean isHalDoesCafWhenFlashOn() {
        return !IS_HM3Y ? IS_HM3Z : true;
    }

    public static boolean isSupportedMagicMirror() {
        if (Build.IS_INTERNATIONAL_BUILD) {
            return false;
        }
        return FeatureParser.getBoolean("support_camera_magic_mirror", false);
    }

    public static boolean isReleaseLaterForGallery() {
        return true;
    }

    public static boolean isSupportSquare() {
        return FeatureParser.getBoolean("support_camera_square_mode", false);
    }

    public static boolean isNewHdrParamKeyUsed() {
        return (IS_A9 || IS_A10 || IS_X11 || IS_X7 || IS_MI3W || IS_MI4 || IS_X5 || IS_X9 || IS_H2XLTE || IS_HM2A || IS_HM3LTE || IS_HM3X) ? false : true;
    }

    public static boolean isPanoUsePreviewFrame() {
        return !FeatureParser.getBoolean("support_full_size_panorama", false);
    }

    public static boolean isHFRVideoCaptureSupported() {
        return (IS_B3 || IS_B3_PRO || isMTKPlatform()) ? false : true;
    }

    public static boolean isHFRVideoPauseSupported() {
        return FeatureParser.getBoolean("support_hfr_video_pause", true);
    }

    public static boolean isSupportedStereo() {
        return IS_H3C;
    }

    public static boolean isSupportedOpticalZoom() {
        return (IS_C1 || IS_D2) ? true : IS_C8;
    }

    public static boolean isSupportedPortrait() {
        return (IS_C1 || IS_D2) ? true : IS_C8;
    }

    public static boolean isOrientationIndicatorEnabled() {
        return false;
    }

    public static ArrayList<String> getFpNavEventNameList() {
        if (sFpNavEventNameList == null) {
            sFpNavEventNameList = new ArrayList();
            String[] strArray = FeatureParser.getStringArray("fp_nav_event_name_list");
            if (strArray != null) {
                for (String str : strArray) {
                    sFpNavEventNameList.add(str);
                }
            }
        }
        return sFpNavEventNameList;
    }

    public static boolean isSupportFullSizeEffect() {
        return FeatureParser.getBoolean("is_full_size_effect", false);
    }

    public static boolean isSupportBurstDenoise() {
        return (IS_XIAOMI || IS_B5) ? true : IS_A13;
    }

    public static boolean isSupportGroupShot() {
        return FeatureParser.getBoolean("support_camera_groupshot", false);
    }

    public static boolean isISPRotated() {
        return FeatureParser.getBoolean("is_camera_isp_rotated", true);
    }

    public static boolean isSupportFHDHFR() {
        return IS_A7;
    }

    public static boolean isFrontRemosicSensor() {
        return FeatureParser.getBoolean("is_front_remosic_sensor", false);
    }

    public static boolean isSupportFrontFlash() {
        return FeatureParser.getBoolean("support_front_flash", false);
    }

    public static boolean isSupportVideoFrontFlash() {
        return isSupportFrontFlash() ? FeatureParser.getBoolean("support_video_front_flash", true) : false;
    }
}
