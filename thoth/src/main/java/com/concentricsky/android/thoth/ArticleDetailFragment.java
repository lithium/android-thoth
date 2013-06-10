package com.concentricsky.android.thoth;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import com.concentricsky.android.thoth.models.Article;

/**
* Created by wiggins on 5/23/13.
*/
public class ArticleDetailFragment extends Fragment {
    private WebView mBodyWeb;
    private TextView mTitleText;
    private TextView mSubtitleText;

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
//        mSubtitleText = (TextView) root.findViewById(R.id.article_title);

        mBodyWeb = (WebView)root.findViewById(R.id.article_web);
        WebSettings settings = mBodyWeb.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setPluginState(WebSettings.PluginState.ON);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
//        settings.setLoadWithOverviewMode(true);
//        settings.setUseWideViewPort(true);
//        mBodyWeb.setInitialScale(100);
        load_article();
        return root;
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
                    .append("</h1>");
            if (mArticle.timestamp != null) {
                builder.append("<div id=\"thoth-timestamp\">")
                       .append(DateUtils.fuzzyTimestamp(getActivity(), mArticle.timestamp.getTime()))
                       .append("</div>");
            }
            builder.append("<div id=\"thoth-content\">")
                   .append(mArticle.description)
                   .append("</div></body>");
            mBodyWeb.loadDataWithBaseURL("file:///android_asset/", builder.toString(), "text/html", "UTF-8", null);

//            mTitleText.setText(mArticle.title);
//            mSubtitleText.setText(DateUtils.fuzzyTimestamp(getActivity(), mArticle.timestamp.getTime()));
        }
    }

}
