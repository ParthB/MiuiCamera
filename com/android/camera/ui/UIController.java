package com.android.camera.ui;

import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.recyclerview.R;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.android.camera.Camera;
import com.android.camera.Device;
import com.android.camera.Thumbnail;
import com.android.camera.Util;
import com.android.camera.effect.EffectController;
import com.android.camera.preferences.PreferenceGroup;
import com.android.camera.preferences.PreferenceInflater;

public class UIController extends V6ModuleUI implements MessageDispacher {
    public BottomControlPanel mBottomControlPanel;
    public TextView mDebugInfoView;
    public V6EdgeShutterView mEdgeShutterView;
    private View mFrontCameraHintView;
    public V6CameraGLSurfaceView mGLView;
    private TextView mHibernateHintView;
    private MutexView mLastMutexView;
    private View mMainContent;
    private Runnable mMutexRecover = new Runnable() {
        public void run() {
            if (UIController.this.mLastMutexView != null) {
                UIController.this.mLastMutexView.show();
                UIController.this.mLastMutexView = null;
            } else {
                UIController.this.mPreviewPage.updateOrientationLayout(false);
            }
            UIController.this.getPortraitButton().show();
        }
    };
    private View mPortraitUseHintView;
    private PreferenceGroup mPreferenceGroup;
    public V6PreviewPage mPreviewPage;
    public V6PreviewPanel mPreviewPanel;
    public V6SettingPage mSettingPage;
    public V6SettingsStatusBar mSettingsStatusBar;
    public V6SmartShutterButton mSmartShutterButton;
    public V6SurfaceViewFrame mSurfaceViewParent;
    public TopControlPanel mTopControlPanel;

    public UIController(Camera activity) {
        super(activity);
    }

    public void onCreate() {
        this.mBottomControlPanel = (BottomControlPanel) findViewById(R.id.bottom_control_panel);
        this.mTopControlPanel = (TopControlPanel) findViewById(R.id.top_control_panel);
        this.mSettingPage = (V6SettingPage) findViewById(R.id.v6_setting_page);
        this.mPreviewPage = (V6PreviewPage) findViewById(R.id.v6_preview_page);
        this.mPreviewPanel = (V6PreviewPanel) findViewById(R.id.v6_preview_panel);
        this.mSmartShutterButton = (V6SmartShutterButton) findViewById(R.id.v6_smart_shutter_button);
        this.mSettingsStatusBar = (V6SettingsStatusBar) findViewById(R.id.v6_setting_status_bar);
        this.mGLView = (V6CameraGLSurfaceView) findViewById(R.id.v6_gl_surface_view);
        this.mSurfaceViewParent = (V6SurfaceViewFrame) findViewById(R.id.v6_surface_view_parent);
        this.mEdgeShutterView = (V6EdgeShutterView) findViewById(R.id.edge_shutter_view);
        this.mDebugInfoView = (TextView) findViewById(R.id.camera_debug_content);
        this.mMainContent = findViewById(R.id.main_content);
        this.mHibernateHintView = (TextView) findViewById(R.id.hibernate_hint_view);
        this.mPortraitUseHintView = findViewById(R.id.portrait_use_hint_cover);
        this.mFrontCameraHintView = findViewById(R.id.front_camera_hint_cover);
        this.mGLView.setVisibility(0);
        setMessageDispacher(this);
        updatePreferenceGroup();
        super.onCreate();
        if (Util.checkDeviceHasNavigationBar(this.mActivity)) {
            ViewCompat.setOnApplyWindowInsetsListener(this.mMainContent, new OnApplyWindowInsetsListener() {
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    int insetBottom = insets.getSystemWindowInsetBottom();
                    RelativeLayout lowerControlGroup = UIController.this.getBottomControlLowerGroup();
                    LayoutParams lp = (LayoutParams) lowerControlGroup.getLayoutParams();
                    if (Device.is18x9RatioScreen() && insetBottom == 0) {
                        insetBottom = Util.getNavigationBarHeight(UIController.this.mActivity);
                    }
                    lp.bottomMargin = insetBottom;
                    lowerControlGroup.requestLayout();
                    if (V6ModulePicker.isPanoramaModule()) {
                        UIController.this.getModeExitView().setLayoutParameters(0, UIController.this.mActivity.getResources().getDimensionPixelSize(R.dimen.pano_mode_exit_button_margin_bottom) - (insetBottom > 0 ? 0 : Util.getNavigationBarHeight(UIController.this.mActivity) / 2));
                    }
                    return ViewCompat.onApplyWindowInsets(v, insets);
                }
            });
        }
        if (Device.is18x9RatioScreen()) {
            this.mBottomControlPanel.findViewById(R.id.top_control_bottom_line).setVisibility(4);
        }
    }

    public void onCameraOpen() {
        super.onCameraOpen();
        getPreviewPage().updatePopupIndicator();
    }

    public void onPause() {
        super.onPause();
        this.mLastMutexView = null;
    }

    public void onResume() {
        super.onResume();
        EffectController.getInstance().setBlurEffect(false);
        getHibernateHintView().setVisibility(8);
    }

    public boolean handleMessage(int what, int sender, Object extra1, Object extra2) {
        if (what != 4) {
            switch (sender) {
                case R.id.show_mode_animation_done:
                    onShowModeSetting();
                    return true;
                case R.id.hide_mode_animation_done:
                    onDismissModeSetting();
                    return true;
                case R.id.mode_button:
                    showModeSetting();
                    return true;
                case R.id.v6_frame_layout:
                    getModeExitView().updateBackground();
                    return true;
                case R.id.v6_focus_view:
                    if (what != 2) {
                        return false;
                    }
                    getEffectCropView().removeTiltShiftMask();
                    return this.mActivity.getCurrentModule().handleMessage(what, sender, extra1, extra2);
                case R.id.dismiss_setting:
                    hideModeSetting();
                    return true;
                default:
                    break;
            }
        } else if (((Boolean) extra1).booleanValue()) {
            onMutexViewShow(sender);
        } else {
            onMutexViewHide(sender);
        }
        return false;
    }

    private void onMutexViewShow(int sender) {
        switch (sender) {
            case R.id.v6_flash_mode_button:
            case R.id.v6_hdr:
            case R.id.skin_beatify_button:
                if (getSettingsStatusBar().isSubViewShown()) {
                    this.mLastMutexView = getSettingsStatusBar();
                    this.mLastMutexView.hide();
                } else {
                    this.mLastMutexView = null;
                }
                getPortraitButton().hide();
                break;
            case R.id.v6_setting_status_bar:
                getTopPopupParent().dismissAllPopup(true);
                getPortraitButton().hide();
                break;
        }
        this.mPreviewPage.removeCallbacks(this.mMutexRecover);
        this.mPreviewPage.updateOrientationLayout(true);
    }

    private void onMutexViewHide(int sender) {
        switch (sender) {
            case R.id.v6_flash_mode_button:
            case R.id.v6_hdr:
            case R.id.skin_beatify_button:
                this.mPreviewPage.postDelayed(this.mMutexRecover, 150);
                return;
            case R.id.v6_setting_status_bar:
                this.mPreviewPage.updateOrientationLayout(false);
                getPortraitButton().show();
                return;
            default:
                return;
        }
    }

    public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
        switch (receiver) {
            case 1:
                return this.mActivity.handleMessage(what, sender, extra1, extra2);
            case 2:
                return this.mActivity.getCurrentModule().handleMessage(what, sender, extra1, extra2);
            case 3:
                return handleMessage(what, sender, extra1, extra2);
            default:
                return false;
        }
    }

    public void updatePreferenceGroup() {
        int i;
        PreferenceInflater inflater = new PreferenceInflater(this.mActivity);
        if (V6ModulePicker.isVideoModule()) {
            i = R.xml.video_preferences;
        } else {
            i = R.xml.camera_preferences;
        }
        this.mPreferenceGroup = (PreferenceGroup) inflater.inflate(i);
    }

    public TopControlPanel getTopControlPanel() {
        return this.mTopControlPanel;
    }

    public BottomControlPanel getBottomControlPanel() {
        return this.mBottomControlPanel;
    }

    public BottomControlLowerPanel getBottomControlLowerPanel() {
        return this.mBottomControlPanel.mLowerPanel;
    }

    public RelativeLayout getBottomControlLowerGroup() {
        return this.mBottomControlPanel.getLowerGroup();
    }

    public V6ShutterButton getShutterButton() {
        return getBottomControlLowerPanel().getShutterButton();
    }

    public void updateThumbnailView(Thumbnail t, boolean needAnimation) {
        getBottomControlLowerPanel().updateThumbnailView(t, needAnimation);
    }

    public V6ThumbnailButton getThumbnailButton() {
        return getBottomControlLowerPanel().getThumbnailButton();
    }

    public View getCaptureProgressBar() {
        return getBottomControlLowerPanel().getProgressBar();
    }

    public V6ModulePicker getModulePicker() {
        return getBottomControlLowerPanel().getModulePicker();
    }

    public V6BottomAnimationImageView getReviewDoneView() {
        return this.mBottomControlPanel.getReviewDoneView();
    }

    public V6BottomAnimationImageView getReviewCanceledView() {
        return this.mBottomControlPanel.getReviewCanceledView();
    }

    public V6PauseRecordingButton getPauseRecordingButton() {
        return getBottomControlLowerPanel().getVideoPauseButton();
    }

    public HdrButton getHdrButton() {
        return this.mTopControlPanel.getHdrButton();
    }

    public SkinBeautyButton getSkinBeautyButton() {
        return this.mTopControlPanel.getSkinBeautyButton();
    }

    public PeakButton getPeakButton() {
        return this.mTopControlPanel.getPeakButton();
    }

    public V6SurfaceViewFrame getSurfaceViewFrame() {
        return this.mSurfaceViewParent;
    }

    public V6PreviewPage getPreviewPage() {
        return this.mPreviewPage;
    }

    public V6SettingPage getSettingPage() {
        return this.mSettingPage;
    }

    public V6SettingButton getSettingButton() {
        return this.mSettingPage.mSettingButton;
    }

    public OrientationIndicator getOrientationIndicator() {
        return this.mPreviewPage.mLyingOriFlag;
    }

    public V6PreviewPanel getPreviewPanel() {
        return this.mPreviewPanel;
    }

    public V6RecordingTimeView getVideoRecordingTimeView() {
        return this.mPreviewPanel.mVideoRecordingTimeView;
    }

    public ImageView getAsdIndicator() {
        return this.mPreviewPage.mAsdIndicatorView;
    }

    public StereoButton getStereoButton() {
        return this.mPreviewPage.mStereoButton;
    }

    public ZoomButton getZoomButton() {
        return this.mPreviewPage.mZoomButton;
    }

    public PortraitButton getPortraitButton() {
        return this.mPreviewPage.mPortraitButton;
    }

    public TextView getPortraitHintTextView() {
        return this.mPreviewPage.mPortraitHintTextView;
    }

    public FaceView getFaceView() {
        return this.mPreviewPanel.mFaceView;
    }

    public ObjectView getObjectView() {
        return this.mPreviewPanel.mObjectView;
    }

    public V6EffectCropView getEffectCropView() {
        return this.mPreviewPanel.mCropView;
    }

    public ViewGroup getPopupParentLayout() {
        return this.mPreviewPage.mPopupParentLayout;
    }

    public ViewGroup getPopupParent() {
        return this.mPreviewPage.mPopupParent;
    }

    public TopPopupParent getTopPopupParent() {
        return this.mPreviewPage.mTopPopupParent;
    }

    public V6EdgeShutterView getEdgeShutterView() {
        return this.mEdgeShutterView;
    }

    public V6CameraGLSurfaceView getGLView() {
        return this.mGLView;
    }

    public V6PreviewFrame getPreviewFrame() {
        return this.mPreviewPanel.mPreviewFrame;
    }

    public RotateTextView getMultiSnapNum() {
        return this.mPreviewPanel.mMultiSnapNum;
    }

    public FocusView getFocusView() {
        return this.mPreviewPanel.mFocusView;
    }

    public ImageView getReviewImageView() {
        return this.mPreviewPanel.mVideoReviewImage;
    }

    public RotateImageView getReviewPlayView() {
        return this.mPreviewPanel.mVideoReviewPlay;
    }

    public V6ModeExitView getModeExitView() {
        return this.mPreviewPage.mModeExitView;
    }

    public TextView getWarningMessageView() {
        return this.mPreviewPage.mWarningView;
    }

    public LinearLayout getWarningMessageParent() {
        return this.mPreviewPage.mWarningMessageLayout;
    }

    public RelativeLayout getPanoramaViewRoot() {
        return this.mPreviewPage.mPanoramaViewRoot;
    }

    public View getPopupIndicatorLayout() {
        return this.mPreviewPage.mPopupIndicatorLayout;
    }

    public BottomControlUpperPanel getBottomControlUpperPanel() {
        return this.mBottomControlPanel.mUpperPanel;
    }

    public ModeButton getModeButton() {
        return this.mBottomControlPanel.mUpperPanel.mModeButton;
    }

    public FlashButton getFlashButton() {
        return this.mTopControlPanel.getFlashButton();
    }

    public EffectButton getEffectButton() {
        return getBottomControlUpperPanel().getEffectButton();
    }

    public V6VideoCaptureButton getVideoCaptureButton() {
        return getBottomControlLowerPanel().getVideoCaptureButton();
    }

    public void useProperView() {
        if (Device.isMDPRender() && V6ModulePicker.isVideoModule()) {
            getSurfaceViewFrame().initSurfaceView();
        } else {
            getGLView().setVisibility(0);
        }
    }

    public void showDebugView() {
        if (this.mDebugInfoView != null) {
            this.mDebugInfoView.setVisibility(0);
        }
    }

    public void showDebugInfo(String info) {
        if (this.mDebugInfoView != null) {
            this.mDebugInfoView.setText(info);
        }
    }

    public TextView getHibernateHintView() {
        return this.mHibernateHintView;
    }

    public View getPortraitUseHintView() {
        return this.mPortraitUseHintView;
    }

    public View getFrontCameraHintView() {
        return this.mFrontCameraHintView;
    }

    public V6SmartShutterButton getSmartShutterButton() {
        return this.mSmartShutterButton;
    }

    public V6SettingsStatusBar getSettingsStatusBar() {
        return this.mSettingsStatusBar;
    }

    public PreferenceGroup getPreferenceGroup() {
        synchronized (this) {
            if (this.mPreferenceGroup == null) {
                updatePreferenceGroup();
            }
        }
        return this.mPreferenceGroup;
    }

    private void showModeSetting() {
        this.mActivity.setBlurFlag(true);
        getPreviewPage().switchWithAnimation(false);
        this.mTopControlPanel.onShowModeSettings();
        this.mBottomControlPanel.animateOut(null);
        this.mTopControlPanel.animateOut(null);
        if (!getPreviewFrame().isFullScreen()) {
            this.mGLView.setVisibility(0);
        }
        this.mEdgeShutterView.setVisibility(4);
        this.mPreviewPanel.setVisibility(8);
        this.mSettingsStatusBar.setVisibility(8);
        getSettingPage().show();
        enableControls(false);
        dispacherMessage(0, R.id.mode_button, 2, null, null);
        this.mPreviewPage.removeCallbacks(this.mMutexRecover);
    }

    private void hideModeSetting() {
        enableControls(false);
        getModeButton().updateRemind();
        this.mSettingPage.dismiss();
    }

    private void onShowModeSetting() {
        enableControls(true);
    }

    private void onDismissModeSetting() {
        this.mActivity.setBlurFlag(false);
        if (!V6ModulePicker.isPanoramaModule()) {
            getBottomControlUpperPanel().animateIn(null);
        }
        this.mBottomControlPanel.animateIn(null);
        this.mTopControlPanel.animateIn(null);
        getPreviewPage().switchWithAnimation(true);
        this.mPreviewPanel.setVisibility(0);
        this.mSettingsStatusBar.setVisibility(0);
        enableControls(true);
        dispacherMessage(0, R.id.hide_mode_animation_done, 2, null, null);
    }

    public boolean onBack() {
        if (!this.mSettingPage.isShown()) {
            return false;
        }
        hideModeSetting();
        return true;
    }
}
