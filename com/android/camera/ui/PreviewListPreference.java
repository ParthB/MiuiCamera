package com.android.camera.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.ListPreference;
import android.support.v7.recyclerview.R;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.camera.R$styleable;
import java.util.ArrayList;
import java.util.List;

public class PreviewListPreference extends ListPreference {
    private CharSequence[] mDefaultValues;
    private int mExtraPaddingEnd;
    private CharSequence[] mLabels;

    class PreviewListAdapter implements ListAdapter {
        private ListAdapter mAdapter;
        private int mPaddingEnd;

        public PreviewListAdapter(ListAdapter adapter) {
            this.mAdapter = adapter;
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            this.mAdapter.registerDataSetObserver(observer);
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            this.mAdapter.unregisterDataSetObserver(observer);
        }

        public int getCount() {
            return this.mAdapter.getCount();
        }

        public Object getItem(int position) {
            return this.mAdapter.getItem(position);
        }

        public long getItemId(int position) {
            return this.mAdapter.getItemId(position);
        }

        public boolean hasStableIds() {
            return this.mAdapter.hasStableIds();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) this.mAdapter.getView(position, convertView, parent);
            if (this.mPaddingEnd == 0) {
                this.mPaddingEnd = view.getPaddingEnd() + PreviewListPreference.this.mExtraPaddingEnd;
            }
            view.setSingleLine(false);
            view.setPadding(view.getPaddingStart(), view.getPaddingTop(), this.mPaddingEnd, view.getPaddingBottom());
            return view;
        }

        public int getItemViewType(int position) {
            return this.mAdapter.getItemViewType(position);
        }

        public int getViewTypeCount() {
            return this.mAdapter.getViewTypeCount();
        }

        public boolean isEmpty() {
            return this.mAdapter.isEmpty();
        }

        public boolean areAllItemsEnabled() {
            return this.mAdapter.areAllItemsEnabled();
        }

        public boolean isEnabled(int position) {
            return this.mAdapter.isEnabled(position);
        }
    }

    public PreviewListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.mDefaultValues != null) {
            setDefaultValue(findSupportedDefaultValue(this.mDefaultValues));
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.PreviewListPreference, 0, 0);
        this.mLabels = a.getTextArray(0);
        this.mExtraPaddingEnd = context.getResources().getDimensionPixelSize(R.dimen.preference_entry_padding_end);
        a.recycle();
    }

    public PreviewListPreference(Context context) {
        this(context, null);
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

    protected void onBindView(View view) {
        super.onBindView(view);
        TextView valueView = (TextView) view.findViewById(R.id.value_right);
        if (valueView != null) {
            CharSequence value;
            if (this.mLabels == null) {
                value = getEntry();
            } else {
                value = getLabel();
            }
            if (TextUtils.isEmpty(value)) {
                valueView.setVisibility(8);
                return;
            }
            valueView.setText(String.valueOf(value));
            valueView.setVisibility(0);
        }
    }

    protected View onCreateView(ViewGroup parent) {
        return LayoutInflater.from(getContext()).inflate(R.layout.preference_value_list, parent, false);
    }

    public CharSequence getLabel() {
        int index = findIndexOfValue(getValue());
        if (index < 0 || this.mLabels == null) {
            return null;
        }
        return this.mLabels[index];
    }

    protected void showDialog(Bundle state) {
        super.showDialog(state);
        ListView listView = ((AlertDialog) getDialog()).getListView();
        int checkedItem = listView.getCheckedItemPosition();
        listView.setAdapter(new PreviewListAdapter(listView.getAdapter()));
        if (checkedItem > -1) {
            listView.setItemChecked(checkedItem, true);
            listView.setSelection(checkedItem);
        }
    }
}
