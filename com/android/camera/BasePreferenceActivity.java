package com.android.camera;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings.Secure;
import android.support.v7.recyclerview.R;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.storage.PriorityStorageBroadcastReceiver;
import com.android.camera.storage.Storage;
import com.android.camera.ui.PreviewListPreference;
import com.android.camera.ui.V6ModulePicker;
import java.util.ArrayList;
import java.util.List;

public abstract class BasePreferenceActivity extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = BasePreferenceActivity.class.getSimpleName();
    private int mFaceDetectionHitCountDown;
    private int mFromWhere;
    private Preference mPortraitWithFaceBeautyPreference;
    protected PreferenceScreen mPreferenceGroup;
    protected CameraSettingPreferences mPreferences;

    protected abstract int getPreferenceXml();

    protected abstract void onSettingChanged(int i);

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Util.updateCountryIso(this);
        this.mFromWhere = getIntent().getIntExtra("from_where", 0);
        this.mPreferences = CameraSettingPreferences.instance();
        CameraSettings.upgradeGlobalPreferences(this.mPreferences);
        Storage.initStorage(this);
        initializeActivity();
        if (getIntent().getBooleanExtra("StartActivityWhenLocked", false)) {
            getWindow().addFlags(524288);
        }
    }

    public void onResume() {
        super.onResume();
        if (CameraSettings.isCameraPortraitWithFaceBeautyOptionVisible()) {
            this.mFaceDetectionHitCountDown = -1;
        } else {
            this.mFaceDetectionHitCountDown = 8;
        }
    }

    private void initializeActivity() {
        this.mPreferenceGroup = getPreferenceScreen();
        if (this.mPreferenceGroup != null) {
            this.mPreferenceGroup.removeAll();
        }
        addPreferencesFromResource(getPreferenceXml());
        this.mPreferenceGroup = getPreferenceScreen();
        if (this.mPreferenceGroup == null) {
            finish();
        }
        this.mPortraitWithFaceBeautyPreference = this.mPreferenceGroup.findPreference("pref_camera_portrait_with_facebeauty_key");
        registerListener();
        filterByPreference();
        filterByFrom();
        filterByDeviceID();
        filterByCameraID();
        filterByIntent();
        filterGroup();
        updateEntries();
        updatePreferences(this.mPreferenceGroup, this.mPreferences);
        updateConflictPreference(null);
    }

    private void filterByPreference() {
        PreviewListPreference videoquality = (PreviewListPreference) this.mPreferenceGroup.findPreference("pref_video_quality_key");
        if (videoquality != null) {
            filterUnsupportedOptions(this.mPreferenceGroup, videoquality, CameraSettings.getSupportedVideoQuality(CameraSettings.getCameraId()));
        }
        String speed = CameraSettings.getVideoSpeed(this.mPreferences);
        if (Device.IS_X9 && !"normal".equals(speed)) {
            removePreference(this.mPreferenceGroup, "pref_camera_movie_solid_key");
        }
        if (!Device.isHFRVideoCaptureSupported() && "slow".equals(speed)) {
            removePreference(this.mPreferenceGroup, "pref_video_captrue_ability_key");
        }
        if (!CameraSettings.isCameraPortraitWithFaceBeautyOptionVisible()) {
            removePreference(this.mPreferenceGroup, "pref_camera_portrait_with_facebeauty_key");
        }
    }

    private void registerListener() {
        registerListener(this.mPreferenceGroup, this);
        Preference restore = this.mPreferenceGroup.findPreference("pref_restore");
        if (restore != null) {
            restore.setOnPreferenceClickListener(this);
        }
        Preference priorityStorage = this.mPreferenceGroup.findPreference("pref_priority_storage");
        if (priorityStorage != null) {
            priorityStorage.setOnPreferenceClickListener(this);
        }
        Preference faceDetection = this.mPreferenceGroup.findPreference("pref_camera_facedetection_key");
        if (faceDetection != null) {
            faceDetection.setOnPreferenceClickListener(this);
        }
    }

    private void filterByFrom() {
        if (this.mFromWhere == 1) {
            removePreference(this.mPreferenceGroup, "category_camcorder_setting");
        } else if (this.mFromWhere == 2) {
            removePreference(this.mPreferenceGroup, "category_camera_setting");
            removePreference(this.mPreferenceGroup, "pref_qc_camera_sharpness_key");
            removePreference(this.mPreferenceGroup, "pref_qc_camera_contrast_key");
            removePreference(this.mPreferenceGroup, "pref_qc_camera_saturation_key");
            removePreference(this.mPreferenceGroup, "pref_camera_facedetection_key");
            removePreference(this.mPreferenceGroup, "pref_camera_show_gender_age_key");
            removePreference(this.mPreferenceGroup, "pref_camera_autoexposure_key");
            removePreference(this.mPreferenceGroup, "pref_scan_qrcode_key");
            removePreference(this.mPreferenceGroup, "pref_camera_portrait_with_facebeauty_key");
        }
    }

    private void filterByDeviceID() {
        if (!Device.isSupportedHFR()) {
            removePreference(this.mPreferenceGroup, "pref_camera_hfr_key");
        }
        if (!Device.isSupportedIntelligentBeautify()) {
            removePreference(this.mPreferenceGroup, "pref_camera_show_gender_age_key");
        }
        if (!Device.isSupportedSkinBeautify()) {
            removePreference(this.mPreferenceGroup, "pref_camera_show_gender_age_key");
        }
        removePreference(this.mPreferenceGroup, "pref_camera_long_press_shutter_key");
        if (!Device.isSupportedMovieSolid()) {
            removePreference(this.mPreferenceGroup, "pref_camera_movie_solid_key");
        }
        if (!Device.isSupportedTimeWaterMark()) {
            removePreference(this.mPreferenceGroup, "pref_watermark_key");
        }
        if (!Device.isSupportedFaceInfoWaterMark()) {
            removePreference(this.mPreferenceGroup, "pref_face_info_watermark_key");
        }
        if (!Device.isSupportedMuteCameraSound()) {
            removePreference(this.mPreferenceGroup, "pref_camerasound_key");
        }
        if (!Device.isSupportedGPS()) {
            removePreference(this.mPreferenceGroup, "pref_camera_recordlocation_key");
        }
        if (Device.isPad() || (Device.IS_MI3TD && this.mPreferences.isFrontCamera())) {
            removePreference(this.mPreferenceGroup, "pref_camera_picturesize_key");
        }
        if (!Storage.secondaryStorageMounted()) {
            removePreference(this.mPreferenceGroup, "pref_priority_storage");
        }
        if (!Device.isSupportedChromaFlash()) {
            removePreference(this.mPreferenceGroup, "pref_auto_chroma_flash_key");
        }
        if (!Device.isSupportedLongPressBurst()) {
            removePreference(this.mPreferenceGroup, "pref_camera_long_press_shutter_feature_key");
        }
        if (!Device.isSupportedObjectTrack()) {
            removePreference(this.mPreferenceGroup, "pref_capture_when_stable_key");
        }
        if (!Device.isSupportedAsdNight()) {
            removePreference(this.mPreferenceGroup, "pref_camera_asd_night_key");
        }
        if (!Device.isSupportedAsdFlash()) {
            removePreference(this.mPreferenceGroup, "pref_camera_asd_popup_key");
        }
        if (!Device.isSupportedQuickSnap()) {
            removePreference(this.mPreferenceGroup, "pref_camera_snap_key");
        }
        if (!Device.isSupportGroupShot()) {
            removePreference(this.mPreferenceGroup, "pref_groupshot_with_primitive_picture_key");
        }
        if (!CameraSettings.isSupportedPortrait()) {
            removePreference(this.mPreferenceGroup, "pref_camera_portrait_with_facebeauty_key");
        }
        if (Device.isThirdDevice()) {
            removePreference(this.mPreferenceGroup, "pref_camera_facedetection_key");
            removePreference(this.mPreferenceGroup, "pref_front_mirror_key");
            removePreference(this.mPreferenceGroup, "pref_qc_camera_sharpness_key");
            removePreference(this.mPreferenceGroup, "pref_qc_camera_contrast_key");
            removePreference(this.mPreferenceGroup, "pref_qc_camera_saturation_key");
            removePreference(this.mPreferenceGroup, "pref_camera_autoexposure_key");
        }
        if (Device.IS_D2A) {
            removePreference(this.mPreferenceGroup, "pref_scan_qrcode_key");
        }
    }

    private void filterByCameraID() {
        if (this.mPreferences.isFrontCamera()) {
            removePreference(this.mPreferenceGroup, "pref_camera_hfr_key");
            removePreference(this.mPreferenceGroup, "pref_video_focusmode_key");
            removePreference(this.mPreferenceGroup, "pref_camera_skinToneEnhancement_key");
            removePreference(this.mPreferenceGroup, "pref_scan_qrcode_key");
            removePreference(this.mPreferenceGroup, "pref_camera_autoexposure_key");
            removePreference(this.mPreferenceGroup, "pref_qc_camera_sharpness_key");
            removePreference(this.mPreferenceGroup, "pref_qc_camera_contrast_key");
            removePreference(this.mPreferenceGroup, "pref_qc_camera_saturation_key");
            removePreference(this.mPreferenceGroup, "pref_auto_chroma_flash_key");
            removePreference(this.mPreferenceGroup, "pref_capture_when_stable_key");
            removePreference(this.mPreferenceGroup, "pref_video_time_lapse_frame_interval_key");
            removePreference(this.mPreferenceGroup, "pref_camera_long_press_shutter_feature_key");
            removePreference(this.mPreferenceGroup, "pref_camera_asd_night_key");
            removePreference(this.mPreferenceGroup, "pref_camera_asd_popup_key");
            removePreference(this.mPreferenceGroup, "pref_camera_movie_solid_key");
            removePreference(this.mPreferenceGroup, "pref_camera_portrait_with_facebeauty_key");
            return;
        }
        removePreference(this.mPreferenceGroup, "pref_front_mirror_key");
    }

    public void filterUnsupportedOptions(PreferenceGroup group, PreviewListPreference pref, List<String> supported) {
        if (supported == null || supported.size() <= 1) {
            removePreference(group, pref.getKey());
            return;
        }
        pref.filterUnsupported(supported);
        if (pref.getEntries().length <= 1) {
            removePreference(group, pref.getKey());
        } else {
            resetIfInvalid(pref);
        }
    }

    private void resetIfInvalid(ListPreference pref) {
        if (pref.findIndexOfValue(pref.getValue()) == -1) {
            pref.setValueIndex(0);
        }
    }

    private void registerListener(PreferenceGroup group, OnPreferenceChangeListener l) {
        int total = group.getPreferenceCount();
        for (int i = 0; i < total; i++) {
            Preference child = group.getPreference(i);
            if (child instanceof PreferenceGroup) {
                registerListener((PreferenceGroup) child, l);
            } else {
                child.setOnPreferenceChangeListener(l);
            }
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("pref_restore")) {
            RotateDialogController.showSystemAlertDialog(this, getString(R.string.confirm_restore_title), getString(R.string.confirm_restore_message), getString(17039370), new Runnable() {
                public void run() {
                    BasePreferenceActivity.this.restorePreferences();
                }
            }, getString(17039360), null);
            return true;
        }
        if ("pref_priority_storage".equals(preference.getKey())) {
            PriorityStorageBroadcastReceiver.setPriorityStorage(((CheckBoxPreference) preference).isChecked());
        } else if ("pref_camera_facedetection_key".equals(preference.getKey()) && CameraSettings.isSupportedPortrait() && CameraSettings.isBackCamera() && V6ModulePicker.isCameraModule() && this.mFaceDetectionHitCountDown > 0) {
            this.mFaceDetectionHitCountDown--;
            if (this.mFaceDetectionHitCountDown == 0) {
                Toast.makeText(this, R.string.portrait_with_facebeauty_hint, 1).show();
                CameraSettings.setCameraPortraitWithFaceBeautyOptionVisible(true);
                addPreference("category_advance_setting", this.mPortraitWithFaceBeautyPreference);
            }
        }
        return false;
    }

    private void restorePreferences() {
        CameraSettings.restorePreferences(this, this.mPreferences);
        initializeActivity();
        PriorityStorageBroadcastReceiver.setPriorityStorage(getResources().getBoolean(R.bool.priority_storage));
        onSettingChanged(3);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        onSettingChanged(1);
        Editor editor = this.mPreferences.edit();
        String key = preference.getKey();
        if (newValue instanceof String) {
            editor.putString(key, (String) newValue);
        } else if (newValue instanceof Boolean) {
            editor.putBoolean(key, ((Boolean) newValue).booleanValue());
        } else if (newValue instanceof Integer) {
            editor.putInt(key, ((Integer) newValue).intValue());
        } else if (newValue instanceof Long) {
            editor.putLong(key, ((Long) newValue).longValue());
        } else if (newValue instanceof Float) {
            editor.putFloat(key, ((Float) newValue).floatValue());
        } else {
            throw new IllegalStateException("unhandled new value with type=" + newValue.getClass().getName());
        }
        editor.apply();
        updateConflictPreference(preference);
        return true;
    }

    protected boolean removePreference(PreferenceGroup root, String key) {
        Preference child = root.findPreference(key);
        if (child != null && root.removePreference(child)) {
            return true;
        }
        int count = root.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            child = root.getPreference(i);
            if ((child instanceof PreferenceGroup) && removePreference((PreferenceGroup) child, key)) {
                return true;
            }
        }
        return false;
    }

    protected boolean addPreference(String group, Preference preference) {
        Preference preferenceGroup = this.mPreferenceGroup.findPreference(group);
        if (!(preferenceGroup instanceof PreferenceGroup)) {
            return false;
        }
        ((PreferenceGroup) preferenceGroup).addPreference(preference);
        return true;
    }

    private void updateEntries() {
        PreviewListPreference pictureSize = (PreviewListPreference) this.mPreferenceGroup.findPreference("pref_camera_picturesize_key");
        PreviewListPreference antiBanding = (PreviewListPreference) this.mPreferenceGroup.findPreference("pref_camera_antibanding_key");
        CheckBoxPreference chromaFlash = (CheckBoxPreference) this.mPreferenceGroup.findPreference("pref_auto_chroma_flash_key");
        PreviewListPreference videoquality = (PreviewListPreference) this.mPreferenceGroup.findPreference("pref_video_quality_key");
        PreviewListPreference snapType = (PreviewListPreference) this.mPreferenceGroup.findPreference("pref_camera_snap_key");
        if (pictureSize != null) {
            pictureSize.setEntries(PictureSizeManager.getEntries());
            pictureSize.setEntryValues(PictureSizeManager.getEntryValues());
            pictureSize.setDefaultValue(PictureSizeManager.getDefaultValue());
            pictureSize.setValue(PictureSizeManager.getDefaultValue());
        }
        if (antiBanding != null && Util.isAntibanding60()) {
            antiBanding.setValue(getString(R.string.pref_camera_antibanding_60));
            antiBanding.setDefaultValue(getString(R.string.pref_camera_antibanding_60));
        }
        if (chromaFlash != null) {
            chromaFlash.setChecked(getResources().getBoolean(CameraSettings.getDefaultPreferenceId(R.bool.pref_camera_auto_chroma_flash_default)));
        }
        if (videoquality != null) {
            String defaultVideoQuality = getString(CameraSettings.getDefaultPreferenceId(R.string.pref_video_quality_default));
            videoquality.setDefaultValue(defaultVideoQuality);
            videoquality.setValue(defaultVideoQuality);
        }
        if (snapType != null && Device.isSupportedQuickSnap()) {
            String defaultSnapType = getString(R.string.pref_camera_snap_default);
            snapType.setDefaultValue(defaultSnapType);
            snapType.setValue(defaultSnapType);
            String settingsSnapType = Secure.getString(getContentResolver(), "key_long_press_volume_down");
            if ("public_transportation_shortcuts".equals(settingsSnapType) || "none".equals(settingsSnapType)) {
                snapType.setValue(getString(R.string.pref_camera_snap_value_off));
                return;
            }
            String snapValue = CameraSettingPreferences.instance().getString("pref_camera_snap_key", null);
            if (snapValue != null) {
                Secure.putString(getContentResolver(), "key_long_press_volume_down", CameraSettings.getMiuiSettingsKeyForStreetSnap(snapValue));
                CameraSettingPreferences.instance().edit().remove("pref_camera_snap_key").apply();
                snapType.setValue(snapValue);
            } else if ("Street-snap-picture".equals(settingsSnapType)) {
                snapType.setValue(getString(R.string.pref_camera_snap_value_take_picture));
            } else if ("Street-snap-movie".equals(settingsSnapType)) {
                snapType.setValue(getString(R.string.pref_camera_snap_value_take_movie));
            }
        }
    }

    private void updatePreferences(PreferenceGroup group, SharedPreferences sp) {
        if (group != null) {
            int count = group.getPreferenceCount();
            for (int i = 0; i < count; i++) {
                Preference child = group.getPreference(i);
                if (child instanceof PreviewListPreference) {
                    PreviewListPreference list = (PreviewListPreference) child;
                    if ("pref_camera_picturesize_key".equals(list.getKey())) {
                        list.setValue(PictureSizeManager.getPictureSize(true).toString());
                    } else {
                        list.setValue(getFilterValue(list, sp));
                    }
                    child.setPersistent(false);
                } else if (child instanceof CheckBoxPreference) {
                    CheckBoxPreference checkBox = (CheckBoxPreference) child;
                    checkBox.setChecked(sp.getBoolean(checkBox.getKey(), checkBox.isChecked()));
                    child.setPersistent(false);
                } else if (child instanceof PreferenceGroup) {
                    updatePreferences((PreferenceGroup) child, sp);
                } else {
                    Log.v(TAG, "no need update preference for " + child.getKey());
                }
            }
        }
    }

    private String getFilterValue(PreviewListPreference list, SharedPreferences sp) {
        String defaultValue = list.getValue();
        if (sp == null) {
            return defaultValue;
        }
        CharSequence value = sp.getString(list.getKey(), defaultValue);
        if (!CameraSettings.isStringValueContains(value, list.getEntryValues())) {
            value = defaultValue;
            if (!CameraSettings.isStringValueContains(defaultValue, list.getEntryValues())) {
                CharSequence[] entryValues = list.getEntryValues();
                if (entryValues != null && entryValues.length >= 1) {
                    value = entryValues[0];
                }
            }
            Editor editor = sp.edit();
            editor.putString(list.getKey(), value.toString());
            editor.apply();
        }
        return value.toString();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        finish();
        return true;
    }

    protected void filterByIntent() {
        ArrayList<String> removeKeys = getIntent().getStringArrayListExtra("remove_keys");
        if (removeKeys != null) {
            for (String key : removeKeys) {
                removePreference(this.mPreferenceGroup, key);
            }
        }
    }

    private void filterGroup() {
        filterGroupIfEmpty("category_device_setting");
        filterGroupIfEmpty("category_camcorder_setting");
        filterGroupIfEmpty("category_camera_setting");
        filterGroupIfEmpty("category_advance_setting");
    }

    private void filterGroupIfEmpty(String key) {
        Preference group = this.mPreferenceGroup.findPreference(key);
        if (group != null && (group instanceof PreferenceGroup) && ((PreferenceGroup) group).getPreferenceCount() == 0) {
            removePreference(this.mPreferenceGroup, key);
        }
    }

    protected void onStop() {
        super.onStop();
        finish();
    }

    private void updateConflictPreference(Preference preference) {
        if (Device.IS_X9 && !this.mPreferences.isFrontCamera() && CameraSettings.isMovieSolidOn(this.mPreferences) && 6 <= CameraSettings.getPreferVideoQuality()) {
            CheckBoxPreference movieSolid = (CheckBoxPreference) this.mPreferenceGroup.findPreference("pref_camera_movie_solid_key");
            PreviewListPreference videoquality = (PreviewListPreference) this.mPreferenceGroup.findPreference("pref_video_quality_key");
            Editor editor;
            if (preference == null || !"pref_camera_movie_solid_key".equals(preference.getKey())) {
                editor = this.mPreferences.edit();
                editor.putBoolean("pref_camera_movie_solid_key", false);
                editor.apply();
                movieSolid.setChecked(false);
                return;
            }
            String defaultQuality = getString(CameraSettings.getDefaultPreferenceId(R.string.pref_video_quality_default));
            editor = this.mPreferences.edit();
            editor.putString("pref_video_quality_key", defaultQuality);
            editor.apply();
            videoquality.setValue(defaultQuality);
        }
    }
}
