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
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    ArrayList<String> fcm_tokens_list = new ArrayList<>();
    ArrayList<String> topics = new ArrayList<>();


    String TAG = "MainAct TAG";

    private GoogleMap mMap;
    LatLng newLatLng;
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    TextView nameTv, emailTv;
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

        initProgressDialog();
        fetchAllUsers();
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

        showProgressBar();

        databaseReference.child(Constants.LOCATIONS_DB).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (!dataSnapshot.hasChildren()) {
                    Toast.makeText(MainActivity.this, "No Members Available!", Toast.LENGTH_SHORT).show();
                } else {
                    for (DataSnapshot mDataSnapshot : dataSnapshot.getChildren()) {
                        double lat = mDataSnapshot.child("lat").getValue(Double.class);
                        double lon = mDataSnapshot.child("lng").getValue(Double.class);
                        String name = mDataSnapshot.child("name").getValue(String.class);

                        addMarker(lat, lon, name);

                    }

                    dismissProgressBar();

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(12.899829, 77.573682), 8.0f));


                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Loading Cancelled!", Toast.LENGTH_SHORT).show();
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

        String condition = "";
        condition = "'"+topics.get(0)+"' in topics";

        for(int i = 1;i < topics.size();i++){
            condition = condition +" || '"+topics.get(i)+"' in topics";
        }

        Log.d(TAG, "sendNotificationsToEveryone: "+condition);

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
            notification.put("click_action","NOTIFY_ACTIVITY");

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

                if(dataSnapshot.hasChildren()){
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        topics.add(ds.getKey());
                    }

                }else{
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
            // Handle the camera action
        } else if (id == R.id.nav_circle) {

        } else if (id == R.id.nav_people) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_logout) {
            stopService(new Intent(MainActivity.this, LocationMonitoringService.class));
            Constants.removeLocation();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            //  Constants.removeToken(MainActivity.this);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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

}
