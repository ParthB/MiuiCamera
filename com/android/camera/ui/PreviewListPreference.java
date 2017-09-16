package com.android.camera.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import java.util.ArrayList;
import java.util.List;

public class PreviewListPreference extends ListPreference {
    private CharSequence[] mDefaultValues;

    public PreviewListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.mDefaultValues != null) {
            setDefaultValue(findSupportedDefaultValue(this.mDefaultValues));
        }
    }

    public PreviewListPreference(Context context) {
        this(context, null);
    }

    public void setValue(String value) {
        super.setValue(value);
        setSummary(getEntry());
    }

    protected Object onGetDefaultValue(TypedArray a, int index) {
        TypedValue tv = a.peekValue(index);
        if (tv != null && tv.type == 1) {
            this.mDefaultValues = a.getTextArray(index);
        }
        return this.mDefaultValues != null ? this.mDefaultValues[0] : a.getString(index);
    }

    public void setEntryValues(CharSequence[] entryValues) {
        super.setEntryValues(entryValues);
        if (this.mDefaultValues != null) {
            setDefaultValue(findSupportedDefaultValue(this.mDefaultValues));
        }
    }

    private CharSequence findSupportedDefaultValue(CharSequence[] values) {
        CharSequence[] supportedValues = getEntryValues();
        if (supportedValues == null) {
            return null;
        }
        for (CharSequence sv : supportedValues) {
            for (CharSequence v : values) {
                if (sv != null && sv.equals(v)) {
                    return v;
                }
            }
        }
        return null;
    }

    public void filterUnsupported(List<String> supported) {
        CharSequence[] oldEntries = getEntries();
        CharSequence[] oldEntryValues = getEntryValues();
        ArrayList<CharSequence> entries = new ArrayList();
        ArrayList<CharSequence> entryValues = new ArrayList();
        int len = oldEntries.length;
        for (int i = 0; i < len; i++) {
            if (supported.indexOf(oldEntryValues[i].toString()) >= 0) {
                entries.add(oldEntries[i]);
                entryValues.add(oldEntryValues[i]);
            }
        }
        int size = entries.size();
        setEntries((CharSequence[]) entries.toArray(new CharSequence[size]));
        setEntryValues((CharSequence[]) entryValues.toArray(new CharSequence[size]));
    }
}
