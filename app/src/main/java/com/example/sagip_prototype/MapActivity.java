package com.example.sagip_prototype;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set default location (Manila, Philippines)
        LatLng manila = new LatLng(14.5995, 120.9842);
        mMap.addMarker(new MarkerOptions().position(manila).title("Manila"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(manila, 12));

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        
        // Enable my location button
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }
} 