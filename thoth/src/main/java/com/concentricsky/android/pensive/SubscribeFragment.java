package com.concentricsky.android.pensive;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.android.volley.*;
import com.android.volley.toolbox.Volley;
import com.concentricsky.android.pensive.models.Feed;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by wiggins on 5/17/13.
 */
public class SubscribeFragment extends Fragment
                               implements   Response.Listener<Feed>,
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
    private ViewGroup mSaveProgress;
    private ViewGroup mContent;

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
        mContent = (ViewGroup)mResultsView.findViewById(android.R.id.content);
        mSaveProgress = (ViewGroup)mResultsView.findViewById(R.id.save_progress);




        mResultsList = (ListView)mResultsView.findViewById(android.R.id.list);
        mResultsAdapter = new FeedResultAdapter(getActivity());
        mResultsList.setAdapter(mResultsAdapter);

        mFeedTags = (AutoCompleteTextView)mResultsView.findViewById(R.id.feed_tags);

        mConfirmButton = (Button)mResultsView.findViewById(android.R.id.button1);
        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Feed feed = (Feed) mResultsAdapter.getItem(0);
                feed.tags = mFeedTags.getText().toString().split(",");
                if (feed.tags == null || feed.tags.length < 0 || feed.tags[0] == null || feed.tags[0].isEmpty()) {
                    feed.tags = new String [] {getString(R.string.unfiled)};
                }

                save_feeds();
            }
        });

        return mViewSwitcher;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.subscribe);
    }

    private void save_feeds() {
        final String[] tags = mFeedTags.getText().toString().split(",");
        mTask = new AsyncTask<Void, Integer, Void>() {
            public ProgressBar _progress;


            @Override
            protected void onPreExecute() {
                mSaveProgress.setVisibility(View.VISIBLE);
                mContent.setVisibility(View.GONE);

                _progress = (ProgressBar)mSaveProgress.findViewById(android.R.id.secondaryProgress);
                ArrayList<Feed> feeds = mResultsAdapter.getItems();
                _progress.setMax(feeds.size());


            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                _progress.setProgress(values[values.length-1]);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                ThothMainActivity activity = (ThothMainActivity)getActivity();
                activity.reloadTags();
                getFragmentManager().popBackStack();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                Feed[] feeds = mResultsAdapter.getCheckedItems();
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                if (feeds != null) {
                    int i=0;
                    for (Feed feed : feeds) {
                        if (tags == null || tags.length < 0 || tags[0] == null || tags[0].isEmpty()) {
                            feed.tags = new String [] {getString(R.string.unfiled)};
                        } else {
                            feed.tags = tags;
                        }
                        feed.save(db);
                        publishProgress(++i);
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

    @Override
    public void onPause() {
        super.onPause();
        mInputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
    }

    private void search_url() {
        setUrl(mLinkText.getText().toString());
        mInputManager.hideSoftInputFromWindow(mLinkText.getWindowToken(), 0);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    public void setUrl(String url) {
        mUrl = url;
        if (mUrl != null && !mUrl.startsWith("http")) {
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
        mError.setText("");
    }

    @Override
    public void onResponse(Feed response) {
        if (mRequestQueue == null) {
            return;
        }

        if (response.url == null) { // only found feed urls, re-submit each for scraping
            for (String url : response.tags) {
                Feed f = new Feed();
                f.url = url;
                mRequestQueue.add(new SubscribeToFeedRequest(url, this, this));
            }
        }
        else {
            mViewSwitcher.setDisplayedChild(1);
            mInputManager.hideSoftInputFromWindow(mLinkText.getWindowToken(), 0);
            mResultsAdapter.addResult(response);
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        String errmsg = error.toString();
        errmsg = errmsg.replace("java.net.UnknownHostException: ","");
        errmsg = errmsg.replace("com.android.volley.NoConnectionError: ","");
        errmsg = errmsg.replace("java.net.UnknownHostException: ","");
        errmsg = errmsg.replace("com.android.volley.ParseError","").trim();
        if (errmsg.isEmpty()) {
            mError.setText(R.string.subscribe_parse_error);
        }
        else {
            mError.setText(errmsg);
        }
        mProgress.setVisibility(View.INVISIBLE);
        mError.setVisibility(View.VISIBLE);
    }



    private static class FeedResultAdapter extends BaseAdapter
    {
        private final LayoutInflater mInflater;
        private ArrayList<Feed> mResults;
        private HashSet<Integer> mCheckedItems;

        public FeedResultAdapter(Context context)
        {
            mInflater = LayoutInflater.from(context);
            mCheckedItems = new HashSet<Integer>();
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
            final int position = i;


            CheckBox cb = (CheckBox)root.findViewById(android.R.id.checkbox);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        root.setBackgroundResource(R.color.unread_background);
                        mCheckedItems.add(position);
                    } else {
                        root.setBackgroundResource(R.color.read_background);
                        mCheckedItems.remove(position);
                    }
                }
            });
            cb.setChecked(mCheckedItems.contains(position));

            tv = (TextView)root.findViewById(R.id.feed_title);
            tv.setText(feed.title);

            tv = (TextView)root.findViewById(R.id.feed_description);
            tv.setText(feed.description);

            tv = (TextView)root.findViewById(R.id.feed_link);
            tv.setText(feed.url);

            return root;
        }

        public void changeResults(ArrayList<Feed> results) {
            mResults = results;
            notifyDataSetChanged();
        }

        public ArrayList<Feed> getItems() {
            return mResults;
        }

        public void addResult(Feed response) {
            if (mResults == null) {
                mResults = new ArrayList<Feed>();
                mCheckedItems.add(0);
            }
            mResults.add(response);
            notifyDataSetChanged();
        }

        public Feed[] getCheckedItems() {
            Feed[] out = new Feed[mCheckedItems.size()];
            int i,o=0;
            for (i=0; i < mResults.size(); i++) {
                if (mCheckedItems.contains(i)) {
                    out[o++] = mResults.get(i);
                }
            }
            return out;
        }
    }
}
