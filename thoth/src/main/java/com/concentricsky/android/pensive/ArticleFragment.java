package com.concentricsky.android.pensive;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;
import com.codeslap.gist.SimpleCursorLoader;
import com.concentricsky.android.pensive.models.Article;

/**
 * Created by wiggins on 5/23/13.
 */
public class ArticleFragment extends Fragment implements ThothFragmentInterface,
                                                         ViewPager.OnPageChangeListener,
                                                         LoaderManager.LoaderCallbacks<Cursor>
{
    private static final int LOADER_ID_ARTICLE_CURSOR = 2;

    private long mFeedId;
    private long mTagId;
    private long mArticleId;

    private ViewPager mViewPager;
    private ArticlePagerAdapter mAdapter;
    private int mPosition;
    private Cursor mCursor;
    private ShareActionProvider mShareActionProvider;
    private boolean mInitializedHack=false;
    private WebView mWebView;
    private LoaderManager mLoaderManager;
    private boolean mHideRead;


    public static ArticleFragment newInstance(long article_id, long tag_id, long feed_id)
    {
        ArticleFragment fragment = new ArticleFragment();

        Bundle args = new Bundle();
        args.putLong("article_id", article_id);
        args.putLong("tag_id", tag_id);
        args.putLong("feed_id", feed_id);
        fragment.setArguments(args);
        return fragment;
    }


    public ArticleFragment()
    {}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentActivity activity = getActivity();

        mLoaderManager = getLoaderManager();
        SharedPreferences preferences = activity.getSharedPreferences("preferences", 0);
        mHideRead = preferences.getBoolean("hideUnread", false);

        Bundle args = getArguments();
        if (args != null) {
            long article_id = args.getLong("article_id", -1);
            long feed_id = args.getLong("feed_id", -1);
            long tag_id = args.getLong("tag_id", -1);
            setArticle(article_id, tag_id, feed_id);
        }

        if (savedInstanceState != null) {
            mArticleId = savedInstanceState.getLong("article_id");
        }
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

        change_cursor();

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
                Article article = mAdapter.getArticle(mViewPager.getCurrentItem());
                if (article != null) {
                    visit_link(article.link);
                }
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void visit_link(String link)
    {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(link));
        startActivity(i);
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
        change_cursor();
    }


    public void setArticle(long article_id, long tag_id, long feed_id)
    {
        mArticleId = article_id;
        mTagId = tag_id;
        mFeedId = feed_id;

        mLoaderManager.restartLoader(LOADER_ID_ARTICLE_CURSOR, null, this);
    }

    public void setArticle(Cursor cursor, int position) {
        mCursor = cursor;
        mPosition = position;
    }

    private void change_cursor()
    {
        if (mCursor == null)
            return;
        mAdapter.changeCursor(mCursor);

        int pos = mAdapter.getPositionFromId(mArticleId);
        mViewPager.setCurrentItem(pos, true);

        if (!mInitializedHack && mPosition == 0) {
            // HACK: this is needed to fix THOT-37. onPageSelected isnt called for the first page the first time.
            // see: http://stackoverflow.com/questions/16074058/onpageselected-doesnt-work-for-first-page
            mInitializedHack = true;
            onPageSelected(0);
        }

    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int i) {
        Article a = (Article) mAdapter.getArticle(i);
        if (a != null) {
            if (a.unread == 1) {
                a.asyncSave(ThothDatabaseHelper.getInstance().getWritableDatabase());
                ThothMainActivity activity = (ThothMainActivity)getActivity();
                activity.reloadTags();
            }

            mArticleId = a._id;

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

    @Override
    public void onPause() {
        super.onPause();
        if (mWebView != null)
            mWebView.loadUrl("file:///android_asset/stop_playing_video");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new SimpleCursorLoader(getActivity()) {
            @Override
            public Cursor loadInBackground() {
                if (mTagId != -1)
                    return ThothDatabaseHelper.getInstance().getArticleCursorByTag(mTagId, mHideRead);
                return ThothDatabaseHelper.getInstance().getArticleCursor(mFeedId, mHideRead);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        change_cursor();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private class ArticlePagerAdapter extends PagerAdapter
    {
        private Cursor mCursor;

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Article article = getArticle(position);

            LayoutInflater inflater = getActivity().getLayoutInflater();
            ViewGroup page = (ViewGroup)inflater.inflate(R.layout.fragment_articledetail, container, false);

            final ProgressBar progressbar = (ProgressBar) page.findViewById(android.R.id.progress);
            progressbar.setProgress(0);
            progressbar.setVisibility(View.VISIBLE);

            mWebView = (WebView) page.findViewById(R.id.article_web);
            WebSettings settings = mWebView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setPluginState(WebSettings.PluginState.ON);

//            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);

            settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
            settings.setAppCachePath(getActivity().getCacheDir().toString());
            settings.setAppCacheEnabled(true);
            mWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    if (progressbar != null) {
                        progressbar.setProgress(newProgress * 100);
                    }
                }

                @Override
                public void onShowCustomView(View view, CustomViewCallback callback) {
                    super.onShowCustomView(view, callback);
                }

                @Override
                public void onHideCustomView() {
                    super.onHideCustomView();
                }

                @Override
                public View getVideoLoadingProgressView() {
                    return progressbar;
                }
            });
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    progressbar.setVisibility(View.GONE);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.startsWith("http")) {
                        visit_link(url);
                        return true;
                    }
                    return false;
                }
            });
            if (article != null) {
                StringBuilder builder = new StringBuilder("<head>"+
                        "<meta name=\"viewport\" width=\"initial-scale=1, maximum-scale=1, minimum-scale=1, user-scalable=no\"/>"+
                        "<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/css/articledetail.css\" /></head>"+
                        "<body><h1 id=\"pensive-title\"><a href=\"")
                        .append(article.link)
                        .append("\">")
                        .append(article.title)
                        .append("</a></h1>");
                if (article.timestamp != null) {
                    builder.append("<p id=\"pensive-metadata\"><span class=\"feed-name\">")
                            .append(article.feed_title)
                            .append("</span> / <span class=\"timestamp\">")
                            .append(DateUtils.fuzzyTimestamp(getActivity(), article.timestamp.getTime()))
                            .append("</span></p>");
                }
                builder.append("<div id=\"pensive-content\">")
                        .append(article.description)
                        .append("</div></body>");
                mWebView.loadDataWithBaseURL("http://www.youtube.com", builder.toString(), "text/html", "UTF-8", null);
            }
            container.addView(page);


            return page;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
//            super.destroyItem(container, position, object);
            ViewGroup page = (ViewGroup)object;
            container.removeViewInLayout(page);
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
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

        public Article getArticle(int position) {
            if (mCursor.isClosed()) {
                return null;
            }
            Article article = new Article();
            mCursor.moveToPosition(position);
            article.hydrate(mCursor);
            return article;
        }

        public int getPositionFromId(long id) {
            int pos;
            int len = getCount();
            for (pos=0; pos < len; pos++) {
                Article a = getArticle(pos);
                if (a._id == id)
                    return pos;
            }
            return 0;
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong("article_id", mArticleId);
    }
}
