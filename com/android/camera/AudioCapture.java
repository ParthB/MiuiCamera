package com.android.camera;

import com.android.camera.Clapper.ClapperListener;
import com.android.camera.ui.V6ShutterButton;

public class AudioCapture implements ClapperListener {
    private Callback mCallback;
    private Clapper mClapper;
    private boolean mIsRunning = false;

    public interface Callback {
        V6ShutterButton getShutterButton();

        boolean readyToAudioCapture();

        void releaseShutter();
    }

    public AudioCapture(Callback callback) {
        Util.checkNotNull(callback);
        this.mCallback = callback;
    }

    public boolean start() {
        if (this.mIsRunning) {
            return true;
        }
        this.mClapper = new Clapper(this);
        if (!this.mClapper.start()) {
            return false;
        }
        this.mIsRunning = true;
        return true;
    }

    public void pause() {
        if (this.mIsRunning) {
            this.mIsRunning = false;
            this.mClapper.stop();
        }
    }

    public boolean isRunning() {
        return this.mIsRunning;
    }

    public void heard(float progress) {
        if (this.mCallback == null) {
            return;
        }
        if (this.mIsRunning) {
            this.mCallback.getShutterButton().setAudioProgress(progress);
        } else {
            this.mCallback.getShutterButton().setAudioProgress(-1.0f);
        }
    }

    public void releaseShutter() {
        if (this.mCallback.readyToAudioCapture() && this.mIsRunning) {
            this.mCallback.releaseShutter();
        }
    }
}
