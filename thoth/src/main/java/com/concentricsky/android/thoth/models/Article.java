package com.concentricsky.android.thoth.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.util.Log;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by wiggins on 5/18/13.
 */
public class Article implements Serializable
{
    private static final long serialVersionUID = 3L;

    public long _id;
    public long feed_id;
    public String link;
    public String title;
    public String description;
    public String guid;
    public Date timestamp;
    public int unread=1;

    public static final int DATABASE_VERSION = 7;

    public static final String ARTICLE_TABLE_NAME = "article";
    public static final String ARTICLE_TABLE_CREATE =
            "CREATE TABLE " + ARTICLE_TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY,"+
                    "feed_id INTEGER,"+
                    "guid TEXT,"+
                    "link TEXT,"+
                    "title TEXT,"+
                    "description TEXT,"+
                    "unread INTEGER,"+
                    "timestamp INTEGER);";
    public static final String ARTICLE_TABLE_DROP = "DROP TABLE IF EXISTS "+ARTICLE_TABLE_NAME+";";
    public static final String ARTICLE_TABLE_INSERT = "INSERT INTO " + ARTICLE_TABLE_NAME + " (" +
            "feed_id,"+
            "guid,"+
            "link,"+
            "title,"+
            "description," +
            "unread,"+
            "timestamp) VALUES (?,?,?,?,?,?,?);";

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
            //update read/unread
            SQLiteStatement stmt = db.compileStatement("UPDATE " + ARTICLE_TABLE_NAME + " SET unread=0 WHERE _id=?");
            stmt.bindLong(1, this._id);
            stmt.executeUpdateDelete();

        } else {
            if (this.guid == null) {
                this.guid = this.link;
            }
            Cursor c = db.rawQuery("SELECT _id FROM "+ARTICLE_TABLE_NAME+ " WHERE feed_id=? AND guid=?", new String[] {
                                                                                    String.valueOf(this.feed_id),
                                                                                    this.guid});
            if (c != null && c.moveToFirst()) {
                // article with this guid already exists! we're done...
                return true;
            }


            SQLiteStatement stmt = db.compileStatement(ARTICLE_TABLE_INSERT);
            stmt.bindLong(1, this.feed_id);
            stmt.bindString(2, this.guid);
            stmt.bindString(3, this.link);
            stmt.bindString(4, title != null ? title : "no title");
            stmt.bindString(5, description != null ? description : "");
            stmt.bindLong(6, 1);
            stmt.bindLong(7, timestamp != null ? timestamp.getTime() : System.currentTimeMillis());
            this._id = stmt.executeInsert();
        }
        return true;
    }

    public static Cursor load(SQLiteDatabase db, long article_id) {
        return db.rawQuery("SELECT * FROM "+ARTICLE_TABLE_NAME+ " WHERE _id=?", new String[] {String.valueOf(article_id)});
    }

    public void hydrate(Cursor c) {
        this._id = c.getLong(c.getColumnIndexOrThrow("_id"));
        this.feed_id = c.getLong(c.getColumnIndexOrThrow("feed_id"));
        this.link = c.getString(c.getColumnIndexOrThrow("link"));
        this.title = c.getString(c.getColumnIndexOrThrow("title"));
        this.description = c.getString(c.getColumnIndexOrThrow("description"));
        this.unread = c.getInt(c.getColumnIndexOrThrow("unread"));
        this.guid = c.getString(c.getColumnIndexOrThrow("guid"));

    }

    public void asyncSave(final SQLiteDatabase db) {
        AsyncTask<Article, Void, Void> task = new AsyncTask<Article, Void, Void>() {
            @Override
            protected Void doInBackground(Article... articles) {
                for (Article article : articles) {
                    article.save(db);
                }
                return null;
            }
        };
        task.execute(this);
    }
}
