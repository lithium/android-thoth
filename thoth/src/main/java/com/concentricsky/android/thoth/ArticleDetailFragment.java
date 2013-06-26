package com.concentricsky.android.thoth;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.concentricsky.android.thoth.models.Article;

/**
* Created by wiggins on 5/23/13.
*/
public class ArticleDetailFragment extends Fragment {
    private WebView mBodyWeb;
//    private TextView mTitleText;
//    private TextView mSubtitleText;
    private ProgressBar mProgress;

    public ArticleDetailFragment() {
    }

    private Article mArticle;

    static ArticleDetailFragment newInstance(Article article) {
        ArticleDetailFragment f = new ArticleDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("article", article);
        f.setArguments(args);
        f.setArticle(article);
        return f;
    }

    void setArticle(Article article) {
        this.mArticle = article;
    }

    Article getArticle() {
        return mArticle;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_articledetail, container, false);

//        mTitleText = (TextView) root.findViewById(R.id.article_title);
//        mSubtitleText = (ATextView) root.findViewById(R.id.article_title);
        mProgress = (ProgressBar)root.findViewById(android.R.id.progress);

        mBodyWeb = (WebView)root.findViewById(R.id.article_web);
        WebSettings settings = mBodyWeb.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setPluginState(WebSettings.PluginState.ON);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setAppCachePath(getActivity().getCacheDir().toString());
        settings.setAppCacheEnabled(true);
        mBodyWeb.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (mProgress != null) {
                    mProgress.setProgress(newProgress*100);
                }

            }
        });
        mBodyWeb.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mProgress.setVisibility(View.GONE);
            }
        });
//        settings.setLoadWithOverviewMode(true);
//        settings.setUseWideViewPort(true);
//        mBodyWeb.setInitialScale(100);
        return root;
    }


    @Override
    public void onResume() {
        super.onResume();
        load_article();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mArticle = getArguments() != null ? (Article)getArguments().getSerializable("article") : null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void load_article()
    {
        if (mArticle != null) {
            StringBuilder builder = new StringBuilder("<head><link rel=\"stylesheet\" type=\"text/css\" href=\"css/articledetail.css\" /></head>"+
                    "<body><h1 id=\"thoth-title\"><a href=\"")
                    .append(mArticle.link)
                    .append("\">")
                    .append(mArticle.title)
                    .append("</a></h1>");
            if (mArticle.timestamp != null) {
                builder.append("<p id=\"thoth-timestamp\"><span>")
                       .append(DateUtils.fuzzyTimestamp(getActivity(), mArticle.timestamp.getTime()))
                       .append("</span></p>");
            }
            builder.append("<div id=\"thoth-content\">")
                   .append(mArticle.description)
                   .append("</div></body>");
            mProgress.setProgress(0);
            mProgress.setVisibility(View.VISIBLE);
            mBodyWeb.loadDataWithBaseURL("file:///android_asset/", builder.toString(), "text/html", "UTF-8", null);

//            mTitleText.setText(mArticle.title);
//            mSubtitleText.setText(DateUtils.fuzzyTimestamp(getActivity(), mArticle.timestamp.getTime()));
        }
    }

}
