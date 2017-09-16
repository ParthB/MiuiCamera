package com.android.camera;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.Preference;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.support.v7.recyclerview.R;
import android.widget.Toast;
import com.android.camera.ui.PreviewListPreference;

public class CameraPreferenceActivity extends BasePreferenceActivity {
    private AlertDialog mDoubleConfirmActionChooseDialog = null;

    protected int getPreferenceXml() {
        return R.xml.camera_other_preferences;
    }

    protected void onSettingChanged(int type) {
        CameraSettings.sCameraChangeManager.request(type);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!preference.getKey().equals("pref_camera_snap_key") || newValue == null) {
            return super.onPreferenceChange(preference, newValue);
        }
        if (System.getInt(getContentResolver(), "volumekey_wake_screen", 0) == 1) {
            Toast.makeText(this, R.string.pref_camera_snap_toast_when_volume_can_wake_screen, 0).show();
            return false;
        } else if ((newValue.equals(getString(CameraSettings.getDefaultPreferenceId(R.string.pref_camera_snap_value_take_picture))) || newValue.equals(getString(CameraSettings.getDefaultPreferenceId(R.string.pref_camera_snap_value_take_movie)))) && "public_transportation_shortcuts".equals(Secure.getString(getContentResolver(), "key_long_press_volume_down"))) {
            bringUpDoubleConfirmDlg((PreviewListPreference) preference, (String) newValue);
            return false;
        } else {
            Secure.putString(getContentResolver(), "key_long_press_volume_down", CameraSettings.getMiuiSettingsKeyForStreetSnap((String) newValue));
            return true;
        }
    }

    protected void filterByIntent() {
        super.filterByIntent();
        if (getIntent().getBooleanExtra("IsCaptureIntent", false)) {
            removePreference(this.mPreferenceGroup, "pref_capture_when_stable_key");
            removePreference(this.mPreferenceGroup, "pref_groupshot_with_primitive_picture_key");
        }
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getIntent().getCharSequenceExtra(":miui:starting_window_label") != null) {
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.pref_camera_settings_category);
        }
    }

    private void bringUpDoubleConfirmDlg(final PreviewListPreference preference, final String snapItem) {
        if (this.mDoubleConfirmActionChooseDialog == null) {
            OnClickListener listener = new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == -1) {
                        CameraPreferenceActivity.this.mDoubleConfirmActionChooseDialog = null;
                        preference.setValue(snapItem);
                        Secure.putString(CameraPreferenceActivity.this.getContentResolver(), "key_long_press_volume_down", CameraSettings.getMiuiSettingsKeyForStreetSnap(snapItem));
                    } else if (which == -2) {
                        CameraPreferenceActivity.this.mDoubleConfirmActionChooseDialog.dismiss();
                        CameraPreferenceActivity.this.mDoubleConfirmActionChooseDialog = null;
                    }
                }
            };
            this.mDoubleConfirmActionChooseDialog = new Builder(this).setTitle(R.string.title_snap_double_confirm).setMessage(R.string.message_snap_double_confirm).setPositiveButton(R.string.snap_confirmed, listener).setNegativeButton(R.string.snap_cancel, listener).setCancelable(false).create();
            this.mDoubleConfirmActionChooseDialog.show();
        }
    }
}
