package com.concentricsky.android.thoth.com.concentricsky.android.thoth.models;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

/**
 * Created by wiggins on 5/18/13.
 */
public class Article {
    public long _id;
    public long feed_id;
    public String link;
    public String title;
    public String description;
    public String guid;
    public long timestamp;

    public static final int DATABASE_VERSION = 3;

    public static final String ARTICLE_TABLE_NAME = "article";
    public static final String ARTICLE_TABLE_CREATE =
            "CREATE TABLE " + ARTICLE_TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY,"+
                    "feed_id INTEGER,"+
                    "guid TEXT,"+
                    "link TEXT,"+
                    "title TEXT,"+
                    "description TEXT,"+
                    "timestamp INTEGER);";
    public static final String ARTICLE_TABLE_DROP = "DROP TABLE IF EXISTS "+ARTICLE_TABLE_NAME+";";
    public static final String ARTICLE_TABLE_INSERT = "INSERT INTO " + ARTICLE_TABLE_NAME + " (" +
            "feed_id,"+
            "guid,"+
            "link,"+
            "title,"+
            "description) VALUES (?,?,?,?,?);";

    public static void createDatabase(SQLiteDatabase db)
    {
        db.execSQL(ARTICLE_TABLE_CREATE);
    }
    public static void upgradeDatabase(SQLiteDatabase db, int i, int i2)
    {
        db.execSQL(ARTICLE_TABLE_DROP);
        createDatabase(db);
    }


    public boolean save(SQLiteDatabase db) {
        if (db.isReadOnly())
            return false;
        if (this.feed_id < 1)
            return false;

        if (this._id != 0) {
            //TODO: update read/unread

        } else {
            SQLiteStatement stmt = db.compileStatement(ARTICLE_TABLE_INSERT);
            stmt.bindLong(1, this.feed_id);
            stmt.bindString(2, this.guid);
            stmt.bindString(3, this.link);
            stmt.bindString(4, this.title);
            stmt.bindString(5, description);
            this._id = stmt.executeInsert();
        }
        return true;
    }
}
