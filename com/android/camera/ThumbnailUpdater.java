package com.android.camera;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import java.io.File;

public class ThumbnailUpdater {
    private ActivityBase mActivityBase;
    private ContentResolver mContentResolver = this.mActivityBase.getContentResolver();
    private AsyncTask<Void, Void, Thumbnail> mLoadThumbnailTask;
    private Thumbnail mThumbnail;

    private class LoadThumbnailTask extends AsyncTask<Void, Void, Thumbnail> {
        private boolean mLookAtCache;

        public LoadThumbnailTask(boolean lookAtCache) {
            this.mLookAtCache = lookAtCache;
        }

        protected Thumbnail doInBackground(Void... params) {
            Thumbnail t = null;
            if (isCancelled()) {
                return null;
            }
            if (ThumbnailUpdater.this.mThumbnail != null) {
                Uri thumbnailUri = ThumbnailUpdater.this.mThumbnail.getUri();
                if (Util.isUriValid(thumbnailUri, ThumbnailUpdater.this.mContentResolver) && thumbnailUri.equals(Thumbnail.getLastThumbnailUri(ThumbnailUpdater.this.mContentResolver))) {
                    return ThumbnailUpdater.this.mThumbnail;
                }
            }
            if ((!ThumbnailUpdater.this.mActivityBase.startFromSecureKeyguard() || ThumbnailUpdater.this.mActivityBase.getSecureUriList().size() > 0) && this.mLookAtCache) {
                t = Thumbnail.getLastThumbnailFromFile(ThumbnailUpdater.this.mActivityBase.getFilesDir(), ThumbnailUpdater.this.mContentResolver);
            }
            if (isCancelled()) {
                return null;
            }
            int code;
            Uri uri = null;
            if (t != null) {
                uri = t.getUri();
            }
            Thumbnail[] result = new Thumbnail[1];
            if (ThumbnailUpdater.this.mActivityBase.startFromSecureKeyguard()) {
                code = Thumbnail.getLastThumbnailFromUriList(ThumbnailUpdater.this.mContentResolver, result, ThumbnailUpdater.this.mActivityBase.getSecureUriList(), uri);
            } else {
                code = Thumbnail.getLastThumbnailFromContentResolver(ThumbnailUpdater.this.mContentResolver, result, uri);
            }
            switch (code) {
                case -1:
                    return t;
                case 0:
                    return null;
                case 1:
                    return result[0];
                case 2:
                    cancel(true);
                    return null;
                default:
                    return null;
            }
        }

        protected void onPostExecute(Thumbnail thumbnail) {
            if (!isCancelled()) {
                ThumbnailUpdater.this.mThumbnail = thumbnail;
                ThumbnailUpdater.this.updateThumbnailView(false);
            }
        }
    }

    private class SaveThumbnailTask extends AsyncTask<Thumbnail, Void, Void> {
        private SaveThumbnailTask() {
        }

        protected Void doInBackground(Thumbnail... params) {
            File filesDir = ThumbnailUpdater.this.mActivityBase.getFilesDir();
            for (Thumbnail saveLastThumbnailToFile : params) {
                saveLastThumbnailToFile.saveLastThumbnailToFile(filesDir);
            }
            return null;
        }
    }

    public ThumbnailUpdater(ActivityBase activity) {
        this.mActivityBase = activity;
    }

    public Thumbnail getThumbnail() {
        return this.mThumbnail;
    }

    public void setThumbnail(Thumbnail t, boolean update) {
        boolean z = (t == null || !t.needUpdateAnimation()) ? false : update;
        setThumbnail(t, update, z);
    }

    public void setThumbnail(Thumbnail t, boolean update, boolean needAnimation) {
        this.mThumbnail = t;
        if (update) {
            updateThumbnailView(needAnimation);
        }
    }

    public void setThumbnail(Thumbnail t) {
        setThumbnail(t, true, t != null ? t.needUpdateAnimation() : false);
    }

    public void updateThumbnailView(boolean needAnimation) {
        this.mActivityBase.getUIController().updateThumbnailView(this.mThumbnail, needAnimation);
    }

    public void cancelTask() {
        if (this.mLoadThumbnailTask != null) {
            this.mLoadThumbnailTask.cancel(true);
            this.mLoadThumbnailTask = null;
        }
    }

    public void getLastThumbnail() {
        if (this.mLoadThumbnailTask != null) {
            this.mLoadThumbnailTask.cancel(true);
        }
        this.mLoadThumbnailTask = new LoadThumbnailTask(true).execute(new Void[0]);
    }

    public void getLastThumbnailUncached() {
        if (this.mLoadThumbnailTask != null) {
            this.mLoadThumbnailTask.cancel(true);
        }
        this.mLoadThumbnailTask = new LoadThumbnailTask(false).execute(new Void[0]);
    }

    public void saveThumbnailToFile() {
        if (this.mThumbnail != null && !this.mThumbnail.fromFile()) {
            new SaveThumbnailTask().execute(new Thumbnail[]{this.mThumbnail});
        }
    }
}
