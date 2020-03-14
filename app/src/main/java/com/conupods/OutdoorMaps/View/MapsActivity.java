package com.conupods.OutdoorMaps.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.conupods.R;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private static final float DEFAULT_ZOOM = 15f;

    // These could to be moved outside of the file
    public final static double SGW_LAT = 45.496080;
    public final static double SGW_LNG = -73.577957;
    public final static double LOY_LAT = 45.458333;
    public final static double LOY_LNG = -73.640450;

    // LatLng objects for the campuses
    public final static LatLng SGW_CAMPUS_LOC = new LatLng(SGW_LAT, SGW_LNG);
    public final static LatLng LOY_CAMPUS_LOC = new LatLng(LOY_LAT, LOY_LNG);

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    public static final int RESOLVABLE_API_ERROR_REQUEST_CODE = 51;

    private GoogleMap mMap;
    private BuildingOverlays mBuildingOverlays;


    private final String COURSE_LOCATION_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private final String FINE_LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;

    //Activity Components
    //private  Button locationBtn;
    private View mMapView;
    private EditText mSearchBar;


    //Variables for logic
    private boolean mPermissionsGranted = false;
    private Location lastKnownLocation;
    private LocationCallback locationCallback;

    //Providers
    //TODO Might need to update to more recent version
    private FusedLocationProviderClient fusedLocationProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initializeMap();

        if(!mPermissionsGranted)
            getLocationPermission();
        else{
            getDeviceCurrentLocation();
        }

    }



    private void initializeMap() {
        Log.d(TAG, "Initializing Map...");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mMapView = mapFragment.getView();
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
        Log.d(TAG, "Map is ready");

        mMap = googleMap;

        if(mPermissionsGranted){
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);


            createLocationRequest();
        }

        MapInitializer mapInitializer = new MapInitializer(mMap);
        mapInitializer.initializeSearchBar((EditText) findViewById(R.id.search));
        mapInitializer.initializeToggleButtons((Button) findViewById(R.id.SGW), (Button) findViewById(R.id.LOY));


        Toast.makeText(this, "Maps is ready", Toast.LENGTH_SHORT).show();
        mBuildingOverlays = new BuildingOverlays(mMap,getString(R.string.geojson_url));
        mBuildingOverlays.overlayPolygons();
    }

    private void createLocationRequest() {
        //Building the location request
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder requestBuilder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        //Make the requests to the current settings of the device
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> tasks = settingsClient.checkLocationSettings(requestBuilder.build());

        tasks.addOnSuccessListener(MapsActivity.this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                moveToCampus(SGW_CAMPUS_LOC);
            }
        });


        tasks.addOnFailureListener(MapsActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(e instanceof ResolvableApiException){
                    ResolvableApiException resolvableApiException = (ResolvableApiException) e;

                    try{
                        resolvableApiException.startResolutionForResult(MapsActivity.this, RESOLVABLE_API_ERROR_REQUEST_CODE);
                    }
                    catch(Exception e1){
                        Log.e(TAG, "Error in getting settings for location request... ");
                        e1.printStackTrace();
                    }
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 51 && resultCode == RESULT_OK){

            getDeviceCurrentLocation();
        }
    }

    private void getLocationPermission(){
        Log.d(TAG, "Getting Location Permissions");


        /** After android Marshmellow release, we need to explicitly check for
         * permissions such as location permissions*/

        String[] permissions = {
                 FINE_LOCATION_PERMISSION,COURSE_LOCATION_PERMISSION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "FINE_LOCATION_PERMISSION given");

            if(ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "COURSE_LOCATION_PERMISSION given");
                mPermissionsGranted = true;
            }
        }

        else
            ActivityCompat.requestPermissions(MapsActivity.this, permissions, LOCATION_PERMISSION_REQUEST_CODE);



    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        Log.d(TAG, "onRequestPermissionsResult is called");

        mPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length>0){
                    for(int i = 0; i< grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mPermissionsGranted = false;
                            Log.d(TAG, "Permissions Failed "+ i);
                            Log.d(TAG, "Permissions Failed");
                            return;
                        }
                    }

                    Log.d(TAG, "Permissions Granted");
                    mPermissionsGranted = true;
                    initializeMap();
                }
            }
        }
    }


    private void getDeviceCurrentLocation(){
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(mPermissionsGranted){
                final Task currentLocation = fusedLocationProvider.getLastLocation();

                currentLocation.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: Got the current lastKnownLocation");
                            lastKnownLocation = (Location) currentLocation.getResult();

                            if(lastKnownLocation != null){
                                //moveCamera(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), DEFAULT_ZOOM);
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                            else{
                                final LocationRequest locationRequest = LocationRequest.create();
                                locationRequest.setInterval(10000);
                                locationRequest.setFastestInterval(5000);

                                locationCallback = new LocationCallback(){
                                    @Override
                                    public void onLocationResult(LocationResult locationResult) {
                                        super.onLocationResult(locationResult);

                                        if(locationResult == null){
                                            return;
                                        }
                                        lastKnownLocation = locationResult.getLastLocation();
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                        //fusedLocationProvider.removeLocationUpdates(locationCallback);
                                    }
                                };

                                fusedLocationProvider.requestLocationUpdates(locationRequest, locationCallback, null);
                            }

                        }
                        else{
                            Log.d(TAG, "onComplete: Current Location is null");
                            Toast.makeText(MapsActivity.this, "Current location not found", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }catch(SecurityException e){
            Log.e(TAG, "getDeviceCurrentLOcation: SecurityException: "+ e.getMessage());
        }
    }


    // Add a marker in starting location and move the camera
    private void moveToCampus(LatLng targetCampus) {
        mMap.addMarker(new MarkerOptions().position(targetCampus).title("Marker in Campus"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(targetCampus));
    }


    private void initLocationButton() {
        Button locationButton = findViewById(R.id.locationButton);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceCurrentLocation();
            }
        });
    }
}