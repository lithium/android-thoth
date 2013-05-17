package com.concentricsky.android.thoth;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by wiggins on 5/17/13.
 */
public class ArticleListFragment extends ListFragment implements ThothFragmentInterface {
    public ArticleListFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_articlelist, container, false);

        return root;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {
        menu.findItem(R.id.action_share).setVisible(!drawer_open);
        menu.findItem(R.id.action_visitpage).setVisible(!drawer_open);
    }
}
