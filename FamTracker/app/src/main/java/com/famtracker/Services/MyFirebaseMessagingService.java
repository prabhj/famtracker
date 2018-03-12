package com.famtracker.Services;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.famtracker.Constants;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by darshan on 17/03/17.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String TAG = "messaging TAG";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "onMessageReceived: "+remoteMessage);


        //notifyTheActivity(rider_id);

    }

 /*   private void notifyTheActivity(String rider_id) {

        Intent mIntent = new Intent(Constants.BROADCAST_CONSTANT);
        mIntent.putExtra("rider_id", rider_id);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(mIntent);
    }*/
/*

    private void getRiderDetails(String rider_id) {

        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference(Constants.RIDERS_DB);
        mDatabaseReference = mDatabaseReference.child(rider_id);

    }
*/


}
