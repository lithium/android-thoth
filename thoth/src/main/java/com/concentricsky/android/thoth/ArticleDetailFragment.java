package com.concentricsky.android.thoth;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import com.concentricsky.android.thoth.com.concentricsky.android.thoth.models.Article;

/**
* Created by wiggins on 5/23/13.
*/
class ArticleDetailFragment extends Fragment {
    private TextView mTitleText;
    private WebView mBodyWeb;
    private Article mArticle;

    static ArticleDetailFragment newInstance(Article article) {
        ArticleDetailFragment f = new ArticleDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("article", article);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_articledetail, container, false);
        mTitleText = (TextView)root.findViewById(R.id.article_title);
        mBodyWeb = (WebView)root.findViewById(R.id.article_web);
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
            mTitleText.setText(mArticle.title);
            mBodyWeb.loadData(mArticle.description, "text/html", "UTF-8");
        }
    }

}
