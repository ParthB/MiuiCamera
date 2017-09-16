package com.android.camera;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;

public class SensorStateManager {
    private static final int CAPTURE_POSTURE_DEGREE = SystemProperties.getInt("capture_degree", 45);
    private static final double GYROSCOPE_MOVING_THRESHOLD = ((double) (((float) SystemProperties.getInt("camera_moving_threshold", 15)) / 10.0f));
    private static final double GYROSCOPE_STABLE_THRESHOLD = ((double) (((float) SystemProperties.getInt("camera_stable_threshold", 9)) / 10.0f));
    private final Sensor mAccelerometerSensor;
    private SensorEventListener mAccelerometerSensorEventListenerImpl = new SensorEventListener() {
        private float[] finalFilter = new float[3];
        private float[] firstFilter = new float[3];

        public void onSensorChanged(SensorEvent event) {
            if (SensorStateManager.this.mSensorStateListener != null) {
                this.firstFilter[0] = (this.firstFilter[0] * 0.8f) + (event.values[0] * 0.19999999f);
                this.firstFilter[1] = (this.firstFilter[1] * 0.8f) + (event.values[1] * 0.19999999f);
                this.firstFilter[2] = (this.firstFilter[2] * 0.8f) + (event.values[2] * 0.19999999f);
                this.finalFilter[0] = (this.finalFilter[0] * 0.7f) + (this.firstFilter[0] * 0.3f);
                this.finalFilter[1] = (this.finalFilter[1] * 0.7f) + (this.firstFilter[1] * 0.3f);
                this.finalFilter[2] = (this.finalFilter[2] * 0.7f) + (this.firstFilter[2] * 0.3f);
                Log.v("SensorStateManager", "finalFilter=" + this.finalFilter[0] + " " + this.finalFilter[1] + " " + this.finalFilter[2] + " event.values=" + event.values[0] + " " + event.values[1] + " " + event.values[2]);
                float orientation = -1.0f;
                float X = -this.finalFilter[0];
                float Y = -this.finalFilter[1];
                float Z = -this.finalFilter[2];
                if (4.0f * ((X * X) + (Y * Y)) >= Z * Z) {
                    orientation = SensorStateManager.this.normalizeDegree(90.0f - (((float) Math.atan2((double) (-Y), (double) X)) * 57.29578f));
                }
                if (orientation != SensorStateManager.this.mOrientation) {
                    if (Math.abs(SensorStateManager.this.mOrientation - orientation) > 3.0f) {
                        clearFilter();
                    }
                    SensorStateManager.this.mOrientation = orientation;
                    Log.v("SensorStateManager", "SensorEventListenerImpl TYPE_ACCELEROMETER mOrientation=" + SensorStateManager.this.mOrientation + " mIsLying=" + SensorStateManager.this.mIsLying);
                    SensorStateManager.this.mSensorStateListener.onDeviceOrientationChanged(SensorStateManager.this.mOrientation, SensorStateManager.this.mIsLying);
                }
            }
        }

        private void clearFilter() {
            for (int i = 0; i < this.firstFilter.length; i++) {
                this.firstFilter[i] = 0.0f;
                this.finalFilter[i] = 0.0f;
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.v("SensorStateManager", "onAccuracyChanged accuracy=" + accuracy);
        }
    };
    private int mAccelerometerTag = 0;
    private long mAccelerometerTimeStamp = 0;
    private double[] mAngleSpeed = new double[]{GYROSCOPE_STABLE_THRESHOLD, GYROSCOPE_STABLE_THRESHOLD, GYROSCOPE_STABLE_THRESHOLD, GYROSCOPE_STABLE_THRESHOLD, GYROSCOPE_STABLE_THRESHOLD};
    private int mAngleSpeedIndex = -1;
    private double mAngleTotal = 0.0d;
    private int mCapturePosture = 0;
    private boolean mDeviceStable;
    private boolean mEdgeTouchEnabled;
    private boolean mFocusSensorEnabled;
    private boolean mGradienterEnabled;
    private final Sensor mGyroscope;
    private SensorEventListener mGyroscopeListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent e) {
            long elapseTime = Math.abs(e.timestamp - SensorStateManager.this.mGyroscopeTimeStamp);
            if (SensorStateManager.this.mSensorStateListener != null && SensorStateManager.this.mSensorStateListener.isWorking() && elapseTime >= 100000000) {
                if (SensorStateManager.this.mGyroscopeTimeStamp == 0 || elapseTime > 1000000000) {
                    SensorStateManager.this.mGyroscopeTimeStamp = e.timestamp;
                    return;
                }
                float dT = ((float) elapseTime) * 1.0E-9f;
                double w = Math.sqrt((double) (((e.values[0] * e.values[0]) + (e.values[1] * e.values[1])) + (e.values[2] * e.values[2])));
                SensorStateManager.this.mGyroscopeTimeStamp = e.timestamp;
                if (SensorStateManager.GYROSCOPE_MOVING_THRESHOLD < w) {
                    SensorStateManager.this.deviceBeginMoving();
                }
                SensorStateManager sensorStateManager = SensorStateManager.this;
                SensorStateManager sensorStateManager2 = SensorStateManager.this;
                sensorStateManager.mAngleSpeedIndex = sensorStateManager2.mAngleSpeedIndex = sensorStateManager2.mAngleSpeedIndex + 1 % SensorStateManager.this.mAngleSpeed.length;
                SensorStateManager.this.mAngleSpeed[SensorStateManager.this.mAngleSpeedIndex] = w;
                if (w >= 0.05000000074505806d) {
                    sensorStateManager = SensorStateManager.this;
                    sensorStateManager.mAngleTotal = sensorStateManager.mAngleTotal + (((double) dT) * w);
                    if (SensorStateManager.this.mAngleTotal > 0.5235987755982988d) {
                        SensorStateManager.this.mAngleTotal = 0.0d;
                        SensorStateManager.this.deviceKeepMoving(10000.0d);
                    }
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private long mGyroscopeTimeStamp = 0;
    private Handler mHandler;
    private boolean mIsLying = false;
    private SensorEventListener mLinearAccelerationListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent e) {
            long elapseTime = Math.abs(e.timestamp - SensorStateManager.this.mAccelerometerTimeStamp);
            if (SensorStateManager.this.mSensorStateListener != null && SensorStateManager.this.mSensorStateListener.isWorking() && elapseTime >= 100000000) {
                if (SensorStateManager.this.mAccelerometerTimeStamp == 0 || elapseTime > 1000000000) {
                    SensorStateManager.this.mAccelerometerTimeStamp = e.timestamp;
                    return;
                }
                double a = Math.sqrt((double) (((e.values[0] * e.values[0]) + (e.values[1] * e.values[1])) + (e.values[2] * e.values[2])));
                SensorStateManager.this.mAccelerometerTimeStamp = e.timestamp;
                if (a > 1.0d) {
                    SensorStateManager.this.deviceKeepMoving(a);
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final Sensor mLinearAccelerometer;
    private float mOrientation = -1.0f;
    private final Sensor mOrientationSensor;
    private SensorEventListener mOrientationSensorEventListener;
    private int mRate;
    private boolean mRotationFlagEnabled;
    private HandlerThread mSensorListenerThread;
    private final SensorManager mSensorManager;
    private int mSensorRegister;
    private SensorStateListener mSensorStateListener;
    private Handler mThreadHandler;

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            switch (msg.what) {
                case 1:
                    SensorStateManager.this.deviceBecomeStable();
                    return;
                case 2:
                    SensorStateManager sensorStateManager = SensorStateManager.this;
                    int i = msg.arg1;
                    if (msg.arg2 != 1) {
                        z = false;
                    }
                    sensorStateManager.update(i, z);
                    return;
                default:
                    return;
            }
        }
    }

    class OrientationSensorEventListenerImpl implements SensorEventListener {
        OrientationSensorEventListenerImpl() {
        }

        public void onSensorChanged(SensorEvent event) {
            if (SensorStateManager.this.mSensorStateListener != null) {
                boolean isLying;
                float orientation = -1.0f;
                float y = event.values[1];
                float z = event.values[2];
                float absY = Math.abs(y);
                float absZ = Math.abs(z);
                int hysteresis = SensorStateManager.this.mIsLying ? 5 : 0;
                int minBound = hysteresis + 26;
                int maxBound = 153 - hysteresis;
                if (absY <= ((float) minBound) || absY >= ((float) maxBound)) {
                    boolean z2 = absZ <= ((float) minBound) || absZ >= ((float) maxBound);
                    isLying = z2;
                } else {
                    isLying = false;
                }
                if (isLying && Math.abs(absY - absZ) > 1.0f) {
                    if (absY > absZ) {
                        orientation = (float) (y < 0.0f ? 0 : 180);
                    } else if (absY < absZ) {
                        orientation = (float) (z < 0.0f ? 90 : 270);
                    }
                }
                if (Math.abs(absZ - 90.0f) < ((float) SensorStateManager.CAPTURE_POSTURE_DEGREE)) {
                    SensorStateManager.this.changeCapturePosture(z < 0.0f ? 1 : 2);
                } else {
                    SensorStateManager.this.changeCapturePosture(0);
                }
                if (isLying != SensorStateManager.this.mIsLying || (isLying && orientation != SensorStateManager.this.mOrientation)) {
                    SensorStateManager.this.mIsLying = isLying;
                    if (SensorStateManager.this.mIsLying) {
                        SensorStateManager.this.mOrientation = orientation;
                    }
                    Log.v("SensorStateManager", "SensorEventListenerImpl TYPE_ORIENTATION mOrientation=" + SensorStateManager.this.mOrientation + " mIsLying=" + SensorStateManager.this.mIsLying);
                    SensorStateManager.this.mSensorStateListener.onDeviceOrientationChanged(SensorStateManager.this.mOrientation, SensorStateManager.this.mIsLying);
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.v("SensorStateManager", "onAccuracyChanged accuracy=" + accuracy);
        }
    }

    public interface SensorStateListener {
        boolean isWorking();

        void notifyDevicePostureChanged();

        void onDeviceBecomeStable();

        void onDeviceBeginMoving();

        void onDeviceKeepMoving(double d);

        void onDeviceOrientationChanged(float f, boolean z);
    }

    public SensorStateManager(Context context, Looper looper) {
        this.mSensorManager = (SensorManager) context.getSystemService("sensor");
        this.mLinearAccelerometer = this.mSensorManager.getDefaultSensor(10);
        this.mGyroscope = this.mSensorManager.getDefaultSensor(4);
        this.mOrientationSensor = this.mSensorManager.getDefaultSensor(3);
        this.mAccelerometerSensor = this.mSensorManager.getDefaultSensor(1);
        this.mHandler = new MainHandler(looper);
        this.mRate = 30000;
        if (canDetectOrientation()) {
            this.mOrientationSensorEventListener = new OrientationSensorEventListenerImpl();
        }
        this.mSensorListenerThread = new HandlerThread("SensorListenerThread");
        this.mSensorListenerThread.start();
    }

    public void setSensorStateListener(SensorStateListener l) {
        this.mSensorStateListener = l;
    }

    public void setFocusSensorEnabled(boolean enable) {
        if (this.mFocusSensorEnabled != enable) {
            this.mFocusSensorEnabled = enable;
            this.mHandler.removeMessages(2);
            int sensor = 3;
            if (!this.mFocusSensorEnabled) {
                sensor = filterUnregistSensor(3);
            }
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2, sensor, enable ? 1 : 0), 1000);
        }
    }

    public void setGradienterEnabled(boolean enable) {
        if (this.mGradienterEnabled != enable) {
            this.mGradienterEnabled = enable;
            int sensor = 12;
            if (!this.mGradienterEnabled) {
                sensor = filterUnregistSensor(12);
            }
            update(sensor, this.mGradienterEnabled);
        }
    }

    public void setRotationIndicatorEnabled(boolean enable) {
        if (Device.isOrientationIndicatorEnabled() && canDetectOrientation() && this.mRotationFlagEnabled != enable) {
            this.mRotationFlagEnabled = enable;
            int sensor = 4;
            if (!this.mRotationFlagEnabled) {
                sensor = filterUnregistSensor(4);
            }
            update(sensor, this.mRotationFlagEnabled);
        }
    }

    private int filterUnregistSensor(int sensor) {
        if (this.mEdgeTouchEnabled) {
            sensor = (sensor & -3) & -5;
        }
        if (this.mRotationFlagEnabled) {
            sensor &= -5;
        }
        if (this.mFocusSensorEnabled) {
            sensor = (sensor & -2) & -3;
        }
        if (this.mGradienterEnabled) {
            return (sensor & -9) & -5;
        }
        return sensor;
    }

    public void setEdgeTouchEnabled(boolean enable) {
        if (this.mEdgeTouchEnabled != enable) {
            this.mEdgeTouchEnabled = enable;
            int sensor = 6;
            if (!this.mEdgeTouchEnabled) {
                if (this.mGradienterEnabled) {
                    sensor = 2;
                }
                if (this.mFocusSensorEnabled) {
                    sensor &= -3;
                }
            }
            update(sensor, this.mEdgeTouchEnabled);
        }
    }

    private void update(int sensor, boolean enable) {
        if (!enable && isPartialContains(this.mSensorRegister, sensor)) {
            unregister(sensor);
        } else if (enable && !isContains(this.mSensorRegister, sensor)) {
            register(sensor);
        }
    }

    public void register() {
        int sensor = 0;
        if (this.mFocusSensorEnabled) {
            sensor = 1 | 2;
        }
        if (this.mEdgeTouchEnabled) {
            sensor = (sensor | 2) | 4;
        }
        if (this.mGradienterEnabled) {
            sensor = (sensor | 8) | 4;
        }
        if (this.mRotationFlagEnabled) {
            sensor |= 4;
        }
        register(sensor);
    }

    public void register(int sensor) {
        if (!isContains(this.mSensorRegister, sensor)) {
            if (this.mThreadHandler == null && isPartialContains(sensor, 12)) {
                this.mThreadHandler = new Handler(this.mSensorListenerThread.getLooper());
            }
            if (this.mFocusSensorEnabled) {
                this.mDeviceStable = true;
                sensor = (sensor | 1) | 2;
                this.mHandler.removeMessages(2);
            }
            if (isContains(sensor, 2) && !isContains(this.mSensorRegister, 2)) {
                this.mSensorManager.registerListener(this.mGyroscopeListener, this.mGyroscope, 2);
                this.mSensorRegister |= 2;
            }
            if (isContains(sensor, 1) && !isContains(this.mSensorRegister, 1)) {
                this.mSensorManager.registerListener(this.mLinearAccelerationListener, this.mLinearAccelerometer, 2);
                this.mSensorRegister |= 1;
            }
            if (canDetectOrientation() && isContains(sensor, 4) && !isContains(this.mSensorRegister, 4)) {
                this.mSensorManager.registerListener(this.mOrientationSensorEventListener, this.mOrientationSensor, this.mRate, this.mThreadHandler);
                this.mSensorRegister |= 4;
            }
            if (isContains(sensor, 8) && !isContains(this.mSensorRegister, 8)) {
                this.mSensorManager.registerListener(this.mAccelerometerSensorEventListenerImpl, this.mAccelerometerSensor, this.mRate, this.mThreadHandler);
                this.mSensorRegister |= 8;
            }
        }
    }

    public void unregister(int sensor) {
        if (this.mSensorRegister != 0) {
            if (!this.mFocusSensorEnabled || sensor == 15) {
                if (!this.mFocusSensorEnabled && this.mHandler.hasMessages(2)) {
                    sensor |= 1;
                    if (!this.mEdgeTouchEnabled) {
                        sensor |= 2;
                    }
                }
                reset();
                this.mHandler.removeMessages(2);
            }
            if (isContains(sensor, 2) && isContains(this.mSensorRegister, 2)) {
                this.mSensorManager.unregisterListener(this.mGyroscopeListener);
                this.mSensorRegister &= -3;
            }
            if (isContains(sensor, 1) && isContains(this.mSensorRegister, 1)) {
                this.mSensorManager.unregisterListener(this.mLinearAccelerationListener);
                this.mSensorRegister &= -2;
            }
            if (isContains(sensor, 4) && isContains(this.mSensorRegister, 4)) {
                this.mSensorManager.unregisterListener(this.mOrientationSensorEventListener);
                this.mSensorRegister &= -5;
                this.mIsLying = false;
                changeCapturePosture(0);
            }
            if (isContains(sensor, 8) && isContains(this.mSensorRegister, 8)) {
                this.mSensorManager.unregisterListener(this.mAccelerometerSensorEventListenerImpl);
                this.mSensorRegister &= -9;
            }
        }
    }

    private boolean isContains(int total, int special) {
        return (total & special) == special;
    }

    private boolean isPartialContains(int total, int special) {
        return (total & special) != 0;
    }

    public void reset() {
        this.mHandler.removeMessages(1);
        this.mAngleTotal = 0.0d;
        this.mDeviceStable = true;
        this.mAccelerometerTag = 0;
    }

    private float normalizeDegree(float degree) {
        while (degree >= 360.0f) {
            degree -= 360.0f;
        }
        while (degree < 0.0f) {
            degree += 360.0f;
        }
        return degree;
    }

    public boolean canDetectOrientation() {
        return this.mOrientationSensor != null;
    }

    public boolean isDeviceLying() {
        return this.mIsLying;
    }

    public int getCapturePosture() {
        return this.mCapturePosture;
    }

    private void deviceBeginMoving() {
        this.mSensorStateListener.onDeviceBeginMoving();
    }

    private void deviceBecomeStable() {
        if (this.mFocusSensorEnabled) {
            this.mSensorStateListener.onDeviceBecomeStable();
        }
    }

    private void deviceKeepMoving(double a) {
        if (this.mFocusSensorEnabled) {
            this.mSensorStateListener.onDeviceKeepMoving(a);
        }
    }

    private void changeCapturePosture(int posture) {
        if (this.mCapturePosture != posture) {
            this.mCapturePosture = posture;
            if (this.mSensorStateListener != null) {
                this.mSensorStateListener.notifyDevicePostureChanged();
            }
        }
    }

    public void onDestory() {
        this.mSensorListenerThread.quit();
    }
}
