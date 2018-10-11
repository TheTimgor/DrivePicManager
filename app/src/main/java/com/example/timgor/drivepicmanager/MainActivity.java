package com.example.timgor.drivepicmanager;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.List;

public class MainActivity extends GenericActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            GenericActivity.setUpAPI();
        } catch (java.io.IOException e) {
            displayMessage("IO exception: " + e, Toast.LENGTH_LONG);
        } catch (java.security.GeneralSecurityException e){
            displayMessage("Security exception: " + e, Toast.LENGTH_LONG);
        }
        Drive service = getService();

        /*
        try {
            FileList result = service.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
            List<File> files = result.getFiles();
            if (files == null || files.isEmpty()) {
                displayMessage(("No files found.");
            } else {
                displayMessage(("Files:");
                for (File file : files) {
                    displayMessage(String.format("%s (%s)", file.getName(), file.getId()));
                }
            }
        } catch (java.io.IOException e) {
            displayMessage("IO exception: " + e);
        }
        */

    }




}
