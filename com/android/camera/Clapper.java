package com.android.camera;

import android.media.MediaRecorder;
import android.os.Build.VERSION;
import android.util.Log;
import java.io.File;

public class Clapper {
    public static final int AMPLITUDE_ABSOLUTE_THRESHOLD = (SCALE_FACTOR * 5000);
    public static final int AMPLITUDE_INIT = (SCALE_FACTOR * 2000);
    private static final int DEFAULT_AMPLITUDE_DIFF = (SCALE_FACTOR * 2000);
    public static final int SCALE_FACTOR = getScaleFactor();
    private boolean mContinueRecording;
    private ClapperListener mListener;
    private MediaRecorder mRecorder;

    public interface ClapperListener {
        void heard(float f);

        void releaseShutter();
    }

    public static int getScaleFactor() {
        if (Device.IS_MI2A || Device.IS_C3A || Device.IS_PAD1) {
            return 1;
        }
        if (Device.IS_MI4 || Device.IS_X5 || Device.IS_A9 || (Device.IS_H2XLTE && 21 <= VERSION.SDK_INT)) {
            return 6;
        }
        return 3;
    }

    public Clapper(ClapperListener listener) {
        this.mListener = listener;
    }

    private boolean startRecorder() {
        this.mRecorder = new MediaRecorder();
        try {
            this.mRecorder.setAudioSource(1);
            this.mRecorder.setOutputFormat(1);
            this.mRecorder.setAudioEncoder(1);
            this.mRecorder.setOutputFile(CameraAppImpl.getAndroidContext().getFilesDir() + File.separator + "camera_claaper_recorder.3gp");
            this.mRecorder.prepare();
            this.mRecorder.start();
            return true;
        } catch (Exception e) {
            Log.e("Clapper", "Failed to start media recorder. Maybe it is used by other app.");
            e.printStackTrace();
            return false;
        }
    }

    public boolean start() {
        boolean result = startRecorder();
        if (result) {
            this.mContinueRecording = true;
            new Thread(new Runnable() {
                public void run() {
                    Clapper.this.threadRecordClap();
                }
            }).start();
        }
        return result;
    }

    public void stop() {
        this.mContinueRecording = false;
    }

    public void stopRecorder() {
        if (this.mRecorder != null) {
            try {
                this.mContinueRecording = false;
                this.mRecorder.stop();
                this.mRecorder.release();
                this.mRecorder = null;
            } catch (Exception e) {
                Log.e("Clapper", "Failed to stop media recorder.");
                e.printStackTrace();
            }
        }
    }

    private void threadRecordClap() {
        int averageAmplitude = AMPLITUDE_INIT;
        int studyTimes = 3;
        do {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Log.e("Clapper", "Thread.sleep() interrupted");
            }
            int finishAmplitude = this.mRecorder.getMaxAmplitude();
            if (finishAmplitude > averageAmplitude) {
                averageAmplitude = (int) ((((double) averageAmplitude) * 0.9d) + (((double) finishAmplitude) * 0.09999999999999998d));
            } else {
                averageAmplitude = (int) ((((double) averageAmplitude) * 0.8d) + (((double) finishAmplitude) * 0.19999999999999996d));
            }
            if (studyTimes > 0) {
                studyTimes--;
            } else {
                int threshold;
                int ampDifference = finishAmplitude - averageAmplitude;
                if (averageAmplitude > AMPLITUDE_INIT) {
                    threshold = (int) (((double) DEFAULT_AMPLITUDE_DIFF) * (((((double) averageAmplitude) * 0.5d) / ((double) AMPLITUDE_INIT)) + 0.5d));
                } else {
                    threshold = DEFAULT_AMPLITUDE_DIFF;
                }
                if (this.mListener != null) {
                    if (finishAmplitude > AMPLITUDE_ABSOLUTE_THRESHOLD || ampDifference >= threshold) {
                        this.mListener.heard(1.0f);
                        this.mListener.releaseShutter();
                    } else {
                        this.mListener.heard(Math.max(Math.abs(((float) finishAmplitude) / ((float) AMPLITUDE_ABSOLUTE_THRESHOLD)), Math.abs(((float) ampDifference) / ((float) threshold))));
                    }
                }
            }
        } while (this.mContinueRecording);
        stopRecorder();
    }
}
