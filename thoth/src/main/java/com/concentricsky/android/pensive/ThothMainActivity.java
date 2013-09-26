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
import android.widget.*;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.codeslap.gist.SimpleCursorLoader;


public class ThothMainActivity extends FragmentActivity
                               implements  ArticleListFragment.ArticleSelectedListener,
                                           FragmentManager.OnBackStackChangedListener
{
    private ActionBar mActionBar;
    private DrawerLayout mDrawerLayout;
    private ThothActionBarDrawerToggle mDrawerToggle;

    private FragmentManager mFragmentManager;

    private boolean mIsTabletLayout=false;
    private NavigationFragment mNavigationFragment;

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

    @Override
    protected void onResume() {
        super.onResume();
        //todo: does navigation fragment need this?
//        if (mLoaderManager.getLoader(TAG_LOADER_ID) == null) {
//            mLoaderManager.initLoader(TAG_LOADER_ID, null, this); //navigation drawer: start tag loader
//        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
            requestWindowFeature(Window.FEATURE_PROGRESS);
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setProgressBarIndeterminateVisibility(false);


        mActionBar = getActionBar();

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            mIsTabletLayout = true;
        } else {
            mIsTabletLayout = false;

            //set up action bar home drawer toggle
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeButtonEnabled(true);
            mDrawerToggle = new ThothActionBarDrawerToggle();
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        }



        //set up fragments
        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.addOnBackStackChangedListener(this);
        mNavigationFragment = new NavigationFragment();




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
            //regular app start up

            if (savedInstanceState == null) {
                // only add Navigation and All Feeds to backstack if we aren't resuming
                showAllFeeds();
            }

            // show the drawer if the user hasn't opened it themselves yet.
            if (!mIsTabletLayout) {
                SharedPreferences prefs = getSharedPreferences("preferences", 0);
                if (!prefs.getBoolean("userUnderstandsDrawer", false)) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            }

        }

    }




    public void reloadTags() {
        //todo: move to navigation fragment
//        mLoaderManager.restartLoader(TAG_LOADER_ID, null, this);
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


//    private ThothFragmentInterface getCurrentFragment()
//    {
//        try {
//            ThothFragmentInterface iface = (ThothFragmentInterface)mFragmentManager.findFragmentById(R.id.content_frame);
//            return iface;
//        } catch (ClassCastException e) {  }
//        return null;
//    }

//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        boolean is_open = mDrawerLayout.isDrawerOpen(mDrawerList);
//
//        try {
//            ThothFragmentInterface frag = getCurrentFragment();
//            if (frag != null) {
//                frag.onPrepareOptionsMenu(menu, is_open);
//            }
//        } catch (java.lang.ClassCastException e) {
//        }
//
//        return super.onPrepareOptionsMenu(menu);
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (mDrawerToggle.onOptionsItemSelected(item)) {
//            return true;
//        }
//
//        int itemId = item.getItemId();
//        switch (itemId) {
//        case R.id.action_about:
//            showAboutDialog();
//            return true;
//        case R.id.action_subscribe:
//            showSubscribe(null);
//            return true;
//        }
//
//        ThothFragmentInterface frag = getCurrentFragment();
//        if (frag != null && frag.onOptionsItemSelected(item)) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }



    /*
     * ActionBarDrawerToggle needs these
     */

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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }





    public void showSubscribe(String url)
    {
//        SubscribeFragment frag = new SubscribeFragment();
//        frag.setUrl(url);
//        FragmentTransaction trans = mFragmentManager.beginTransaction();
//        trans.replace(R.id.content_frame, frag, "current_fragment").addToBackStack("Subscribe");
//        trans.commit();
//        invalidateOptionsMenu();
//        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }


    public void showImport(Uri uri)
    {
//        ImportFragment frag = new ImportFragment();
//        frag.setZipfileUri(uri);
//        FragmentTransaction trans = mFragmentManager.beginTransaction();
//        trans.replace(R.id.content_frame, frag, "current_fragment").addToBackStack("Import");
//        trans.commit();
//        invalidateOptionsMenu();
//        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void showManageFeeds()
    {
//        mDrawerLayout.closeDrawers();
//        ManageFragment frag = new ManageFragment();
//        FragmentTransaction trans = mFragmentManager.beginTransaction();
//        trans.replace(R.id.content_frame, frag, "current_fragment").addToBackStack("Manage");
//        trans.commit();
//        invalidateOptionsMenu();
    }

    private void showAboutDialog() {
        AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
        aboutDialogFragment.show(getSupportFragmentManager(), "About");
    }

    public void showAllFeeds()
    {
        HomeFragment frag = HomeFragment.newInstance();
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.navigation_frame, mNavigationFragment);
        trans.replace(R.id.list_frame, frag, "AllFeeds");
        trans.commit();
        mActionBar.setTitle(R.string.all_feeds);
    }

    public void pushArticleList(long tag_id, long feed_id, int scroll_position, int scroll_offset)
    {
        // only ever put one ArticleList on the stack
        ArticleListFragment frag = (ArticleListFragment)mFragmentManager.findFragmentByTag("ArticleList");
        if (frag == null) {
            FragmentTransaction trans = mFragmentManager.beginTransaction();
            frag = ArticleListFragment.newInstance(tag_id, feed_id);
            trans.replace(R.id.list_frame, frag, "ArticleList");
            trans.addToBackStack("ArticleList");
            trans.commit();
        } else {
            frag.setTagFeed(tag_id, feed_id);
        }

        invalidateOptionsMenu();
    }

    public void pushArticleDetail(long article_id, long tag_id, long feed_id)
    {
        ArticleFragment frag = ArticleFragment.newInstance(article_id, tag_id, feed_id);
        FragmentTransaction trans = mFragmentManager.beginTransaction();

        if (mIsTabletLayout) {
            ArticleListFragment listFragment = (ArticleListFragment)mFragmentManager.findFragmentById(R.id.list_frame);
            listFragment.setLayoutWidth(400);
            trans.replace(R.id.detail_frame, frag, "ArticleDetail");
            trans.hide(mNavigationFragment);
        } else {
            trans.replace(R.id.list_frame, frag, "ArticleDetail");
        }
        trans.addToBackStack("ArticleDetail");
        trans.commit();

        invalidateOptionsMenu();
    }

    @Override
    public void onBackStackChanged() {
        if (mIsTabletLayout) {
            ArticleListFragment listFragment = (ArticleListFragment)mFragmentManager.findFragmentById(R.id.list_frame);
            Fragment detailFragment = mFragmentManager.findFragmentById(R.id.detail_frame);
            if (detailFragment == null && listFragment != null) {
                listFragment.setLayoutWidth(LinearLayout.LayoutParams.MATCH_PARENT);
            }
        }

    }

}
