package com.famtracker.Activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.famtracker.Constants;
import com.famtracker.Models.Circles;
import com.famtracker.Models.Members;
import com.famtracker.R;
import com.famtracker.Services.LocationMonitoringService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    ArrayList<String> fcm_tokens_list = new ArrayList<>();
    ArrayList<String> topics = new ArrayList<>();
    ArrayList<Circles> circlesArrayList = new ArrayList<>();


    String TAG = "MainAct TAG";

    private GoogleMap mMap;
    LatLng newLatLng;
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    TextView nameTv, emailTv, numberOfMembers;
    CircleImageView imageView;
    ImageLoader imageLoader;
    DisplayImageOptions options;

    ProgressDialog mProgressDialog;

    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main2);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }


        numberOfMembers = (TextView) findViewById(R.id.tv);
        initProgressDialog();
        fetchAllUsers();
        fetchCirclesData();
        initNavigationBar();
        initMap();
        initUil();
        setHeaderContent();
        getTopics();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private void initUil() {

        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheOnDisc(true).cacheInMemory(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .displayer(new FadeInBitmapDisplayer(300)).build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                getApplicationContext())
                .defaultDisplayImageOptions(defaultOptions)
                .memoryCache(new WeakMemoryCache())
                .discCacheSize(100 * 1024 * 1024).build();

        ImageLoader.getInstance().init(config);

        dispImg();
    }

    private void dispImg() {

        imageLoader = ImageLoader.getInstance();
        options = new DisplayImageOptions.Builder().cacheInMemory(true)
                .cacheOnDisc(true).resetViewBeforeLoading(true)
                .build();
    }

    private void initProgressDialog() {

        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle("Loading");
        mProgressDialog.setMessage("Please wait...");
        mProgressDialog.setCancelable(false);

    }

    private void fetchAllUsers() {

        try {
            mMap.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final ArrayList<Members> membersArrayList = new ArrayList<>();

        showProgressBar();

        String default_circle = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(Constants.DEFAULT_CIRCLE_ID, "NA");
        databaseReference.child(Constants.CIRCLES_DB).child(default_circle).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChildren()) {
                    Toast.makeText(MainActivity.this, "You are not in any circles!", Toast.LENGTH_SHORT).show();
                    numberOfMembers.setText("0 members");
                    dismissProgressBar();
                } else {
                    for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                        Members members = dataSnapshot1.getValue(Members.class);
                        membersArrayList.add(members);
                    }
                    fetchMembersLocations(membersArrayList);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Loading Cancelled!", Toast.LENGTH_SHORT).show();
                dismissProgressBar();
            }
        });


    }

    private void fetchMembersLocations(ArrayList<Members> membersArrayList) {

        final ArrayList<String> uidsList = new ArrayList<>();
        for (Members members : membersArrayList) {
            uidsList.add(members.getUid());
        }

        databaseReference.child(Constants.LOCATIONS_DB).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (!dataSnapshot.hasChildren()) {
                    Toast.makeText(MainActivity.this, "No Members Available!", Toast.LENGTH_SHORT).show();
                    dismissProgressBar();
                } else {
                    int count = 0;
                    for (DataSnapshot mDataSnapshot : dataSnapshot.getChildren()) {
                        if (uidsList.contains(mDataSnapshot.getKey())) {

                            double lat = mDataSnapshot.child("lat").getValue(Double.class);
                            double lon = mDataSnapshot.child("lng").getValue(Double.class);
                            String name = mDataSnapshot.child("name").getValue(String.class);

                            addMarker(lat, lon, name);
                            count++;
                        }
                    }
                    numberOfMembers.setText(count + " members");
                    dismissProgressBar();

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(12.899829, 77.573682), 14.0f));

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Loading Cancelled!", Toast.LENGTH_SHORT).show();
                dismissProgressBar();
            }
        });


    }



   /* private void getAllData() {

        showProgressBar();

        databaseReference.child(Constants.USERS_DB).child(Constants.getUid()).child("circles")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.hasChildren()) {
                            Toast.makeText(MainActivity.this, "You are not in any circles, join any!", Toast.LENGTH_SHORT).show();
                        } else {
                            for (DataSnapshot circleSnapshot : dataSnapshot.getChildren()) {
//                                Circles circles = circleSnapshot.getValue(Circles.class);
                                String circleId = circleSnapshot.getKey();

                                databaseReference.child(Constants.CIRCLES_DB).child(circleId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot innerDataSnapshot) {
                                        if (!innerDataSnapshot.hasChildren()) {
                                            Toast.makeText(MainActivity.this, "One of your circles have no members!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            for (DataSnapshot membersSnapshot : innerDataSnapshot.getChildren()) {
                                                String token = membersSnapshot.child("fcm_token").getValue(String.class);
                                                Log.d("TAG", "onDataChange: " + membersSnapshot.child("name").getValue(String.class));
                                                fcm_tokens_list.add(token);
                                            }

                                            dismissProgressBar();
                                            sendNotificationsToEveryone(fcm_tokens_list);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Toast.makeText(MainActivity.this, "Loading cancelled!", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(MainActivity.this, "Loading cancelled!", Toast.LENGTH_SHORT).show();
                    }
                });

    }*/

    private void initNavigationBar() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        Log.d("header content", "setHeaderContent: " + headerView);

        nameTv = (TextView) headerView.findViewById(R.id.userEmail);
        emailTv = (TextView) headerView.findViewById(R.id.userName);
        imageView = (CircleImageView) headerView.findViewById(R.id.imageView);

    }

    private void setHeaderContent() {

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String image = firebaseUser.getPhotoUrl() + "";
        String name = Constants.getName();
        Log.d("header content", "setHeaderContent: " + image + "" + name + " " + firebaseUser.getEmail());
        if (name != null) {
            nameTv.setText(name);
        }
        emailTv.setText(firebaseUser.getEmail());
        if (image != null || !(image.equals(" "))) {
            imageLoader.displayImage(image, imageView, options);
        } else {
            imageView.setImageResource(R.drawable.user);
            Log.d("TAG", "setHeaderContent: background");
        }
    }


    private void initMap() {

        //initially loading the map view
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.sos) {
            notifyEveryone();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void notifyEveryone() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog
                .setTitle("Notify All")
                .setMessage("Send SOS to everyone?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        getAllData();

                        sendNotificationsToEveryone();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setCancelable(false).show();


    }

    private void sendNotificationsToEveryone() {

//        ArrayList<String> topics = getTopics();

        // write all the parameters into JSON

        /*String condition = "";
        condition = "'"+topics.get(0)+"' in topics";

        for(int i = 1;i < topics.size();i++){
            condition = condition +" || '"+topics.get(i)+"' in topics";
        }

        Log.d(TAG, "sendNotificationsToEveryone: "+condition);
*/
        JSONObject data = new JSONObject();

        JSONObject main = new JSONObject();

        JSONObject notification = new JSONObject();

        String from = PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                .getString(Constants.getToken(MainActivity.this), "NA");

        try {
            data.put("title", " SOS ");
            data.put("body", Constants.getName() + " has sent an SOS");
            data.put("from_id", from);
            data.put("name", Constants.getName());
            data.put("lat", newLatLng.latitude + "");
            data.put("lng", newLatLng.longitude + "");

            notification.put("title", " SOS ");
            notification.put("body", Constants.getName() + " has sent an SOS");
            notification.put("click_action", "NOTIFY_ACTIVITY");

            main.put("to", "/topics/FT");

//            main.put("condition", condition);


            main.put("data", data);
            main.put("notification", notification);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://fcm.googleapis.com/fcm/send";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                main,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "onResponse() called with: " + "response = [" + response + "]");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse() called with: " + "error = [" + error + "]");
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "key=" + getResources().getString(R.string.API_KEY));
                params.put("Content-Type", "application/json");

                return params;
            }
        };


        queue.add(jsonObjectRequest);


    }

    private void getTopics() {


        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(Constants.USERS_DB).child(Constants.getUid()).child("circles");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChildren()) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        topics.add(ds.getKey());
                    }

                } else {
                    Toast.makeText(MainActivity.this, "You are not in any circles!", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Loading Cancelled!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_about) {
            showAbout();
        } else if (id == R.id.nav_circle) {
            addNewCircle();
        } else if (id == R.id.nav_people) {
            callSelectCircle();
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_logout) {
            stopService(new Intent(MainActivity.this, LocationMonitoringService.class));
            Constants.removeLocation();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            //  Constants.removeToken(MainActivity.this);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showAbout() {

        Element versionElement = new Element();
        versionElement.setTitle("Version 1.2");

        View aboutPage = new AboutPage(this)
                .isRTL(false)
                .setDescription(getResources().getString(R.string.about_content))
                .setImage(R.drawable.famlogo)
                .addItem(versionElement)
                .addGroup("Connect with us")
                .addEmail("famtracker45@gmail.com")
                .addFacebook("DarshanGowda.399")
                .addYoutube("UCdPQtdWIsg7_pi4mrRu46vA")
                .addPlayStore("com.ideashower.readitlater.pro")
                .addGitHub("medyo")
                .addInstagram("medyo80")
                .create();

        AlertDialog.Builder alBuilder = new AlertDialog.Builder(MainActivity.this);
        alBuilder.setView(aboutPage).show();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

//        LatLng start = new LatLng(12.908478, 77.564529);
//        mMap.addMarker(new MarkerOptions().position(start));
//        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(start, 12.0f));

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

//        addMarker(12.981647, 77.694089,"DELL");


    }

    public void addMarker(double lat, double lon, String name) {

        if (mMap != null) {
            LatLng start = new LatLng(lat, lon);
            mMap.addMarker(new MarkerOptions().title(name).position(start));
        }

    }

    private void showProgressBar() {
        mProgressDialog.show();
    }

    private void dismissProgressBar() {
        mProgressDialog.dismiss();
    }

    public void CirclesOnClick(View view) {
        Intent mIntent = new Intent(MainActivity.this, CirclesActivity.class);
        startActivity(mIntent);
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

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

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {
                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        newLatLng = new LatLng(location.getLatitude(), location.getLongitude());
    }


    /*
    *
    * private void sendMessage(ChatMessage chatMessage, String to) {


        // write all the parameters into JSON
        JSONObject data = new JSONObject();

        JSONObject main = new JSONObject();

        String from = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(Constants.TOKEN_ID, "");

        try {
            data.put("message", chatMessage.getContent());
            data.put("from_id", from);

            main.put("to", to);
            main.put("data", data);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://fcm.googleapis.com/fcm/send";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                main,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(Constants.TAG, "onResponse() called with: " + "response = [" + response + "]");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(Constants.TAG, "onErrorResponse() called with: " + "error = [" + error + "]");
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "key=" + getResources().getString(R.string.API_KEY));
                params.put("Content-Type", "application/json");

                return params;
            }
        };


        queue.add(jsonObjectRequest);


    }
    *
    * */


    private void sendEmailVerification() {

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MainActivity.this, "Verification email has been sent, please verify!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Failed to send verification email...", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void addNewCircle() {

//        FirebaseUser mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (!mFirebaseUser.isEmailVerified()) {
//            AlertDialog.Builder alerBuilder = new AlertDialog.Builder(MainActivity.this);
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

        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.create_circle_layout, (ViewGroup) findViewById(R.id.root_layout));
        final TextInputEditText circleName = (TextInputEditText) view.findViewById(R.id.circleName);
        final TextInputEditText circleDescription = (TextInputEditText) view.findViewById(R.id.circleDescription);


        AlertDialog.Builder alBuilder = new AlertDialog.Builder(MainActivity.this);
        alBuilder.setView(view).setTitle("Create new circle").setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String sCircleName = circleName.getText().toString();
                String sCircleDescription = circleDescription.getText().toString();

                createCircle(sCircleName, sCircleDescription);

                Toast.makeText(MainActivity.this, "You will be added to the circle automatically!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();


            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setCancelable(false).show();

    }

//    }

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

//                mArrayList.add(circles);
//                mCirclesAdapter.notifyItemInserted(mArrayList.size() - 1);
                //check if this is the first circle
                if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(Constants.DEFAULT_CIRCLE_ID, null) == null) {
                    PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString(Constants.DEFAULT_CIRCLE_ID, circles.getId())
                            .putString(Constants.DEFAULT_CIRCLE_NAME, circles.getCircleName()).apply();
                }

                //add him to that topic , topic name -> circle ID
                FirebaseMessaging.getInstance().subscribeToTopic(key);


            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: creating new circle failed");
                Toast.makeText(MainActivity.this, "Couldn't create the circle, please try again!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void callSelectCircle() {

        String def_id = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(Constants.DEFAULT_CIRCLE_ID, "NA");
        int pos = 0;

        ArrayList<String> items = new ArrayList<>();
        for (int i = 0; i < circlesArrayList.size(); i++) {
            items.add(circlesArrayList.get(i).getCircleName());
            Log.d(TAG, "callSelectCircle: " + items.get(i));
            if (circlesArrayList.get(i).getId().equals(def_id)) {
                pos = i;
            }
        }
        String[] itemsList = new String[items.size()];
        itemsList = items.toArray(itemsList);

        if(circlesArrayList.size()>0){
            new AlertDialog.Builder(this)
                    .setSingleChoiceItems(itemsList, pos, null)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            // Do something useful withe the position of the selected radio button
                            String id = circlesArrayList.get(selectedPosition).getId();
                            String name = circlesArrayList.get(selectedPosition).getCircleName();

                            PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString(Constants.DEFAULT_CIRCLE_ID, id)
                                    .putString(Constants.DEFAULT_CIRCLE_NAME, name).apply();
                            fetchAllUsers();

                        }
                    })
                    .show();
        }else{
            Toast.makeText(this, "Not in any circles", Toast.LENGTH_SHORT).show();
        }

    }

    private void fetchCirclesData() {

//        showProgressBar();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(Constants.USERS_DB);
        databaseReference.child(Constants.getUid()).child("circles")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

//                        dismissProgressBar();

                        if (!dataSnapshot.hasChildren()) {
                            Toast.makeText(MainActivity.this, "You are not in any circles, Add a new one", Toast.LENGTH_SHORT).show();
                        } else {
                            for (DataSnapshot mDataSnapshot : dataSnapshot.getChildren()) {
                                Circles mCircles = mDataSnapshot.getValue(Circles.class);
                                circlesArrayList.add(mCircles);
                            }

                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(MainActivity.this, "Loading Cancelled", Toast.LENGTH_SHORT).show();
                    }
                });


    }

}
