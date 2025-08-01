package com.venomdevelopment.sunwise;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
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

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HomeFragment extends Fragment implements SavedLocationAdapter.OnLocationClickListener {

    private static final String TAG = "HomeFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String BASE_URL_POINTS = "https://api.weather.gov/points/";
    private static final String USER_AGENT = "Sunwise/v0-prerelease" + System.getProperty("http.agent");
    private static final String PREF_SAVED_LOCATIONS = "saved_locations";

    private EditText search;
    private Button searchButton;
    private Button locationButton;
    private RecyclerView savedLocationsRecyclerView;
    private SavedLocationAdapter savedLocationAdapter;
    private List<String> savedLocationsList = new ArrayList<>();
    private List<String> originalSavedLocationsList = new ArrayList<>(); // Keep a copy of the original list
    private EditText savedLocationsSearch;
    private LottieAnimationView animationViewHome;
    public static final String myPref = "addressPref";
    private LocationManager locationManager;
    private RequestQueue requestQueue;
    private String currentDetectedLocation = "";
    private LinearLayout progressBar;
    private int pendingWeatherRequests = 0;
    private boolean isLoadingSavedLocations = false;
    private boolean autoLocationEnabled = true;
    private SharedPreferences sunwisePrefs;
    private boolean isAutoDetectTriggered = false;
    private int geocodeReloadAttempts = 0;
    private static final int MAX_GEOCODE_RELOADS = 8;
    private Set<String> geocodedLocations = new HashSet<>();
    private boolean lastGeocodeWasAutoDetect = false;
    private RecyclerView detectedLocationRecyclerView;
    private SavedLocationAdapter detectedLocationAdapter;
    private List<String> detectedLocationList = new ArrayList<>();
    private WeatherViewModel weatherViewModel;
    private Handler reloadHandler = new Handler(Looper.getMainLooper());
    private int weatherReloadAttempts = 0;
    private static final int MAX_WEATHER_RELOADS = 8;
    private static final int RELOAD_DELAY_MS = 500; // 2 seconds between retries
    private final Set<String> processedGeocodeAddresses = new HashSet<>();
    
    // Location timeout tracking
    private static final int LOCATION_TIMEOUT_MS = 5000; // 5 seconds
    private boolean isLocationDetectionInProgress = false;
    private long locationDetectionStartTime = 0;
    private boolean hasLocationTimeoutOccurred = false;

    public String getPreferenceValue() {
        SharedPreferences sp = requireActivity().getSharedPreferences(myPref, 0);
        String str = sp.getString("address", "");
        return str;
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

    private void saveSavedLocations(Set<String> locations) {
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(myPref, 0).edit();
        editor.putStringSet(PREF_SAVED_LOCATIONS, locations);
        editor.apply();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        if (!checkLocationPermission()) {
            requestLocationPermission();
        }
        search = v.findViewById(R.id.text_search);
        searchButton = v.findViewById(R.id.search);
        locationButton = v.findViewById(R.id.locationButton);
        detectedLocationRecyclerView = v.findViewById(R.id.detectedLocationRecyclerView);
        savedLocationsRecyclerView = v.findViewById(R.id.savedLocationsRecyclerView);
        savedLocationsSearch = v.findViewById(R.id.savedLocationsSearch);
        animationViewHome = v.findViewById(R.id.animation_view);
        progressBar = v.findViewById(R.id.progressBar);
        requestQueue = SunwiseApp.getInstance().getRequestQueue();

        savedLocationsRecyclerView.setNestedScrollingEnabled(false);
        savedLocationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        savedLocationAdapter = new SavedLocationAdapter(savedLocationsList, this);
        savedLocationsRecyclerView.setAdapter(savedLocationAdapter);
        
        // Initialize weatherViewModel before loadSavedLocations() to prevent null pointer exception
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        
        loadSavedLocations();

        detectedLocationRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        detectedLocationAdapter = new SavedLocationAdapter(detectedLocationList, this);
        detectedLocationRecyclerView.setAdapter(detectedLocationAdapter);

        // Set up the text watcher for the search input
        savedLocationsSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSavedLocations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        search.setOnEditorActionListener((v1, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                String address = search.getText().toString().trim();
                if (!address.isEmpty()) {
                    // Instead of direct navigation, check country first
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
                // Instead of direct navigation, check country first
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

        // Hide the Detect Location button if auto-detect is enabled
        if (autoLocationEnabled) {
            locationButton.setVisibility(View.GONE);
        } else {
            locationButton.setVisibility(View.VISIBLE);
        }

        if (autoLocationEnabled) {
            isAutoDetectTriggered = true;
            if (checkLocationPermission()) {
                getCurrentLocation();
            }
        }

        geocodeReloadAttempts = 0;

        weatherViewModel.getLocationWeatherMap().observe(getViewLifecycleOwner(), locationWeatherMap -> {
            // This observer is now only used for updates after initial load
            // The retry logic handles the initial loading
            if (progressBar.getVisibility() != View.VISIBLE) {
                // Only update if loading is not showing (initial load is complete)
                updateAdaptersWithWeatherData();
            }
        });

        weatherViewModel.getCurrentTemperature().observe(getViewLifecycleOwner(), temp -> {
            // Update the UI element for current temperature, e.g.:
            // currentTempTextView.setText(temp);
        });
        weatherViewModel.getDescription().observe(getViewLifecycleOwner(), desc -> {
            // Update the UI element for description
        });
        weatherViewModel.getHumidity().observe(getViewLifecycleOwner(), humidity -> {
            // Update the UI element for humidity
        });
        weatherViewModel.getWind().observe(getViewLifecycleOwner(), wind -> {
            // Update the UI element for wind
        });
        weatherViewModel.getPrecipitation().observe(getViewLifecycleOwner(), precip -> {
            // Update the UI element for precipitation
        });

        // Show loading initially
        showLoading();
        
        // Start the retry logic for weather data
        // Even if no saved locations, we might have detected location coming
        startWeatherDataRetry();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if auto-detect setting has changed and update button visibility
        boolean currentAutoLocationEnabled = sunwisePrefs.getBoolean("auto_location_enabled", true);
        if (currentAutoLocationEnabled != autoLocationEnabled) {
            autoLocationEnabled = currentAutoLocationEnabled;
            if (locationButton != null) {
                if (autoLocationEnabled) {
                    locationButton.setVisibility(View.GONE);
                } else {
                    locationButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void loadSavedLocations() {
        Set<String> savedSet = getSavedLocations();
        originalSavedLocationsList.clear();
        originalSavedLocationsList.addAll(savedSet);
        savedLocationsList.clear();
        savedLocationsList.addAll(originalSavedLocationsList); // Initialize displayed list
        if (savedLocationAdapter != null) {
            savedLocationAdapter.notifyDataSetChanged();
        }
        
        // Only trigger weather data retry if we have saved locations and no weather retry is already in progress
        if (!savedLocationsList.isEmpty() && weatherReloadAttempts == 0) {
            weatherReloadAttempts = 0;
            startWeatherDataRetry();
        }
    }

    private void filterSavedLocations(String query) {
        savedLocationsList.clear();
        if (query.isEmpty()) {
            savedLocationsList.addAll(originalSavedLocationsList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (String location : originalSavedLocationsList) {
                if (location.toLowerCase().contains(lowerCaseQuery)) {
                    savedLocationsList.add(location);
                }
            }
        }
        if (savedLocationAdapter != null) {
            savedLocationAdapter.notifyDataSetChanged();
        }
    }

    private void updateSavedLocations(String location) {
        if (!isAdded() || getActivity() == null) return;
        Set<String> savedSet = getSavedLocations();
        if (savedSet.contains(location)) {
            savedSet.remove(location); // Move to top
        }
        savedSet.add(location);
        saveSavedLocations(savedSet);
        loadSavedLocations(); // Reload to update the order and reset search
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            // Start location detection timeout tracking
            isLocationDetectionInProgress = true;
            locationDetectionStartTime = System.currentTimeMillis();
            hasLocationTimeoutOccurred = false;
            
            // Set up location timeout
            reloadHandler.postDelayed(() -> {
                if (isLocationDetectionInProgress && !hasLocationTimeoutOccurred) {
                    Log.d(TAG, "Location detection timeout after " + LOCATION_TIMEOUT_MS + "ms, hiding spinner but continuing location detection");
                    hasLocationTimeoutOccurred = true;
                    hideLoading();
                }
            }, LOCATION_TIMEOUT_MS);
            
            detectedLocationAdapter.notifyDataSetChanged();
            locationManager.requestSingleUpdate(LocationManager.FUSED_PROVIDER, locationListener, null);
        } else {
            detectedLocationAdapter.notifyDataSetChanged();
        }
    }

    private final android.location.LocationListener locationListener = new android.location.LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            // Stop location detection tracking
            isLocationDetectionInProgress = false;
            
            Log.d(TAG, "Location detected after " + (System.currentTimeMillis() - locationDetectionStartTime) + "ms");
            
            reverseGeocode(location.getLatitude(), location.getLongitude());
            locationManager.removeUpdates(this);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {}

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            // Stop location detection tracking
            isLocationDetectionInProgress = false;
            detectedLocationAdapter.notifyDataSetChanged();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    private void reverseGeocode(double latitude, double longitude) {
        if (!isAdded() || getActivity() == null) return;
        String url = String.format(Locale.US, "%s&lat=%f&lon=%f", NominatimHostManager.getRandomReverseUrl(), latitude, longitude);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String displayName = response.getString("display_name");
                        Log.d("HomeFragment", "reverseGeocode: Detected location is " + displayName);
                        detectedLocationList.clear();
                        detectedLocationList.add(displayName);
                        detectedLocationAdapter.notifyDataSetChanged();
                        currentDetectedLocation = displayName;
                        
                        // Only trigger weather data retry if no weather retry is already in progress
                        if (weatherReloadAttempts == 0) {
                            weatherReloadAttempts = 0;
                            startWeatherDataRetry();
                        }
                        
                        // Don't automatically navigate - let user choose
                        isAutoDetectTriggered = false;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing reverse geocoding response: " + e.getMessage());
                        detectedLocationAdapter.notifyDataSetChanged();
                        showNominatimErrorDialog("Error parsing location data.");
                    }
                }, error -> {
            String errorMessage = "Unknown error";
            if (error != null) {
                if (error.getMessage() != null) {
                    errorMessage = error.getMessage();
                } else if (error.networkResponse != null) {
                    errorMessage = "HTTP " + error.networkResponse.statusCode;
                } else {
                    errorMessage = "Network error";
                }
            }
            Log.e(TAG, "Reverse geocoding error from primary host: " + errorMessage);
            // Try fallback host
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
                        Log.d("HomeFragment", "reverseGeocodeWithFallback: Detected location is " + displayName);
                        detectedLocationList.clear();
                        detectedLocationList.add(displayName);
                        detectedLocationAdapter.notifyDataSetChanged();
                        currentDetectedLocation = displayName;
                        
                        // Only trigger weather data retry if no weather retry is already in progress
                        if (weatherReloadAttempts == 0) {
                            weatherReloadAttempts = 0;
                            startWeatherDataRetry();
                        }
                        
                        // Don't automatically navigate - let user choose
                        isAutoDetectTriggered = false;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing reverse geocoding response from fallback: " + e.getMessage());
                        detectedLocationAdapter.notifyDataSetChanged();
                        showNominatimErrorDialog("Error parsing location data.");
                    }
                }, error -> {
            String errorMessage = "Unknown error";
            if (error != null) {
                if (error.getMessage() != null) {
                    errorMessage = error.getMessage();
                } else if (error.networkResponse != null) {
                    errorMessage = "HTTP " + error.networkResponse.statusCode;
                } else {
                    errorMessage = "Network error";
                }
            }
            Log.e(TAG, "Reverse geocoding error from fallback host: " + errorMessage);
            detectedLocationAdapter.notifyDataSetChanged();
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
        if (isAdded()) { // Check if the fragment is still attached
            new AlertDialog.Builder(requireContext())
                    .setTitle("Oh No! Looks like Nominatim is down.")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            Log.w(TAG, "Error dialog not shown: Fragment is not attached.");
        }
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
            throw new ClassCastException(context.toString()
                    + " must implement OnNavigateToForecastListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigateToForecastListener = null;
    }

    private void setLocationAndNavigateToForecast(String location) {
        if (!isAdded() || getActivity() == null) return;
        
        // Cancel all weather fetching immediately when navigating to ForecastFragment
        cancelWeatherFetching();
        
        if (!isAutoDetectTriggered) {
            // Only navigate if not triggered by auto-detect
            if (navigateToForecastListener != null) {
                navigateToForecastListener.onNavigateToForecast(location);
            }
        }
        isAutoDetectTriggered = false;
    }

    @Override
    public void onLocationClick(String location) {
        // Save the selected location to SharedPreferences
        writeToPreference(location);
        
        // Cancel all weather fetching immediately when navigating to ForecastFragment
        cancelWeatherFetching();
        
        setLocationAndNavigateToForecast(location);
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

    private void checkAndHideLoadingUI() {
        // This method is now only used for geocoding completion
        // Weather data loading is handled by the retry logic
        boolean detectedReady = false;
        Log.d("HomeFragment", String.valueOf(detectedLocationList));
        if (!detectedLocationList.isEmpty() && detectedLocationList.get(0) != null) {
            String locationText = detectedLocationList.get(0);
            detectedReady = !locationText.equals("N/A") && !locationText.equals("--") && !locationText.isEmpty();
            Log.d("HomeFragment", "Detected location ready: " + detectedReady);
        }

        // Only hide loading if we have detected location and no weather retry is in progress
        if (detectedReady && weatherReloadAttempts >= MAX_WEATHER_RELOADS) {
            Log.d("HomeFragment", "Geocoding complete and weather retry finished, checking if we can hide spinner");
            // The weather retry logic will handle hiding the spinner when data is available
        }
    }

    private void startWeatherDataRetry() {
        weatherReloadAttempts = 0;
        Log.d(TAG, "Starting weather data retry logic");
        attemptWeatherDataFetch();
    }

    private void attemptWeatherDataFetch() {
        if (!isAdded() || getActivity() == null) return;
        
        // Stop if weather fetching has been cancelled
        if (weatherReloadAttempts >= MAX_WEATHER_RELOADS) {
            Log.d(TAG, "Weather fetching cancelled, stopping attempts");
            return;
        }
        
        Log.d(TAG, "Weather data fetch attempt " + (weatherReloadAttempts + 1) + " of " + MAX_WEATHER_RELOADS);
        
        // Get current weather data to check what we already have
        Map<String, WeatherViewModel.WeatherSummary> currentMap = weatherViewModel.getLocationWeatherMap().getValue();
        
        // Fetch weather only for locations that don't already have weather data
        List<String> allLocations = new ArrayList<>();
        allLocations.addAll(savedLocationsList);
        allLocations.addAll(detectedLocationList);
        
        List<String> locationsNeedingWeather = new ArrayList<>();
        
        for (String location : allLocations) {
            if (currentMap == null || currentMap.get(location) == null || 
                currentMap.get(location).temperature == null || 
                currentMap.get(location).temperature.isEmpty() || 
                currentMap.get(location).temperature.equals("--")) {
                locationsNeedingWeather.add(location);
            }
        }
        
        Log.d(TAG, "Current locations - Saved: " + savedLocationsList.size() + ", Detected: " + detectedLocationList.size());
        Log.d(TAG, "Locations needing weather: " + locationsNeedingWeather.size() + " out of " + allLocations.size());
        
        if (allLocations.isEmpty()) {
            // No locations to fetch, but don't hide loading yet
            // Wait for either detected location or saved locations to be added
            Log.d(TAG, "No locations to fetch yet, waiting for locations to be added");
            weatherReloadAttempts++;
            if (weatherReloadAttempts < MAX_WEATHER_RELOADS) {
                Log.d(TAG, "Scheduling next retry in " + RELOAD_DELAY_MS + "ms");
                reloadHandler.postDelayed(this::attemptWeatherDataFetch, RELOAD_DELAY_MS);
            } else {
                // Only hide loading if location detection has timed out or completed
                if (hasLocationTimeoutOccurred || !isLocationDetectionInProgress) {
                    Log.d(TAG, "Max weather reload attempts reached with no locations, hiding loading spinner");
                    hideLoading();
                } else {
                    Log.d(TAG, "Max weather reload attempts reached but location detection still in progress, keeping spinner");
                }
            }
            return;
        }
        
        // Only fetch weather for locations that need it
        if (!locationsNeedingWeather.isEmpty()) {
            Log.d(TAG, "Fetching weather for locations that need it: " + locationsNeedingWeather);
            weatherViewModel.fetchWeatherForLocations(requireContext(), locationsNeedingWeather);
        } else {
            Log.d(TAG, "All locations already have weather data, no need to fetch");
        }
        
        // Check if we have data after a longer delay to give API time to respond
        reloadHandler.postDelayed(() -> {
            if (!isAdded() || getActivity() == null) return;
            
            // Stop if weather fetching has been cancelled
            if (weatherReloadAttempts >= MAX_WEATHER_RELOADS) {
                Log.d(TAG, "Weather fetching cancelled in delayed callback, stopping");
                return;
            }
            
            Map<String, WeatherViewModel.WeatherSummary> updatedMap = weatherViewModel.getLocationWeatherMap().getValue();
            int locationsWithData = 0;
            
            if (updatedMap != null) {
                Log.d(TAG, "Current weather map has " + updatedMap.size() + " entries");
                for (String location : allLocations) {
                    WeatherViewModel.WeatherSummary summary = updatedMap.get(location);
                    if (summary != null && summary.temperature != null && !summary.temperature.isEmpty() && !summary.temperature.equals("--")) {
                        locationsWithData++;
                        Log.d(TAG, "Location " + location + " has temperature: " + summary.temperature);
                    } else {
                        Log.d(TAG, "Location " + location + " has no valid temperature data");
                    }
                }
            } else {
                Log.d(TAG, "Weather map is null");
            }
            
            Log.d(TAG, "Weather check: " + locationsWithData + "/" + allLocations.size() + " locations have data");
            
            if (locationsWithData == allLocations.size() && allLocations.size() > 0) {
                Log.d(TAG, "All weather data received, hiding loading spinner");
                hideLoading();
                updateAdaptersWithWeatherData();
            } else {
                weatherReloadAttempts++;
                if (weatherReloadAttempts < MAX_WEATHER_RELOADS) {
                    Log.d(TAG, "Not all weather data yet, retrying in " + RELOAD_DELAY_MS + "ms (attempt " + weatherReloadAttempts + ")");
                    reloadHandler.postDelayed(this::attemptWeatherDataFetch, RELOAD_DELAY_MS);
                } else {
                    // Only hide loading if location detection has timed out or completed
                    if (hasLocationTimeoutOccurred || !isLocationDetectionInProgress) {
                        Log.d(TAG, "Max weather reload attempts reached, hiding loading spinner");
                        hideLoading();
                        updateAdaptersWithWeatherData();
                    } else {
                        Log.d(TAG, "Max weather reload attempts reached but location detection still in progress, keeping spinner");
                        updateAdaptersWithWeatherData();
                    }
                }
            }
        }, 3000); // Check after 3 seconds to give API more time to respond
    }

    private void updateAdaptersWithWeatherData() {
        if (!isAdded() || getActivity() == null) return;
        
        Map<String, WeatherViewModel.WeatherSummary> currentMap = weatherViewModel.getLocationWeatherMap().getValue();
        if (currentMap != null) {
            savedLocationAdapter.setWeatherSummaries(currentMap);
            savedLocationAdapter.notifyDataSetChanged();
            detectedLocationAdapter.setWeatherSummaries(currentMap);
            detectedLocationAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel any pending reload attempts and weather fetching
        cancelWeatherFetching();
    }

    private void cancelWeatherFetching() {
        // Cancel all pending reload attempts
        reloadHandler.removeCallbacksAndMessages(null);
        
        // Reset retry counters to stop further attempts
        weatherReloadAttempts = MAX_WEATHER_RELOADS;
        geocodeReloadAttempts = MAX_GEOCODE_RELOADS;
        
        // Reset location detection tracking
        isLocationDetectionInProgress = false;
        hasLocationTimeoutOccurred = false;
        
        // Cancel any pending Volley requests for this fragment
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
        
        Log.d(TAG, "Weather fetching cancelled - navigating to ForecastFragment");
    }
    private void checkCountryAndProceed(String address) {
        if (!isAdded() || getActivity() == null || address == null || address.trim().isEmpty()) {
            if (address != null && !address.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a valid address", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        showLoading(); // Show loading indicator

        Log.d(TAG, "Checking country for address (with retry): " + address);

        // This is the core linkage: Calling GeocodingRetryManager
        GeocodingRetryManager.geocodeWithRetry(
                requireContext(), // Context
                address,          // Address to geocode
                USER_AGENT,       // User-Agent
                new GeocodingRetryManager.GeocodingSuccessCallback() {
                    @Override
                    public void onSuccess(GeocodingResponseParser.GeocodingResult result) {
                        if (!isAdded() || getActivity() == null) return; // Fragment not attached or activity gone
                        hideLoading();

                        String countryCode = result.getCountryCode();
                        String displayName = result.getDisplayName();

                        Log.d(TAG, "Geocoding success for country check. Address: '" + address + "', DisplayName: '" + displayName + "', Country Code: '" + countryCode + "'");

                        if ("us".equalsIgnoreCase(countryCode)) {
                            // It's a US location, proceed with saving and navigation
                            writeToPreference(address); // Use the original searched address for preference
                            setLocationAndNavigateToForecast(address); // Navigate with original address
                        } else {
                            // Not a US location (or countryCode is null if parsing failed but was "successful" retry)
                            showUsOnlyDialog(displayName != null && !displayName.isEmpty() ? displayName : address, countryCode);
                        }
                    }
                },
                new GeocodingRetryManager.GeocodingFailureCallback() {
                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded() || getActivity() == null) return; // Fragment not attached or activity gone
                        hideLoading();
                        Log.e(TAG, "Geocoding failed after retries for country check (address: '" + address + "'): " + errorMessage);
                        // Show a generic error or the one from the retry manager
                        showNominatimErrorDialog("Could not verify location. Please try again. Error: " + errorMessage);
                    }
                }
        );
    }

    // The showUsOnlyDialog method (ensure it's present in your HomeFragment)
    private void showUsOnlyDialog(String attemptedAddress, @Nullable String countryCode) {
        if (!isAdded() || getActivity() == null) return;

        String message;
        if (countryCode != null && !countryCode.isEmpty() && !"us".equalsIgnoreCase(countryCode)) {
            message = "Sorry, \"" + attemptedAddress + "\" (located in " + countryCode.toUpperCase() + ") is not supported. This app currently only provides weather information for locations within the United States.";
        } else if (attemptedAddress != null && !attemptedAddress.isEmpty()){
            // This covers cases where countryCode might be null (e.g. geocoding failed to determine country)
            // or if countryCode was 'us' but we still want to show a generic message for some edge cases.
            // However, the primary check is the 'if' above. If it's 'us', we wouldn't reach showUsOnlyDialog
            // unless called from onFailure or if the GeocodingResult had 'us' but was still problematic.
            // For clarity, this condition is more for "could not determine it's US"
            message = "Sorry, the location \"" + attemptedAddress + "\" could not be identified as being within the United States, or the location is not supported. This app currently only supports US locations.";
        } else {
            message = "Sorry, the location could not be identified as being within the United States. This app currently only supports US locations.";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Location Not Supported")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

}