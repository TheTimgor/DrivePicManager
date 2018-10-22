package com.example.timgor.drivepicmanager;

import android.content.Intent;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.List;

public class MainActivity extends GenericActivity {
    private static String TAG = "Main Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpAPI();

        /*

        //*/

    }

    public void onPause() {
        super.onPause();
        Log.d(TAG, "activity paused");
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_button:
                Intent i = new Intent(this, ListActivity.class);
                startActivity(i);
                break;
        }
    }

}
