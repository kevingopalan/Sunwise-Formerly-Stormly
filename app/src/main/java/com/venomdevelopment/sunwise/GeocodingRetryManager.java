package com.venomdevelopment.sunwise;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;

/**
 * Utility class to manage retry logic for geocoding requests.
 * Attempts up to 8 retries on working hosts with 10ms delays.
 */
public class GeocodingRetryManager {
    /**
     * Attempts to geocode an address with retry logic and optional countrycodes filter
     * @param context The application context
     * @param address The address to geocode
     * @param userAgent The User-Agent header to use
     * @param countrycodes Optional country code filter (e.g. "us"), or null for no filter
     * @param successCallback Callback for successful geocoding
     * @param failureCallback Callback for failed geocoding
     */
    public static void geocodeWithRetry(Context context, String address, String userAgent,
                                       String countrycodes,
                                       GeocodingSuccessCallback successCallback,
                                       GeocodingFailureCallback failureCallback) {
        if (context == null) {
            Log.w(TAG, "Context is null, cannot proceed with geocoding retry");
            failureCallback.onFailure("Unable to complete geocoding request");
            return;
        }
        String workingHostUrl = NominatimHostManager.getWorkingHostUrl();
        if (workingHostUrl == null) {
            NominatimHostManager.setDynamicMaxRetryAttempts(8);
        } else {
            NominatimHostManager.setDynamicMaxRetryAttempts(16);
        }
        geocodeWithRetry(context, address, userAgent, countrycodes, successCallback, failureCallback, 0);
    }

    /**
     * Internal method for recursive retry logic with countrycodes
     */
    private static void geocodeWithRetry(Context context, String address, String userAgent,
                                        String countrycodes,
                                        GeocodingSuccessCallback successCallback,
                                        GeocodingFailureCallback failureCallback,
                                        int attemptCount) {
        if (context == null) {
            Log.w(TAG, "Context is null during retry attempt " + attemptCount + ", aborting");
            failureCallback.onFailure("Unable to complete geocoding request");
            return;
        }
        if (attemptCount >= NominatimHostManager.getDynamicMaxRetryAttempts()) {
            Log.w(TAG, "Max retry attempts reached for address: " + address);
            failureCallback.onFailure("Some locations couldn't be geocoded after multiple attempts");
            return;
        }
        String hostUrl = NominatimHostManager.getWorkingHostUrl();
        if (hostUrl == null) {
            Log.d(TAG, "No known working hosts, trying all available APIs for attempt " + (attemptCount + 1));
            hostUrl = getNextAvailableApiUrl(attemptCount);
            if (hostUrl == null) {
                Log.w(TAG, "No geocoding APIs available for retry");
                failureCallback.onFailure("No geocoding services available");
                return;
            }
        }
        String encodedAddress = address.replaceAll(" ", "+");
        final String geocodeUrl;
        final boolean isCensus = NominatimHostManager.isCensusGeocoderUrl(hostUrl);
        if (isCensus) {
            geocodeUrl = hostUrl + encodedAddress + NominatimHostManager.getCensusGeocoderParams();
        } else {
            // Always ensure only one '?' in the URL, and all params use '&'
            String baseUrl = hostUrl + encodedAddress;
            String params = "format=json&addressdetails=1";
            // If this is a Nominatim search and countrycodes is not set, add countrycodes=us unless this is a country check (countrycodes=null)
            if (countrycodes != null && !countrycodes.isEmpty()) {
                params += "&countrycodes=" + countrycodes;
            } else if (!baseUrl.contains("countrycodes=us") && countrycodes != null) {
                params += "&countrycodes=us";
            }
            // Remove any trailing '?' or '&' from baseUrl
            baseUrl = baseUrl.replaceAll("[?&]+$", "");
            geocodeUrl = baseUrl + "&" + params;
        }
        Log.d(TAG, "Retry attempt " + (attemptCount + 1) + " for address: " + address + " using: " + geocodeUrl);
        Log.i(TAG, "Geocoding request URL: " + geocodeUrl);

        // Log the URL for all geocoding services (Census and Nominatim)
        if (isCensus) {
            Log.i(TAG, "Census Geocoder request URL: " + geocodeUrl);
        } else {
            Log.i(TAG, "Nominatim Geocoder request URL: " + geocodeUrl);
        }
        if (isCensus) {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, geocodeUrl, null, response -> {
                        try {
                            GeocodingResponseParser.GeocodingResult result =
                                    GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                            if (result != null) {
                                NominatimHostManager.recordHostSuccess(geocodeUrl);
                                successCallback.onSuccess(result);
                            } else {
                                NominatimHostManager.addDelay(() -> 
                                    geocodeWithRetry(context, address, userAgent, countrycodes, successCallback, failureCallback, attemptCount + 1));
                            }
                        } catch (Exception e) {
                            if (response != null && response.toString().contains("<!DOCTYPE")) {
                                Log.e(TAG, "Received HTML response from Census geocoder on attempt " + (attemptCount + 1) + ": " + response.toString());
                                failureCallback.onFailure("Received HTML error page from Census geocoder");
                                return;
                            }
                            Log.e(TAG, "Error parsing Census response on attempt " + (attemptCount + 1) + ": " + e.getMessage());
                            NominatimHostManager.addDelay(() -> 
                                geocodeWithRetry(context, address, userAgent, countrycodes, successCallback, failureCallback, attemptCount + 1));
                        }
                    }, error -> {
                        Log.e(TAG, "Census request failed on attempt " + (attemptCount + 1) + ": " + error.getMessage());
                        NominatimHostManager.addDelay(() -> 
                            geocodeWithRetry(context, address, userAgent, countrycodes, successCallback, failureCallback, attemptCount + 1));
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("User-Agent", userAgent);
                    return headers;
                }
            };
            jsonObjectRequest.setShouldCache(false);
            SunwiseApp.getInstance().getRequestQueue().add(jsonObjectRequest);
        } else {
            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                    (Request.Method.GET, geocodeUrl, null, response -> {
                        try {
                            GeocodingResponseParser.GeocodingResult result =
                                    GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                            if (result != null) {
                                NominatimHostManager.recordHostSuccess(geocodeUrl);
                                successCallback.onSuccess(result);
                            } else {
                                NominatimHostManager.addDelay(() -> 
                                    geocodeWithRetry(context, address, userAgent, countrycodes, successCallback, failureCallback, attemptCount + 1));
                            }
                        } catch (Exception e) {
                            if (response != null && response.toString().contains("<!DOCTYPE")) {
                                Log.e(TAG, "Received HTML response from Nominatim geocoder on attempt " + (attemptCount + 1) + ": " + response.toString());
                                failureCallback.onFailure("Received HTML error page from Nominatim geocoder");
                                return;
                            }
                            Log.e(TAG, "Error parsing Nominatim response on attempt " + (attemptCount + 1) + ": " + e.getMessage());
                            NominatimHostManager.addDelay(() -> 
                                geocodeWithRetry(context, address, userAgent, countrycodes, successCallback, failureCallback, attemptCount + 1));
                        }
                    }, error -> {
                        Log.e(TAG, "Nominatim request failed on attempt " + (attemptCount + 1) + ": " + error.getMessage());
                        NominatimHostManager.addDelay(() -> 
                            geocodeWithRetry(context, address, userAgent, countrycodes, successCallback, failureCallback, attemptCount + 1));
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("User-Agent", userAgent);
                    return headers;
                }
            };
            jsonArrayRequest.setShouldCache(false);
            SunwiseApp.getInstance().getRequestQueue().add(jsonArrayRequest);
        }
    }
    
    private static final String TAG = "GeocodingRetryManager";
    
    /**
     * Interface for handling successful geocoding results
     */
    public interface GeocodingSuccessCallback {
        void onSuccess(GeocodingResponseParser.GeocodingResult result);
    }
    
    /**
     * Interface for handling geocoding failures
     */
    public interface GeocodingFailureCallback {
        void onFailure(String errorMessage);
    }
    
    /**
     * Attempts to geocode an address with retry logic
     * @param context The application context
     * @param address The address to geocode
     * @param userAgent The User-Agent header to use
     * @param successCallback Callback for successful geocoding
     * @param failureCallback Callback for failed geocoding
     */
    public static void geocodeWithRetry(Context context, String address, String userAgent,
                                       GeocodingSuccessCallback successCallback,
                                       GeocodingFailureCallback failureCallback) {
        // Check if context is null (fragment detached)
        if (context == null) {
            Log.w(TAG, "Context is null, cannot proceed with geocoding retry");
            failureCallback.onFailure("Unable to complete geocoding request");
            return;
        }
        // Determine host state and set retry attempts
        String workingHostUrl = NominatimHostManager.getWorkingHostUrl();
        if (workingHostUrl == null) {
            // Unknown host state, use 8 attempts
            NominatimHostManager.setDynamicMaxRetryAttempts(8);
        } else {
            // Known working host, use 16 attempts
            NominatimHostManager.setDynamicMaxRetryAttempts(16);
        }
        geocodeWithRetry(context, address, userAgent, successCallback, failureCallback, 0);
    }
    
    /**
     * Internal method for recursive retry logic
     */
    private static void geocodeWithRetry(Context context, String address, String userAgent,
                                        GeocodingSuccessCallback successCallback,
                                        GeocodingFailureCallback failureCallback,
                                        int attemptCount) {

        // Check if context is null (fragment detached)
        if (context == null) {
            Log.w(TAG, "Context is null during retry attempt " + attemptCount + ", aborting");
            failureCallback.onFailure("Unable to complete geocoding request");
            return;
        }

        // Check if we've exceeded max attempts
        if (attemptCount >= NominatimHostManager.getDynamicMaxRetryAttempts()) {
            Log.w(TAG, "Max retry attempts reached for address: " + address);
            failureCallback.onFailure("Some locations couldn't be geocoded after multiple attempts");
            return;
        }

        // Try to get a working host URL first (preferred)
        String hostUrl = NominatimHostManager.getWorkingHostUrl();

        // If no working host is available, try all available APIs in sequence
        if (hostUrl == null) {
            Log.d(TAG, "No known working hosts, trying all available APIs for attempt " + (attemptCount + 1));
            hostUrl = getNextAvailableApiUrl(attemptCount);

            if (hostUrl == null) {
                Log.w(TAG, "No geocoding APIs available for retry");
                failureCallback.onFailure("No geocoding services available");
                return;
            }
        }

        // Encode the address
        String encodedAddress = address.replaceAll(" ", "+");
        final String geocodeUrl;
        final boolean isCensus = NominatimHostManager.isCensusGeocoderUrl(hostUrl);
        
        if (isCensus) {
            geocodeUrl = hostUrl + encodedAddress + NominatimHostManager.getCensusGeocoderParams();
        } else {
            geocodeUrl = hostUrl + encodedAddress + "&format=json&addressdetails=1";
        }
        
        Log.d(TAG, "Retry attempt " + (attemptCount + 1) + " for address: " + address + " using: " + geocodeUrl);
        
        if (isCensus) {
            // Use JsonObjectRequest for Census
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, geocodeUrl, null, response -> {
                        try {
                            GeocodingResponseParser.GeocodingResult result =
                                    GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                            if (result != null) {
                                NominatimHostManager.recordHostSuccess(geocodeUrl);
                                successCallback.onSuccess(result);
                            } else {
                                // No results, retry after delay
                                NominatimHostManager.addDelay(() -> 
                                    geocodeWithRetry(context, address, userAgent, successCallback, failureCallback, attemptCount + 1));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing Census response on attempt " + (attemptCount + 1) + ": " + e.getMessage());
                            // Retry after delay
                            NominatimHostManager.addDelay(() -> 
                                geocodeWithRetry(context, address, userAgent, successCallback, failureCallback, attemptCount + 1));
                        }
                    }, error -> {
                        Log.e(TAG, "Census request failed on attempt " + (attemptCount + 1) + ": " + error.getMessage());
                        // Retry after delay
                        NominatimHostManager.addDelay(() -> 
                            geocodeWithRetry(context, address, userAgent, successCallback, failureCallback, attemptCount + 1));
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("User-Agent", userAgent);
                    return headers;
                }
            };
            jsonObjectRequest.setShouldCache(false);
            SunwiseApp.getInstance().getRequestQueue().add(jsonObjectRequest);
        } else {
            // Use JsonArrayRequest for Nominatim
            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                    (Request.Method.GET, geocodeUrl, null, response -> {
                        try {
                            GeocodingResponseParser.GeocodingResult result =
                                    GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                            if (result != null) {
                                NominatimHostManager.recordHostSuccess(geocodeUrl);
                                successCallback.onSuccess(result);
                            } else {
                                // No results, retry after delay
                                NominatimHostManager.addDelay(() -> 
                                    geocodeWithRetry(context, address, userAgent, successCallback, failureCallback, attemptCount + 1));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing Nominatim response on attempt " + (attemptCount + 1) + ": " + e.getMessage());
                            // Retry after delay
                            NominatimHostManager.addDelay(() -> 
                                geocodeWithRetry(context, address, userAgent, successCallback, failureCallback, attemptCount + 1));
                        }
                    }, error -> {
                        Log.e(TAG, "Nominatim request failed on attempt " + (attemptCount + 1) + ": " + error.getMessage());
                        // Retry after delay
                        NominatimHostManager.addDelay(() -> 
                            geocodeWithRetry(context, address, userAgent, successCallback, failureCallback, attemptCount + 1));
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("User-Agent", userAgent);
                    return headers;
                }
            };
            jsonArrayRequest.setShouldCache(false);
            SunwiseApp.getInstance().getRequestQueue().add(jsonArrayRequest);
        }
    }

    /**
     * Gets the next available API URL to try when no working hosts are known.
     * Cycles through all available APIs (Nominatim and Census) based on attempt count.
     * @param attemptCount The current attempt number
     * @return The next API URL to try, or null if no more APIs available
     */
    private static String getNextAvailableApiUrl(int attemptCount) {
        // Define all available API URLs
        String[] allApiUrls = {
            "https://osm-nominatim.gs.mil/search?q=",
            "https://nominatim.openstreetmap.org/search?q=",
            "https://geocoding.geo.census.gov/geocoder/locations/onelineaddress?address="
        };

        // Cycle through APIs based on attempt count
        int apiIndex = attemptCount % allApiUrls.length;
        return allApiUrls[apiIndex];
    }
}