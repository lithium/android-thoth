package com.concentricsky.android.pensive;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.concentricsky.android.pensive.models.Feed;

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
//            String charset = HttpHeaderParser.parseCharset(response.headers);
            parsed = new String(response.data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        Feed feed = null;

        if (response.headers.containsKey("Content-Type") && response.headers.get("Content-Type").startsWith("text/html")) {
//            String feed_url = FeedHelper.scanHtmlForFeedUrl(mUrl, parsed);
            String[] feed_urls = FeedHelper.scanHtmlForFeedUrl(mUrl, parsed);
            if (feed_urls != null) {
                feed = new Feed();
                feed.tags = feed_urls;
                feed.url = null;
                feed.title = null; // indicate this still needs to be scraped
                return Response.success(feed, HttpHeaderParser.parseCacheHeaders(response));
            }
        }
        else {
            feed = FeedHelper.attemptToParseFeed(null, parsed);
            if (feed != null) {
                feed.url = mUrl;
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
