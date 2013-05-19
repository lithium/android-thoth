package com.concentricsky.android.thoth.com.concentricsky.android.thoth.models;

import android.database.Cursor;

/**
 * Created by wiggins on 5/18/13.
 */
public class Tag {
    public long _id;
    public String title;



    public void hydrate(Cursor c)
    {
        this._id = c.getLong(c.getColumnIndexOrThrow("_id"));
        this.title = c.getString(c.getColumnIndexOrThrow("title"));
    }


}
