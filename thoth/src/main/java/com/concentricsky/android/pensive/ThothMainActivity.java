package com.concentricsky.android.pensive;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.*;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.codeslap.gist.SimpleCursorLoader;


public class ThothMainActivity extends FragmentActivity
                               implements LoaderManager.LoaderCallbacks<Cursor>,FragmentManager.OnBackStackChangedListener, ArticleListFragment.ArticleSelectedListener {
    private ActionBar mActionBar;
    private DrawerLayout mDrawerLayout;
    private ExpandableListView mDrawerList;
    private ThothActionBarDrawerToggle mDrawerToggle;
    private DrawerItemClickListener mDrawerClickListener;
    private ThothDrawerAdapter mDrawerAdapter;

    private FragmentManager mFragmentManager;
    private LoaderManager mLoaderManager;
    private SparseIntArray mNavLoaderIds;
    private static final int TAG_LOADER_ID=-1;

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    private boolean mSharing = false;
    private long mTagId = -1;
    private long mFeedId = -1;
    private ThothActivityState mActivityState = ThothActivityState.THOTH_STATE_ALL_FEEDS;
    private int mArticlePosition= -1;
    private int mScrollTo=-1;
    private boolean mNoFeeds=false;

    @Override
    public void onArticleSelected(long article_id, long tag_id, long feed_id){
        Fragment frag = getSupportFragmentManager().findFragmentByTag("ArticleDetail");
        if (frag != null) { //tablet layout
            try {
                ArticleFragment articleFragment = (ArticleFragment)frag;
                articleFragment.setArticle(article_id, tag_id, feed_id);
            } catch (ClassCastException e) {}
        } else {
            pushArticleDetail(article_id, tag_id, feed_id);
        }

    }

    public enum ThothActivityState {
        THOTH_STATE_ALL_FEEDS, THOTH_STATE_FEED, THOTH_STATE_TAG, //ArticleListFragment with some type of cursor
        THOTH_STATE_DETAIL, //ArticleFragment with article id plus ArticleListFragment state
        THOTH_STATE_DISREGARD,
    };


    @Override
    protected void onResume() {
        super.onResume();
        if (mLoaderManager.getLoader(TAG_LOADER_ID) == null) {
            mLoaderManager.initLoader(TAG_LOADER_ID, null, this); //navigation drawer: start tag loader
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
            requestWindowFeature(Window.FEATURE_PROGRESS);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //set up action bar
        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);
        setProgressBarIndeterminateVisibility(false);


        //set up navigation drawer
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerAdapter = new ThothDrawerAdapter();
        mDrawerList = (ExpandableListView)findViewById(R.id.navigation_list);
        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerClickListener = new DrawerItemClickListener();
        mDrawerList.setOnGroupClickListener(mDrawerClickListener);

        mDrawerToggle = new ThothActionBarDrawerToggle();
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mNavLoaderIds = new SparseIntArray();         //navigation drawer: map loader ids -> tag ids
        mLoaderManager = getSupportLoaderManager();

        mRequestQueue = Volley.newRequestQueue(this);
        mImageLoader = new ImageLoader(mRequestQueue, new BitmapLruCache());


        //set up sync adapter
        ContentResolver contentResolver = getContentResolver();
        AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        String authority = getString(R.string.sync_provider_authority);
        Account newAccount = new Account(getString(R.string.app_name), getString(R.string.sync_account_type));
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            contentResolver.setIsSyncable(newAccount, authority, 1);
        }
        contentResolver.addPeriodicSync(newAccount, authority, new Bundle(), 900); // 15 minutes
        contentResolver.setSyncAutomatically(newAccount, authority, true);


        //set up fragments
        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.addOnBackStackChangedListener(this);


//        if (savedInstanceState != null) {
//            String state = savedInstanceState.getString("thoth_state", null);
//            mActivityState = state != null ? ThothActivityState.valueOf(state) : ThothActivityState.THOTH_STATE_ALL_FEEDS;
//
//            mFeedId = savedInstanceState.getLong("thoth_feed_id", 0);
//            mTagId = savedInstanceState.getLong("thoth_tag_id", -1);
//            mArticlePosition = savedInstanceState.getInt("thoth_article_position", -1);
//
//            mScrollTo = savedInstanceState.getInt("thoth_scroll_position", -1);
//        }


        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            // share intent
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            showSubscribe(url);
        }
        else
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            //import opml / google-reader-takeout zip
            Uri uri  = getIntent().getData();
            showImport(uri);
        } else {

            showAllFeeds();

            if (savedInstanceState != null) {
                long article_id = savedInstanceState.getLong("article_id",-1);
                long tag_id = savedInstanceState.getLong("tag_id",-1);
                long feed_id = savedInstanceState.getLong("feed_id",-1);

                if (article_id > 0) {
                    pushArticleDetail(article_id, tag_id, feed_id);
                } else if (tag_id > 0 && feed_id > 0) {
                    int scroll_position = savedInstanceState.getInt("scroll_position",0);
                    int scroll_offset = savedInstanceState.getInt("scroll_offset",0);
                    pushArticleList(tag_id, feed_id, scroll_position, scroll_offset);
                }
            }

            // show the drawer if the user hasn't opened it themselves yet.
            SharedPreferences prefs = getSharedPreferences("preferences", 0);
            if (!prefs.getBoolean("userUnderstandsDrawer", false)) {
                mDrawerLayout.openDrawer(GravityCompat.START);
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

        try {
            ThothFragmentInterface frag = getCurrentFragment();
            if (frag != null) {
                frag.onPrepareOptionsMenu(menu, is_open);
            }
        } catch (java.lang.ClassCastException e) {
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int itemId = item.getItemId();
        if (itemId == R.id.action_about) {
            showAboutDialog();
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
            if (cursor == null || cursor.getCount() < 1) {
                mNoFeeds = true;
            } else {
                mNoFeeds = false;
            }
//            if (mArticleListFragment != null)
//                mArticleListFragment.setNoFeeds(mNoFeeds);
            mDrawerAdapter.changeCursor(cursor);
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
//            mDrawerAdapter.setChildrenCursor(loader_id, null);
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
            return true;
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
            invalidateOptionsMenu();
            SharedPreferences prefs = getSharedPreferences("preferences", 0);
            prefs.edit().putBoolean("userUnderstandsDrawer", true).commit();
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

        protected void bindView(View view, Context context, Cursor cursor) {
            TextView tv = (TextView) view.findViewById(android.R.id.text1);
            String title = String.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            tv.setText(title);

            tv = (TextView) view.findViewById(android.R.id.text2);
            int unread_idx = cursor.getColumnIndex("unread");
            if (unread_idx != -1) {
                long unread = cursor.getLong(unread_idx);
                if (unread > 0) {
                    tv.setVisibility(View.VISIBLE);
                    tv.setText( String.valueOf(unread) );
                }
                else {
                    tv.setVisibility(View.INVISIBLE);
                }
            }
            else {
                tv.setVisibility(View.INVISIBLE);
            }

        }

        @Override
        protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
            bindView(view,context,cursor);
            final long feed_id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            view.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    try {
                        ThothNavigationDrawerListener listener = (ThothNavigationDrawerListener)getCurrentFragment();
                        listener.onNavigationClickFeed(feed_id);
                    } catch (ClassCastException e) {
                    }
//                    if (feed_id != mFeedId) {
//
//                        mFeedId = feed_id;
//                        mActivityState = ThothActivityState.THOTH_STATE_FEED;
////                        mArticleListFragment = ArticleListFragment.newInstance(feed_id, -1);
////                        pushArticleList(true);
//
//                        pushArticleList(-1, feed_id, 0, 0);
//
//
//                    } else if (mActivityState != ThothActivityState.THOTH_STATE_FEED) {
//                        getSupportFragmentManager().popBackStack();
//                    }
                    mDrawerLayout.closeDrawers();
                }
            });
            view.setBackgroundResource(R.color.navigation_child_background);

            NetworkImageView icon = (NetworkImageView) view.findViewById(android.R.id.icon1);
            icon.setDefaultImageResId(R.drawable.rss_default);
            icon.setErrorImageResId(R.drawable.rss_default);
            String link = cursor.getString(cursor.getColumnIndexOrThrow("link"));
            if (!TextUtils.isEmpty(link)) {
                icon.setImageUrl(link+"/favicon.ico", mImageLoader);
            }
            else {
                icon.setImageUrl(null, mImageLoader);
            }
        }
        @Override
        protected void bindGroupView(View view, Context context, Cursor cursor, boolean is_expanded) {
            bindView(view, context, cursor);
            final int groupPosition = cursor.getPosition();
            final long _id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));

            ImageView iv = (ImageView)view.findViewById(R.id.group_indicator);
            iv.setImageResource(is_expanded ? R.drawable.expand : R.drawable.collapse);

            if (_id < 0) {
                iv.setVisibility(View.INVISIBLE);
            } else {
                iv.setVisibility(View.VISIBLE);
            }

            View left = view.findViewById(R.id.left);
            View right = view.findViewById(R.id.right);
            left.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (_id == -2) {
                        //all feeds
                        try {
                            ThothNavigationDrawerListener listener = (ThothNavigationDrawerListener)getCurrentFragment();
                            listener.onNavigationAllFeeds();
                        } catch (ClassCastException e) {
                        }
                    }
                    else if (_id == -3) {
                        //manage feeds
                        showManageFeeds();
                    }
                    else if (_id > 0) {
                        //tag
                        long tag_id = mDrawerAdapter.getGroupId(groupPosition);
                        try {
                            ThothNavigationDrawerListener listener = (ThothNavigationDrawerListener)getCurrentFragment();
                            listener.onNavigationClickTag(tag_id);
                        } catch (ClassCastException e) { }
                    }

                    mDrawerLayout.closeDrawers();
                }
            });
            right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mDrawerList.isGroupExpanded(groupPosition))
                        mDrawerList.collapseGroup(groupPosition);
                    else
                        mDrawerList.expandGroup(groupPosition);
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

        @Override
        public void changeCursor(Cursor cursor) {
            String[] extras_fields = new String[] {"_id", "title"};
            MatrixCursor top_extras = new MatrixCursor(extras_fields);
            MatrixCursor bottom_extras = new MatrixCursor(extras_fields);

            if (!mNoFeeds)
                top_extras.addRow(new String[] {"-2", getString(R.string.all_feeds)});
            bottom_extras.addRow(new String[] {"-3", getString(R.string.action_manage_feeds)});

            Cursor newCursor = new MergeCursor(new Cursor[] {top_extras, cursor, bottom_extras});
            super.changeCursor(newCursor);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
//        outState.putString("thoth_state", mActivityState.name());
//        outState.putLong("thoth_feed_id", mFeedId);
//        outState.putLong("thoth_tag_id", mTagId);
//        outState.putInt("thoth_article_position", mArticlePosition);
//        outState.putInt("thoth_scroll_position", mArticleListFragment.getScrollPosition());
        super.onSaveInstanceState(outState);
    }

//    public void pushArticleList(boolean add_to_back_stack)
//    {
//        if (mScrollTo != -1) {
//            mArticleListFragment.scrollToPosition(mScrollTo);
//            mScrollTo = -1;
//        }
//        FragmentTransaction trans = mFragmentManager.beginTransaction();
//        trans.replace(R.id.content_frame, mArticleListFragment, "current_fragment");
//        if (add_to_back_stack)
//            trans.addToBackStack("ArticleList");
//        trans.commit();
//        invalidateOptionsMenu();
//    }


//    public void showArticle(Cursor cursor, int position)
//    {
//        showArticle(cursor,position,false);
//    }
//    public void showArticle(Cursor cursor, int position, boolean allow_state_loss)
//    {
//        if (mArticleFragment == null) {
//            mArticleFragment = new ArticleFragment();
//        }
//        mArticleFragment.setArticle(cursor, position);
//
//        mActivityState = ThothActivityState.THOTH_STATE_DETAIL;
//        mArticlePosition = position;
//
//        FragmentTransaction trans = mFragmentManager.beginTransaction();
//        trans.replace(R.id.content_frame, mArticleFragment, "current_fragment").addToBackStack("Article");
//        if (allow_state_loss)
//            trans.commitAllowingStateLoss();
//        else
//            trans.commit();
//        invalidateOptionsMenu();
//    }
    public void showSubscribe(String url)
    {
        SubscribeFragment frag = new SubscribeFragment();
        mActivityState = ThothActivityState.THOTH_STATE_DISREGARD;
        frag.setUrl(url);
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.content_frame, frag, "current_fragment").addToBackStack("Subscribe");
        trans.commit();
        invalidateOptionsMenu();
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }


    public void showImport(Uri uri)
    {
        ImportFragment frag = new ImportFragment();
        mActivityState = ThothActivityState.THOTH_STATE_DISREGARD;
        frag.setZipfileUri(uri);
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.content_frame, frag, "current_fragment").addToBackStack("Import");
        trans.commit();
        invalidateOptionsMenu();
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void showManageFeeds()
    {
        mDrawerLayout.closeDrawers();
        ManageFragment frag = new ManageFragment();
        mActivityState = ThothActivityState.THOTH_STATE_DISREGARD;
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.content_frame, frag, "current_fragment").addToBackStack("Manage");
        trans.commit();
        invalidateOptionsMenu();
    }

    private void showAboutDialog() {
        AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
        aboutDialogFragment.show(getSupportFragmentManager(), "About");
    }

    public void showAllFeeds()
    {
        ArticleListFragment frag = ArticleListFragment.newInstance(-1, 0);
        frag.setArticleSelectedListener(this);
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.content_frame, frag, "current_fragment");
        trans.addToBackStack("AllFeeds");
        trans.commit();
    }

    public void pushArticleList(long tag_id, long feed_id, int scroll_position, int scroll_offset)
    {
        ArticleListFragment frag = ArticleListFragment.newInstance(tag_id, feed_id);
        frag.scrollToPosition(scroll_position, scroll_offset);
        frag.setArticleSelectedListener(this);
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.content_frame, frag, "current_fragment");
        trans.addToBackStack("ArticleList");
        trans.commit();
        invalidateOptionsMenu();
    }


//    public void pushArticleDetail(long article_id, Cursor cursor)
//    {
//        ArticleFragment frag = ArticleFragment.newInstance(article_id,cursor);
//        article_detail(frag);
//    }
    public void pushArticleDetail(long article_id, long tag_id, long feed_id)
    {
        ArticleFragment frag = ArticleFragment.newInstance(article_id, tag_id, feed_id);
        article_detail(frag);
    }
    private void article_detail(ArticleFragment frag)
    {
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.content_frame, frag, "current_fragment");
        trans.addToBackStack("ArticleDetail");
        trans.commit();
        invalidateOptionsMenu();
    }
}
