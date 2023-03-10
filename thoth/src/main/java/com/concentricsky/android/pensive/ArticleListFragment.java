package com.concentricsky.android.pensive;

import android.app.Activity;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.SparseArray;
import android.view.*;
import android.widget.*;
import com.codeslap.gist.SimpleCursorLoader;
import com.concentricsky.android.pensive.models.Article;
import com.concentricsky.android.pensive.models.Feed;
import com.concentricsky.android.pensive.models.Tag;

/**
 * Created by wiggins on 5/17/13.
 */
public class ArticleListFragment extends ResizableListFragment
{
    private static final String TAG = "ArticleListFragment";
    private LoaderManager mLoaderManager;

    private ArticleListAdapter mAdapter;
    private long mFeedId=-1;
    private long mTagId=-1;

    private static final int FEED_LOADER_ID=-2;
    private static final int ARTICLE_LOADER_ID=-3;
    private static final int TAG_LOADER_ID=-4;
    private MenuItem mRefreshMenuItem;
    private boolean mRefreshing=false;
    private View mNoFeedsView;
    private ProgressBar mProgress;
    private boolean mNoFeeds=false;
    private ListView mList;
    private boolean mHideRead = false;
    private MenuItem mToggleMenuItem;
    private boolean mPaused = false;
    private SharedPreferences mPreferences;
    private int mScrollPosition = 0;
    private int mScrollOffset = 0;
    private Handler mResumeHandler;
    private MenuItem mMarkAsReadMenuItem;
    private View mEmpty;
    private AsyncTask<Void, Integer, Void> mTask;
    private SyncResponseReceiver mSyncResponseReceiver;
    private boolean mShowHighlighted=false;
    private SparseArray<Boolean> mHighlightedCache;

    public ArticleListFragment() {
        setHasOptionsMenu(true);
        mHighlightedCache = new SparseArray<Boolean>();
    }

    public static ArticleListFragment newInstance(long tag_id, long feed_id) {
        ArticleListFragment fragment = new ArticleListFragment();

        Bundle args = new Bundle();
        args.putLong("feed_id", feed_id);
        args.putLong("tag_id", tag_id);
        fragment.setArguments(args);

        return fragment;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentActivity activity = getActivity();

        mAdapter = new ArticleListAdapter(activity, null);
        mLoaderManager = activity.getSupportLoaderManager();

        mPreferences = activity.getSharedPreferences("preferences", 0);
        mHideRead = mPreferences.getBoolean("hideUnread", false);

        try {
            ArticleSelectedListener iface = (ArticleSelectedListener)activity;
            setArticleSelectedListener(iface);
        } catch (ClassCastException e) { }

        Bundle args = getArguments();
        if (args != null) {
            long feed_id = args.getLong("feed_id", -1);
            if (feed_id != -1)
                setFeed(feed_id);
            long tag_id = args.getLong("tag_id", -1);
            if (tag_id != -1)
                setTag(tag_id);
        }

        if (savedInstanceState != null) {
            mScrollPosition = savedInstanceState.getInt("scroll_position",0);
            mScrollOffset = savedInstanceState.getInt("scroll_offset",0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_articlelist, container, false);
        mNoFeedsView = root.findViewById(R.id.no_feeds);
        mNoFeedsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThothMainActivity activity = (ThothMainActivity) getActivity();
                activity.showSubscribe(null);
            }
        });
        mProgress = (ProgressBar)root.findViewById(android.R.id.progress);
        Loader<Object> loader = mLoaderManager.getLoader(ARTICLE_LOADER_ID);
        mProgress.setVisibility(loader == null || loader.isStarted() ? View.GONE : View.VISIBLE);

        mList = (ListView)root.findViewById(android.R.id.list);
        mEmpty = (View)root.findViewById(R.id.empty);


        setListAdapter(mAdapter);

        mHideRead = mPreferences.getBoolean("hideUnread", false);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        load_feed();

        mSyncResponseReceiver = new SyncResponseReceiver();
        IntentFilter filter = new IntentFilter(RefreshFeedIntentService.ALL_FEEDS_SYNCED);
        filter.addAction(RefreshFeedIntentService.FEED_SYNCED);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        getActivity().registerReceiver(mSyncResponseReceiver, filter);

    }



    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mList != null) {
            View v = mList.getChildAt(0);
            outState.putInt("scroll_position", mList.getFirstVisiblePosition());
            outState.putInt("scroll_offset", (v == null) ? 0 : v.getTop());
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
        if (mPaused) {
            mPaused = false;
            mLoaderManager.restartLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
        }
        setNoFeeds(mNoFeeds);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPaused = true;
        if (mTask != null)
            mTask.cancel(true);


        if (mList != null) {
            View v = mList.getChildAt(0);
            mScrollPosition = mList.getFirstVisiblePosition();
            mScrollOffset = (v == null) ? 0 : v.getTop();
        }


        try {
            Activity activity = getActivity();
            activity.unregisterReceiver(mSyncResponseReceiver);
        } catch (IllegalArgumentException e) {
            // catch Receiver not registered
        }
    }

    @Override
    public void onDestroyView() {
        mLoaderManager.destroyLoader(FEED_LOADER_ID);
        super.onDestroyView();
    }




    public void setTag(long tag_id) {
        mTagId = tag_id;
        mFeedId = -1;
        mScrollPosition = 0;
        mScrollOffset = 0;
        load_feed();
    }

    public void setFeed(long feed_id) {

        if (feed_id == mFeedId) {
            return;
        }

        mTagId = -1;
        mFeedId = feed_id;
        mScrollPosition = 0;
        mScrollOffset = 0;
        load_feed();
    }
    public void setTagFeed(long tag_id, long feed_id)
    {
        mTagId = tag_id;
        mFeedId = feed_id;
        mScrollPosition = 0;
        mScrollOffset = 0;
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
            mLoaderManager.restartLoader(TAG_LOADER_ID, null, new TagLoader(mTagId));
        }
        else {
            if (mFeedId < 1) {
                getActivity().getActionBar().setTitle(R.string.all_feeds);
            }
            mLoaderManager.initLoader(FEED_LOADER_ID, null, new FeedLoader(mFeedId));
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean drawer_open = false;

        menu.findItem(R.id.action_subscribe).setVisible(!drawer_open);

        mRefreshMenuItem = menu.findItem(R.id.action_refresh);
        mRefreshMenuItem.setVisible(mRefreshing || mNoFeeds ? false : !drawer_open);

        mToggleMenuItem = menu.findItem(R.id.action_toggle_unread);
        mToggleMenuItem.setVisible(true);
        mToggleMenuItem.setTitle(mHideRead ? R.string.action_show_read : R.string.action_hide_read);

        mMarkAsReadMenuItem = menu.findItem(R.id.action_mark_as_read);
        mMarkAsReadMenuItem.setVisible(mNoFeeds ? false : true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
       switch (item.getItemId()) {
           case R.id.action_refresh:
               refresh_feeds();
               return true;
           case R.id.action_toggle_unread:
               setHideRead(!mHideRead);
               return true;
           case R.id.action_mark_as_read:
               markAllAsRead();
               return true;
       }
        return super.onOptionsItemSelected(item);
    }

    private void markAllAsRead() {
        if (mTask != null)
            return;

        mTask = new AsyncTask<Void, Integer, Void>() {
            @Override
            protected void onPreExecute() {
                mProgress.setVisibility(View.VISIBLE);
                mProgress.setIndeterminate(false);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mProgress.setVisibility(View.GONE);
                mLoaderManager.restartLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
                mTask = null;

                ThothMainActivity activity = (ThothMainActivity)getActivity();
                activity.reloadTags();
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                mProgress.setProgress(values[values.length - 1]);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                Cursor cursor = mAdapter.getCursor();
                if (cursor != null) {
                    int max = cursor.getCount();
                    mProgress.setMax(max);
                    int id_idx = cursor.getColumnIndex("_id");
                    int unread_idx = cursor.getColumnIndex("unread");
                    SQLiteDatabase db = ThothDatabaseHelper.getInstance().getWritableDatabase();
                    int i;
                    for (i=0; i < max; i++) {
                        cursor.moveToPosition(i);
                        long id = cursor.getLong(id_idx);
                        long unread = cursor.getInt(unread_idx);
                        if (unread == 1) {
                            Article.markAsRead(db, id);
                        }
                        publishProgress(i);
                    }
                }
                return null;
            }
        };
        mTask.execute();


    }

    private void setHideRead(boolean hide_read)
    {
        mHideRead = hide_read;
        mToggleMenuItem.setTitle(mHideRead ? R.string.action_show_read : R.string.action_hide_read);
        mLoaderManager.restartLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
        mPreferences.edit().putBoolean("hideUnread", mHideRead).commit();
    }



    private void refresh_feeds() {
        mRefreshing = true;
        Activity activity = getActivity();
        activity.setProgressBarIndeterminateVisibility(true);
        activity.setProgressBarVisibility(true);
        mRefreshMenuItem.setVisible(false);

        Intent intent = new Intent(activity, RefreshFeedIntentService.class);
        intent.putExtra("feed_id", mFeedId);
        intent.putExtra("tag_id", mTagId);
        activity.startService(intent);
    }

    public void setHighlightedArticle(int position, long id) {
        mList.setItemChecked(position, true);
        mList.smoothScrollToPosition(position);

        mHighlightedCache.append(position, true);
    }
    public void setShowHighlighted(boolean show_highlighted) {
        mShowHighlighted = show_highlighted;
    }


    public class SyncResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ThothMainActivity activity = (ThothMainActivity)getActivity();
            if (activity == null) {
                return;
            }
            String action = intent.getAction();

            if (RefreshFeedIntentService.ALL_FEEDS_SYNCED.equals(action)) {
                mRefreshing = false;
                activity.setProgressBarIndeterminateVisibility(false);
                activity.setProgressBarVisibility(false);
                if (mRefreshMenuItem != null)
                    mRefreshMenuItem.setVisible(true);
                if (mLoaderManager != null)
                    mLoaderManager.restartLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
            }
            else
            if (RefreshFeedIntentService.FEED_SYNCED.equals(action)) {
                int progress = intent.getIntExtra("progress",50);
                progress = (int)(10000*((float)progress/100));
                activity.setProgress(progress);
                mLoaderManager.restartLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
            }
            activity.reloadTags();
        }
    }

    public void setNoFeeds(boolean has_none) {
        mNoFeeds = has_none;
        if (mNoFeedsView != null) {
            mNoFeedsView.setVisibility(has_none ? View.VISIBLE : View.GONE);
        }

        if (mMarkAsReadMenuItem != null)
            mMarkAsReadMenuItem.setVisible(mNoFeeds ? false : true);
        if (mRefreshMenuItem != null)
            mRefreshMenuItem.setVisible(mNoFeeds ? false : true);

        if (mEmpty != null) {
            if (mNoFeeds)
                mEmpty.setVisibility(View.GONE);
        }
    }

    public void resumeArticleDetail(Handler handler) {
        mResumeHandler = handler;
    }

    public Cursor getCursor() {
        return mAdapter.getCursor();
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
                        return ThothDatabaseHelper.getInstance().getArticleCursor(mFeedId, mHideRead);
                    if (mTagId != -1)
                        return ThothDatabaseHelper.getInstance().getArticleCursorByTag(mTagId, mHideRead);
                    return null;
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            if (mAdapter != null)
                mAdapter.changeCursor(cursor);

            if (mProgress != null)
                mProgress.setVisibility(View.GONE);

            if (mList != null) {
                mList.setSelectionFromTop(mScrollPosition, mScrollOffset);
            }

            if (mEmpty != null) {
                mEmpty.setVisibility(!mNoFeeds && (cursor == null || cursor.getCount() < 1) ? View.VISIBLE : View.GONE);
            }


            if (mResumeHandler != null) {
                mResumeHandler.handleMessage(null);
                mResumeHandler = null;
            }


        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
            if (mAdapter != null)
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
            ImageView highlight;
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
            holder.highlight = (ImageView)root.findViewById(R.id.highlight);
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

            int pos = cursor.getPosition();
            boolean checked = getListView().isItemChecked(pos);
            boolean db_unread = cursor.getInt(mUnreadIdx) == 1 ? true : false;
            boolean local_unread = (mHighlightedCache.get(pos) == null);
            boolean unread = db_unread && local_unread;

            holder.title.setTextAppearance(context, unread ? R.style.TextAppearance_article_unread : R.style.TextAppearance_article_read);
            view.setBackgroundResource(unread ? R.drawable.rightborder_unread : R.drawable.rightborder_read);
            holder.highlight.setVisibility(mShowHighlighted && checked ? View.VISIBLE : View.GONE);
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

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mArticleSelectedListener != null)
            mArticleSelectedListener.onArticleSelected(id, mTagId, mFeedId);
        setHighlightedArticle(position, id);
    }


    public interface ArticleSelectedListener {
        void onArticleSelected(long article_id, long tag_id, long feed_id);
    };

    private ArticleSelectedListener mArticleSelectedListener;
    public void setArticleSelectedListener(ArticleSelectedListener listener) {
        mArticleSelectedListener = listener;
    }
}
