package com.concentricsky.android.thoth;

import android.content.Context;
import android.content.Intent;
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
        load_cursor();
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
        Article a = (Article) mAdapter.getArticle(i);
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

            WebView webview = (WebView) page.findViewById(R.id.article_web);
            WebSettings settings = webview.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setPluginState(WebSettings.PluginState.ON);
            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
            settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
            settings.setAppCachePath(getActivity().getCacheDir().toString());
            settings.setAppCacheEnabled(true);
            webview.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    if (progressbar != null) {
                        progressbar.setProgress(newProgress * 100);
                    }

                }
            });
            webview.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    progressbar.setVisibility(View.GONE);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    visit_link(url);
                    return true;
//                    return super.shouldOverrideUrlLoading(view, url);
                }
            });
            StringBuilder builder = new StringBuilder("<head><link rel=\"stylesheet\" type=\"text/css\" href=\"css/articledetail.css\" /></head>"+
                    "<body><h1 id=\"thoth-title\"><a href=\"")
                    .append(article.link)
                    .append("\">")
                    .append(article.title)
                    .append("</a></h1>");
            if (article.timestamp != null) {
                builder.append("<p id=\"thoth-timestamp\"><span>")
                        .append(DateUtils.fuzzyTimestamp(getActivity(), article.timestamp.getTime()))
                        .append("</span></p>");
            }
            builder.append("<div id=\"thoth-content\">")
                    .append(article.description)
                    .append("</div></body>");
            webview.loadDataWithBaseURL("file:///android_asset/", builder.toString(), "text/html", "UTF-8", null);

            container.addView(page);


            return page;
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
            Article article = new Article();
            mCursor.moveToPosition(position);
            article.hydrate(mCursor);
            return article;
        }

    }
}
