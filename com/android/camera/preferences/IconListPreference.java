package com.android.camera.preferences;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import com.android.camera.IntArray;
import com.android.camera.R$styleable;
import java.util.List;

public class IconListPreference extends ListPreference {
    private boolean mEnabled = true;
    private int[] mIconIds;
    private int[] mImageIds;
    private int[] mLargeIconIds;
    private int mSingleIconId;

    public IconListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.IconListPreference, 0, 0);
        Resources res = context.getResources();
        this.mSingleIconId = a.getResourceId(1, 0);
        this.mIconIds = getIds(res, a.getResourceId(0, 0));
        this.mLargeIconIds = getIds(res, a.getResourceId(2, 0));
        this.mImageIds = getIds(res, a.getResourceId(3, 0));
        a.recycle();
    }

    public int getSingleIcon() {
        return this.mSingleIconId;
    }

    public int[] getIconIds() {
        return this.mIconIds;
    }

    public int[] getImageIds() {
        return this.mImageIds;
    }

    public void setIconIds(int[] iconIds) {
        this.mIconIds = iconIds;
    }

    public void setIconRes(int resId) {
        this.mIconIds = getIds(this.mContext.getResources(), resId);
    }

    public boolean getEnable() {
        return this.mEnabled;
    }

    private int[] getIds(Resources res, int iconsRes) {
        if (iconsRes == 0) {
            return null;
        }
        TypedArray array = res.obtainTypedArray(iconsRes);
        int n = array.length();
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            ids[i] = array.getResourceId(i, 0);
        }
        array.recycle();
        return ids;
    }

    public void filterUnsupported(List<String> supported) {
        CharSequence[] entryValues = getEntryValues();
        IntArray iconIds = new IntArray();
        IntArray largeIconIds = new IntArray();
        IntArray imageIds = new IntArray();
        int len = entryValues.length;
        for (int i = 0; i < len; i++) {
            if (supported.indexOf(entryValues[i].toString()) >= 0) {
                if (this.mIconIds != null) {
                    iconIds.add(this.mIconIds[i]);
                }
                if (this.mLargeIconIds != null) {
                    largeIconIds.add(this.mLargeIconIds[i]);
                }
                if (this.mImageIds != null) {
                    imageIds.add(this.mImageIds[i]);
                }
            }
        }
        if (this.mIconIds != null) {
            this.mIconIds = iconIds.toArray(new int[iconIds.size()]);
        }
        if (this.mLargeIconIds != null) {
            this.mLargeIconIds = largeIconIds.toArray(new int[largeIconIds.size()]);
        }
        if (this.mImageIds != null) {
            this.mImageIds = imageIds.toArray(new int[imageIds.size()]);
        }
        super.filterUnsupported(supported);
    }
}
