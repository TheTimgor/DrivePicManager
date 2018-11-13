package com.example.timgor.drivepicmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.constraint.ConstraintLayout;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static com.example.timgor.drivepicmanager.GenericActivity.REQUEST_AUTHORIZATION;

public class ListActivity extends android.app.ListActivity {

    private static final String TAG = "List Activity ~~ ";
    private FilePreviewAdapter adapter;
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayList<FilePreview> listPreview = new ArrayList<FilePreview>();
    List<File> files = new ArrayList<File>();
    public static final String FILE_ID = "com.example.drivepicmanager.FILE_ID";
    public static final String CONTENT_LINK = "com.example.drivepicmanager.CONTENT_LINK";
    private String currentFolder = "root";
    protected String nextPageToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        adapter = new FilePreviewAdapter(this, R.layout.filepreview_layout,listPreview);
        setListAdapter(adapter);

        Log.d(TAG, "adapter set up");

        ListView l = (ListView)((ConstraintLayout)findViewById(R.id.rootLayout)).getChildAt(0);
        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File f = files.get(position);
                Log.d(TAG, "position " + position);
                Log.d(TAG, "type " + f.getMimeType());
                Log.d(TAG, "name " + f.getName());
                if(f.getMimeType().contains("folder")) {
                    ListView l = (ListView)((ConstraintLayout)findViewById(R.id.rootLayout)).getChildAt(0);
                    l.setSelectionAfterHeaderView();
                    currentFolder = f.getId();
                    nextPageToken = null;
                    files = new ArrayList<File>();
                    listPreview = new ArrayList<FilePreview>();
                    //adapter = new FilePreviewAdapter(this, R.layout.filepreview_layout,listPreview);
                    //setListAdapter(adapter);
                    //adapter.notifyDataSetChanged();
                    Log.d(TAG, "opening folder " + currentFolder);
                    displayFiles();
                } else {
                    Intent i = new Intent(ListActivity.this, DisplayFileActivity.class);
                    i.putExtra(FILE_ID, f.getId());
                    i.putExtra(CONTENT_LINK, f.getWebContentLink());
                    startActivity(i);
                }
            }
        });

        displayFiles();
    }

    public void switchFolder(File f){
        ListView l = (ListView)((ConstraintLayout)findViewById(R.id.rootLayout)).getChildAt(0);
        l.setSelectionAfterHeaderView();
        currentFolder = f.getId();
        nextPageToken = null;
        files = new ArrayList<File>();
        listPreview = new ArrayList<FilePreview>();
        //adapter = new FilePreviewAdapter(this, R.layout.filepreview_layout,listPreview);
        //setListAdapter(adapter);
        //adapter.notifyDataSetChanged();
        Log.d(TAG, "opening folder " + currentFolder);
        //displayFiles();
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

        displayMessage("displaying files . . . ", Toast.LENGTH_LONG);

        GetFileTask t = new GetFileTask();
        t.execute(1);

    }

    public void displayResult(){
        adapter.notifyDataSetChanged();
        Log.d(TAG, "updated adapter");
    }

    private class InitTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }

    private class GetFileTask extends AsyncTask<Integer,Void,Void>{

        @Override
        protected Void doInBackground(Integer... params) {
            Log.d(TAG, "started thread");

            int pageSize = params[0].intValue();

            Drive service = GenericActivity.getService();

            Log.d(TAG, "got service");

            //*




            FileList result = null;

            try {
                result = service.files().list()
                        .setQ(String.format("'%s' in parents and trashed = false", currentFolder))
                        .setFields("nextPageToken, files(id, name, mimeType, thumbnailLink)")
                        .setPageToken(nextPageToken)
                        .setPageSize(pageSize)
                        .execute();
                files.addAll(result.getFiles());
                nextPageToken = result.getNextPageToken();
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(TAG,"IO exception: " + e);
                e.printStackTrace();
            }

            if(result!=null) {

                Log.d(TAG, "Got result");

                Log.d(TAG, "number of files: " + files.size());

                Log.d(TAG, "got files list");

                if (files == null || files.isEmpty()) {
                    Log.w(TAG, "No files found.");
                } else {

                    for (File file : files) {

                        listPreview.add(new FilePreview(file, String.format("(%s) %s \n%s", file.getMimeType(), file.getName(), file.getWebContentLink())));
                        listItems.add(String.format("(%s) %s \n%s", file.getMimeType(), file.getName(), file.getWebContentLink()));

                    }
                }
            }


            //*/

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            displayResult();
        }

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

    public class FilePreview{
        public File file;
        public String displayText;


        private FilePreview(File file, String displayText) {
            this.file = file;
            this.displayText = displayText;
        }



    }

    public class FilePreviewAdapter extends ArrayAdapter<FilePreview> {
        private Context context;
        ArrayList<FilePreview> previews = new ArrayList<FilePreview>();

        public FilePreviewAdapter(Context context, int resource, ArrayList<FilePreview> objects) {
            super(context, resource);
            this.context = context;
            this.previews = objects;
            Log.d(TAG, "created adapter");
        }

        @Override
        public int getCount() {
            return previews.size();
        }

        @Override
        public FilePreview getItem(int position) {
            return previews.get(position);
        }



        public View getView(int position, View convertView, ViewGroup parent) {

            if(nextPageToken != null && position == previews.size()-1) {
                GetFileTask t0 = new GetFileTask();
                t0.execute(100);
            }

            Log.d(TAG,"token"+nextPageToken);

            FilePreview preview = previews.get(position);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.filepreview_layout, null);
            ImageView icon = (ImageView) view.findViewById(R.id.thumb);
            TextView text = (TextView) view.findViewById(R.id.text);

            File file = preview.file;

            DisplayImageTask t = new DisplayImageTask();
            t.execute(file,icon);

            text.setText(preview.displayText);

            Log.d(TAG, "displayed text " + preview.displayText);

            return view;

        }
    }

    private class DisplayImageTask extends AsyncTask<Object, Void, Bitmap> {
        ImageView icon;

        @Override
        protected Bitmap doInBackground(Object... params) {
            File file = (File) params[0];
            icon = (ImageView) params[1];

            Bitmap bitmap = null;
            try {
                if (file.getThumbnailLink() != null) {
                    //*
                    //URL url = new URL(URLEncoder.encode(file.getThumbnailLink(),"utf-8"));
                    URL url = new URL(file.getThumbnailLink());
                    Log.d(TAG, String.format("%s (%s)", file.getName(), file.getThumbnailLink()));
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(input);
                    //*/
                    //bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.debug);
                }
            } catch (IOException e) {
                Log.e(TAG, "IO exception: " + e);
                e.printStackTrace();
            }


            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            setImage(icon, bitmap);
        }
    }

    public void setImage(ImageView v, Bitmap b){
        v.setImageBitmap(b);
    }
}
