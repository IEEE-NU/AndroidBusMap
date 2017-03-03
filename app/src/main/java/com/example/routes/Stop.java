package com.example.routes;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

/**
 * Represents a bus stop
 */
public class Stop {
    int id;
    String name;
    double lat;
    double lon;
    int buddy;
    HashMap<String, String> stopTimeStrings; //maps route name to stop time string

    LatLng getLatLng() {
        return new LatLng(lat, lon);
    }
}
