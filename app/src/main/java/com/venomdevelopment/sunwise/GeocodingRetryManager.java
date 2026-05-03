package com.venomdevelopment.sunwise;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified utility class to manage geocoding requests with retry logic, rate limiting, and caching.
 */
public class GeocodingRetryManager {
    private static final String TAG = "GeocodingRetryManager";
    private static final Map<String, GeocodingResponseParser.GeocodingResult> cache = new ConcurrentHashMap<>();

    public interface GeocodingSuccessCallback {
        void onSuccess(GeocodingResponseParser.GeocodingResult result);
    }

    public interface GeocodingFailureCallback {
        void onFailure(String errorMessage);
    }

    /**
     * Main entry point for geocoding an address.
     */
    public static void geocodeWithRetry(Context context, String address, String userAgent,
                                       GeocodingSuccessCallback successCallback,
                                       GeocodingFailureCallback failureCallback) {
        geocodeWithRetry(context, address, userAgent, "us", successCallback, failureCallback);
    }

    /**
     * Geocodes with an optional country filter.
     */
    public static void geocodeWithRetry(Context context, String address, String userAgent,
                                       String countrycodes,
                                       GeocodingSuccessCallback successCallback,
                                       GeocodingFailureCallback failureCallback) {
        if (context == null) {
            failureCallback.onFailure("Context is null");
            return;
        }

        String cacheKey = address + (countrycodes != null ? "_" + countrycodes : "");
        if (cache.containsKey(cacheKey)) {
            Log.d(TAG, "Returning cached result for: " + address);
            successCallback.onSuccess(cache.get(cacheKey));
            return;
        }

        // Try System Geocoder first
        if (Geocoder.isPresent()) {
            new Thread(() -> {
                try {
                    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                    // Fetch multiple results to find a match for the requested country
                    List<Address> addresses = geocoder.getFromLocationName(address, 5);
                    Address bestMatch = null;
                    
                    if (addresses != null && !addresses.isEmpty()) {
                        if (countrycodes != null) {
                            for (Address addr : addresses) {
                                if (countrycodes.equalsIgnoreCase(addr.getCountryCode())) {
                                    bestMatch = addr;
                                    break;
                                }
                            }
                        } else {
                            bestMatch = addresses.get(0);
                        }
                    }

                    if (bestMatch != null) {
                        Address addr = bestMatch;
                        String displayName = addr.getAddressLine(0);
                        if (displayName == null) displayName = address;
                        
                        GeocodingResponseParser.GeocodingResult result = new GeocodingResponseParser.GeocodingResult(
                            String.valueOf(addr.getLatitude()),
                            String.valueOf(addr.getLongitude()),
                            displayName,
                            addr.getCountryCode()
                        );
                        new Handler(Looper.getMainLooper()).post(() -> {
                            cache.put(cacheKey, result);
                            successCallback.onSuccess(result);
                        });
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "System Geocoder failed, falling back to Nominatim", e);
                }
                // If system geocoder fails or doesn't find a matching country, fallback to Nominatim/Census
                new Handler(Looper.getMainLooper()).post(() -> performGeocode(context, address, userAgent, countrycodes, successCallback, failureCallback, 0));
            }).start();
        } else {
            performGeocode(context, address, userAgent, countrycodes, successCallback, failureCallback, 0);
        }
    }

    private static void performGeocode(Context context, String address, String userAgent,
                                      String countrycodes,
                                      GeocodingSuccessCallback successCallback,
                                      GeocodingFailureCallback failureCallback,
                                      int attemptCount) {
        if (attemptCount >= NominatimHostManager.getMaxRetryAttempts()) {
            failureCallback.onFailure("Max retries reached");
            return;
        }

        // Alternate between Nominatim and Census for retries
        String hostUrl = (attemptCount % 2 == 0) 
            ? NominatimHostManager.getPrimarySearchUrl() 
            : NominatimHostManager.getCensusGeocoderSearchUrl();

        String encodedAddress = address.replaceAll(" ", "+");
        final boolean isCensus = NominatimHostManager.isCensusGeocoderUrl(hostUrl);
        final String geocodeUrl;

        if (isCensus) {
            geocodeUrl = hostUrl + encodedAddress + NominatimHostManager.getCensusGeocoderParams();
        } else {
            String params = "format=json&addressdetails=1";
            if (countrycodes != null) params += "&countrycodes=" + countrycodes;
            geocodeUrl = hostUrl + encodedAddress + "&" + params;
        }

        if (isCensus) {
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, geocodeUrl, null,
                response -> handleResponse(context, address, countrycodes, response, geocodeUrl, userAgent, successCallback, failureCallback, attemptCount),
                error -> handleError(context, address, userAgent, countrycodes, error.getMessage(), successCallback, failureCallback, attemptCount)) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", userAgent);
                    return headers;
                }
            };
            request.setShouldCache(false);
            NominatimHostManager.enqueueRequest(request);
        } else {
            JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, geocodeUrl, null,
                response -> handleResponse(context, address, countrycodes, response, geocodeUrl, userAgent, successCallback, failureCallback, attemptCount),
                error -> handleError(context, address, userAgent, countrycodes, error.getMessage(), successCallback, failureCallback, attemptCount)) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", userAgent);
                    return headers;
                }
            };
            request.setShouldCache(false);
            NominatimHostManager.enqueueRequest(request);
        }
    }

    private static void handleResponse(Context context, String address, String countrycodes, Object response, String url, String userAgent,
                                      GeocodingSuccessCallback success, GeocodingFailureCallback failure, int attempt) {
        try {
            GeocodingResponseParser.GeocodingResult result = GeocodingResponseParser.parseGeocodingResponse(response, url);
            if (result != null) {
                // If a country filter was requested, verify it here for web-based fallbacks too
                if (countrycodes != null && result.getCountryCode() != null && !countrycodes.equalsIgnoreCase(result.getCountryCode())) {
                    // This result doesn't match the requested country, try next attempt
                    performGeocode(context, address, userAgent, countrycodes, success, failure, attempt + 1);
                    return;
                }

                NominatimHostManager.recordHostSuccess(url);
                String cacheKey = address + (countrycodes != null ? "_" + countrycodes : "");
                cache.put(cacheKey, result);
                success.onSuccess(result);
            } else {
                performGeocode(context, address, userAgent, countrycodes, success, failure, attempt + 1);
            }
        } catch (Exception e) {
            handleError(context, address, userAgent, countrycodes, e.getMessage(), success, failure, attempt);
        }
    }

    private static void handleError(Context context, String address, String userAgent, String countrycodes, String error,
                                   GeocodingSuccessCallback success, GeocodingFailureCallback failure, int attempt) {
        Log.e(TAG, "Geocoding error on attempt " + attempt + ": " + error);
        performGeocode(context, address, userAgent, countrycodes, success, failure, attempt + 1);
    }

    /**
     * Reverse geocodes coordinates. System Geocoder with Nominatim backup.
     */
    public static void reverseGeocode(Context context, double lat, double lon, String userAgent, GeocodingSuccessCallback callback, GeocodingFailureCallback failureCallback) {
        if (Geocoder.isPresent()) {
            new Thread(() -> {
                try {
                    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        String displayName = addr.getAddressLine(0);
                        GeocodingResponseParser.GeocodingResult result = new GeocodingResponseParser.GeocodingResult(
                            String.valueOf(lat),
                            String.valueOf(lon),
                            displayName,
                            addr.getCountryCode()
                        );
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(result));
                        return;
                    }
                } catch (Exception ignored) {}
                new Handler(Looper.getMainLooper()).post(() -> performReverseGeocodeBackup(lat, lon, userAgent, callback, failureCallback));
            }).start();
        } else {
            performReverseGeocodeBackup(lat, lon, userAgent, callback, failureCallback);
        }
    }

    private static void performReverseGeocodeBackup(double lat, double lon, String userAgent, GeocodingSuccessCallback callback, GeocodingFailureCallback failureCallback) {
        String url = NominatimHostManager.getReverseUrl() + "&lat=" + lat + "&lon=" + lon;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                GeocodingResponseParser.GeocodingResult result = GeocodingResponseParser.parseGeocodingResponse(response, url);
                if (result != null) {
                    callback.onSuccess(result);
                } else if (failureCallback != null) {
                    failureCallback.onFailure("Failed to parse reverse geocoding response");
                }
            }, 
            error -> {
                if (failureCallback != null) {
                    failureCallback.onFailure(error.getMessage());
                }
            }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", userAgent);
                return headers;
            }
        };
        request.setShouldCache(false);
        NominatimHostManager.enqueueRequest(request);
    }
}
