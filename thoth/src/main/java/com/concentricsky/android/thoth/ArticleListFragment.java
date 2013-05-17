package com.concentricsky.android.thoth;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.*;

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
        menu.findItem(R.id.action_subscribe).setVisible(!drawer_open);
        menu.findItem(R.id.action_refresh).setVisible(!drawer_open);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
       switch (item.getItemId()) {
           case R.id.action_subscribe:
               ThothMainActivity act = (ThothMainActivity)getActivity();
               act.showSubscribe();
               return true;
       }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
    }
}
