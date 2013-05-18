package com.concentricsky.android.thoth;

import android.app.Fragment;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by wiggins on 5/17/13.
 */
public class SubscribeFragment extends Fragment implements ThothFragmentInterface {
    private EditText mLinkText;
    private Button mSubmitButton;

    public SubscribeFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_subscribe, container, false);

        mLinkText = (EditText)root.findViewById(R.id.subscribe_link);
        mSubmitButton = (Button)root.findViewById(R.id.subscribe_submit);

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                String link = mLinkText.getText();
//                ThothDatabaseHelper.getInstance().addFeed(link,title,tags);
            }
        });

        return root;
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu, boolean drawer_open) {

//        menu.findItem(R.id.action_share).setVisible(!drawer_open);
//        menu.findItem(R.id.action_visitpage).setVisible(!drawer_open);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }
}
