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

    public static class GeocodingResult {
        private final String latitude;
        private final String longitude;
        private final String displayName;
        private final String countryCode;

        public GeocodingResult(String latitude, String longitude, String displayName, String countryCode) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.displayName = displayName;
            this.countryCode = countryCode;
        }

        public String getLatitude() { return latitude; }
        public String getLongitude() { return longitude; }
        public String getDisplayName() { return displayName; }
        public String getCountryCode() { return countryCode; }
    }

    public static GeocodingResult parseGeocodingResponse(Object response, String url) {
        if (response instanceof JSONArray) {
            return parseNominatimResponse((JSONArray) response, url);
        } else if (response instanceof JSONObject) {
            if (NominatimHostManager.isCensusGeocoderUrl(url)) {
                return parseCensusGeocoderResponse((JSONObject) response, url);
            } else {
                return parseNominatimSingleResponse((JSONObject) response, url);
            }
        }
        return null;
    }

    private static GeocodingResult parseNominatimResponse(JSONArray response, String sourceUrl) {
        if (response == null || response.length() == 0) return null;
        try {
            JSONObject best = response.getJSONObject(0);
            return parseNominatimSingleResponse(best, sourceUrl);
        } catch (JSONException e) { return null; }
    }

    private static GeocodingResult parseNominatimSingleResponse(JSONObject obj, String sourceUrl) {
        try {
            String lat = obj.getString("lat");
            String lon = obj.getString("lon");
            String name = obj.getString("display_name");
            String cc = obj.optJSONObject("address") != null ? obj.getJSONObject("address").optString("country_code") : null;
            return new GeocodingResult(lat, lon, name, cc);
        } catch (JSONException e) { return null; }
    }

    private static GeocodingResult parseCensusGeocoderResponse(JSONObject response, String sourceUrl) {
        try {
            JSONArray matches = response.getJSONObject("result").getJSONArray("addressMatches");
            if (matches.length() == 0) return null;
            JSONObject first = matches.getJSONObject(0);
            JSONObject coords = first.getJSONObject("coordinates");
            return new GeocodingResult(
                String.valueOf(coords.getDouble("y")),
                String.valueOf(coords.getDouble("x")),
                first.getString("matchedAddress"),
                "us"
            );
        } catch (JSONException e) { return null; }
    }
}
