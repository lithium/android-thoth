package com.concentricsky.android.thoth;

import android.app.Fragment;
import android.os.Bundle;
import android.view.*;

/**
 * Created by wiggins on 5/17/13.
 */
public class SubscribeFragment extends Fragment implements ThothFragmentInterface {
    public SubscribeFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_subscribe, container, false);

        return root;
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {

//        menu.findItem(R.id.action_share).setVisible(!drawer_open);
//        menu.findItem(R.id.action_visitpage).setVisible(!drawer_open);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }
}
