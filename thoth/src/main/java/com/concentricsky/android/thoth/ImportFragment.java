package com.concentricsky.android.thoth;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.concentricsky.android.thoth.models.Feed;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by wiggins on 6/6/13.
 */
public class ImportFragment extends ListFragment
                            implements   ThothFragmentInterface
{
    private Uri mUri;
    private RequestQueue mRequestQueue;
    private ThothDatabaseHelper mDbHelper;
    private ContentResolver mContentResolver;
    private ScanZipfileTask mScanningTask;
    private FeedResultAdapter mAdapter;


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

        ListView list = (ListView) root.findViewById(android.R.id.list);
//        mAdapter = new ArrayAdapter<Feed>(getActivity(), android.R.layout.simple_list_item_2, android.R.id.text1);
        mAdapter = new FeedResultAdapter(getActivity());
        list.setAdapter(mAdapter);

//        return super.onCreateView(inflater, container, savedInstanceState);
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
            switch (item.mStatus) {
                case STATUS_SEARCHING:
                    text2.setText("Searching...");
                    break;
                case STATUS_FOUND:
                    if (item.mFeed.tags != null)
                        text2.setText("Added to: "+TextUtils.join(",", item.mFeed.tags)+".");
                    else
                        text2.setText("Added.");
                    break;
                case STATUS_ERROR:
                    text2.setText("Error parsing feed.");
                    break;
            }
            return view;
        }
    }

    private class ScanZipfileTask extends AsyncTask<Uri, AddFeedHelper, Void> {
        @Override
        protected void onProgressUpdate(AddFeedHelper... helpers) {
//            super.onProgressUpdate(values);
            for (AddFeedHelper helper : helpers) {
                mAdapter.add(helper);
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            popBackStack();
        }

        @Override
        protected Void doInBackground(Uri... uris) {
            try {
                InputStream zip = mContentResolver.openInputStream(mUri);

                ArrayList<Feed> feeds = mDbHelper.importTakeoutZip(zip);

                if (feeds != null) {
                    for (Feed feed : feeds) {
                        AddFeedHelper helper = new AddFeedHelper(feed);
                        publishProgress(helper);
                    }
                }
            } catch (IOException e) {
                popBackStack();
//                e.printStackTrace();
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
        activity.getFragmentManager().popBackStack("Import", FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {

    }


    private enum AddHelperStatus { STATUS_SEARCHING, STATUS_FOUND, STATUS_ERROR };
    private class AddFeedHelper implements Response.Listener<Feed>, Response.ErrorListener {
        private Feed mFeed;
        private AddHelperStatus mStatus;

        public AddFeedHelper(Feed f) {
            mFeed = f;
            mStatus = AddHelperStatus.STATUS_SEARCHING;
            mRequestQueue.add(new SubscribeToFeedRequest(mFeed.url, this, this));
        }

        @Override
        public void onResponse(Feed response) {
            if (mRequestQueue == null)
                return;

            if (response.title == null) {//
                mRequestQueue.add(new SubscribeToFeedRequest(response.url, this, this));
            }
            else {
                response.tags = mFeed.tags;
                response.asyncSave(mDbHelper.getWritableDatabase());
                mStatus = AddHelperStatus.STATUS_FOUND;
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            if (mRequestQueue == null)
                return;

            mStatus = AddHelperStatus.STATUS_ERROR;
            mAdapter.notifyDataSetChanged();

        }
    }
}
