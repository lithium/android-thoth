package com.concentricsky.android.thoth;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.codeslap.gist.SimpleCursorLoader;
import com.concentricsky.android.thoth.models.Feed;

/**
 * Created by wiggins on 7/7/13.
 */
public class EditFeedFragment extends Fragment
                                implements LoaderManager.LoaderCallbacks<Cursor>,ThothFragmentInterface
{
    private static final int LOADER_TAGS = 1;
    private final Feed mFeed;
    private AutoCompleteTagsAdapter mAdapter;
    private View mButtonContainer;
    private AutoSuggestTagsView mFeedTags;
    private EditText mTitleText;
    private MenuItem mSaveItem;
    private MenuItem mDeleteItem;

    public EditFeedFragment(Feed feed) {
        mFeed = feed;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new AutoCompleteTagsAdapter(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.edit_feed, container, false);

        TextView tv = (TextView) root.findViewById(R.id.original_title);
        tv.setText(mFeed.title);

        mTitleText = (EditText) root.findViewById(R.id.title);
        mTitleText.setText(mFeed.title);

        mFeedTags = (AutoSuggestTagsView) root.findViewById(R.id.tags);
        if (mFeed.tags_concat != null)
            mFeedTags.setText(mFeed.tags_concat);
        mFeedTags.setAdapter(mAdapter);

        mButtonContainer = root.findViewById(R.id.button_container);
        Button submit = (Button) root.findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveFeed();
            }
        });
        Button cancel = (Button) root.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().popBackStack();
            }
        });
        return root;
    }

    private void saveFeed() {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                mFeed.title = mTitleText.getText().toString();
                mFeed.tags = mFeedTags.getText().toString().split(",");
                mFeed.save(ThothDatabaseHelper.getInstance().getWritableDatabase());
                return null;
            }

            @Override
            protected void onPreExecute() {
                getActivity().setProgressBarIndeterminateVisibility(true);
                mButtonContainer.setVisibility(View.GONE);
                mDeleteItem.setVisible(false);
                mSaveItem.setVisible(false);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                getActivity().setProgressBarIndeterminateVisibility(false);
                getFragmentManager().popBackStack();
            }
        };
        asyncTask.execute();
    }

    private void deleteFeed() {
        AsyncTask<Void,Void,Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                getActivity().setProgressBarIndeterminateVisibility(true);
                mButtonContainer.setVisibility(View.GONE);
                mDeleteItem.setVisible(false);
                mSaveItem.setVisible(false);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                mFeed.delete(ThothDatabaseHelper.getInstance().getWritableDatabase());
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                getActivity().setProgressBarIndeterminateVisibility(false);
                getFragmentManager().popBackStack();
            }
        };
        asyncTask.execute();

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(LOADER_TAGS, null, this);
        getActivity().getActionBar().setTitle(R.string.edit_feed);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new SimpleCursorLoader(getActivity()) {
            @Override
            public Cursor loadInBackground() {
                return ThothDatabaseHelper.getInstance().getTagCursor();
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_editfeed, menu);
        mDeleteItem = menu.findItem(R.id.action_delete);
        mSaveItem = menu.findItem(R.id.action_save);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {
        menu.findItem(R.id.action_manage_feeds).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveFeed();
                return true;
            case R.id.action_delete:
                deleteFeed();
                return true;
        }
        return false;
    }
}
