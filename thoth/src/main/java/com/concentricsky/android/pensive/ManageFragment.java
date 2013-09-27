package com.concentricsky.android.pensive;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.*;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import com.codeslap.gist.SimpleCursorLoader;
import com.concentricsky.android.pensive.models.Feed;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by wiggins on 7/7/13.
 */
public class ManageFragment extends ListFragment
                            implements   LoaderManager.LoaderCallbacks<Cursor>
{
    private static final int LOADER_FEEDS = 1;
    private LoaderManager mLoaderManager;
    private CursorAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoaderManager = getLoaderManager();
        mAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_activated_2, null,
                new String[] {"title", "tags"},
                new int[] {android.R.id.text1, android.R.id.text2},
                0);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setListAdapter(mAdapter);
        mLoaderManager.restartLoader(LOADER_FEEDS, null, this);
        getActivity().getActionBar().setTitle(R.string.action_manage_feeds);

        final ListView listView = getListView();
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new ManageMultiChoiceListener());

    }

    private class ManageMultiChoiceListener implements AbsListView.MultiChoiceModeListener
    {
        private ArrayList<Integer> mActivePositions;
        private MenuItem mEditMenuItem;
        private ProgressDialog mProgress;


        public ManageMultiChoiceListener()
        {
            mActivePositions = new ArrayList<Integer>();
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
            View childAt = getListView().getChildAt(i);
            if (childAt != null)
                childAt.setActivated(b);
            if (b) {
                mActivePositions.add(i);
            }
            else {
                if (mActivePositions.contains(i))
                    mActivePositions.remove(i);
            }

            if (mActivePositions.size() > 1) {
                mEditMenuItem.setVisible(false);
            }
            else {
                mEditMenuItem.setVisible(true);
            }

        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.manage_context, menu);
            mEditMenuItem = menu.findItem(R.id.action_edit);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            mActivePositions.clear();
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

            switch (menuItem.getItemId()) {
                case R.id.action_edit:
                    Iterator<Integer> iterator = mActivePositions.iterator();
                    if (iterator.hasNext()) {
                        Integer pos = iterator.next();
                        edit_feed_at(pos);
                    }
                    actionMode.finish();
                    return true;
                case R.id.action_delete:
                    deleteSelected();
                    actionMode.finish();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
        }

        private void deleteSelected() {
            AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected void onPreExecute() {
                    mProgress = new ProgressDialog(getActivity());
                    mProgress.setTitle(getString(R.string.deleting));
                    mProgress.setMessage(getString(R.string.please_wait));
                    mProgress.setCancelable(false);
                    mProgress.setIndeterminate(true);
                    mProgress.show();
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    mProgress.hide();
                    mProgress = null;
                    mLoaderManager.restartLoader(LOADER_FEEDS, null, ManageFragment.this);
                }

                @Override
                protected Void doInBackground(Void... voids) {
                    Iterator<Integer> iterator = mActivePositions.iterator();
                    while (iterator.hasNext()) {
                        Integer pos = iterator.next();
                        Feed feed = get_feed_at(pos);
                        feed.delete(ThothDatabaseHelper.getInstance().getWritableDatabase());
                    }
                    return null;
                }
            };
            asyncTask.execute();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ThothMainActivity activity = (ThothMainActivity)getActivity();
        activity.reloadTags();
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

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        edit_feed_at(position);
    }

    private Feed get_feed_at(int position) {
        Feed feed = new Feed();
        Cursor c = mAdapter.getCursor();
        c.moveToPosition(position);
        feed.hydrate(c);
        return feed;
    }

    private void edit_feed_at(int position) {
        ThothMainActivity activity = (ThothMainActivity)getActivity();
        activity.showEditFeed(get_feed_at(position));
    }
}
