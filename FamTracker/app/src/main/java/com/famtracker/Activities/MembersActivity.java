package com.famtracker.Activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
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
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.famtracker.Adapters.MembersAdapter;
import com.famtracker.Constants;
import com.famtracker.Models.Circles;
import com.famtracker.Models.Members;
import com.famtracker.Models.User;
import com.famtracker.R;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MembersActivity extends AppCompatActivity {

    String TAG = "MembersAct TAG";


    RecyclerView mRecyclerView;
    MembersAdapter mMembersAdapter;
    ArrayList<Members> membersArrayList = new ArrayList<>();
    String CircleId, CircleName, CircleDescription;

    DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference(Constants.CIRCLES_DB);

    ProgressDialog mProgressDialog;
    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_members);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                addNewMember();

            }
        });

        CircleId = getIntent().getStringExtra("id");
        CircleName = getIntent().getStringExtra("name");
        CircleDescription = getIntent().getStringExtra("description");
        initProgressDialog();
        fetchData();
        initRecyclerView();


    }

    private void addNewMember() {

        View view = LayoutInflater.from(MembersActivity.this).inflate(R.layout.create_new_member, (ViewGroup) findViewById(R.id.root_layout));
        final TextInputEditText editText = (TextInputEditText) view.findViewById(R.id.memberId);
        AlertDialog.Builder alBuilder = new AlertDialog.Builder(MembersActivity.this);
        alBuilder.setView(view)
                .setTitle("Add member")
                .setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        showProgressBar();
                        addMember(editText.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setCancelable(false).show();
    }

    private void addMember(final String emailId) {

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (validateEmail(emailId)) {

            mAuth.fetchProvidersForEmail(emailId).addOnCompleteListener(new OnCompleteListener<ProviderQueryResult>() {
                @Override
                public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                    Log.d(TAG, "onComplete: " + task.getResult().getProviders());

                    //to check if email already exists
                    if (task.getResult().getProviders().size() > 0) {

                        performAddingMember(emailId);

                    } else {
                        Snackbar.make(fab, "Email id not registered", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        dismissProgressBar();
                    }

                }
            });


        } else {
            Snackbar.make(fab, "Enter a valid email id", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            dismissProgressBar();
        }


    }

    private void performAddingMember(final String emailId) {

        RequestQueue queue = Volley.newRequestQueue(MembersActivity.this);

        String url = "https://us-central1-familytracker-53602.cloudfunctions.net/addMember?email=" + emailId + "&circle=" + CircleId;

        StringRequest stringRequest = new StringRequest(StringRequest.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                final String uid = response;

                final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                databaseReference.child(Constants.USERS_DB).child(uid).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        dismissProgressBar();

                        String name = dataSnapshot.child("name").getValue(String.class);
                        String email = dataSnapshot.child("email").getValue(String.class);
                        String photoUrl = dataSnapshot.child("photoUrl").getValue(String.class);
                        final Members member = new Members(uid, name, email, photoUrl);

                        Circles circles = new Circles(CircleName, CircleDescription, CircleId);
                       /* HashMap<String,Object> circleHashMap = new HashMap<String, Object>();
                        circleHashMap.put(CircleId,circles);


                        HashMap<String, Object> mainCircle = new HashMap<>();
                        mainCircle.put(uid, member);*/

                        HashMap<String, Object> mainHash = new HashMap<>();
                        mainHash.put(Constants.USERS_DB + "/" + uid + "/circles/" + CircleId, circles);
                        mainHash.put(Constants.CIRCLES_DB + "/" + CircleId + "/" + uid, member);


                        databaseReference
                                .updateChildren(mainHash).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Log.d(TAG, "onComplete: added member to circle");

                                if (!membersArrayList.contains(member)) {
                                    membersArrayList.add(member);
                                    mMembersAdapter.notifyItemInserted(membersArrayList.size() - 1);
                                }

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "onFailure: " + e);
                            }
                        });

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                        Log.d(TAG, "onCancelled: ");
                        dismissProgressBar();
                        Toast.makeText(MembersActivity.this, "Error while loading!", Toast.LENGTH_SHORT).show();

                    }
                });

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "onErrorResponse: " + error);
                Snackbar.make(fab, "Error", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                dismissProgressBar();
            }
        });


        queue.add(stringRequest);

    }

    private void initProgressDialog() {

        mProgressDialog = new ProgressDialog(MembersActivity.this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle("Loading");
        mProgressDialog.setMessage("Please wait...");
        mProgressDialog.setCancelable(false);
    }

    private void fetchData() {

        showProgressBar();

        mDatabaseReference.child(CircleId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                dismissProgressBar();
                Log.d(TAG, "onDataChange: " + dataSnapshot);
                if (!dataSnapshot.hasChildren()) {
                    Toast.makeText(MembersActivity.this, "There are no members in the circle, add some!", Toast.LENGTH_SHORT).show();
                } else {
                    for (DataSnapshot mSnapshot : dataSnapshot.getChildren()) {
                        Members members = mSnapshot.getValue(Members.class);
                        membersArrayList.add(members);
                    }
                    mMembersAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MembersActivity.this, "Loading cancelled", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initRecyclerView() {

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(MembersActivity.this);
        mRecyclerView.setLayoutManager(layoutManager);

        mMembersAdapter = new MembersAdapter(MembersActivity.this, membersArrayList);
        mRecyclerView.setAdapter(mMembersAdapter);

    }

    private void showProgressBar() {
        mProgressDialog.show();
    }

    private void dismissProgressBar() {
        mProgressDialog.dismiss();
    }

    //email validation logic
    private boolean validateEmail(String emailID) {

        if (emailID.equals("")) {
            Toast.makeText(this, "Enter something!!", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!emailID.contains("@") || !emailID.endsWith(".com")) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;

    }

}
