package com.concentricsky.android.thoth;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
public class SubscribeFragment extends Fragment
                               implements   ThothFragmentInterface,
                                            Response.Listener<Feed>,
                                            Response.ErrorListener

{
    private static final String TAG = "ThothSubscribeFragment";
    private ThothDatabaseHelper mDbHelper;
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
    private String mUrl;

    public SubscribeFragment() {

        mDbHelper = ThothDatabaseHelper.getInstance();
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
        mRequestQueue = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mLinkText.setText(mUrl == null ? "" : mUrl);
        mFeedTags.setText("");
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
        Cursor cursor = mDbHelper.getTagCursor();
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                                            android.R.layout.simple_list_item_1,
                                            cursor, new String[] {"title"}, new int[] {android.R.id.text1}, 0);
        adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                return cursor.getString(cursor.getColumnIndexOrThrow("title"));
            }
        });
        mFeedTags.setAdapter(adapter);


        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setUrl(mLinkText.getText().toString());
            }

        });

        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.d(TAG, "create new feed with tags: "+tags);

                mFeed.tags = mFeedTags.getText().toString().split(",");
//                for (String tag_name : tags) {
//                    Tag tag = Tag.getOrCreate(mDbHelper.getReadableDatabase(), tag_name.trim());
//                }

                // TODO: move this off main thread
                mFeed.save(mDbHelper.getWritableDatabase());
                popBackStack();
            }
        });

        scan_url(); // run scan if already have url
        return root;
    }

    private void popBackStack()
    {
        ThothMainActivity activity = (ThothMainActivity)getActivity();
        activity.reloadTags();
        activity.getFragmentManager().popBackStack("Subscribe", FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
            Log.d(TAG, "Feed URL found: " + response.url);
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


    public void setUrl(String url) {
        mUrl = url;
        scan_url();
    }

    private void scan_url() {
        if (mUrl == null || mRequestQueue == null || mLinkText == null) { // we havent attached yet...
            return;
        }
        mLinkText.setText(mUrl);
        mRequestQueue.add(new SubscribeToFeedRequest(mUrl, SubscribeFragment.this, SubscribeFragment.this));
        mProgress.setVisibility(View.VISIBLE);
        mDetailView.setVisibility(View.INVISIBLE);
    }
}
