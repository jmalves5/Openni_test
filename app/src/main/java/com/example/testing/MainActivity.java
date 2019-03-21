package com.example.testing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;

import org.openni.OpenNI;
import org.openni.android.OpenNIHelper;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = "NiViewer";

    private OpenNIHelper mOpenNIHelper;

    private String mRecording;

    private LinearLayout mStreamsContainer;

    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        this.mOpenNIHelper = new OpenNIHelper(this);

        OpenNI.setLogAndroidOutput(true);
        OpenNI.setLogMinSeverity(0);
        OpenNI.initialize();
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.app_name));
        this.mStreamsContainer = (LinearLayout) findViewById(R.id.streams_container);
        onConfigurationChanged(getResources().getConfiguration());
    }

    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent != null) {
            Uri data = intent.getData();
            if (data != null) {
                this.mRecording = data.getEncodedPath();
                Log.d(TAG, "Will open file " + this.mRecording);
            }
        }
    }




    protected void onDestroy() {
        super.onDestroy();
    }

}
