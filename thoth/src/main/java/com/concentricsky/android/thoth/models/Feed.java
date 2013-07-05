package com.concentricsky.android.thoth.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;

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
    public long ttl = 900;
    public long timestamp;


    public String[] tags;
    public ArrayList<Article> articles;

    public static final int DATABASE_VERSION = 6;

    public static final String FEED_TABLE_NAME = "feed";
    public static final String FEED_TABLE_CREATE =
            "CREATE TABLE " + FEED_TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY,"+
                    "url TEXT,"+
                    "link TEXT,"+
                    "title TEXT,"+
                    "description TEXT,"+
                    "ttl INTEGER,"+
                    "timestamp INTEGER,"+
                    "unread INTEGER);";
    public static final String FEED_TABLE_DROP = "DROP TABLE IF EXISTS "+FEED_TABLE_NAME+";";
    public static final String FEED_TABLE_INSERT = "INSERT INTO " + FEED_TABLE_NAME + " (" +
            "url,"+
            "link,"+
            "title,"+
            "ttl,"+
            "description,"+
            "timestamp) VALUES (?,?,?,?,?,strftime('%s', 'now'));";
    public static final String FEED_TABLE_UPDATE = "UPDATE " + FEED_TABLE_NAME + " SET " +
            "url=?,"+
            "link=?,"+
            "title=?,"+
            "ttl=?,"+
            "description=?,"+
            "timestamp=strftime('%s', 'now') WHERE _id=?";



    public static final String FEEDTAG_TABLE_NAME = "feedtag";
    public static final String FEEDTAG_TABLE_CREATE =
            "CREATE TABLE " + FEEDTAG_TABLE_NAME + " ("+
                    "feed_id INTEGER,"+
                    "tag_id INTEGER);" +
                    "CREATE INDEX feedtag_feed_idx ON "+FEEDTAG_TABLE_NAME+" (feed_id);" +
                    "CREATE INDEX feedtag_tag_idx ON "+FEEDTAG_TABLE_NAME+" (tag_id);";
    public static final String FEEDTAG_TABLE_DROP = "DROP TABLE IF EXISTS "+FEEDTAG_TABLE_NAME+";";
    public static final String FEEDTAG_TABLE_INSERT = "INSERT INTO " + FEEDTAG_TABLE_NAME + " (feed_id,tag_id) VALUES (?,?);";




    public static void createDatabase(SQLiteDatabase db)
    {
        db.execSQL(FEED_TABLE_CREATE);
        db.execSQL(FEEDTAG_TABLE_CREATE);
    }
    public static void upgradeDatabase(SQLiteDatabase db, int i, int i2)
    {
        db.execSQL(FEED_TABLE_DROP);
        db.execSQL(FEEDTAG_TABLE_DROP);
        createDatabase(db);
    }

    public void hydrate(Cursor c)
    {
        this._id = c.getLong(c.getColumnIndexOrThrow("_id"));
        this.url = c.getString(c.getColumnIndexOrThrow("url"));
        this.link = c.getString(c.getColumnIndexOrThrow("link"));
        this.title = c.getString(c.getColumnIndexOrThrow("title"));
        this.description = c.getString(c.getColumnIndexOrThrow("description"));
        this.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
    }

    public boolean save(SQLiteDatabase db)
    {
        if (db.isReadOnly())
            return false;

        if (this._id != 0) {
            SQLiteStatement feed_update = db.compileStatement(FEED_TABLE_UPDATE);
            feed_update.bindString(1, this.url);
            feed_update.bindString(2, this.link);
            feed_update.bindString(3, this.title);
            feed_update.bindLong(4, this.ttl);
            feed_update.bindString(5, this.description);
            feed_update.bindLong(6, this._id);
            if (feed_update.executeUpdateDelete() < 1) {
                return false;
            };

            SQLiteStatement stmt = db.compileStatement("UPDATE "+Feed.FEED_TABLE_NAME+" SET unread=(SELECT COUNT(_id) FROM "+Article.ARTICLE_TABLE_NAME+" WHERE unread=1 AND feed_id=?) WHERE _id=?");
            stmt.bindLong(1, this._id);
            stmt.bindLong(2, this._id);
            stmt.executeUpdateDelete();

            stmt = db.compileStatement("UPDATE tag SET unread=(SELECT SUM(unread) FROM feed JOIN feedtag ON feed._id=feed_id WHERE tag_id=tag._id) WHERE _id IN (SELECT tag_id FROM feedtag WHERE feed_id=?)");
            stmt.bindLong(1, this._id);
            stmt.executeUpdateDelete();
        }
        else {
            Cursor c = db.rawQuery("SELECT _id FROM "+FEED_TABLE_NAME+ " WHERE url=?", new String[] {this.url});
            if (c.moveToFirst()) {
                return false; // feed with url already exists
            }

            SQLiteStatement feed_insert = db.compileStatement(FEED_TABLE_INSERT);
            feed_insert.bindString(1, this.url);
            feed_insert.bindString(2, link != null ? link : "");
            feed_insert.bindString(3, title != null ? title : "");
            feed_insert.bindLong(4, ttl);
            feed_insert.bindString(5, description != null ? description : "");
            this._id = feed_insert.executeInsert();
        }

        if (this.tags != null) {
            //drop all old tag associations
            db.execSQL("DELETE FROM "+FEEDTAG_TABLE_NAME+ " WHERE feed_id=?", new String[] {String.valueOf(this._id)});
            //getorcreate tags and add feed to them
            for (String tag_name : this.tags) {
                Tag tag = Tag.getOrCreate(db, tag_name.trim());

                SQLiteStatement feedtag_insert = db.compileStatement(FEEDTAG_TABLE_INSERT);
                feedtag_insert.bindLong(1, this._id);
                feedtag_insert.bindLong(2, tag._id);
                feedtag_insert.executeInsert();

            }
        }


        if (this.articles != null) {
            for (Article article : this.articles) {
                article.feed_id = this._id;
                article.save(db);
            }

        }

        return true;
    }


    public static Cursor load(SQLiteDatabase db, long feed_id) {
        return db.rawQuery("SELECT * FROM "+FEED_TABLE_NAME+ " WHERE _id=?", new String[] {String.valueOf(feed_id)});
    }

    public void asyncSave(final SQLiteDatabase db) {
        AsyncTask<Feed, Void, Void> task = new AsyncTask<Feed, Void, Void>() {
            @Override
            protected Void doInBackground(Feed... feeds) {
                for (Feed feed : feeds) {
                    feed.save(db);
                }
                return null;
            }
        };
        task.execute(this);
    }
}
