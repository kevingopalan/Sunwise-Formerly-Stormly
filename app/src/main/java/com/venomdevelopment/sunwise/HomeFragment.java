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
    // Store filtered US locations for weather and display
    private final List<String> usSavedLocationsList = new ArrayList<>();
    private final List<String> usDetectedLocationList = new ArrayList<>();
    private RecyclerView suggestionsRecyclerView;
    private LocationSuggestionAdapter suggestionAdapter;
    private final List<String> suggestionList = new ArrayList<>();

    private static final String TAG = "HomeFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String BASE_URL_POINTS = "https://api.weather.gov/points/";
    private static final String USER_AGENT = "Sunwise/v1 (venomdevelopmentofficial@gmail.com)" + System.getProperty("http.agent");
    private static final String PREF_SAVED_LOCATIONS = "saved_locations";

    private EditText search;
    private Button searchButton;
    private Button locationButton;
    private RecyclerView savedLocationsRecyclerView;
    private SavedLocationAdapter savedLocationAdapter;
    private final List<String> savedLocationsList = new ArrayList<>();
    private final List<String> originalSavedLocationsList = new ArrayList<>(); // Keep a copy of the original list
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
        suggestionsRecyclerView = v.findViewById(R.id.suggestionsRecyclerView);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        suggestionAdapter = new LocationSuggestionAdapter(suggestionList, suggestion -> {
            search.setText(suggestion);
            suggestionsRecyclerView.setVisibility(View.GONE);
            checkCountryAndProceed(suggestion);
        });
        suggestionsRecyclerView.setAdapter(suggestionAdapter);
        suggestionsRecyclerView.setVisibility(View.GONE);
        // Location suggestion dropdown for main search bar
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

        // Hide suggestions dropdown when user clicks outside
        search.setOnFocusChangeListener((v1, hasFocus) -> {
            if (!hasFocus) {
                suggestionsRecyclerView.setVisibility(View.GONE);
            } else {
                // Only show if there are suggestions and input is focused
                if (!suggestionList.isEmpty()) {
                    suggestionsRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
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

    detectedLocationRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    detectedLocationAdapter = new SavedLocationAdapter(detectedLocationList, this);
    detectedLocationRecyclerView.setAdapter(detectedLocationAdapter);

    // Initialize weatherViewModel before loadSavedLocations() to prevent null pointer exception
    weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
    loadSavedLocations();

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
        if (!originalSavedLocationsList.equals(new ArrayList<>(savedSet))) {
            originalSavedLocationsList.clear();
            originalSavedLocationsList.addAll(savedSet);
            savedLocationsList.clear();
            usSavedLocationsList.clear();
            final int[] completed = {0};
            if (savedSet.isEmpty()) {
                if (savedLocationAdapter != null) savedLocationAdapter.notifyDataSetChanged();
                // Do NOT hideLoading() here; let weather logic control spinner after all loading is complete
                return;
            }
            for (String location : savedSet) {
                GeocodingRetryManager.geocodeWithRetry(requireContext(), location, USER_AGENT, "us",
                    result -> {
                        if (result != null) {
                            Log.d(TAG, "Geocoding result for saved location '" + location + "': countryCode=" + result.getCountryCode() + ", displayName=" + result.getDisplayName());
                            if ("us".equalsIgnoreCase(result.getCountryCode())) {
                                Log.d(TAG, "adding location to the US saved locations list: " + location);
                                usSavedLocationsList.add(location);
                            } else {
                                // Show dialog for non-US saved location and do NOT add to list
                                showUsOnlyDialog(result.getDisplayName(), result.getCountryCode());
                            }
                        } else {
                            Log.d(TAG, "Geocoding result for saved location '" + location + "' is null");
                        }
                        completed[0]++;
                        if (completed[0] == savedSet.size()) {
                            savedLocationsList.addAll(usSavedLocationsList);
                            if (savedLocationAdapter != null) savedLocationAdapter.notifyDataSetChanged();
                            if (!usSavedLocationsList.isEmpty() && weatherReloadAttempts == 0) {
                                weatherReloadAttempts = 0;
                                startWeatherDataRetry();
                            }
                        }
                    },
                    errorMessage -> {
                        Log.d(TAG, "Geocoding error for saved location '" + location + "': " + errorMessage);
                        completed[0]++;
                        if (completed[0] == savedSet.size()) {
                            savedLocationsList.addAll(usSavedLocationsList);
                            if (savedLocationAdapter != null) savedLocationAdapter.notifyDataSetChanged();
                            if (!usSavedLocationsList.isEmpty() && weatherReloadAttempts == 0) {
                                weatherReloadAttempts = 0;
                                startWeatherDataRetry();
                            }
                        }
                    }
                );
            }
        }
        // Do NOT hideLoading() here; let weather logic control spinner after all loading is complete
    }

    private void filterSavedLocations(String query) {
        if (query.isEmpty()) {
            if (!savedLocationsList.equals(originalSavedLocationsList)) {
                savedLocationsList.clear();
                savedLocationsList.addAll(originalSavedLocationsList);
                if (savedLocationAdapter != null) savedLocationAdapter.notifyDataSetChanged();
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
                if (savedLocationAdapter != null) savedLocationAdapter.notifyDataSetChanged();
            }
        }
    }

    // Fetch location suggestions from geocoding API
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

    private void updateSavedLocations(String location) {
        if (!isAdded() || getActivity() == null) return;
        Set<String> savedSet = getSavedLocations();
        if (savedSet.remove(location)) {
            savedSet.add(location); // Move to top
            saveSavedLocations(savedSet);
            loadSavedLocations(); // Reload to update the order and reset search
        }
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
                        String countryCode = null;
                        if (response.has("address")) {
                            JSONObject addressObj = response.getJSONObject("address");
                            countryCode = addressObj.optString("country_code", null);
                        }
                        Log.d("HomeFragment", "reverseGeocode: Detected location is " + displayName + ", Country Code: " + countryCode);
                        detectedLocationList.clear();
                        detectedLocationList.add(displayName);
                        detectedLocationAdapter.notifyDataSetChanged();
                        currentDetectedLocation = displayName;
                        // Always add detected location to usDetectedLocationList for weather fetch
                        usDetectedLocationList.clear();
                        Log.d(TAG, "Adding displayName to US detected location list: " + displayName);
                        usDetectedLocationList.add(displayName);
                        // Always trigger weather data fetch for detected location
                        List<String> singleLocationList = new ArrayList<>();
                        singleLocationList.add(displayName);
                        weatherViewModel.fetchWeatherForLocations(requireContext(), singleLocationList);
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
                        Log.d("HomeFragment", "reverseGeocodingWithFallback: Detected location is " + displayName);
                        detectedLocationList.clear();
                        detectedLocationList.add(displayName);
                        detectedLocationAdapter.notifyDataSetChanged();
                        currentDetectedLocation = displayName;
                        // Always add detected location to usDetectedLocationList for weather fetch
                        usDetectedLocationList.clear();
                        Log.d(TAG, "Adding displayName to US detected location list: " + displayName);
                        usDetectedLocationList.add(displayName);
                        // Always trigger weather data fetch for detected location
                        List<String> singleLocationList = new ArrayList<>();
                        singleLocationList.add(displayName);
                        weatherViewModel.fetchWeatherForLocations(requireContext(), singleLocationList);
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
                    .setTitle("Oh No! Looks like geocoding failed!")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            Log.w(TAG, "Error dialog not shown: Fragment is not attached.");
        }
    }

    private void showUsOnlyDialog(String attemptedAddress, @Nullable String countryCode) {
        if (!isAdded() || getActivity() == null) return;
        String message;
        if (countryCode != null && !countryCode.isEmpty() && !"us".equalsIgnoreCase(countryCode)) {
            message = "Sorry, '" + attemptedAddress + "' (located in " + countryCode.toUpperCase() + ") is not supported. This app currently only provides weather information for locations within the United States.";
        } else if (attemptedAddress != null && !attemptedAddress.isEmpty()) {
            message = "Could not determine if '" + attemptedAddress + "' is in the US. Please check your input or try another location.";
        } else {
            message = "Location not supported.";
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
        cancelWeatherFetching();
        showLoading();
        // Start weather fetch for the location
        List<String> singleLocationList = new ArrayList<>();
        singleLocationList.add(location);
        weatherViewModel.fetchWeatherForLocations(requireContext(), singleLocationList);
        // Wait for weather data, then navigate
        reloadHandler.postDelayed(() -> {
            Map<String, WeatherViewModel.WeatherSummary> currentMap = weatherViewModel.getLocationWeatherMap().getValue();
            boolean hasWeather = false;
            if (currentMap != null && currentMap.get(location) != null && currentMap.get(location).temperature != null && !currentMap.get(location).temperature.isEmpty() && !currentMap.get(location).temperature.equals("--")) {
                hasWeather = true;
            }
            if (hasWeather) {
                hideLoading();
                if (navigateToForecastListener != null) {
                    navigateToForecastListener.onNavigateToForecast(location);
                }
            } else {
                // Retry after delay if weather not ready
                reloadHandler.postDelayed(() -> setLocationAndNavigateToForecast(location), RELOAD_DELAY_MS);
            }
        }, RELOAD_DELAY_MS);
    }

    @Override
    public void onLocationClick(String location) {
        // Save the selected location to SharedPreferences
        writeToPreference(location);
        setLocationAndNavigateToForecast(location);
    }

    private void showLoading() {
        Log.d(TAG, "showLoading() called. progressBar=" + (progressBar != null ? progressBar.getVisibility() : "null"));
        if (progressBar != null && progressBar.getVisibility() != View.VISIBLE) {
            progressBar.setAlpha(1f);
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "showLoading() finished. progressBar=" + (progressBar != null ? progressBar.getVisibility() : "null"));
        } else {
            Log.d(TAG, "showLoading() failed. progressBar=" + (progressBar != null ? progressBar.getVisibility() : "null"));
        }
    }

    private void hideLoading() {
        Log.d(TAG, "hideLoading called");
        if (progressBar != null && progressBar.getVisibility() == View.VISIBLE) {
            progressBar.animate()
                .alpha(0f)
                .setDuration(200)
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
        List<String> allLocations = new ArrayList<>();
        // Stop if weather fetching has been cancelled
        if (weatherReloadAttempts >= MAX_WEATHER_RELOADS) {
            Log.d(TAG, "Weather fetching cancelled, stopping attempts");
            // Only hide spinner if there are no locations AND location loading is complete
            allLocations.addAll(usSavedLocationsList);
//            allLocations.addAll(usDetectedLocationList);
            if (allLocations.isEmpty()) {
                hideLoading();
            }
            return;
        }
        
        Log.d(TAG, "Weather data fetch attempt " + (weatherReloadAttempts + 1) + " of " + MAX_WEATHER_RELOADS);
        
        // Get current weather data to check what we already have
        Map<String, WeatherViewModel.WeatherSummary> currentMap = weatherViewModel.getLocationWeatherMap().getValue();
        

        // Asynchronously filter allLocations to only US locations using geocoding with countrycodes=us
        allLocations.addAll(usSavedLocationsList);
        Log.d(TAG, "Added all US saved locations to allLocations. Size:" + usSavedLocationsList.size());
//        allLocations.addAll(usDetectedLocationList);
//        Log.d(TAG, "Added US detected location to allLocations. Size:" + usDetectedLocationList.size());
        if (allLocations.isEmpty()) {
            weatherReloadAttempts++;
            Log.w(TAG, "allLocations is empty and has no locations, weatherReloadAttempts goes up but spinner stays.");
            // Do NOT hideLoading() here; let weather logic control spinner after all loading is complete
            if (weatherReloadAttempts < MAX_WEATHER_RELOADS) {
                reloadHandler.postDelayed(this::attemptWeatherDataFetch, RELOAD_DELAY_MS);
            } else {
                if (hasLocationTimeoutOccurred || !isLocationDetectionInProgress) {
                    if (locationLoadingComplete()) {
                        hideLoading();
                    }
                }
            }
            return;
        }

        // Track those needing weather
        List<String> locationsNeedingWeather = new ArrayList<>();
        for (String location : allLocations) {
            if (currentMap == null || currentMap.get(location) == null || 
                currentMap.get(location).temperature == null || 
                currentMap.get(location).temperature.isEmpty() || 
                currentMap.get(location).temperature.equals("--")) {
                locationsNeedingWeather.add(location);
            }
        }
        if (!locationsNeedingWeather.isEmpty()) {
            weatherViewModel.fetchWeatherForLocations(requireContext(), locationsNeedingWeather);
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

    private boolean locationLoadingComplete() {
        // Implement logic to check if all location loading (from storage/geocoding) is finished
        // For now, return true if completed[0] == savedSet.size() or similar
        return true; // TODO: Replace with actual check if needed
    }

    private void updateAdaptersWithWeatherData() {
        if (!isAdded() || getActivity() == null) return;
        Map<String, WeatherViewModel.WeatherSummary> currentMap = weatherViewModel.getLocationWeatherMap().getValue();
        if (currentMap != null) {
            // Only update savedLocationAdapter with saved locations
            Map<String, WeatherViewModel.WeatherSummary> savedMap = new HashMap<>();
            for (String loc : usSavedLocationsList) {
                if (currentMap.containsKey(loc)) {
                    savedMap.put(loc, currentMap.get(loc));
                }
            }
            savedLocationAdapter.setWeatherSummaries(savedMap);
            savedLocationAdapter.notifyDataSetChanged();
            // Only update detectedLocationAdapter with current detected location
            Map<String, WeatherViewModel.WeatherSummary> detectedMap = new HashMap<>();
            for (String loc : usDetectedLocationList) {
                if (currentMap.containsKey(loc)) {
                    detectedMap.put(loc, currentMap.get(loc));
                }
            }
            detectedLocationAdapter.setWeatherSummaries(detectedMap);
            detectedLocationAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel any pending reload attempts and weather fetching
        cancelWeatherFetching();
        // Do NOT hideLoading() here; let weather logic control spinner
    }

    private void cancelWeatherFetching() {
        reloadHandler.removeCallbacksAndMessages(null);
        weatherReloadAttempts = MAX_WEATHER_RELOADS;
        geocodeReloadAttempts = MAX_GEOCODE_RELOADS;
        isLocationDetectionInProgress = false;
        hasLocationTimeoutOccurred = false;
        if (requestQueue != null) requestQueue.cancelAll(TAG);
        Log.d(TAG, "Weather fetching cancelled.");
        // Do NOT hideLoading() here; let weather logic control spinner
    }
    private void checkCountryAndProceed(String address) {
        if (!isAdded() || getActivity() == null || address == null || address.trim().isEmpty()) {
            if (address != null && !address.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a valid address", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        showLoading();
        Log.d(TAG, "Checking country for address (with retry): " + address);
        // 1. Zip code check
        if (address.matches("\\d{5}")) {
            GeocodingRetryManager.geocodeWithRetry(
                requireContext(),
                address,
                USER_AGENT,
                "us",
                new GeocodingRetryManager.GeocodingSuccessCallback() {
                    @Override
                    public void onSuccess(GeocodingResponseParser.GeocodingResult usResult) {
                        if (!isAdded() || getActivity() == null) return;
                        String usCountryCode = usResult.getCountryCode();
                        String usDisplayName = usResult.getDisplayName();
                        Log.d(TAG, "Zipcode geocoding (countrycodes=us) success. Address: '" + address + "', DisplayName: '" + usDisplayName + "', Country Code: '" + usCountryCode + "'");
                        // Always show zip code warning dialog
                        String message = "You searched for a zip code: '" + address + "'.\n" +
                            "Location found: '" + usDisplayName + "' (" + (usCountryCode != null ? usCountryCode.toUpperCase() : "Unknown") + ").";
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Zip Code Search")
                            .setMessage(message)
                            .setPositiveButton("OK", (dialog, which) -> {
                                if ("us".equalsIgnoreCase(usCountryCode)) {
                                    writeToPreference(address);
                                    setLocationAndNavigateToForecast(address);
                                }
                                hideLoading();
                            })
                            .setCancelable(false)
                            .show();
                    }
                },
                new GeocodingRetryManager.GeocodingFailureCallback() {
                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded() || getActivity() == null) return;
                        hideLoading();
                        Log.e(TAG, "Zipcode geocoding failed after retries for country check (address: '" + address + "'): " + errorMessage);
                        showNominatimErrorDialog("Could not verify zipcode location. Please try again. Error: " + errorMessage);
                    }
                }
            );
            return;
        }
        // 2. Geocode with countrycodes=us
        GeocodingRetryManager.geocodeWithRetry(
            requireContext(),
            address,
            USER_AGENT,
            "us",
            new GeocodingRetryManager.GeocodingSuccessCallback() {
                @Override
                public void onSuccess(GeocodingResponseParser.GeocodingResult usResult) {
                    if (!isAdded() || getActivity() == null) return;
                    String usCountryCode = usResult.getCountryCode();
                    String usDisplayName = usResult.getDisplayName();
                    Log.d(TAG, "Geocoding (countrycodes=us) success. Address: '" + address + "', DisplayName: '" + usDisplayName + "', Country Code: '" + usCountryCode + "'");
                    // Now run second geocode without countrycodes for comparison only
                    GeocodingRetryManager.geocodeWithRetry(
                        requireContext(),
                        address,
                        USER_AGENT,
                        null,
                        new GeocodingRetryManager.GeocodingSuccessCallback() {
                            @Override
                            public void onSuccess(GeocodingResponseParser.GeocodingResult globalResult) {
                                if (!isAdded() || getActivity() == null) return;
                                String globalCountryCode = globalResult.getCountryCode();
                                String globalDisplayName = globalResult.getDisplayName();
                                Log.d(TAG, "Geocoding (no countrycodes) success. Address: '" + address + "', DisplayName: '" + globalDisplayName + "', Country Code: '" + globalCountryCode + "'");
                                // Show dialog with both results, but always use US result
                                showDualLocationDialog(usDisplayName, usCountryCode, globalDisplayName, globalCountryCode);
                                hideLoading();
                            }
                        },
                        new GeocodingRetryManager.GeocodingFailureCallback() {
                            @Override
                            public void onFailure(String errorMessage) {
                                if (!isAdded() || getActivity() == null) return;
                                hideLoading();
                                Log.e(TAG, "Global geocoding failed after retries for country check (address: '" + address + "'): " + errorMessage);
                                // Show dialog with only US result
                                showDualLocationDialog(usDisplayName, usCountryCode, usDisplayName, usCountryCode);
                            }
                        }
                    );
                }
            },
            new GeocodingRetryManager.GeocodingFailureCallback() {
                @Override
                public void onFailure(String errorMessage) {
                    if (!isAdded() || getActivity() == null) return;
                    hideLoading();
                    Log.e(TAG, "Geocoding failed after retries for country check (address: '" + address + "'): " + errorMessage);
                    showNominatimErrorDialog("Could not verify location. Is the location in the United States? Error: " + errorMessage);
                }
            }
        );
    }

    private void showDualLocationDialog(String usDisplayName, String usCountryCode, String globalDisplayName, String globalCountryCode) {
        if (!isAdded() || getActivity() == null) return;
        String message = "The location you have queried may have multiple candidates. Defaulting to: '" + usDisplayName + "' (" + usCountryCode.toUpperCase() + ").\n" +
                "If you continue, the app will use this result.\n" +
                "If you cancel, you will return to the homepage.\n\n";
        new AlertDialog.Builder(requireContext())
                .setTitle("Location Results Comparison")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Continue", (dialog, which) -> {
                    writeToPreference(usDisplayName);
                    setLocationAndNavigateToForecast(usDisplayName);
                    hideLoading();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    hideLoading();
                    // Optionally, you can add logic to reset UI or navigate to homepage here
                })
                .show();
    }
}