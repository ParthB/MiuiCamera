package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;
import com.android.camera.ActivityBase;
import com.android.camera.Log;
import com.android.camera.effect.EffectController;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModeView extends RelativeLayout implements Rotatable, AnimationListener, MessageDispacher {
    private int mColumnCount;
    protected String mCurrentMode = "mode_none";
    private Set<String> mDisabledIndicator = new HashSet();
    private boolean mEnabled;
    private int mFirstSelectedItem = -1;
    protected ArrayList<V6IndicatorButton> mIndicators = new ArrayList();
    protected boolean mIsAnimating = false;
    private int mItemWidth;
    protected boolean mKeepExitButtonGone = false;
    protected MessageDispacher mMessageDispacher;
    protected int mOrientation = 0;
    protected PreferenceGroup mPreferenceGroup;
    protected ArrayList<Rotatable> mRotatables = new ArrayList();
    private int mRowCount;
    private ScreenView mSettingScreen;

    public ModeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ModeView(Context context) {
        super(context);
    }

    public ModeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mItemWidth = getResources().getDimensionPixelSize(R.dimen.v6_setting_item_width);
    }

    public void setOrientation(int orientation, boolean animation) {
        this.mOrientation = orientation;
        if (this.mIndicators != null) {
            for (int i = 0; i < this.mIndicators.size(); i++) {
                ((V6AbstractIndicator) this.mIndicators.get(i)).setOrientation(orientation, animation);
            }
        }
    }

    public void reloadPreferences() {
        for (V6IndicatorButton b : this.mIndicators) {
            b.setOrientation(this.mOrientation, false);
            b.reloadPreference();
        }
    }

    public void removePopup() {
        for (V6IndicatorButton indicator : this.mIndicators) {
            indicator.removePopup();
            PopupManager.getInstance(this.mContext).removeOnOtherPopupShowedListener(indicator);
        }
    }

    public View getCurrentPopup() {
        for (V6IndicatorButton i : this.mIndicators) {
            if (i.isPopupVisible()) {
                return i.getPopup();
            }
        }
        return null;
    }

    private void resetOtherSettings(V6IndicatorButton but) {
        if (this.mFirstSelectedItem >= 0 && this.mFirstSelectedItem < this.mIndicators.size()) {
            ((V6IndicatorButton) this.mIndicators.get(this.mFirstSelectedItem)).resetSettings();
        }
    }

    public void dissmissAllPopup() {
        for (V6IndicatorButton indicator : this.mIndicators) {
            indicator.onDestroy();
            if (indicator.getPopup() != null) {
                indicator.dismissPopup();
            }
        }
    }

    public void overrideSettings(String... keyvalues) {
        if (keyvalues.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        for (V6IndicatorButton b : this.mIndicators) {
            b.overrideSettings(keyvalues);
        }
    }

    protected void updatePrefence(IconListPreference pref) {
        if ("pref_camera_shader_coloreffect_key".equals(pref.getKey())) {
            pref.setEntries(EffectController.getInstance().getEntries());
            pref.setEntryValues(EffectController.getInstance().getEntryValues());
        }
    }

    public void initializeSettingScreen(PreferenceGroup preferenceGroup, List<String> keys, MessageDispacher p, int columns) {
        this.mPreferenceGroup = preferenceGroup;
        this.mMessageDispacher = p;
        this.mSettingScreen = (ScreenView) findViewById(R.id.setting_screens);
        this.mSettingScreen.setSeekPointResource(R.drawable.screen_view_seek_point_selector);
        LayoutParams seekbarParams = new LayoutParams(-2, -2, 49);
        seekbarParams.setMargins(0, getResources().getDimensionPixelSize(R.dimen.mode_settings_screen_indicator_margin_top), 0, 0);
        this.mSettingScreen.setSeekBarPosition(seekbarParams);
        this.mSettingScreen.removeAllScreens();
        this.mSettingScreen.setOverScrollRatio(0.0f);
        this.mFirstSelectedItem = -1;
        this.mDisabledIndicator.clear();
        dissmissAllPopup();
        removePopup();
        this.mIndicators.clear();
        initIndicators(keys);
        this.mSettingScreen.setCurrentScreen(0);
    }

    private void initScreenView(int keySize) {
        if (keySize < 9) {
            this.mRowCount = ((keySize + 3) - 1) / 3;
            if (keySize >= 3) {
                keySize = 3;
            }
            this.mColumnCount = keySize;
            return;
        }
        this.mRowCount = 3;
        this.mColumnCount = 3;
    }

    protected void initIndicators(List<String> keys) {
        int screenCount = ((keys.size() - 1) / 9) + 1;
        initScreenView(keys.size());
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        for (int screenIndex = 0; screenIndex < screenCount; screenIndex++) {
            ModeGridView gridView = new ModeGridView(getContext(), this.mSettingScreen, this.mRowCount, this.mColumnCount, this.mItemWidth, this.mItemWidth, screenIndex);
            for (int gridIndex = 0; gridIndex < 9; gridIndex++) {
                int listIndex = gridIndex + ((screenIndex * 3) * 3);
                if (listIndex >= keys.size()) {
                    break;
                }
                IconListPreference pref = (IconListPreference) this.mPreferenceGroup.findPreference((String) keys.get(listIndex));
                if (pref != null) {
                    updatePrefence(pref);
                    V6IndicatorButton b = (V6IndicatorButton) inflater.inflate(R.layout.v6_indicator_button, gridView, false);
                    b.initialize(pref, this, ((ActivityBase) this.mContext).getUIController().getPopupParent(), this.mItemWidth, this.mItemWidth, this.mPreferenceGroup);
                    b.setMessageDispacher(this);
                    if (b.isItemSelected()) {
                        this.mFirstSelectedItem = this.mIndicators.size();
                    }
                    gridView.addView(b);
                    this.mIndicators.add(b);
                }
            }
            this.mSettingScreen.addView(gridView);
        }
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
        for (V6IndicatorButton i : this.mIndicators) {
            if (!this.mDisabledIndicator.contains(i.getKey())) {
                i.setEnabled(enabled);
            }
        }
        super.setEnabled(enabled);
    }

    public boolean resetSettings() {
        if (this.mFirstSelectedItem == -1 || this.mFirstSelectedItem >= this.mIndicators.size()) {
            return false;
        }
        ((V6IndicatorButton) this.mIndicators.get(this.mFirstSelectedItem)).resetSettings();
        return true;
    }

    public void onAnimationStart(Animation animation) {
    }

    public void onAnimationEnd(Animation animation) {
    }

    public void onAnimationRepeat(Animation animation) {
    }

    public boolean isItemSelected() {
        return this.mFirstSelectedItem != -1;
    }

    public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
        Log.v("Camera5", "ModeView dispacherMessage mEnabled=" + this.mEnabled + " what=" + what + " sender=" + sender + " receiver=" + receiver + " extra1=" + extra1 + " extra2=" + extra2 + " getVisibility()=" + getVisibility() + " mEnabled=" + this.mEnabled);
        if (what == 8) {
            resetOtherSettings((V6IndicatorButton) extra2);
            return true;
        }
        ((ActivityBase) this.mContext).getUIController().getPreviewPage().onPopupChange();
        if (what == 6 && (extra2 instanceof V6IndicatorButton)) {
            if (((V6IndicatorButton) extra2).isItemSelected()) {
                this.mFirstSelectedItem = this.mIndicators.indexOf(extra2);
            } else {
                this.mFirstSelectedItem = -1;
            }
        }
        this.mMessageDispacher.dispacherMessage(what, R.id.v6_setting_page, 2, extra1, extra2);
        if (extra2 instanceof V6IndicatorButton) {
            Log.v("Camera5", "call indicatorbutton reloadPreference");
            ((V6IndicatorButton) extra2).reloadPreference();
        }
        return false;
    }

    public void resetSelectedFlag() {
        this.mFirstSelectedItem = -1;
    }
}
