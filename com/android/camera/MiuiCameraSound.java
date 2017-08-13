package com.android.camera;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.Log;
import java.io.IOException;

public class MiuiCameraSound {
    private static final String[] SOUND_FILES = new String[]{"camera_click.ogg", "/system/media/audio/ui/camera_focus.ogg", "video_record_start.ogg", "video_record_end.ogg", "camera_fast_burst.ogg", "sound_shuter_delay_bee.ogg", "/system/media/audio/ui/NumberPickerValueChange.ogg", "audio_capture.ogg"};
    private final AssetManager mAssetManager;
    private final AudioManager mAudioManager;
    private long mLastPlayTime = 0;
    private OnLoadCompleteListener mLoadCompleteListener = new OnLoadCompleteListener() {
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            if (status != 0) {
                Log.e("MiuiCameraSound", "Unable to load sound for playback (status: " + status + ")");
            } else if (MiuiCameraSound.this.mSoundIdToPlay == sampleId) {
                soundPool.play(sampleId, 1.0f, 1.0f, 0, 0, 1.0f);
                MiuiCameraSound.this.mSoundIdToPlay = -1;
            }
        }
    };
    private int mSoundIdToPlay;
    private int[] mSoundIds;
    private SoundPool mSoundPool;

    public MiuiCameraSound(Context context) {
        int i;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mAssetManager = context.getAssets();
        if (Device.isSupportedMuteCameraSound()) {
            i = 1;
        } else {
            i = 7;
        }
        this.mSoundPool = new SoundPool(1, i, 0);
        this.mSoundPool.setOnLoadCompleteListener(this.mLoadCompleteListener);
        this.mSoundIds = new int[SOUND_FILES.length];
        for (int i2 = 0; i2 < this.mSoundIds.length; i2++) {
            this.mSoundIds[i2] = -1;
        }
        this.mSoundIdToPlay = -1;
    }

    public synchronized void load(int soundName) {
        if (soundName >= 0) {
            if (soundName < SOUND_FILES.length) {
                if (this.mSoundIds[soundName] == -1) {
                    if (soundName == 6 || soundName == 1) {
                        this.mSoundIds[soundName] = this.mSoundPool.load(SOUND_FILES[soundName], 1);
                    } else {
                        this.mSoundIds[soundName] = loadFromAsset(soundName);
                    }
                }
            }
        }
        throw new RuntimeException("Unknown sound requested: " + soundName);
    }

    private int loadFromAsset(int soundName) {
        int soundId = -1;
        AssetFileDescriptor assetFileDescriptor = null;
        try {
            assetFileDescriptor = this.mAssetManager.openFd(SOUND_FILES[soundName]);
            soundId = this.mSoundPool.load(assetFileDescriptor, 1);
            if (assetFileDescriptor != null) {
                try {
                    assetFileDescriptor.close();
                } catch (IOException e) {
                    Log.e("MiuiCameraSound", "IOException occurs when closing Camera Sound AssetFileDescriptor.");
                    e.printStackTrace();
                }
            }
        } catch (IOException e2) {
            e2.printStackTrace();
            if (assetFileDescriptor != null) {
                try {
                    assetFileDescriptor.close();
                } catch (IOException e22) {
                    Log.e("MiuiCameraSound", "IOException occurs when closing Camera Sound AssetFileDescriptor.");
                    e22.printStackTrace();
                }
            }
        } catch (Throwable th) {
            if (assetFileDescriptor != null) {
                try {
                    assetFileDescriptor.close();
                } catch (IOException e222) {
                    Log.e("MiuiCameraSound", "IOException occurs when closing Camera Sound AssetFileDescriptor.");
                    e222.printStackTrace();
                }
            }
        }
        return soundId;
    }

    private synchronized void play(int soundName, int times) {
        if (soundName >= 0) {
            if (soundName < SOUND_FILES.length) {
                if (this.mSoundIds[soundName] == -1) {
                    if (soundName == 6 || soundName == 1) {
                        this.mSoundIdToPlay = this.mSoundPool.load(SOUND_FILES[soundName], 1);
                    } else {
                        this.mSoundIdToPlay = loadFromAsset(soundName);
                    }
                    this.mSoundIds[soundName] = this.mSoundIdToPlay;
                } else {
                    this.mSoundPool.play(this.mSoundIds[soundName], 1.0f, 1.0f, 0, times - 1, 1.0f);
                    this.mLastPlayTime = System.currentTimeMillis();
                }
            }
        }
        throw new RuntimeException("Unknown sound requested: " + soundName);
    }

    public void release() {
        if (this.mSoundPool != null) {
            this.mSoundPool.release();
            this.mSoundPool = null;
        }
    }

    public void playSound(int soundId, int times) {
        if (Device.IS_CM || this.mAudioManager.getRingerMode() == 2) {
            play(soundId, times);
        }
    }

    public void playSound(int soundId) {
        playSound(soundId, 1);
    }

    public long getLastSoundPlayTime() {
        return this.mLastPlayTime;
    }
}
