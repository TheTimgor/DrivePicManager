package com.example.timgor.drivepicmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
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
import android.os.Process;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE;
import static com.example.timgor.drivepicmanager.GenericActivity.REQUEST_AUTHORIZATION;

public class ListActivity extends android.app.ListActivity {

    private static final String TAG = "List Activity ~~ ";
    private FilePreviewAdapter adapter;
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayList<FilePreview> listPreview = new ArrayList<FilePreview>();
    List<File> files;
    public static final String FILE_ID = "com.example.drivepicmanager.FILE_ID";
    public static final String CONTENT_LINK = "com.example.drivepicmanager.CONTENT_LINK";
    private String currentFolder = "root";
    ArrayList<DisplayImageTask> displayImageTasks = new ArrayList<>();
    ArrayList<Bitmap> imageBitmaps = new ArrayList<>();
    ListView listView;
    public boolean isGetFilesTaskPaused = false;
    GetFilesTask getFilesTask = new GetFilesTask();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        adapter = new FilePreviewAdapter(this, R.layout.filepreview_layout,listPreview);
        setListAdapter(adapter);

        Log.d(TAG, "adapter set up");

        listView = (ListView)((ConstraintLayout)findViewById(R.id.rootLayout)).getChildAt(0);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File f = files.get(position);
                Log.d(TAG, "position " + position);
                if(f.getMimeType().contains("folder")) {
                    getFilesTask.cancel(false);
                    currentFolder = f.getId();
                    Log.d(TAG, "opening folder " + currentFolder);
                    displayFiles();
                } else {
                    //isGetFilesTaskPaused = true;
                    Intent i = new Intent(ListActivity.this, DisplayFileActivity.class);
                    i.putExtra(FILE_ID, f.getId());
                    i.putExtra(CONTENT_LINK, f.getWebContentLink());
                    startActivity(i);

                }
            }
        });

        currentFolder = "root";
        displayFiles();

        AsyncTask t = new ThumbTask();
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    @Override
    protected void onResume() {
        super.onResume();
        isGetFilesTaskPaused = false;
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

        getFilesTask = new GetFilesTask();
        getFilesTask.execute();

    }

    public void displayResult(){
        adapter.notifyDataSetChanged();
        Log.d(TAG, "updated adapter");
    }




    private class GetFilesTask extends AsyncTask<Void,Void,Void>{

        ArrayList<FilePreview> tempListPreview = new ArrayList<FilePreview>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            listItems.clear();
            listPreview.clear();
            displayImageTasks.clear();
            imageBitmaps.clear();
            tempListPreview.clear();

            displayResult();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "started thread");

            Drive service = GenericActivity.getService();

            Log.d(TAG, "got service");

            FileList result = null;
            files = new ArrayList<File>();
            List<File> currentFiles;
            String pageToken = null;


            //*


            try {
                do {
                    if(!isGetFilesTaskPaused) {

                        result = service.files().list()
                                .setQ(String.format("'%s' in parents and trashed = false", currentFolder))
                                .setFields("nextPageToken, files(id, name, mimeType, thumbnailLink)")
                                .setPageToken(pageToken)
                                .setPageSize(10)
                                .execute();
                        files.addAll(result.getFiles());
                        currentFiles = result.getFiles();
                        for (File file : currentFiles) {

                            if(isCancelled()){
                                return null;
                            }

                            imageBitmaps.add(null);
                            displayImageTasks.add(new DisplayImageTask());
                            tempListPreview.add(new FilePreview(file, String.format("(%s) %s \n%s", file.getMimeType(), file.getName(), file.getWebContentLink())));
                            listItems.add(String.format("(%s) %s \n%s", file.getMimeType(), file.getName(), file.getWebContentLink()));

                        }
                        pageToken = result.getNextPageToken();
                        publishProgress();


                    }

                } while (pageToken != null && !(isCancelled()));
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

                /*
                listItems.clear();
                listPreview.clear();


                if (files == null || files.isEmpty()) {
                    Log.w(TAG, "No files found.");
                } else {

                    for (File file : files) {

                        listPreview.add(new FilePreview(file, String.format("(%s) %s \n%s", file.getMimeType(), file.getName(), file.getWebContentLink())));
                        listItems.add(String.format("(%s) %s \n%s", file.getMimeType(), file.getName(), file.getWebContentLink()));
                    }
                }
                */
            }


            //*/

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {


            listPreview.addAll(tempListPreview);
            tempListPreview.clear();
            displayResult();


            Log.d(TAG, "onProgressUpdate");
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

        @NonNull
        @Override
        public Context getContext() {
            return context;
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

            FilePreview preview = previews.get(position);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.filepreview_layout, null);
            ImageView icon = (ImageView) view.findViewById(R.id.thumb);
            TextView text = (TextView) view.findViewById(R.id.text);

            File file = preview.file;

            text.setText(preview.displayText);

            Log.d(TAG, "displayed text " + preview.displayText);

            Bitmap bitmap;
            bitmap = imageBitmaps.get(position);
            setImage(icon, bitmap);

            return view;

        }
    }

    private class DisplayImageTask extends AsyncTask<Object, Void, Bitmap> {
        ImageView icon;
        int position = -1;
        BufferedInputStream input;

        @Override
        protected Bitmap doInBackground(Object... params) {
            Log.d(TAG, "doInBackground: started display image task");
            Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND + THREAD_PRIORITY_MORE_FAVORABLE);

            File file = (File) params[0];
            icon = (ImageView) params[1];
            position = (int) params[2];

            Bitmap bitmap = null;
            try {
                if (file.getThumbnailLink() != null) {
                    //*
                    //URL url = new URL(URLEncoder.encode(file.getThumbnailLink(),"utf-8"));
                    URL url = new URL(file.getThumbnailLink());
                    Log.d(TAG, String.format("%s (%s)", file.getName(), file.getThumbnailLink()));
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    input = new BufferedInputStream(connection.getInputStream());
                    bitmap = BitmapFactory.decodeStream(input);
                    //*/
                    //bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.debug);
                }
            } catch (IOException e) {
                Log.e(TAG, "IO exception: " + e);
                e.printStackTrace();
            }

            Log.d(TAG, "doInBackground: returning bitmap");
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            try {
                setImage(icon, bitmap);
                imageBitmaps.set(position, bitmap);
            } catch(ArrayIndexOutOfBoundsException e){
                Log.e(TAG, "onPostExecute: ", e);
                e.printStackTrace();
            }
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG, "onCancelled");
            if(input != null) {
                input.mark(0);
                try {
                    input.reset();
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    public void setImage(ImageView v, Bitmap b){
        v.setImageBitmap(b);
        v.postInvalidate();

    }

    private class ThumbTask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            while (!isCancelled()){

                for(int position = listView.getFirstVisiblePosition(); position <= listView.getLastVisiblePosition(); position++) {
                    FilePreview preview = adapter.getItem(position);
                    File file = preview.file;

                    if (imageBitmaps.size() > position && imageBitmaps.get(position) == null && file.getThumbnailLink() != null) {

                        Context context = adapter.getContext();
                        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                        View view = inflater.inflate(R.layout.filepreview_layout, null);
                        ImageView icon = (ImageView) view.findViewById(R.id.thumb);

                        Log.d(TAG, "doInBackground: link " + file.getThumbnailLink());
                        DisplayImageTask t = displayImageTasks.get(position);
                        if (t.getStatus() == AsyncTask.Status.PENDING) {
                            try {
                                t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, file, icon, position);
                            } catch (java.util.concurrent.RejectedExecutionException e) {
                                e.printStackTrace();
                                Log.e(TAG, "getView: ", e);
                            }
                            displayImageTasks.set(position, new DisplayImageTask());
                        }


                    }

                }

                int firstHideIndex = listView.getFirstVisiblePosition();
                //Log.d(TAG, "getView: firstHideIndex " + firstHideIndex);

                int lastHideIndex = listView.getLastVisiblePosition();
                //Log.d(TAG, "getView: lastHideIndex " + lastHideIndex);


                for(int i = 0; i<displayImageTasks.size(); i++){
                    if(displayImageTasks.get(i) != null) {
                        if (i < firstHideIndex || i > lastHideIndex && displayImageTasks.get(i).getStatus() == AsyncTask.Status.RUNNING) {
                            //Log.d(TAG, "getView: cancelling task at " + i);
                            try {
                                displayImageTasks.get(i).cancel(true);
                                displayImageTasks.set(i, new DisplayImageTask());
                            } catch (IndexOutOfBoundsException e) {
                                Log.e(TAG, "doInBackground: ", e);
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            return null;
        }
    }
}
