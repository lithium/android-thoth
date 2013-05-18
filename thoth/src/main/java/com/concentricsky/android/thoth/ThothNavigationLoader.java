package com.concentricsky.android.thoth;

import android.content.Context;
import android.database.Cursor;
import com.codeslap.gist.SimpleCursorLoader;

/**
 * Created by wiggins on 5/18/13.
 */
public class ThothNavigationLoader extends SimpleCursorLoader {

    public ThothNavigationLoader(Context context) {
        super(context);
    }

    @Override
    public Cursor loadInBackground() {
        return ThothDatabaseHelper.getInstance().getTagCursor();
    }
}
