package com.example.routes;

import com.google.android.gms.maps.model.LatLng;

/**
 * Represents a bus stop
 */
public class Stop {
    int id;
    String name;
    double lat;
    double lon;
    int buddy;

    LatLng getLatLng() {
        return new LatLng(lat, lon);
    }
}
