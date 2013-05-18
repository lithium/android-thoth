package com.concentricsky.android.thoth;

import com.concentricsky.android.thoth.com.concentricsky.android.thoth.models.Feed;

/**
 * Created by wiggins on 5/18/13.
 */
public class FeedHelper {
   public static Feed attemptToParseFeed(String data)
   {
       Feed ret = null;
       ret = attemptToParseRss(data);
       if (ret == null)
           ret = attemptToParseAtom(data);
       return ret;
   }

    public static Feed attemptToParseRss(String data)
    {


        return null;
    }

    public static Feed attemptToParseAtom(String data)
    {

        return null;
    }

    public static String scanHtmlForFeedUrl(String data)
    {

        return null;
    }
}
