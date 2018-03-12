package com.famtracker.Activities;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.famtracker.Constants;
import com.famtracker.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MembersLocationOnMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    String uid;


    String name;
    double lat;
    double lon;

    boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_members_location_on_map);

        uid = getIntent().getStringExtra("uid");
        if (uid != null)
            fetchLocation(uid);
        else {
            name = getIntent().getStringExtra("name");
            lat = Double.parseDouble(getIntent().getStringExtra("lat"));
            lon = Double.parseDouble(getIntent().getStringExtra("lng"));

            Log.d("TAG", "onCreate: " + name + " " + lat + "," + lon);

            notify = true;

        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void fetchLocation(String uid) {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(Constants.LOCATIONS_DB);
        databaseReference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (!dataSnapshot.hasChildren()) {
                    Toast.makeText(MembersLocationOnMapActivity.this, "No location data found", Toast.LENGTH_SHORT).show();
                } else {
                    double lat = dataSnapshot.child("lat").getValue(Double.class);
                    double lon = dataSnapshot.child("lng").getValue(Double.class);
                    String name = dataSnapshot.child("name").getValue(String.class);

                    addMarker(lat, lon, name);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MembersLocationOnMapActivity.this, "Loading Cancelled!", Toast.LENGTH_SHORT).show();
            }
        });

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(notify){
            addMarker(lat,lon,name);
        }

    }

    public void addMarker(double lat, double lon, String name) {

        if (mMap != null) {
            LatLng start = new LatLng(lat, lon);
            mMap.addMarker(new MarkerOptions().title(name).position(start));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 12.0f));
        }

    }
}
