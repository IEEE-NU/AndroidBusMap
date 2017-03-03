package com.example.routes;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Represents a bus route
 */
public class Route {
    int id;
    String name;
    String formalName;
    int color;
    List<LatLng> path;
    List<Integer> stops;
}
