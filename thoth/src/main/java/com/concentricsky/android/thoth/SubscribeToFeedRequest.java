package com.concentricsky.android.thoth;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.concentricsky.android.thoth.com.concentricsky.android.thoth.models.Feed;

import java.io.UnsupportedEncodingException;

/**
* Created by wiggins on 5/18/13.
*/
class SubscribeToFeedRequest extends Request<Feed> {
    private Response.Listener<Feed> mListener;
    private String mUrl;
    private Response.ErrorListener mErrorListener;

    public SubscribeToFeedRequest(String url, Response.Listener<Feed> listener, Response.ErrorListener errorListener)
    {
        super(Method.GET, url, errorListener);
        mListener = listener;
        mErrorListener = errorListener;
        mUrl = url;
    }

    @Override
    protected Response<Feed> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        Feed feed = null;

        if (response.headers.containsKey("Content-Type") && response.headers.get("Content-Type").startsWith("text/html")) {
            String feed_url = FeedHelper.scanHtmlForFeedUrl(mUrl, parsed);
            if (feed_url != null) {
                feed = new Feed();
                feed.url = feed_url;
                feed.title = null; // indicate this still needs to be scraped
                return Response.success(feed, HttpHeaderParser.parseCacheHeaders(response));
            }
        }
        else {
            feed = FeedHelper.attemptToParseFeed(parsed);
            feed.url = mUrl;
            if (feed != null) {
                return Response.success(feed, HttpHeaderParser.parseCacheHeaders(response));
            }
        }

        return Response.error(new ParseError());
    }

    @Override
    protected void deliverResponse(Feed response) {
        mListener.onResponse(response);
    }

}
