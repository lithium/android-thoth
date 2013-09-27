package com.concentricsky.android.pensive;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.concentricsky.android.pensive.models.Feed;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by wiggins on 6/6/13.
 */
public class ImportFragment extends ListFragment
{
    private Uri mUri;
    private RequestQueue mRequestQueue;
    private ThothDatabaseHelper mDbHelper;
    private ContentResolver mContentResolver;
    private ScanZipfileTask mScanningTask;
    private FeedResultAdapter mAdapter;
    private ProgressBar mProgressBar;


    public ImportFragment() {
        mDbHelper = ThothDatabaseHelper.getInstance();
    }

    public void setZipfileUri(Uri uri) {
        mUri = uri;
        scan_zipfile();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_import, container, false);

        mProgressBar = (ProgressBar) root.findViewById(android.R.id.progress);

        ListView list = (ListView) root.findViewById(android.R.id.list);
        mAdapter = new FeedResultAdapter(getActivity());
        list.setAdapter(mAdapter);

        scan_zipfile();
        return root;
    }

    private void scan_zipfile() {
        if (mUri == null || mRequestQueue == null) {
            return;
        }

        mScanningTask = new ScanZipfileTask();
        mScanningTask.execute(mUri);
    }

    private class FeedResultAdapter extends ArrayAdapter<AddFeedHelper>
    {
        public FeedResultAdapter(Context context)
        {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(android.R.layout.simple_list_item_2, null);
            }
            TextView text1 = (TextView) view.findViewById(android.R.id.text1);
            TextView text2 = (TextView) view.findViewById(android.R.id.text2);
            AddFeedHelper item = getItem(position);
            text1.setText(item.mFeed.title);
            Context context = getContext();
            switch (item.mStatus) {
                case STATUS_DUPLICATE:
                    text2.setText(context.getString(R.string.import_status_duplicate));
                    break;
                case STATUS_SEARCHING:
                    text2.setText(context.getString(R.string.import_status_searching));
                    break;
                case STATUS_FOUND:
                    if (item.mFeed.tags != null)
                        text2.setText(context.getString(R.string.import_status_added_to)+TextUtils.join(",", item.mFeed.tags));
                    else
                        text2.setText(context.getString(R.string.import_status_added));
                    break;
                case STATUS_ERROR:
                    text2.setText(context.getString(R.string.import_status_error));
                    break;
            }
            return view;
        }
    }

    private class ScanZipfileTask extends AsyncTask<Uri, AddFeedHelper, Void> {
        private int mMax=-1;

        @Override
        protected void onProgressUpdate(AddFeedHelper... helpers) {
            if (mMax!=-1) {
                mProgressBar.setMax(mMax);
                mMax=-1;
            }

            if (helpers != null) {
                for (AddFeedHelper helper : helpers) {
                    mAdapter.add(helper);
                    mAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        protected Void doInBackground(Uri... uris) {
            try {
                ArrayList<Feed> feeds = null;


                if (mUri.getScheme().startsWith("http")) {
                    URL url = new URL(mUri.toString());
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    BufferedInputStream stream = new BufferedInputStream(conn.getInputStream());
                    feeds = OpmlParser.parse(new InputStreamReader(stream));
                }
                else {
                    InputStream file = mContentResolver.openInputStream(mUri);
                    feeds = mDbHelper.importTakeoutZip(file);
                    file.close();

                    if (feeds == null) {
                        feeds = OpmlParser.parse(new InputStreamReader(mContentResolver.openInputStream(mUri)));
                    }
                }

                if (feeds != null) {
                    mMax = feeds.size();
                    for (Feed feed : feeds) {
                        AddFeedHelper helper = new AddFeedHelper(feed);
                        publishProgress(helper);
                    }
                }
                else {
                    // no feeds found in import...
                    popBackStack();
                }

            } catch (IOException e) {
                e.printStackTrace();
                popBackStack();
            }

            return null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mRequestQueue.stop();
        mRequestQueue = null;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mRequestQueue = Volley.newRequestQueue(activity);
        mContentResolver = activity.getContentResolver();

    }


    private void popBackStack()
    {
        ThothMainActivity activity = (ThothMainActivity)getActivity();
        activity.reloadTags();
        getFragmentManager().popBackStack();
    }


    private enum AddHelperStatus { STATUS_SEARCHING, STATUS_FOUND, STATUS_DUPLICATE, STATUS_ERROR };
    private class AddFeedHelper implements Response.Listener<Feed>, Response.ErrorListener {
        private Feed mFeed;
        private AddHelperStatus mStatus;

        public AddFeedHelper(Feed f) {
            mFeed = f;
            if (!mDbHelper.doesFeedExist(mFeed.url)) {
                mRequestQueue.add(new SubscribeToFeedRequest(mFeed.url, this, this));
                mStatus = AddHelperStatus.STATUS_SEARCHING;
            }
            else {
                increment_progress();
                mStatus = AddHelperStatus.STATUS_DUPLICATE;
            }
        }

        @Override
        public void onResponse(Feed response) {
            if (mRequestQueue == null)
                return;

            if (response.title == null) {
                mRequestQueue.add(new SubscribeToFeedRequest(response.url, this, this));
            }
            else {
                response.tags = mFeed.tags;
                response.asyncSave(mDbHelper.getWritableDatabase());
                mStatus = AddHelperStatus.STATUS_FOUND;
                mAdapter.notifyDataSetChanged();
                increment_progress();
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            if (mRequestQueue == null)
                return;

            mStatus = AddHelperStatus.STATUS_ERROR;
            mAdapter.notifyDataSetChanged();
            increment_progress();
        }

    }

    private void increment_progress()
    {
        mProgressBar.setProgress(mProgressBar.getProgress()+1);
        if (mProgressBar.getProgress() >= mProgressBar.getMax()) {
            popBackStack();
        }
    }
}
