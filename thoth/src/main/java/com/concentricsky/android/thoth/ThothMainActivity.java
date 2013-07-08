package com.concentricsky.android.thoth;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.*;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.SparseIntArray;
import android.view.*;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import com.codeslap.gist.SimpleCursorLoader;


public class ThothMainActivity extends FragmentActivity
                               implements LoaderManager.LoaderCallbacks<Cursor>,FragmentManager.OnBackStackChangedListener {
    private ActionBar mActionBar;
    private DrawerLayout mDrawerLayout;
    private ExpandableListView mDrawerList;
    private ThothActionBarDrawerToggle mDrawerToggle;
    private FragmentManager mFragmentManager;
    private ArticleListFragment mArticleListFragment;
    private SubscribeFragment mSubscribeFragment;
    private ThothDrawerAdapter mDrawerAdapter;


    private SparseIntArray mNavLoaderIds;
    private static final int TAG_LOADER_ID=-1;
    private boolean mSharing = false;
    private ArticleFragment mArticleFragment;
    private LoaderManager mLoaderManager;
    private SQLiteDatabase mWritableDb;
    private DrawerItemClickListener mDrawerClickListener;
    private ImportFragment mImportFragment;
    private TextView mNoFeedsText;

    private long mTagId = -1;
    private long mFeedId = -1;
    private ThothActivityState mActivityState = ThothActivityState.THOTH_STATE_ALL_FEEDS;
    private int mArticlePosition= -1;

    public enum ThothActivityState {
        THOTH_STATE_ALL_FEEDS, THOTH_STATE_FEED, THOTH_STATE_TAG, //ArticleListFragment with some type of cursor
        THOTH_STATE_DETAIL, //ArticleFragment with article id plus ArticleListFragment state
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        setProgressBarIndeterminateVisibility(false);

        //set up action bar
        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);

        //set up navigation drawer
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerAdapter = new ThothDrawerAdapter();
        mDrawerList = (ExpandableListView)findViewById(R.id.navigation_list);
        mNoFeedsText = new TextView(this);
        mNoFeedsText.setText(R.string.empty_feeds_message);
//        mDrawerList.addHeaderView(mNoFeedsText);
        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerClickListener = new DrawerItemClickListener();
        mDrawerList.setOnGroupClickListener(mDrawerClickListener);

        mDrawerToggle = new ThothActionBarDrawerToggle();
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mNavLoaderIds = new SparseIntArray();         //navigation drawer: map loader ids -> tag ids
        mLoaderManager = getSupportLoaderManager();
        if (mLoaderManager.getLoader(TAG_LOADER_ID) == null) {
            mLoaderManager.initLoader(TAG_LOADER_ID, null, this); //navigation drawer: start tag loader
        }


        //set up fragments
        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.addOnBackStackChangedListener(this);
        mArticleListFragment = new ArticleListFragment();
        mSubscribeFragment = null; //create on demand


        if (savedInstanceState != null) {
            String state = savedInstanceState.getString("thoth_state", null);
            mActivityState = state != null ? ThothActivityState.valueOf(state) : ThothActivityState.THOTH_STATE_ALL_FEEDS;

            mFeedId = savedInstanceState.getLong("thoth_feed_id", 0);
            mTagId = savedInstanceState.getLong("thoth_tag_id", -1);
            mArticlePosition = savedInstanceState.getInt("thoth_article_position", -1);

            int scrollTo = savedInstanceState.getInt("thoth_scroll_position", -1);
            mArticleListFragment.scrollToPosition(scrollTo);
        }


        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            // share intent
            mSharing = true;
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            showSubscribe(url);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        else
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            //import google reader takeout zip
            mSharing = true;
            Uri uri  = getIntent().getData();
            showImport(uri);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        else {
            switch (mActivityState) {
                case THOTH_STATE_TAG:
                    mArticleListFragment.setTag(mTagId);
                    showArticleList();
                    break;
                case THOTH_STATE_FEED:
                    mArticleListFragment.setFeed(mFeedId);
                    showArticleList();
                    break;
                case THOTH_STATE_ALL_FEEDS:
                    mArticleListFragment.setFeed(0);
                    showArticleList();
                    break;
                case THOTH_STATE_DETAIL:
                    if (mTagId != -1) {
                        mArticleListFragment.setTag(mTagId);
                    }
                    else {
                        mArticleListFragment.setFeed(mFeedId != -1 ? mFeedId : 0);
                    }
                    mArticleListFragment.resumeArticleDetail(new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            showArticle(mArticleListFragment.getCursor(), mArticlePosition, true);
                        }
                    });
                    showArticleList();
//                    showArticle(null, mArticlePosition);
                    break;
            }
        }


    }





    public void reloadTags() {
        mLoaderManager.restartLoader(TAG_LOADER_ID, null, this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.thoth_main, menu);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_subscribe).setVisible(false);
        menu.findItem(R.id.action_share).setVisible(false);
        menu.findItem(R.id.action_visitpage).setVisible(false);
        menu.findItem(R.id.action_toggle_unread).setVisible(false);
        menu.findItem(R.id.action_mark_as_read).setVisible(false);
        return true;
    }

    private ThothFragmentInterface getCurrentFragment()
    {
        return (ThothFragmentInterface)mFragmentManager.findFragmentByTag("current_fragment");
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean is_open = mDrawerLayout.isDrawerOpen(mDrawerList);

        ThothFragmentInterface frag = getCurrentFragment();
        if (frag != null) {
            frag.onPrepareOptionsMenu(menu, is_open);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        ThothFragmentInterface frag = getCurrentFragment();
        if (frag != null && frag.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /*
     * ActionBarDrawerToggle needs these
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }


    /*
     * LoaderManager Methods
     */
    @Override
    public Loader<Cursor> onCreateLoader(int loader_id, Bundle bundle) {
        if (loader_id == TAG_LOADER_ID) { // tag loader
            return new SimpleCursorLoader(this) {
                @Override
                public Cursor loadInBackground() {
                    return ThothDatabaseHelper.getInstance().getTagCursor();
                }
            };
        }

        final long tag_id = mNavLoaderIds.get(loader_id, -1);
        if (tag_id != -1) {
            return new SimpleCursorLoader(this) {
                @Override
                public Cursor loadInBackground() {
                    return ThothDatabaseHelper.getInstance().getFeedCursor(tag_id);
                }
            };
        }
        // shouldn't be here!
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int loader_id = loader.getId();
        if (loader_id == TAG_LOADER_ID) { //tag cursor
            if (cursor.getCount() < 2) {
                mArticleListFragment.setNoFeeds(true);
            } else {
//                mDrawerList.removeHeaderView(mNoFeedsText);
                mDrawerAdapter.changeCursor(cursor);
                mArticleListFragment.setNoFeeds(false);
            }
        }
        else {
            //loader_id is the group pos of the children cursor we are trying to load
            mDrawerAdapter.setChildrenCursor(loader_id, cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mDrawerAdapter == null)
            return;

        int loader_id = loader.getId();
        if (loader_id == TAG_LOADER_ID) { //tag cursor
            mDrawerAdapter.changeCursor(null);
        } else {
            mDrawerAdapter.setChildrenCursor(loader_id, null);
        }
    }

    @Override
    public void onBackStackChanged() {
        if (mSharing && mFragmentManager.getBackStackEntryCount() == 0)
            finish();
    }



    /*
     * Private Classes
     */
    private class DrawerItemClickListener implements ExpandableListView.OnGroupClickListener
    {
        @Override
        public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
            return false;
        }
    }

    private class ThothActionBarDrawerToggle extends ActionBarDrawerToggle {
        public ThothActionBarDrawerToggle() {
            super(ThothMainActivity.this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
        }
        @Override
        public void onDrawerClosed(View drawerView) {
            invalidateOptionsMenu();
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            reloadTags();
            invalidateOptionsMenu();
        }
    }


    private class ThothDrawerAdapter extends SimpleCursorTreeAdapter {

        public ThothDrawerAdapter() {
            super(ThothMainActivity.this,
                    null,
                    R.layout.item_navigation_group,
                    new String[]{"title","unread"}, // groupFrom,
                    new int[]{android.R.id.text1, android.R.id.text2}, // groupTo,
                    R.layout.item_navigation_child,
                    new String[]{"title","unread"}, // childFrom,
                    new int[]{android.R.id.text1, android.R.id.text2} // childTo,
            );
        }


        protected void bindView(View view, Context context, Cursor cursor, boolean isLastChild) {
            TextView tv = (TextView) view.findViewById(android.R.id.text1);
            String title = String.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            tv.setText(title.isEmpty() ? getString(R.string.unfiled) : title);
            tv = (TextView) view.findViewById(android.R.id.text2);
            long unread = cursor.getLong(cursor.getColumnIndexOrThrow("unread"));
            if (unread > 0) {
                tv.setVisibility(View.VISIBLE);
                tv.setText( String.valueOf(unread) );
            }
            else {
                tv.setVisibility(View.INVISIBLE);
            }

        }

        @Override
        protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
            bindView(view,context,cursor,isLastChild);
            final long feed_id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mArticleListFragment.setFeed(feed_id);
                    mFeedId = feed_id;
                    mActivityState = ThothActivityState.THOTH_STATE_FEED;
                    showArticleList();
                    mDrawerLayout.closeDrawers();
                }
            });
            view.setBackgroundResource(R.color.navigation_child_background);
        }
        @Override
        protected void bindGroupView(View view, Context context, Cursor cursor, boolean isLastChild) {
            bindView(view,context,cursor,isLastChild);
            final int groupPosition = cursor.getPosition();
            boolean is_expanded = mDrawerList.isGroupExpanded(groupPosition);

            ImageView iv = (ImageView)view.findViewById(R.id.group_indicator);
            iv.setImageResource(is_expanded ? R.drawable.collapse : R.drawable.expand);
            if (groupPosition == 0) {
                iv.setVisibility(View.INVISIBLE);
            } else {
                iv.setVisibility(View.VISIBLE);
            }

            View left = view.findViewById(R.id.left);
            View right = view.findViewById(R.id.right);
            left.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (groupPosition == 0) { // All feeds clicked
                        mArticleListFragment.setFeed(0);
                        mActivityState = ThothActivityState.THOTH_STATE_ALL_FEEDS;
                    }
                    else {
                        mTagId = mDrawerAdapter.getGroupId(groupPosition);
                        mArticleListFragment.setTag(mTagId);
                        mActivityState = ThothActivityState.THOTH_STATE_TAG;
                    }
                    showArticleList();
                    mDrawerLayout.closeDrawers();
                }
            });
            view.setBackgroundResource(is_expanded ? R.color.navigation_expanded_background : R.color.navigation_collapsed_background);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor cursor) {
            int tag_id = cursor.getInt(cursor.getColumnIndex("_id"));
            int loader_id = cursor.getPosition();

            mNavLoaderIds.append(loader_id, tag_id);

            Loader loader = mLoaderManager.getLoader(loader_id);
            if (loader != null && !loader.isReset()) {
                mLoaderManager.restartLoader(loader_id, null, ThothMainActivity.this);
            }
            else {
                mLoaderManager.initLoader(loader_id, null, ThothMainActivity.this);
            }

            return null;
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("thoth_state", mActivityState.name());
        outState.putLong("thoth_feed_id", mFeedId);
        outState.putLong("thoth_tag_id", mTagId);
        outState.putInt("thoth_article_position", mArticlePosition);
        outState.putInt("thoth_scroll_position", mArticleListFragment.getScrollPosition());
        super.onSaveInstanceState(outState);
    }
/*
     * Private Methods
     */

    public void showArticleList()
    {
        mFragmentManager.beginTransaction().replace(R.id.content_frame, mArticleListFragment, "current_fragment").commit();
        invalidateOptionsMenu();
    }

    public void showSubscribe(String url)
    {
        if (mSubscribeFragment == null) {
            mSubscribeFragment = new SubscribeFragment();
        }
        mSubscribeFragment.setUrl(url);
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.content_frame, mSubscribeFragment, "current_fragment").addToBackStack("Subscribe");
        trans.commit();
        invalidateOptionsMenu();
    }

    public void showArticle(Cursor cursor, int position)
    {
        showArticle(cursor,position,false);
    }
    public void showArticle(Cursor cursor, int position, boolean allow_state_loss)
    {
        if (mArticleFragment == null) {
            mArticleFragment = new ArticleFragment();
        }
        mArticleFragment.setArticle(cursor, position);

        mActivityState = ThothActivityState.THOTH_STATE_DETAIL;
        mArticlePosition = position;

        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.content_frame, mArticleFragment, "current_fragment").addToBackStack("Article");
        if (allow_state_loss)
            trans.commitAllowingStateLoss();
        else
            trans.commit();
        invalidateOptionsMenu();
    }


    public void showImport(Uri uri)
    {
        if (mImportFragment == null) {
            mImportFragment = new ImportFragment();
        }
        mImportFragment.setZipfileUri(uri);
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.content_frame, mImportFragment, "current_fragment").addToBackStack("Import");
        trans.commit();
        invalidateOptionsMenu();
    }

}
