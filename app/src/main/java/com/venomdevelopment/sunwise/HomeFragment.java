package com.venomdevelopment.sunwise;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HomeFragment extends Fragment implements SavedLocationAdapter.OnLocationClickListener {
    private final List<String> usSavedLocationsList = new ArrayList<>();
    private final List<String> usDetectedLocationList = new ArrayList<>();
    private RecyclerView suggestionsRecyclerView;
    private LocationSuggestionAdapter suggestionAdapter;
    private final List<String> suggestionList = new ArrayList<>();

    private static final String TAG = "HomeFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String USER_AGENT = "Sunwise/v1 (venomdevelopmentofficial@gmail.com)" + System.getProperty("http.agent");
    private static final String PREF_SAVED_LOCATIONS = "saved_locations";

    private EditText search;
    private Button locationButton;
    private SavedLocationAdapter savedLocationAdapter;
    private final List<String> savedLocationsList = new ArrayList<>();
    private final List<String> originalSavedLocationsList = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private CancellationTokenSource cancellationTokenSource;
    private RequestQueue requestQueue;
    private LinearLayout progressBar;
    private boolean autoLocationEnabled = true;
    private SharedPreferences sunwisePrefs;
    private SavedLocationAdapter detectedLocationAdapter;
    private final List<String> detectedLocationList = new ArrayList<>();
    private WeatherViewModel weatherViewModel;
    private final Handler reloadHandler = new Handler(Looper.getMainLooper());
    private int weatherReloadAttempts = 0;
    private static final int MAX_WEATHER_RELOADS = 10;
    private static final int RELOAD_DELAY_MS = 2000;

    private static final int LOCATION_TIMEOUT_MS = 10000;
    private boolean isLocationDetectionInProgress = false;
    private long locationDetectionStartTime = 0;
    private boolean hasLocationTimeoutOccurred = false;

    private final Runnable fetchWeatherRunnable = this::attemptWeatherDataFetch;

    public static final String myPref = "addressPref";

    public String getPreferenceValue() {
        SharedPreferences sp = requireActivity().getSharedPreferences(myPref, 0);
        return sp.getString("address", "");
    }

    public void writeToPreference(String thePreference) {
        if (!isAdded() || getActivity() == null) return;
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(myPref, 0).edit();
        editor.putString("address", thePreference);
        editor.apply();
    }

    private Set<String> getSavedLocations() {
        if (!isAdded() || getActivity() == null) return new HashSet<>();
        SharedPreferences prefs = requireActivity().getSharedPreferences(myPref, 0);
        return prefs.getStringSet(PREF_SAVED_LOCATIONS, new HashSet<>());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        if (!checkLocationPermission()) {
            requestLocationPermission();
        }
        
        search = v.findViewById(R.id.text_search);
        suggestionsRecyclerView = v.findViewById(R.id.suggestionsRecyclerView);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        suggestionAdapter = new LocationSuggestionAdapter(suggestionList, suggestion -> {
            search.setText(suggestion);
            suggestionsRecyclerView.setVisibility(View.GONE);
            checkCountryAndProceed(suggestion);
        });
        suggestionsRecyclerView.setAdapter(suggestionAdapter);
        suggestionsRecyclerView.setVisibility(View.GONE);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() < 3 || !search.hasFocus()) {
                    suggestionList.clear();
                    suggestionAdapter.notifyDataSetChanged();
                    suggestionsRecyclerView.setVisibility(View.GONE);
                    return;
                }
                fetchLocationSuggestions(query);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        search.setOnFocusChangeListener((v1, hasFocus) -> {
            if (!hasFocus) {
                suggestionsRecyclerView.setVisibility(View.GONE);
            } else if (!suggestionList.isEmpty()) {
                suggestionsRecyclerView.setVisibility(View.VISIBLE);
            }
        });

        Button searchButton = v.findViewById(R.id.search);
        locationButton = v.findViewById(R.id.locationButton);
        RecyclerView detectedLocationRecyclerView = v.findViewById(R.id.detectedLocationRecyclerView);
        RecyclerView savedLocationsRecyclerView = v.findViewById(R.id.savedLocationsRecyclerView);
        EditText savedLocationsSearch = v.findViewById(R.id.savedLocationsSearch);
        progressBar = v.findViewById(R.id.progressBar);
        requestQueue = SunwiseApp.getInstance().getRequestQueue();

        savedLocationsRecyclerView.setNestedScrollingEnabled(false);
        savedLocationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        savedLocationAdapter = new SavedLocationAdapter(savedLocationsList, this);
        savedLocationsRecyclerView.setAdapter(savedLocationAdapter);

        detectedLocationRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        detectedLocationAdapter = new SavedLocationAdapter(detectedLocationList, this);
        detectedLocationRecyclerView.setAdapter(detectedLocationAdapter);

        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        loadSavedLocations();

        savedLocationsSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSavedLocations(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        search.setOnEditorActionListener((v1, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String address = search.getText().toString().trim();
                if (!address.isEmpty()) {
                    checkCountryAndProceed(address);
                } else {
                    Toast.makeText(requireContext(), "Please enter an address", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        searchButton.setOnClickListener(v1 -> {
            String address = search.getText().toString().trim();
            if (!address.isEmpty()) {
                checkCountryAndProceed(address);
            } else {
                Toast.makeText(requireContext(), "Please enter an address", Toast.LENGTH_SHORT).show();
            }
        });

        locationButton.setOnClickListener(v1 -> {
            if (checkLocationPermission()) {
                getCurrentLocation();
            } else {
                requestLocationPermission();
            }
        });

        search.setText(getPreferenceValue(), TextView.BufferType.EDITABLE);

        sunwisePrefs = requireContext().getSharedPreferences("SunwiseSettings", Context.MODE_PRIVATE);
        autoLocationEnabled = sunwisePrefs.getBoolean("auto_location_enabled", true);
        locationButton.setVisibility(autoLocationEnabled ? View.GONE : View.VISIBLE);

        if (autoLocationEnabled && checkLocationPermission()) {
            getCurrentLocation();
        }

        weatherViewModel.getLocationWeatherMap().observe(getViewLifecycleOwner(), locationWeatherMap -> {
            updateAdaptersWithWeatherData();
        });

        showLoading();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean currentAutoLocationEnabled = sunwisePrefs.getBoolean("auto_location_enabled", true);
        if (currentAutoLocationEnabled != autoLocationEnabled) {
            autoLocationEnabled = currentAutoLocationEnabled;
            if (locationButton != null) {
                locationButton.setVisibility(autoLocationEnabled ? View.GONE : View.VISIBLE);
            }
        }
    }

    private void loadSavedLocations() {
        Set<String> savedSet = getSavedLocations();
        List<String> savedList = new ArrayList<>(savedSet);
        
        if (savedSet.isEmpty()) {
            originalSavedLocationsList.clear();
            savedLocationsList.clear();
            synchronized(usSavedLocationsList) { usSavedLocationsList.clear(); }
            savedLocationAdapter.notifyDataSetChanged();
            startWeatherDataRetry();
            return;
        }

        if (!originalSavedLocationsList.equals(savedList) || usSavedLocationsList.isEmpty()) {
            originalSavedLocationsList.clear();
            originalSavedLocationsList.addAll(savedList);
            
            savedLocationsList.clear();
            synchronized(usSavedLocationsList) { usSavedLocationsList.clear(); }
            final int[] completed = {0};
            
            for (String location : savedSet) {
                GeocodingRetryManager.geocodeWithRetry(requireContext(), location, USER_AGENT, "us",
                    result -> {
                        if (result != null && "us".equalsIgnoreCase(result.getCountryCode())) {
                            synchronized(usSavedLocationsList) { usSavedLocationsList.add(location); }
                        }
                        completed[0]++;
                        if (completed[0] == savedSet.size()) {
                            finalizeSavedLocationsLoading();
                        }
                    },
                    errorMessage -> {
                        completed[0]++;
                        if (completed[0] == savedSet.size()) {
                            finalizeSavedLocationsLoading();
                        }
                    }
                );
            }
        } else {
            startWeatherDataRetry();
        }
    }

    private void finalizeSavedLocationsLoading() {
        savedLocationsList.clear();
        synchronized(usSavedLocationsList) {
            savedLocationsList.addAll(usSavedLocationsList);
        }
        savedLocationAdapter.notifyDataSetChanged();
        startWeatherDataRetry();
    }

    private void filterSavedLocations(String query) {
        if (query.isEmpty()) {
            if (!savedLocationsList.equals(originalSavedLocationsList)) {
                savedLocationsList.clear();
                savedLocationsList.addAll(originalSavedLocationsList);
                savedLocationAdapter.notifyDataSetChanged();
            }
        } else {
            List<String> filtered = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase();
            for (String location : originalSavedLocationsList) {
                if (location.toLowerCase().contains(lowerCaseQuery)) {
                    filtered.add(location);
                }
            }
            if (!savedLocationsList.equals(filtered)) {
                savedLocationsList.clear();
                savedLocationsList.addAll(filtered);
                savedLocationAdapter.notifyDataSetChanged();
            }
        }
    }

    private void fetchLocationSuggestions(String query) {
        String encodedQuery = query.replaceAll(" ", "+");
        String url = NominatimHostManager.getRandomSearchUrl() + encodedQuery + "&format=json&addressdetails=1&countrycodes=us";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                suggestionList.clear();
                for (int i = 0; i < response.length(); i++) {
                    try {
                        String displayName = response.getJSONObject(i).optString("display_name", "");
                        if (!displayName.isEmpty()) suggestionList.add(displayName);
                    } catch (JSONException ignored) {}
                }
                if (!suggestionList.isEmpty()) {
                    suggestionAdapter.notifyDataSetChanged();
                    suggestionsRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    suggestionsRecyclerView.setVisibility(View.GONE);
                }
            }, error -> {
                suggestionList.clear();
                suggestionAdapter.notifyDataSetChanged();
                suggestionsRecyclerView.setVisibility(View.GONE);
            }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                return headers;
            }
        };
        requestQueue.add(request);
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (checkLocationPermission()) {
                getCurrentLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (fusedLocationClient == null) return;

        showLoading();
        
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
        }
        cancellationTokenSource = new CancellationTokenSource();

        isLocationDetectionInProgress = true;
        locationDetectionStartTime = System.currentTimeMillis();
        hasLocationTimeoutOccurred = false;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && (System.currentTimeMillis() - location.getTime()) < 60000) {
                Log.d(TAG, "Using recent last location");
                isLocationDetectionInProgress = false;
                reverseGeocode(location.getLatitude(), location.getLongitude());
            } else {
                requestFreshLocation();
            }
        }).addOnFailureListener(e -> requestFreshLocation());
    }

    @SuppressLint("MissingPermission")
    private void requestFreshLocation() {
        reloadHandler.postDelayed(() -> {
            if (isLocationDetectionInProgress && !hasLocationTimeoutOccurred) {
                Log.d(TAG, "Location detection UI timeout, hiding spinner");
                hasLocationTimeoutOccurred = true;
                hideLoading();
            }
        }, LOCATION_TIMEOUT_MS);

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
            .addOnSuccessListener(location -> {
                isLocationDetectionInProgress = false;
                if (location != null) {
                    Log.d(TAG, "Location detected after " + (System.currentTimeMillis() - locationDetectionStartTime) + "ms");
                    reverseGeocode(location.getLatitude(), location.getLongitude());
                } else {
                    Log.e(TAG, "Location is null even after success");
                    hideLoading();
                    Toast.makeText(requireContext(), "Could not detect location.", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                isLocationDetectionInProgress = false;
                hideLoading();
                Log.e(TAG, "Location detection failed: " + e.getMessage());
            });
    }

    private void reverseGeocode(double latitude, double longitude) {
        if (!isAdded() || getActivity() == null) return;
        String url = String.format(Locale.US, "%s&lat=%f&lon=%f", NominatimHostManager.getRandomReverseUrl(), latitude, longitude);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String displayName = response.getString("display_name");
                        Log.d(TAG, "reverseGeocode: Detected location is " + displayName);
                        detectedLocationList.clear();
                        detectedLocationList.add(displayName);
                        detectedLocationAdapter.notifyDataSetChanged();
                        
                        synchronized(usDetectedLocationList) {
                            usDetectedLocationList.clear();
                            usDetectedLocationList.add(displayName);
                        }
                        
                        startWeatherDataRetry();
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing reverse geocoding response: " + e.getMessage());
                        showNominatimErrorDialog("Error parsing location data.");
                    }
                }, error -> {
            Log.e(TAG, "Reverse geocoding error, trying fallback");
            reverseGeocodeWithFallback(latitude, longitude);
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                return headers;
            }
        };
        jsonObjectRequest.setShouldCache(false);
        requestQueue.add(jsonObjectRequest);
    }

    private void reverseGeocodeWithFallback(double latitude, double longitude) {
        if (!isAdded() || getActivity() == null) return;
        String url = String.format(Locale.US, "%s&lat=%f&lon=%f", NominatimHostManager.getFallbackReverseUrl(), latitude, longitude);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String displayName = response.getString("display_name");
                        detectedLocationList.clear();
                        detectedLocationList.add(displayName);
                        detectedLocationAdapter.notifyDataSetChanged();
                        
                        synchronized(usDetectedLocationList) {
                            usDetectedLocationList.clear();
                            usDetectedLocationList.add(displayName);
                        }
                        
                        startWeatherDataRetry();
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing reverse geocoding response from fallback: " + e.getMessage());
                        showNominatimErrorDialog("Error parsing location data.");
                    }
                }, error -> {
            hideLoading();
            showNominatimErrorDialog("Could not connect to location service from any available host.");
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                return headers;
            }
        };
        jsonObjectRequest.setShouldCache(false);
        requestQueue.add(jsonObjectRequest);
    }

    private void showNominatimErrorDialog(String message) {
        if (isAdded()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Geocoding failed")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    private void showUsOnlyDialog(String attemptedAddress, @Nullable String countryCode) {
        if (!isAdded() || getActivity() == null) return;
        String message;
        if (countryCode != null && !"us".equalsIgnoreCase(countryCode)) {
            message = "Sorry, '" + attemptedAddress + "' (located in " + countryCode.toUpperCase() + ") is not supported. This app only provides weather for the US.";
        } else {
            message = "Location '" + attemptedAddress + "' not supported.";
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Location Not Supported")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    public interface OnNavigateToForecastListener {
        void onNavigateToForecast(String location);
    }

    private OnNavigateToForecastListener navigateToForecastListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNavigateToForecastListener) {
            navigateToForecastListener = (OnNavigateToForecastListener) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement OnNavigateToForecastListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigateToForecastListener = null;
    }

    private void setLocationAndNavigateToForecast(String location) {
        if (!isAdded() || getActivity() == null) return;
        cancelWeatherFetching();
        showLoading();
        
        List<String> singleLocationList = new ArrayList<>();
        singleLocationList.add(location);
        weatherViewModel.fetchWeatherForLocations(requireContext(), singleLocationList);
        
        reloadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Map<String, WeatherViewModel.WeatherSummary> currentMap = weatherViewModel.getLocationWeatherMap().getValue();
                WeatherViewModel.WeatherSummary summary = currentMap != null ? currentMap.get(location) : null;
                boolean hasWeather = summary != null && summary.temperature != null && !summary.temperature.equals("--");
                
                if (hasWeather) {
                    hideLoading();
                    if (navigateToForecastListener != null) {
                        navigateToForecastListener.onNavigateToForecast(location);
                    }
                } else {
                    reloadHandler.postDelayed(this, RELOAD_DELAY_MS);
                }
            }
        }, RELOAD_DELAY_MS);
    }

    @Override
    public void onLocationClick(String location) {
        writeToPreference(location);
        checkCountryAndProceed(location);
    }

    private void showLoading() {
        if (progressBar != null && progressBar.getVisibility() != View.VISIBLE) {
            progressBar.setAlpha(1f);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (progressBar != null && progressBar.getVisibility() == View.VISIBLE) {
            progressBar.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> progressBar.setVisibility(View.GONE))
                .start();
        }
    }

    private void startWeatherDataRetry() {
        weatherReloadAttempts = 0;
        reloadHandler.removeCallbacks(fetchWeatherRunnable);
        attemptWeatherDataFetch();
    }

    private void attemptWeatherDataFetch() {
        if (!isAdded() || getActivity() == null) return;
        
        List<String> allLocations = new ArrayList<>();
        synchronized(usSavedLocationsList) { allLocations.addAll(usSavedLocationsList); }
        synchronized(usDetectedLocationList) { allLocations.addAll(usDetectedLocationList); }

        if (allLocations.isEmpty()) {
            if (!isLocationDetectionInProgress) {
                hideLoading();
            } else {
                weatherReloadAttempts++;
                reloadHandler.postDelayed(fetchWeatherRunnable, RELOAD_DELAY_MS);
            }
            return;
        }

        Map<String, WeatherViewModel.WeatherSummary> currentMap = weatherViewModel.getLocationWeatherMap().getValue();
        List<String> needingWeather = new ArrayList<>();
        int locationsWithData = 0;

        for (String loc : allLocations) {
            WeatherViewModel.WeatherSummary summary = currentMap != null ? currentMap.get(loc) : null;
            boolean hasData = summary != null && summary.temperature != null && 
                            !summary.temperature.isEmpty() && !summary.temperature.equals("--");
            
            if (hasData) {
                locationsWithData++;
            } else {
                needingWeather.add(loc);
            }
        }

        if (!needingWeather.isEmpty()) {
            weatherViewModel.fetchWeatherForLocations(requireContext(), needingWeather);
        }

        updateAdaptersWithWeatherData();

        if (locationsWithData == allLocations.size()) {
            hideLoading();
        } else if (weatherReloadAttempts < MAX_WEATHER_RELOADS) {
            weatherReloadAttempts++;
            reloadHandler.postDelayed(fetchWeatherRunnable, RELOAD_DELAY_MS);
        } else {
            hideLoading();
        }
    }

    private void updateAdaptersWithWeatherData() {
        if (!isAdded() || getActivity() == null) return;
        Map<String, WeatherViewModel.WeatherSummary> currentMap = weatherViewModel.getLocationWeatherMap().getValue();
        if (currentMap != null) {
            Map<String, WeatherViewModel.WeatherSummary> savedMap = new HashMap<>();
            synchronized(usSavedLocationsList) {
                for (String loc : usSavedLocationsList) {
                    WeatherViewModel.WeatherSummary summary = currentMap.get(loc);
                    if (summary != null) savedMap.put(loc, summary);
                }
            }
            savedLocationAdapter.setWeatherSummaries(savedMap);
            savedLocationAdapter.notifyDataSetChanged();
            
            Map<String, WeatherViewModel.WeatherSummary> detectedMap = new HashMap<>();
            synchronized(usDetectedLocationList) {
                for (String loc : usDetectedLocationList) {
                    WeatherViewModel.WeatherSummary summary = currentMap.get(loc);
                    if (summary != null) detectedMap.put(loc, summary);
                }
            }
            detectedLocationAdapter.setWeatherSummaries(detectedMap);
            detectedLocationAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelWeatherFetching();
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
        }
    }

    private void cancelWeatherFetching() {
        reloadHandler.removeCallbacksAndMessages(null);
        weatherReloadAttempts = MAX_WEATHER_RELOADS;
        isLocationDetectionInProgress = false;
        hasLocationTimeoutOccurred = false;
        if (requestQueue != null) requestQueue.cancelAll(TAG);
    }

    private void checkCountryAndProceed(String address) {
        if (!isAdded() || getActivity() == null || address == null || address.trim().isEmpty()) return;
        
        showLoading();
        if (address.matches("\\d{5}")) {
            GeocodingRetryManager.geocodeWithRetry(requireContext(), address, USER_AGENT, "us",
                usResult -> {
                    if (!isAdded()) return;
                    String usCountryCode = usResult.getCountryCode();
                    String usDisplayName = usResult.getDisplayName();
                    new AlertDialog.Builder(requireContext())
                        .setTitle("Zip Code Search")
                        .setMessage("Location found: '" + usDisplayName + "'.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            if ("us".equalsIgnoreCase(usCountryCode)) {
                                writeToPreference(address);
                                setLocationAndNavigateToForecast(address);
                            }
                            hideLoading();
                        })
                        .setCancelable(false)
                        .show();
                },
                errorMessage -> {
                    hideLoading();
                    showNominatimErrorDialog("Could not verify zipcode: " + errorMessage);
                }
            );
            return;
        }

        GeocodingRetryManager.geocodeWithRetry(requireContext(), address, USER_AGENT, "us",
            usResult -> {
                if (!isAdded()) return;
                String usDisplayName = usResult.getDisplayName();
                GeocodingRetryManager.geocodeWithRetry(requireContext(), address, USER_AGENT, null,
                    globalResult -> {
                        if (!isAdded()) return;
                        showDualLocationDialog(usDisplayName, globalResult.getDisplayName(), globalResult.getCountryCode());
                        hideLoading();
                    },
                    errorMessage -> {
                        if (!isAdded()) return;
                        showDualLocationDialog(usDisplayName, null, null);
                        hideLoading();
                    }
                );
            },
            errorMessage -> {
                hideLoading();
                showNominatimErrorDialog("Geocoding failed: " + errorMessage);
            }
        );
    }

    private void showDualLocationDialog(String usDisplayName, String globalDisplayName, String globalCountryCode) {
        if (!isAdded() || getActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Confirm Location");
        String message = "We found this location:\n\n" + usDisplayName;
        if (globalDisplayName != null && !globalDisplayName.equals(usDisplayName)) {
            message += "\n\n(Note: Also identified as '" + globalDisplayName + "' in " + (globalCountryCode != null ? globalCountryCode.toUpperCase() : "Unknown") + ")";
        }
        builder.setMessage(message);
        builder.setPositiveButton("Use This Location", (dialog, which) -> {
            writeToPreference(usDisplayName);
            setLocationAndNavigateToForecast(usDisplayName);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
