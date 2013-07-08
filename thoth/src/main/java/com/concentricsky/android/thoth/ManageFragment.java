package com.concentricsky.android.thoth;

import android.*;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import com.codeslap.gist.SimpleCursorLoader;

/**
 * Created by wiggins on 7/7/13.
 */
public class ManageFragment extends ListFragment
                            implements   ThothFragmentInterface, LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_FEEDS = 1;
    private LoaderManager mLoaderManager;
    private CursorAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoaderManager = getLoaderManager();
//        mAdapter = new FeedCursorAdapter(getActivity());
        mAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_2, null,
                new String[] {"title", "tags"},
                new int[] {android.R.id.text1, android.R.id.text2},
                0);

    }




    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setListAdapter(mAdapter);
        mLoaderManager.restartLoader(LOADER_FEEDS, null, this);
        getActivity().getActionBar().setTitle(R.string.action_manage_feeds);
    }

    private void popBackStack()
    {
        ThothMainActivity activity = (ThothMainActivity)getActivity();
        activity.reloadTags();
        activity.getFragmentManager().popBackStack("Manage", FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new SimpleCursorLoader(getActivity()) {
            @Override
            public Cursor loadInBackground() {
                return ThothDatabaseHelper.getInstance().getManageFeedsCursor();
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


    private static class FeedCursorAdapter extends CursorAdapter {
        private FeedCursorAdapter(Context context) {
            super(context, null, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return null;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

        }
    }
}
