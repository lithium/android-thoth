package com.concentricsky.android.thoth;

import com.concentricsky.android.thoth.com.concentricsky.android.thoth.models.Article;
import com.concentricsky.android.thoth.com.concentricsky.android.thoth.models.Feed;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Created by wiggins on 5/18/13.
 */
public class RssFeedParser {

    public enum RssXmlState {RSSXML_NONE, RSSXML_CHANNEL, RSSXML_ITEM};

    public RssFeedParser() {
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

            RssXmlState state = RssXmlState.RSSXML_NONE;

            xpp.setInput(new StringReader(data));
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch(eventType) {
                    case XmlPullParser.START_TAG: {
                        String tag_name = xpp.getName();

                        if (state == RssXmlState.RSSXML_NONE) {
                            if (tag_name.equals("channel")) {
                                state = RssXmlState.RSSXML_CHANNEL;
                            }
                        }
                        else
                        if (state == RssXmlState.RSSXML_CHANNEL) {
                            if (tag_name.equals("item")) {
                                if (feed.articles == null) {
                                    feed.articles = new ArrayList<Article>();
                                }
                                state = RssXmlState.RSSXML_ITEM;
                                article = new Article();
                            }
                        }
                        break;
                    }
                    case XmlPullParser.END_TAG: {
                        String tag_name = xpp.getName();
                        if (state == RssXmlState.RSSXML_CHANNEL) {
                            if (tag_name.equals("channel")) {
                                state = RssXmlState.RSSXML_NONE;
                            }
                            else if (tag_name.equals("title")) {
                                feed.title = lastText;
                            }
                            else if (tag_name.equals("description")) {
                                feed.description = lastText;
                            }
                            else if (tag_name.equals("link")) {
                                feed.link = lastText;
                            }
                        }
                        else if (state == RssXmlState.RSSXML_ITEM) {
                            if (tag_name.equals("item")) {
                                state = RssXmlState.RSSXML_CHANNEL;
                                if (article != null) {
                                    feed.articles.add(article);
                                    article = null;
                                }
                            }
                            else if (tag_name.equals("title")) {
                                article.title = lastText;
                            }
                            else if (tag_name.equals("link")) {
                                article.link = lastText;
                            }
                            else if (tag_name.equals("description")) {
                                article.description = lastText;
                            }
                            else if (tag_name.equals("guid")) {
                                article.guid = lastText;
                            }
                            else if (tag_name.equals("pubDate")) {
                                //                            article.timestamp = xpp.getText();
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
