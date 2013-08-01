package com.concentricsky.android.pensive;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by wiggins on 7/26/13.
 */
public class SyncAuthenticatorService extends Service {
    private SyncAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        mAuthenticator = new SyncAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
