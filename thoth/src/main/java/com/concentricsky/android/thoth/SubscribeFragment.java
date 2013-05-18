package com.concentricsky.android.thoth;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * Created by wiggins on 5/17/13.
 */
public class SubscribeFragment extends Fragment implements ThothFragmentInterface {
    private static final String TAG = "ThothSubscribeFragment";
    private RequestQueue mRequestQueue;
    private EditText mLinkText;
    private Button mSubmitButton;

    public SubscribeFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mRequestQueue = Volley.newRequestQueue(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mRequestQueue.stop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_subscribe, container, false);

        mLinkText = (EditText)root.findViewById(R.id.subscribe_link);
        mSubmitButton = (Button)root.findViewById(R.id.subscribe_submit);

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String link = mLinkText.getText().toString();
//                ThothDatabaseHelper.getInstance().addFeed(link,title,tags);

                mRequestQueue.add(new SubscribeFeedRequest(link));
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


    private class SubscribeFeedRequest extends StringRequest {

        private SubscribeFeedRequest(String url) {
            super(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "got subscribe response");

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "volley error! "+error);

                    }
                });


        }

    }
}
