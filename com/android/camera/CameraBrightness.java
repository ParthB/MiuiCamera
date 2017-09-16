package com.android.camera;

import android.app.Activity;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.Spline;
import android.view.WindowManager.LayoutParams;
import com.android.camera.aosp_porting.ReflectUtil;
import com.android.internal.R.array;
import com.android.internal.R.bool;
import com.android.internal.R.integer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CameraBrightness {
    private static Spline mPositiveScreenManualBrightnessSpline;
    private static Spline mScreenManualBrightnessSpline;
    private int mBrightness;
    private AsyncTask<Void, Void, Integer> mCameraBrightnessTask;
    private Activity mCurrentActivity;
    private boolean mFirstFocusChanged = false;
    private boolean mPaused;
    private boolean mUseDefaultValue = true;

    private class CameraBrightnessTask extends AsyncTask<Void, Void, Integer> {
        private CameraBrightnessTask() {
        }

        protected Integer doInBackground(Void... params) {
            Log.v("CameraBrightness", "doInBackground useDefaultValue=" + CameraBrightness.this.mUseDefaultValue + " paused=" + CameraBrightness.this.mPaused);
            if (CameraBrightness.this.mUseDefaultValue || CameraBrightness.this.mPaused) {
                return Integer.valueOf(-1);
            }
            int currentBacklight = getCurrentBackLight();
            if (currentBacklight <= 0) {
                return null;
            }
            CameraBrightness.this.createSpline();
            LayoutParams layoutParams = CameraBrightness.this.mCurrentActivity.getWindow().getAttributes();
            if (layoutParams.screenBrightness > 0.0f) {
                int lcdbrightness;
                float brightnessF = layoutParams.screenBrightness * 255.0f;
                if (CameraBrightness.mPositiveScreenManualBrightnessSpline != null) {
                    lcdbrightness = Math.round(CameraBrightness.mPositiveScreenManualBrightnessSpline.interpolate(brightnessF));
                } else {
                    lcdbrightness = Math.round(brightnessF);
                }
                if (Math.abs(lcdbrightness - currentBacklight) <= 1) {
                    Log.v("CameraBrightness", "doInBackground brightness unchanged");
                    return null;
                }
            }
            int brightness = currentBacklight;
            if (CameraBrightness.mScreenManualBrightnessSpline != null) {
                brightness = (int) CameraBrightness.mScreenManualBrightnessSpline.interpolate((float) currentBacklight);
            }
            return Integer.valueOf(Util.clamp((int) (((double) brightness) * (((double) (0.1f + ((((float) Util.clamp(brightness, 0, 185)) / 185.0f) * 0.3f))) + 1.0d)), 0, 255));
        }

        protected void onPostExecute(Integer result) {
            if (!isCancelled() && result != null) {
                updateBrightness(result.intValue());
            }
        }

        private void updateBrightness(int brightness) {
            if (brightness != -1 || CameraBrightness.this.mUseDefaultValue || CameraBrightness.this.mPaused) {
                LayoutParams localLayoutParams = CameraBrightness.this.mCurrentActivity.getWindow().getAttributes();
                if (CameraBrightness.this.mUseDefaultValue || CameraBrightness.this.mPaused) {
                    localLayoutParams.screenBrightness = -1.0f;
                } else {
                    localLayoutParams.screenBrightness = ((float) brightness) / 255.0f;
                }
                Log.v("CameraBrightness", "updateBrightness setting=" + brightness + " useDefaultValue=" + CameraBrightness.this.mUseDefaultValue + " screenBrightness=" + localLayoutParams.screenBrightness);
                CameraBrightness.this.mCurrentActivity.getWindow().setAttributes(localLayoutParams);
                CameraBrightness.this.mBrightness = brightness;
            }
        }

        private int getCurrentBackLight() {
            Object backlight = null;
            int tryTimes = 0;
            while (true) {
                if (("0".equals(backlight) || backlight == null) && tryTimes < 3 && !isCancelled()) {
                    backlight = execCommand("cat sys/class/leds/lcd-backlight/brightness");
                    if ("0".equals(backlight) || backlight == null) {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            Log.e("CameraBrightness", e.getMessage());
                        }
                        tryTimes++;
                    }
                }
            }
            Log.v("CameraBrightness", "getCurrentBackLight currentSetting=" + backlight);
            if (TextUtils.isEmpty(backlight)) {
                return -1;
            }
            int backlightBits = CameraBrightness.this.getAndroidIntResource("config_backlightBits");
            int bl = (int) Float.parseFloat(backlight);
            if (backlightBits > 8) {
                bl >>= backlightBits - 8;
                Log.v("CameraBrightness", "getCurrentBackLight convert to " + bl);
            }
            return bl;
        }

        private String execCommand(String command) {
            InterruptedException e;
            IOException e2;
            long time = System.currentTimeMillis();
            String result = "";
            try {
                Process proc = Runtime.getRuntime().exec(command);
                if (proc.waitFor() != 0) {
                    Log.e("CameraBrightness", "exit value = " + proc.exitValue());
                    return result;
                }
                BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                try {
                    StringBuffer stringBuffer = new StringBuffer();
                    while (true) {
                        String line = in.readLine();
                        if (line == null) {
                            break;
                        }
                        stringBuffer.append(line);
                    }
                    in.close();
                    result = stringBuffer.toString();
                    Log.v("CameraBrightness", "execCommand lcd value=" + result + " cost=" + (System.currentTimeMillis() - time));
                } catch (InterruptedException e3) {
                    e = e3;
                    BufferedReader bufferedReader = in;
                    Log.e("CameraBrightness", "execCommand InterruptedException");
                    e.printStackTrace();
                    return result;
                } catch (IOException e4) {
                    e2 = e4;
                    Log.e("CameraBrightness", "execCommand IOException");
                    e2.printStackTrace();
                    return result;
                }
                return result;
            } catch (InterruptedException e5) {
                e = e5;
                Log.e("CameraBrightness", "execCommand InterruptedException");
                e.printStackTrace();
                return result;
            } catch (IOException e6) {
                e2 = e6;
                Log.e("CameraBrightness", "execCommand IOException");
                e2.printStackTrace();
                return result;
            }
        }
    }

    public CameraBrightness(Activity activity) {
        this.mCurrentActivity = activity;
    }

    private void adjustBrightness() {
        if (Device.adjustScreenLight() && this.mCurrentActivity != null) {
            cancelLastTask();
            this.mCameraBrightnessTask = new CameraBrightnessTask().execute(new Void[0]);
        }
    }

    private void cancelLastTask() {
        if (this.mCameraBrightnessTask != null) {
            this.mCameraBrightnessTask.cancel(true);
        }
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        boolean z = true;
        Log.v("CameraBrightness", "onWindowFocusChanged hasFocus=" + hasFocus + " mFirstFocusChanged=" + this.mFirstFocusChanged);
        if (!this.mFirstFocusChanged && hasFocus) {
            this.mFirstFocusChanged = true;
        } else if (!this.mPaused) {
            if (!(this.mCurrentActivity instanceof BasePreferenceActivity) && hasFocus) {
                z = false;
            }
            this.mUseDefaultValue = z;
            adjustBrightness();
        }
    }

    public int getCurrentBrightness() {
        return this.mBrightness;
    }

    public void onResume() {
        this.mUseDefaultValue = this.mCurrentActivity instanceof BasePreferenceActivity;
        this.mPaused = false;
        Log.v("CameraBrightness", "onResume adjustBrightness");
        adjustBrightness();
    }

    public void onPause() {
        this.mFirstFocusChanged = false;
        this.mPaused = true;
        cancelLastTask();
    }

    private void createSpline() {
        if ((mScreenManualBrightnessSpline == null || mPositiveScreenManualBrightnessSpline == null) && getAndroidBoolRes("config_manual_spline_available", true)) {
            int[] inBrightness = getAndroidArrayRes("config_manualBrightnessRemapIn");
            int[] outBrightness = getAndroidArrayRes("config_manualBrightnessRemapOut");
            mScreenManualBrightnessSpline = createManualBrightnessSpline(outBrightness, inBrightness);
            mPositiveScreenManualBrightnessSpline = createManualBrightnessSpline(inBrightness, outBrightness);
            if (mScreenManualBrightnessSpline == null || mPositiveScreenManualBrightnessSpline == null) {
                Log.e("CameraBrightness", "Error to create manual brightness spline");
            }
        }
    }

    private boolean getAndroidBoolRes(String key, boolean defaultValue) {
        int resId = ReflectUtil.getFieldInt(bool.class, null, key, 0);
        if (resId == 0) {
            return defaultValue;
        }
        return CameraAppImpl.getAndroidContext().getResources().getBoolean(resId);
    }

    private int[] getAndroidArrayRes(String key) {
        int resId = ReflectUtil.getFieldInt(array.class, null, key, 0);
        if (resId != 0) {
            return CameraAppImpl.getAndroidContext().getResources().getIntArray(resId);
        }
        return new int[]{0, 255};
    }

    private int getAndroidIntResource(String key) {
        int resId = ReflectUtil.getFieldInt(integer.class, null, key, 0);
        if (resId == 0) {
            return 0;
        }
        return CameraAppImpl.getAndroidContext().getResources().getInteger(resId);
    }

    private static Spline createManualBrightnessSpline(int[] inBrightness, int[] outBrightness) {
        try {
            int n = inBrightness.length;
            float[] x = new float[n];
            float[] y = new float[n];
            for (int i = 0; i < n; i++) {
                x[i] = (float) inBrightness[i];
                y[i] = (float) outBrightness[i];
            }
            return Spline.createMonotoneCubicSpline(x, y);
        } catch (IllegalArgumentException ex) {
            Log.e("CameraBrightness", "Could not create manual-brightness spline.", ex);
            return null;
        }
    }
}
