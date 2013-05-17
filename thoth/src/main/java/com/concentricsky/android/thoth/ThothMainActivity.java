package com.concentricsky.android.thoth;

import android.app.*;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
//import com.android.volley.RequestQueue;
//import com.android.volley.toolbox.Volley;

public class ThothMainActivity extends Activity {
    private ActionBar mActionBar;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ThothActionBarDrawerToggle mDrawerToggle;
    private FragmentManager mFragmentManager;
    private ArticleListFragment mArticleListFragment;
    private SubscribeFragment mSubscribeFragment;


//    private RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set up navigation drawer
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerList = (ListView)findViewById(R.id.navigation_drawer);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
//        mDrawerList.setAdapter();
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

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
