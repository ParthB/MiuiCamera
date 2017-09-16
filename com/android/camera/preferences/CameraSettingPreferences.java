package com.android.camera.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import com.android.camera.CameraAppImpl;
import com.android.camera.CameraHolder;
import com.android.camera.CameraSettings;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CameraSettingPreferences implements SharedPreferences {
    private static CameraSettingPreferences sPreferences;
    private SharedPreferences mPrefGlobal;
    private SharedPreferences mPrefLocal;
    private SharedPreferences mPrefModeGlobal;
    private int mPreferencesLocalId;

    private class MyEditor implements Editor {
        private Editor mEditorGlobal;
        private Editor mEditorLocal;
        private Editor mEditorModeGlobal;

        MyEditor() {
            this.mEditorGlobal = CameraSettingPreferences.this.mPrefGlobal.edit();
            this.mEditorModeGlobal = CameraSettingPreferences.this.mPrefModeGlobal.edit();
            this.mEditorLocal = CameraSettingPreferences.this.mPrefLocal.edit();
        }

        public boolean commit() {
            return (this.mEditorGlobal.commit() && this.mEditorModeGlobal.commit()) ? this.mEditorLocal.commit() : false;
        }

        public void apply() {
            this.mEditorGlobal.apply();
            this.mEditorModeGlobal.apply();
            this.mEditorLocal.apply();
        }

        public Editor clear() {
            this.mEditorGlobal.clear();
            this.mEditorModeGlobal.clear();
            this.mEditorLocal.clear();
            return this;
        }

        public Editor remove(String key) {
            this.mEditorGlobal.remove(key);
            this.mEditorModeGlobal.remove(key);
            this.mEditorLocal.remove(key);
            return this;
        }

        public Editor putString(String key, String value) {
            if (CameraSettingPreferences.isGlobal(key)) {
                this.mEditorGlobal.putString(key, value);
            } else if (CameraSettingPreferences.isModeGlobal(key)) {
                this.mEditorModeGlobal.putString(key, value);
            } else {
                this.mEditorLocal.putString(key, value);
            }
            return this;
        }

        public Editor putInt(String key, int value) {
            if (CameraSettingPreferences.isGlobal(key)) {
                this.mEditorGlobal.putInt(key, value);
            } else if (CameraSettingPreferences.isModeGlobal(key)) {
                this.mEditorModeGlobal.putInt(key, value);
            } else {
                this.mEditorLocal.putInt(key, value);
            }
            return this;
        }

        public Editor putLong(String key, long value) {
            if (CameraSettingPreferences.isGlobal(key)) {
                this.mEditorGlobal.putLong(key, value);
            } else if (CameraSettingPreferences.isModeGlobal(key)) {
                this.mEditorModeGlobal.putLong(key, value);
            } else {
                this.mEditorLocal.putLong(key, value);
            }
            return this;
        }

        public Editor putFloat(String key, float value) {
            if (CameraSettingPreferences.isGlobal(key)) {
                this.mEditorGlobal.putFloat(key, value);
            } else if (CameraSettingPreferences.isModeGlobal(key)) {
                this.mEditorModeGlobal.putFloat(key, value);
            } else {
                this.mEditorLocal.putFloat(key, value);
            }
            return this;
        }

        public Editor putBoolean(String key, boolean value) {
            if (CameraSettingPreferences.isGlobal(key)) {
                this.mEditorGlobal.putBoolean(key, value);
            } else if (CameraSettingPreferences.isModeGlobal(key)) {
                this.mEditorModeGlobal.putBoolean(key, value);
            } else {
                this.mEditorLocal.putBoolean(key, value);
            }
            return this;
        }

        public Editor putStringSet(String key, Set<String> set) {
            throw new UnsupportedOperationException();
        }
    }

    private CameraSettingPreferences(Context context) {
        this.mPrefGlobal = context.getSharedPreferences("camera_settings_global", 0);
    }

    public static synchronized CameraSettingPreferences instance() {
        CameraSettingPreferences cameraSettingPreferences;
        synchronized (CameraSettingPreferences.class) {
            if (sPreferences == null) {
                sPreferences = new CameraSettingPreferences(CameraAppImpl.getAndroidContext());
                sPreferences.setLocalIdInternal(sPreferences.getCameraId());
            }
            cameraSettingPreferences = sPreferences;
        }
        return cameraSettingPreferences;
    }

    public CameraSettingPreferences setLocalId(int preferencesLocalId) {
        if (preferencesLocalId != this.mPreferencesLocalId) {
            return setLocalIdInternal(preferencesLocalId);
        }
        return this;
    }

    private CameraSettingPreferences setLocalIdInternal(int preferencesLocalId) {
        this.mPreferencesLocalId = preferencesLocalId;
        this.mPrefModeGlobal = CameraAppImpl.getAndroidContext().getSharedPreferences("camera_settings_simple_mode_global", 0);
        this.mPrefLocal = CameraAppImpl.getAndroidContext().getSharedPreferences("camera_settings_simple_mode_local_" + preferencesLocalId, 0);
        CameraSettings.upgradeLocalPreferences(this.mPrefLocal);
        return sPreferences;
    }

    public boolean isFrontCamera() {
        return instance().getCameraId() == CameraHolder.instance().getFrontCameraId();
    }

    public boolean isBackCamera() {
        return instance().getCameraId() == CameraHolder.instance().getBackCameraId();
    }

    private static boolean isGlobal(String key) {
        return (key.equals("pref_camera_id_key") || key.equals("pref_camera_recordlocation_key") || key.equals("pref_camera_volumekey_function_key") || key.equals("pref_version_key") || key.equals("pref_camerasound_key") || key.equals("pref_camera_referenceline_key") || key.equals("pref_watermark_key") || key.equals("pref_dualcamera_watermark") || key.equals("pref_face_info_watermark_key") || key.equals("pref_camera_antibanding_key") || key.equals("pref_front_mirror_key") || key.equals("pref_camera_show_gender_age_key") || key.equals("open_camera_fail_key") || key.equals("pref_camera_first_use_hint_shown_key") || key.equals("pref_camera_first_portrait_use_hint_shown_key") || key.equals("pref_camera_confirm_location_shown_key") || key.equals("pref_front_camera_first_use_hint_shown_key") || key.equals("pref_key_camera_smart_shutter_position") || key.equals("pref_priority_storage") || key.equals("pref_camera_snap_key") || key.equals("pref_groupshot_with_primitive_picture_key") || key.equals("pref_camera_mode_settings_key_remind")) ? true : key.equals("panorama_last_start_direction_key");
    }

    private static boolean isModeGlobal(String key) {
        return key.equals("pref_video_captrue_ability_key");
    }

    private int getCameraId() {
        return Integer.parseInt(this.mPrefGlobal.getString("pref_camera_id_key", "0"));
    }

    public Map<String, ?> getAll() {
        Map<String, Object> result = new HashMap();
        result.putAll(this.mPrefLocal.getAll());
        result.putAll(this.mPrefModeGlobal.getAll());
        result.putAll(this.mPrefGlobal.getAll());
        return result;
    }

    public String getString(String key, String defValue) {
        if (isGlobal(key)) {
            return this.mPrefGlobal.getString(key, defValue);
        }
        if (isModeGlobal(key)) {
            return this.mPrefModeGlobal.getString(key, defValue);
        }
        return this.mPrefLocal.getString(key, defValue);
    }

    public int getInt(String key, int defValue) {
        if (isGlobal(key)) {
            return this.mPrefGlobal.getInt(key, defValue);
        }
        if (isModeGlobal(key)) {
            return this.mPrefModeGlobal.getInt(key, defValue);
        }
        return this.mPrefLocal.getInt(key, defValue);
    }

    public long getLong(String key, long defValue) {
        if (isGlobal(key)) {
            return this.mPrefGlobal.getLong(key, defValue);
        }
        if (isModeGlobal(key)) {
            return this.mPrefModeGlobal.getLong(key, defValue);
        }
        return this.mPrefLocal.getLong(key, defValue);
    }

    public float getFloat(String key, float defValue) {
        if (isGlobal(key)) {
            return this.mPrefGlobal.getFloat(key, defValue);
        }
        if (isModeGlobal(key)) {
            return this.mPrefModeGlobal.getFloat(key, defValue);
        }
        return this.mPrefLocal.getFloat(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue) {
        if (isGlobal(key)) {
            return this.mPrefGlobal.getBoolean(key, defValue);
        }
        if (isModeGlobal(key)) {
            return this.mPrefModeGlobal.getBoolean(key, defValue);
        }
        return this.mPrefLocal.getBoolean(key, defValue);
    }

    public Set<String> getStringSet(String key, Set<String> set) {
        throw new UnsupportedOperationException();
    }

    public boolean contains(String key) {
        if (this.mPrefLocal.contains(key) || this.mPrefModeGlobal.contains(key) || this.mPrefGlobal.contains(key)) {
            return true;
        }
        return false;
    }

    public Editor edit() {
        return new MyEditor();
    }

    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }

    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }
}
