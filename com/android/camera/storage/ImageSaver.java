package com.android.camera.storage;

import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import com.android.camera.ActivityBase;
import com.android.camera.Device;
import com.android.camera.Log;
import com.android.camera.Thumbnail;
import com.android.camera.Util;
import com.android.camera.google.PhotosSpecialTypesProvider;
import com.android.camera.google.ProcessingMediaManager;
import com.android.camera.storage.Storage.StorageListener;
import com.android.gallery3d.exif.ExifInterface;
import java.util.ArrayList;

public class ImageSaver extends Thread {
    private ActivityBase mActivity;
    private Handler mHandler;
    private int mHostState;
    private boolean mIsImageCaptureIntent;
    private Uri mLastImageUri;
    private MemoryManager mMemoryManager;
    private Thumbnail mPendingThumbnail;
    private ArrayList<SaveRequest> mQueue;
    private boolean mShouldStop;
    private boolean mStop;
    private Runnable mUpdateThumbnail = new Runnable() {
        public void run() {
            ImageSaver.this.updateThumbnail();
            ImageSaver.this.mActivity.getScreenHint().updateHint();
        }
    };
    private Object mUpdateThumbnailLock = new Object();

    private class MemoryManager implements StorageListener {
        private long mMaxMemory;
        private int mMaxTotalMemory;
        private Runtime mRuntime;
        private int mSaveTaskMemoryLimit;
        private int mSavedQueueMemoryLimit;
        private int mSaverMemoryUse;

        private MemoryManager() {
            this.mRuntime = Runtime.getRuntime();
        }

        private void initMemory() {
            this.mMaxMemory = this.mRuntime.maxMemory();
            this.mMaxTotalMemory = (int) (((float) this.mMaxMemory) * 0.95f);
            this.mSaverMemoryUse = 0;
            initLimit();
            Storage.setStorageListener(this);
            Log.d("CameraMemoryManager", "initMemory: maxMemory=" + this.mMaxMemory);
        }

        private void initLimit() {
            long totalValidMemory = this.mMaxMemory - getBaseMemory();
            if (Storage.isUsePhoneStorage()) {
                this.mSaveTaskMemoryLimit = (int) (((float) totalValidMemory) * 0.6f);
            } else {
                this.mSaveTaskMemoryLimit = (int) (((float) totalValidMemory) * 0.5f);
                if (62914560 < this.mSaveTaskMemoryLimit) {
                    this.mSaveTaskMemoryLimit = 62914560;
                }
            }
            this.mSavedQueueMemoryLimit = (int) (((float) this.mSaveTaskMemoryLimit) * 1.3f);
        }

        private long getBaseMemory() {
            switch (Util.sWindowWidth) {
                case 720:
                    return 20971520;
                case 1080:
                    return 41943040;
                case 1440:
                    return 62914560;
                default:
                    return this.mRuntime.totalMemory() - this.mRuntime.freeMemory();
            }
        }

        private void addUsedMemory(int length) {
            this.mSaverMemoryUse += length;
        }

        private void reduceUsedMemory(int length) {
            this.mSaverMemoryUse -= length;
        }

        private int getBurstDelay() {
            int delayMultiple = 0;
            if (isNeedSlowDown()) {
                if (this.mSaverMemoryUse >= (this.mSaveTaskMemoryLimit * 7) / 8) {
                    delayMultiple = 8;
                } else if (this.mSaverMemoryUse >= (this.mSaveTaskMemoryLimit * 5) / 6) {
                    delayMultiple = 5;
                } else if (this.mSaverMemoryUse >= (this.mSaveTaskMemoryLimit * 4) / 5) {
                    delayMultiple = 4;
                } else if (this.mSaverMemoryUse >= (this.mSaveTaskMemoryLimit * 3) / 4) {
                    delayMultiple = 3;
                } else {
                    delayMultiple = 1;
                }
            }
            log("getBurstDelay: delayMultiple=" + delayMultiple);
            return delayMultiple * 100;
        }

        private int getTotalUsedMemory() {
            long total = this.mRuntime.totalMemory();
            long free = this.mRuntime.freeMemory();
            long totalUsed = total - free;
            log("getLeftMemory: maxMemory=" + this.mMaxMemory + ", total=" + total + ", free=" + free + ", totalUsed=" + totalUsed);
            return (int) totalUsed;
        }

        private boolean isSaveQueueFull() {
            log("isSaveQueueFull: usedMemory=" + this.mSaverMemoryUse);
            return this.mSaverMemoryUse >= this.mSavedQueueMemoryLimit;
        }

        private boolean isReachedMemoryLimit() {
            log("isReachedMemoryLimit: usedMemory=" + this.mSaverMemoryUse);
            return this.mSaverMemoryUse >= this.mSaveTaskMemoryLimit;
        }

        private boolean isNeedStopCapture() {
            if (!isReachedMemoryLimit() && this.mMaxTotalMemory > getTotalUsedMemory()) {
                if (Storage.getLeftSpace() > ((long) this.mSaverMemoryUse)) {
                    return false;
                }
            }
            Log.d("CameraMemoryManager", "isNeedStopCapture: needStop=" + true);
            return true;
        }

        private boolean isNeedSlowDown() {
            boolean slowDown = Device.isMTKPlatform() ? this.mSaverMemoryUse >= (this.mSaveTaskMemoryLimit * 3) / 4 : this.mSaverMemoryUse >= this.mSaveTaskMemoryLimit / 2;
            log("isNeedSlowDown: return " + slowDown + " mSaverMemoryUse=" + this.mSaverMemoryUse + " mSaveTaskMemoryLimit=" + this.mSaveTaskMemoryLimit);
            return slowDown;
        }

        private void log(String msg) {
            if (Util.sIsDumpLog) {
                Log.v("CameraMemoryManager", msg);
            }
        }

        public void onStoragePathChanged() {
            initMemory();
        }
    }

    public static class SaveRequest {
        public byte[] data;
        public long date;
        public ExifInterface exif;
        public boolean finalImage;
        public int height;
        public boolean isHide;
        public boolean isMap;
        public boolean isPortrait;
        public Location loc;
        public String oldTitle;
        public int orientation;
        public String title;
        public Uri uri;
        public int width;
    }

    public ImageSaver(ActivityBase activity, Handler handler, boolean isImageCaptureIntent) {
        this.mActivity = activity;
        this.mHandler = handler;
        this.mIsImageCaptureIntent = isImageCaptureIntent;
        this.mQueue = new ArrayList();
        this.mMemoryManager = new MemoryManager();
        start();
    }

    public void addImage(byte[] data, String title, long date, Uri uri, Location loc, int width, int height, ExifInterface exif, int orientation, boolean isHide, boolean isMapFile, boolean finalImage) {
        addImage(data, title, null, date, uri, loc, width, height, exif, orientation, isHide, isMapFile, finalImage, false);
    }

    public void addImage(byte[] data, String title, String oldTitle, long date, Uri uri, Location loc, int width, int height, ExifInterface exif, int orientation, boolean isHide, boolean isMapFile, boolean finalImage, boolean isPortrait) {
        SaveRequest r = new SaveRequest();
        r.data = data;
        r.date = date;
        r.uri = uri;
        r.title = title;
        r.oldTitle = oldTitle;
        r.loc = loc == null ? null : new Location(loc);
        r.width = width;
        r.height = height;
        r.exif = exif;
        r.orientation = orientation;
        r.isHide = isHide;
        r.isMap = isMapFile;
        r.finalImage = finalImage;
        r.isPortrait = isPortrait;
        addImage(r);
    }

    public void updateImage(String title, String oldTitle) {
        SaveRequest r = new SaveRequest();
        r.title = title;
        r.oldTitle = oldTitle;
        addImage(r);
    }

    public int getBurstDelay() {
        return this.mMemoryManager.getBurstDelay();
    }

    public boolean isNeedStopCapture() {
        return this.mMemoryManager.isNeedStopCapture();
    }

    public boolean isNeedSlowDown() {
        return this.mMemoryManager.isNeedSlowDown();
    }

    public float getSuitableBurstShotSpeed() {
        return 0.66f;
    }

    public void addImage(SaveRequest r) {
        synchronized (this) {
            if (2 == this.mHostState) {
                Log.v("ImageSaver", "addImage: host is being destroyed.");
                return;
            }
            if (this.mMemoryManager.isSaveQueueFull()) {
                this.mShouldStop = true;
            }
            if (r.data != null) {
                this.mMemoryManager.addUsedMemory(r.data.length);
            }
            this.mQueue.add(r);
            notifyAll();
        }
    }

    public boolean shouldStopShot() {
        return this.mShouldStop;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        /*
        r20 = this;
        r0 = r20;
        r2 = r0.mMemoryManager;
        r2.initMemory();
    L_0x0007:
        monitor-enter(r20);
        r0 = r20;
        r2 = r0.mQueue;	 Catch:{ all -> 0x00ba }
        r2 = r2.isEmpty();	 Catch:{ all -> 0x00ba }
        if (r2 == 0) goto L_0x002a;
    L_0x0012:
        r0 = r20;
        r2 = r0.mStop;	 Catch:{ all -> 0x00ba }
        if (r2 == 0) goto L_0x0023;
    L_0x0018:
        r2 = "ImageSaver";
        r3 = "run: exiting";
        com.android.camera.Log.d(r2, r3);	 Catch:{ all -> 0x00ba }
        monitor-exit(r20);
        return;
    L_0x0023:
        r20.wait();	 Catch:{ InterruptedException -> 0x0028 }
    L_0x0026:
        monitor-exit(r20);
        goto L_0x0007;
    L_0x0028:
        r18 = move-exception;
        goto L_0x0026;
    L_0x002a:
        r0 = r20;
        r2 = r0.mQueue;	 Catch:{ all -> 0x00ba }
        r3 = 0;
        r19 = r2.get(r3);	 Catch:{ all -> 0x00ba }
        r19 = (com.android.camera.storage.ImageSaver.SaveRequest) r19;	 Catch:{ all -> 0x00ba }
        monitor-exit(r20);
        r0 = r19;
        r2 = r0.oldTitle;
        if (r2 == 0) goto L_0x004a;
    L_0x003c:
        r0 = r19;
        r2 = r0.uri;
        if (r2 != 0) goto L_0x004a;
    L_0x0042:
        r0 = r20;
        r2 = r0.mLastImageUri;
        r0 = r19;
        r0.uri = r2;
    L_0x004a:
        r0 = r19;
        r3 = r0.data;
        r0 = r19;
        r4 = r0.uri;
        r0 = r19;
        r5 = r0.title;
        r0 = r19;
        r6 = r0.date;
        r0 = r19;
        r8 = r0.loc;
        r0 = r19;
        r9 = r0.width;
        r0 = r19;
        r10 = r0.height;
        r0 = r19;
        r11 = r0.exif;
        r0 = r19;
        r12 = r0.orientation;
        r0 = r19;
        r13 = r0.isHide;
        r0 = r19;
        r14 = r0.isMap;
        r0 = r19;
        r15 = r0.finalImage;
        r0 = r19;
        r0 = r0.oldTitle;
        r16 = r0;
        r0 = r19;
        r0 = r0.isPortrait;
        r17 = r0;
        r2 = r20;
        r2.storeImage(r3, r4, r5, r6, r8, r9, r10, r11, r12, r13, r14, r15, r16, r17);
        monitor-enter(r20);
        r0 = r19;
        r2 = r0.data;	 Catch:{ all -> 0x00b7 }
        if (r2 == 0) goto L_0x009e;
    L_0x0092:
        r0 = r20;
        r2 = r0.mMemoryManager;	 Catch:{ all -> 0x00b7 }
        r0 = r19;
        r3 = r0.data;	 Catch:{ all -> 0x00b7 }
        r3 = r3.length;	 Catch:{ all -> 0x00b7 }
        r2.reduceUsedMemory(r3);	 Catch:{ all -> 0x00b7 }
    L_0x009e:
        r0 = r20;
        r2 = r0.mQueue;	 Catch:{ all -> 0x00b7 }
        r3 = 0;
        r2.remove(r3);	 Catch:{ all -> 0x00b7 }
        r0 = r20;
        r2 = r0.mMemoryManager;	 Catch:{ all -> 0x00b7 }
        r2 = r2.isSaveQueueFull();	 Catch:{ all -> 0x00b7 }
        if (r2 != 0) goto L_0x0026;
    L_0x00b0:
        r2 = 0;
        r0 = r20;
        r0.mShouldStop = r2;	 Catch:{ all -> 0x00b7 }
        goto L_0x0026;
    L_0x00b7:
        r2 = move-exception;
        monitor-exit(r20);
        throw r2;
    L_0x00ba:
        r2 = move-exception;
        monitor-exit(r20);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.storage.ImageSaver.run():void");
    }

    public void onHostResume(boolean isCaptureIntent) {
        synchronized (this) {
            this.mIsImageCaptureIntent = isCaptureIntent;
            this.mHostState = 0;
            Log.v("ImageSaver", "onHostResume: isCapture=" + this.mIsImageCaptureIntent);
        }
    }

    public void onHostPause() {
        synchronized (this) {
            this.mHostState = 1;
            boolean isQueueEmpty = this.mQueue.isEmpty();
        }
        synchronized (this.mUpdateThumbnailLock) {
            this.mHandler.removeCallbacks(this.mUpdateThumbnail);
            this.mPendingThumbnail = null;
        }
        if (!isQueueEmpty) {
            this.mActivity.getThumbnailUpdater().setThumbnail(null, false);
        }
        Log.v("ImageSaver", "onHostPause");
    }

    public void onHostDestroy() {
        synchronized (this) {
            this.mHostState = 2;
            this.mStop = true;
            notifyAll();
        }
        synchronized (this.mUpdateThumbnailLock) {
            this.mHandler.removeCallbacks(this.mUpdateThumbnail);
            this.mPendingThumbnail = null;
        }
        Log.v("ImageSaver", "onHostDestroy");
    }

    private void updateThumbnail() {
        synchronized (this.mUpdateThumbnailLock) {
            this.mHandler.removeCallbacks(this.mUpdateThumbnail);
            Thumbnail t = this.mPendingThumbnail;
            this.mPendingThumbnail = null;
        }
        if (t != null) {
            this.mActivity.getThumbnailUpdater().setThumbnail(t);
        }
    }

    private void storeImage(byte[] data, Uri uri, String title, long date, Location loc, int width, int height, ExifInterface exif, int orientation, boolean isHide, boolean isMap, boolean finalImage, String oldTitle, boolean isPortrait) {
        if (uri != null) {
            Storage.updateImage(this.mActivity, data, exif, uri, title, loc, orientation, width, height, oldTitle, isPortrait);
        } else if (data != null) {
            uri = Storage.addImage(this.mActivity, title, date, loc, orientation, data, width, height, false, isHide, isMap, false, false, isPortrait);
        }
        if (isPortrait) {
            PhotosSpecialTypesProvider.markPortraitSpecialType(this.mActivity, uri);
        }
        ProcessingMediaManager.instance().removeProcessingMedia(this.mActivity, uri);
        Storage.getAvailableSpace();
        if (uri != null) {
            boolean needThumbnail;
            synchronized (this) {
                if (this.mHostState == 0 && isLastImageForThumbnail() && !this.mIsImageCaptureIntent) {
                    needThumbnail = finalImage;
                } else {
                    needThumbnail = false;
                }
            }
            if (needThumbnail) {
                Thumbnail t;
                int inSampleSize = Integer.highestOneBit((int) Math.ceil(Math.max((double) width, (double) height) / 512.0d));
                if (isMap) {
                    t = Thumbnail.createThumbnailFromUri(this.mActivity.getContentResolver(), uri, false);
                } else {
                    t = Thumbnail.createThumbnail(data, orientation, inSampleSize, uri, false);
                }
                synchronized (this.mUpdateThumbnailLock) {
                    this.mPendingThumbnail = t;
                    this.mHandler.post(this.mUpdateThumbnail);
                }
            }
            synchronized (this) {
                if (!this.mIsImageCaptureIntent) {
                    Util.broadcastNewPicture(this.mActivity, uri);
                    this.mLastImageUri = uri;
                    if (finalImage) {
                        this.mActivity.addSecureUri(uri);
                    }
                }
            }
        }
    }

    private boolean isLastImageForThumbnail() {
        int i = 0;
        while (i < this.mQueue.size()) {
            if (i > 0 && ((SaveRequest) this.mQueue.get(i)).finalImage) {
                return false;
            }
            i++;
        }
        return true;
    }
}
