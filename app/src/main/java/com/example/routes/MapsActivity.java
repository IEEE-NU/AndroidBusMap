package com.example.routes;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.vision.text.Text;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.graphics.Color.parseColor;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private List<Stop> mStopList;
    private List<Stop2> mStop2List;
    private List<Route> mRouteList;
    private List<Route2> mRoute2List;
    private HashMap<Integer, Marker> mMarkerMap; // maps stop id to google maps marker
    private HashMap<Integer, Polyline> mPolylineMap; // maps route id to google maps polyline
    private RequestQueue mRequestQueue;
    private boolean[] mSelectedRoutes;

    public class Stop {
        int id;
        String name;
        double lat;
        double lon;
        int buddy;
        String nextStopTime;

        LatLng getLatLng() {
            return new LatLng(lat, lon);
        }
    }

    public class Stop2 {
        String name;
        String routeName;
        double lat;
        double lon;
        String stopTimes;
    }

    public class Route {
        int id;
        String name;
        String short_name;
        int color;
        List<LatLng> path;
        List<Integer> stops;
    }

    public class Route2 {
        String name;
        List<LatLng> path;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // set up queue for http requests (for route/stop info)
        mRequestQueue = Volley.newRequestQueue(this);

        mRouteList = new ArrayList<>();
        getRoutes();

        mStopList = new ArrayList<>();
        getStops();

        mStop2List = new ArrayList<>();
        mRoute2List = new ArrayList<>();
        getStopTimes();

        Button selectRouteButton = (Button) findViewById(R.id.select_route_button);
        selectRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                routeSelectionDialog();
            }
        });
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

                        // TODO
                        // actually get stop times
                        stop.nextStopTime = "N: 3:52pm \nS: 3:57pm";
                        mStopList.add(stop);
                    }

                    drawMaps();
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

    public String getJSONString(String filename) {
        InputStream is = getResources().openRawResource(
                getResources().getIdentifier(filename,
                "raw", getPackageName()));
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String jsonString = writer.toString();
        return jsonString;
    }

    public void getStopTimes() {
        String jsonString = getJSONString("campus_loop");
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonString);
            JSONArray features = jsonObject.getJSONArray("features");

            // get route info stored in last feature
            Route2 route2 = new Route2();
            JSONObject routeFeature = features.getJSONObject(features.length()-1);
            JSONObject routeProperties = routeFeature.getJSONObject("properties");
            String routeName = routeProperties.getString("Name");
            route2.name = routeName;

            // get path of route
            route2.path = new ArrayList<>();
            JSONObject routeGeometry = routeFeature.getJSONObject("geometry");
            JSONArray routeCoordinates = routeGeometry.getJSONArray("coordinates");
            for (int i = 0; i < routeGeometry.length(); i++) {
                JSONArray coordinate = routeCoordinates.getJSONArray(i);
                LatLng latLng = new LatLng(coordinate.getDouble(0),coordinate.getDouble(1));
                route2.path.add(latLng);
            }

            // get stop info stored in first n-1 features
            for (int i = 0; i < features.length()-1; i++) {
                Stop2 stop2 = new Stop2();
                stop2.routeName = routeName;
                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");
                stop2.name = properties.getString("Name");
                stop2.stopTimes = properties.getString("description");

                JSONObject geometry = feature.getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                stop2.lat = coordinates.getDouble(0);
                stop2.lon = coordinates.getDouble(1);
                mStop2List.add(stop2);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

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

    private void drawStopMarker(Stop stop) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(stop.getLatLng())
                .title(stop.name)
                .snippet(stop.nextStopTime)
                .visible(false);
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

        // set custom marker info window view so stop times are visible
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View view = getLayoutInflater().inflate(R.layout.marker_text, null);

                TextView stopName = (TextView) view.findViewById(R.id.stop_name);
                stopName.setText(marker.getTitle());
                stopName.setTypeface(null, Typeface.BOLD);

                TextView stopTime = (TextView) view.findViewById(R.id.stop_time);
                stopTime.setText(marker.getSnippet());

                return view;
            }
        });

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
}
