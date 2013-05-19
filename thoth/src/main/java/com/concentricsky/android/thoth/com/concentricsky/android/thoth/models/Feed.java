package com.concentricsky.android.thoth.com.concentricsky.android.thoth.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import com.concentricsky.android.thoth.ThothDatabaseHelper;

import java.util.ArrayList;

/**
 * Created by wiggins on 5/18/13.
 */
public class Feed {
    public long _id=0;
    public String url;
    public String link;
    public String title;
    public String description;

    public ArrayList<Tag> tags;
    public ArrayList<Article> articles;


    public void hydrate(Cursor c)
    {
        this._id = c.getLong(c.getColumnIndexOrThrow("_id"));
        this.url = c.getString(c.getColumnIndexOrThrow("url"));
        this.link = c.getString(c.getColumnIndexOrThrow("link"));
        this.title = c.getString(c.getColumnIndexOrThrow("title"));
        this.description = c.getString(c.getColumnIndexOrThrow("description"));
    }

    public static final String FEED_TABLE_NAME = "feed";
    public static final String FEED_TABLE_INSERT = "INSERT INTO " + FEED_TABLE_NAME + " (" +
        "url,"+
        "link,"+
        "title,"+
        "description) VALUES (?,?,?,?);";
    public static final String FEED_TABLE_UPDATE = "UPDATE " + FEED_TABLE_NAME + " SET" +
        "url=?,"+
        "link=?,"+
        "title=?,"+
        "description=? WHERE _id=?";

    public boolean save(SQLiteDatabase db)
    {
        if (db.isReadOnly())
            return false;

        if (this._id != 0) {
            SQLiteStatement feed_update = db.compileStatement(FEED_TABLE_UPDATE);
            feed_update.bindString(1, this.url);
            feed_update.bindString(2, this.link);
            feed_update.bindString(3, this.title);
            feed_update.bindString(4, this.description);
            feed_update.bindLong(5, this._id);
            return feed_update.executeUpdateDelete() > 0;
        }

        SQLiteStatement feed_insert = db.compileStatement(FEED_TABLE_INSERT);
        feed_insert.bindString(1, this.url);
        feed_insert.bindString(2, this.link);
        feed_insert.bindString(3, this.title);
        feed_insert.bindString(4, this.description);
        this._id = feed_insert.executeInsert();
        return true;
    }
}
