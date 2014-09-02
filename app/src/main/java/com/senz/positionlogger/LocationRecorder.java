package com.senz.positionlogger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class LocationRecorder {
    private SQLiteDatabase mDB;
    private static final String LOCATION_RECORDS_TABLE_NAME = "location_records";
    private static final String KEY_ID = "id";
    private static final String KEY_TIME_RETRIEVED = "time_retrieved";
    private static final String KEY_REPORTED = "reported";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_PROVIDER = "provider";
    private static final String KEY_ACCURACY = "accuracy";
    private static final String KEY_ALTITUDE = "altitude";
    private static final String KEY_BEARING = "bearing";
    private static final String KEY_SPEED = "speed";

    private SQLiteStatement mItemsReadyCountStatement;
    private ContentValues mReportedTrue;

    private class LocationRecordsOpenHelper extends SQLiteOpenHelper {
        private final String LOCATION_RECORDS_TABLE_CREATE =
                "CREATE TABLE " + LOCATION_RECORDS_TABLE_NAME + " (" +
                        KEY_ID + " INTEGER PRIMARY KEY, " +
                        KEY_TIME_RETRIEVED + " DATETIME default CURRENT_TIMESTAMP, " +
                        KEY_REPORTED + " INTEGER default 0, " +
                        KEY_LATITUDE + " DOUBLE NOT NULL, " +
                        KEY_LONGITUDE + " DOUBLE NOT NULL, " +
                        KEY_TIMESTAMP + " DATETIME NOT NULL, " +
                        KEY_PROVIDER + " CHAR(8) NOT NULL, " +
                        KEY_ACCURACY + " FLOAT, " +
                        KEY_ALTITUDE + " DOUBLE, " +
                        KEY_BEARING + " FLOAT, " +
                        KEY_SPEED + " FLOAT);";

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(LOCATION_RECORDS_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int newVersion, int oldVersion) {
            throw new UnsupportedOperationException();
        }

        LocationRecordsOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }
    };

    public void init(Context context, String name, int version) {
        mDB = (new LocationRecordsOpenHelper(context, name, null, version)).getWritableDatabase();

        String itemsReadyCountSQL = "SELECT COUNT(*) FROM " + LOCATION_RECORDS_TABLE_NAME + " WHERE " + KEY_REPORTED + " = 0;";
        mItemsReadyCountStatement = mDB.compileStatement(itemsReadyCountSQL);

        mReportedTrue = new ContentValues();
        mReportedTrue.put(KEY_REPORTED, 1);
    }

    ContentValues locationToContentValues(Location location) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_LATITUDE, location.getLatitude());
        cv.put(KEY_LONGITUDE, location.getLongitude());
        cv.put(KEY_TIMESTAMP, location.getTime());
        cv.put(KEY_PROVIDER, location.getProvider());
        if (location.hasAccuracy()) {
            cv.put(KEY_ACCURACY, location.getAccuracy());
        }
        if (location.hasAltitude()) {
            cv.put(KEY_ALTITUDE, location.getAltitude());
        }
        if (location.hasBearing()) {
            cv.put(KEY_BEARING, location.getBearing());
        }
        if (location.hasSpeed()) {
            cv.put(KEY_SPEED, location.getSpeed());
        }
        return cv;
    }

    public Collection<LocationRecord> recordAndGetReports(Location location) {
        ContentValues cv = locationToContentValues(location);
        cv.put(KEY_TIME_RETRIEVED, System.currentTimeMillis());
        long res = mDB.insertOrThrow(LOCATION_RECORDS_TABLE_NAME, null, cv);
        L.d("just inserted: " + Long.toString(res));
        L.d("items ready: " + Long.toString(mItemsReadyCountStatement.simpleQueryForLong()));
        if (mItemsReadyCountStatement.simpleQueryForLong() < 10) {
            return null;
        }
        else {
            Collection<LocationRecord> lrs = new ArrayList<>();
            Cursor cursor = mDB.query(LOCATION_RECORDS_TABLE_NAME, null, KEY_REPORTED + " = 0", null, null, null, KEY_ID, "10");
            while (cursor.moveToNext()) {
                lrs.add(LocationRecord.fromCursorRow(cursor));
            }
            cursor.close();
            return lrs;
        }
    }

    public void setReportSuccessful(Collection<LocationRecord> lrs) {
        StringBuilder sb = new StringBuilder();
        Iterator<LocationRecord> it = lrs.iterator();
        sb.append(KEY_ID);
        sb.append(" IN (");
        sb.append(it.next().getId());
        while (it.hasNext()) {
            sb.append(",");
            sb.append(it.next().getId());
        }
        sb.append(")");

        mDB.update(LOCATION_RECORDS_TABLE_NAME, mReportedTrue, sb.toString(), null);
    }

    public static class LocationRecord extends Location {
        int mId;
        long mTimeRetrieved;
        boolean mReported;
        LocationRecord(int id, long timeRetrieved, boolean reported, Location location) {
            super(location);
            mId = id;
            mTimeRetrieved = timeRetrieved;
            mReported = reported;
        }

        public static LocationRecord fromCursorRow(Cursor cursor) {
            int id = -1;
            long time_retrieved = -1;
            boolean reported = false;
            Location l = new Location("");
            int columnCount = cursor.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                switch (cursor.getColumnName(i)) {
                    case KEY_ID:
                        id = cursor.getInt(i);
                        break;
                    case KEY_TIME_RETRIEVED:
                        time_retrieved = cursor.getLong(i);
                        break;
                    case KEY_REPORTED:
                        reported = (cursor.getInt(i) == 1);
                        break;
                    case KEY_LATITUDE:
                        l.setLatitude(cursor.getDouble(i));
                        break;
                    case KEY_LONGITUDE:
                        l.setLongitude(cursor.getDouble(i));
                        break;
                    case KEY_PROVIDER:
                        l.setProvider(cursor.getString(i));
                        break;
                    case KEY_TIMESTAMP:
                        l.setTime(cursor.getLong(i));
                        break;
                    case KEY_ACCURACY:
                        l.setAccuracy(cursor.getFloat(i));
                        break;
                    case KEY_ALTITUDE:
                        l.setAltitude(cursor.getFloat(i));
                        break;
                    case KEY_BEARING:
                        l.setBearing(cursor.getFloat(i));
                        break;
                    case KEY_SPEED:
                        l.setSpeed(cursor.getFloat(i));
                        break;
                }
            }
            return new LocationRecord(id, time_retrieved, reported, l);
        }

        int getId() {
            return mId;
        }

        long getTimeRetrieved() {
            return mTimeRetrieved;
        }

        boolean getReported() {
            return mReported;
        }
    }
}
