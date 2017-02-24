package com.example.routes;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.graphics.Color.parseColor;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    private List<Stop> mStopList;
    private List<Route> mRouteList;
    private HashMap<Integer, Marker> mMarkerMap; // maps stop id to google maps marker
    private HashMap<Integer, Polyline> mPolylineMap; // maps route id to google maps polyline
    private RequestQueue mRequestQueue;
    private boolean[] mSelectedRoutes;
    private Bitmap imageBitmap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationRequest mLocationRequest;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission
                .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    2);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        createLocationRequest();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        startLocationUpdates();
        displayClosestStop(findClosestStop());
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, (LocationListener) this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        imageBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.piny);
        imageBitmap = resizeMapIcons(imageBitmap,30,30);

        // set up queue for http requests (for route/stop info)
        mRequestQueue = Volley.newRequestQueue(this);

        mRouteList = new ArrayList<>();
        getRoutes();

        mStopList = new ArrayList<>();
        getStops();

        Button selectRouteButton = (Button) findViewById(R.id.select_route_button);
        selectRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                routeSelectionDialog();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    public void getRoutes() {
        String url = "https://northwestern.doublemap.com/map/v2/routes";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject object = response.getJSONObject(i);
                        Route route = new Route();
                        route.id = object.getInt("id");
                        route.name = object.getString("name");
                        route.short_name = object.getString("short_name");
                        route.color = parseColor("#" + object.getString("color"));

                        // get points on route's path
                        JSONArray path_arr = object.getJSONArray("path");
                        route.path = new ArrayList<>();
                        for (int j = 0; j < path_arr.length() && j + 1 < path_arr.length(); j += 2) {
                            route.path.add(new LatLng(path_arr.getDouble(j), path_arr.getDouble(j + 1)));
                        }


                        // get points on route's path
                        JSONArray stops_arr = object.getJSONArray("stops");
                        route.stops = new ArrayList<>();
                        for (int j = 0; j < stops_arr.length(); j++) {
                            route.stops.add(stops_arr.getInt(j));
                        }

                        mRouteList.add(route);
                    }

                    // by default, show all routes
                    mSelectedRoutes = new boolean[mRouteList.size()];
                    for (int i = 0; i < mRouteList.size(); i++) {
                        mSelectedRoutes[i] = true;
                    }

                    drawMaps();
                    displayClosestStop(findClosestStop());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        mRequestQueue.add(jsonArrayRequest);

    }

    public void getStops() {
        String url = "https://northwestern.doublemap.com/map/v2/stops";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject object = response.getJSONObject(i);
                        Stop stop = new Stop();
                        stop.id = object.getInt("id");
                        stop.name = object.getString("name");
                        stop.lat = object.getDouble("lat");
                        stop.lon = object.getDouble("lon");

                        mStopList.add(stop);
                    }

                    drawMaps();
                    displayClosestStop(findClosestStop());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        mRequestQueue.add(jsonArrayRequest);
        displayClosestStop(findClosestStop());
    }

    public void drawMaps() {
        // draw map will be called from both onMapReady, getRoutes, and getStops
        // but only run when all have completed
        if (mMap == null) {
            return;
        }
        if (mRouteList.size() == 0) {
            return;
        }
        if (mStopList.size() == 0) {
            return;
        }

        // make invisible markers for each stop
        mMarkerMap = new HashMap<>();
        for (int i = 0; i < mStopList.size(); i++) {
            Stop stop = mStopList.get(i);
            drawStopMarker(stop);
        }

        // draw invisible lines for each route
        mPolylineMap = new HashMap<>();
        for (int i = 0; i < mRouteList.size(); i++) {
            Route route = mRouteList.get(i);
            drawRouteLine(route);
        }

        // show selected routes by making their lines and markers visible
        updateMaps();
    }
    public Bitmap resizeMapIcons(Bitmap imageBitmap, int width, int height){
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }

    private void drawStopMarker(Stop stop) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(stop.getLatLng())
                .title(stop.name)
                .visible(false)
                .icon(BitmapDescriptorFactory.fromBitmap(imageBitmap));
        Marker marker = mMap.addMarker(markerOptions);
        mMarkerMap.put(stop.id, marker);
    }

    private void drawRouteLine(Route route) {
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(route.path)
                .color(route.color)
                .visible(false);
        Polyline polyline = mMap.addPolyline(polylineOptions);
        mPolylineMap.put(route.id, polyline);
    }

    private void updateMaps() {
        // loop twice because some stops are in multiple routes
        // don't want to hide a stop that should be shown in another route
        for (int i = 0; i < mRouteList.size(); i++) {
            Route route = mRouteList.get(i);
            if (!mSelectedRoutes[i]) {
                setRouteVisibility(route,false);
            }
        }
        for (int i = 0; i < mRouteList.size(); i++) {
            Route route = mRouteList.get(i);
            if (mSelectedRoutes[i]) {
                setRouteVisibility(route,true);
            }
        }
    }

    private void setRouteVisibility(Route route, Boolean visibility) {
        Polyline polyline = mPolylineMap.get(route.id);
        polyline.setVisible(visibility);
        for (int j = 0; j < route.stops.size(); j++) {
            int stop_id = route.stops.get(j);
            Marker marker = mMarkerMap.get(stop_id);
            marker.setVisible(visibility);
        }
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

        LatLng northwestern = new LatLng(42.056437, -87.675900);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(northwestern, 12));

        drawMaps();
    }

    private void routeSelectionDialog() {
        String[] routeNames = new String[mRouteList.size()];
        for (int i = 0; i < mRouteList.size(); i++) {
            routeNames[i] = mRouteList.get(i).name;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMultiChoiceItems(routeNames, mSelectedRoutes, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        mSelectedRoutes[i] = b;
                        updateMaps();
                    }
                })
                .setTitle("Select Routes to Display")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private Stop findClosestStop(){
        if (mLocation == null) {
            return null;
        }
        if (mStopList.size() == 0) {
            return null;
        }
        double min = Double.MAX_VALUE;
        Location stop = new Location("");
        int index = 0;
        for (int i = 0; i < mStopList.size() ; i++) {
            stop.setLatitude(mStopList.get(i).lat);
            stop.setLongitude(mStopList.get(i).lon);
            if (mLocation.distanceTo(stop) < min){
                index = i;
            }

        }
        return mStopList.get(index);
    }

    private void displayClosestStop(Stop closest_stop){
        if (closest_stop == null) {
            return;
        }
        TextView text = (TextView) findViewById(R.id.textView);
        text.setText("You are closest to " + closest_stop.name );
    }
}



