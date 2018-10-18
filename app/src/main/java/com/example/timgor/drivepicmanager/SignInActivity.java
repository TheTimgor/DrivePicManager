package com.example.timgor.drivepicmanager;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.drive.Drive;


public class SignInActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);


        if (!GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(getActivity()),
                Drive.SCOPE_APPFOLDER)) {
            GoogleSignIn.requestPermissions(
                    MyExampleActivity.this,
                    RC_REQUEST_PERMISSION_SUCCESS_CONTINUE_FILE_CREATION,
                    GoogleSignIn.getLastSignedInAccount(getActivity()),
                    Drive.SCOPE_APPFOLDER);
        } else {
            saveToDriveAppFolder();
        }

    }

    private Context getActivity() {
        return this.getApplicationContext();
    }


}
