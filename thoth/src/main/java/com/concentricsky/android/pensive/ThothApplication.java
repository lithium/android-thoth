package com.concentricsky.android.pensive;

import android.app.Application;

/**
 * Created by wiggins on 5/18/13.
 */
public class ThothApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // initialize the database helper singleton instance
        ThothDatabaseHelper dbh = ThothDatabaseHelper.getInstance();
        dbh.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

//        ThothDatabaseHelper.getInstance().close();
    }
}
