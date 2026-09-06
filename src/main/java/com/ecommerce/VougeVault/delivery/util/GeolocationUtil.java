package com.ecommerce.VougeVault.delivery.util;

/**
 * Simple geolocation calculations
 * Only keeps what's actually needed
 */
public class GeolocationUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     * Example: Mumbai (19.0760, 72.8777) to Thane (19.2183, 72.9781) = ~30 km
     */
    public static Double calculateDistanceKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        double dlat = lat2Rad - lat1Rad;
        double dlon = lon2Rad - lon1Rad;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dlon / 2) * Math.sin(dlon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        return Math.round(distance * 10.0) / 10.0;
    }

    /**
     * Estimate delivery time based on distance
     * Simple rule: 30 km/h in city
     * Example: 10 km away = 20 minutes
     */
    public static Integer estimateTimeMinutes(Double distanceKm) {
        if (distanceKm == null) return null;
        double timeHours = distanceKm / 30.0;  // 30 km/h average
        int timeMinutes = (int) Math.ceil(timeHours * 60);
        return Math.max(timeMinutes, 1);
    }

    /**
     * Check if arrived at destination (within 100 meters)
     */
    public static Boolean hasArrivedAtDestination(Double currentLat, Double currentLon,
                                                  Double destLat, Double destLon) {
        Double distanceKm = calculateDistanceKm(currentLat, currentLon, destLat, destLon);
        Double distanceMeters = distanceKm * 1000;
        return distanceMeters <= 100;  // Within 100 meters
    }

    /**
     * Generate random 6-digit OTP
     */
    public static String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }
}