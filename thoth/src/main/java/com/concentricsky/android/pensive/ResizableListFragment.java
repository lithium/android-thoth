package com.concentricsky.android.pensive;

import android.support.v4.app.ListFragment;
import android.widget.LinearLayout;
import android.widget.ListView;

/**
 * Created by wiggins on 9/27/13.
 */
public class ResizableListFragment extends ListFragment
{
    public void setLayoutWidth(int width) {
        ListView listView = getListView();
        if (listView != null) {
            listView.setLayoutParams(new LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.MATCH_PARENT));
        }

    }
}
