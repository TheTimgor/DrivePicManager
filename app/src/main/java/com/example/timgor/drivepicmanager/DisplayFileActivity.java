package com.example.timgor.drivepicmanager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DisplayFileActivity extends GenericActivity {
    File driveFile;
    String contentURl;
    private String TAG = "Display File Activity ~~";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_file);
        DisplayFileActivity.GetFileTask t = new DisplayFileActivity.GetFileTask();
        contentURl = getIntent().getStringExtra(ListActivity.CONTENT_LINK);
        t.execute();


    }

    private class GetFileTask extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... params) {

            try {
                Intent i = getIntent();
                String id = i.getStringExtra(ListActivity.FILE_ID);
                driveFile = service.files().get(id).execute();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            displayResult();
        }
    }



    private void displayResult() {
        TextView content = findViewById(R.id.textContent);
        ImageView icon = findViewById(R.id.displayImage);
        String data = contentURl;
        content.setText(data);
        Log.d(TAG, "displayed data " + data);
        boolean isImage = driveFile.getMimeType().split("/")[0].equals("image");
        //Bitmap debugBM = BitmapFactory.decodeResource(getResources(),R.drawable.debug);
        //setImage(icon, debugBM);
        String id = driveFile.getId();
        new DisplayImageTask().execute(id,String.valueOf(isImage));



    }

    private class DisplayImageTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... strings) {
            String id = strings[0];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Bitmap bitmap = null;
            if(strings[1]=="true") {
                try {
                    service.files().get(id)
                            .executeMediaAndDownloadTo(outputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, e.toString());
                }
                byte[] bitmapdata = outputStream.toByteArray();
                bitmap = BitmapFactory.decodeByteArray(bitmapdata, 0, bitmapdata.length);

            }
            return bitmap;
        }
        protected void onPostExecute(Bitmap b){
            setImage((ImageView) findViewById(R.id.displayImage),b);
        }
    }

    public void setImage(ImageView v, Bitmap b){
        v.setImageBitmap(b);
    }



}
