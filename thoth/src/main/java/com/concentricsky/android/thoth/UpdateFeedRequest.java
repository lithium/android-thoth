package com.concentricsky.android.thoth;

import android.util.Log;
import com.android.volley.*;
import com.android.volley.toolbox.HttpHeaderParser;
import com.concentricsky.android.thoth.models.Feed;

import java.io.UnsupportedEncodingException;

/**
 * Created by wiggins on 5/20/13.
 */

public class UpdateFeedRequest extends Request<Boolean> {


    private static final boolean DEBUG_ALWAYS_QUEUE_FEED_REFRESH = true;
    private final Response.Listener<Boolean> mListener;
    private final Feed mFeed;




    public static UpdateFeedRequest queue_if_needed(RequestQueue queue, Feed feed, Response.Listener<Boolean> listener, Response.ErrorListener errorListener)
    {
        long now = System.currentTimeMillis()/1000;
        if (now - feed.timestamp <= feed.ttl && !DEBUG_ALWAYS_QUEUE_FEED_REFRESH) {
            return null;
        }
        UpdateFeedRequest request = new UpdateFeedRequest(feed, listener, errorListener);
        queue.add(request);
        return request;
    }

    public UpdateFeedRequest(Feed feed, Response.Listener<Boolean> listener, Response.ErrorListener errorListener)
    {
        super(Method.GET, feed.url, errorListener);
        mFeed = feed;
        mListener = listener;
        setRetryPolicy(new DefaultRetryPolicy(3000, 1, 1));
    }


    @Override
    protected Response<Boolean> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
//            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            parsed = new String(response.data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }

        Feed feed = FeedHelper.attemptToParseFeed(mFeed, parsed);
        if (feed != null) {
            feed.save(ThothDatabaseHelper.getInstance().getWritableDatabase());
            return Response.success(true, HttpHeaderParser.parseCacheHeaders(response));
        }
        return Response.success(false, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(Boolean success) {
        if (mListener != null) {
            mListener.onResponse(success);
        }
    }
}
