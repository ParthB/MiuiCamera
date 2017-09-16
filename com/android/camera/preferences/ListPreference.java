package com.android.camera.preferences;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import com.android.camera.R$styleable;
import com.android.camera.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ListPreference extends CameraPreference {
    private final CharSequence[] mDefaultValues;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private final boolean mHasPopup;
    private final String mKey;
    private String mValue;

    public ListPreference(Context context, AttributeSet attrs) {
        boolean z;
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.ListPreference, 0, 0);
        this.mKey = (String) Util.checkNotNull(a.getString(0));
        String popup = a.getString(4);
        if (popup == null) {
            z = false;
        } else {
            z = Boolean.valueOf(popup).booleanValue();
        }
        this.mHasPopup = z;
        TypedValue tv = a.peekValue(1);
        if (tv == null || tv.type != 1) {
            this.mDefaultValues = new CharSequence[1];
            this.mDefaultValues[0] = a.getString(1);
        } else {
            this.mDefaultValues = a.getTextArray(1);
        }
        setEntries(a.getTextArray(3));
        setEntryValues(a.getTextArray(2));
        a.recycle();
    }

    public String getKey() {
        return this.mKey;
    }

    public boolean hasPopup() {
        return this.mHasPopup;
    }

    public CharSequence[] getEntries() {
        return this.mEntries;
    }

    public CharSequence[] getEntryValues() {
        return this.mEntryValues;
    }

    public void setEntries(CharSequence[] entries) {
        if (entries == null) {
            entries = new CharSequence[0];
        }
        this.mEntries = entries;
    }

    public void setEntryValues(CharSequence[] values) {
        if (values == null) {
            values = new CharSequence[0];
        }
        this.mEntryValues = values;
    }

    public void setEntryValues(int entryValuesResId) {
        setEntryValues(this.mContext.getResources().getTextArray(entryValuesResId));
    }

    public String getValue() {
        this.mValue = getSharedPreferences().getString(this.mKey, findSupportedDefaultValue());
        return this.mValue;
    }

    public boolean isDefaultValue() {
        String defaultValue = findSupportedDefaultValue();
        this.mValue = getSharedPreferences().getString(this.mKey, defaultValue);
        return Objects.equals(defaultValue, this.mValue);
    }

    public String findSupportedDefaultValue() {
        for (int i = 0; i < this.mDefaultValues.length; i++) {
            for (Object equals : this.mEntryValues) {
                if (equals.equals(this.mDefaultValues[i])) {
                    return this.mDefaultValues[i].toString();
                }
            }
        }
        return null;
    }

    public void setValue(String value) {
        if (findIndexOfValue(value) < 0) {
            throw new IllegalArgumentException();
        }
        this.mValue = value;
        persistStringValue(value);
    }

    public void setValueIndex(int index) {
        setValue(this.mEntryValues[index].toString());
    }

    public void filterValue() {
        if (findIndexOfValue(getValue()) < 0) {
            Log.e("ListPreference", "filterValue index < 0, value=" + getValue());
            print();
            setValueIndex(0);
        }
    }

    public int findIndexOfValue(String value) {
        int n = this.mEntryValues.length;
        for (int i = 0; i < n; i++) {
            if (Util.equals(this.mEntryValues[i], value)) {
                return i;
            }
        }
        return -1;
    }

    public String getEntry() {
        int index = findIndexOfValue(getValue());
        if (index < 0) {
            Log.e("ListPreference", "getEntry index=" + index);
            print();
            setValue(findSupportedDefaultValue());
            index = findIndexOfValue(getValue());
        }
        return this.mEntries[index].toString();
    }

    protected void persistStringValue(String value) {
        Editor editor = getSharedPreferences().edit();
        editor.putString(this.mKey, value);
        editor.apply();
    }

    public void filterUnsupported(List<String> supported) {
        ArrayList<CharSequence> entries = new ArrayList();
        ArrayList<CharSequence> entryValues = new ArrayList();
        int len = this.mEntryValues.length;
        for (int i = 0; i < len; i++) {
            if (supported.indexOf(this.mEntryValues[i].toString()) >= 0) {
                entries.add(this.mEntries[i]);
                entryValues.add(this.mEntryValues[i]);
            }
        }
        int size = entries.size();
        this.mEntries = (CharSequence[]) entries.toArray(new CharSequence[size]);
        this.mEntryValues = (CharSequence[]) entryValues.toArray(new CharSequence[size]);
    }

    public void print() {
        int i;
        Log.v("ListPreference", "Preference key=" + getKey() + ". value=" + getValue());
        for (i = 0; i < this.mEntryValues.length; i++) {
            Log.v("ListPreference", "entryValues[" + i + "]=" + this.mEntryValues[i]);
        }
        for (i = 0; i < this.mEntries.length; i++) {
            Log.v("ListPreference", "mEntries[" + i + "]=" + this.mEntries[i]);
        }
        for (i = 0; i < this.mDefaultValues.length; i++) {
            Log.v("ListPreference", "mDefaultValues[" + i + "]=" + this.mDefaultValues[i]);
        }
    }
}
