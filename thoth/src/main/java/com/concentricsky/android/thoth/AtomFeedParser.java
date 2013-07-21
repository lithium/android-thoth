package com.concentricsky.android.thoth;

import android.util.Log;
import com.concentricsky.android.thoth.models.Article;
import com.concentricsky.android.thoth.models.Feed;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by wiggins on 5/18/13.
 */
public class AtomFeedParser {

    public enum AtomXmlState {NONE, IN_FEED, IN_ENTRY};

    public AtomFeedParser() {
    }

    public Feed parse(Feed feed, String data)
    {
        if (feed == null) {
            feed = new Feed();
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();
            Article article = null;
            String lastText = null;

            AtomXmlState state = AtomXmlState.NONE;
//            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            SimpleDateFormat[] date_formats = {
                new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss'Z'"),
                new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ssZ"),
                new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
                new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSZ"),
            };
            for (SimpleDateFormat sdf : date_formats) {
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            }


            xpp.setInput(new StringReader(new String(data.getBytes("UTF-8"))));
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch(eventType) {
                    case XmlPullParser.START_TAG: {
                        String tag_name = xpp.getName();

                        if (state == AtomXmlState.NONE) {
                            if (tag_name.equals("feed")) {
                                state = AtomXmlState.IN_FEED;
                            }
                        }
                        else
                        if (state == AtomXmlState.IN_FEED) {
                            if (tag_name.equals("entry")) {
                                if (feed.articles == null) {
                                    feed.articles = new ArrayList<Article>();
                                }
                                state = AtomXmlState.IN_ENTRY;
                                article = new Article();
                            }
                        }
                        break;
                    }
                    case XmlPullParser.END_TAG: {
                        String tag_name = xpp.getName();
                        if (state == AtomXmlState.IN_FEED) {
                            if (tag_name.equals("feed")) {
                                state = AtomXmlState.NONE;
                            }
                            else if (tag_name.equals("title")) {
                                feed.title = lastText;
                            }
                            else if (tag_name.equals("subtitle")) {
                                feed.description = lastText;
                            }
                            else if (tag_name.equals("link")) {
                                String rel = xpp.getAttributeValue(null, "rel");
                                if (rel != null) {
                                    if (rel.equals("self")) {
                                        feed.url = xpp.getAttributeValue(null, "href");
                                    } else if (rel.equals("alternate")) {
                                        feed.link = xpp.getAttributeValue(null, "href");
                                    }
                                }
                            }
                        }
                        else if (state == AtomXmlState.IN_ENTRY) {
                            if (tag_name.equals("entry")) {
                                state = AtomXmlState.IN_FEED;
                                if (article != null) {
                                    feed.articles.add(article);
                                    article = null;
                                }
                            }
                            else if (tag_name.equals("title")) {
                                article.title = lastText;
                            }
                            else if (tag_name.equals("link")) {
                                String rel = xpp.getAttributeValue(null, "rel");
                                if (rel != null && rel.equals("alternate")) {
                                    article.link = xpp.getAttributeValue(null, "href");
                                }
                            }
                            else if (tag_name.equals("summary")) {
                                if (article.description == null) { // prefer content:encoded if present already
                                    article.description = lastText;
                                }
                            }
                            else if (tag_name.equals("content")) {
                                article.description = lastText;
                            }
                            else if (tag_name.equals("id")) {
                                article.guid = lastText;
                            }
//                            else if (tag_name.equals("published")) {
//                                if (article.timestamp == null)
//                                    article.timestamp = sdf.parse(lastText, new ParsePosition(0));
//                            }
                            else if (tag_name.equals("updated")) {
                                for (SimpleDateFormat sdf : date_formats) {
                                    Date date = sdf.parse(lastText, new ParsePosition(0));
                                    if (date != null) {
                                        article.timestamp = date;
                                        break;
                                    }
                                    else {
                                        Log.v("foo", "foo");

                                    }
                                }
                            }
                        }
                        break;
                    }
                    case XmlPullParser.TEXT:
                        lastText = xpp.getText();
                        break;
                }
                eventType = xpp.next();
            }
        } catch (IOException e) {
            feed = null;
        } catch (XmlPullParserException e) {
            feed = null;
        }

        return feed;
    }
}
