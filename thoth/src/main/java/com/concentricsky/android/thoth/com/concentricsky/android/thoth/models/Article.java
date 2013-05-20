package com.concentricsky.android.thoth.com.concentricsky.android.thoth.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by wiggins on 5/18/13.
 */
public class Article {
    public Feed feed;
    public String link;
    public String title;
    public String description;
    public String guid;
    public long timestamp;

    public static final int DATABASE_VERSION = 2;
    public static final String ARTICLE_TABLE_NAME = "article";
    public static final String ARTICLE_TABLE_CREATE =
            "CREATE TABLE " + ARTICLE_TABLE_NAME + " (" +
                    "feed_id INTEGER,"+
                    "guid TEXT,"+
                    "link TEXT,"+
                    "title TEXT,"+
                    "description TEXT,"+
                    "timestamp INTEGER);";
    public static final String ARTICLE_TABLE_DROP = "DROP TABLE IF EXISTS "+ARTICLE_TABLE_NAME+";";

    public static void createDatabase(SQLiteDatabase db)
    {
        db.execSQL(ARTICLE_TABLE_CREATE);
    }
    public static void upgradeDatabase(SQLiteDatabase db, int i, int i2)
    {
        db.execSQL(ARTICLE_TABLE_DROP);
        createDatabase(db);
    }






}
