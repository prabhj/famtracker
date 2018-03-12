package com.famtracker.Models;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.famtracker.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

/**
 * Created by darshan on 26/04/17.
 */

public class User {


    private static final String TAG = "user TAG";

    String name, email;
    String photoUrl, fcm_token;

    public User(String name, String email, Uri photoUrl, String fcm_token) {
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl.toString();
        this.fcm_token = fcm_token;
    }

    public String getFcm_token() {
        return fcm_token;
    }

    public void setFcm_token(String fcm_token) {
        this.fcm_token = fcm_token;
    }

    public User() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }


    public void setPhotoUrl(Uri photoUrl) {
        try {
            this.photoUrl = photoUrl.toString();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    public void writeNewUser(String uid) {

        User mUser = this;

        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference(Constants.USERS_DB);
        /*mDatabaseReference.child(uid).setValue(mUser)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "onComplete: user data stored successfully");
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onComplete: failed to store user data");
            }
        });
*/
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(uid+"/email", mUser.getEmail());
        hashMap.put(uid+"/fcm_token", mUser.getFcm_token());
        hashMap.put(uid+"/name", mUser.getName());
        hashMap.put(uid+"/photoUrl", mUser.getPhotoUrl());
        mDatabaseReference.updateChildren(hashMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "onComplete: user data stored successfully");
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onComplete: failed to store user data");
            }
        });



        //mDatabaseReference.child(uid).setValue(mUser)

    }

}

