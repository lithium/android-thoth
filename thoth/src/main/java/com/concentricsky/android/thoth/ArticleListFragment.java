package com.concentricsky.android.thoth;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.codeslap.gist.SimpleCursorLoader;
import com.concentricsky.android.thoth.com.concentricsky.android.thoth.models.Feed;

/**
 * Created by wiggins on 5/17/13.
 */
public class ArticleListFragment extends ListFragment
                                 implements ThothFragmentInterface, Response.Listener<Boolean>, Response.ErrorListener {
    private static final String TAG = "ArticleListFragment";
    private LoaderManager mLoaderManager;

    private SimpleCursorAdapter mAdapter;
    private long mFeedId;
    private Feed mFeed;
    private TextView mFeedTitle;

    private static final int FEED_LOADER_ID=-2;
    private static final int ARTICLE_LOADER_ID=-3;
    private RequestQueue mRequestQueue;

    public ArticleListFragment() {
    }


    public void setFeed(long feed_id) {
        if (feed_id == mFeedId) {
            return;
        }

        mFeedId = feed_id;
        mFeed = null;
        if (mLoaderManager != null) {
            mLoaderManager.destroyLoader(ARTICLE_LOADER_ID);
            mLoaderManager.destroyLoader(FEED_LOADER_ID);
        }
        load_feed();
    }
    private void load_feed()
    {
        if (mLoaderManager == null) {
            return;
        }

        mLoaderManager.initLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
        mLoaderManager.initLoader(FEED_LOADER_ID, null, new FeedLoader());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
//        super.onListItemClick(l, v, position, id);
        ThothMainActivity activity = (ThothMainActivity) getActivity();
        activity.showArticle(mFeedId, id);
    }

    private void update_feed()
    {
        if (mFeed == null) {
            return;
        }
        mFeedTitle.setText(mFeed.title);
        mRequestQueue.add(new UpdateFeedRequest(mFeed, this, this));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mRequestQueue = Volley.newRequestQueue(activity);
        mLoaderManager = activity.getLoaderManager();
        if (mAdapter == null) {
            mAdapter = new SimpleCursorAdapter(activity, android.R.layout.simple_list_item_1, null,
                    new String[] {"title"},
                    new int[] {android.R.id.text1}, 0);
            setListAdapter(mAdapter);
        }
        load_feed();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoaderManager.destroyLoader(FEED_LOADER_ID);
        mRequestQueue.stop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_articlelist, container, false);

        mFeedTitle = (TextView)root.findViewById(R.id.articlelist_feed_title);
        return root;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {
        menu.findItem(R.id.action_subscribe).setVisible(!drawer_open);
        menu.findItem(R.id.action_refresh).setVisible(!drawer_open);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
       switch (item.getItemId()) {
           case R.id.action_subscribe:
               ThothMainActivity act = (ThothMainActivity)getActivity();
               act.showSubscribe(null);
               return true;
           case R.id.action_refresh:
               if (mFeed != null) {
                   mRequestQueue.add(new UpdateFeedRequest(mFeed, this, this));
               }
               return true;
       }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onResponse(Boolean response) {
        mLoaderManager.restartLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(TAG, "Volley error: " + error);
    }


    private class FeedLoader implements LoaderManager.LoaderCallbacks<Cursor>
    {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new SimpleCursorLoader(getActivity()) {
                @Override
                public Cursor loadInBackground() {
                    return Feed.load(ThothDatabaseHelper.getInstance().getReadableDatabase(), mFeedId);
                }
            };
        }
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor.moveToFirst()) {
                mFeed = new Feed();
                mFeed.hydrate(cursor);
                update_feed();
            }
        }
        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    }

    private class ArticleCursorLoader implements LoaderManager.LoaderCallbacks<Cursor>
    {

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new SimpleCursorLoader(getActivity()) {
                @Override
                public Cursor loadInBackground() {
                    return ThothDatabaseHelper.getInstance().getArticleCursor(mFeedId);
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            mAdapter.changeCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
            mAdapter.changeCursor(null);
        }
    }

}
