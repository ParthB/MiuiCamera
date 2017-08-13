package com.android.camera;

import java.util.HashMap;

public class MutexModeManager {
    private int mCurrentMutexMode = 0;
    private int mLastMutexMode = 0;
    private HashMap<String, HashMap<String, Runnable>> mRunnableMap;

    public MutexModeManager(HashMap<String, HashMap<String, Runnable>> map) {
        this.mRunnableMap = map;
    }

    public void setMutexMode(int mode) {
        switchMutexMode(this.mCurrentMutexMode, mode);
    }

    public void resetMutexMode() {
        switchMutexMode(this.mCurrentMutexMode, 0);
    }

    public void resetMutexModeDummy() {
        this.mLastMutexMode = this.mCurrentMutexMode;
        this.mCurrentMutexMode = 0;
    }

    public int getMutexMode() {
        return this.mCurrentMutexMode;
    }

    public int getLastMutexMode() {
        return this.mLastMutexMode;
    }

    public boolean isSupportedFlashOn() {
        return this.mCurrentMutexMode == 0 || this.mCurrentMutexMode == 4;
    }

    public boolean isSupportedTorch() {
        if (!Device.isSupportedTorchCapture()) {
            return false;
        }
        if (this.mCurrentMutexMode == 0 || this.mCurrentMutexMode == 7 || this.mCurrentMutexMode == 2) {
            return true;
        }
        return false;
    }

    public boolean isNormal() {
        return this.mCurrentMutexMode == 0;
    }

    public boolean isAoHdr() {
        return this.mCurrentMutexMode == 2;
    }

    public boolean isMorphoHdr() {
        return this.mCurrentMutexMode == 1;
    }

    public boolean isUbiFocus() {
        return this.mCurrentMutexMode == 6;
    }

    public boolean isHdr() {
        if (this.mCurrentMutexMode == 2 || this.mCurrentMutexMode == 1 || this.mCurrentMutexMode == 5) {
            return true;
        }
        return false;
    }

    public boolean isSceneHdr() {
        return this.mCurrentMutexMode == 5;
    }

    public boolean isRAW() {
        return this.mCurrentMutexMode == 4;
    }

    public boolean isBurstShoot() {
        return this.mCurrentMutexMode == 7;
    }

    public boolean isNeedComposed() {
        return (this.mCurrentMutexMode == 0 || this.mCurrentMutexMode == 2 || this.mCurrentMutexMode == 7) ? false : true;
    }

    public boolean isHandNight() {
        return this.mCurrentMutexMode == 3;
    }

    public String getSuffix() {
        switch (this.mCurrentMutexMode) {
            case 1:
            case 5:
                return "_HDR";
            case 2:
                return "_AO_HDR";
            case 3:
                return "_HHT";
            case 4:
                return "_RAW";
            default:
                return "";
        }
    }

    public static String getMutexModeName(int mode) {
        switch (mode) {
            case 1:
                return "hdr";
            case 2:
                return "aohdr";
            case 3:
                return "hand-night";
            case 4:
                return "raw";
            case 7:
                return "burst-shoot";
            default:
                return "none";
        }
    }

    private void exit(int mode) {
        this.mLastMutexMode = this.mCurrentMutexMode;
        this.mCurrentMutexMode = 0;
        if (mode != 0 && this.mRunnableMap != null) {
            HashMap<String, Runnable> map = (HashMap) this.mRunnableMap.get(getMutexModeName(mode));
            if (map != null) {
                Runnable exit = (Runnable) map.get("exit");
                if (exit != null) {
                    exit.run();
                }
            }
        }
    }

    private void enter(int mode) {
        this.mCurrentMutexMode = mode;
        if (mode != 0 && this.mRunnableMap != null) {
            HashMap<String, Runnable> map = (HashMap) this.mRunnableMap.get(getMutexModeName(mode));
            if (map != null) {
                Runnable enter = (Runnable) map.get("enter");
                if (enter != null) {
                    enter.run();
                }
            }
        }
    }

    private void switchMutexMode(int from, int to) {
        if (from != to) {
            exit(from);
            enter(to);
        }
    }
}
