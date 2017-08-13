package com.android.camera.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.android.camera.CameraHolder;
import com.android.camera.module.BaseModule;
import com.android.camera.module.BaseModule.CameraMode;
import com.xiaomi.settingsdk.backup.ICloudBackup;
import com.xiaomi.settingsdk.backup.data.DataPackage;
import com.xiaomi.settingsdk.backup.data.PrefsBackupHelper;
import com.xiaomi.settingsdk.backup.data.PrefsBackupHelper.PrefEntry;
import java.util.ArrayList;
import java.util.List;

public class CameraSettingsBackupImpl implements ICloudBackup {
    private static final PrefEntry[] PREF_ENTRIES = CameraBackupSettings.PREF_ENTRIES;

    interface BackupRestoreHandler {
        void handle(SharedPreferences sharedPreferences, DataPackage dataPackage, PrefEntry[] prefEntryArr);
    }

    private static String getPrefixByCameraIdAndMode(int cameraId, CameraMode mode) {
        return "camera_settings_simple_mode_local_" + BaseModule.getPreferencesLocalId(cameraId, mode);
    }

    private static String getCloudPrefixByCameraIdAndMode(int cameraId, CameraMode mode) {
        if (checkCameraId(cameraId)) {
            int backCameraId = CameraHolder.instance().getBackCameraId();
            int frontCameraId = CameraHolder.instance().getFrontCameraId();
            if (cameraId == backCameraId) {
                cameraId = 0;
            } else if (cameraId == frontCameraId) {
                cameraId = 1;
            }
        }
        return "camera_settings_simple_mode_local_" + BaseModule.getPreferencesLocalId(cameraId, mode);
    }

    private static boolean checkCameraId(int cameraId) {
        if (cameraId < 0) {
            return false;
        }
        if (cameraId < 2) {
            return true;
        }
        throw new IllegalArgumentException("cameraId is invalid: " + cameraId);
    }

    public void onBackupSettings(Context context, DataPackage dataPackage) {
        Log.v("CameraSettingsBackupImpl", "Backing up settings to cloud.");
        handleBackupOrRestore(context, dataPackage, new BackupRestoreHandler() {
            public void handle(SharedPreferences sharedPref, DataPackage dataPackage, PrefEntry[] entries) {
                PrefsBackupHelper.backup(sharedPref, dataPackage, entries);
            }
        });
    }

    public void onRestoreSettings(Context context, DataPackage dataPackage, int packageVersion) {
        Log.v("CameraSettingsBackupImpl", "Restoring settings from cloud");
        handleBackupOrRestore(context, dataPackage, new BackupRestoreHandler() {
            public void handle(SharedPreferences sharedPref, DataPackage dataPackage, PrefEntry[] entries) {
                PrefsBackupHelper.restore(sharedPref, dataPackage, entries);
            }
        });
    }

    private void handleBackupOrRestore(Context context, DataPackage dataPackage, BackupRestoreHandler handler) {
        if (handler != null) {
            List<Integer> availableCameraIds = getAvaliableCameraIds();
            for (CameraMode mode : CameraMode.values()) {
                for (Integer intValue : availableCameraIds) {
                    int cameraId = intValue.intValue();
                    SharedPreferences sharedPref = context.getSharedPreferences(getPrefixByCameraIdAndMode(cameraId, mode), 0);
                    if (sharedPref != null) {
                        handler.handle(sharedPref, dataPackage, addPrefixToEntries(PREF_ENTRIES, getCloudPrefixByCameraIdAndMode(cameraId, mode)));
                    }
                }
            }
            handler.handle(context.getSharedPreferences("camera_settings_global", 0), dataPackage, addPrefixToEntries(PREF_ENTRIES, "camera_settings_global"));
            handler.handle(context.getSharedPreferences("camera_settings_simple_mode_global", 0), dataPackage, addPrefixToEntries(PREF_ENTRIES, "camera_settings_simple_mode_global"));
        }
    }

    private static List<Integer> getAvaliableCameraIds() {
        List<Integer> availableCameraIds = new ArrayList();
        int backCameraId = CameraHolder.instance().getBackCameraId();
        int frontCameraId = CameraHolder.instance().getFrontCameraId();
        if (checkCameraId(backCameraId)) {
            availableCameraIds.add(Integer.valueOf(backCameraId));
        }
        if (checkCameraId(frontCameraId)) {
            availableCameraIds.add(Integer.valueOf(frontCameraId));
        }
        return availableCameraIds;
    }

    private static PrefEntry[] addPrefixToEntries(PrefEntry[] prefEntries, String sharedPrefName) {
        PrefEntry[] entriesWithPrefix = new PrefEntry[prefEntries.length];
        for (int i = 0; i < prefEntries.length; i++) {
            PrefEntry entry = prefEntries[i];
            Class<?> valueClass = entry.getValueClass();
            String cloudKey = sharedPrefName + "::" + entry.getCloudKey();
            String localKey = entry.getLocalKey();
            Object defaultValue = entry.getDefaultValue();
            PrefEntry entryWithPrefix = null;
            if (valueClass.equals(Integer.class)) {
                if (defaultValue == null) {
                    entryWithPrefix = PrefEntry.createIntEntry(cloudKey, localKey);
                } else {
                    entryWithPrefix = PrefEntry.createIntEntry(cloudKey, localKey, ((Integer) defaultValue).intValue());
                }
            } else if (valueClass.equals(Boolean.class)) {
                if (defaultValue == null) {
                    entryWithPrefix = PrefEntry.createBoolEntry(cloudKey, localKey);
                } else {
                    entryWithPrefix = PrefEntry.createBoolEntry(cloudKey, localKey, ((Boolean) defaultValue).booleanValue());
                }
            } else if (valueClass.equals(String.class)) {
                if (defaultValue == null) {
                    entryWithPrefix = PrefEntry.createStringEntry(cloudKey, localKey);
                } else {
                    entryWithPrefix = PrefEntry.createStringEntry(cloudKey, localKey, (String) defaultValue);
                }
            } else if (valueClass.equals(Long.class)) {
                if (defaultValue == null) {
                    entryWithPrefix = PrefEntry.createLongEntry(cloudKey, localKey);
                } else {
                    entryWithPrefix = PrefEntry.createLongEntry(cloudKey, localKey, ((Long) defaultValue).longValue());
                }
            }
            entriesWithPrefix[i] = entryWithPrefix;
        }
        return entriesWithPrefix;
    }

    public int getCurrentVersion(Context context) {
        return 1;
    }
}
