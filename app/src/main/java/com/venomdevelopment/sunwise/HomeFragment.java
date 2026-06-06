package com.venomdevelopment.sunwise;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HomeFragment extends Fragment implements SavedLocationAdapter.OnLocationClickListener {
    private final List<String> usSavedLocationsList = new ArrayList<>();
    private final List<String> usDetectedLocationList = new ArrayList<>();

    private static final String TAG = "HomeFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String USER_AGENT = "Sunwise/v1 (venomdevelopmentofficial@gmail.com)" + System.getProperty("http.agent");
    private static final String PREF_SAVED_LOCATIONS = "saved_locations";

    private AutoCompleteTextView search;
    private Button locationButton;
    private SavedLocationAdapter savedLocationAdapter;
    private final List<String> savedLocationsList = new ArrayList<>();
    private final List<String> originalSavedLocationsList = new ArrayList<>();
    private LocationManager locationManager;
    private ArrayAdapter<String> suggestionAdapter;
    private final List<String> suggestionList = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable suggestionRunnable;
    private Runnable showDropdownRunnable;
    private static final int AUTOCOMPLETE_DEBOUNCE_MS = 300;
    private static final int AUTOCOMPLETE_SHOW_DELAY_MS = 50;
    private LocationListener activeLocationListener;
    private RequestQueue requestQueue;
    private boolean autoLocationEnabled = true;
    private SharedPreferences sunwisePrefs;
    private SavedLocationAdapter detectedLocationAdapter;
    private TextView noSavedLocationsPlaceholder;
    private TextView noDetectedLocationsPlaceholder;
    private final List<String> detectedLocationList = new ArrayList<>();
    private WeatherViewModel weatherViewModel;
    private final Handler reloadHandler = new Handler(Looper.getMainLooper());
    private int weatherReloadAttempts = 0;
    private static final int MAX_WEATHER_RELOADS = 12;
    private static final int RELOAD_DELAY_MS = 2000;

    private static final int LOCATION_TIMEOUT_MS = 15000;
    private boolean isLocationDetectionInProgress = false;
    private boolean isSavedLocationsLoading = false;
    private FrameLayout locationNeeded;
    private TextView locationPermissionDeniedText;

    private final Runnable fetchWeatherRunnable = this::attemptWeatherDataFetch;

    public static final String myPref = "addressPref";

    public interface OnNavigateToForecastListener {
        void onNavigateToForecast(String location);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        initViews(v);
        setupRecyclerViews(v);

        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        sunwisePrefs = requireContext().getSharedPreferences("SunwiseSettings", Context.MODE_PRIVATE);
        autoLocationEnabled = sunwisePrefs.getBoolean("auto_location_enabled", true);
        locationButton.setVisibility(autoLocationEnabled ? View.GONE : View.VISIBLE);

        weatherViewModel.getLocationWeatherMap().observe(getViewLifecycleOwner(), map -> updateAdaptersWithWeatherData());

        // Initial load of saved locations
        loadSavedLocations();

        if (autoLocationEnabled) {
            if (checkLocationPermission()) {
                getCurrentLocation();
            } else {
                requestLocationPermission();
            }
        }

        return v;
    }

    private void initViews(View v) {
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        search = v.findViewById(R.id.text_search);
        locationNeeded = v.findViewById(R.id.locationNeeded);
        locationPermissionDeniedText = v.findViewById(R.id.locationpermgotdenied);
        locationButton = v.findViewById(R.id.locationButton);
        requestQueue = SunwiseApp.getInstance().getRequestQueue();
        locationNeeded.setClickable(true);

        suggestionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.select_dialog_item, suggestionList);
        search.setAdapter(suggestionAdapter);
        search.setThreshold(3);
        search.setOnItemClickListener((parent, view, position, id) -> {
            String suggestion = (String) parent.getItemAtPosition(position);
            checkCountryAndProceed(suggestion);
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleSuggestionFetch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        v.findViewById(R.id.search).setOnClickListener(v1 -> performSearch());
        locationButton.setOnClickListener(v1 -> {
            if (checkLocationPermission()) getCurrentLocation();
            else requestLocationPermission();
        });
        locationNeeded.setOnClickListener(v1 -> openAppSettings());
    }

    private void setupRecyclerViews(View v) {
        RecyclerView savedRv = v.findViewById(R.id.savedLocationsRecyclerView);
        savedRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        savedLocationAdapter = new SavedLocationAdapter(savedLocationsList, this);
        savedRv.setAdapter(savedLocationAdapter);

        noSavedLocationsPlaceholder = v.findViewById(R.id.noSavedLocationsPlaceholder);
        noDetectedLocationsPlaceholder = v.findViewById(R.id.locationfetch);

        RecyclerView detectedRv = v.findViewById(R.id.detectedLocationRecyclerView);
        detectedRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        detectedLocationAdapter = new SavedLocationAdapter(detectedLocationList, this);
        detectedRv.setAdapter(detectedLocationAdapter);

        ((EditText)v.findViewById(R.id.savedLocationsSearch)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterSavedLocations(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch() {
        String addr = search.getText().toString().trim();
        if (!addr.isEmpty()) checkCountryAndProceed(addr);
        else Toast.makeText(requireContext(), "Please enter an address", Toast.LENGTH_SHORT).show();
    }

    private void loadSavedLocations() {
        SharedPreferences sp = requireActivity().getSharedPreferences(myPref, 0);
        Set<String> savedSet = sp.getStringSet(PREF_SAVED_LOCATIONS, new HashSet<>());
        
        List<String> list = new ArrayList<>(savedSet);
        usSavedLocationsList.clear();
        usSavedLocationsList.addAll(list);
        
        originalSavedLocationsList.clear();
        originalSavedLocationsList.addAll(list);
        savedLocationsList.clear();
        savedLocationsList.addAll(list);
        savedLocationAdapter.notifyDataSetChanged();
        updateSavedLocationsPlaceholder();
        
        startWeatherDataRetry();
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        isLocationDetectionInProgress = true;

        Location last = getBestLastKnownLocation();
        if (last != null && (System.currentTimeMillis() - last.getTime()) < 60000) {
            reverseGeocode(last);
            return;
        }

        cancelLocationUpdates();

        LocationListener locationListener = new LocationListener() {
            @Override public void onLocationChanged(@NonNull Location l) {
                cancelLocationUpdates();
                reverseGeocode(l);
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
        };

        activeLocationListener = locationListener;
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }

        reloadHandler.postDelayed(() -> {
            if (isLocationDetectionInProgress) {
                isLocationDetectionInProgress = false;
                startWeatherDataRetry();
            }
        }, LOCATION_TIMEOUT_MS);
    }

    @SuppressLint("MissingPermission")
    private Location getBestLastKnownLocation() {
        Location best = null;
        for (String provider : Arrays.asList(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) {
            if (!locationManager.isProviderEnabled(provider)) continue;
            Location location = locationManager.getLastKnownLocation(provider);
            if (location == null) continue;
            if (best == null || location.getTime() > best.getTime()) {
                best = location;
            }
        }
        return best;
    }

    private void reverseGeocode(Location l) {
        GeocodingRetryManager.reverseGeocode(requireContext(), l.getLatitude(), l.getLongitude(), USER_AGENT, result -> {
            if (isAdded()) {
                String name = result.getDisplayName();
                detectedLocationList.clear();
                detectedLocationList.add(name);
                detectedLocationAdapter.notifyDataSetChanged();
                writeToPreference(name);
                usDetectedLocationList.clear();
                usDetectedLocationList.add(name);
                isLocationDetectionInProgress = false;
                noDetectedLocationsPlaceholder.setVisibility(View.GONE);
                startWeatherDataRetry();
            }
        }, err -> {
            if (isAdded()) {
                isLocationDetectionInProgress = false;
                startWeatherDataRetry();
            }
        });
    }

    private void checkCountryAndProceed(String address) {
        GeocodingRetryManager.geocodeWithRetry(requireContext(), address, USER_AGENT, "us", result -> {
            if (!isAdded()) return;
            new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Location")
                .setMessage("Use this location?\n\n" + result.getDisplayName())
                .setPositiveButton("Yes", (d, w) -> {
                    navigateToForecast(address);
                })
                .setNegativeButton("No", (d, w) -> {})
                .show();
        }, err -> {
            if (!isAdded()) return;
            new AlertDialog.Builder(requireContext())
                .setTitle("Location Not Found")
                .setMessage("We couldn't find a location matching \"" + address + "\" in the United States. Please try a different search.")
                .setPositiveButton("OK", null)
                .show();
        });
    }

    private void navigateToForecast(String loc) {
        if (getActivity() instanceof OnNavigateToForecastListener) {
            ((OnNavigateToForecastListener) getActivity()).onNavigateToForecast(loc);
        }
    }

    private void writeToPreference(String loc) {
        requireActivity().getSharedPreferences(myPref, 0).edit().putString("address", loc).apply();
    }

    private void startWeatherDataRetry() {
        weatherReloadAttempts = 0;
        reloadHandler.removeCallbacks(fetchWeatherRunnable);
        attemptWeatherDataFetch();
    }

    private void attemptWeatherDataFetch() {
        if (!isAdded()) return;
        List<String> all = new ArrayList<>(usSavedLocationsList);
        all.addAll(usDetectedLocationList);
        
        Map<String, WeatherViewModel.WeatherSummary> currentMap = weatherViewModel.getLocationWeatherMap().getValue();
        List<String> needingWeather = new ArrayList<>();
        
        for (String loc : all) {
            WeatherViewModel.WeatherSummary summary = currentMap != null ? currentMap.get(loc) : null;
            if (summary == null || summary.temperature == null || summary.temperature.equals("--")) {
                needingWeather.add(loc);
            }
        }

        if (!needingWeather.isEmpty()) {
            weatherViewModel.fetchWeatherForLocations(requireContext(), needingWeather);
            if (weatherReloadAttempts++ < MAX_WEATHER_RELOADS) {
                reloadHandler.postDelayed(fetchWeatherRunnable, RELOAD_DELAY_MS);
            }
        } else {
            reloadHandler.removeCallbacks(fetchWeatherRunnable);
        }
    }

    private void updateAdaptersWithWeatherData() {
        if (!isAdded()) return;
        Map<String, WeatherViewModel.WeatherSummary> map = weatherViewModel.getLocationWeatherMap().getValue();
        if (map == null) return;
        savedLocationAdapter.setWeatherSummaries(map);
        detectedLocationAdapter.setWeatherSummaries(map);
        savedLocationAdapter.notifyDataSetChanged();
        detectedLocationAdapter.notifyDataSetChanged();
    }

    private void filterSavedLocations(String q) {
        List<String> filtered = new ArrayList<>();
        for (String loc : originalSavedLocationsList) if (loc.toLowerCase().contains(q.toLowerCase())) filtered.add(loc);
        savedLocationsList.clear();
        savedLocationsList.addAll(filtered);
        savedLocationAdapter.notifyDataSetChanged();
        updateSavedLocationsPlaceholder();
    }

    private void updateSavedLocationsPlaceholder() {
        boolean hasSavedLocations = !originalSavedLocationsList.isEmpty();
        noSavedLocationsPlaceholder.setVisibility(hasSavedLocations ? View.GONE : View.VISIBLE);
        RecyclerView savedRv = getView() != null ? getView().findViewById(R.id.savedLocationsRecyclerView) : null;
        if (savedRv != null) {
            savedRv.setVisibility(hasSavedLocations ? View.VISIBLE : View.GONE);
        }
    }

    private void scheduleSuggestionFetch(String query) {
        if (suggestionRunnable != null) {
            searchHandler.removeCallbacks(suggestionRunnable);
        }
        if (query.trim().isEmpty()) {
            clearSuggestions();
            return;
        }
        suggestionRunnable = () -> fetchLocationSuggestions(query.trim());
        searchHandler.postDelayed(suggestionRunnable, AUTOCOMPLETE_DEBOUNCE_MS);
    }

    private void fetchLocationSuggestions(String query) {
        suggestionList.clear();
        suggestionAdapter.clear();
        suggestionAdapter.notifyDataSetChanged();

        if (requestQueue == null) {
            Log.w(TAG, "Request queue not initialized");
            return;
        }

        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
            String url = "https://photon.komoot.io/api/?q=" + encoded + "&limit=5&countrycode=us";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        JSONArray features = response.optJSONArray("features");
                        if (features != null) {
                            for (int i = 0; i < features.length(); i++) {
                                JSONObject feature = features.optJSONObject(i);
                                if (feature == null) continue;
                                JSONObject props = feature.optJSONObject("properties");
                                if (props == null) continue;
                                String suggestion = buildSuggestionFromProperties(props);
                                if (!suggestion.isEmpty() && !suggestionList.contains(suggestion)) {
                                    suggestionList.add(suggestion);
                                }
                            }
                        }
                        suggestionAdapter.clear();
                        suggestionAdapter.addAll(suggestionList);
                        suggestionAdapter.notifyDataSetChanged();
                        if (showDropdownRunnable != null) {
                            searchHandler.removeCallbacks(showDropdownRunnable);
                        }
                        if (!suggestionList.isEmpty() && search.hasFocus()) {
                            showDropdownRunnable = search::showDropDown;
                            searchHandler.postDelayed(showDropdownRunnable, AUTOCOMPLETE_SHOW_DELAY_MS);
                        }
                    }, error -> {
                        Log.e(TAG, "Suggestion request failed", error);
                        suggestionAdapter.notifyDataSetChanged();
                    });
            requestQueue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch suggestions", e);
            suggestionAdapter.notifyDataSetChanged();
        }
    }

    private void clearSuggestions() {
        suggestionList.clear();
        suggestionAdapter.clear();
        suggestionAdapter.notifyDataSetChanged();
        if (showDropdownRunnable != null) {
            searchHandler.removeCallbacks(showDropdownRunnable);
            showDropdownRunnable = null;
        }
    }

    private String buildSuggestionFromProperties(JSONObject props) {
        String name = props.optString("name", "").trim();
        if (name.isEmpty()) return "";
        String city = props.optString("city", "").trim();
        String state = props.optString("state", "").trim();
        ArrayList<String> chunks = new ArrayList<>();
        chunks.add(name);
        if (!city.isEmpty() && !city.equalsIgnoreCase(name)) {
            chunks.add(city);
        }
        if (!state.isEmpty() && !state.equalsIgnoreCase(name) && !state.equalsIgnoreCase(city)) {
            chunks.add(state);
        }
        return TextUtils.join(", ", chunks);
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) return;
        boolean granted = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                granted = true;
                break;
            }
        }
        if (granted) {
            showLocationNeeded(false);
            getCurrentLocation();
        } else {
            showLocationNeeded(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isAdded()) return;
        if (autoLocationEnabled) {
            if (checkLocationPermission()) {
                if (locationNeeded.getVisibility() == View.VISIBLE) {
                    showLocationNeeded(false);
                }
                if (!isLocationDetectionInProgress && detectedLocationList.isEmpty()) {
                    getCurrentLocation();
                }
            } else if (locationNeeded.getVisibility() != View.VISIBLE) {
                requestLocationPermission();
            }
        }
    }

    @Override
    public void onPause() {
        stopHomeWork();
        super.onPause();
    }

    private void showLocationNeeded(boolean show) {
        locationNeeded.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            locationPermissionDeniedText.setText("Location permission is required to detect your current location. Tap to open app settings.");
            locationButton.setVisibility(View.GONE);
        } else {
            locationButton.setVisibility(autoLocationEnabled ? View.GONE : View.VISIBLE);
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void cancelLocationUpdates() {
        if (locationManager != null && activeLocationListener != null) {
            locationManager.removeUpdates(activeLocationListener);
            activeLocationListener = null;
        }
    }

    private void stopHomeWork() {
        cancelLocationUpdates();
        reloadHandler.removeCallbacksAndMessages(null);
        clearSuggestions();
        isLocationDetectionInProgress = false;
    }

    @Override public void onLocationClick(String loc) { navigateToForecast(loc); }
    @Override public void onDestroyView() { stopHomeWork(); super.onDestroyView(); }
}
