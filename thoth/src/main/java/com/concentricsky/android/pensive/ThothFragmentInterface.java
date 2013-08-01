package com.concentricsky.android.pensive;

import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by wiggins on 5/17/13.
 */
public interface ThothFragmentInterface {

//    public Fragment getFragment();
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open);
    public boolean onOptionsItemSelected(MenuItem item);
}
