package com.concentricsky.android.thoth;

import android.app.*;
import android.content.Context;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
//import com.android.volley.RequestQueue;
//import com.android.volley.toolbox.Volley;

public class ThothMainActivity extends Activity
                               implements LoaderManager.LoaderCallbacks<Cursor>
{
    private ActionBar mActionBar;
    private DrawerLayout mDrawerLayout;
    private ExpandableListView mDrawerList;
    private ThothActionBarDrawerToggle mDrawerToggle;
    private FragmentManager mFragmentManager;
    private ArticleListFragment mArticleListFragment;
    private SubscribeFragment mSubscribeFragment;
//    private ThothDatabaseHelper mDbHelper;
    private ThothDrawerAdapter mDrawerAdapter;


//    private RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mDbHelper = new ThothDatabaseHelper((Context)this);

        //set up navigation drawer
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerAdapter = new ThothDrawerAdapter();
        mDrawerList = (ExpandableListView)findViewById(R.id.navigation_drawer);
        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        getLoaderManager().initLoader(0, null, this);

        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);

        mDrawerToggle = new ThothActionBarDrawerToggle();
        mDrawerLayout.setDrawerListener(mDrawerToggle);


        //set up fragments
        mFragmentManager = getFragmentManager();
        mArticleListFragment = new ArticleListFragment();
        mSubscribeFragment = null; //create on demand



       if (savedInstanceState == null) {
           //initial startup
           showArticleList();

           //debug tables
//           ThothDatabaseHelper mDbHelper = ThothDatabaseHelper.getInstance();
//           long[] tags = {mDbHelper.addTag("hobby")};
//           mDbHelper.addFeed("http://diydrones.com/profiles/blog/feed?user=3m9btzxk9mkpg&amp;xn_auth=no", "Joshua Ott's Posts - DIY Drones", tags);
//           mDbHelper.addFeed("http://www.fleshpilot.com/?feed=rss2", "Flesh Pilot", tags);
       }



        //http://www.youtube.com/watch?v=yhv8l9F44qo#t=14m36
//        mRequestQueue = Volley.newRequestQueue(this);


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
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new ThothNavigationLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mDrawerAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mDrawerAdapter.changeCursor(null);
    }


    /*
     * Private Classes
     */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
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
                    android.R.layout.simple_expandable_list_item_2,
                    new String[]{"_id", "title"}, // groupFrom,
                    new int[]{android.R.id.text1, android.R.id.text2}, // groupTo,
                    android.R.layout.simple_expandable_list_item_2,
                    new String[]{"_id", "title"}, // childFrom,
                    new int[]{android.R.id.text1, android.R.id.text2}); // childTo);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor cursor) {
            int tag_id = cursor.getInt(cursor.getColumnIndex("_id"));
            return ThothDatabaseHelper.getInstance().getFeedCursor(tag_id);
        }

//        @Override
//        protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
//            super.bindChildView(view, context, cursor, isLastChild);
////            Button b = (Button)view.findViewById(android.R.id.button1);
//        }
    }

    /*
     * Private Methods
     */

    public void showArticleList()
    {
        mFragmentManager.beginTransaction().replace(R.id.content_frame, mArticleListFragment, "current_fragment").commit();
        invalidateOptionsMenu();
    }

    public void showSubscribe()
    {
        if (mSubscribeFragment == null) {
            mSubscribeFragment = new SubscribeFragment();
        }
        FragmentTransaction trans = mFragmentManager.beginTransaction();
        trans.replace(R.id.content_frame, mSubscribeFragment, "current_fragment").addToBackStack(null);
        trans.commit();
        invalidateOptionsMenu();
    }

}
