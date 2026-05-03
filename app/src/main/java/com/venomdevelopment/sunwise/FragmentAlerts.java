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
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FragmentAlerts extends Fragment {

    private static final String TAG = "FragmentAlerts";
    private RecyclerView recyclerView;
    private TextView noDataTextView;
    private AlertsRecyclerViewAdapter adapter;
    private LinearLayout progressBar;

    private static final String BASE_URL_ALERTS = "https://api.weather.gov/alerts/active?point=";
    private static final String USER_AGENT = "Sunwise/v1 (venomdevelopmentofficial@gmail.com)" + System.getProperty("http.agent");
    private static final String myPref = "addressPref";

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

        String address = getPreferenceValue();
        if (address.isEmpty()) {
            Toast.makeText(getContext(), "No address stored in preferences", Toast.LENGTH_SHORT).show();
        } else {
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

    public String getPreferenceValue() {
        return requireActivity().getSharedPreferences(myPref, 0).getString("address", "");
    }
}
