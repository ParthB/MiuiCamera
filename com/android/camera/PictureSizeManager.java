package com.android.camera;

import android.hardware.Camera.Size;
import android.support.v7.recyclerview.R;
import com.android.camera.preferences.CameraSettingPreferences;
import java.util.ArrayList;
import java.util.List;

public class PictureSizeManager {
    private static String sDefaultValue = "4x3";
    private static final ArrayList<String> sEntryValues = new ArrayList();
    private static final ArrayList<PictureSize> sPictureList = new ArrayList();

    static {
        sEntryValues.add("4x3");
        sEntryValues.add("16x9");
    }

    public static String[] getEntries() {
        return new String[]{CameraSettings.getString(R.string.pref_camera_picturesize_entry_standard), CameraSettings.getString(R.string.pref_camera_picturesize_entry_fullscreen)};
    }

    public static String[] getEntryValues() {
        String[] result = new String[sEntryValues.size()];
        sEntryValues.toArray(result);
        return result;
    }

    public static String getDefaultValue() {
        return sDefaultValue;
    }

    public static PictureSize getPictureSize(boolean settingOnly) {
        if (settingOnly || !CameraSettings.isSwitchOn("pref_camera_square_mode_key")) {
            return new PictureSize(CameraSettingPreferences.instance().getString("pref_camera_picturesize_key", sDefaultValue));
        }
        return new PictureSize(1, 1);
    }

    public static PictureSize getBestPictureSize() {
        PictureSize candidate = getPictureSize(false);
        PictureSize desire = null;
        if (candidate.isAspectRatio16_9()) {
            desire = _findMaxRatio16_9(sPictureList);
        } else if (candidate.isAspectRatio4_3()) {
            desire = _findMaxRatio4_3(sPictureList);
        } else if (candidate.isAspectRatio1_1()) {
            desire = _findMaxRatio1_1(sPictureList);
        }
        if (desire == null || desire.isEmpty()) {
            return new PictureSize(((PictureSize) sPictureList.get(0)).width, ((PictureSize) sPictureList.get(0)).height);
        }
        return desire;
    }

    public static PictureSize getBestPanoPictureSize() {
        PictureSize desire;
        if (CameraSettings.isAspectRatio4_3(Util.sWindowWidth, Util.sWindowHeight)) {
            desire = _findMaxRatio4_3(sPictureList);
        } else {
            desire = _findMaxRatio16_9(sPictureList);
        }
        if (desire == null || desire.isEmpty()) {
            return new PictureSize(((PictureSize) sPictureList.get(0)).width, ((PictureSize) sPictureList.get(0)).height);
        }
        return desire;
    }

    public static void initialize(ActivityBase mActivity, List<Size> supported, int limit) {
        sPictureList.clear();
        if (supported == null || supported.size() == 0) {
            throw new IllegalArgumentException("The supported picture size list return from hal is null!");
        }
        initSensorRatio(supported);
        if (limit != 0) {
            ArrayList<Size> list = new ArrayList();
            for (Size size : supported) {
                if (size.width * size.height <= limit) {
                    list.add(size);
                }
            }
            supported = list;
        }
        PictureSize size4_3 = findMaxRatio4_3(supported);
        if (size4_3 != null) {
            sPictureList.add(size4_3);
        }
        PictureSize size1_1 = findMaxRatio1_1(supported);
        if (size1_1 != null) {
            sPictureList.add(size1_1);
        }
        PictureSize size16_9 = findMaxRatio16_9(supported);
        if (size16_9 != null) {
            sPictureList.add(size16_9);
        }
        if (sPictureList.size() == 0) {
            throw new IllegalArgumentException("Not find the desire picture sizes!");
        }
    }

    public static PictureSize _findMaxRatio4_3(List<PictureSize> supported) {
        int maxWidth = 0;
        int maxHeight = 0;
        for (PictureSize size : supported) {
            if (CameraSettings.isAspectRatio4_3(size.width, size.height) && size.width * size.height > maxWidth * maxHeight) {
                maxWidth = size.width;
                maxHeight = size.height;
            }
        }
        return maxWidth != 0 ? new PictureSize(maxWidth, maxHeight) : new PictureSize();
    }

    public static PictureSize _findMaxRatio1_1(List<PictureSize> supported) {
        int maxWidth = 0;
        int maxHeight = 0;
        for (PictureSize size : supported) {
            if (CameraSettings.isAspectRatio1_1(size.width, size.height) && size.width * size.height > maxWidth * maxHeight) {
                maxWidth = size.width;
                maxHeight = size.height;
            }
        }
        return maxWidth != 0 ? new PictureSize(maxWidth, maxHeight) : new PictureSize();
    }

    private static PictureSize _findMaxRatio16_9(List<PictureSize> supported) {
        int maxWidth = 0;
        int maxHeight = 0;
        for (PictureSize size : supported) {
            if (CameraSettings.isAspectRatio16_9(size.width, size.height) && size.width * size.height > maxWidth * maxHeight) {
                maxWidth = size.width;
                maxHeight = size.height;
            }
        }
        return maxWidth != 0 ? new PictureSize(maxWidth, maxHeight) : new PictureSize();
    }

    private static PictureSize findMaxRatio4_3(List<Size> supported) {
        int maxWidth = 0;
        int maxHeight = 0;
        for (Size size : supported) {
            if (CameraSettings.isAspectRatio4_3(size.width, size.height) && size.width * size.height > maxWidth * maxHeight) {
                maxWidth = size.width;
                maxHeight = size.height;
            }
        }
        return maxWidth != 0 ? new PictureSize(maxWidth, maxHeight) : new PictureSize();
    }

    private static PictureSize findMaxRatio1_1(List<Size> supported) {
        int maxWidth = 0;
        int maxHeight = 0;
        for (Size size : supported) {
            if (CameraSettings.isAspectRatio1_1(size.width, size.height) && size.width * size.height > maxWidth * maxHeight) {
                maxWidth = size.width;
                maxHeight = size.height;
            }
        }
        return maxWidth != 0 ? new PictureSize(maxWidth, maxHeight) : new PictureSize();
    }

    public static PictureSize findMaxRatio16_9(List<Size> supported) {
        int maxWidth = 0;
        int maxHeight = 0;
        for (Size size : supported) {
            if (CameraSettings.isAspectRatio16_9(size.width, size.height) && size.width * size.height > maxWidth * maxHeight) {
                maxWidth = size.width;
                maxHeight = size.height;
            }
        }
        return maxWidth != 0 ? new PictureSize(maxWidth, maxHeight) : new PictureSize();
    }

    private static void initSensorRatio(List<Size> supported) {
        if (Device.IS_X9 || Device.IS_A8) {
            sDefaultValue = "16x9";
            return;
        }
        int maxIndex = -1;
        int maxValue = 0;
        PictureSize pictureSize = new PictureSize();
        for (int i = 0; i < supported.size(); i++) {
            pictureSize.setPictureSize((Size) supported.get(i));
            if (maxValue < pictureSize.area()) {
                maxIndex = i;
                maxValue = pictureSize.area();
            }
        }
        pictureSize.setPictureSize((Size) supported.get(maxIndex));
        if (pictureSize.isAspectRatio4_3()) {
            sDefaultValue = "4x3";
        } else {
            sDefaultValue = "16x9";
        }
    }
}
