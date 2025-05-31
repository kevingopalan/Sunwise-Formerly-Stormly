package com.venomdevelopment.sunwise;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FragmentAlerts extends Fragment {

    private static final String TAG = "FragmentAlerts";
    private RecyclerView recyclerView;
    private AlertsRecyclerViewAdapter adapter;

    private static final String NOMINATIM_URL = "https://osm-nominatim.gs.mil/search?q=";
    private static final String BASE_URL_POINTS = "https://api.weather.gov/alerts/active?point=";
    private static final String USER_AGENT = "Mozilla/5.0";  // Make sure to set a User-Agent
    private static final String myPref = "addressPref";  // Your SharedPreferences name

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.alertsRecycler);  // Adjusted the RecyclerView ID
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the adapter
        adapter = new AlertsRecyclerViewAdapter(getContext(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());;
        recyclerView.setAdapter(adapter);

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
        // Encode the address for the URL
        String encodedAddress = address.replaceAll(" ", "+");
        String geocodeUrl = NOMINATIM_URL + encodedAddress + "&format=json&addressdetails=1";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, geocodeUrl, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            // Get the first result
                            JSONObject firstResult = response.getJSONObject(0);
                            String lat = firstResult.getString("lat");
                            String lon = firstResult.getString("lon");

                            // Build the points URL using the coordinates
                            String pointsUrl = BASE_URL_POINTS + lat + "," + lon;
                            fetchWeatherData(pointsUrl);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error parsing geocoding data", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(com.android.volley.VolleyError error) {
                        Log.e(TAG, "Error fetching geocoding data: " + error.getMessage());
                        Toast.makeText(getContext(), "Error fetching geocoding data", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                return headers;
            }
        };

        // Add the request to the Volley request queue
        Volley.newRequestQueue(getContext()).add(jsonArrayRequest);
    }

    private void fetchWeatherData(String pointsUrl) {
        // Fetch weather alerts based on the coordinates (pointsUrl)
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, pointsUrl, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
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
                                String type = "";
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
                                alertDescriptions.add(headline + System.getProperty("line.separator") + System.getProperty("line.separator") + description);
                            }

                            // Update the RecyclerView with the alerts
                            new Handler(Looper.getMainLooper()).post(() -> updateRecyclerView(alertHeadlines, alertTypes, alertDescriptions));

                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing weather data", e);
                        }
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

        // Add the request to the Volley request queue
        Volley.newRequestQueue(getContext()).add(jsonObjectRequest);
    }

    private void updateRecyclerView(List<String> alertHeadlines, List<String> alertTypes, List<String> alertDescriptions) {
        // Update the adapter with the new data
        adapter = new AlertsRecyclerViewAdapter(getContext(), alertHeadlines, alertTypes, alertDescriptions);
        recyclerView.setAdapter(adapter);
    }


    // Method to get the stored address from SharedPreferences
    public String getPreferenceValue() {
        SharedPreferences sp = getActivity().getSharedPreferences(myPref, 0);
        return sp.getString("address", "");
    }

    // Method to store an address in SharedPreferences
    public void writeToPreference(String thePreference) {
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(myPref, 0).edit();
        editor.putString("address", thePreference);
        editor.commit();
    }

    // Models for the NWS API response
    public static class Alert {
        Properties properties;
    }

    public static class Properties {
        String event;
        String headline;
    }
}
