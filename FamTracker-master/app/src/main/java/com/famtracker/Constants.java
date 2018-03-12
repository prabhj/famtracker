package com.famtracker;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by prabhjot on 26/04/17.
 */

public class Constants {

    public static final String PACKAGE_ID = "com.famtracker";
    public static final String FCM_TOKEN = PACKAGE_ID + ".fcm_token";
    public static final String UID_PREF = PACKAGE_ID + ".uid";


    public static final String USERS_DB = "Users";
    public static final String CIRCLES_DB = "Circles";
    public static final String LOCATIONS_DB = "Locations";

    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    private static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    public static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    public static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    public static final String RUNNING = "runningInBackground"; // Recording data in background


    public static String getToken(Context context) {

        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(Constants.FCM_TOKEN, "NA");


    }

    public static void removeLocation() {

        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mDatabaseReference.child(Constants.LOCATIONS_DB).child(Constants.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d("TAG", "onComplete: location removed");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("TAG", "onFailure: location removal failed");
            }
        });


    }

    public static final String Locations_DB = "Locations";

    public static String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public static String getName() {
        String name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (name != null) {
            return name;
        } else {
            return FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }
    }

    public static String getEmail() {
        return FirebaseAuth.getInstance().getCurrentUser().getEmail();
    }

    public static String getPhotoUrl(){
        return  FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl().toString();
    }

}
