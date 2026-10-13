package com.vietblog.domain.search;

public final class GeoAlgorithms {
    private GeoAlgorithms() {}

    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 6371.0088 * 2 * Math.asin(Math.sqrt(Math.max(0, Math.min(1, a))));
    }
}
