package com.concentricsky.android.pensive;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.codeslap.gist.SimpleCursorLoader;

/**
 * Created by wiggins on 9/26/13.
 */
public class NavigationFragment extends Fragment

{
    private NavigationListener mNavigationListener;
    private ExpandableListView mDrawerList;

    private LoaderManager mLoaderManager;
    private SparseIntArray mNavLoaderIds;
    private static final int TAG_LOADER_ID=-1;

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private ThothDrawerAdapter mDrawerAdapter;
    private NavigationLoaderCallbacks mLoaderCallbacks;
    private boolean mFeedsPresent;

    public void reload() {
        if (mLoaderCallbacks != null && mLoaderManager != null) {
            mLoaderManager.restartLoader(TAG_LOADER_ID, null, mLoaderCallbacks); //navigation drawer: start tag loader
        }
    }


    public interface NavigationListener
    {
        public void onFeedsDiscovered(boolean feeds_are_present);

        public void onTagClicked(long tag_id);
        public void onFeedClicked(long feed_id);
        public void onAllFeedsClicked();
        public void onManageFeedsClicked();
        public void onSubscribeClicked();
    };
    public void setNavigationListener(NavigationListener listener) { mNavigationListener = listener; }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_navigation, container, false);

        mDrawerList = (ExpandableListView)v.findViewById(android.R.id.list);
        mDrawerList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                return true;
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        FragmentActivity activity = getActivity();

        try {
            setNavigationListener((NavigationListener)activity);
        } catch (ClassCastException e) {
            mNavigationListener = null;
        }

        mDrawerAdapter = new ThothDrawerAdapter(activity);
        mDrawerList.setAdapter(mDrawerAdapter);

        mNavLoaderIds = new SparseIntArray();         //navigation drawer: map loader ids -> tag ids
        mLoaderManager = getLoaderManager();
        mLoaderCallbacks = new NavigationLoaderCallbacks();
        mLoaderManager.restartLoader(TAG_LOADER_ID, null, mLoaderCallbacks); //navigation drawer: start tag loader

        mRequestQueue = Volley.newRequestQueue(activity);
        mImageLoader = new ImageLoader(mRequestQueue, new BitmapLruCache());

    }



    private class NavigationLoaderCallbacks  implements LoaderManager.LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int loader_id, Bundle bundle) {
            Context context = getActivity();
            if (loader_id == TAG_LOADER_ID) { // tag loader
                return new SimpleCursorLoader(context) {
                    @Override
                    public Cursor loadInBackground() {
                        return ThothDatabaseHelper.getInstance().getTagCursor();
                    }
                };
            }

            final long tag_id = mNavLoaderIds.get(loader_id, -1);
            if (tag_id != -1) {
                return new SimpleCursorLoader(context) {
                    @Override
                    public Cursor loadInBackground() {
                        return ThothDatabaseHelper.getInstance().getFeedCursor(tag_id);
                    }
                };
            }
            // shouldn't be here!
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            int loader_id = loader.getId();
            if (loader_id == TAG_LOADER_ID) { //tag cursor
                if (mNavigationListener != null) {
                    if (cursor == null || cursor.getCount() < 1) {
                        mFeedsPresent = false;
                    } else {
                        mFeedsPresent = true;
                    }
                    mNavigationListener.onFeedsDiscovered(mFeedsPresent);
                }
                mDrawerAdapter.changeCursor(cursor);
            }
            else {
                //loader_id is the group pos of the children cursor we are trying to load
                mDrawerAdapter.setChildrenCursor(loader_id, cursor);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
    //        if (mDrawerAdapter == null)
    //            return;
    //
    //        int loader_id = loader.getId();
    //        if (loader_id == TAG_LOADER_ID) { //tag cursor
    //            mDrawerAdapter.changeCursor(null);
    //        } else {
    ////            mDrawerAdapter.setChildrenCursor(loader_id, null);
    //        }
        }

    }

    private class ThothDrawerAdapter extends SimpleCursorTreeAdapter {

        public ThothDrawerAdapter(Context context) {
            super(context,
                    null,
                    R.layout.item_navigation_group,
                    new String[]{"title","unread"}, // groupFrom,
                    new int[]{android.R.id.text1, android.R.id.text2}, // groupTo,
                    R.layout.item_navigation_child,
                    new String[]{"title","unread"}, // childFrom,
                    new int[]{android.R.id.text1, android.R.id.text2} // childTo,
            );
        }

        protected void bindView(View view, Context context, Cursor cursor) {
            TextView tv = (TextView) view.findViewById(android.R.id.text1);
            String title = String.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            tv.setText(title);

            tv = (TextView) view.findViewById(android.R.id.text2);
            int unread_idx = cursor.getColumnIndex("unread");
            if (unread_idx != -1) {
                long unread = cursor.getLong(unread_idx);
                if (unread > 0) {
                    tv.setVisibility(View.VISIBLE);
                    tv.setText( String.valueOf(unread) );
                }
                else {
                    tv.setVisibility(View.INVISIBLE);
                }
            }
            else {
                tv.setVisibility(View.INVISIBLE);
            }

        }

        @Override
        protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
            bindView(view,context,cursor);
            final long feed_id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            final String title = String.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("title")));

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (mNavigationListener != null) {
                        mNavigationListener.onFeedClicked(feed_id);
                    }

                }
            });
            view.setBackgroundResource(R.color.navigation_child_background);

            String link = cursor.getString(cursor.getColumnIndexOrThrow("link"));
            View default_favicon = (ImageView)view.findViewById(R.id.missing_favicon);
            NetworkImageView icon = (NetworkImageView) view.findViewById(R.id.favicon);
            if (!TextUtils.isEmpty(link)) {
                icon.setDefaultImageResId(R.drawable.rss_default);
                icon.setErrorImageResId(R.drawable.rss_default);
                icon.setImageUrl(link+"/favicon.ico", mImageLoader);
                icon.setVisibility(View.VISIBLE);
                default_favicon.setVisibility(View.GONE);
            }
            else {
                icon.setImageUrl(null, mImageLoader);
                icon.setVisibility(View.GONE);
                default_favicon.setVisibility(View.VISIBLE);
            }
        }
        @Override
        protected void bindGroupView(View view, Context context, Cursor cursor, boolean is_expanded) {
            bindView(view, context, cursor);
            final int groupPosition = cursor.getPosition();
            final long _id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));

            ImageView iv = (ImageView)view.findViewById(R.id.group_indicator);
            iv.setImageResource(is_expanded ? R.drawable.collapse : R.drawable.expand);

            if (_id < 0) {
                iv.setVisibility(View.INVISIBLE);
            } else {
                iv.setVisibility(View.VISIBLE);
            }

            View left = view.findViewById(R.id.left);
            View right = view.findViewById(R.id.right);
            left.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (mNavigationListener != null) {

                        if (_id == -2) {
                            //all feeds
                            mNavigationListener.onAllFeedsClicked();
                        }
                        else if (_id == -3) {
                            //manage feeds
                            mNavigationListener.onManageFeedsClicked();
                        }
                        else if (_id == -4) {
                            //subscribe
                            mNavigationListener.onSubscribeClicked();
                        }
                        else if (_id > 0) {
                            //tag
                            long tag_id = mDrawerAdapter.getGroupId(groupPosition);
                            mNavigationListener.onTagClicked(tag_id);
                        }
                    }


                }
            });
            right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mDrawerList.isGroupExpanded(groupPosition))
                        mDrawerList.collapseGroup(groupPosition);
                    else
                        mDrawerList.expandGroup(groupPosition);
                }
            });
            view.setBackgroundResource(is_expanded ? R.color.navigation_expanded_background : R.color.navigation_collapsed_background);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor cursor) {
            int tag_id = cursor.getInt(cursor.getColumnIndex("_id"));
            int loader_id = cursor.getPosition();

            mNavLoaderIds.append(loader_id, tag_id);

            Loader loader = mLoaderManager.getLoader(loader_id);
            if (loader != null && !loader.isReset()) {
                mLoaderManager.restartLoader(loader_id, null, mLoaderCallbacks);
            }
            else {
                mLoaderManager.initLoader(loader_id, null, mLoaderCallbacks);
            }

            return null;
        }

        @Override
        public void changeCursor(Cursor cursor) {
            String[] extras_fields = new String[] {"_id", "title"};
            MatrixCursor top_extras = new MatrixCursor(extras_fields);
            MatrixCursor bottom_extras = new MatrixCursor(extras_fields);

            if (mFeedsPresent) {
                top_extras.addRow(new String[] {"-2", getString(R.string.all_feeds)});
                bottom_extras.addRow(new String[] {"-3", getString(R.string.action_manage_feeds)});
            }
            bottom_extras.addRow(new String[] {"-4", getString(R.string.add_new_feed)});

            Cursor newCursor = new MergeCursor(new Cursor[] {top_extras, cursor, bottom_extras});
            super.changeCursor(newCursor);
        }
    }
}
