package com.concentricsky.android.pensive;

import android.accounts.Account;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.concentricsky.android.pensive.models.Feed;

import java.util.Iterator;
import java.util.Vector;

/**
 * Created by wiggins on 7/26/13.
 */
public class ThothSyncAdapter extends AbstractThreadedSyncAdapter
{
    private final RequestQueue mRequestQueue;

    public ThothSyncAdapter(Context context, boolean autoInitialize) {
        this(context, autoInitialize, false);
    }
    public ThothSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mRequestQueue = Volley.newRequestQueue(context);
    }


    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {

        Vector<UpdateFeedRequest> requests = new Vector<UpdateFeedRequest>();

        Cursor cursor = null;
        cursor = ThothDatabaseHelper.getInstance().getAllFeedsCursor();
        if (cursor != null && cursor.moveToFirst()) {
            int id_idx = cursor.getColumnIndexOrThrow("_id");
            for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                Feed feed = new Feed();
                feed.hydrate( cursor );
                UpdateFeedRequest request = UpdateFeedRequest.queue_if_needed(mRequestQueue, feed, null, null);
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
                }
            }

            synchronized (this) {
                try { this.wait(500); } catch (InterruptedException e) { }
            }
        }


    }
}
