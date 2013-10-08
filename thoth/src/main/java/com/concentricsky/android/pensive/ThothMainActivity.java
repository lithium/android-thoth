package com.concentricsky.android.pensive;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.*;
import android.widget.*;
import com.concentricsky.android.pensive.models.Feed;


public class ThothMainActivity extends FragmentActivity
                               implements  ArticleListFragment.ArticleSelectedListener,
                                           ArticleFragment.ArticleSwipedListener,
                                           FragmentManager.OnBackStackChangedListener,
                                           NavigationFragment.NavigationListener
{
    private ActionBar mActionBar;
    private DrawerLayout mDrawerLayout;
    private ThothActionBarDrawerToggle mDrawerToggle;

    private FragmentManager mFragmentManager;

    private boolean mIsTabletLayout=false;
    private NavigationFragment mNavigationFragment;
    private int mCurrentOrientation=-1;

    @Override
    public void onArticleSelected(long article_id, long tag_id, long feed_id){
        Fragment frag = getSupportFragmentManager().findFragmentByTag("ArticleDetail");
        if (frag != null) { //tablet layout
            try {
                ArticleFragment articleFragment = (ArticleFragment)frag;
                articleFragment.setArticle(article_id, tag_id, feed_id);
            } catch (ClassCastException e) {}
        } else {
            showArticleDetail(article_id, tag_id, feed_id);
        }
    }

    @Override
    public void onArticleSwiped(int position, long id) {
        try {
            ArticleListFragment frag = (ArticleListFragment)mFragmentManager.findFragmentById(R.id.list_frame);
            frag.setHighlightedArticle(position, id);
        } catch (ClassCastException e) {

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
        setProgressBarIndeterminateVisibility(false);


        mActionBar = getActionBar();

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            mIsTabletLayout = true;
        } else {
            mIsTabletLayout = false;

            //set up action bar home drawer toggle
            mDrawerToggle = new ThothActionBarDrawerToggle(this);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeButtonEnabled(true);
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



        mCurrentOrientation = getResources().getConfiguration().orientation;


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
        mNavigationFragment.reload();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.thoth_main, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int itemId = item.getItemId();
        switch (itemId) {
        case R.id.action_about:
            showAboutDialog();
            return true;
        case R.id.action_subscribe:
            showSubscribe(null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFeedsDiscovered(boolean feeds_are_present) {

    }

    @Override
    public void onTagClicked(long tag_id) {
        close_detail_if_present();
        showArticleList(tag_id, -1, 0, 0);

        //TODO: pass title in
        //                        Cursor c = getCursor();
        //                        c.moveToPosition(groupPosition);
        //                        String title = String.valueOf(c.getString(c.getColumnIndexOrThrow("title")));
        //                        getActionBar().setTitle( title );
        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawers();
    }

    @Override
    public void onFeedClicked(long feed_id) {
        //TODO: pass title in
//                    getActionBar().setTitle( title );
        close_detail_if_present();
        showArticleList(-1, feed_id, 0, 0);
        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawers();
    }

    @Override
    public void onAllFeedsClicked() {
        mFragmentManager.popBackStack(null,FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawers();
    }

    @Override
    public void onManageFeedsClicked() {
        showManageFeeds();
        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawers();
    }
    private void close_detail_if_present()
    {
        Fragment frag = mFragmentManager.findFragmentByTag("ArticleDetail");
        if (frag != null)
            mFragmentManager.popBackStack();
    }

    public void setDisplayHomeAsUpEnabled(boolean enabled) {
        if (mIsTabletLayout) {
            ActionBar actionBar = getActionBar();
            if (actionBar != null)
                actionBar.setDisplayHomeAsUpEnabled(enabled);
        }
    }



    /*
     * ActionBarDrawerToggle needs these
     */

    private class ThothActionBarDrawerToggle extends ActionBarDrawerToggle {
        public ThothActionBarDrawerToggle(Activity activity) {
            super(activity, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
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
        mCurrentOrientation = newConfig.orientation;
        if (mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }





    public void showSubscribe(String url)
    {
        SubscribeFragment frag = new SubscribeFragment();
        frag.setUrl(url);
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.list_frame, frag, "current_fragment").addToBackStack("Subscribe");
        trans.commit();
    }


    public void showImport(Uri uri)
    {
        ImportFragment frag = new ImportFragment();
        frag.setZipfileUri(uri);
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.list_frame, frag, "current_fragment").addToBackStack("Import");
        trans.commit();
    }

    private void showAboutDialog() {
        AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
        aboutDialogFragment.show(getSupportFragmentManager(), "About");
    }

    public void showAllFeeds()
    {
        ArticleListFragment frag = ArticleListFragment.newInstance(-1,0); // All Feeds
        if (mIsTabletLayout && mCurrentOrientation != Configuration.ORIENTATION_PORTRAIT) {
            frag.setShowHighlighted(true);
        }
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.navigation_frame, mNavigationFragment);
        trans.replace(R.id.list_frame, frag, "AllFeeds");
        trans.commit();
        mActionBar.setTitle(R.string.all_feeds);
    }


    public void showArticleList(long tag_id, long feed_id, int scroll_position, int scroll_offset)
    {
        // only ever put one ArticleList on the stack
        ArticleListFragment frag = (ArticleListFragment)mFragmentManager.findFragmentByTag("ArticleList");
        if (frag == null) {
            FragmentTransaction trans = mFragmentManager.beginTransaction();
            frag = ArticleListFragment.newInstance(tag_id, feed_id);
            if (mIsTabletLayout && mCurrentOrientation != Configuration.ORIENTATION_PORTRAIT) {
                frag.setShowHighlighted(true);
            }
            trans.replace(R.id.list_frame, frag, "ArticleList");
            trans.addToBackStack("ArticleList");
            trans.commit();
        } else {
            frag.setTagFeed(tag_id, feed_id);
        }

    }

    public void showArticleDetail(long article_id, long tag_id, long feed_id)
    {
        ArticleFragment frag = ArticleFragment.newInstance(article_id, tag_id, feed_id);
        show_detail_frame(frag, "ArticleDetail");
    }

    public void showManageFeeds()
    {
        ManageFragment frag = (ManageFragment)mFragmentManager.findFragmentByTag("ManageFeeds");
        if (frag != null)
            return;
        frag = new ManageFragment();

        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.list_frame, frag, "ManageFeeds");
        trans.addToBackStack("ManageFeeds");
        trans.commit();
    }

    public void showEditFeed(Feed feed)
    {
        EditFeedFragment frag = (EditFeedFragment)mFragmentManager.findFragmentByTag("EditFeed");
        if (frag == null) {
            frag = new EditFeedFragment(feed);
            show_detail_frame(frag, "EditFeed");
        } else {
            //todo: implement for tablet
//            frag.setFeed(feed);
        }
    }


    private void show_detail_frame(Fragment frag, String tag)
    {
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        if (mIsTabletLayout) {
            ResizableListFragment listFragment = (ResizableListFragment)mFragmentManager.findFragmentById(R.id.list_frame);
            if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                trans.replace(R.id.detail_frame, frag, tag);
                trans.hide(mNavigationFragment);
                trans.hide(listFragment);
            } else {
                listFragment.setLayoutWidth(400);
                trans.replace(R.id.detail_frame, frag, tag);
                trans.hide(mNavigationFragment);
            }
        } else {
            trans.replace(R.id.list_frame, frag, tag);
        }
        trans.addToBackStack(tag);
        trans.commit();
    }

    @Override
    public void onBackStackChanged() {
        if (mIsTabletLayout) {
            try {
                ResizableListFragment listFragment = (ResizableListFragment)mFragmentManager.findFragmentById(R.id.list_frame);
                Fragment detailFragment = mFragmentManager.findFragmentById(R.id.detail_frame);
                if (detailFragment == null && listFragment != null) {
                    listFragment.setLayoutWidth(LinearLayout.LayoutParams.MATCH_PARENT);
                }
            } catch (ClassCastException e) {

            }
        }

    }

}
