package com.concentricsky.android.thoth;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.webkit.WebView;
import android.widget.TextView;
import com.codeslap.gist.SimpleCursorLoader;
import com.concentricsky.android.thoth.com.concentricsky.android.thoth.models.Article;

/**
 * Created by wiggins on 5/23/13.
 */
public class ArticleFragment extends Fragment implements ThothFragmentInterface, ViewPager.OnPageChangeListener {
    private static final int CURSOR_LOADER_ID = 1;

    private long mFeedId;
    private long mArticleId;

//    private RequestQueue mRequestQueue;
    private LoaderManager mLoaderManager;
//    private Article mArticle;
    private ViewPager mViewPager;
    private ArticlePagerAdapter mAdapter;

//    private WebView mBodyWeb;
//    private TextView mTitleText;

    public ArticleFragment()
    {}

    @Override
    public void onDetach() {
        super.onDetach();

//        mRequestQueue.stop();
//        mRequestQueue = null;
        mLoaderManager.destroyLoader(CURSOR_LOADER_ID);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

//        mRequestQueue = Volley.newRequestQueue(activity);
        mLoaderManager = activity.getLoaderManager();
//        load_article();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_article, container, false);

        mAdapter = new ArticlePagerAdapter();
        mViewPager = (ViewPager)root;
//        mViewPager.setAdapter();
        mViewPager.setOnPageChangeListener(this);

//        mTitleText = (TextView)root.findViewById(R.id.article_title);
//        mBodyWeb = (WebView)root.findViewById(R.id.article_web);

        return root;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                return true;
            case R.id.action_visitpage:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {
        menu.findItem(R.id.action_share).setVisible(!drawer_open);
        menu.findItem(R.id.action_visitpage).setVisible(!drawer_open);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
    }

//    public void setArticle(long article_id) {
//        if (article_id == mArticleId)
//            return;
//        mArticleId = article_id;
//        mArticle = null;
//        if (mLoaderManager != null) {
//            mLoaderManager.destroyLoader(ARTICLE_LOADER_ID);
//        }
//        load_article();
//    }
//
//    private void load_article() {
//        if (mLoaderManager == null)
//            return;
//        mLoaderManager.initLoader(ARTICLE_LOADER_ID, null, new ArticleLoader());
//    }
//
//    private void update_article() {
//        if (mArticle == null)
//            return;
//
//        mTitleText.setText(mArticle.title);
//        mBodyWeb.loadData(mArticle.description, "text/html", "UTF-8");
//    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int i) {

    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    private class ArticleLoader implements LoaderManager.LoaderCallbacks<Cursor>
    {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new SimpleCursorLoader(getActivity()) {
                @Override
                public Cursor loadInBackground() {
//                    return Article.load(ThothDatabaseHelper.getInstance().getReadableDatabase(), mArticleId);
                    return ThothDatabaseHelper.getInstance().getArticleCursor(mFeedId);
                }
            };
        }
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            mAdapter.changeCursor(cursor);
        }
        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.changeCursor(null);
        }
    }


    private class ArticlePagerAdapter extends FragmentPagerAdapter {
        private ArticlePagerAdapter() {
            super(getActivity().getFragmentManager());
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
//            return ArticleDetailFragment.newInstance(position);
            return null;
        }

        @Override
        public int getCount() {
            return 0;
        }

        public void changeCursor(Cursor cursor) {

        }
    }
}
