package com.concentricsky.android.thoth;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.concentricsky.android.thoth.models.Feed;

import java.io.UnsupportedEncodingException;

/**
 * Created by wiggins on 5/20/13.
 */

public class UpdateFeedRequest extends Request<Boolean> {


    private final Response.Listener<Boolean> mListener;
    private final Feed mFeed;

    public UpdateFeedRequest(Feed feed, Response.Listener<Boolean> listener, Response.ErrorListener errorListener)
    {
        super(Method.GET, feed.url, errorListener);
        mFeed = feed;
        mListener = listener;
    }


    @Override
    protected Response<Boolean> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
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
