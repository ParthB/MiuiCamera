package com.android.camera;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import com.android.camera.permission.PermissionManager;

public class LocationManager {
    private static LocationManager sLocationManager;
    private Listener mListener;
    LocationListener[] mLocationListeners = new LocationListener[]{new LocationListener("gps"), new LocationListener("network")};
    private android.location.LocationManager mLocationManager;
    private boolean mRecordLocation;

    public interface Listener {
        void hideGpsOnScreenIndicator();

        void showGpsOnScreenIndicator(boolean z);
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;
        String mProvider;
        boolean mValid = false;

        public LocationListener(String provider) {
            this.mProvider = provider;
            this.mLastLocation = new Location(this.mProvider);
        }

        public void onLocationChanged(Location newLocation) {
            if (newLocation.getLatitude() != 0.0d || newLocation.getLongitude() != 0.0d) {
                if (LocationManager.this.mListener != null && LocationManager.this.mRecordLocation && "gps".equals(this.mProvider)) {
                    LocationManager.this.mListener.showGpsOnScreenIndicator(true);
                }
                if (this.mValid) {
                    Log.v("LocationManager", "update location, it is from " + this.mProvider);
                } else {
                    Log.d("LocationManager", "Got first location, it is from " + this.mProvider);
                }
                this.mLastLocation.set(newLocation);
                this.mValid = true;
            }
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
            this.mValid = false;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case 0:
                case 1:
                    this.mValid = false;
                    if (LocationManager.this.mListener != null && LocationManager.this.mRecordLocation && "gps".equals(provider)) {
                        LocationManager.this.mListener.showGpsOnScreenIndicator(false);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        public Location current() {
            return this.mValid ? this.mLastLocation : null;
        }
    }

    private LocationManager() {
    }

    public static LocationManager instance() {
        if (sLocationManager == null) {
            sLocationManager = new LocationManager();
        }
        return sLocationManager;
    }

    public Location getCurrentLocation() {
        if (!this.mRecordLocation) {
            return null;
        }
        for (int i = 0; i < this.mLocationListeners.length; i++) {
            Location l = this.mLocationListeners[i].current();
            if (l != null) {
                Log.v("LocationManager", "get current location, it is from " + this.mLocationListeners[i].mProvider);
                return l;
            }
        }
        Log.d("LocationManager", "No location received yet.");
        return null;
    }

    public void recordLocation(boolean recordLocation) {
        if (this.mRecordLocation != recordLocation) {
            this.mRecordLocation = recordLocation;
            if (recordLocation && PermissionManager.checkCameraLocationPermissions()) {
                startReceivingLocationUpdates();
            } else {
                stopReceivingLocationUpdates();
            }
        }
    }

    private void startReceivingLocationUpdates() {
        if (this.mLocationManager == null) {
            this.mLocationManager = (android.location.LocationManager) CameraAppImpl.getAndroidContext().getSystemService("location");
        }
        if (this.mLocationManager != null) {
            try {
                this.mLocationManager.requestLocationUpdates("network", 1000, 0.0f, this.mLocationListeners[1]);
            } catch (SecurityException ex) {
                Log.i("LocationManager", "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex2) {
                Log.d("LocationManager", "provider does not exist " + ex2.getMessage());
            }
            try {
                this.mLocationManager.requestLocationUpdates("gps", 1000, 0.0f, this.mLocationListeners[0]);
                if (this.mListener != null) {
                    this.mListener.showGpsOnScreenIndicator(false);
                }
            } catch (SecurityException ex3) {
                Log.i("LocationManager", "fail to request location update, ignore", ex3);
            } catch (IllegalArgumentException ex22) {
                Log.d("LocationManager", "provider does not exist " + ex22.getMessage());
            }
            Log.d("LocationManager", "startReceivingLocationUpdates");
        }
    }

    private void stopReceivingLocationUpdates() {
        if (this.mLocationManager != null) {
            for (int i = 0; i < this.mLocationListeners.length; i++) {
                try {
                    this.mLocationManager.removeUpdates(this.mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i("LocationManager", "fail to remove location listners, ignore", ex);
                }
                this.mLocationListeners[i].mValid = false;
            }
            Log.d("LocationManager", "stopReceivingLocationUpdates");
        }
        if (this.mListener != null) {
            this.mListener.hideGpsOnScreenIndicator();
        }
    }
}
