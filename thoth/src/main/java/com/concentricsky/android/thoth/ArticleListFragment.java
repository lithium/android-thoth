package com.concentricsky.android.thoth;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.*;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.codeslap.gist.SimpleCursorLoader;
import com.concentricsky.android.thoth.models.Feed;
import com.concentricsky.android.thoth.models.Tag;

import java.util.Iterator;
import java.util.Vector;

/**
 * Created by wiggins on 5/17/13.
 */
public class ArticleListFragment extends ListFragment
                                 implements ThothFragmentInterface, Response.Listener<Boolean>, Response.ErrorListener {
    private static final String TAG = "ArticleListFragment";
    private LoaderManager mLoaderManager;

    private ArticleListAdapter mAdapter;
    private long mFeedId;
    private long mTagId;

    private static final int FEED_LOADER_ID=-2;
    private static final int ARTICLE_LOADER_ID=-3;
    private static final int TAG_LOADER_ID=-4;
    private RequestQueue mRequestQueue;
    private MenuItem mRefreshMenuItem;
    private boolean mRefreshing=false;
    private TextView mNoFeedsText;
    private ProgressBar mProgress;
    private boolean mNoFeeds=false;
    private ListView mList;
    private RefreshFeedsTask mRefreshTask;
    private boolean mHideUnread = false;
    private MenuItem mToggleMenuItem;
    private boolean mPaused = false;
    private SharedPreferences mPreferences;
    private int mScrollPosition = -1;
    private Handler mResumeHandler;

    public ArticleListFragment() {
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ThothMainActivity activity = (ThothMainActivity) getActivity();
        Cursor cursor = mAdapter.getCursor();
        activity.showArticle(cursor, position);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentActivity activity = getActivity();

        mAdapter = new ArticleListAdapter(activity, null);
        mRequestQueue = Volley.newRequestQueue(activity);
        mLoaderManager = activity.getSupportLoaderManager();
        load_feed();

        mPreferences = activity.getSharedPreferences("preferences", 0);
        mHideUnread = mPreferences.getBoolean("hideUnread", false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_articlelist, container, false);
        mNoFeedsText = (TextView) root.findViewById(R.id.no_feeds);
        mNoFeedsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThothMainActivity activity = (ThothMainActivity) getActivity();
                activity.showSubscribe(null);
            }
        });
        mNoFeedsText.setVisibility(mNoFeeds ? View.VISIBLE : View.GONE);
        mProgress = (ProgressBar)root.findViewById(android.R.id.progress);
        Loader<Object> loader = mLoaderManager.getLoader(ARTICLE_LOADER_ID);
        mProgress.setVisibility(loader.isStarted() ? View.GONE : View.VISIBLE);

        mList = (ListView)root.findViewById(android.R.id.list);


        setListAdapter(mAdapter);
//        load_feed();

        mHideUnread = mPreferences.getBoolean("hideUnread", false);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
        if (mPaused) {
            mPaused = false;
            mLoaderManager.restartLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mPaused = true;
    }

    @Override
    public void onDestroyView() {
        mLoaderManager.destroyLoader(FEED_LOADER_ID);
        mRequestQueue.stop();
        if (mRefreshTask != null)
            mRefreshTask.cancel(true);
        super.onDestroyView();
    }




    public void setTag(long tag_id) {
        mTagId = tag_id;
        mFeedId = -1;
        load_feed();
    }

    public void setFeed(long feed_id) {

        if (feed_id == mFeedId) {
            return;
        }

        mTagId = -1;
        mFeedId = feed_id;
        load_feed();
    }
    private void load_feed()
    {
        if (mLoaderManager == null) {
            return;
        } else {
            mLoaderManager.destroyLoader(ARTICLE_LOADER_ID);
            mLoaderManager.destroyLoader(FEED_LOADER_ID);
        }

        mLoaderManager.initLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
        if (mTagId > 0) {
            refresh_task_execute();
            mLoaderManager.restartLoader(TAG_LOADER_ID, null, new TagLoader(mTagId));
        }
        else {
            if (mFeedId < 1) {
                getActivity().getActionBar().setTitle(R.string.all_feeds);
            }
            mLoaderManager.initLoader(FEED_LOADER_ID, null, new FeedLoader(mFeedId));
        }
    }

    private void refresh_task_execute()
    {
        if (mRefreshTask != null)
            return;

        mRefreshTask = new RefreshFeedsTask();
        mRefreshTask.execute(mTagId);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {
        menu.findItem(R.id.action_subscribe).setVisible(!drawer_open);
        mRefreshMenuItem = menu.findItem(R.id.action_refresh);
        mRefreshMenuItem.setVisible(mRefreshing ? false : !drawer_open);

        mToggleMenuItem = menu.findItem(R.id.action_toggle_unread);
        mToggleMenuItem.setVisible(true);
        mToggleMenuItem.setTitle(mHideUnread ? R.string.action_show_unread : R.string.action_hide_unread);

        menu.findItem(R.id.action_mark_as_read).setVisible(true);
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
               refresh_feeds();
               return true;
           case R.id.action_toggle_unread:
               setHideUnread(!mHideUnread);
               return true;
           case R.id.action_mark_as_read:
               return true;
       }
        return super.onOptionsItemSelected(item);
    }

    private void setHideUnread(boolean hide_unread)
    {
        mHideUnread = hide_unread;
        mToggleMenuItem.setTitle(mHideUnread ? R.string.action_show_unread : R.string.action_hide_unread);
        mLoaderManager.restartLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
        mPreferences.edit().putBoolean("hideUnread", mHideUnread).commit();
    }



    private void refresh_feeds() {
        mRefreshing = true;
        Activity activity = getActivity();
        activity.setProgressBarIndeterminateVisibility(true);
        mRefreshMenuItem.setVisible(false);

        if (mFeedId > 0) {
            mLoaderManager.restartLoader(FEED_LOADER_ID, null, new FeedLoader(mFeedId));
        }
        else
        {
            refresh_task_execute();
        }
    }


    @Override
    public void onResponse(Boolean response) {
        mRefreshing = false;
        mRefreshMenuItem.setVisible(true);
        getActivity().setProgressBarIndeterminateVisibility(false);
        mLoaderManager.restartLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(TAG, "Volley error: " + error);
    }

    public void setNoFeeds(boolean has_none) {
        mNoFeeds = has_none;
        if (mNoFeedsText != null) {
            mNoFeedsText.setVisibility(has_none ? View.VISIBLE : View.GONE);
        }
    }

    public void scrollToPosition(int position) {
        mScrollPosition = position;
        if (mList != null && mScrollPosition != -1) {
            mList.setSelectionFromTop(position, 0);
        }
    }

    public int getScrollPosition() {
        if (mList != null) {
            return mList.getFirstVisiblePosition();
        }
        return -1;
    }

    public void resumeArticleDetail(Handler handler) {
        mResumeHandler = handler;
    }

    public Cursor getCursor() {
        return mAdapter.getCursor();
    }

    private class RefreshFeedsTask extends AsyncTask<Long, Void, Void>
    {
        @Override
        protected void onPostExecute(Void aVoid) {
            mRefreshing = false;
            if (mRefreshMenuItem != null)
                mRefreshMenuItem.setVisible(true);
            getActivity().setProgressBarIndeterminateVisibility(false);
            mRefreshTask = null;
        }

        @Override
        protected void onPreExecute() {
            mRefreshing = true;
            if (mRefreshMenuItem != null)
                mRefreshMenuItem.setVisible(false);
            getActivity().setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            mAdapter.notifyDataSetChanged();
        }

        @Override
        protected Void doInBackground(Long... tag_ids) {
            Vector<UpdateFeedRequest> requests = new Vector<UpdateFeedRequest>();

            for (long tag_id : tag_ids) {
                Cursor cursor = null;
                if (tag_id == 0) {
                    cursor = ThothDatabaseHelper.getInstance().getAllFeedsCursor();
                } else {
                    cursor = ThothDatabaseHelper.getInstance().getFeedCursor(tag_id);
                }
                if (cursor != null && cursor.moveToFirst()) {
                    int id_idx = cursor.getColumnIndexOrThrow("_id");
                    for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                        Feed feed = new Feed();
                        feed.hydrate( cursor );
                        UpdateFeedRequest request = UpdateFeedRequest.queue_if_needed(mRequestQueue, feed, null, null);
                        if (request != null)
                            requests.add(request);
                    }
                }
            }

            int completed = 0;
            while (requests.size() > 0) {
                Iterator<UpdateFeedRequest> it = requests.iterator();
                while (it.hasNext()) {
                    UpdateFeedRequest request = it.next();
                    if (request.hasHadResponseDelivered()) {
                        it.remove();
                        publishProgress();
                    }
                }

                synchronized (this) {
                    try { this.wait(500); } catch (InterruptedException e) { }
                }
            }

            return null;
        }

    }


    private class FeedLoader implements LoaderManager.LoaderCallbacks<Cursor>
    {
        private long _feed_id;

        public FeedLoader(long feed_id)
        {
            _feed_id = feed_id;
        }
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new SimpleCursorLoader(getActivity()) {
                @Override
                public Cursor loadInBackground() {
                    return Feed.load(ThothDatabaseHelper.getInstance().getReadableDatabase(), _feed_id);
                }
            };
        }
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor.moveToFirst()) {
                Feed feed = new Feed();
                feed.hydrate(cursor);

                getActivity().getActionBar().setTitle( feed.title );
                UpdateFeedRequest.queue_if_needed(mRequestQueue, feed, ArticleListFragment.this, ArticleListFragment.this);

//                refresh_feeds();
            }
        }
        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    }

    private class TagLoader implements LoaderManager.LoaderCallbacks<Cursor>
    {
        private long _tag_id;

        public TagLoader(long tag_id) {
            _tag_id = tag_id;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new SimpleCursorLoader(getActivity()) {
                @Override
                public Cursor loadInBackground() {
                    return Tag.load(ThothDatabaseHelper.getInstance().getReadableDatabase(), _tag_id);
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            if (cursor.moveToFirst()) {
                Tag tag = new Tag();
                tag.hydrate(cursor);
                getActivity().getActionBar().setTitle(tag.title.isEmpty() ? getString(R.string.unfiled) : tag.title);
            }

        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {

        }
    }



    private class ArticleCursorLoader implements LoaderManager.LoaderCallbacks<Cursor>
    {

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            if (mProgress != null)
                mProgress.setVisibility(View.VISIBLE);
            return new SimpleCursorLoader(getActivity()) {
                @Override
                public Cursor loadInBackground() {
                    if (mFeedId != -1)
                        return ThothDatabaseHelper.getInstance().getArticleCursor(mFeedId, mHideUnread);
                    if (mTagId != -1)
                        return ThothDatabaseHelper.getInstance().getArticleCursorByTag(mTagId, mHideUnread);
                    return null;
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            mAdapter.changeCursor(cursor);
            if (mProgress != null)
                mProgress.setVisibility(View.GONE);
            if (mScrollPosition != -1)
                mList.setSelectionFromTop(mScrollPosition, 0);


            if (mResumeHandler != null) {
                mResumeHandler.handleMessage(null);
                mResumeHandler = null;
            }


        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
            mAdapter.changeCursor(null);
        }
    }

    private class ArticleListAdapter extends CursorAdapter {
        private final LayoutInflater mInflater;
        private int mArticleTitleIdx=-1;
        private int mFeedTitleIdx=-1;
        private int mTimestampIdx=-1;
        private int mUnreadIdx=-1;


        private final int[] UNREAD_STATES = {android.R.attr.state_checked};
        private final int[] READ_STATES = {};

        private class ViewHolder {
            TextView title;
            TextView feed;
            TextView date;

        };

        public ArticleListAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            View root = mInflater.inflate(R.layout.item_articlelist, viewGroup, false);
            final ViewHolder holder = new ViewHolder();
            holder.title = (TextView)root.findViewById(R.id.title);
            holder.feed = (TextView)root.findViewById(R.id.feed_title);
            holder.date = (TextView)root.findViewById(R.id.timestamp);
            root.setTag(holder);
            return root;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder)view.getTag();

            if (mArticleTitleIdx != -1)
                holder.title.setText( cursor.getString(mArticleTitleIdx) );
            if (mFeedTitleIdx != -1)
                holder.feed.setText( cursor.getString(mFeedTitleIdx) );
            if (mTimestampIdx != -1)
                holder.date.setText(DateUtils.fuzzyTimestamp(context, cursor.getLong(mTimestampIdx)));

            boolean unread = cursor.getInt(mUnreadIdx) == 1 ? true : false;
            holder.title.setTextAppearance(context, unread ? R.style.TextAppearance_article_unread : R.style.TextAppearance_article_read);

            view.setBackgroundResource(unread ? R.color.unread_background : R.color.read_background);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            if (cursor != null) {
                mArticleTitleIdx = cursor.getColumnIndex("title");
                mFeedTitleIdx = cursor.getColumnIndex("feed_title");
                mTimestampIdx = cursor.getColumnIndex("timestamp");
                mUnreadIdx = cursor.getColumnIndex("unread");
            }
        }
    }
}
