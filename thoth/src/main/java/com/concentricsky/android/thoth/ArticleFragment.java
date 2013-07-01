package com.concentricsky.android.thoth;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.ShareActionProvider;
import com.codeslap.gist.SimpleCursorLoader;
import com.concentricsky.android.thoth.models.Article;

/**
 * Created by wiggins on 5/23/13.
 */
public class ArticleFragment extends Fragment implements ThothFragmentInterface, ViewPager.OnPageChangeListener {
    private static final int CURSOR_LOADER_ID = 1;

    private long mFeedId;
    private long mTagId;
    private long mArticleId;

    private ViewPager mViewPager;
    private ArticlePagerAdapter mAdapter;
    private int mPosition;
    private Cursor mCursor;
    private ShareActionProvider mShareActionProvider;


    public ArticleFragment()
    {}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentActivity activity = getActivity();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_article, container, false);

        mAdapter = new ArticlePagerAdapter();
        mViewPager = (ViewPager)root;
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(this);

        load_cursor();

        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem item = menu.findItem(R.id.action_share);
        mShareActionProvider = new ShareActionProvider(getActivity());
        item.setActionProvider(mShareActionProvider);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_visitpage: {
                Article article = getArticle();
                if (article != null) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(article.link));
                    startActivity(i);
                }
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {
        MenuItem share =  menu.findItem(R.id.action_share);
        share.setVisible(!drawer_open);
        mShareActionProvider = (ShareActionProvider)share.getActionProvider();
        menu.findItem(R.id.action_visitpage).setVisible(!drawer_open);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
        load_cursor();
    }

    public Article getArticle()
    {
        ArticleDetailFragment frag = (ArticleDetailFragment) mAdapter.getItem(mViewPager.getCurrentItem());
        if (frag != null)
            return frag.getArticle();
        return null;
    }

    public void setArticle(Cursor cursor, int position) {
        mCursor = cursor;
        mPosition = position;
    }

    private void load_cursor()
    {
        if (mCursor == null)
            return;
        mAdapter.changeCursor(mCursor);
        mViewPager.setCurrentItem(mPosition, true);

    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int i) {
        ArticleDetailFragment frag = (ArticleDetailFragment) mAdapter.getItem(i);
        Article a = frag.getArticle();
        if (a != null) {
            if (a.unread == 1)
                a.asyncSave(ThothDatabaseHelper.getInstance().getWritableDatabase());

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, a.link);
            intent.setType("text/plain");
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(intent);
            }
        }

    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }


    private class ArticlePagerAdapter extends FragmentStatePagerAdapter
    {
        private Cursor mCursor;

        private ArticlePagerAdapter() {
            super(getActivity().getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            if (mCursor == null) {
                return null;
            }
            mCursor.moveToPosition(position);
            Article article = new Article();
            article.hydrate(mCursor);
            return ArticleDetailFragment.newInstance(article);
        }

        @Override
        public int getCount() {
            if (mCursor == null)
                return 0;
            return mCursor.getCount();
        }

        public void changeCursor(Cursor cursor) {
            mCursor = cursor;
            notifyDataSetChanged();
        }

    }
}
