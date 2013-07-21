package com.concentricsky.android.thoth;

import android.app.Activity;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.*;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.codeslap.gist.SimpleCursorLoader;
import com.concentricsky.android.thoth.models.Article;
import com.concentricsky.android.thoth.models.Feed;
import com.concentricsky.android.thoth.models.Tag;

/**
 * Created by wiggins on 5/17/13.
 */
public class ArticleListFragment extends ListFragment
                                 implements ThothFragmentInterface
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
    private TextView mNoFeedsText;
    private ProgressBar mProgress;
    private boolean mNoFeeds=false;
    private ListView mList;
    private boolean mHideRead = false;
    private MenuItem mToggleMenuItem;
    private boolean mPaused = false;
    private SharedPreferences mPreferences;
    private int mScrollPosition = -1;
    private Handler mResumeHandler;
    private MenuItem mMarkAsReadMenuItem;
    private MenuItem mManageFeedsMenuItem;
    private View mEmpty;
    private AsyncTask<Void, Integer, Void> mTask;

    public ArticleListFragment() {
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentActivity activity = getActivity();

        mAdapter = new ArticleListAdapter(activity, null);
        mLoaderManager = activity.getSupportLoaderManager();

        mPreferences = activity.getSharedPreferences("preferences", 0);
        mHideRead = mPreferences.getBoolean("hideUnread", false);
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

        SyncResponseReceiver receiver = new SyncResponseReceiver();
        IntentFilter filter = new IntentFilter(RefreshFeedIntentService.ALL_FEEDS_SYNCED);
        filter.addAction(RefreshFeedIntentService.FEED_SYNCED);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        getActivity().registerReceiver(receiver, filter);

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
        setNoFeeds(mNoFeeds);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPaused = true;
        if (mTask != null)
            mTask.cancel(true);
    }

    @Override
    public void onDestroyView() {
        mLoaderManager.destroyLoader(FEED_LOADER_ID);
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
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {
        menu.findItem(R.id.action_subscribe).setVisible(!drawer_open);
        mRefreshMenuItem = menu.findItem(R.id.action_refresh);
        mRefreshMenuItem.setVisible(mRefreshing || mNoFeeds ? false : !drawer_open);

        mToggleMenuItem = menu.findItem(R.id.action_toggle_unread);
        mToggleMenuItem.setVisible(true);
        mToggleMenuItem.setTitle(mHideRead ? R.string.action_show_read : R.string.action_hide_read);

        mMarkAsReadMenuItem = menu.findItem(R.id.action_mark_as_read);
        mManageFeedsMenuItem = menu.findItem(R.id.action_manage_feeds);
        mMarkAsReadMenuItem.setVisible(mNoFeeds ? false : true);
        mManageFeedsMenuItem.setVisible(mNoFeeds ? false : true);
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

    public class SyncResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            String action = intent.getAction();

            if (RefreshFeedIntentService.ALL_FEEDS_SYNCED.equals(action)) {
                mRefreshing = false;
                activity.setProgressBarIndeterminateVisibility(false);
                activity.setProgressBarVisibility(false);
                mRefreshMenuItem.setVisible(true);
                mLoaderManager.restartLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
            }
            else
            if (RefreshFeedIntentService.FEED_SYNCED.equals(action)) {
                int progress = intent.getIntExtra("progress",50);
                progress = (int)(10000*((float)progress/100));
                activity.setProgress(progress);
                mLoaderManager.restartLoader(ARTICLE_LOADER_ID, null, new ArticleCursorLoader());
            }
        }
    }

    public void setNoFeeds(boolean has_none) {
        mNoFeeds = has_none;
        if (mNoFeedsText != null) {
            mNoFeedsText.setVisibility(has_none ? View.VISIBLE : View.GONE);
        }
        if (mManageFeedsMenuItem != null)
            mManageFeedsMenuItem.setVisible(mNoFeeds ? false : true);
        if (mMarkAsReadMenuItem != null)
            mMarkAsReadMenuItem.setVisible(mNoFeeds ? false : true);
        if (mRefreshMenuItem != null)
            mRefreshMenuItem.setVisible(mNoFeeds ? false : true);

        if (mEmpty != null) {
            if (mNoFeeds)
                mEmpty.setVisibility(View.GONE);
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
            mAdapter.changeCursor(cursor);
            if (mProgress != null)
                mProgress.setVisibility(View.GONE);
            if (mScrollPosition != -1)
                mList.setSelectionFromTop(mScrollPosition, 0);

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

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ThothMainActivity activity = (ThothMainActivity) getActivity();
        Cursor cursor = mAdapter.getCursor();
        activity.showArticle(cursor, position);
    }

}
