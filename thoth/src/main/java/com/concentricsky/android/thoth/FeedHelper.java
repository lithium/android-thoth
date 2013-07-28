package com.concentricsky.android.thoth;

import android.util.Log;
import com.concentricsky.android.thoth.models.Feed;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wiggins on 5/18/13.
 */
public class FeedHelper {

    public static Feed attemptToParseFeed(Feed feed, String data)
   {
       Feed ret = null;
       ret = attemptToParseRss(feed, data);
       if (ret == null || ret.title == null)
           ret = attemptToParseAtom(feed, data);
       return ret;
   }

    public enum RssXmlState {RSSXML_NONE, RSSXML_CHANNEL, RSSXML_ITEM};

    public static Feed attemptToParseRss(Feed feed, String data)
    {
        RssFeedParser parser = new RssFeedParser();
        return parser.parse(feed, data);
    }

    public static Feed attemptToParseAtom(Feed feed, String data)
    {
        AtomFeedParser parser = new AtomFeedParser();
        Log.d("THOTH", data);
        return parser.parse(feed, data);
    }

    public static String[] scanHtmlForFeedUrl(String root, String data)
    {
        Pattern feed_re = Pattern.compile("<link ([^>]*type=\"application/(?:rss|atom)\\+xml\"[^>]*)>");
        Pattern href_re = Pattern.compile("href=\"([^\"]+)\"");
        ArrayList<String> out = new ArrayList<String>();

//        Matcher m;
        boolean found=false;

//        m = rss_re.matcher(data);
//        found = m.find();
//        if (!found) {
//            m = atom_re.matcher(data);
//            found = m.find();
//        }

        Matcher m = feed_re.matcher(data);
//        if (found) {
        while (m.find()) {
            Matcher m2 = href_re.matcher(m.group(1));
            if (m2.find()) {
                String href = m2.group(1);
                if (href.startsWith("http")) {
                    out.add(href);
//                    return href;
                }
                else {
//                return (root.endsWith("/") ? root : root+'/')+href;
                    out.add((root.endsWith("/") ? root : root+'/')+href);
                }
            }
        }
        if (out.size() < 1)
            return null;

        String[] ret = new String[out.size()];
        out.toArray(ret);
        return ret;
    }
}
