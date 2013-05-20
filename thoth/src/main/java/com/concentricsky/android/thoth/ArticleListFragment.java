package com.concentricsky.android.thoth;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.*;
import android.widget.SimpleCursorAdapter;
import com.codeslap.gist.SimpleCursorLoader;

/**
 * Created by wiggins on 5/17/13.
 */
public class ArticleListFragment extends ListFragment
                                 implements ThothFragmentInterface,
                                            LoaderManager.LoaderCallbacks<Cursor>
{
    private LoaderManager mLoaderManager;

    private static final int FEED_LOADER_ID=-2;
    private SimpleCursorAdapter mAdapter;
    private long mFeedId=-1;

    public ArticleListFragment() {

    }


    public void setFeed(long feed_id) {
        mFeedId = feed_id;
        load_feed();
    }
    private void load_feed()
    {
        if (mLoaderManager == null || mFeedId < 1) {
            return;
        }
        mLoaderManager.initLoader(FEED_LOADER_ID, null, this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mLoaderManager = activity.getLoaderManager();
        if (mAdapter == null) {
            mAdapter = new SimpleCursorAdapter(activity, android.R.layout.simple_list_item_1, null,
                    new String[] {"title"},
                    new int[] {android.R.id.text1}, 0);
        }
        load_feed();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoaderManager.destroyLoader(FEED_LOADER_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_articlelist, container, false);

        return root;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {
        menu.findItem(R.id.action_subscribe).setVisible(!drawer_open);
        menu.findItem(R.id.action_refresh).setVisible(!drawer_open);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
       switch (item.getItemId()) {
           case R.id.action_subscribe:
               ThothMainActivity act = (ThothMainActivity)getActivity();
               act.showSubscribe();
               return true;
       }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new SimpleCursorLoader(getActivity()) {
            @Override
            public Cursor loadInBackground() {
                return ThothDatabaseHelper.getInstance().getArticleCursor(mFeedId);
            }
        };


    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.changeCursor(null);
    }

}
