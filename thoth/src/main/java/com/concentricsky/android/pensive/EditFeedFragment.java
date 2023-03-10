package com.concentricsky.android.pensive;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.concentricsky.android.pensive.models.Feed;

/**
 * Created by wiggins on 7/7/13.
 */
public class EditFeedFragment extends Fragment
{
    private static final int LOADER_TAGS = 1;
    private final Feed mFeed;
    private View mButtonContainer;
    private AutoSuggestTagsView mFeedTags;
    private EditText mTitleText;
    private MenuItem mSaveItem;
    private MenuItem mDeleteItem;
    private InputMethodManager mInputManager;

    public EditFeedFragment(Feed feed) {
        mFeed = feed;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            mFeedTags.setText(mFeed.tags_concat+",");

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

                String unfiled = getString(R.string.unfiled);
                String tags = mFeedTags.getText().toString();
                mFeed.tags = tags.replaceAll(unfiled+",", "").split(",");
                if (mFeed.tags == null || mFeed.tags.length < 0 || mFeed.tags[0] == null || mFeed.tags[0].isEmpty()) {
                    mFeed.tags = new String [] {unfiled};
                }
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
                Activity a = getActivity();
                if (a != null)
                    a.setProgressBarIndeterminateVisibility(false);
                getFragmentManager().popBackStack();
            }
        };
        asyncTask.execute();

    }

    @Override
    public void onPause() {
        super.onPause();
        ThothMainActivity activity = (ThothMainActivity)getActivity();
        mInputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        activity.reloadTags();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentActivity activity = getActivity();
        activity.getActionBar().setTitle(R.string.edit_feed);

        mInputManager = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_editfeed, menu);
        mDeleteItem = menu.findItem(R.id.action_delete);
        mSaveItem = menu.findItem(R.id.action_save);
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
