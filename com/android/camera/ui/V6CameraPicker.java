package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraHolder;
import com.android.camera.preferences.ListPreference;
import com.android.camera.preferences.PreferenceInflater;
import java.util.ArrayList;

public class V6CameraPicker extends AnimationImageView implements OnClickListener {
    private static final String TAG = V6CameraPicker.class.getSimpleName();
    private int mCameraFacing;
    private boolean mEnabled;
    private boolean mInitEntryValues;
    private ListPreference mPreference = ((ListPreference) new PreferenceInflater(this.mContext).inflate((int) R.xml.v6_camera_picker_preferences));
    private boolean mVisible = true;

    public V6CameraPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    public void onResume() {
        super.onResume();
        setVisibility(0);
    }

    public void enableControls(boolean enable) {
        setEnabled(enable);
        this.mEnabled = enable;
    }

    public void onCameraOpen() {
        super.onCameraOpen();
        initEntryValues();
        updateVisible();
        reloadPreference();
    }

    private void updateVisible() {
        if (isNeedShow()) {
            this.mVisible = true;
            setVisibility(0);
            return;
        }
        this.mVisible = false;
        setVisibility(8);
    }

    private boolean isNeedShow() {
        if (this.mPreference == null || this.mPreference.getEntryValues() == null) {
            return false;
        }
        return this.mPreference.getEntryValues().length > 1;
    }

    private void initEntryValues() {
        if (!this.mInitEntryValues) {
            ArrayList<CharSequence> idList = new ArrayList(2);
            if (CameraHolder.instance().getBackCameraId() != -1) {
                idList.add(Math.min(0, idList.size()), String.valueOf(CameraHolder.instance().getBackCameraId()));
            }
            if (CameraHolder.instance().getFrontCameraId() != -1) {
                idList.add(Math.min(1, idList.size()), String.valueOf(CameraHolder.instance().getFrontCameraId()));
            }
            this.mPreference.setEntryValues((CharSequence[]) idList.toArray(new CharSequence[idList.size()]));
            this.mInitEntryValues = true;
        }
    }

    private void reloadPreference() {
        if (isNeedShow()) {
            if (TextUtils.equals(this.mPreference.getEntryValues()[1], this.mPreference.getValue())) {
                this.mCameraFacing = 1;
            } else {
                this.mCameraFacing = 0;
            }
        }
    }

    public void onClick(View v) {
        if (this.mVisible && this.mEnabled) {
            int newCameraFacing;
            Log.v(TAG, "click switch camera button");
            if (this.mCameraFacing == 0) {
                newCameraFacing = 1;
            } else {
                newCameraFacing = 0;
            }
            int oldFacing = this.mCameraFacing;
            this.mCameraFacing = newCameraFacing;
            AutoLockManager.getInstance(this.mContext).onUserInteraction();
            if (!this.mMessageDispacher.dispacherMessage(0, R.id.v6_camera_picker, 2, Integer.valueOf(Integer.parseInt((String) this.mPreference.getEntryValues()[this.mCameraFacing])), null)) {
                this.mCameraFacing = oldFacing;
            }
        }
    }

    public void setVisibility(int visibility) {
        if (!this.mVisible) {
            visibility = 8;
        }
        super.setVisibility(visibility);
    }
}
