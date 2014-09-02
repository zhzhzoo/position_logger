package com.senz.positionlogger;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.android.internal.util.Predicate;

public class SenzLocation {
    private Context mContext;
    private LocationManager mLM;
    private Handler mHandler;

    public void init(Context context, Looper looper) {
        mContext = context;
        mLM = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (looper == null) {
            looper = Looper.myLooper();
        }
        mHandler = new Handler(looper);
    }

    public void requestLocation(final SenzLocationListener listener, final Predicate<Location> predicate) {
        Location l;
        final boolean networkEnabled, gpsEnabled;
        final class Wrapper{
            public LocationListener locationListener;
            public Runnable gpsTimeout, networkTimeout;
        };
        final Wrapper wrapper = new Wrapper();

        networkEnabled = mLM.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        gpsEnabled = mLM.isProviderEnabled(LocationManager.GPS_PROVIDER);

        wrapper.locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (predicate.apply(location)) {
                    mLM.removeUpdates(this);
                    mHandler.removeCallbacks(wrapper.networkTimeout);
                    mHandler.removeCallbacks(wrapper.gpsTimeout);
                    listener.onLocationGet(location);
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
            }
        };

        wrapper.gpsTimeout = new Runnable () {
            @Override
            public void run() {
                mLM.removeUpdates(wrapper.locationListener);
                L.d("gps request timed out");
                listener.onLocationGet(null);
            }
        };

        wrapper.networkTimeout = new Runnable () {
            @Override
            public void run() {
                mLM.removeUpdates(wrapper.locationListener);
                L.d("network request timed out");
                if (gpsEnabled) {
                    L.d("requesting gps location");
                    mLM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, wrapper.locationListener, mHandler.getLooper());
                    mHandler.postDelayed(wrapper.gpsTimeout, 30000);
                }
            }
        };

        if (gpsEnabled) {
            l = mLM.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (predicate.apply(l)) {
                L.d("using gps last known location");
                listener.onLocationGet(l);
                return;
            }
        }
        else {
            L.d("gps disabled");
        }
        if (networkEnabled) {
            l = mLM.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (predicate.apply(l)) {
                L.d("using network last known location");
                listener.onLocationGet(l);
                return;
            }
            L.d("requesting network location");
            mLM.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, wrapper.locationListener, mHandler.getLooper());
            mHandler.postDelayed(wrapper.networkTimeout, 5000);
        }
        else {
            L.d("network disabled");
            if (gpsEnabled) {
                L.d("requesting gps location");
                mLM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, wrapper.locationListener, mHandler.getLooper());
                mHandler.postDelayed(wrapper.gpsTimeout, 30000);
            }
            else {
                listener.onLocationGet(null);
                return;
            }
        }
    }

    public interface SenzLocationListener {
        public void onLocationGet(Location location);
    }
}
