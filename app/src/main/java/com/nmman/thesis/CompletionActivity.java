package com.nmman.thesis;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class CompletionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completion);
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}
