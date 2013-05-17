package com.concentricsky.android.thoth;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class ThothMainActivity extends Activity {

    private RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRequestQueue = Volley.newRequestQueue(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.thoth_main, menu);
        return true;
    }
    
}
