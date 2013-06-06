package com.concentricsky.android.thoth;

import android.util.Log;
import com.concentricsky.android.thoth.models.Feed;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Created by wiggins on 6/3/13.
 */
public class OpmlParser {

    private static final String TAG = "ThothOpmlParser";

    public static ArrayList<Feed> parse(String data)
    {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(data));
            int eventType = xpp.getEventType();

            String lastText = null;
            ArrayList<Feed> feeds = new ArrayList<Feed>();
            Feed feed;
            String tag=null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch(eventType) {
                    case XmlPullParser.START_TAG: {
                        String tag_name = xpp.getName();
                        String feed_url = xpp.getAttributeValue(null, "xmlUrl");
                        if (feed_url == null) { // start of a tag
                            tag = xpp.getAttributeValue(null, "title");
                        }
                        else {
                            feed = new Feed();
                            feed.url = feed_url;
                            feed.link = xpp.getAttributeValue(null, "htmlUrl");
                            feed.title = xpp.getAttributeValue(null, "title");
                            if (tag != null) {
                                feed.tags = new String[] {tag};
                            }
                            feeds.add(feed);
                        }
                        break;
                    }
                    case XmlPullParser.END_TAG: {
                        String tag_name = xpp.getName();
                        break;
                    }
                    case XmlPullParser.TEXT: {
                        lastText = xpp.getText();
                        break;
                    }
                }
                eventType = xpp.next();
            }
            return feeds;

        } catch (IOException e) {
            Log.d(TAG, "Couldn't parse OPML");
        } catch (XmlPullParserException e) {
            Log.d(TAG, "Couldn't parse OPML");
        }

        return null;
    }
}
