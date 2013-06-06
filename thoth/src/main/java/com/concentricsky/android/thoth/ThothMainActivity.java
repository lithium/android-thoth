package com.concentricsky.android.thoth;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.codeslap.gist.SimpleCursorLoader;
import com.concentricsky.android.thoth.models.Feed;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


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

//    private RequestQueue mRequestQueue;

    private SparseIntArray mNavLoaderIds;
    private static final int TAG_LOADER_ID=-1;
    private boolean mSharing = false;
    private ArticleFragment mArticleFragment;
    private LoaderManager mLoaderManager;
    private ThothDatabaseHelper mDbHelper;
    private SQLiteDatabase mWritableDb;
    private RequestQueue mRequestQueue;
    private DrawerItemClickListener mDrawerClickListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set up action bar
        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);

        //set up navigation drawer
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerAdapter = new ThothDrawerAdapter();
        mDrawerList = (ExpandableListView)findViewById(R.id.navigation_drawer);
        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerClickListener = new DrawerItemClickListener();
        mDrawerList.setOnChildClickListener(mDrawerClickListener);
//        mDrawerList.setOnGroupClickListener(mDrawerClickListener);
        mDrawerList.setOnGroupExpandListener(mDrawerClickListener);

        mDrawerToggle = new ThothActionBarDrawerToggle();
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mNavLoaderIds = new SparseIntArray();         //navigation drawer: map loader ids -> tag ids
        mLoaderManager =  getSupportLoaderManager();
        mLoaderManager.initLoader(TAG_LOADER_ID, null, this); //navigation drawer: start tag loader


        //set up fragments
        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.addOnBackStackChangedListener(this);
        mArticleListFragment = new ArticleListFragment();
        mSubscribeFragment = null; //create on demand


        mDbHelper = ThothDatabaseHelper.getInstance();
        mRequestQueue = Volley.newRequestQueue(this);

        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            // share intent
            mSharing = true;
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            showSubscribe(url);
        }
        else
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
//            mSharing = true;
//            showSubscribe("");
            try {
                Uri uri  = getIntent().getData();
                if (uri != null) {
                    InputStream zip = getContentResolver().openInputStream(uri);
                    
                    ArrayList<Feed> feeds = mDbHelper.importTakeoutZip(zip);
                    
                    if (feeds != null) {
//                        final RequestQueue queue = Volley.newRequestQueue(this);
                        for (Feed feed : feeds) {
                            new AddFeedHelper(feed);
//                            final SubscribeToFeedRequest request = new SubscribeToFeedRequest(feed.url,
//                                    new Response.Listener<Feed>() {
//                                        @Override
//                                        public void onResponse(Feed feed) {
//                                            if (feed.title == null) { // only found a feed url, re-scrape
////                                                queue.add(new SubscribeToFeedRequest(feed.url, this, this));
//                                                //found an html...
//                                            }
//                                            else {
//                                                feed.save(writedb);
//                                            }
//                                        }
//                                    },
//                                    new Response.ErrorListener() {
//                                        @Override
//                                        public void onErrorResponse(VolleyError error) {
//
//                                        }
//                                    });
//                            queue.add(request);
                        }
                    }
                }
            } catch (IOException e) {
                finish();
//                e.printStackTrace();
            }
        }
        else {

            if (savedInstanceState == null) {
                //initial startup
                showArticleList();
            }
        }


    }

    private class AddFeedHelper implements Response.Listener<Feed>, Response.ErrorListener {
        private Feed mFeed;
        public AddFeedHelper(Feed f) {
            mFeed = f;
            mRequestQueue.add(new SubscribeToFeedRequest(mFeed.url, this, this));
        }

        @Override
        public void onResponse(Feed response) {
            if (response.title == null) {//
                mRequestQueue.add(new SubscribeToFeedRequest(response.url, this, this));
            }
            else {
                response.tags = mFeed.tags;
                response.save(mDbHelper.getWritableDatabase());
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {

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
            mDrawerAdapter.changeCursor(cursor);
        }
        else {
            //loader_id is the group pos of the children cursor we are trying to load
            mDrawerAdapter.setChildrenCursor(loader_id, cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
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
    private class DrawerItemClickListener implements ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupExpandListener {

        @Override
        public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long l) {
            mArticleListFragment.setFeed(l);
            showArticleList();
            mDrawerLayout.closeDrawers();

            return true;
        }

        @Override
        public void onGroupExpand(int groupPosition) {
            mArticleListFragment.setTag(mDrawerAdapter.getGroupId(groupPosition));
            showArticleList();
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
        }
    }


    private class ThothDrawerAdapter extends SimpleCursorTreeAdapter {

        public ThothDrawerAdapter() {
            super(ThothMainActivity.this,
                    null,
                    android.R.layout.simple_expandable_list_item_1,
                    new String[]{"title"}, // groupFrom,
                    new int[]{android.R.id.text1}, // groupTo,
                    android.R.layout.simple_expandable_list_item_1,
                    new String[]{"title"}, // childFrom,
                    new int[]{android.R.id.text1}); // childTo);
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

    public void showArticle(long feed_id, int position)
    {
        if (mArticleFragment == null) {
            mArticleFragment = new ArticleFragment();
        }
        mArticleFragment.setArticle(feed_id, position);
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.content_frame, mArticleFragment, "current_fragment").addToBackStack("Article");
        trans.commit();
        invalidateOptionsMenu();
    }

}
