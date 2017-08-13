package com.android.camera;

import com.android.camera.camera_adapter.CameraMTK.FBLevel;
import com.android.camera.camera_adapter.CameraMTK.FBParams;
import com.android.camera.hardware.CameraHardwareProxy.CameraHardwareFace;

public class MtkFBParamsUtil {
    private static final StringBuilder ADJUSTMENTS = new StringBuilder();
    private static final int[] BASE_VALUES = new int[]{-10, -11, -7, -12, -8, -8, -2, -12, -4, 0, 0, -9};

    static {
        ADJUSTMENTS.append("1200,1200,1201,1201,1200,1210,").append("2301,2411,2412,2412,2311,2421,").append("3411,3522,3623,3623,3512,3622,").append("1200,1211,1311,1311,1211,1311,").append("2301,2512,2522,2522,2412,2522,").append("3511,3723,3734,3734,3623,3733,");
    }

    public static void getAdvancedValue(FBParams params) {
        params.skinColor = Integer.parseInt(CameraSettings.getBeautifyDetailValue("pref_skin_beautify_skin_color_key"));
        params.smoothLevel = Integer.parseInt(CameraSettings.getBeautifyDetailValue("pref_skin_beautify_skin_smooth_key"));
        params.slimFace = Integer.parseInt(CameraSettings.getBeautifyDetailValue("pref_skin_beautify_slim_face_key"));
        params.enlargeEye = Integer.parseInt(CameraSettings.getBeautifyDetailValue("pref_skin_beautify_enlarge_eye_key"));
    }

    public static void getIntelligentValue(FBParams params, FBLevel level, CameraHardwareFace face) {
        getBaseValue(params, level);
        adjustValue(params, level, face);
    }

    private static void getBaseValue(FBParams params, FBLevel level) {
        if (params != null) {
            int baseIndex = level.ordinal() * 4;
            params.skinColor = BASE_VALUES[baseIndex];
            params.smoothLevel = BASE_VALUES[baseIndex + 1];
            params.slimFace = BASE_VALUES[baseIndex + 2];
            params.enlargeEye = BASE_VALUES[baseIndex + 3];
        }
    }

    private static void adjustValue(FBParams params, FBLevel level, CameraHardwareFace face) {
        int i = 0;
        if (params != null && face != null) {
            int genderIndex = getGenderIndex(face.gender);
            if (genderIndex != 2) {
                float age = genderIndex == 0 ? face.ageMale : face.ageFemale;
                if (genderIndex != 0) {
                    i = 1;
                }
                int start = (((i * 5) * 6) * 3) + ((level.ordinal() * 5) * 6);
                int refStart = start + 10;
                start += getAgeIndex(age) * 5;
                int start2 = start + 1;
                int refStart2 = refStart + 1;
                params.skinColor = trimValue((ADJUSTMENTS.charAt(start) - ADJUSTMENTS.charAt(refStart)) + params.skinColor);
                start = start2 + 1;
                refStart = refStart2 + 1;
                params.smoothLevel = trimValue((ADJUSTMENTS.charAt(start2) - ADJUSTMENTS.charAt(refStart2)) + params.smoothLevel);
                start2 = start + 1;
                refStart2 = refStart + 1;
                params.slimFace = trimValue((ADJUSTMENTS.charAt(start) - ADJUSTMENTS.charAt(refStart)) + params.slimFace);
                start = start2 + 1;
                refStart = refStart2 + 1;
                params.enlargeEye = trimValue((ADJUSTMENTS.charAt(start2) - ADJUSTMENTS.charAt(refStart2)) + params.enlargeEye);
            }
        }
    }

    private static int trimValue(int value) {
        if (value < -12) {
            return -12;
        }
        if (value > 12) {
            return 12;
        }
        return value;
    }

    private static int getAgeIndex(float age) {
        if (age <= 7.0f) {
            return 0;
        }
        if (age <= 17.0f) {
            return 1;
        }
        if (age <= 30.0f) {
            return 2;
        }
        if (age <= 44.0f) {
            return 3;
        }
        if (age <= 60.0f) {
            return 4;
        }
        return 5;
    }

    private static int getGenderIndex(float gender) {
        if (gender < 0.4f) {
            return 1;
        }
        if (gender > 0.6f) {
            return 0;
        }
        return 2;
    }
}
