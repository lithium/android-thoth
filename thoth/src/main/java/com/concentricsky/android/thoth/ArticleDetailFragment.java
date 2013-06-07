package com.concentricsky.android.thoth;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.concentricsky.android.thoth.models.Article;

/**
* Created by wiggins on 5/23/13.
*/
public class ArticleDetailFragment extends Fragment {
    private WebView mBodyWeb;

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
        mBodyWeb = (WebView)root.findViewById(R.id.article_web);
        WebSettings settings = mBodyWeb.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setPluginState(WebSettings.PluginState.ON);
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
            mBodyWeb.loadData("<head></head><body><h1 id=\"thoth-title\">"+mArticle.title+"</h1>"+mArticle.description+"</body>", "text/html", "UTF-8");
        }
    }

}
