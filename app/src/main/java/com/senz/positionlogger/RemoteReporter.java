package com.senz.positionlogger;

import android.content.Context;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.senz.positionlogger.LocationRecorder.LocationRecord;

public class RemoteReporter {
    private static RemoteReporter theRemoteReporter = null;
    public static RemoteReporter getRemoteReporter(Context context) {
        if (theRemoteReporter != null) {
            return theRemoteReporter;
        }
        else {
            AVOSCloud.initialize(context, "vigxpgtjk8w6ruxcfaw4kju3ssyttgcqz38y6y6uablqivjd", "dxbawm2hh0338hb37wap59gticgr92dpajd80tzekrgv1ptw");
            return new RemoteReporter();
        }
    }

    private RemoteReporter() {
    }

    public void reportAll(String userId, Collection<LocationRecord> lrs) throws AVException {
        List<AVObject> avobjs = new ArrayList<>();
        L.d("===== reporting =====");
        for (LocationRecorder.LocationRecord i : lrs) {
            AVObject avo = locationRecordToAVObject(i);
            avo.put("userId", userId);
            L.d(avo.toString());
            avobjs.add(avo);
        }
        L.d("=====================");
        AVObject.saveAll(avobjs);
    }

    public static AVObject locationRecordToAVObject(LocationRecord lrs) {
        AVObject res = new AVObject("location_record");
        res.put("id", lrs.getId());
        res.put("time_retrieved", lrs.getTimeRetrieved());
        res.put("latitude", lrs.getLatitude());
        res.put("longitude", lrs.getLongitude());
        res.put("provider", lrs.getProvider());
        res.put("timestamp", lrs.getTime());
        if (lrs.hasAltitude()) {
            res.put("altitude", lrs.getAltitude());
        }
        if (lrs.hasAccuracy()) {
            res.put("accuracy", lrs.getAccuracy());
        }
        if (lrs.hasBearing()) {
            res.put("bearing", lrs.getBearing());
        }
        if (lrs.hasSpeed()) {
            res.put("speed", lrs.getSpeed());
        }
        return res;
    }
}
