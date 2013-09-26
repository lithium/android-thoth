package com.concentricsky.android.pensive;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * Created by wiggins on 9/8/13.
 */
public class HomeFragment extends ArticleListFragment
{

    public static HomeFragment newInstance()
    {
        HomeFragment fragment = new HomeFragment();

        Bundle args = new Bundle();
        args.putLong("feed_id", 0);
        args.putLong("tag_id", -1);
        fragment.setArguments(args);

        return fragment;
    }

}
