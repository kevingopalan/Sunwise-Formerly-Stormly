package com.venomdevelopment.sunwise;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FragmentAlerts extends Fragment {

    private static final String TAG = "FragmentAlerts";
    private RecyclerView recyclerView;
    private TextView noDataTextView;
    private AlertsRecyclerViewAdapter adapter;
    private LinearLayout progressBar;

    private static final String BASE_URL_POINTS = "https://api.weather.gov/alerts/active?point=";
    private static final String USER_AGENT = "Sunwise/v1 (venomdevelopmentofficial@gmail.com)" + System.getProperty("http.agent");  // Make sure to set a User-Agent
    private static final String myPref = "addressPref";  // Your SharedPreferences name

    private RequestQueue requestQueue;
    private final Set<String> processedGeocodeAddresses = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        // Initialize requestQueue FIRST!
        requestQueue = SunwiseApp.getInstance().getRequestQueue();

        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.alertsRecyclerView);  // Adjusted the RecyclerView ID
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the adapter
        adapter = new AlertsRecyclerViewAdapter(getContext(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        progressBar = view.findViewById(R.id.progressBar);
        noDataTextView = view.findViewById(R.id.noData);

        // Fetch address from SharedPreferences
        String address = getPreferenceValue();
        Log.d(TAG, "Address: " + address);
        if (address.isEmpty()) {
            Toast.makeText(getContext(), "No address stored in preferences", Toast.LENGTH_SHORT).show();
        } else {
            // Fetch coordinates using the stored address
            fetchGeocodingData(address);
        }

        return view;
    }

    private void fetchGeocodingData(String address) {
        if (processedGeocodeAddresses.contains(address)) return;
        showLoading();
        // Encode the address for the URL
        String encodedAddress = address.replaceAll(" ", "+");
        String baseUrl = NominatimHostManager.getRandomSearchUrl();
        final String geocodeUrl;
        final boolean isCensus = NominatimHostManager.isCensusGeocoderUrl(baseUrl);
        if (isCensus) {
            geocodeUrl = baseUrl + NominatimHostManager.getCensusGeocoderParams();
        } else {
            geocodeUrl = baseUrl + "&format=json&addressdetails=1";
        }

        if (isCensus) {
            // Use JsonObjectRequest for Census
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, geocodeUrl, null, response -> {
                        try {
                            GeocodingResponseParser.GeocodingResult result =
                                    GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                            if (result != null) {
                                NominatimHostManager.recordHostSuccess(geocodeUrl);
                                String pointsUrl = BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude();
                                fetchWeatherData(pointsUrl);
                                hideLoading();
                                processedGeocodeAddresses.add(address);
                            } else {
                                NominatimHostManager.addDelay(() -> {
                                    if (isAdded()) fetchGeocodingDataWithFallback(address);
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            NominatimHostManager.addDelay(() -> {
                                if (isAdded()) fetchGeocodingDataWithFallback(address);
                            });
                        }
                    }, error -> {
                        Log.e(TAG, "Error fetching geocoding data from Census Geocoder: " + error.getMessage());
                        NominatimHostManager.addDelay(() -> {
                            if (isAdded()) fetchGeocodingDataWithFallback(address);
                        });
                    }) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("User-Agent", USER_AGENT);
                    return headers;
                }
            };
            requestQueue.getCache().clear();
            jsonObjectRequest.setShouldCache(false);
            if (isAdded()) requestQueue.add(jsonObjectRequest);
        } else {
            // Use JsonArrayRequest for Nominatim
            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                    (Request.Method.GET, geocodeUrl, null, response -> {
                        try {
                            GeocodingResponseParser.GeocodingResult result =
                                    GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                            if (result != null) {
                                NominatimHostManager.recordHostSuccess(geocodeUrl);
                                String pointsUrl = BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude();
                                fetchWeatherData(pointsUrl);
                                hideLoading();
                                processedGeocodeAddresses.add(address);
                            } else {
                                NominatimHostManager.addDelay(() -> {
                                    if (isAdded()) fetchGeocodingDataWithFallback(address);
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            NominatimHostManager.addDelay(() -> {
                                if (isAdded()) fetchGeocodingDataWithFallback(address);
                            });
                        }
                    }, error -> {
                        Log.e(TAG, "Error fetching geocoding data from primary host: " + error.getMessage());
                        NominatimHostManager.addDelay(() -> {
                            if (isAdded()) fetchGeocodingDataWithFallback(address);
                        });
                    }) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("User-Agent", USER_AGENT);
                    return headers;
                }
            };
            requestQueue.getCache().clear();
            jsonArrayRequest.setShouldCache(false);
            if (isAdded()) requestQueue.add(jsonArrayRequest);
        }
    }

    private void fetchGeocodingDataWithFallback(String address) {
        if (processedGeocodeAddresses.contains(address)) return;
        if (!isAdded()) return;
        // Encode the address for the URL
        String encodedAddress = address.replaceAll(" ", "+");
        String geocodeUrl = NominatimHostManager.getFallbackSearchUrl() + encodedAddress + "&format=json&addressdetails=1";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, geocodeUrl, null, response -> {
                    try {
                        // Use the new parser to handle different API formats
                        GeocodingResponseParser.GeocodingResult result = 
                            GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                        
                        if (result != null) {
                            NominatimHostManager.recordHostSuccess(geocodeUrl);
                        // Build the points URL using the coordinates
                            String pointsUrl = BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude();
                        fetchWeatherData(pointsUrl);
                            hideLoading();
                            processedGeocodeAddresses.add(address);
                        } else {
                            // Try Census Geocoder as final fallback
                            NominatimHostManager.addDelay(() -> {
                                if (isAdded()) fetchGeocodingDataWithCensusFallback(address);
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Try Census Geocoder as final fallback
                        NominatimHostManager.addDelay(() -> {
                            if (isAdded()) fetchGeocodingDataWithCensusFallback(address);
                        });
                    }
                }, error -> {
                    Log.e(TAG, "Error fetching geocoding data from fallback host: " + error.getMessage());
                    // Try Census Geocoder as final fallback
                    NominatimHostManager.addDelay(() -> {
                        if (isAdded()) fetchGeocodingDataWithCensusFallback(address);
                    });
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                return headers;
            }
        };
        requestQueue.getCache().clear();
        jsonArrayRequest.setShouldCache(false);

        // Add the request to the Volley request queue
        if (isAdded()) requestQueue.add(jsonArrayRequest);
    }

    private void fetchGeocodingDataWithCensusFallback(String address) {
        if (processedGeocodeAddresses.contains(address)) return;
        if (!isAdded()) return;
        // Encode the address for the URL
        String encodedAddress = address.replaceAll(" ", "+");
        final String geocodeUrl = NominatimHostManager.getCensusGeocoderSearchUrl() + encodedAddress + NominatimHostManager.getCensusGeocoderParams();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, geocodeUrl, null, response -> {
                    try {
                        // Use the new parser to handle Census Geocoder format
                        GeocodingResponseParser.GeocodingResult result = 
                            GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                        
                        if (result != null) {
                            NominatimHostManager.recordHostSuccess(geocodeUrl);
                            // Build the points URL using the coordinates
                            String pointsUrl = BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude();
                            fetchWeatherData(pointsUrl);
                            hideLoading();
                            processedGeocodeAddresses.add(address);
                        } else {
                            // All hosts failed, try retry mechanism
                            tryRetryGeocoding(address);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        // All hosts failed, try retry mechanism
                        tryRetryGeocoding(address);
                    }
                }, error -> {
                    Log.e(TAG, "Error fetching geocoding data from Census Geocoder: " + error.getMessage());
                    // All hosts failed, try retry mechanism
                    tryRetryGeocoding(address);
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                return headers;
            }
        };
        requestQueue.getCache().clear();
        jsonObjectRequest.setShouldCache(false);

        // Add the request to the Volley request queue
        if (isAdded()) requestQueue.add(jsonObjectRequest);
    }

    private void tryRetryGeocoding(String address) {
        if (processedGeocodeAddresses.contains(address)) return;
        if (NominatimHostManager.hasSuccessfulHost()) {
            Context context = isAdded() ? requireContext() : null;
            if (context == null) {
                Log.w(TAG, "Context is null, cannot proceed with retry geocoding");
                hideLoading();
                return;
            }
            showLoading();
            GeocodingRetryManager.geocodeWithRetry(
                context,
                address,
                USER_AGENT,
                result -> {
                    if (isAdded()) {
                        String pointsUrl = BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude();
                        fetchWeatherData(pointsUrl);
                    }
                    hideLoading();
                },
                errorMessage -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                    hideLoading();
                }
            );
        } else {
            if (isAdded()) {
                Toast.makeText(getContext(), "Error fetching geocoding data from all available services", Toast.LENGTH_SHORT).show();
            }
            hideLoading();
        }
    }

    private void fetchWeatherData(String pointsUrl) {
        // Fetch weather alerts based on the coordinates (pointsUrl)
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, pointsUrl, null, response -> {
                    try {
                        // Parse the JSON response for weather alerts
                        JSONArray featuresArray = response.getJSONArray("features");

                        List<String> alertDescriptions = new ArrayList<>();
                        List<String> alertTypes = new ArrayList<>();
                        List<String> alertHeadlines = new ArrayList<>();

                        // Loop through the alerts and add them to the lists
                        for (int i = 0; i < featuresArray.length(); i++) {
                            JSONObject alert = featuresArray.getJSONObject(i);
                            JSONObject properties = alert.getJSONObject("properties");
                            String headline = properties.getString("headline");
                            String type;
                            Log.d(TAG, "Headline: " + headline);
                            String event = properties.getString("event");
                            String description = properties.getString("description");
                            if (event.toLowerCase().contains("watch")) {
                                type = "watch";
                            } else if (event.toLowerCase().contains("warning")) {
                                type = "warning";
                            } else if (event.toLowerCase().contains("advisory")) {
                                type = "advisory";
                            } else {
                                type = "unknown";
                            }
                            alertHeadlines.add(event);
                            alertTypes.add(type);
                            alertDescriptions.add(headline + System.lineSeparator() + System.lineSeparator() + description);
                        }

                        // Update the RecyclerView with the alerts
                        new Handler(Looper.getMainLooper()).post(() -> updateRecyclerView(alertHeadlines, alertTypes, alertDescriptions));

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing weather data", e);
                    }
                }, error -> {
                    // Handle error
                    Log.e(TAG, "Error fetching weather alerts", error);
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                // Set User-Agent here for NWS request
                headers.put("User-Agent", USER_AGENT);  // Use the same User-Agent defined for Nominatim
                return headers;
            }
        };
        requestQueue.getCache().clear();
        jsonObjectRequest.setShouldCache(false);

        // Add the request to the Volley request queue
        if (isAdded()) requestQueue.add(jsonObjectRequest);
    }

    private void updateRecyclerView(List<String> alertHeadlines, List<String> alertTypes, List<String> alertDescriptions) {
        // Update the adapter with the new data
        adapter = new AlertsRecyclerViewAdapter(getContext(), alertHeadlines, alertTypes, alertDescriptions);
        recyclerView.setAdapter(adapter);
        if (adapter != null && adapter.getItemCount() == 0) {
            noDataTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noDataTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setAlpha(1f);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (progressBar != null && progressBar.getVisibility() == View.VISIBLE) {
            progressBar.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> progressBar.setVisibility(View.GONE))
                .start();
        }
    }

    // Method to get the stored address from SharedPreferences
    public String getPreferenceValue() {
        SharedPreferences sp = requireActivity().getSharedPreferences(myPref, 0);
        return sp.getString("address", "");
    }

}
