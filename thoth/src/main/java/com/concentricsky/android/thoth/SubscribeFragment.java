package com.concentricsky.android.thoth;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.android.volley.*;
import com.android.volley.toolbox.Volley;
import com.concentricsky.android.thoth.models.Feed;

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
    private AutoCompleteTextView mFeedTags;
    private ProgressBar mProgress;
    private TextView mError;
    private String mUrl;
    private InputMethodManager mInputManager;
    private ViewSwitcher mViewSwitcher;
    private ViewGroup mEntryView;
    private ViewGroup mResultsView;
    private FeedResultAdapter mResultsAdapter;
    private ListView mResultsList;
    private AsyncTask<Void, Integer, Void> mTask;

    public SubscribeFragment() {

        mDbHelper = ThothDatabaseHelper.getInstance();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mRequestQueue = Volley.newRequestQueue(activity);
        mInputManager = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mViewSwitcher = (ViewSwitcher)inflater.inflate(R.layout.fragment_subscribe, container, false);

        mEntryView = (ViewGroup)inflater.inflate(R.layout.subscribe_url_entry, null, false);
        mResultsView = (ViewGroup)inflater.inflate(R.layout.subscribe_results, null, false);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mViewSwitcher.addView(mEntryView);
        mViewSwitcher.addView(mResultsView);

        mLinkText = (EditText)mEntryView.findViewById(R.id.subscribe_link);
        mLinkText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionid, KeyEvent keyEvent) {
                switch (actionid) {
                    case EditorInfo.IME_ACTION_NEXT:
                    case EditorInfo.IME_ACTION_DONE:
                        search_url();
                        return true;
                }
                return false;
            }
        });
        mLinkText.requestFocus();

        mProgress = (ProgressBar)mEntryView.findViewById(android.R.id.progress);
        mError = (TextView)mEntryView.findViewById(R.id.subscribe_error);
        mSubmitButton = (Button)mEntryView.findViewById(R.id.subscribe_submit);
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                search_url();
            }
        });




        mResultsList = (ListView)mResultsView.findViewById(android.R.id.list);
        mResultsAdapter = new FeedResultAdapter(getActivity());
        mResultsList.setAdapter(mResultsAdapter);

        mFeedTags = (AutoCompleteTextView)mResultsView.findViewById(R.id.feed_tags);
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

        mConfirmButton = (Button)mResultsView.findViewById(android.R.id.button1);
        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Feed feed = (Feed) mResultsAdapter.getItem(0);
                feed.tags = mFeedTags.getText().toString().split(",");

                save_feeds();
            }
        });

        return mViewSwitcher;
    }

    private void save_feeds() {
        mTask = new AsyncTask<Void, Integer, Void>() {
            @Override
            protected void onPostExecute(Void aVoid) {
                getFragmentManager().popBackStack();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                ArrayList<Feed> feeds = mResultsAdapter.getItems();
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                if (feeds != null) {
                    for (Feed feed : feeds) {
                        feed.save(db);
                    }
                }
                return null;
            }
        };
        mTask.execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        mLinkText.setText(mUrl == null ? "" : mUrl);
        mFeedTags.setText("");
        if (mUrl == null)
            mInputManager.showSoftInput(mLinkText, InputMethodManager.SHOW_IMPLICIT);
        scan_url();
        mViewSwitcher.setDisplayedChild(0);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mRequestQueue.stop();
        mRequestQueue = null;
    }


    private void search_url() {
        setUrl(mLinkText.getText().toString());
        mInputManager.hideSoftInputFromWindow(mLinkText.getWindowToken(), 0);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    public void setUrl(String url) {
        mUrl = url;
        if (mUrl != null && !mUrl.startsWith("http://")) {
            mUrl = "http://"+mUrl;
        }
        scan_url();
    }

    private void scan_url() {
        if (mUrl == null || mRequestQueue == null || mLinkText == null) { // we havent attached yet...
            return;
        }
        mLinkText.setText(mUrl);
        mRequestQueue.add(new SubscribeToFeedRequest(mUrl, SubscribeFragment.this, SubscribeFragment.this));
        mProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResponse(Feed response) {
        if (response.title == null) { // only found a feed url, re-scrape
            Log.d(TAG, "Feed URL found: " + response.url);
            if (mRequestQueue != null)
                mRequestQueue.add(new SubscribeToFeedRequest(response.url, this, this));
        }
        else {
            mViewSwitcher.showNext();
            ArrayList<Feed> feeds = new ArrayList<Feed>();
            feeds.add(response);
            mResultsAdapter.changeResults(feeds);
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.d(TAG, "volley error! "+error);
        mError.setText(error.toString());
        mProgress.setVisibility(View.INVISIBLE);
        mError.setVisibility(View.VISIBLE);
    }



    private static class FeedResultAdapter extends BaseAdapter
    {
        private final LayoutInflater mInflater;
        private ArrayList<Feed> mResults;

        public FeedResultAdapter(Context context)
        {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mResults != null ? mResults.size() : 0;
        }

        @Override
        public Object getItem(int i) {
            return mResults.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            final View root = mInflater.inflate(R.layout.item_subscribe_result, viewGroup, false);
            Feed feed = (Feed)getItem(i);
            TextView tv;

            CheckBox cb = (CheckBox)root.findViewById(android.R.id.checkbox);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    root.setBackgroundResource(b ? R.color.unread_background : R.color.read_background);
                }
            });
            cb.setChecked(true);

            tv = (TextView)root.findViewById(R.id.feed_title);
            tv.setText(feed.title);

            tv = (TextView)root.findViewById(R.id.feed_description);
            tv.setText(feed.description);

            tv = (TextView)root.findViewById(R.id.feed_link);
            tv.setText(feed.link);

            return root;
        }

        public void changeResults(ArrayList<Feed> results) {
            mResults = results;
            notifyDataSetChanged();
        }

        public ArrayList<Feed> getItems() {
            return mResults;
        }
    }
}
