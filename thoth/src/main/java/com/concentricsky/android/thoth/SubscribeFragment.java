package com.concentricsky.android.thoth;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.android.volley.*;
import com.android.volley.toolbox.Volley;
import com.concentricsky.android.thoth.com.concentricsky.android.thoth.models.Feed;
import com.concentricsky.android.thoth.com.concentricsky.android.thoth.models.Tag;

import java.util.ArrayList;

/**
 * Created by wiggins on 5/17/13.
 */
public class SubscribeFragment extends  Fragment
                               implements   ThothFragmentInterface,
                                            Response.Listener<Feed>,
                                            Response.ErrorListener

{
    private static final String TAG = "ThothSubscribeFragment";
    private RequestQueue mRequestQueue;
    private EditText mLinkText;
    private Button mSubmitButton;
    private Button mConfirmButton;
    private View mDetailView;
    private TextView mFeedLink;
    private TextView mFeedDescription;
    private TextView mFeedTitle;
    private AutoCompleteTextView mFeedTags;
    private Feed mFeed;
    private ProgressBar mProgress;
    private TextView mError;

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
        mConfirmButton = (Button)root.findViewById(R.id.subscribe_confirm);
        mDetailView = root.findViewById(R.id.subscribe_feed_detail);

        mProgress = (ProgressBar)root.findViewById(android.R.id.progress);
        mError = (TextView)root.findViewById(R.id.subscribe_error);


        mFeedTitle = (TextView)root.findViewById(R.id.feed_title);
        mFeedDescription = (TextView)root.findViewById(R.id.feed_description);
        mFeedLink = (TextView)root.findViewById(R.id.feed_link);
        mFeedTags = (AutoCompleteTextView)root.findViewById(R.id.feed_tags);


        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = mLinkText.getText().toString();
                mRequestQueue.add(new SubscribeToFeedRequest(url, SubscribeFragment.this, SubscribeFragment.this));
                mProgress.setVisibility(View.VISIBLE);
                mDetailView.setVisibility(View.INVISIBLE);
            }

        });

        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] tags = mFeedTags.getText().toString().split(",");
                Log.d(TAG, "create new feed with tags: "+tags);
            }
        });

        return root;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onResponse(Feed response) {
        if (response.title == null) { // only found a feed url, re-scrape
            mRequestQueue.add(new SubscribeToFeedRequest(response.url, this, this));
        }
        else {
            // feed found
            Log.d(TAG, "Feed found: " + response.title);

            mFeed = response;
            mFeedTitle.setText(response.title);
            mFeedLink.setText(response.link);
            mFeedDescription.setText(response.description);
            mDetailView.setVisibility(View.VISIBLE);
            mProgress.setVisibility(View.GONE);
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.d(TAG, "volley error! "+error);
        mError.setText(error.toString());
        mProgress.setVisibility(View.INVISIBLE);
        mError.setVisibility(View.VISIBLE);
    }


}
