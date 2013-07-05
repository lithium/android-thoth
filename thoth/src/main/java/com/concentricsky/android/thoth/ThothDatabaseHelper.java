package com.concentricsky.android.thoth;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.concentricsky.android.thoth.models.Article;
import com.concentricsky.android.thoth.models.Feed;
import com.concentricsky.android.thoth.models.Tag;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by wiggins on 5/17/13.
 */
public class ThothDatabaseHelper
{
    private static ThothDatabaseHelper instance = null;
    private ThothOpenHelper mOpenHelper;

    public ThothDatabaseHelper() {  }


    public SQLiteDatabase getWritableDatabase() {
        if (mOpenHelper != null)
            return mOpenHelper.getWritableDatabase();
        return null;
    }
    public SQLiteDatabase getReadableDatabase() {
        if (mOpenHelper != null)
            return mOpenHelper.getReadableDatabase();
        return null;
    }

    public static ThothDatabaseHelper getInstance() {
        if (instance == null) {
            instance = new ThothDatabaseHelper();
        }
        return instance;
    }

    public void init(Context context) {
       mOpenHelper = new ThothOpenHelper(context);
    }



    private static final String TAG = "ThothDatabaseHelper";



    private static final String DATABASE_NAME = "thoth.db";
    private static final int DATABASE_VERSION = Tag.DATABASE_VERSION + Feed.DATABASE_VERSION + Article.DATABASE_VERSION;



    private class ThothOpenHelper extends SQLiteOpenHelper {

        public ThothOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            Tag.createDatabase(sqLiteDatabase);
            Feed.createDatabase(sqLiteDatabase);
            Article.createDatabase(sqLiteDatabase);

        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
            Tag.upgradeDatabase(sqLiteDatabase, i, i2);
            Feed.upgradeDatabase(sqLiteDatabase, i, i2);
            Article.upgradeDatabase(sqLiteDatabase, i, i2);
        }

    }


    public ArrayList<Feed> importTakeoutZip(InputStream zipfile) {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(zipfile));
        try {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String name = ze.getName();
                if (name.endsWith("Reader/subscriptions.xml")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                    }
                    return OpmlParser.parse(baos.toString());
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }


    public Cursor getTagCursor()
    {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + Tag.TAG_TABLE_NAME + " ORDER BY ordering,title COLLATE NOCASE ASC", null);
        if (!c.moveToFirst())
            return null;
        return c;
    }

    public Cursor getAllFeedsCursor()
    {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM "+Feed.FEED_TABLE_NAME, null);
        if (!c.moveToFirst())
            return null;
        return c;
    }

    public Cursor getFeedCursor(long tag_id)
    {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT "+Feed.FEED_TABLE_NAME+".* FROM "+Feed.FEED_TABLE_NAME+" JOIN "+Feed.FEEDTAG_TABLE_NAME+" ON feed_id=_id WHERE tag_id=?", new String[] {String.valueOf(tag_id)});
        if (!c.moveToFirst())
            return null;
        return c;
    }

    public Cursor getArticleCursor(long feed_id, boolean hide_unread) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = null;
        if (feed_id == 0) { //all articles
            c = db.rawQuery("SELECT "+Article.ARTICLE_TABLE_NAME+".*,feed.title as feed_title FROM "+Article.ARTICLE_TABLE_NAME+
                                " JOIN "+Feed.FEED_TABLE_NAME+" ON feed_id="+Feed.FEED_TABLE_NAME+"._id "+
                                (hide_unread ? " WHERE article.unread=1" : "")+
                                " ORDER BY timestamp DESC",null);
        }
        else {
            c = db.rawQuery("SELECT "+Article.ARTICLE_TABLE_NAME+".*,feed.title as feed_title FROM "+Article.ARTICLE_TABLE_NAME+
                    " JOIN "+Feed.FEED_TABLE_NAME+" ON feed_id="+Feed.FEED_TABLE_NAME+"._id "+
                    " WHERE feed_id=?"+
                    (hide_unread ? " AND article.unread=1" : "")+
                    " ORDER BY timestamp DESC", new String[] {String.valueOf(feed_id)});
        }
        if (c == null || !c.moveToFirst())
            return null;
        return c;
    }

    public Cursor getArticleCursorByTag(long tag_id, boolean hide_unread) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = null;
        c = db.rawQuery("SELECT "+Article.ARTICLE_TABLE_NAME+".*,feed.title as feed_title FROM "+Article.ARTICLE_TABLE_NAME+
                            " JOIN "+Feed.FEEDTAG_TABLE_NAME+" ON "+Article.ARTICLE_TABLE_NAME+".feed_id="+Feed.FEEDTAG_TABLE_NAME+".feed_id "+
                            " JOIN "+Feed.FEED_TABLE_NAME+" ON "+Feed.FEED_TABLE_NAME+"._id="+Feed.FEEDTAG_TABLE_NAME+".feed_id "+
                            " WHERE feedtag.tag_id=? "+
                            (hide_unread ? " AND article.unread=1" : "")+
                            "ORDER BY timestamp DESC", new String[] {String.valueOf(tag_id)});
        if (c == null || !c.moveToFirst()) {
            return null;
        }
        return c;
    }



//
//    public long addFeed(String link, String title, long[] tags)
//    {
//        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//        SQLiteStatement feed_insert = db.compileStatement(FEED_TABLE_INSERT);
//        feed_insert.bindString(1, link);
//        feed_insert.bindString(2, title);
//        long feed_id = feed_insert.executeInsert();
//        updateFeedTags(feed_id, tags);
//
//        return feed_id;
//    }
//
//    public int renameFeed(long feed_id, String title)
//    {
//        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//        SQLiteStatement feed_update = db.compileStatement("UPDATE " + FEED_TABLE_NAME + " SET title=? WHERE feed_id=?");
//        feed_update.bindString(1, title);
//        feed_update.bindLong(2, feed_id);
//        return feed_update.executeUpdateDelete();
//    }
//
//    public void updateFeedTags(long feed_id, long[] tags)
//    {
//        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//
//        db.execSQL("DELETE FROM " + FEEDTAG_TABLE_NAME + " WHERE feed_id=?;", new String[] {String.valueOf(feed_id)});
//
//        SQLiteStatement feedtag_insert = db.compileStatement(FEEDTAG_TABLE_INSERT);
//        for (long tag_id : tags) {
//            feedtag_insert.clearBindings();
//            feedtag_insert.bindLong(1, feed_id);
//            feedtag_insert.bindLong(2, tag_id);
//            feedtag_insert.executeInsert();
//        }
//    }
//
//

}
