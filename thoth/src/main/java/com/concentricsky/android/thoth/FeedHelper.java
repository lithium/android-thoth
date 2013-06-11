package com.concentricsky.android.thoth;

import com.concentricsky.android.thoth.models.Feed;

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
       if (ret == null || ret.link == null)
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
        return parser.parse(feed, data);
    }

    public static String scanHtmlForFeedUrl(String root, String data)
    {
        Pattern rss_re = Pattern.compile("<link (.*type=\"application/rss\\+xml\".*)>");
        Pattern atom_re = Pattern.compile("<link (.*type=\"application/atom\\+xml\".*)>");
        Pattern href_re = Pattern.compile("href=\"([^\"]+)\"");

        Matcher m;
        boolean found=false;

        m = rss_re.matcher(data);
        found = m.find();
        if (!found) {
            m = atom_re.matcher(data);
            found = m.find();
        }

        if (found) {
            Matcher m2 = href_re.matcher(m.group(1));
            if (m2.find()) {
                String href = m2.group(1);
                if (href.startsWith("http")) {
                    return href;
                }
                return (root.endsWith("/") ? root : root+'/')+href;
            }
        }


        return null;
    }
}
