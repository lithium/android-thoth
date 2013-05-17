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

        //http://www.youtube.com/watch?v=yhv8l9F44qo#t=14m36
        mRequestQueue = Volley.newRequestQueue(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.thoth_main, menu);
        return true;
    }
    
}
