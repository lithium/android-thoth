package com.concentricsky.android.pensive;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by wiggins on 7/26/13.
 */
public class SyncService extends Service
{
    private static ThothSyncAdapter sSyncAdapter = null;
    private static final Object sSyncAdapterLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new ThothSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
