package com.concentricsky.android.thoth.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

/**
 * Created by wiggins on 5/18/13.
 */
public class Tag {
    public long _id;
    public String title;

    public static final int DATABASE_VERSION = 10;

    public static final String TAG_TABLE_NAME = "tag";
    public static final String TAG_VIEW_NAME = "tagview";
    public static final String TAG_TABLE_CREATE =
            "CREATE TABLE " + TAG_TABLE_NAME + " ("+
                    "_id INTEGER PRIMARY KEY,"+
                    "title TEXT,"+
                    "ordering INTEGER default 99);";
    public static final String TAG_VIEW_CREATE =  "CREATE VIEW IF NOT EXISTS "+TAG_VIEW_NAME+" AS "+
            "SELECT tag.*,(select count(*) from article where article.unread=1 and feed_id in (select feed_id from feedtag where tag_id=tag._id)) as unread"+
            " FROM "+TAG_TABLE_NAME+";";
    public static final String TAG_TABLE_INSERT = "INSERT INTO " +TAG_TABLE_NAME + " (title) VALUES (?);";
    public static final String TAG_TABLE_DROP = "DROP TABLE IF EXISTS "+TAG_TABLE_NAME+"; DROP VIEW IF EXISTS "+TAG_VIEW_NAME+";";



    public static void createDatabase(SQLiteDatabase db)
    {
        db.execSQL(TAG_TABLE_CREATE);
        db.execSQL("INSERT INTO "+TAG_TABLE_NAME+ " (_id,ordering,title) VALUES (1,0,?)", new String[] {"All Feeds"});
        db.execSQL("INSERT INTO "+TAG_TABLE_NAME+ " (_id,ordering,title) VALUES (2,100,?)", new String[] {"Unfiled"});
    }
    public static void upgradeDatabase(SQLiteDatabase db, int i, int i2)
    {
        db.execSQL(TAG_TABLE_DROP);
        createDatabase(db);
    }
    public static void createView(SQLiteDatabase db)
    {
        db.execSQL(TAG_VIEW_CREATE);
    }


    public void hydrate(Cursor c)
    {
        this._id = c.getLong(c.getColumnIndexOrThrow("_id"));
        this.title = c.getString(c.getColumnIndexOrThrow("title"));
    }


    public static Tag getOrCreate(SQLiteDatabase db, String title)
    {
        Tag tag = new Tag();
        Cursor c = db.rawQuery("SELECT * FROM "+ TAG_TABLE_NAME+" WHERE title=?", new String[] {title});
        if (c.moveToFirst()) {
            tag.hydrate(c);
            return tag;
        }

        SQLiteStatement stmt = db.compileStatement(TAG_TABLE_INSERT);
        stmt.bindString(1, title);
        //hydrate...
        tag._id = stmt.executeInsert();
        tag.title = title;
        return tag;

    }

    public static Cursor load(SQLiteDatabase readableDatabase, long tag_id) {
        return readableDatabase.rawQuery("SELECT * FROM "+TAG_TABLE_NAME+ " WHERE _id=?", new String[] {String.valueOf(tag_id)});
    }
}
