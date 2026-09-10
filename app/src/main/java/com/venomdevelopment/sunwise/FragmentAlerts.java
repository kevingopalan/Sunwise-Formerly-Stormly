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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FragmentAlerts extends Fragment {

    private static final String TAG = "FragmentAlerts";
    private RecyclerView recyclerView;
    private TextView noDataTextView;
    private TextView locationTextView;
    private EditText searchInput;
    private Spinner savedLocationsSpinner;
    private AlertsRecyclerViewAdapter adapter;
    private LinearLayout progressBar;
    private ArrayAdapter<String> savedLocationsAdapter;

    private static final String BASE_URL_ALERTS = "https://api.weather.gov/alerts/active?point=";
    private static final String USER_AGENT = "Sunwise/v1 (venomdevelopmentofficial@gmail.com)" + System.getProperty("http.agent");
    private static final String myPref = "addressPref";
    private static final String PREF_SAVED_LOCATIONS = "saved_locations";

    private RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        requestQueue = SunwiseApp.getInstance().getRequestQueue();

        recyclerView = view.findViewById(R.id.alertsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AlertsRecyclerViewAdapter(getContext(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        progressBar = view.findViewById(R.id.progressBar);
        noDataTextView = view.findViewById(R.id.noData);
        locationTextView = view.findViewById(R.id.alertsLocation);
        searchInput = view.findViewById(R.id.alertsSearchInput);
        savedLocationsSpinner = view.findViewById(R.id.alertsSavedLocationsSpinner);

        savedLocationsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        savedLocationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        savedLocationsSpinner.setAdapter(savedLocationsAdapter);

        savedLocationsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position <= 0 || savedLocationsAdapter.getCount() <= 1) return;
                String selected = savedLocationsAdapter.getItem(position);
                if (selected == null || selected.equals("Select saved location")) return;
                locationTextView.setText(selected);
                fetchAlerts(selected);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        view.findViewById(R.id.alertsSearchButton).setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                locationTextView.setText(query);
                fetchAlerts(query);
            } else {
                Toast.makeText(getContext(), "Enter a location to search", Toast.LENGTH_SHORT).show();
            }
        });

        populateSavedLocations();

        String address = getPreferenceValue();
        if (address.isEmpty()) {
            if (savedLocationsAdapter.getCount() <= 1) {
                Toast.makeText(getContext(), "No address stored in preferences", Toast.LENGTH_SHORT).show();
            }
        } else {
            locationTextView.setText(address);
            fetchAlerts(address);
        }
        return view;
    }

    private void fetchAlerts(String address) {
        showLoading();
        GeocodingRetryManager.geocodeWithRetry(requireContext(), address, USER_AGENT, result -> {
            if (isAdded()) {
                String alertsUrl = BASE_URL_ALERTS + result.getLatitude() + "," + result.getLongitude();
                fetchAlertsData(alertsUrl);
            }
        }, errorMessage -> {
            if (isAdded()) {
                hideLoading();
                Toast.makeText(getContext(), "Geocoding failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchAlertsData(String alertsUrl) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, alertsUrl, null, response -> {
                    if (!isAdded()) return;
                    try {
                        JSONArray featuresArray = response.getJSONArray("features");

                        List<String> alertDescriptions = new ArrayList<>();
                        List<String> alertTypes = new ArrayList<>();
                        List<String> alertHeadlines = new ArrayList<>();

                        for (int i = 0; i < featuresArray.length(); i++) {
                            JSONObject alert = featuresArray.getJSONObject(i);
                            JSONObject properties = alert.getJSONObject("properties");
                            String event = properties.getString("event");
                            String headline = properties.getString("headline");
                            String description = properties.getString("description");
                            
                            String type;
                            if (event.toLowerCase().contains("watch")) type = "watch";
                            else if (event.toLowerCase().contains("warning")) type = "warning";
                            else if (event.toLowerCase().contains("advisory")) type = "advisory";
                            else type = "unknown";

                            alertHeadlines.add(event);
                            alertTypes.add(type);
                            alertDescriptions.add(headline + "\n\n" + description);
                        }

                        updateRecyclerView(alertHeadlines, alertTypes, alertDescriptions);
                        hideLoading();

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing alert data", e);
                        hideLoading();
                    }
                }, error -> {
                    Log.e(TAG, "Error fetching alerts", error);
                    if (isAdded()) hideLoading();
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                return headers;
            }
        };
        jsonObjectRequest.setShouldCache(false);
        requestQueue.add(jsonObjectRequest);
    }

    private void updateRecyclerView(List<String> alertHeadlines, List<String> alertTypes, List<String> alertDescriptions) {
        adapter = new AlertsRecyclerViewAdapter(getContext(), alertHeadlines, alertTypes, alertDescriptions);
        recyclerView.setAdapter(adapter);
        if (adapter.getItemCount() == 0) {
            noDataTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noDataTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading() { if (progressBar != null) progressBar.setVisibility(View.VISIBLE); }
    private void hideLoading() { if (progressBar != null) progressBar.setVisibility(View.GONE); }

    private void populateSavedLocations() {
        SharedPreferences sp = requireActivity().getSharedPreferences(myPref, 0);
        Set<String> set = sp.getStringSet(PREF_SAVED_LOCATIONS, new HashSet<>());
        List<String> locations = new ArrayList<>();
        if (set != null) locations.addAll(set);
        Collections.sort(locations);

        savedLocationsAdapter.clear();
        savedLocationsAdapter.add("Select saved location");
        savedLocationsAdapter.addAll(locations);
        savedLocationsAdapter.notifyDataSetChanged();
        savedLocationsSpinner.setSelection(0, false);
    }

    public String getPreferenceValue() {
        return requireActivity().getSharedPreferences(myPref, 0).getString("address", "");
    }
}
