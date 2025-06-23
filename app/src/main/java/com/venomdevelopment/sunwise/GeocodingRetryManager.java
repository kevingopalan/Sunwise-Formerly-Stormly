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
        
        // Get a working host URL
        String workingHostUrl = NominatimHostManager.getWorkingHostUrl();
        if (workingHostUrl == null) {
            Log.w(TAG, "No working hosts available for retry");
            failureCallback.onFailure("No working geocoding services available");
            return;
        }
        
        // Encode the address
        String encodedAddress = address.replaceAll(" ", "+");
        final String geocodeUrl;
        final boolean isCensus = NominatimHostManager.isCensusGeocoderUrl(workingHostUrl);
        
        if (isCensus) {
            geocodeUrl = workingHostUrl + encodedAddress + NominatimHostManager.getCensusGeocoderParams();
        } else {
            geocodeUrl = workingHostUrl + encodedAddress + "&format=json&addressdetails=1";
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
} 