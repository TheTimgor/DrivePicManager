package com.example.timgor.drivepicmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
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
import java.util.Deque;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.IntStream;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE;
import static com.example.timgor.drivepicmanager.GenericActivity.REQUEST_AUTHORIZATION;
import static com.example.timgor.drivepicmanager.GenericActivity.service;

public class ListActivity extends AppCompatActivity {

    private static final String TAG = "List Activity ~~ ";
    private FilePreviewAdapter adapter;
    ArrayList<FilePreview> listPreview = new ArrayList<FilePreview>();
    public static final String FILE_ID = "com.example.drivepicmanager.FILE_ID";
    public static final String CONTENT_LINK = "com.example.drivepicmanager.CONTENT_LINK";
    private String currentFolder = "root";
    private String currentActionFolder = null;
    private Stack<String> history = new Stack<>();
    ListView listView;
    public boolean isGetFilesTaskPaused = false;
    GetFilesTask getFilesTask = new GetFilesTask();
    Toolbar toolbar;
    public String currentAction = null;
    //DisplayImageTaskCanceller displayImageTaskCanceller = new DisplayImageTaskCanceller();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        adapter = new FilePreviewAdapter(this, R.layout.filepreview_layout,listPreview);
        listView = (ListView)findViewById(R.id.main_list);
        listView.setAdapter(adapter);

        Log.d(TAG, "adapter set up");

        history.push("root");
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File f = listPreview.get(position).file;
                Log.d(TAG, "position " + position);
                if(f.getMimeType().contains("folder")) {
                    currentFolder = f.getId();
                    Log.d(TAG, "opening folder " + currentFolder);
                    history.push(currentFolder);
                    Log.d(TAG, "history " + history);
                    displayFiles();
                } else {
                    if(currentAction == "move"){
                        moveFile(f.getId(), currentActionFolder);
                    } else if (currentAction == "copy") {
                        copyFile(f.getId(), currentActionFolder);
                    } else {
                        //isGetFilesTaskPaused = true;
                        Intent i = new Intent(ListActivity.this, DisplayFileActivity.class);
                        i.putExtra(FILE_ID, f.getId());
                        i.putExtra(CONTENT_LINK, f.getWebContentLink());
                        startActivity(i);
                    }

                }
            }
        });

        /*
        listView.setRecyclerListener(new AbsListView.RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                int position = listView.getPositionForView(view);
                if(position != -1){
                    listPreview.get(position).displayImageTask.cancel(true);
                    listPreview.get(position).displayImageTask = new DisplayImageTask();
                }

            }
        });
        */

        registerForContextMenu(listView);

        //displayImageTaskCanceller.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        currentFolder = "root";
        displayFiles();

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_preview, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;

        switch (item.getItemId()) {
            case R.id.trash:
                String fileId = listPreview.get(position).file.getId();
                trashFile(fileId);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;

            case R.id.action_cancel:
                currentAction = null;
                currentActionFolder = null;
                toolbar.setTitle("DrivePicManager");
                return true;

            case R.id.action_move:
                currentAction = "move";
                currentActionFolder = currentFolder;
                toolbar.setTitle(currentActionFolder);
                return true;

            case R.id.action_copy:
                currentAction = "copy";
                currentActionFolder = currentFolder;
                toolbar.setTitle(currentActionFolder);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void trashFile(String fileID){
        TrashFileTask t = new TrashFileTask();
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fileID);
    }

    public void moveFile(String fileID, String destFolderID){
        MoveFileTask t = new MoveFileTask();
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fileID, destFolderID);
    }

    public void copyFile(String fileID, String destFolderID){
        CopyFileTask t = new CopyFileTask();
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fileID, destFolderID);
    }

    private class TrashFileTask extends AsyncTask<String,Void,Void> {

        @Override
        protected Void doInBackground(String... IDs) {
            String fileId = IDs[0];
            try {
                service.files().delete(fileId).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            displayFiles();
            super.onPostExecute(aVoid);
        }
    }

    private class CopyFileTask extends AsyncTask<String,Void,File> {

        @Override
        protected File doInBackground(String... IDs) {
            String fileId = IDs[0];
            String folderId = IDs[1];

            File file = new File();

            try {
                file.setName("Copy of " + service.files().get(fileId).execute().getName());
                file = service.files().copy(fileId, file).setFields("id,parents").execute();
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: ", e);
            }
            StringBuilder previousParents = new StringBuilder();
            Log.d(TAG, "doInBackground: " + file.toString());
            for (String parent : file.getParents()) {
                previousParents.append(parent);
                previousParents.append(',');
            }
            try {
                file = service.files().update(file.getId(), null)
                        .setAddParents(folderId)
                        .setRemoveParents(previousParents.toString())
                        .setFields("id, parents")
                        .execute();
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: ", e);
            }
            Log.d(TAG, "doInBackground: original, copied " + fileId + ", " + file.getId());
            return file;
        }

        @Override
        protected void onPostExecute(File file) {
            displayResult();
            super.onPostExecute(file);
        }
    }

    private class MoveFileTask extends AsyncTask<String,Void,File> {

        @Override
        protected File doInBackground(String... IDs) {
            String fileId = IDs[0];
            String folderId = IDs[1];

            File file = null;
            try {
                file = service.files().get(fileId)
                        .setFields("parents")
                        .execute();
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: ",e );
            }
            StringBuilder previousParents = new StringBuilder();
            for (String parent : file.getParents()) {
                previousParents.append(parent);
                previousParents.append(',');
            }
            try {
                file = service.files().update(fileId, null)
                        .setAddParents(folderId)
                        .setRemoveParents(previousParents.toString())
                        .setFields("id, parents")
                        .execute();
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: ", e);
            }
            return file;
        }

        @Override
        protected void onPostExecute(File file) {
            displayFiles();
            super.onPostExecute(file);
        }
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

        //displayMessage("displaying files . . . ", Toast.LENGTH_LONG);


        if(getFilesTask != null) {
            getFilesTask.cancel(false);
        }
        getFilesTask = new GetFilesTask();
        getFilesTask.execute();

    }

    public void displayResult(){
        adapter.notifyDataSetChanged();
        Log.d(TAG, "updated adapter");
    }




    private class GetFilesTask extends AsyncTask<Void,Void,Void>{

        ArrayList<FilePreview> tempListPreview = new ArrayList<FilePreview>();
        List<File> files;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            /*
            for(DisplayImageTask t : displayImageTasks){
                t.cancel(true);
            }
            */

            listPreview.clear();
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
                result = service.files().list()
                        .setQ(String.format("'%s' in parents and trashed = false and mimeType = 'application/vnd.google-apps.folder'", currentFolder))
                        .setFields("nextPageToken, files(id, name, mimeType, thumbnailLink)")
                        .execute();
                files.addAll(result.getFiles());
                currentFiles = result.getFiles();
                for (File file : currentFiles) {

                    if(isCancelled()){
                        return null;
                    }

                    tempListPreview.add(new FilePreview(file, String.format("(%s) %s", file.getMimeType(), file.getName())));

                }
                do {
                    result = service.files().list()
                            .setQ(String.format("'%s' in parents and trashed = false and mimeType != 'application/vnd.google-apps.folder'", currentFolder))
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

                        tempListPreview.add(new FilePreview(file, String.format("(%s) %s", file.getMimeType(), file.getName())));

                    }
                    pageToken = result.getNextPageToken();
                    publishProgress();

                } while (pageToken != null && !(isCancelled()));
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);

            } catch (IOException e) {
                Log.e(TAG,"IO exception: ", e);
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
            displayMessage(String.format("Displayed %s files", files.size()));
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
        public DisplayImageTask displayImageTask;
        public Bitmap bitmap;

        private FilePreview(File file, String displayText) {
            this.file = file;
            this.displayText = displayText;
            this.displayImageTask = new DisplayImageTask();
            this.bitmap = null;
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
        public FilePreview getItem(int position){
            if(previews.size() > position) {
                return previews.get(position);
            } else {
                return null;
            }
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
            Log.d(TAG, "getView: " + previews.get(position).displayImageTask.toString());


            Bitmap bitmap;
            if(file.getMimeType().contains("folder")) {
                bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.folder);
            } else {
                bitmap = previews.get(position).bitmap;
            }
            setImage(icon, bitmap);

            if (previews.get(position).displayImageTask.getStatus() == AsyncTask.Status.PENDING) {
                previews.get(position).displayImageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, file, icon, position);
            } else {
                Log.d(TAG, "getView: task not pending");
            }



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
                Log.e(TAG, "IO exception: ", e);
                e.printStackTrace();
            }

            Log.d(TAG, "doInBackground: returning bitmap");
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            try {
                setImage(icon, bitmap);
                if(position<listPreview.size()) {
                    listPreview.get(position).bitmap = bitmap;
                }
            } catch(ArrayIndexOutOfBoundsException e){
                Log.e(TAG, "onPostExecute: ", e);
                e.printStackTrace();
            }
        }

        @Override
        protected void onCancelled() {
            //Log.d(TAG, "onCancelled");
            if(input != null) {
                input.mark(0);
                try {
                    input.reset();
                    input.close();
                } catch (IOException e) {
                    Log.e(TAG, "onCancelled: ",e );
                } catch (NetworkOnMainThreadException e){
                    Log.e(TAG, "onCancelled: ", e);
                }
            }


        }

    }

    public void setImage(ImageView v, Bitmap b){
        v.setImageBitmap(b);
        v.invalidate();
        adapter.notifyDataSetChanged();

    }
    /*
    private class DisplayImageTaskCanceller extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            while(!isCancelled()){
                Log.d(TAG, "doInBackground: listpreview size " + listPreview.size());
                for(int position = 0; position<listPreview.size(); position++) {
                    if (position <= listView.getFirstVisiblePosition() || position >= listView.getLastVisiblePosition()){
                        if (listPreview.get(position).displayImageTask.getStatus() != Status.PENDING) {
                            Log.d(TAG, "doInBackground: cancelling task at " + position);
                            try {
                                listPreview.get(position).displayImageTask.cancel(true);
                            } catch (Exception e) { // keep it classy
                                Log.e(TAG, "doInBackground: oopsie woopsie ", e);
                            }
                        }
                    }
                }
            }
            return null;
        }
    }
    */
    @Override
    public void onBackPressed() {
        if(currentFolder == "root") {
            for (int i = 0; i < listPreview.size(); i++) {
                listPreview.get(i).displayImageTask.cancel(true);
            }
            getFilesTask.cancel(true);
            super.onBackPressed();
        } else {
            history.pop();
            currentFolder = history.peek();
            getFilesTask.cancel(true);
            Log.d(TAG, "history " + history);
            displayFiles();
        }
    }
}
