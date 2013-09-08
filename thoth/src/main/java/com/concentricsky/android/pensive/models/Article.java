package com.concentricsky.android.pensive.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;

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
    public String feed_title;

    public static final int DATABASE_VERSION = 12;

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
        db.execSQL("CREATE INDEX IF NOT EXISTS timestamp_idx ON "+ARTICLE_TABLE_NAME+" (timestamp);");
        db.execSQL("CREATE INDEX IF NOT EXISTS unread_idx ON "+ARTICLE_TABLE_NAME+" (unread);");
        db.execSQL("CREATE INDEX IF NOT EXISTS feed_id ON "+ARTICLE_TABLE_NAME+" (feed_id);");
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


            if (this.link == null || this.guid == null) {
                return false;
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
        this.timestamp = new Date(c.getLong(c.getColumnIndexOrThrow("timestamp")));

        int feed_idx = c.getColumnIndex("feed_title");
        if (feed_idx != -1) {
            this.feed_title = c.getString(feed_idx);
        }

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

    static public boolean markAsRead(SQLiteDatabase db, long article_id) {
        if (db.isReadOnly())
            return false;
        SQLiteStatement stmt = db.compileStatement("UPDATE " + ARTICLE_TABLE_NAME + " SET unread=0 WHERE _id=?");
        stmt.bindLong(1, article_id);
        return stmt.executeUpdateDelete() == 1;
    }
}
