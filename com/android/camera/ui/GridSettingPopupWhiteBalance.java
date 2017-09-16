package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraManager;
import com.android.camera.CameraManager.CameraProxy;
import com.android.camera.CameraSettings;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;

public class GridSettingPopupWhiteBalance extends GridSettingPopup implements OnItemClickListener {
    private static String sWhiteBalanceManual;
    private static String sWhiteBalanceMeasure;
    private View mContentView;
    private int mCurrentKValue = -1;
    private int mItemHeight;
    private int mItemWidth;
    private NumericListAdapter mKItemAdapter;
    private HorizontalListView mListView;
    private OnClickListener mOnBackListener = new OnClickListener() {
        public void onClick(View v) {
            if (GridSettingPopupWhiteBalance.this.mListView.getVisibility() == 0) {
                GridSettingPopupWhiteBalance.this.reloadPreference();
                GridSettingPopupWhiteBalance.this.mListView.setVisibility(8);
                GridSettingPopupWhiteBalance.this.mGridView.setVisibility(0);
            }
        }
    };

    class HorizontalListViewAdapter extends BaseAdapter {
        private CharSequence[] mEntries;
        private LayoutInflater mInflater;
        private NumericListAdapter mNumAdapter;

        private class ViewHolder {
            private TextView mTitle;

            private ViewHolder() {
            }
        }

        public HorizontalListViewAdapter(NumericListAdapter numAdapter) {
            this.mInflater = (LayoutInflater) GridSettingPopupWhiteBalance.this.getContext().getSystemService("layout_inflater");
            this.mNumAdapter = numAdapter;
        }

        public int getCount() {
            if (this.mEntries != null) {
                return this.mEntries.length;
            }
            if (this.mNumAdapter != null) {
                return this.mNumAdapter.getItemsCount();
            }
            return 0;
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public Object getItem(int position) {
            if (this.mEntries != null) {
                return this.mEntries[position];
            }
            if (this.mNumAdapter != null) {
                return this.mNumAdapter.getItem(position);
            }
            return null;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = this.mInflater.inflate(R.layout.horizontal_list_text_item, null);
                TextView title = (TextView) convertView.findViewById(R.id.text_item_title);
                holder.mTitle = title;
                title.setWidth(GridSettingPopupWhiteBalance.this.mItemWidth);
                title.setHeight(GridSettingPopupWhiteBalance.this.mItemHeight);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mTitle.setText((CharSequence) getItem(position));
            return convertView;
        }
    }

    public GridSettingPopupWhiteBalance(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (sWhiteBalanceManual == null || "".equals(sWhiteBalanceManual)) {
            sWhiteBalanceManual = this.mContext.getString(R.string.pref_camera_whitebalance_entryvalue_manual);
            sWhiteBalanceMeasure = this.mContext.getString(R.string.pref_camera_whitebalance_entryvalue_measure);
        }
    }

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        super.initialize(preferenceGroup, preference, p);
        this.mKItemAdapter = new NumericListAdapter(2000, 8000, 100);
        this.mListView.setAdapter(new HorizontalListViewAdapter(this.mKItemAdapter));
        this.mListView.setItemWidth(this.mItemWidth);
    }

    public void onClick(View v) {
        if (this.mGridView.isEnabled()) {
            int index = ((Integer) v.getTag()).intValue();
            this.mGridView.setItemChecked(index, true);
            this.mPreference.setValueIndex(index);
            boolean notifyChange = this.mCurrentIndex != index;
            if (sWhiteBalanceManual.equals(this.mPreference.getValue())) {
                this.mCurrentKValue = CameraSettings.getKValue();
                try {
                    CameraProxy cameraProxy = CameraManager.instance().getCameraProxy();
                    if (cameraProxy != null) {
                        int deviceWB = cameraProxy.getWBCT();
                        if (deviceWB != 0) {
                            this.mCurrentKValue = deviceWB;
                            Log.v("Camera", " Current WB CCT = " + this.mCurrentKValue);
                        }
                    }
                } catch (Exception e) {
                    Log.e("Camera", "Can't get current WB CCT");
                }
                int kIndex = this.mKItemAdapter.getItemIndexByValue(Integer.valueOf(this.mCurrentKValue));
                if (kIndex != -1) {
                    this.mListView.setSelection(kIndex);
                    this.mCurrentKValue = this.mKItemAdapter.getItemValue(kIndex);
                }
                this.mListView.setVisibility(0);
                this.mGridView.setVisibility(4);
                CameraSettings.setKValue(this.mCurrentKValue);
                CameraDataAnalytics.instance().trackEvent("manual_whitebalance_key");
            } else if (sWhiteBalanceMeasure.equals(this.mPreference.getValue())) {
                this.mListView.setVisibility(8);
                this.mGridView.setVisibility(0);
                this.mCurrentIndex = index;
                return;
            } else {
                this.mListView.setVisibility(8);
                this.mGridView.setVisibility(0);
            }
            if (notifyChange) {
                this.mCurrentIndex = index;
                if (this.mMessageDispacher != null) {
                    this.mMessageDispacher.dispacherMessage(6, 0, 3, this.mPreference.getKey(), this);
                }
            }
            AutoLockManager.getInstance(this.mContext).onUserInteraction();
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mListView = (HorizontalListView) findViewById(R.id.horizon_listview);
        this.mItemWidth = getResources().getDimensionPixelSize(R.dimen.whitebalance_item_width);
        this.mItemHeight = getResources().getDimensionPixelSize(R.dimen.manual_popup_layout_height);
        this.mListView.setOnItemClickListener(this);
        this.mContentView = findViewById(R.id.content_layout);
        this.mContentView.setOnClickListener(this.mOnBackListener);
    }

    public void show(boolean animate) {
        super.show(animate);
        if (this.mListView.getVisibility() == 0) {
            this.mListView.setVisibility(8);
            this.mGridView.setVisibility(0);
        }
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        int newKValue = this.mKItemAdapter.getItemValue(position);
        boolean sameItem = newKValue == this.mCurrentKValue;
        this.mCurrentKValue = newKValue;
        CameraSettings.setKValue(this.mCurrentKValue);
        notifyToDispatcher(sameItem, this.mListView.isScrolling());
        this.mListView.setSelection(position);
        AutoLockManager.getInstance(this.mContext).onUserInteraction();
    }

    private void notifyToDispatcher(boolean sameItem, boolean scrolling) {
        if (this.mMessageDispacher == null) {
            return;
        }
        if (!sameItem || !scrolling) {
            this.mMessageDispacher.dispacherMessage(7, 0, 3, "pref_qc_manual_whitebalance_k_value_key", Boolean.valueOf(scrolling));
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.mListView != null) {
            this.mListView.setEnabled(enabled);
        }
    }
}
