package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.android.camera.Log;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;

public class StereoPopup extends V6AbstractSettingPopup implements OnItemClickListener {
    private static final String TAG = StereoPopup.class.getSimpleName();
    private int mCurrentIndex = -1;
    private Animation mHidenAnimation;
    private int mItemHeight;
    private int mItemWidth;
    private HorizontalListView mListView;
    private Animation mShownAnimation;

    private class AnimationListener extends SimpleAnimationListener {
        public AnimationListener(View view, boolean show) {
            super(view, show);
        }

        public void onAnimationEnd(Animation animation) {
            super.onAnimationEnd(animation);
            StereoPopup.this.getParent().requestLayout();
            int msgId = 0;
            if (StereoPopup.this.mShownAnimation == animation) {
                msgId = 11;
            } else if (StereoPopup.this.mHidenAnimation == animation) {
                msgId = 12;
            }
            if (StereoPopup.this.mMessageDispacher != null && msgId > 0) {
                StereoPopup.this.mMessageDispacher.dispacherMessage(msgId, 0, 0, null, null);
            }
        }
    }

    class HorizontalListViewAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        private class ViewHolder {
            private TextView mTitle;

            private ViewHolder() {
            }
        }

        public HorizontalListViewAdapter() {
            this.mInflater = (LayoutInflater) StereoPopup.this.getContext().getSystemService("layout_inflater");
        }

        public int getCount() {
            return StereoPopup.this.mPreference.getEntries().length;
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public Object getItem(int position) {
            return StereoPopup.this.mPreference.getEntries()[position];
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = this.mInflater.inflate(R.layout.horizontal_list_text_item, null);
                TextView title = (TextView) convertView.findViewById(R.id.text_item_title);
                holder.mTitle = title;
                title.setWidth(StereoPopup.this.mItemWidth);
                title.setHeight(StereoPopup.this.mItemHeight);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mTitle.setText(StereoPopup.this.mPreference.getEntries()[position]);
            return convertView;
        }
    }

    public StereoPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        super.initialize(preferenceGroup, (IconListPreference) preferenceGroup.findPreference("pref_camera_stereo_key"), p);
        this.mPreference.filterValue();
        this.mListView.setAdapter(new HorizontalListViewAdapter());
        this.mListView.setItemWidth(this.mItemWidth);
        reloadPreference();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mListView = (HorizontalListView) findViewById(R.id.horizon_listview);
        this.mItemWidth = getResources().getDimensionPixelSize(R.dimen.stereo_item_width);
        this.mItemHeight = getResources().getDimensionPixelSize(R.dimen.manual_popup_layout_height);
        this.mListView.setOnItemClickListener(this);
    }

    public void reloadPreference() {
        this.mCurrentIndex = this.mPreference.findIndexOfValue(this.mPreference.getValue());
        if (this.mCurrentIndex != -1) {
            this.mListView.setSelection(this.mCurrentIndex);
            return;
        }
        Log.e(TAG, "Invalid preference value.");
        this.mPreference.print();
    }

    private void notifyToDispatcher(boolean sameItem, boolean scrolling) {
        if (this.mMessageDispacher == null) {
            return;
        }
        if (!sameItem || !scrolling) {
            this.mMessageDispacher.dispacherMessage(7, 0, 2, this.mPreference.getKey(), Boolean.valueOf(scrolling));
            reloadPreference();
        }
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        boolean sameItem = this.mCurrentIndex == position;
        this.mPreference.setValueIndex(position);
        this.mCurrentIndex = position;
        notifyToDispatcher(sameItem, this.mListView.isScrolling());
    }

    public void setOrientation(int orientation, boolean animation) {
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mListView.setEnabled(enabled);
    }

    public void show(boolean animate) {
        clearAnimation();
        setVisibility(0);
        if (animate) {
            startAnimation(initAnimation(true));
        }
    }

    public void dismiss(boolean animate) {
        clearAnimation();
        if (animate) {
            startAnimation(initAnimation(false));
        } else {
            setVisibility(8);
        }
    }

    private Animation initAnimation(boolean slideUp) {
        if (slideUp) {
            if (this.mShownAnimation == null) {
                this.mShownAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.slide_up);
                this.mShownAnimation.setAnimationListener(new AnimationListener(this, true));
            }
            return this.mShownAnimation;
        }
        if (this.mHidenAnimation == null) {
            this.mHidenAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.slide_down);
            this.mHidenAnimation.setAnimationListener(new AnimationListener(this, false));
        }
        return this.mHidenAnimation;
    }
}
