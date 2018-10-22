package com.example.timgor.drivepicmanager;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.example.timgor.drivepicmanager.GenericActivity.REQUEST_AUTHORIZATION;

public class ListActivity extends android.app.ListActivity {

    private ArrayAdapter<String> adapter;
    private ArrayList<String> listItems = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);
        
        displayFiles();
    }

    public void displayFiles() {
        DisplayFilesThread t = new DisplayFilesThread();
        t.start();

    }

    private class DisplayFilesThread extends Thread {
        public void run() {
            Looper.prepare();
            Drive service = GenericActivity.getService();

            try {

                FileList result = service.files().list()
                        .setPageSize(10)
                        .setFields("nextPageToken, files(id, name)")
                        .execute();

                List<File> files = result.getFiles();

                if (files == null || files.isEmpty()) {
                    displayMessage(("No files found."));
                } else {

                    for (File file : files) {
                        adapter.add(String.format("%s (%s)", file.getName(), file.getId()));
                    }
                }

            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                displayMessage("IO exception: " + e, Toast.LENGTH_LONG);

            }

            Looper.loop();
        }

        public void displayMessage(String message, int duration) {
            Context context = getApplicationContext();
            CharSequence text = message;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

        }

        public void displayMessage(String message) {
            displayMessage(message, Toast.LENGTH_SHORT);
        }

    }
}
