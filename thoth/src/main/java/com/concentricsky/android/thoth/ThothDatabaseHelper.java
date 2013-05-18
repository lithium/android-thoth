package com.concentricsky.android.thoth;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

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
    private static final String FEED_TABLE_INSERT = "INSERT INTO " + FEED_TABLE_NAME + " (link,title) VALUES (?,?);";
    private static final String FEED_TABLE_DROP = "DROP TABLE IF EXISTS "+FEED_TABLE_NAME+";";


    private static final String TAG_TABLE_NAME = "tag";
    private static final String TAG_TABLE_INSERT = "INSERT INTO " +TAG_TABLE_NAME + " (title) VALUES (?);";
    private static final String TAG_TABLE_CREATE =
        "CREATE TABLE " + TAG_TABLE_NAME + " ("+
            "_id INTEGER PRIMARY KEY,"+
            "title TEXT,"+
            "unread INTEGER);";
    private static final String TAG_TABLE_DROP = "DROP TABLE IF EXISTS "+TAG_TABLE_NAME+";";


    private static final String FEEDTAG_TABLE_NAME = "feedtag";
    private static final String FEEDTAG_TABLE_INSERT = "INSERT INTO " + FEEDTAG_TABLE_NAME + " (feed_id,tag_id) VALUES (?,?);";
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

    public long addTag(String title)
    {
        SQLiteDatabase db = getWritableDatabase();
        SQLiteStatement stmt = db.compileStatement(TAG_TABLE_INSERT);
        stmt.bindString(1, title);
        return stmt.executeInsert();
    }

    public long addFeed(String link, String title, long[] tags)
    {
        SQLiteDatabase db = getWritableDatabase();
        SQLiteStatement feed_insert = db.compileStatement(FEED_TABLE_INSERT);
        feed_insert.bindString(1, link);
        feed_insert.bindString(2, title);
        long feed_id = feed_insert.executeInsert();
        updateFeedTags(feed_id, tags);

        return feed_id;
    }

    public int renameFeed(long feed_id, String title)
    {
        SQLiteDatabase db = getWritableDatabase();
        SQLiteStatement feed_update = db.compileStatement("UPDATE " + FEED_TABLE_NAME + " SET title=? WHERE feed_id=?");
        feed_update.bindString(1, title);
        feed_update.bindLong(2, feed_id);
        return feed_update.executeUpdateDelete();
    }

    public void updateFeedTags(long feed_id, long[] tags)
    {
        SQLiteDatabase db = getWritableDatabase();

        db.execSQL("DELETE FROM " + FEEDTAG_TABLE_NAME + " WHERE feed_id=?;", new String[] {String.valueOf(feed_id)});

        SQLiteStatement feedtag_insert = db.compileStatement(FEEDTAG_TABLE_INSERT);
        for (long tag_id : tags) {
            feedtag_insert.clearBindings();
            feedtag_insert.bindLong(1, feed_id);
            feedtag_insert.bindLong(2, tag_id);
            feedtag_insert.executeInsert();
        }
    }

    public Cursor getTagCursor()
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT _id,title FROM " + TAG_TABLE_NAME + " ORDER BY title", null);
        if (!c.moveToFirst()) {
            return null;
        }
        return c;
    }

    public Cursor getFeedCursor(int tag_id)
    {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT _id,title FROM "+FEED_TABLE_NAME+" JOIN "+FEEDTAG_TABLE_NAME+" ON feed_id=_id WHERE tag_id=?", new String[] {String.valueOf(tag_id)});
    }
}
