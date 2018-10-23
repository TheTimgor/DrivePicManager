package com.example.timgor.drivepicmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static com.example.timgor.drivepicmanager.GenericActivity.REQUEST_AUTHORIZATION;

public class ListActivity extends android.app.ListActivity {

    private static final String TAG = "List Activity ~~ ";
    private ArrayAdapter<String> adapter;
    ArrayList<String> listItems = new ArrayList<String>();
    List<File> files;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);

        Log.d(TAG, "adapter set up");

        ListView l = (ListView)((ConstraintLayout)findViewById(R.id.rootLayout)).getChildAt(0);
        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, listItems.get(position));
            }
        });

        displayFiles();
    }

    public void displayFiles() {
        /*
        DisplayFilesThread t = new DisplayFilesThread();
        t.start();
        /*
        try{
            t.join();
        }catch(InterruptedException e ){}
        */


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG,e.toString());
        }

        Log.d(TAG, "got files");

        GetFilesTask t = new GetFilesTask();
        t.execute();

    }

    public void displayResult(){
        adapter.notifyDataSetChanged();
    }




    private class GetFilesTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "started thread");

            Drive service = GenericActivity.getService();

            Log.d(TAG, "got service");

            //*

            try {


                FileList result = service.files().list()
                        .setPageSize(20)
                        .setFields("nextPageToken, files(id, name)")
                        .execute();

                Log.d(TAG, "Got result");

                files = result.getFiles();

                Log.d(TAG, "got files list");

                if (files == null || files.isEmpty()) {
                    Log.e(TAG,"No files found.");
                } else {

                    for (File file : files) {
                        Log.d(TAG,String.format("%s (%s)", file.getName(), file.getId()));
                        listItems.add(String.format("%s (%s)", file.getName(), file.getId()));
                    }
                }

            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(TAG,"IO exception: " + e);

            }
            //*/

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            displayResult();
        }
    }

    private class DisplayFilesThread extends Thread {
        public void run() {

            Log.d(TAG, "started thread");
            Looper.prepare();

            Drive service = GenericActivity.getService();

            Log.d(TAG, "got service");

            //*

            try {


                FileList result = service.files().list()
                        .setPageSize(10)
                        .setFields("nextPageToken, files(id, name)")
                        .execute();

                Log.d(TAG, "Got result");

                files = result.getFiles();

                Log.d(TAG, "got files list");

                if (files == null || files.isEmpty()) {
                    Log.e(TAG,"No files found.");
                } else {

                    for (File file : files) {
                        Log.d(TAG,String.format("%s (%s)", file.getName(), file.getId()));
                        listItems.add(String.format("%s (%s)", file.getName(), file.getId()));
                    }
                }

            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                displayMessage("IO exception: " + e, Toast.LENGTH_LONG);

            }
            //*/

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
