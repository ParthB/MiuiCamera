package android.support.v4.app;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;

/* compiled from: Fragment */
final class FragmentState implements Parcelable {
    public static final Creator<FragmentState> CREATOR = new Creator<FragmentState>() {
        public FragmentState createFromParcel(Parcel in) {
            return new FragmentState(in);
        }

        public FragmentState[] newArray(int size) {
            return new FragmentState[size];
        }
    };
    final Bundle mArguments;
    final String mClassName;
    final int mContainerId;
    final boolean mDetached;
    final int mFragmentId;
    final boolean mFromLayout;
    final boolean mHidden;
    final int mIndex;
    Fragment mInstance;
    final boolean mRetainInstance;
    Bundle mSavedFragmentState;
    final String mTag;

    public FragmentState(Fragment frag) {
        this.mClassName = frag.getClass().getName();
        this.mIndex = frag.mIndex;
        this.mFromLayout = frag.mFromLayout;
        this.mFragmentId = frag.mFragmentId;
        this.mContainerId = frag.mContainerId;
        this.mTag = frag.mTag;
        this.mRetainInstance = frag.mRetainInstance;
        this.mDetached = frag.mDetached;
        this.mArguments = frag.mArguments;
        this.mHidden = frag.mHidden;
    }

    public FragmentState(Parcel in) {
        boolean z;
        boolean z2 = true;
        this.mClassName = in.readString();
        this.mIndex = in.readInt();
        if (in.readInt() != 0) {
            z = true;
        } else {
            z = false;
        }
        this.mFromLayout = z;
        this.mFragmentId = in.readInt();
        this.mContainerId = in.readInt();
        this.mTag = in.readString();
        if (in.readInt() != 0) {
            z = true;
        } else {
            z = false;
        }
        this.mRetainInstance = z;
        if (in.readInt() != 0) {
            z = true;
        } else {
            z = false;
        }
        this.mDetached = z;
        this.mArguments = in.readBundle();
        if (in.readInt() == 0) {
            z2 = false;
        }
        this.mHidden = z2;
        this.mSavedFragmentState = in.readBundle();
    }

    public Fragment instantiate(FragmentHostCallback host, Fragment parent, FragmentManagerNonConfig childNonConfig) {
        if (this.mInstance == null) {
            Context context = host.getContext();
            if (this.mArguments != null) {
                this.mArguments.setClassLoader(context.getClassLoader());
            }
            this.mInstance = Fragment.instantiate(context, this.mClassName, this.mArguments);
            if (this.mSavedFragmentState != null) {
                this.mSavedFragmentState.setClassLoader(context.getClassLoader());
                this.mInstance.mSavedFragmentState = this.mSavedFragmentState;
            }
            this.mInstance.setIndex(this.mIndex, parent);
            this.mInstance.mFromLayout = this.mFromLayout;
            this.mInstance.mRestored = true;
            this.mInstance.mFragmentId = this.mFragmentId;
            this.mInstance.mContainerId = this.mContainerId;
            this.mInstance.mTag = this.mTag;
            this.mInstance.mRetainInstance = this.mRetainInstance;
            this.mInstance.mDetached = this.mDetached;
            this.mInstance.mHidden = this.mHidden;
            this.mInstance.mFragmentManager = host.mFragmentManager;
            if (FragmentManagerImpl.DEBUG) {
                Log.v("FragmentManager", "Instantiated fragment " + this.mInstance);
            }
        }
        this.mInstance.mChildNonConfig = childNonConfig;
        return this.mInstance;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i;
        int i2 = 1;
        dest.writeString(this.mClassName);
        dest.writeInt(this.mIndex);
        if (this.mFromLayout) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeInt(i);
        dest.writeInt(this.mFragmentId);
        dest.writeInt(this.mContainerId);
        dest.writeString(this.mTag);
        if (this.mRetainInstance) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeInt(i);
        if (this.mDetached) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeInt(i);
        dest.writeBundle(this.mArguments);
        if (!this.mHidden) {
            i2 = 0;
        }
        dest.writeInt(i2);
        dest.writeBundle(this.mSavedFragmentState);
    }
}
