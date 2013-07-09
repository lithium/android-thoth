package com.concentricsky.android.thoth;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.codeslap.gist.SimpleCursorLoader;
import com.concentricsky.android.thoth.models.Feed;

/**
 * Created by wiggins on 7/7/13.
 */
public class EditFeedFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_TAGS = 1;
    private final Feed mFeed;
    private AutoCompleteTagsAdapter mAdapter;
    private View mButtonContainer;
    private ProgressBar mProgress;
    private AutoCompleteAppendTextView mFeedTags;

    public EditFeedFragment(Feed feed) {
        mFeed = feed;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new AutoCompleteTagsAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.edit_feed, container, false);

        TextView tv = (TextView) root.findViewById(R.id.original_title);
        tv.setText(mFeed.title);

        EditText title = (EditText) root.findViewById(R.id.title);
        title.setText(mFeed.title);

        mFeedTags = (AutoCompleteAppendTextView) root.findViewById(R.id.tags);
        if (mFeed.tags_concat != null)
            mFeedTags.setText(mFeed.tags_concat);
        mFeedTags.setAdapter(mAdapter);


        mProgress = (ProgressBar)root.findViewById(android.R.id.progress);
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
        mButtonContainer.setVisibility(View.GONE);
        mProgress.setVisibility(View.VISIBLE);
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                mFeed.tags = mFeedTags.getText().toString().split(",");
                mFeed.save(ThothDatabaseHelper.getInstance().getWritableDatabase());
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                getFragmentManager().popBackStack();
            }
        };
        asyncTask.execute();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(LOADER_TAGS, null, this);
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
}
