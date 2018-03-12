package com.famtracker.Activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.famtracker.Models.User;
import com.famtracker.R;
import com.famtracker.Constants;
import com.famtracker.Services.LocationMonitoringService;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private final String TAG = "firebase TAG";
    private static final int RC_SIGN_IN = 1234;

    AutoCompleteTextView emailEt;
    EditText passwordEt;

    private FirebaseAuth mAuth;

    GoogleSignInOptions gso;
    GoogleApiClient mGoogleApiClient;
    CallbackManager mCallbackManager;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();

    }

    //initialisations
    private void init() {


        //fireBase auth
        mAuth = FirebaseAuth.getInstance();

        //google sign in options
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        emailEt = (AutoCompleteTextView) findViewById(R.id.email);
        passwordEt = (EditText) findViewById(R.id.password);

        //onClick for google sign in
        findViewById(R.id.googleSignInButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignUp(v);
            }
        });


        initFbLogin();


    }


    private void initFbLogin() {


        mCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d("Success", "Login");
                        handleFacebookAccessToken(loginResult.getAccessToken());

                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(LoginActivity.this, "Login Cancel", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Toast.makeText(LoginActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

    }


    //sign-up for email and password onClick
    public void emailSignUp(View view) {

        final String email = emailEt.getText().toString();
        final String password = passwordEt.getText().toString();

        if (validateEmail()) {
            if (validatePassword()) {

                mAuth.fetchProvidersForEmail(emailEt.getText().toString()).addOnCompleteListener(new OnCompleteListener<ProviderQueryResult>() {
                    @Override
                    public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                        Log.d(TAG, "onComplete: " + task.getResult().getProviders());

                        //to check if email already exists
                        if (task.getResult().getProviders().size() > 0) {
                            emailSignIn(email, password);
                        } else {
                            emailSignUp(email, password);
                        }

                    }
                });


            } else {
                Toast.makeText(this, "Password not strong enough", Toast.LENGTH_SHORT).show();
            }

        }


    }

    public void emailSignUp(String email, String password) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Sign up failed!",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            //TODO open main activity

                            //  verifyNumber();
                            goToNextActivity();
                        }


                    }


                });
    }

    private void sendEmailVerification() {

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(LoginActivity.this, "Verification email has been sent, please verify!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginActivity.this, "Failed to send verification email...", Toast.LENGTH_SHORT).show();
                    }
                });

    }


    public void emailSignIn(String email, String password) {

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Auth failed",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // verifyNumber();
                            goToNextActivity();
                        }
                    }
                });

    }

    //next activity after successful signIn
    private void goToNextActivity() {

        //store user info
        storeUserInDatabase();
        sendEmailVerification();
        if(checkLocationPermission()){
            startServiceAndActivity();
        }


/*

        Intent mIntent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(mIntent);
        finish();
*/

    }


    private void startLocationService() {
        Intent mIntent = new Intent(LoginActivity.this, LocationMonitoringService.class);
        startService(mIntent);
    }

    //to store user info after successful login and number verification
    private void storeUserInDatabase() {

        Log.d(TAG, "storeUserInDatabase: ");

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        String fcm_token = Constants.getToken(getApplicationContext());

        User mUser = new User();
        if (firebaseUser != null) {
            mUser.setEmail(firebaseUser.getEmail());
            mUser.setName(firebaseUser.getDisplayName());
            mUser.setPhotoUrl(firebaseUser.getPhotoUrl());
            mUser.setFcm_token(fcm_token);
            //push to firebase
            mUser.writeNewUser(firebaseUser.getUid());
        }


    }

    //google sign in onClick
    public void googleSignUp(View view) {

        Log.d(TAG, "googleSignUp: clicked");

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);

    }


    //password validation
    private boolean validatePassword() {

        return passwordEt.getText().toString().length() >= 8;

    }


    //email validation logic
    private boolean validateEmail() {

        if (emailEt.getText().toString().equals("")) {
            Toast.makeText(this, "Enter something!!", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!emailEt.getText().toString().contains("@") || !emailEt.getText().toString().endsWith(".com")) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;

    }


    //if connection fails during signup
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //fb
        mCallbackManager.onActivityResult(requestCode, resultCode, data);

        //if google sign in successful
        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "onActivityResult: signed in");
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with FireBase
                GoogleSignInAccount account = result.getSignInAccount();
                fireBaseAuthWithGoogle(account);
            } else {
                Toast.makeText(this, "Please Sign In to continue further", Toast.LENGTH_SHORT).show();
            }
        }

    }

    //storing G auth to fireBase
    private void fireBaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "AuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
//                            verifyNumber();
                            goToNextActivity();
                        }
                    }
                });
    }


    //storing fb auth to fireBase
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            goToNextActivity();
                        }

                    }
                });
    }


    public void resetPasswordOnClick(View view) {

        if (validateEmail()) {

            AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(LoginActivity.this);
            mAlertDialog
                    .setTitle("Reset password")
                    .setMessage("Password reset link will be sent to " + emailEt.getText().toString() + " ")
                    .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mAuth.sendPasswordResetEmail(emailEt.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(LoginActivity.this, "Reset email sent!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            mAlertDialog.show();


        }

    }

    public void facebookSignUp(View view) {

        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "user_friends"));

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {

            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted.

                    //start service and goto next screen
                   startServiceAndActivity();


                } else {
                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
            }

        }
    }

    private void startServiceAndActivity() {

        startLocationService();

        FirebaseMessaging.getInstance().subscribeToTopic("FT");

        Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();
        Intent mIntent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(mIntent);
        finish();


    }
}

