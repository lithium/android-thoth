package com.concentricsky.android.thoth;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by wiggins on 5/17/13.
 */
public class ThothDatabaseHelper extends SQLiteOpenHelper
{
    private static final String TAG = "ThothDatabaseHelper";



    private static final String DATABASE_NAME = "thoth.db";
    private static final int DATABASE_VERSION = 1;




    private static final String FEED_TABLE_NAME = "feed";
    private static final String FEED_TABLE_CREATE =
        "CREATE TABLE " + FEED_TABLE_NAME + " (" +
            "_id INTEGER PRIMARY KEY,"+
            "link TEXT,"+
            "title TEXT,"+
            "description TEXT,"+
            "timestamp INTEGER,"+
            "unread INTEGER);";
    private static final String FEED_TABLE_DROP = "DROP TABLE IF EXISTS "+FEED_TABLE_NAME+";";


    private static final String TAG_TABLE_NAME = "tag";
    private static final String TAG_TABLE_CREATE =
        "CREATE TABLE " + TAG_TABLE_NAME + " ("+
            "_id INTEGER PRIMARY KEY,"+
            "title TEXT,"+
            "unread INTEGER);";
    private static final String TAG_TABLE_DROP = "DROP TABLE IF EXISTS "+TAG_TABLE_NAME+";";


    private static final String FEEDTAG_TABLE_NAME = "feedtag";
    private static final String FEEDTAG_TABLE_CREATE =
        "CREATE TABLE " + FEEDTAG_TABLE_NAME + " ("+
            "feed_id INTEGER,"+
            "tag_id INTEGER);";
    private static final String FEEDTAG_TABLE_DROP = "DROP TABLE IF EXISTS "+FEEDTAG_TABLE_NAME+";";


    private static final String ARTICLE_TABLE_NAME = "article";
    private static final String ARTICLE_TABLE_CREATE =
        "CREATE TABLE " + ARTICLE_TABLE_NAME + " (" +
            "feed_id INTEGER,"+
            "guid TEXT,"+
            "link TEXT,"+
            "title TEXT,"+
            "description TEXT,"+
            "timestamp INTEGER);";
    private static final String ARTICLE_TABLE_DROP = "DROP TABLE IF EXISTS "+ARTICLE_TABLE_NAME+";";



    public ThothDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(FEED_TABLE_CREATE);
        sqLiteDatabase.execSQL(TAG_TABLE_CREATE);
        sqLiteDatabase.execSQL(FEEDTAG_TABLE_CREATE);
        sqLiteDatabase.execSQL(ARTICLE_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        sqLiteDatabase.execSQL(FEED_TABLE_DROP);
        sqLiteDatabase.execSQL(TAG_TABLE_DROP);
        sqLiteDatabase.execSQL(FEEDTAG_TABLE_DROP);
        sqLiteDatabase.execSQL(ARTICLE_TABLE_DROP);
        onCreate(sqLiteDatabase);

    }
}
