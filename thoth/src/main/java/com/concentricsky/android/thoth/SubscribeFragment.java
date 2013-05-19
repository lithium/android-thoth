package com.concentricsky.android.thoth;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import com.android.volley.*;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.concentricsky.android.thoth.com.concentricsky.android.thoth.models.Feed;

import java.io.UnsupportedEncodingException;

/**
 * Created by wiggins on 5/17/13.
 */
public class SubscribeFragment extends Fragment implements ThothFragmentInterface {
    private static final String TAG = "ThothSubscribeFragment";
    private RequestQueue mRequestQueue;
    private EditText mLinkText;
    private Button mSubmitButton;

    public SubscribeFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mRequestQueue = Volley.newRequestQueue(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mRequestQueue.stop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_subscribe, container, false);

        mLinkText = (EditText)root.findViewById(R.id.subscribe_link);
        mSubmitButton = (Button)root.findViewById(R.id.subscribe_submit);

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String link = mLinkText.getText().toString();
//                ThothDatabaseHelper.getInstance().addFeed(link,title,tags);

                mRequestQueue.add(new SubscribeFeedRequest(link, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "volley error! "+error);
                    }
                }));
            }

        });

        return root;
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {

//        menu.findItem(R.id.action_share).setVisible(!drawer_open);
//        menu.findItem(R.id.action_visitpage).setVisible(!drawer_open);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }


    private class SubscribeFeedRequest extends Request<Feed> {
        private String mLink;
        private Response.ErrorListener mErrorListener;

        public SubscribeFeedRequest(String url, Response.ErrorListener errorListener)
        {
            super(Request.Method.GET, url, errorListener);
            mErrorListener = errorListener;
            mLink = url;
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

            if (response.headers.get("Content-Type").startsWith("text/xml")) {
                feed = FeedHelper.attemptToParseFeed(parsed);
                if (feed != null) {
                    return Response.success(feed, HttpHeaderParser.parseCacheHeaders(response));
                }
            }
            else {
                String feed_url = FeedHelper.scanHtmlForFeedUrl(mLink, parsed);
                if (feed_url != null) {
                    feed = new Feed();
                    feed.link = feed_url;
                    feed.title = null; // indicate this still needs to be scraped
                    return Response.success(feed, HttpHeaderParser.parseCacheHeaders(response));
                }
            }

            return Response.error(new ParseError());
        }

        @Override
        protected void deliverResponse(Feed response) {
            if (response.title == null) {
                mRequestQueue.add(new SubscribeFeedRequest(response.link, mErrorListener));
            }
            else {
                //we found a new feed!
            }

        }

    }

}
