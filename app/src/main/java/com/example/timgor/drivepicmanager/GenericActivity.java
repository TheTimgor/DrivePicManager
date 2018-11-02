package com.example.timgor.drivepicmanager;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.AccountChangeEvent;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public abstract class GenericActivity extends AppCompatActivity {
    private static final String TAG = "Generic activity ~~";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    protected static Drive service;
    protected static GoogleSignInAccount account;
    private static Account mAccount;

    protected static final int RC_REQUEST_PERMISSION_SUCCESS_CONTINUE_SIGNIN = 0;
    protected static final int RC_SIGN_IN = 1;
    protected static final int REQUEST_AUTHORIZATION = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public void setUpAPI(){
        if (!GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(getActivity()),
                com.google.android.gms.drive.Drive.SCOPE_APPFOLDER)) {
            GoogleSignIn.requestPermissions(
                    GenericActivity.this,
                    RC_REQUEST_PERMISSION_SUCCESS_CONTINUE_SIGNIN,
                    GoogleSignIn.getLastSignedInAccount(getActivity()),
                    com.google.android.gms.drive.Drive.SCOPE_APPFOLDER);
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        Log.d(TAG, String.valueOf(signInIntent));
        startActivityForResult(signInIntent, RC_SIGN_IN);


    }



    public static Drive getService(){
        return service;
    }

    private Context getActivity() {
        return this.getApplicationContext();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.w(TAG, "activity returned result");

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Log.w("Sign in flow", "activity returned result");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            account = completedTask.getResult(ApiException.class);
            //updateUI(account);
            Log.w("Sign in flow", "fetched account");
            mAccount = account.getAccount();
            displayMessage(mAccount.toString());

            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            GenericActivity.this,
                            Collections.singleton(
                                    "https://www.googleapis.com/auth/drive")
                    );
            credential.setSelectedAccount(mAccount);
            service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName("REST API sample")
                    .build();
            displayMessage(service.toString());

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("Sign in flow", "signInResult:failed code=" + e.getStatusCode());
        }
    }


}
