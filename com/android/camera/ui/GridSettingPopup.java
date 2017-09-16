package com.android.camera.ui;

import android.content.Context;
import android.media.AudioSystem;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SimpleAdapter;
import com.android.camera.ActivityBase;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraSettings;
import com.android.camera.Util;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridSettingPopup extends V6AbstractSettingPopup implements OnClickListener {
    private final String TAG = "GridSettingPopup";
    protected int mCurrentIndex = -1;
    protected int mDisplayColumnNum = 5;
    protected GridView mGridView;
    protected int mGridViewHeight;
    protected boolean mHasImage = true;
    protected boolean mIgnoreSameItemClick = true;

    protected class MySimpleAdapter extends SimpleAdapter {
        public MySimpleAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            v.setOnClickListener(GridSettingPopup.this);
            v.setTag(new Integer(position));
            if (v instanceof Rotatable) {
                ((Rotatable) v).setOrientation(0, false);
            }
            GridSettingPopup.this.updateItemView(position, v);
            return v;
        }

        public boolean areAllItemsEnabled() {
            return false;
        }
    }

    public GridSettingPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        String[] from;
        int[] to;
        super.initialize(preferenceGroup, preference, p);
        Context context = getContext();
        CharSequence[] entries = this.mPreference.getEntries();
        int[] iconIds = this.mPreference.getImageIds();
        if (iconIds == null) {
            iconIds = this.mPreference.getIconIds();
        }
        ArrayList<HashMap<String, Object>> listItem = new ArrayList();
        for (int i = 0; i < entries.length; i++) {
            HashMap<String, Object> map = new HashMap();
            map.put("text", entries[i].toString());
            if (iconIds != null) {
                map.put("image", Integer.valueOf(iconIds[i]));
            }
            listItem.add(map);
        }
        if (iconIds == null || !this.mHasImage) {
            from = new String[]{"text"};
            to = new int[]{R.id.text};
        } else {
            from = new String[]{"image", "text"};
            to = new int[]{R.id.image, R.id.text};
        }
        MySimpleAdapter listItemAdapter = new MySimpleAdapter(context, listItem, getItemResId(), from, to);
        this.mDisplayColumnNum = listItem.size() < 5 ? listItem.size() : 5;
        this.mGridView.setAdapter(listItemAdapter);
        this.mGridView.setNumColumns(entries.length);
        this.mGridView.setChoiceMode(1);
        initGridViewLayoutParam(entries.length);
        reloadPreference();
    }

    protected void initGridViewLayoutParam(int itemNum) {
        this.mGridView.setLayoutParams(new LayoutParams(itemNum * ((int) (((float) Util.sWindowWidth) / (itemNum == this.mDisplayColumnNum ? (float) this.mDisplayColumnNum : ((float) this.mDisplayColumnNum) + 0.5f))), this.mGridViewHeight));
    }

    public void reloadPreference() {
        this.mCurrentIndex = this.mPreference.findIndexOfValue(this.mPreference.getValue());
        if (this.mCurrentIndex != -1) {
            this.mGridView.setItemChecked(this.mCurrentIndex, true);
            return;
        }
        Log.e("GridSettingPopup", "Invalid preference value.");
        this.mPreference.print();
    }

    protected void updateItemView(int position, View item) {
    }

    public void onClick(View v) {
        if (this.mGridView.isEnabled()) {
            int index = ((Integer) v.getTag()).intValue();
            if (this.mCurrentIndex != index || !this.mIgnoreSameItemClick) {
                boolean sameItem = this.mCurrentIndex == index;
                this.mCurrentIndex = index;
                this.mGridView.setItemChecked(index, true);
                this.mPreference.setValueIndex(index);
                if ("pref_camera_scenemode_key".equals(this.mPreference.getKey())) {
                    CameraSettings.setFocusModeSwitching(true);
                } else if ("pref_audio_focus_key".equals(this.mPreference.getKey()) && ((ActivityBase) this.mContext).getCurrentModule().isVideoRecording()) {
                    AudioSystem.setParameters("camcorder_mode=" + this.mPreference.getValue());
                }
                notifyToDispatcher(sameItem);
                AutoLockManager.getInstance(this.mContext).onUserInteraction();
            }
        }
    }

    protected void notifyToDispatcher(boolean sameItem) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(6, 0, 3, this.mPreference.getKey(), this);
        }
    }

    public void show(boolean animate) {
        super.show(animate);
        if ("pref_camera_scenemode_key".equals(this.mPreference.getKey()) && !"auto".equals(this.mPreference.getValue())) {
            CameraSettings.setFocusModeSwitching(true);
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mGridView = (GridView) findViewById(R.id.settings_grid);
        this.mGridViewHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.manual_popup_layout_height);
    }

    public void setOrientation(int orientation, boolean animation) {
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.mGridView != null) {
            this.mGridView.setEnabled(enabled);
        }
    }

    protected int getItemResId() {
        return R.layout.grid_setting_item;
    }
}
