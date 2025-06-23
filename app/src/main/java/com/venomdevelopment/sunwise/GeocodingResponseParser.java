package com.venomdevelopment.sunwise;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class to parse different geocoding API response formats.
 * Handles both Nominatim and U.S. Census Bureau Geocoder responses.
 */
public class GeocodingResponseParser {
    
    private static final String TAG = "GeocodingResponseParser";
    
    /**
     * Parses geocoding response and extracts latitude and longitude.
     * Supports both Nominatim and Census Geocoder formats.
     * 
     * @param response The JSON response from the geocoding API
     * @param url The URL that was used for the request (to determine API type)
     * @return A GeocodingResult containing lat/lon, or null if parsing failed
     */
    public static GeocodingResult parseGeocodingResponse(JSONArray response, String url) {
        try {
            if (NominatimHostManager.isCensusGeocoderUrl(url)) {
                // Should not happen, but handle gracefully
                Log.e(TAG, "Census Geocoder should return JSONObject, not JSONArray");
                return null;
            } else {
                return parseNominatimResponse(response);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing geocoding response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Overload for JSONObject (for Census Geocoder)
     */
    public static GeocodingResult parseGeocodingResponse(JSONObject response, String url) {
        try {
            if (NominatimHostManager.isCensusGeocoderUrl(url)) {
                return parseCensusGeocoderResponse(response);
            } else {
                // Should not happen, but handle gracefully
                Log.e(TAG, "Nominatim should return JSONArray, not JSONObject");
                return null;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing geocoding response: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses Nominatim API response format
     */
    private static GeocodingResult parseNominatimResponse(JSONArray response) throws JSONException {
        if (response.length() == 0) {
            Log.w(TAG, "Nominatim response is empty");
            return null;
        }
        
        JSONObject firstResult = response.getJSONObject(0);
        String lat = firstResult.getString("lat");
        String lon = firstResult.getString("lon");
        
        return new GeocodingResult(lat, lon);
    }
    
    /**
     * Parses U.S. Census Bureau Geocoder API response format (from JSONObject)
     */
    private static GeocodingResult parseCensusGeocoderResponse(JSONObject response) throws JSONException {
        JSONObject result = response.getJSONObject("result");
        JSONArray matches = result.getJSONArray("addressMatches");
        if (matches.length() == 0) {
            Log.w(TAG, "Census Geocoder response has no matches");
            return null;
        }
        JSONObject match = matches.getJSONObject(0);
        JSONObject coords = match.getJSONObject("coordinates");
        String lon = String.valueOf(coords.getDouble("x"));
        String lat = String.valueOf(coords.getDouble("y"));
        return new GeocodingResult(lat, lon);
    }
    
    /**
     * Parses geocoding response from JSONObject (for reverse geocoding)
     * Only supports Nominatim format since Census Geocoder doesn't support reverse geocoding
     */
    public static String parseReverseGeocodingResponse(JSONObject response) throws JSONException {
        return response.getString("display_name");
    }
    
    /**
     * Data class to hold geocoding results
     */
    public static class GeocodingResult {
        private final String latitude;
        private final String longitude;
        
        public GeocodingResult(String latitude, String longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
        
        public String getLatitude() {
            return latitude;
        }
        
        public String getLongitude() {
            return longitude;
        }
    }
} 