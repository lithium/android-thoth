package com.concentricsky.android.thoth;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.concentricsky.android.thoth.models.Feed;

import java.util.Iterator;
import java.util.Vector;

/**
 * Created by wiggins on 7/11/13.
 */
public class RefreshFeedIntentService extends IntentService {
    public static final String ALL_FEEDS_SYNCED = "com.concentricsky.android.intent.action.ALL_FEEDS_SYNCED";
    public static final String FEED_SYNCED = "com.concentricsky.android.intent.action.FEED_SYNCED";

    public RefreshFeedIntentService() {
        super("PensiveSyncIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long feed_id = intent.getLongExtra("feed_id", -1);
        long tag_id = intent.getLongExtra("tag_id", -1);
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        Vector<UpdateFeedRequest> requests = new Vector<UpdateFeedRequest>();

        Cursor cursor = null;
        if (feed_id == 0) {
            cursor = ThothDatabaseHelper.getInstance().getAllFeedsCursor();
        } else {
            cursor = ThothDatabaseHelper.getInstance().getFeedCursor(tag_id);
        }
        if (cursor != null && cursor.moveToFirst()) {
            int id_idx = cursor.getColumnIndexOrThrow("_id");
            for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                Feed feed = new Feed();
                feed.hydrate( cursor );
                UpdateFeedRequest request = UpdateFeedRequest.queue_if_needed(requestQueue, feed, null, null);
                if (request != null)
                    requests.add(request);
            }
        }

        // wait for all the volley requests to complete
        int completed = 0;
        int max = requests.size();
        while (requests.size() > 0) {
            Iterator<UpdateFeedRequest> it = requests.iterator();
            while (it.hasNext()) {
                UpdateFeedRequest request = it.next();
                if (request.hasHadResponseDelivered()) {
                    it.remove();

                    Intent broadcast = new Intent();
                    broadcast.setAction(FEED_SYNCED);
                    broadcast.addCategory(Intent.CATEGORY_DEFAULT);
                    int progress = (int)((++completed/(float)max)*100);
                    broadcast.putExtra("progress", progress);
                    sendBroadcast(broadcast);
                }
            }

            synchronized (this) {
                try { this.wait(500); } catch (InterruptedException e) { }
            }
        }


        Intent broadcast = new Intent();
        broadcast.setAction(ALL_FEEDS_SYNCED);
        broadcast.addCategory(Intent.CATEGORY_DEFAULT);
        broadcast.putExtra("feed_id", feed_id);
        broadcast.putExtra("tag_id", tag_id);
        sendBroadcast(broadcast);
        return;
    }
}
