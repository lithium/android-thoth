package com.concentricsky.android.pensive;

/**
 * Created by wiggins on 9/8/13.
 */
public interface ThothNavigationDrawerListener {
    public void onNavigationAllFeeds();
    public void onNavigationClickTag(long tag_id);
    public void onNavigationClickFeed(long feed_id);
}
