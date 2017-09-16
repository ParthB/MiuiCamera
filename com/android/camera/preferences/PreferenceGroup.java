package com.android.camera.preferences;

import android.content.Context;
import android.util.AttributeSet;
import java.util.ArrayList;

public class PreferenceGroup extends CameraPreference {
    private ArrayList<CameraPreference> list = new ArrayList();

    public PreferenceGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void addChild(CameraPreference child) {
        this.list.add(child);
    }

    public void removePreference(int index) {
        this.list.remove(index);
    }

    public CameraPreference get(int index) {
        return (CameraPreference) this.list.get(index);
    }

    public int size() {
        return this.list.size();
    }

    public ListPreference findPreference(String key) {
        for (CameraPreference pref : this.list) {
            ListPreference listPref;
            if (pref instanceof ListPreference) {
                listPref = (ListPreference) pref;
                if (listPref.getKey().equals(key)) {
                    return listPref;
                }
            } else if (pref instanceof PreferenceGroup) {
                listPref = ((PreferenceGroup) pref).findPreference(key);
                if (listPref != null) {
                    return listPref;
                }
            } else {
                continue;
            }
        }
        return null;
    }
}
