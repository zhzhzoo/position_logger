package com.senz.positionlogger;

import android.util.Log;

public class L {
    public static final String TAG = "positionlogger";
    public static void d(String str) {
        Log.d(TAG, str);
    }

    public static void d(String str, Exception e) {
        Log.d(TAG, str, e);
    }
}
