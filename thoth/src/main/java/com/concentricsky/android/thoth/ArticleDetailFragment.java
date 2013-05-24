package com.concentricsky.android.thoth;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

/**
* Created by wiggins on 5/23/13.
*/
class ArticleDetailFragment extends Fragment {
    private long mArticleId;
    private TextView mTitleText;
    private WebView mBodyWeb;

    static ArticleDetailFragment newInstance(int article_id) {
        ArticleDetailFragment f = new ArticleDetailFragment();
        Bundle args = new Bundle();
        args.putInt("article_id", article_id);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_articledetail, container, false);
        mTitleText = (TextView)root.findViewById(R.id.article_title);
        mBodyWeb = (WebView)root.findViewById(R.id.article_web);
        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mArticleId = getArguments() != null ? getArguments().getInt("article_id") : 1;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void load_article()
    {
    }

}
