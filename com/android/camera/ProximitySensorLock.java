package com.android.camera;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.recyclerview.R;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import com.android.camera.preferences.CameraSettingPreferences;

public class ProximitySensorLock implements SensorEventListener {
    private static final int SHORTCUT_UNLOCK = (getKeyBitmask(4) | getKeyBitmask(24));
    private Context mContext;
    private final boolean mFromVolumeKey;
    private View mHintView;
    private volatile boolean mJudged;
    private int mKeyPressed;
    private int mKeyPressing;
    private Boolean mProximityNear = null;
    private Sensor mProximitySensor;
    private volatile boolean mResumeCalled;
    private Handler mWorkerHandler;
    private HandlerThread mWorkerThread;

    public boolean intercept(android.view.KeyEvent r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.camera.ProximitySensorLock.intercept(android.view.KeyEvent):boolean
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:59)
	at jadx.core.ProcessClass.process(ProcessClass.java:42)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 6 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.ProximitySensorLock.intercept(android.view.KeyEvent):boolean");
    }

    public ProximitySensorLock(Context context) {
        this.mContext = context;
        if (context instanceof Activity) {
            boolean z;
            if ((8388608 & ((Activity) context).getIntent().getFlags()) == 0) {
                z = true;
            } else {
                z = false;
            }
            this.mFromVolumeKey = z;
        } else {
            this.mFromVolumeKey = false;
        }
        resetKeyStatus();
        this.mJudged = false;
        this.mWorkerThread = new HandlerThread("Proximity sensor lock");
        this.mWorkerThread.start();
        this.mWorkerHandler = new Handler(this.mWorkerThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        CameraDataAnalytics.instance().trackEvent("proximity_lock_keyguard_timeout_times_key");
                        ProximitySensorLock.this.exit();
                        return;
                    case 2:
                        removeMessages(2);
                        if (ProximitySensorLock.this.mProximityNear == null) {
                            Log.d("ProximitySensorLock", "delay check timeout, callback not returned, take it as far");
                            CameraDataAnalytics.instance().trackEvent("proximity_lock_sensor_delay_times_key");
                            ProximitySensorLock.this.mProximityNear = Boolean.valueOf(false);
                            if (!ProximitySensorLock.this.isFromSnap() && ProximitySensorLock.this.mResumeCalled) {
                                ProximitySensorLock.this.judge();
                                return;
                            }
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
    }

    public static boolean supported() {
        return (Device.IS_A8 || Device.IS_D5) ? false : true;
    }

    public static boolean enabled() {
        return supported() ? CameraSettings.isProximityLockOpen(CameraSettingPreferences.instance()) : false;
    }

    public boolean active() {
        return (this.mHintView == null || this.mHintView.getParent() == null) ? false : true;
    }

    public void onResume() {
        Log.d("ProximitySensorLock", "onResume enabled " + enabled() + ", mFromVolumeKey " + this.mFromVolumeKey + ", mProxityNear " + this.mProximityNear);
        if (enabled()) {
            this.mResumeCalled = true;
            if (this.mProximityNear != null) {
                judge();
            }
        }
    }

    private void judge() {
        if (this.mFromVolumeKey ? this.mProximityNear.booleanValue() : false) {
            CameraDataAnalytics.instance().trackEvent("proximity_lock_volume_times_key");
            stopWatching();
            exit();
        } else if (this.mProximityNear.booleanValue()) {
            CameraDataAnalytics.instance().trackEvent("proximity_lock_keyguard_times_key");
            show();
        } else {
            stopWatching();
        }
        this.mJudged = true;
    }

    public boolean shouldQuitSnap() {
        Log.d("ProximitySensorLock", "shouldQuit fromSnap " + isFromSnap() + ", proximity ->" + this.mProximityNear);
        boolean quit = isFromSnap() ? this.mProximityNear != null ? this.mProximityNear.booleanValue() : true : false;
        if (quit) {
            CameraDataAnalytics.instance().trackEvent("proximity_lock_snap_times_key");
        }
        return quit;
    }

    private static int getKeyBitmask(int keycode) {
        switch (keycode) {
            case 3:
                return 4;
            case 4:
                return 8;
            case 24:
                return 64;
            case 25:
                return 32;
            case 26:
                return 16;
            case 82:
            case 187:
                return 2;
            default:
                return 1;
        }
    }

    private boolean shouldBeBlocked(KeyEvent event) {
        if (event == null || !active()) {
            return false;
        }
        switch (event.getKeyCode()) {
            case 79:
            case 85:
            case 86:
            case 87:
            case 126:
            case 127:
                return false;
            default:
                return true;
        }
    }

    public void startWatching() {
        if (enabled() && this.mProximitySensor == null) {
            Log.d("ProximitySensorLock", "startWatching proximity sensor " + this.mContext);
            this.mJudged = false;
            this.mResumeCalled = false;
            SensorManager sm = (SensorManager) this.mContext.getSystemService("sensor");
            this.mProximitySensor = sm.getDefaultSensor(8);
            sm.registerListener(this, this.mProximitySensor, 0, this.mWorkerHandler);
            this.mWorkerHandler.removeMessages(2);
            this.mWorkerHandler.sendEmptyMessageDelayed(2, 300);
        }
    }

    private void stopWatching() {
        if (this.mProximitySensor != null) {
            Log.d("ProximitySensorLock", "stopWatching proximity sensor " + this.mContext);
            ((SensorManager) this.mContext.getSystemService("sensor")).unregisterListener(this);
            this.mProximitySensor = null;
            stopWorkerThread();
        }
    }

    private void stopWorkerThread() {
        if (this.mWorkerThread != null) {
            if (VERSION.SDK_INT >= 19) {
                this.mWorkerThread.quitSafely();
            } else {
                this.mWorkerThread.quit();
            }
            this.mWorkerThread = null;
        }
        this.mWorkerHandler = null;
        this.mJudged = false;
        this.mResumeCalled = false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void show() {
        /*
        r2 = this;
        r0 = enabled();
        if (r0 == 0) goto L_0x000a;
    L_0x0006:
        r0 = r2.mFromVolumeKey;
        if (r0 == 0) goto L_0x000b;
    L_0x000a:
        return;
    L_0x000b:
        r0 = r2.mWorkerHandler;
        if (r0 == 0) goto L_0x0025;
    L_0x000f:
        r0 = r2.mContext;
        if (r0 == 0) goto L_0x0025;
    L_0x0013:
        r0 = r2.mContext;
        r0 = r0 instanceof android.app.Activity;
        if (r0 == 0) goto L_0x0025;
    L_0x0019:
        r0 = r2.mContext;
        r0 = (android.app.Activity) r0;
        r1 = new com.android.camera.ProximitySensorLock$2;
        r1.<init>();
        r0.runOnUiThread(r1);
    L_0x0025:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.ProximitySensorLock.show():void");
    }

    private void doShow() {
        if (this.mWorkerHandler != null) {
            if (active()) {
                hide();
            }
            FrameLayout content = (FrameLayout) ((Activity) this.mContext).findViewById(16908290);
            if (content != null) {
                if (this.mHintView == null) {
                    this.mHintView = inflateHint();
                }
                content.addView(this.mHintView);
                ObjectAnimator animator = ObjectAnimator.ofFloat(this.mHintView, View.ALPHA, new float[]{0.0f, 1.0f});
                animator.setDuration(500);
                animator.start();
                Animation animation = new AlphaAnimation(1.0f, 0.0f);
                animation.setDuration(500);
                animation.setRepeatCount(-1);
                animation.setRepeatMode(2);
                animation.setStartOffset(500);
                this.mHintView.findViewById(R.id.screen_on_proximity_sensor_hint_animation).startAnimation(animation);
                resetKeyStatus();
                this.mWorkerHandler.sendEmptyMessageDelayed(1, 30000);
            }
        }
    }

    private void resetKeyStatus() {
        this.mKeyPressed = 0;
        this.mKeyPressing = 0;
    }

    private void hide() {
        resetKeyStatus();
        if (this.mWorkerHandler != null) {
            this.mWorkerHandler.removeMessages(1);
        }
        if (this.mContext != null && (this.mContext instanceof Activity)) {
            ((Activity) this.mContext).runOnUiThread(new Runnable() {
                public void run() {
                    if (ProximitySensorLock.this.mHintView != null) {
                        ViewGroup parent = (ViewGroup) ProximitySensorLock.this.mHintView.getParent();
                        if (parent != null) {
                            parent.removeView(ProximitySensorLock.this.mHintView);
                        }
                    }
                }
            });
        }
    }

    public void destroy() {
        Log.d("ProximitySensorLock", "destroying");
        hide();
        stopWatching();
        stopWorkerThread();
        this.mJudged = false;
        this.mResumeCalled = false;
        this.mContext = null;
    }

    private View inflateHint() {
        return LayoutInflater.from(this.mContext).inflate(R.layout.screen_on_proximity_sensor_guide, null, false);
    }

    private void exit() {
        if (this.mContext != null && (!(this.mContext instanceof Activity) || !((Activity) this.mContext).isFinishing())) {
            Log.d("ProximitySensorLock", "Finish activity, exiting.");
            ((Activity) this.mContext).finish();
        }
    }

    public void onSensorChanged(SensorEvent event) {
        boolean z;
        boolean z2 = false;
        boolean firstCallback = this.mProximityNear == null;
        boolean far = event.values[0] > 3.0f || event.values[0] == event.sensor.getMaximumRange();
        String str = "ProximitySensorLock";
        StringBuilder append = new StringBuilder().append("onSensorChanged near ");
        if (far) {
            z = false;
        } else {
            z = true;
        }
        Log.d(str, append.append(z).append(", values ").append(event.values[0]).append(", max ").append(event.sensor.getMaximumRange()).toString());
        if (!far) {
            z2 = true;
        }
        this.mProximityNear = Boolean.valueOf(z2);
        if (this.mWorkerHandler != null) {
            boolean beforeDelayCheck = this.mWorkerHandler.hasMessages(2);
            this.mWorkerHandler.removeMessages(2);
            if (isFromSnap() || !this.mResumeCalled) {
                return;
            }
            if (firstCallback && beforeDelayCheck) {
                judge();
                return;
            }
            if (!this.mFromVolumeKey && this.mJudged) {
                if (far) {
                    CameraDataAnalytics.instance().trackEvent("proximity_lock_keyguard_unlock_times_key");
                    hide();
                } else {
                    CameraDataAnalytics.instance().trackEvent("proximity_lock_keyguard_times_key");
                    show();
                }
            }
        }
    }

    private boolean isFromSnap() {
        return !(this.mContext instanceof Activity);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
