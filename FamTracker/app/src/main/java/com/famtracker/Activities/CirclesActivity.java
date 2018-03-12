package com.famtracker.Activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.famtracker.Adapters.CirclesAdapter;
import com.famtracker.Constants;
import com.famtracker.Models.Circles;
import com.famtracker.Models.Members;
import com.famtracker.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;

public class CirclesActivity extends AppCompatActivity {

    RecyclerView mRecyclerView;
    ArrayList<Circles> mArrayList = new ArrayList<>();
    CirclesAdapter mCirclesAdapter;

    ProgressDialog mProgressDialog;

    String TAG = "CirclesAct TAG";

    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(Constants.USERS_DB);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circles);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNewCircle();
            }


        });

        initProgressDialog();
        fetchCirclesData();
        initRecView();
    }

    private void initProgressDialog() {

        mProgressDialog = new ProgressDialog(CirclesActivity.this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle("Loading");
        mProgressDialog.setMessage("Please wait...");
        mProgressDialog.setCancelable(false);

    }

    private void sendEmailVerification() {

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(CirclesActivity.this, "Verification email has been sent, please verify!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CirclesActivity.this, "Failed to send verification email...", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void addNewCircle() {

//        FirebaseUser mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (!mFirebaseUser.isEmailVerified()) {
//            Log.d(TAG, "addNewCircle: not verified");
//            AlertDialog.Builder alerBuilder = new AlertDialog.Builder(CirclesActivity.this);
//            alerBuilder.setTitle("Verification!")
//                    .setMessage("Please verify your email ID to continue further!")
//                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
//                        }
//                    }).setNeutralButton("Resend Email", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    sendEmailVerification();
//                }
//            }).setCancelable(false)
//                    .show();
//        } else {

        View view = LayoutInflater.from(CirclesActivity.this).inflate(R.layout.create_circle_layout, (ViewGroup) findViewById(R.id.root_layout));
        final TextInputEditText circleName = (TextInputEditText) view.findViewById(R.id.circleName);
        final TextInputEditText circleDescription = (TextInputEditText) view.findViewById(R.id.circleDescription);


        AlertDialog.Builder alBuilder = new AlertDialog.Builder(CirclesActivity.this);
        alBuilder.setView(view).setTitle("Create new circle").setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String sCircleName = circleName.getText().toString();
                String sCircleDescription = circleDescription.getText().toString();

                createCircle(sCircleName, sCircleDescription);

                Toast.makeText(CirclesActivity.this, "You will be added to the circle automatically!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();


            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setCancelable(false).show();

//        }

    }

    private void createCircle(String sCircleName, String sCircleDescription) {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        final String key = databaseReference.child(Constants.CIRCLES_DB).push().getKey();

        //current user
        Members member = new Members(Constants.getUid(), Constants.getName(), Constants.getEmail(), Constants.getPhotoUrl());

        HashMap<String, Object> mainCircle = new HashMap<>();
        mainCircle.put(Constants.getUid(), member);

        //new circle
        final Circles circles = new Circles(sCircleName, sCircleDescription, key);

        HashMap<String, Object> updateHashMap = new HashMap<>();
        updateHashMap.put("/" + Constants.CIRCLES_DB + "/" + key, mainCircle);
        updateHashMap.put("/" + Constants.USERS_DB + "/" + Constants.getUid() + "/circles/" + key, circles);

        databaseReference.updateChildren(updateHashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "onComplete: added new circle with key " + key);

                mArrayList.add(circles);
                mCirclesAdapter.notifyItemInserted(mArrayList.size() - 1);
                //check if this is the first circle
                if (PreferenceManager.getDefaultSharedPreferences(CirclesActivity.this).getString(Constants.DEFAULT_CIRCLE_ID, null) == null) {
                    PreferenceManager.getDefaultSharedPreferences(CirclesActivity.this).edit().putString(Constants.DEFAULT_CIRCLE_ID, circles.getId())
                            .putString(Constants.DEFAULT_CIRCLE_NAME, circles.getCircleName()).apply();
                }

                //add him to that topic , topic name -> circle ID
                FirebaseMessaging.getInstance().subscribeToTopic(key);


            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: creating new circle failed");
                Toast.makeText(CirclesActivity.this, "Couldn't create the circle, please try again!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void fetchCirclesData() {

        showProgressBar();

        databaseReference.child(Constants.getUid()).child("circles")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        dismissProgressBar();

                        if (!dataSnapshot.hasChildren()) {
                            Toast.makeText(CirclesActivity.this, "You are not in any circles, Add a new one", Toast.LENGTH_SHORT).show();
                        } else {
                            for (DataSnapshot mDataSnapshot : dataSnapshot.getChildren()) {
                                Circles mCircles = mDataSnapshot.getValue(Circles.class);
                                mArrayList.add(mCircles);
                            }

                            mCirclesAdapter.notifyDataSetChanged();
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(CirclesActivity.this, "Loading Cancelled", Toast.LENGTH_SHORT).show();
                    }
                });


    }

    private void showProgressBar() {
        mProgressDialog.show();
    }

    private void dismissProgressBar() {
        mProgressDialog.dismiss();
    }

    private void initRecView() {

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(CirclesActivity.this);
        mRecyclerView.setLayoutManager(layoutManager);

        mCirclesAdapter = new CirclesAdapter(CirclesActivity.this, mArrayList);
        mRecyclerView.setAdapter(mCirclesAdapter);

    }
}
