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
     * Data class to hold geocoding results
     */
    public static class GeocodingResult {
        private final String latitude;
        private final String longitude;
        private final String displayName; // <-- ADDED
        private final String countryCode; // <-- ADDED

        // Updated constructor
        public GeocodingResult(String latitude, String longitude, String displayName, String countryCode) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.displayName = displayName;
            this.countryCode = countryCode;
        }

        public String getLatitude() {
            return latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        // Getter for displayName
        public String getDisplayName() { // <-- ADDED
            return displayName;
        }

        // Getter for countryCode
        public String getCountryCode() { // <-- ADDED
            return countryCode;
        }
    }

    /**
     * Parses geocoding response and extracts latitude, longitude, displayName, and countryCode.
     * Supports both Nominatim and Census Geocoder formats.
     *
     * @param response The JSON response from the geocoding API
     * @param url The URL that was used for the request (to determine API type)
     * @return A GeocodingResult containing relevant data, or null if parsing failed
     */
    public static GeocodingResult parseGeocodingResponse(JSONArray response, String url) {
        try {
            if (NominatimHostManager.isCensusGeocoderUrl(url)) {
                Log.e(TAG, "Census Geocoder should return JSONObject, not JSONArray. URL: " + url);
                return null;
            } else {
                // Pass the URL to determine source and assist parsing if needed, though not strictly used by parseNominatimResponse directly
                return parseNominatimResponse(response, url);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing geocoding JSON array: " + e.getMessage());
            return null;
        }
    }

    /**
     * Overload for JSONObject (for Census Geocoder)
     */
    public static GeocodingResult parseGeocodingResponse(JSONObject response, String url) {
        try {
            if (NominatimHostManager.isCensusGeocoderUrl(url)) {
                // Pass the URL to determine source and assist parsing if needed
                return parseCensusGeocoderResponse(response, url);
            } else {
                Log.e(TAG, "Nominatim should return JSONArray, not JSONObject. URL: " + url);
                // If you expect Nominatim to sometimes return JSONObject for specific calls (e.g. reverse geocode for one result)
                // you might need a different parsing path here or ensure those calls use parseReverseGeocodingResponse.
                // For forward geocoding for country check, Nominatim (search endpoint) gives JSONArray.
                return null;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing geocoding JSON object: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses Nominatim API response format
     * @param response JSONArray from Nominatim
     * @param sourceUrl The original request URL (for logging/context, not strictly needed for parsing logic here)
     * @return GeocodingResult or null
     * @throws JSONException
     */
    private static GeocodingResult parseNominatimResponse(JSONArray response, String sourceUrl) throws JSONException {
        if (response == null || response.length() == 0) {
            Log.w(TAG, "Nominatim response is null or empty. URL: " + sourceUrl);
            return null;
        }

        // Find the first result with country_code == "us"
        for (int i = 0; i < response.length(); i++) {
            JSONObject resultObj = response.getJSONObject(i);
            String lat = resultObj.optString("lat");
            String lon = resultObj.optString("lon");
            String displayName = resultObj.optString("display_name");
            String countryCode = null;
            JSONObject addressDetails = resultObj.optJSONObject("address");
            if (addressDetails != null) {
                countryCode = addressDetails.optString("country_code", null);
            }
            if ("us".equalsIgnoreCase(countryCode) && !lat.isEmpty() && !lon.isEmpty()) {
                Log.d(TAG, "Parsed Nominatim: Lat=" + lat + ", Lon=" + lon + ", Name=" + displayName + ", CC=" + countryCode + ", URL=" + sourceUrl);
                return new GeocodingResult(lat, lon, displayName, countryCode);
            }
        }
        // If no US result found, fallback to result with lowest place_rank
        int bestIdx = 0;
        int bestRank = Integer.MAX_VALUE;
        for (int i = 0; i < response.length(); i++) {
            JSONObject resultObj = response.getJSONObject(i);
            int placeRank = resultObj.optInt("place_rank", Integer.MAX_VALUE);
            if (placeRank < bestRank) {
                bestRank = placeRank;
                bestIdx = i;
            }
        }
        JSONObject bestResult = response.getJSONObject(bestIdx);
        String lat = bestResult.optString("lat");
        String lon = bestResult.optString("lon");
        String displayName = bestResult.optString("display_name");
        String countryCode = null;
        JSONObject addressDetails = bestResult.optJSONObject("address");
        if (addressDetails != null) {
            countryCode = addressDetails.optString("country_code", null);
        }
        Log.d(TAG, "No US result found. Parsed Nominatim (lowest place_rank): Lat=" + lat + ", Lon=" + lon + ", Name=" + displayName + ", CC=" + countryCode + ", place_rank=" + bestRank + ", URL=" + sourceUrl);
        return new GeocodingResult(lat, lon, displayName, countryCode);
    }

    /**
     * Parses U.S. Census Bureau Geocoder API response format (from JSONObject)
     * @param response JSONObject from Census Geocoder
     * @param sourceUrl The original request URL
     * @return GeocodingResult or null
     * @throws JSONException
     */
    private static GeocodingResult parseCensusGeocoderResponse(JSONObject response, String sourceUrl) throws JSONException {
        if (response == null) {
            Log.w(TAG, "Census Geocoder response is null. URL: " + sourceUrl);
            return null;
        }
        JSONObject result = response.optJSONObject("result");
        if (result == null) {
            Log.w(TAG, "Census Geocoder response missing 'result' object. URL: " + sourceUrl);
            return null;
        }

        JSONArray matches = result.optJSONArray("addressMatches");
        if (matches == null || matches.length() == 0) {
            Log.w(TAG, "Census Geocoder response has no addressMatches. URL: " + sourceUrl);
            return null;
        }

        JSONObject firstMatch = matches.getJSONObject(0); // Assuming we always take the first match
        JSONObject coordinates = firstMatch.optJSONObject("coordinates");
        String matchedAddress = firstMatch.optString("matchedAddress"); // This will be our displayName

        if (coordinates == null) {
            Log.w(TAG, "Census Geocoder response missing 'coordinates'. MatchedAddress: " + matchedAddress + ", URL: " + sourceUrl);
            return null;
        }

        // Census uses x for longitude, y for latitude
        String lon = String.valueOf(coordinates.optDouble("x", Double.NaN));
        String lat = String.valueOf(coordinates.optDouble("y", Double.NaN));

        if (Double.isNaN(coordinates.optDouble("x", Double.NaN)) || Double.isNaN(coordinates.optDouble("y", Double.NaN))) {
            Log.w(TAG, "Census Geocoder response missing x or y in coordinates. MatchedAddress: " + matchedAddress + ", URL: " + sourceUrl);
            return null;
        }

        // For Census, countryCode is always "us" if successful
        String countryCode = "us";
        Log.d(TAG, "Parsed Census: Lat=" + lat + ", Lon=" + lon + ", Name=" + matchedAddress + ", CC=" + countryCode);
        return new GeocodingResult(lat, lon, matchedAddress, countryCode);
    }

    /**
     * Parses geocoding response from JSONObject (for reverse geocoding from Nominatim)
     * Only supports Nominatim format since Census Geocoder doesn't support reverse geocoding
     */
    public static String parseReverseGeocodingResponse(JSONObject response) throws JSONException {
        // This method is separate and returns a String, not a GeocodingResult.
        // It's used by your HomeFragment's reverseGeocode methods.
        // If you wanted this to also return a GeocodingResult, you'd adapt it similarly.
        return response.getString("display_name");
    }
}
