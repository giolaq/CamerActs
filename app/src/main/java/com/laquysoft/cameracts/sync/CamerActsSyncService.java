package com.laquysoft.cameracts.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by joaobiriba on 10/12/14.
 */
public class CamerActsSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static CamerActsSyncAdapter sCamerActsSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("CamerActsSyncService", "onCreate - CamerActsSyncService");
        synchronized (sSyncAdapterLock) {
            if (sCamerActsSyncAdapter == null) {
                sCamerActsSyncAdapter = new CamerActsSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sCamerActsSyncAdapter.getSyncAdapterBinder();
    }
}