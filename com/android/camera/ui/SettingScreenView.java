package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout.LayoutParams;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingScreenView extends SettingView implements AnimationListener {
    private int mColumnCount;
    private Set<String> mDisabledIndicator = new HashSet();
    private Animation mFadeIn;
    private Animation mFadeOut;
    private List<String> mKeys;
    private View mParent;
    private V6AbstractSettingPopup mParentPopup;
    private ViewGroup mPopupRoot;
    private int mRowCount;
    private int mScreenHeight;
    private ScreenView mSettingScreen;
    protected SplitLineDrawer mSplitLineDrawer;

    public SettingScreenView(Context context) {
        super(context);
    }

    public SettingScreenView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SettingScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mSplitLineDrawer = (SplitLineDrawer) findViewById(R.id.split_line_view);
    }

    public void initializeSettingScreen(PreferenceGroup preferenceGroup, List<String> keys, int columns, MessageDispacher p, ViewGroup popupRoot, V6AbstractSettingPopup parentPopup) {
        if (this.mFadeIn != null) {
            this.mFadeIn.setAnimationListener(null);
        }
        if (this.mFadeOut != null) {
            this.mFadeOut.setAnimationListener(null);
        }
        this.mFadeIn = AnimationUtils.loadAnimation(this.mContext, R.anim.screen_setting_fade_in);
        this.mFadeOut = AnimationUtils.loadAnimation(this.mContext, R.anim.screen_setting_fade_out);
        this.mFadeIn.setAnimationListener(this);
        this.mFadeOut.setAnimationListener(this);
        this.mParent = getRootView().findViewById(R.id.setting_view_popup_layout);
        this.mPreferenceGroup = preferenceGroup;
        this.mMessageDispacher = p;
        this.mKeys = keys;
        initScreenView();
        this.mColumnCount = columns;
        this.mSettingScreen = (ScreenView) findViewById(R.id.setting_screens);
        this.mSettingScreen.setLayoutParams(new LayoutParams(-1, this.mScreenHeight));
        this.mSettingScreen.removeAllScreens();
        this.mSettingScreen.setOverScrollRatio(0.0f);
        this.mPopupRoot = popupRoot;
        this.mParentPopup = parentPopup;
        this.mDisabledIndicator.clear();
        ((ViewGroup) getRootView().findViewById(R.id.setting_view_popup_parent)).removeAllViews();
        clearMessageDispatcher();
        this.mIndicators.clear();
        initIndicators(keys);
        initializeSplitLine();
        this.mSettingScreen.setCurrentScreen(0);
    }

    private void clearMessageDispatcher() {
        for (V6AbstractIndicator indicator : this.mIndicators) {
            indicator.onDestroy();
        }
    }

    private void initializeSplitLine() {
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        int viewWidth = w < h ? w : h;
        LayoutParams layout = (LayoutParams) this.mSplitLineDrawer.getLayoutParams();
        layout.height = this.mScreenHeight;
        layout.width = viewWidth;
        this.mSplitLineDrawer.setLayoutParams(layout);
        this.mSplitLineDrawer.initialize(this.mRowCount, getActualColumnCount());
        this.mSplitLineDrawer.setVisibility(0);
    }

    private int getActualColumnCount() {
        if (this.mKeys.size() < this.mColumnCount) {
            return this.mKeys.size();
        }
        return this.mColumnCount;
    }

    private void initScreenView() {
        this.mColumnCount = 4;
        this.mRowCount = 1;
        this.mScreenHeight = getResources().getDimensionPixelSize(R.dimen.settings_screen_height);
    }

    protected void initIndicators(List<String> keys) {
        int screenGridCount = this.mColumnCount * this.mRowCount;
        int screenCount = ((keys.size() - 1) / screenGridCount) + 1;
        int w = (((getResources().getDisplayMetrics().widthPixels - this.mParent.getPaddingLeft()) - this.mParent.getPaddingRight()) - this.mPaddingLeft) - this.mPaddingRight;
        int h = getResources().getDisplayMetrics().heightPixels;
        if (w >= h) {
            w = h;
        }
        int viewWidth = (int) ((((float) w) / ((float) getActualColumnCount())) + 0.5f);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        for (int screenIndex = 0; screenIndex < screenCount; screenIndex++) {
            StaticGridView gridView = new StaticGridView(getContext(), this.mRowCount, getActualColumnCount(), viewWidth, this.mScreenHeight);
            for (int gridIndex = 0; gridIndex < screenGridCount; gridIndex++) {
                int listIndex = gridIndex + ((this.mRowCount * screenIndex) * this.mColumnCount);
                if (listIndex >= keys.size()) {
                    break;
                }
                IconListPreference pref = (IconListPreference) this.mPreferenceGroup.findPreference((String) keys.get(listIndex));
                if (pref != null) {
                    SubScreenIndicatorButton view = (SubScreenIndicatorButton) inflater.inflate(R.layout.sub_screen_indicator_button, gridView, false);
                    view.initialize(pref, this.mMessageDispacher, this.mPopupRoot, viewWidth, -2, this.mPreferenceGroup, this.mParentPopup);
                    this.mIndicators.add(view);
                    view.setContentDescription(pref.getTitle());
                    gridView.addView(view);
                }
            }
            this.mSettingScreen.addView(gridView);
        }
    }

    public void onDestroy() {
        clearMessageDispatcher();
        this.mSettingScreen.removeAllScreens();
        super.onDestroy();
    }

    public void setEnabled(boolean enabled) {
        for (V6AbstractIndicator i : this.mIndicators) {
            if (!this.mDisabledIndicator.contains(i.getKey())) {
                i.setEnabled(enabled);
            }
        }
        super.setEnabled(enabled);
    }

    public void onAnimationStart(Animation animation) {
    }

    public void onAnimationEnd(Animation animation) {
    }

    public void onAnimationRepeat(Animation animation) {
    }

    public void show() {
        if (this.mParent != null) {
            this.mParent.clearAnimation();
            this.mParent.setVisibility(0);
            this.mParent.startAnimation(this.mFadeIn);
        }
    }

    public void dismiss() {
        if (this.mParent != null) {
            this.mParent.clearAnimation();
            this.mParent.startAnimation(this.mFadeOut);
            this.mParent.setVisibility(8);
        }
    }

    public int getVisibility() {
        if (this.mParent != null) {
            return this.mParent.getVisibility();
        }
        return 8;
    }

    public void setVisibility(int visibility) {
        if (visibility == 0) {
            show();
        } else {
            dismiss();
        }
    }

    public void setOrientation(int orientation, boolean animation) {
        if (this.mIndicators != null) {
            for (int i = 0; i < this.mIndicators.size(); i++) {
                ((V6AbstractIndicator) this.mIndicators.get(i)).setOrientation(orientation, animation);
            }
        }
    }
}
