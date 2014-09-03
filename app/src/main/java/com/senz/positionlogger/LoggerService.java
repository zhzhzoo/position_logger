package com.senz.positionlogger;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.os.Process;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.provider.Settings;

import com.android.internal.util.Predicate;
import com.senz.positionlogger.SenzLocation.SenzLocationListener;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class LoggerService extends Service {
    public static final String DATABASE_NAME = "position_logger";
    public static final int DATABASE_VERSION = 1;
    public static final String SHARED_PREFERENCES_NAME = "logger_service_config";
    public static boolean started = false;
    protected static long intervalMillis;
    protected static boolean autoStart;
    protected static String userId;
    protected static WakeLock wakeLock;

    private HandlerThread mHT;
    private Handler mHandler;
    private SenzLocation mSL;
    private LocationRecorder mLR;
    private RemoteReporter mRR;

    @Override
    public void onCreate() {
        super.onCreate();

        mHT = new HandlerThread("LoggerServiceThread", Process.THREAD_PRIORITY_BACKGROUND);
        mHT.start();
        mHandler = new Handler(mHT.getLooper());

        mSL = new SenzLocation();
        mSL.init(this, null);
        initConfiguration(this);
        registerOnSharedPreferenceChangeListener();

        mLR = new LocationRecorder();
        mLR.init(this, DATABASE_NAME, DATABASE_VERSION);

        mRR = RemoteReporter.getRemoteReporter(this);
        L.d("service created");

        started = true;

        mHandler.post(timeoutTask);
    }

    @Override
    public void onDestroy() {
        mHT.quit();
        started = false;
        L.d("service destroyed");

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (wakeLock != null) {
            wakeLock.release();
        }

        return START_STICKY;
    }

    protected static void initConfiguration(Context context) {
        SharedPreferences sp;
        sp = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);

        autoStart = sp.getBoolean("autoStart", false);
        intervalMillis = TimeUnit.MINUTES.toMillis(sp.getInt("interval", 15));
        userId = sp.getString("userId", Settings.Secure.ANDROID_ID);
    }

    private void registerOnSharedPreferenceChangeListener() {
        SharedPreferences sp;
        sp = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Runnable timeoutTask = new Runnable() {
        @Override
        public void run() {
            final long nowMillis = System.currentTimeMillis();
            L.d("timeout, requesting location...");
            mSL.requestLocation(senzLocationListener, new Predicate<Location>() {
                @Override
                public boolean apply(Location location) {
                    return location != null && (nowMillis - location.getTime()) * 3 <= intervalMillis;
                }
            });
        }
    };

    private SenzLocationListener senzLocationListener = new SenzLocationListener() {
        @Override
        public void onLocationGet(Location location) {
            if (location != null) {
                Collection<LocationRecorder.LocationRecord> records = mLR.recordAndGetReports(location);
                if (records != null) {
                    boolean reportSucceeded = true;
                    L.d("ready to report");
                    try {
                        mRR.reportAll(userId, records);
                    }
                    catch (Exception e) {
                        L.d("report error", e);
                        reportSucceeded = false;
                    }
                    if (reportSucceeded) {
                        mLR.setReportSuccessful(records);
                    }
                }
            }
            else {
                L.d("couldn't get location");
            }
            mHandler.postDelayed(timeoutTask, intervalMillis);
        }
    };


    private OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
            switch (key) {
                case "autoStart":
                    autoStart = sp.getBoolean("autoStart", false);
                    break;
                case "interval":
                    intervalMillis = TimeUnit.MINUTES.toMillis(sp.getInt("interval", 15));
                    break;
            }
        }
    };
}
