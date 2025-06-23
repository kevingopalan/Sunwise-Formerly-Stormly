package com.venomdevelopment.sunwise;

import static com.venomdevelopment.sunwise.GraphViewUtils.setLabelTypeface;
import static com.venomdevelopment.sunwise.GraphViewUtils.setTitleTypeface;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter; // Ensure this is imported

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import android.graphics.Paint;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import androidx.core.content.ContextCompat;

public class ForecastFragment extends Fragment {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String BASE_URL_POINTS = "https://api.weather.gov/points/";
    private static final String USER_AGENT = "Sunwise/v0-prerelease" + System.getProperty("http.agent");
    private LottieAnimationView animationViewForecast;
    private RequestQueue requestQueue;
    private TextView currentTempTextForecast; // Changed name for clarity
    private TextView highTempTextForecast;
    private TextView lowTempTextForecast;
    private TextView descTextForecast, humidityTextViewForecast, windTextViewForecast, precipitationTextViewForecast;
    private TextView locationDisplay;
    private Button saveLocationButton;
    private RecyclerView dailyRecyclerView;
    private RecyclerView horizontalHourlyRecyclerView;
    private HorizontalHourlyForecastAdapter horizontalHourlyAdapter;
    private WeatherViewModel weatherViewModel;
    GraphView hourlyGraphViewForecast, dailyGraphViewForecast;
    private String currentTemperature = "";
    private String dailyHighTemperature = "";

    public static final String myPref = "addressPref";

    private LinearLayout progressBar;

    private String tempUnit = "us";
    private String windUnit = "mph";
    private boolean showDecimalTemp = false;
    private boolean use24HourFormat = false;
    private SharedPreferences sunwisePrefs;

    public String getPreferenceValue() {
        SharedPreferences sp = getActivity().getSharedPreferences(myPref, 0);
        String str = sp.getString("address", "");
        return str;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_forecast, container, false);
        currentTempTextForecast = v.findViewById(R.id.currentTempText); // Changed ID in layout
        highTempTextForecast = v.findViewById(R.id.highTempText);
        lowTempTextForecast = v.findViewById(R.id.lowTempText);
        descTextForecast = v.findViewById(R.id.text_desc);
        locationDisplay = v.findViewById(R.id.locationDisplay);
        saveLocationButton = v.findViewById(R.id.saveLocationButton);
        animationViewForecast = v.findViewById(R.id.animation_view);
        dailyRecyclerView = v.findViewById(R.id.dailyRecyclerView);
        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dailyRecyclerView.setNestedScrollingEnabled(true);
        horizontalHourlyRecyclerView = v.findViewById(R.id.hourlyRecyclerView);
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        horizontalHourlyRecyclerView.setLayoutManager(horizontalLayoutManager);
        humidityTextViewForecast = v.findViewById(R.id.humidity);
        windTextViewForecast = v.findViewById(R.id.wind);
        precipitationTextViewForecast = v.findViewById(R.id.precipitation);
        hourlyGraphViewForecast = v.findViewById(R.id.hrGraphContent);
        dailyGraphViewForecast = v.findViewById(R.id.dayGraphContent);
        progressBar = v.findViewById(R.id.progressBar);

        // Initialize Volley RequestQueue
        requestQueue = SunwiseApp.getInstance().getRequestQueue();

        // Get the Shared ViewModel
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        String location = null;
        if (getArguments() != null && getArguments().containsKey("location")) {
            location = getArguments().getString("location");
        }
        if (location != null && !location.isEmpty()) {
            weatherViewModel.fetchWeatherForLocation(requireContext(), location);
        }

        // Observe LiveData and update UI as before
        weatherViewModel.getCurrentTemperature().observe(getViewLifecycleOwner(), temp -> {
            if (currentTempTextForecast != null) currentTempTextForecast.setText(temp);
        });
        weatherViewModel.getHighTemperature().observe(getViewLifecycleOwner(), high -> {
            if (highTempTextForecast != null) highTempTextForecast.setText(high);
        });
        weatherViewModel.getLowTemperature().observe(getViewLifecycleOwner(), low -> {
            if (lowTempTextForecast != null) lowTempTextForecast.setText(low);
        });
        weatherViewModel.getDescription().observe(getViewLifecycleOwner(), desc -> {
            if (descTextForecast != null) descTextForecast.setText(desc);
        });
        weatherViewModel.getHumidity().observe(getViewLifecycleOwner(), humidity -> {
            if (humidityTextViewForecast != null) humidityTextViewForecast.setText(humidity);
        });
        weatherViewModel.getWind().observe(getViewLifecycleOwner(), wind -> {
            if (windTextViewForecast != null) windTextViewForecast.setText(wind);
        });
        weatherViewModel.getPrecipitation().observe(getViewLifecycleOwner(), precip -> {
            if (precipitationTextViewForecast != null) precipitationTextViewForecast.setText(precip);
        });
        
        // Observe graph data
        weatherViewModel.getHourlyGraphData().observe(getViewLifecycleOwner(), series -> {
            if (hourlyGraphViewForecast != null && series != null) {
                hourlyGraphViewForecast.removeAllSeries();
                hourlyGraphViewForecast.addSeries(series);
                Log.d(TAG, "Hourly graph updated with " + series.getHighestValueX() + " data points");
            }
        });
        
        weatherViewModel.getDailyGraphDataDay().observe(getViewLifecycleOwner(), series -> {
            if (dailyGraphViewForecast != null && series != null) {
                dailyGraphViewForecast.removeAllSeries();
                dailyGraphViewForecast.addSeries(series);
                Log.d(TAG, "Daily graph updated with " + series.getHighestValueX() + " data points");
            }
        });

        sunwisePrefs = requireContext().getSharedPreferences("SunwiseSettings", Context.MODE_PRIVATE);
        tempUnit = sunwisePrefs.getString("unit", "us");
        windUnit = sunwisePrefs.getString("wind_unit", "mph");
        showDecimalTemp = sunwisePrefs.getBoolean("show_decimal_temp", false);
        use24HourFormat = sunwisePrefs.getBoolean("use_24_hour_format", false);

        saveLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentLocation = locationDisplay.getText().toString().trim();
                if (!currentLocation.isEmpty() && !currentLocation.equals("Location")) {
                    saveLocationToList(currentLocation);
                } else {
                    Toast.makeText(getContext(), "No location to save", Toast.LENGTH_SHORT).show();
                }
            }
        });
        locationDisplay.setText(getPreferenceValue());
        // Remove automatic weather fetch - let user manually trigger it
        
        // Get location from Bundle arguments first, fall back to SharedPreferences
        String locationToFetch = null;
        if (getArguments() != null && getArguments().containsKey("location")) {
            locationToFetch = getArguments().getString("location");
            locationDisplay.setText(locationToFetch);
        } else {
            locationToFetch = getPreferenceValue();
        }
        
        // Fetch weather data for the location
        if (!locationToFetch.isEmpty()) {
            fetchGeocodingData(locationToFetch);
        }

        hourlyGraphViewForecast.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        dailyGraphViewForecast.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        hourlyGraphViewForecast.getViewport().setXAxisBoundsManual(true);
        dailyGraphViewForecast.getViewport().setXAxisBoundsManual(true);
        hourlyGraphViewForecast.getViewport().setYAxisBoundsManual(false);
        hourlyGraphViewForecast.getViewport().setMinX(0);
        hourlyGraphViewForecast.getViewport().setMaxX(23);
        dailyGraphViewForecast.getViewport().setMinX(0);
        dailyGraphViewForecast.getViewport().setMaxX(6);
        setLabelTypeface(getContext(), hourlyGraphViewForecast, R.font.montsemibold);
        setLabelTypeface(getContext(), dailyGraphViewForecast, R.font.montsemibold);
        setTitleTypeface(getContext(), hourlyGraphViewForecast, R.font.montsemibold);
        setTitleTypeface(getContext(), dailyGraphViewForecast, R.font.montsemibold);

        return v;
    }

    private void fetchGeocodingData(String address) {
        showLoading();
        // Encode the address for the URL
        String encodedAddress = address.replaceAll(" ", "+");
        String baseUrl = NominatimHostManager.getRandomSearchUrl() + encodedAddress;
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
                                updateLocationDisplay(address);
                                fetchWeatherData(pointsUrl);
                                hideLoading();
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
                    headers.put("Accept", "application/geo+json,application/json");
                    headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
                    headers.put("Pragma", "no-cache");
                    headers.put("Expires", "0");
                    Log.d(TAG, "Request headers: " + headers.toString());
                    return headers;
                }
            };
            requestQueue.getCache().clear();
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
                                updateLocationDisplay(address);
                                fetchWeatherData(pointsUrl);
                                hideLoading();
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
                    headers.put("Accept", "application/geo+json,application/json");
                    headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
                    headers.put("Pragma", "no-cache");
                    headers.put("Expires", "0");
                    Log.d(TAG, "Request headers: " + headers.toString());
                    return headers;
                }
            };
            requestQueue.getCache().clear();
            if (isAdded()) requestQueue.add(jsonArrayRequest);
        }
    }

    private void fetchGeocodingDataWithFallback(String address) {
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
                            updateLocationDisplay(address);
                            fetchWeatherData(pointsUrl);
                            hideLoading();
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
                headers.put("Accept", "application/geo+json,application/json");
                headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.put("Pragma", "no-cache");
                headers.put("Expires", "0");
                Log.d(TAG, "Request headers: " + headers.toString());
                return headers;
            }
        };
        requestQueue.getCache().clear();
        if (isAdded()) requestQueue.add(jsonArrayRequest);
    }

    private void fetchGeocodingDataWithCensusFallback(String address) {
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
                            updateLocationDisplay(address);
                            fetchWeatherData(pointsUrl);
                            hideLoading();
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
                headers.put("Accept", "application/geo+json,application/json");
                headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.put("Pragma", "no-cache");
                headers.put("Expires", "0");
                Log.d(TAG, "Request headers: " + headers.toString());
                return headers;
            }
        };
        requestQueue.getCache().clear();
        if (isAdded()) requestQueue.add(jsonObjectRequest);
    }

    private void tryRetryGeocoding(String address) {
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
                        updateLocationDisplay(address);
                        fetchWeatherData(pointsUrl);
                    }
                    hideLoading();
                },
                errorMessage -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "All geocoding services are currently unavailable. Please try again later.", Toast.LENGTH_LONG).show();
                    }
                    hideLoading();
                }
            );
        } else {
            if (isAdded()) {
                Toast.makeText(getContext(), "All geocoding services are currently unavailable. Please try again later.", Toast.LENGTH_LONG).show();
            }
            hideLoading();
        }
    }

    private void fetchWeatherData(String pointsUrl) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, pointsUrl, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        logLongJson(TAG, "Full weather API response: " + response.toString());
                        try {
                            // Extract the forecast URLs from the response
                            JSONObject properties = response.getJSONObject("properties");
                            String forecastUrl = properties.getString("forecast");
                            String forecastHourlyUrl = properties.getString("forecastHourly");

                            // Fetch daily forecast data
                            fetchDailyForecast(forecastUrl);

                            // Fetch hourly forecast data
                            fetchHourlyForecast(forecastHourlyUrl);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error parsing points data", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching points data: " + error.getMessage());
                        Toast.makeText(getContext(), "Error fetching points data", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                headers.put("Accept", "application/geo+json,application/json");
                headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.put("Pragma", "no-cache");
                headers.put("Expires", "0");
                Log.d(TAG, "Request headers: " + headers.toString());
                return headers;
            }
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                if (response != null && response.headers != null) {
                    Log.d(TAG, "Response headers: " + response.headers.toString());
                }
                return super.parseNetworkResponse(response);
            }
        };
        jsonObjectRequest.setShouldCache(false);
        requestQueue.getCache().clear();
        requestQueue.add(jsonObjectRequest);
    }

    private void fetchDailyForecast(String forecastUrl) {
        JsonObjectRequest forecastRequest = new JsonObjectRequest(
                Request.Method.GET, forecastUrl, null, response -> {
                    if (isAdded()) {
                    try {
                        JSONObject properties = response.getJSONObject("properties");
                        JSONArray periods = properties.getJSONArray("periods");
                        LineGraphSeries<DataPoint> daySeries = new LineGraphSeries<>();
                        LineGraphSeries<DataPoint> nightSeries = new LineGraphSeries<>();
                        boolean isDaytimeInitial = periods.getJSONObject(0).getBoolean("isDaytime");

                        for (int i = 0; i < 7; i++) {
                            JSONObject dayPeriod = periods.getJSONObject(2 * i);
                            JSONObject nightPeriod = periods.getJSONObject(2 * i + 1);
                            double dayTemp = dayPeriod.getDouble("temperature");
                            double nightTemp = nightPeriod.getDouble("temperature");
                            daySeries.appendData(new DataPoint(i, dayTemp), false, 7);
                            nightSeries.appendData(new DataPoint(i, nightTemp), false, 7);
                        }
                        weatherViewModel.setDailyGraphDataDay(daySeries);
                        weatherViewModel.setDailyGraphDataNight(nightSeries);

                        // ... (rest of your daily forecast parsing for RecyclerView) ...
                        ArrayList<SpannableString> dailyItems = new ArrayList<>();
                        ArrayList<String> dailyTime = new ArrayList<>();
                        ArrayList<String> dailyIcon = new ArrayList<>();
                        ArrayList<String> dailyPrecipitation = new ArrayList<>();
                        ArrayList<String> dailyHumidity = new ArrayList<>();
                        ArrayList<String> dailyLottieAnimList = new ArrayList<>();
                        ArrayList<String> dailyDescList = new ArrayList<>();
                        Map<String, DailyForecastPair> forecastMap = new HashMap<>();
                        for (int i = 0; i < periods.length(); i++) {
                            JSONObject current = periods.getJSONObject(i);
                            String name = current.optString("name");
                            boolean isDaytime = current.getBoolean("isDaytime");
                            String dayOfWeek = name.split(" ")[0];
                            DailyForecastPair pair;
                            if (!forecastMap.containsKey(dayOfWeek)) {
                                forecastMap.put(dayOfWeek, new DailyForecastPair());
                            }
                            pair = forecastMap.get(dayOfWeek);
                            double tempVal = current.optDouble("temperature", Double.NaN);
                            String temperature = Double.isNaN(tempVal) ? "--" : formatTemperature(tempVal, tempUnit, showDecimalTemp);
                            String description = current.optString("shortForecast");
                            String precipitationProbability = String.valueOf(current.getJSONObject("probabilityOfPrecipitation").optInt("value", 1013));
                            String humidityValue = current.has("relativeHumidity") ? current.getJSONObject("relativeHumidity").optInt("value") + "%" : "N/A";
                            if (precipitationProbability.equals("1013")) {
                                precipitationProbability = "N/A";
                            }
                            // Use icon URL from API instead
                            String iconUrl = current.optString("icon", "");
                            String icon = iconUrl;
                            String lottieAnim = iconUrl; // Use icon URL for lottie animation
                            String prefix = isDaytime ? "_day" : "_night";
                            pair.time = name.replace("This", "").trim();
                            if (name.contains("Afternoon")) {
                                pair.afternoonTemperature = temperature;
                                pair.afternoonIcon = icon;
                                pair.afternoonLottieAnim = lottieAnim;
                                pair.afternoonDescription = description;
                                pair.afternoonPrecipitation = precipitationProbability;
                                pair.afternoonHumidity = humidityValue;
                            } else if (name.contains("Tonight")) {
                                pair.tonightTemperature = temperature;
                                pair.tonightIcon = icon;
                                pair.tonightLottieAnim = lottieAnim;
                                pair.tonightDescription = description;
                                pair.tonightPrecipitation = precipitationProbability;
                                pair.tonightHumidity = humidityValue;
                            } else if (isDaytime && !name.contains("Night")) {
                                pair.dayTemperature = temperature;
                                pair.dayIcon = icon;
                                pair.dayLottieAnim = lottieAnim;
                                pair.dayDescription = description;
                                pair.dayPrecipitation = precipitationProbability;
                                pair.dayHumidity = humidityValue;
                            } else if (!isDaytime) {
                                pair.nightTemperature = temperature;
                                pair.nightIcon = icon;
                                pair.nightLottieAnim = lottieAnim;
                                pair.nightDescription = description;
                                pair.nightPrecipitation = precipitationProbability;
                                pair.nightHumidity = humidityValue;
                            }
                        }
                        Set<String> processedDays = new HashSet<>();
                        for (int i = 0; i < periods.length(); i++) {
                            JSONObject current = periods.getJSONObject(i);
                            String name = current.optString("name");
                            String dayOfWeek = name.split(" ")[0];
                            DailyForecastPair pair = forecastMap.get(dayOfWeek);
                            if (pair != null) {
                                SpannableString coloredTemperature = new SpannableString("");
                                String descriptionText = "";
                                String precipitationText = "";
                                String humidityText = "";
                                String primaryIcon = "";
                                String primaryLottieAnim = "";
                                boolean isDaytime = current.getBoolean("isDaytime");
                                String prefix = isDaytime ? "_day" : "_night";
                                String description = current.optString("shortForecast");
                                String icon;
                                String lottieAnim;
                                // OLD DESCRIPTION-BASED MAPPING - COMMENTED OUT
                                /*
                                if (description.toLowerCase().contains("snow")) {
                                    icon = "snow";
                                    lottieAnim = "snow";
                                } else if (description.toLowerCase().contains("rain") || description.toLowerCase().contains("showers")) {
                                    icon = "lrain";
                                    lottieAnim = "rain";
                                } else if (description.toLowerCase().contains("partly")) {
                                    icon = "pcloudy";
                                    lottieAnim = "partly_cloudy" + prefix;
                                } else if (description.toLowerCase().contains("sun")) {
                                    icon = "sun";
                                    lottieAnim = "clear" + prefix;
                                } else if (description.toLowerCase().contains("clear")) {
                                    icon = "clear";
                                    lottieAnim = "clear" + prefix;
                                } else if (description.toLowerCase().contains("storm")) {
                                    icon = "tstorm";
                                    lottieAnim = "thunderstorms" + prefix;
                                } else if (description.toLowerCase().contains("wind") || description.toLowerCase().contains("gale") || description.toLowerCase().contains("dust") || description.toLowerCase().contains("blow")) {
                                    icon = "wind";
                                    lottieAnim = "wind";
                                } else if (description.toLowerCase().contains("fog")) {
                                    icon = "clouds";
                                    lottieAnim = "fog";
                                } else if (description.toLowerCase().contains("haze")) {
                                    icon = "clouds";
                                    lottieAnim = "haze";
                                } else {
                                    icon = "clouds";
                                    lottieAnim = "cloudy";
                                }
                                */
                                // Use icon URL from API instead
                                String iconUrl = current.optString("icon", "");
                                icon = iconUrl;
                                lottieAnim = iconUrl;
                                if (name.contains("Afternoon")) {
                                    coloredTemperature = new SpannableString(pair.afternoonTemperature != null ? pair.afternoonTemperature : "");
                                    if (pair.afternoonTemperature != null) coloredTemperature.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.holo_red_light)), 0, pair.afternoonTemperature.length(), 0);
                                    descriptionText = pair.afternoonDescription;
                                    precipitationText = pair.afternoonPrecipitation != null ? "Afternoon: " + pair.afternoonPrecipitation : "";
                                    humidityText = pair.afternoonHumidity != null ? "Afternoon: " + pair.afternoonHumidity : "";
                                    primaryIcon = pair.afternoonIcon;
                                    primaryLottieAnim = pair.afternoonLottieAnim;
                                    dailyItems.add(coloredTemperature);
                                    dailyTime.add(name.replace("This", "").trim());
                                    dailyIcon.add(primaryIcon);
                                    dailyPrecipitation.add(precipitationText);
                                    dailyHumidity.add(humidityText);
                                    dailyLottieAnimList.add(primaryLottieAnim);
                                    dailyDescList.add(descriptionText);
                                    processedDays.add(dayOfWeek + "Afternoon");
                                } else if (name.contains("Tonight")) {
                                    coloredTemperature = new SpannableString(pair.tonightTemperature != null ? pair.tonightTemperature : "");
                                    if (pair.tonightTemperature != null) coloredTemperature.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.holo_blue_light)), 0, pair.tonightTemperature.length(), 0);
                                    descriptionText = pair.tonightDescription;
                                    precipitationText = pair.tonightPrecipitation != null ? "Tonight: " + pair.tonightPrecipitation : "";
                                    humidityText = pair.tonightHumidity != null ? "Tonight: " + pair.tonightHumidity : "";
                                    primaryIcon = pair.tonightIcon;
                                    primaryLottieAnim = pair.tonightLottieAnim;
                                    dailyItems.add(coloredTemperature);
                                    dailyTime.add(name.replace("This", "").trim());
                                    dailyIcon.add(primaryIcon);
                                    dailyPrecipitation.add(precipitationText);
                                    dailyHumidity.add(humidityText);
                                    dailyLottieAnimList.add(primaryLottieAnim);
                                    dailyDescList.add(descriptionText);
                                    processedDays.add(dayOfWeek + "Tonight");
                                } else if (!processedDays.contains(dayOfWeek)) {
                                    String tempText = "";
                                    if (pair.dayTemperature != null) {
                                        tempText += pair.dayTemperature;
                                    }
                                    if (pair.nightTemperature != null) {
                                        tempText += (pair.dayTemperature != null ? " / " : "") + pair.nightTemperature;
                                    }
                                    coloredTemperature = new SpannableString(tempText);
                                    if (pair.dayTemperature != null) {
                                        coloredTemperature.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.holo_red_light)), 0, pair.dayTemperature.length(), 0);
                                    }
                                    if (pair.nightTemperature != null) {
                                        int nightStart = tempText.indexOf(pair.nightTemperature);
                                        if (nightStart != -1) {
                                            coloredTemperature.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.holo_blue_light)), nightStart, nightStart + pair.nightTemperature.length(), 0);
                                        }
                                    }
                                    descriptionText = (pair.dayDescription != null ? pair.dayDescription : "") + (pair.nightDescription != null && pair.dayDescription != null && !pair.dayDescription.equals(pair.nightDescription) ? " / " + pair.nightDescription : (pair.nightDescription != null ? pair.nightDescription : ""));
                                    precipitationText = (pair.dayPrecipitation != null ? "Day: " + pair.dayPrecipitation : "") + (pair.nightPrecipitation != null ? " / Night: " + pair.nightPrecipitation : "");
                                    humidityText = (pair.dayHumidity != null ? "Day: " + pair.dayHumidity : "") + (pair.nightHumidity != null ? " / Night: " + pair.nightHumidity : "");
                                    primaryIcon = pair.dayIcon != null ? pair.dayIcon : pair.nightIcon;
                                    primaryLottieAnim = pair.dayLottieAnim != null ? pair.dayLottieAnim : pair.nightLottieAnim;
                                    dailyItems.add(coloredTemperature);
                                    dailyTime.add(dayOfWeek.replace("This", "").trim());
                                    dailyIcon.add(primaryIcon);
                                    dailyPrecipitation.add(precipitationText);
                                    dailyHumidity.add(humidityText);
                                    dailyLottieAnimList.add(primaryLottieAnim);
                                    dailyDescList.add(descriptionText);
                                    processedDays.add(dayOfWeek);
                                }
                            }
                        }
                        if (!dailyItems.isEmpty()) {
                            String temp = dailyItems.get(0).toString().split(" / ")[0];
                            String desc = dailyDescList.get(0).split(" / ")[0];
                            String highTemp = "";
                            String lowTemp = "";
                            for (DailyForecastPair pair : forecastMap.values()) {
                                if (pair.dayTemperature != null) {
                                    // Extract the numeric part and compare (e.g., "78°" -> 78)
                                    int dayTemp = Integer.parseInt(pair.dayTemperature.replaceAll("[^\\d.]", ""));
                                    if (highTemp.isEmpty() || dayTemp > Integer.parseInt(highTemp.replaceAll("[^\\d.]", ""))) {
                                        highTemp = pair.dayTemperature;
                                    }
                                }
                                if (pair.nightTemperature != null) {
                                    // Extract the numeric part and compare (e.g., "78°" -> 78)
                                    int nightTemp = Integer.parseInt(pair.nightTemperature.replaceAll("[^\\d.]", ""));
                                    if (lowTemp.isEmpty() || nightTemp < Integer.parseInt(lowTemp.replaceAll("[^\\d.]", ""))) {
                                        lowTemp = pair.nightTemperature;
                                    }
                                }
                            }
                            weatherViewModel.setHighTemperature(highTemp);
                            weatherViewModel.setLowTemperature(lowTemp);
                            weatherViewModel.setDescription(desc);
                            
                            // Safe animation loading with error handling
                            try {
                                if (!dailyLottieAnimList.isEmpty()) {
                                    String iconUrl = dailyLottieAnimList.get(0);
                                    String animationName = extractAnimationNameFromIcon(iconUrl);
                                    int animationResId = getResources().getIdentifier(animationName, "raw", getContext().getPackageName());
                                    if (animationResId == 0) {
                                        Log.w("ForecastFragment", "Missing animation for: " + animationName + ", falling back to not_available");
                                        animationResId = getResources().getIdentifier("not_available", "raw", getContext().getPackageName());
                                    }
                                    if (animationResId != 0) {
                                        animationViewForecast.setAnimation(animationResId);
                                        animationViewForecast.loop(true);
                                        animationViewForecast.playAnimation();
                                    } else {
                                        Log.e("ForecastFragment", "Even not_available animation not found, hiding animation view");
                                        animationViewForecast.setVisibility(View.GONE);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("ForecastFragment", "Error loading animation", e);
                                animationViewForecast.setVisibility(View.GONE);
                            }
                        }
                        DailyForecastAdapter adapter = new DailyForecastAdapter(getContext(), dailyItems, dailyTime, dailyIcon, dailyPrecipitation, dailyHumidity, dailyLottieAnimList, dailyDescList);
                        dailyRecyclerView.setAdapter(adapter);
                        final LinearLayoutManager layoutManager = (LinearLayoutManager) dailyRecyclerView.getLayoutManager();
                        final int[] firstVisibleItemPosition = new int[1];
                        adapter.setOnItemExpandListener(new DailyForecastAdapter.OnItemExpandListener() {
                            @Override
                            public void onItemExpanded(int position) {
                                firstVisibleItemPosition[0] = layoutManager.findFirstVisibleItemPosition();
                                layoutManager.scrollToPositionWithOffset(position, 0);
                            }
                            @Override
                            public void onItemContracted(int position) {
                                layoutManager.scrollToPosition(firstVisibleItemPosition[0]);
                            }
                        });

                        // After fetching daily forecast data and parsing periods:
                        ArrayList<Double> dailyHighs = new ArrayList<>();
                        ArrayList<Double> dailyLows = new ArrayList<>();
                        ArrayList<Integer> dayIndices = new ArrayList<>();
                        int dayIndex = 0;
                        for (int i = 0; i < periods.length(); i++) {
                            JSONObject period = periods.getJSONObject(i);
                            if (period.getString("isDaytime").equals("true")) {
                                double high = period.optDouble("temperature", Double.NaN);
                                if (!Double.isNaN(high)) {
                                    dailyHighs.add(high);
                                    dayIndices.add(dayIndex);
                                }
                            } else {
                                double low = period.optDouble("temperature", Double.NaN);
                                if (!Double.isNaN(low)) {
                                    dailyLows.add(low);
                                }
                                dayIndex++;
                            }
                        }
                        LineGraphSeries<DataPoint> highSeries = new LineGraphSeries<>();
                        LineGraphSeries<DataPoint> lowSeries = new LineGraphSeries<>();
                        for (int i = 0; i < dailyHighs.size(); i++) {
                            highSeries.appendData(new DataPoint(i, dailyHighs.get(i)), false, dailyHighs.size());
                        }
                        for (int i = 0; i < dailyLows.size(); i++) {
                            lowSeries.appendData(new DataPoint(i, dailyLows.get(i)), false, dailyLows.size());
                        }
                        highSeries.setColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));
                        lowSeries.setColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light));
                        dailyGraphViewForecast.removeAllSeries();
                        dailyGraphViewForecast.addSeries(highSeries);
                        dailyGraphViewForecast.addSeries(lowSeries);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing daily forecast data", Toast.LENGTH_SHORT).show();
                    }
                    } else {
                        Log.d("ForecastFragment", "Response received but fragment is not attached, ignoring UI update.");
                        // Optionally cancel further processing of this response if it's no longer relevant
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching daily forecast: " + error.getMessage());
                        Toast.makeText(getContext(), "Error fetching daily forecast", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                headers.put("Accept", "application/geo+json,application/json");
                headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.put("Pragma", "no-cache");
                headers.put("Expires", "0");
                Log.d(TAG, "Request headers: " + headers.toString());
                return headers;
            }
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                if (response != null && response.headers != null) {
                    Log.d(TAG, "Response headers: " + response.headers.toString());
                }
                return super.parseNetworkResponse(response);
            }
        };
        forecastRequest.setShouldCache(false);
        requestQueue.getCache().clear();
        requestQueue.add(forecastRequest);
    }
    private static class DailyForecastPair {
        String dayTemperature;
        String nightTemperature;
        String afternoonTemperature;
        String tonightTemperature;
        String dayIcon;
        String nightIcon;
        String afternoonIcon;
        String tonightIcon;
        String dayLottieAnim;
        String nightLottieAnim;
        String afternoonLottieAnim;
        String tonightLottieAnim;
        String dayDescription;
        String nightDescription;
        String afternoonDescription;
        String tonightDescription;
        String time;
        String dayPrecipitation;
        String nightPrecipitation;
        String afternoonPrecipitation;
        String tonightPrecipitation;
        String dayHumidity;
        String nightHumidity;
        String afternoonHumidity;
        String tonightHumidity;
        @NonNull
        @Override
        public String toString() {
            return "Day Temp: " + dayTemperature + ", Night Temp: " + nightTemperature +
                    ", Afternoon Temp: " + afternoonTemperature + ", Tonight Temp: " + tonightTemperature;
        }
    }
    private void fetchHourlyForecast(String forecastHourlyUrl) {
        Log.d(TAG, "DEBUGGING URL - Fetching hourly forecast from URL: " + forecastHourlyUrl);
        JsonObjectRequest forecastHourlyRequest = new JsonObjectRequest(
                Request.Method.GET, forecastHourlyUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                logLongJson(TAG, "Full hourly forecast API response: " + response.toString());
                if (isAdded()) {
                    try {
                        JSONObject properties = response.getJSONObject("properties");
                        JSONArray periods = properties.getJSONArray("periods");
                        if (periods.length() > 0) {
                            JSONObject currentHour = WeatherDataUtils.getFirstHourlyPeriod(response);
                            double tempVal = currentHour.optDouble("temperature", Double.NaN);
                            String temperature = Double.isNaN(tempVal) ? "--" : formatTemperature(tempVal, tempUnit, showDecimalTemp);
                            String shortForecast = currentHour.optString("shortForecast");
                            weatherViewModel.setCurrentTemperature(temperature); // Set CURRENT temp
                            weatherViewModel.setDescription(shortForecast);
                            String windspeed = currentHour.optString("windSpeed");
                            weatherViewModel.setWind(formatWind(windspeed, windUnit));
                            // Set humidity and precipitation for current hour
                            String precipitationProbability = String.valueOf(currentHour.getJSONObject("probabilityOfPrecipitation").optInt("value", 1013));
                            String humidityValue = currentHour.has("relativeHumidity") ? currentHour.getJSONObject("relativeHumidity").optInt("value") + "%" : "N/A";
                            if (precipitationProbability.equals("1013")) {
                                precipitationProbability = "N/A";
                            }
                            weatherViewModel.setPrecipitation(precipitationProbability + "%");
                            weatherViewModel.setHumidity(humidityValue);
                            
                            // Set dynamic background based on current hour's weather
                            String currentIconUrl = currentHour.optString("icon", "");
                            boolean isDaytime = currentHour.getBoolean("isDaytime");
                            setDynamicBackgroundFromIcon(currentIconUrl, isDaytime);
                        }
                        ArrayList<String> hourlyItems = new ArrayList<>();
                        ArrayList<String> hourlyTime = new ArrayList<>();
                        ArrayList<String> hourlyIcon = new ArrayList<>();
                        ArrayList<String> hourlyPrecipitation = new ArrayList<>();
                        ArrayList<String> hourlyHumidity = new ArrayList<>();
                        ArrayList<String> hourlyLottieAnimList = new ArrayList<>();
                        ArrayList<String> hourlyDescList = new ArrayList<>();
                        DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                        DateTimeFormatter outputFormatter = use24HourFormat ? 
                            DateTimeFormatter.ofPattern("HH:00") : 
                            DateTimeFormatter.ofPattern("h:00 a");
                        DateTimeFormatter dayOutputFormatter = DateTimeFormatter.ofPattern("EEE");
                        LocalDateTime now = LocalDateTime.now();
                        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
                        for (int i = 0; i < periods.length() && i < 48; i++) {
                            JSONObject current = periods.getJSONObject(i);
                            String temperatureStr = current.optString("temperature");
                            double tempVal = Double.NaN;
                            try { tempVal = Double.parseDouble(temperatureStr); } catch (Exception ignore) {}
                            String formattedTemp = Double.isNaN(tempVal) ? "--" : formatTemperature(tempVal, tempUnit, showDecimalTemp);
                            String description = current.optString("shortForecast");
                            String precipitationProbability = String.valueOf(current.getJSONObject("probabilityOfPrecipitation").optInt("value", 1013));
                            String humidityValue = current.has("relativeHumidity") ? current.getJSONObject("relativeHumidity").optInt("value") + "%" : "N/A";
                            if (precipitationProbability.equals("1013")) {
                                precipitationProbability = "N/A";
                            }
                            // Use icon URL from API
                            String iconUrl = current.optString("icon", "");
                            String icon = iconUrl;
                            String lottieAnim = iconUrl;
                            // Add to lists
                            LocalDateTime startTime = LocalDateTime.parse(current.getString("startTime"), inputFormatter);
                            String formattedTime = startTime.format(outputFormatter);
                            String formattedDay = startTime.format(dayOutputFormatter);
                            String displayTime = formattedTime;
                            if (i == 0 && now.getDayOfMonth() == startTime.getDayOfMonth()) {
                                displayTime = "Now";
                            } else if (now.getDayOfMonth() != startTime.getDayOfMonth() && startTime.getHour() == 0) {
                                displayTime = formattedDay;
                            } else if (now.getDayOfMonth() != startTime.getDayOfMonth()) {
                                displayTime = formattedDay + " " + formattedTime;
                            }
                            hourlyItems.add(formattedTemp);
                            hourlyTime.add(displayTime);
                            hourlyIcon.add(icon);
                            hourlyPrecipitation.add(precipitationProbability + "%");
                            hourlyHumidity.add(humidityValue);
                            hourlyLottieAnimList.add(lottieAnim); // Add the actual lottie animation
                            hourlyDescList.add(description);
                            // For graphing, use tempVal, not formattedTemp
                            DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            Date date = java.sql.Timestamp.valueOf(startTime.format(timestampFormatter));
                            Log.d(TAG, "Graph Point - Time: " + date + ", Temp: " + tempVal);  // Log data point
                            if (!Double.isNaN(tempVal)) {
                                series.appendData(new DataPoint(i, tempVal), false, 48);
                            }
                        }
                        weatherViewModel.setHourlyGraphData(series);
                        horizontalHourlyAdapter = new HorizontalHourlyForecastAdapter(getContext(), hourlyItems, hourlyTime, hourlyIcon, hourlyPrecipitation, hourlyHumidity, hourlyLottieAnimList, hourlyDescList);
                        horizontalHourlyRecyclerView.setAdapter(horizontalHourlyAdapter);
                        final LinearLayoutManager layoutManager = (LinearLayoutManager) horizontalHourlyRecyclerView.getLayoutManager();
                        final int[] firstVisibleItemPosition = new int[1];
                        horizontalHourlyAdapter.setOnItemExpandListener(new HorizontalHourlyForecastAdapter.OnItemExpandListener() {
                            @Override
                            public void onItemExpanded(int position) {
                                firstVisibleItemPosition[0] = layoutManager.findFirstVisibleItemPosition();
                                layoutManager.scrollToPositionWithOffset(position, 0); // Align start of item with start of RecyclerView
                            }

                            @Override
                            public void onItemContracted(int position) {
                                horizontalHourlyRecyclerView.smoothScrollToPosition(firstVisibleItemPosition[0]); // Keep smooth scrolling on contract
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing hourly forecast data", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("ForecastFragment", "Response received but fragment is not attached, ignoring UI update.");
                    // Optionally cancel further processing of this response if it's no longer relevant
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error fetching hourly forecast: " + error.getMessage());
                Toast.makeText(getContext(), "Error fetching hourly forecast", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                headers.put("Accept", "application/geo+json,application/json");
                headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.put("Pragma", "no-cache");
                headers.put("Expires", "0");
                Log.d(TAG, "Request headers: " + headers.toString());
                return headers;
            }
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                if (response != null && response.headers != null) {
                    Log.d(TAG, "Response headers: " + response.headers.toString());
                }
                return super.parseNetworkResponse(response);
            }
        };
        forecastHourlyRequest.setShouldCache(false);
        requestQueue.getCache().clear();
        requestQueue.add(forecastHourlyRequest);
    }
    private static class WeatherData {
        private String temperature;
        private String description;
        public WeatherData(String temperature, String description) {
            this.temperature = temperature;
            this.description = description;
        }
        public String getTemperature() {
            return temperature;
        }
        public String getDescription() {
            return description;
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
    private void logLongJson(String tag, String json) {
        int maxLogSize = 2000;
        for (int i = 0; i <= json.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = Math.min(start + maxLogSize, json.length());
            Log.d(tag, json.substring(start, end));
        }
    }
    private String formatTemperature(double temp, String unit, boolean showDecimal) {
        // US: °F, SI: °C, CA: °C, UK: °C
        double displayTemp = temp;
        String unitLabel = "°F";
        switch (unit) {
            case "si":
            case "ca":
            case "uk":
                displayTemp = (temp - 32) * 5.0 / 9.0;
                unitLabel = "°C";
                break;
            case "us":
            default:
                displayTemp = temp;
                unitLabel = "°F";
                break;
        }
        if (showDecimal) {
            DecimalFormat df = new DecimalFormat("#.#");
            df.setRoundingMode(RoundingMode.HALF_UP);
            return df.format(displayTemp) + unitLabel;
        } else {
            return Math.round(displayTemp) + unitLabel;
        }
    }
    private String formatWind(String windSpeedStr, String unit) {
        // windSpeedStr is like "10 mph" or "16 km/h"
        if (windSpeedStr == null || windSpeedStr.isEmpty()) return "--";
        String[] parts = windSpeedStr.split(" ");
        if (parts.length < 2) return windSpeedStr;
        try {
            double value = Double.parseDouble(parts[0]);
            String origUnit = parts[1];
            double ms = value;
            if (origUnit.contains("mph")) ms = value * 0.44704;
            else if (origUnit.contains("km")) ms = value / 3.6;
            // Convert to target
            String unitLabel = "mph";
            switch (unit) {
                case "si":
                    ms = ms; unitLabel = "m/s"; break;
                case "ca":
                    ms = ms * 3.6; unitLabel = "km/h"; break;
                case "uk":
                    ms = ms / 0.44704; unitLabel = "mph"; break;
                case "us":
                default:
                    ms = ms / 0.44704; unitLabel = "mph"; break;
            }
            return (Math.round(ms * 10.0) / 10.0) + " " + unitLabel;
        } catch (Exception e) {
            return windSpeedStr;
        }
    }
    private void saveLocationToList(String location) {
        if (!isAdded() || getActivity() == null) return;
        
        SharedPreferences prefs = requireActivity().getSharedPreferences(myPref, 0);
        Set<String> savedLocations = prefs.getStringSet("saved_locations", new HashSet<>());
        
        // Create a new set to avoid modification issues
        Set<String> newSavedLocations = new HashSet<>(savedLocations);
        newSavedLocations.add(location);
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("saved_locations", newSavedLocations);
        editor.apply();
        
        Toast.makeText(getContext(), "Location saved: " + location, Toast.LENGTH_SHORT).show();
    }
    private void updateLocationDisplay(String location) {
        if (locationDisplay != null && isAdded()) {
            locationDisplay.setText(location);
        }
    }

    private String extractAnimationNameFromIcon(String iconUrl) {
        if (iconUrl == null || iconUrl.isEmpty()) {
            return "clear_day";
        }
        try {
            String iconName = iconUrl;
            if (iconUrl.startsWith("http")) {
                int idx = iconUrl.indexOf("/icons/land/");
                if (idx != -1) {
                    iconName = iconUrl.substring(idx + 12); // after /icons/land/
                    int qIdx = iconName.indexOf('?');
                    if (qIdx != -1) iconName = iconName.substring(0, qIdx);
                    int cIdx = iconName.indexOf(',');
                    if (cIdx != -1) iconName = iconName.substring(0, cIdx);
                }
            }
            // Handle complex weather conditions (e.g., "day/sct/tsra_hi")
            String[] weatherParts = iconName.split("/");
            if (weatherParts.length > 2) {
                String primaryWeather = weatherParts[weatherParts.length - 1];
                String dayNight = weatherParts[0];
                iconName = dayNight + "/" + primaryWeather;
            }
            return convertIconToAnimationName(iconName);
        } catch (Exception e) {
            Log.e("ForecastFragment", "Error extracting animation name from icon: " + iconUrl, e);
        }
        return "clear_day";
    }

    private String convertIconToAnimationName(String iconName) {
        // Thunderstorm and tornado mappings
        switch (iconName) {
            case "day/sct/tsra_hi":
            case "day/sct/tsra":
            case "night/sct/tsra_hi":
            case "night/sct/tsra":
            case "day/tsra": case "night/tsra":
            case "day/tsra_sct": case "night/tsra_sct":
            case "day/tsra_hi": case "night/tsra_hi":
            case "day/tornado": case "night/tornado":
                return "lightning_bolt";
        }
        
        // Original mappings
        switch (iconName) {
            // Clear/Sunny conditions
            case "day/skc": case "night/skc":
                return "clear_day";
            case "day/few": case "night/few":
                return "clear_day";
            case "day/wind_skc": case "night/wind_skc":
                return "clear_day";
            case "day/wind_few": case "night/wind_few":
                return "clear_day";
            case "day/wind_sct": case "night/wind_sct":
                return "partly_cloudy_day";
            case "day/wind_bkn": case "night/wind_bkn":
                return "cloudy";
            case "day/wind_ovc": case "night/wind_ovc":
                return "overcast";
                
            // Partly cloudy conditions
            case "day/sct": case "night/sct":
                return "partly_cloudy_day";
            case "day/sct/rain": case "night/sct/rain":
                return "partly_cloudy_day_rain";
            case "day/sct/snow": case "night/sct/snow":
                return "partly_cloudy_day_snow";
                
            // Mostly cloudy conditions
            case "day/bkn": case "night/bkn":
                return "cloudy";
            case "day/bkn/rain": case "night/bkn/rain":
                return "overcast_day_rain";
            case "day/bkn/snow": case "night/bkn/snow":
                return "overcast_day_snow";
                
            // Overcast conditions
            case "day/ovc": case "night/ovc":
                return "overcast";
            case "day/ovc/rain": case "night/ovc/rain":
                return "overcast_day_rain";
            case "day/ovc/snow": case "night/ovc/snow":
                return "overcast_day_snow";
                
            // Rain conditions
            case "day/rain": case "night/rain":
                return "rain";
            case "day/rain_showers": case "night/rain_showers":
                return "rain";
            case "day/rain_showers_hi": case "night/rain_showers_hi":
                return "rain";
                
            // Snow conditions
            case "day/snow": case "night/snow":
                return "snow";
            case "day/sleet": case "night/sleet":
                return "sleet";
            case "day/fzra": case "night/fzra":
                return "sleet"; // Freezing rain, use sleet animation
                
            // Severe weather
            case "day/hurricane": case "night/hurricane":
                return "hurricane";
            case "day/tropical_storm": case "night/tropical_storm":
                return "hurricane"; // Use hurricane animation
            case "day/blizzard": case "night/blizzard":
                return "snow"; // Use snow animation
                
            // Atmospheric conditions
            case "day/dust": case "night/dust":
                return "dust";
            case "day/smoke": case "night/smoke":
                return "overcast"; // No smoke animation, use overcast
            case "day/haze": case "night/haze":
                return "haze";
            case "day/fog": case "night/fog":
                return "fog";
                
            // Temperature extremes
            case "day/hot": case "night/hot":
                return "clear_day";
            case "day/cold": case "night/cold":
                return "clear_day";
                
            // Legacy/fallback cases
            case "day/clear": case "night/clear":
                return "clear_day";
            case "day/partly_cloudy": case "night/partly_cloudy":
                return "partly_cloudy_day";
            case "day/mostly_cloudy": case "night/mostly_cloudy":
                return "cloudy";
            case "day/drizzle": case "night/drizzle":
                return "rain";
            default:
                Log.w("ForecastFragment", "Unknown icon name: " + iconName + ", using clear_day as fallback");
                return "clear_day";
        }
    }

    private boolean isCurrentlyDaytime() {
        java.time.LocalTime now = java.time.LocalTime.now();
        int currentHour = now.getHour();
        
        // Consider 5 AM to 8 PM as daytime (15 hours of daylight)
        // This is a reasonable approximation for most locations
        return currentHour >= 5 && currentHour < 20;
    }

    private void setDynamicBackgroundFromIcon(String iconUrl, boolean isDaytime) {
        if (getView() == null) return;
        
        View rootView = getView();
        int gradientResId = getGradientForWeatherFromIcon(iconUrl, isDaytime);
        
        // Apply gradient background
        rootView.setBackground(ContextCompat.getDrawable(requireContext(), gradientResId));
    }
    
    private int getGradientForWeatherFromIcon(String iconUrl, boolean isDaytime) {
        if (iconUrl == null || iconUrl.isEmpty()) {
            return isDaytime ? R.drawable.gradient_clear_day : R.drawable.gradient_clear_night;
        }
        
        try {
            String iconName = iconUrl;
            if (iconUrl.startsWith("http")) {
                int idx = iconUrl.indexOf("/icons/land/");
                if (idx != -1) {
                    iconName = iconUrl.substring(idx + 12); // after /icons/land/
                    int qIdx = iconName.indexOf('?');
                    if (qIdx != -1) iconName = iconName.substring(0, qIdx);
                    int cIdx = iconName.indexOf(',');
                    if (cIdx != -1) iconName = iconName.substring(0, cIdx);
                }
            }
            
            // Handle complex weather conditions (e.g., "day/sct/tsra_hi")
            String[] weatherParts = iconName.split("/");
            if (weatherParts.length > 2) {
                String primaryWeather = weatherParts[weatherParts.length - 1];
                String dayNight = weatherParts[0];
                iconName = dayNight + "/" + primaryWeather;
            }
            
            return getGradientForIconCode(iconName, isDaytime);
        } catch (Exception e) {
            Log.e("ForecastFragment", "Error extracting icon code from URL: " + iconUrl, e);
        }
        
        return isDaytime ? R.drawable.gradient_clear_day : R.drawable.gradient_clear_night;
    }
    
    private int getGradientForIconCode(String iconName, boolean isDaytime) {
        // Thunderstorm and tornado mappings
        switch (iconName) {
            case "day/sct/tsra_hi":
            case "day/sct/tsra":
            case "night/sct/tsra_hi":
            case "night/sct/tsra":
            case "day/tsra": case "night/tsra":
            case "day/tsra_sct": case "night/tsra_sct":
            case "day/tsra_hi": case "night/tsra_hi":
            case "day/tornado": case "night/tornado":
                return isDaytime ? R.drawable.gradient_thunderstorm_day : R.drawable.gradient_thunderstorm_night;
        }
        
        // Rain conditions
        switch (iconName) {
            case "day/rain": case "night/rain":
            case "day/rain_showers": case "night/rain_showers":
            case "day/rain_showers_hi": case "night/rain_showers_hi":
            case "day/sct/rain": case "night/sct/rain":
            case "day/bkn/rain": case "night/bkn/rain":
            case "day/ovc/rain": case "night/ovc/rain":
            case "day/drizzle": case "night/drizzle":
                return isDaytime ? R.drawable.gradient_rain_day : R.drawable.gradient_rain_night;
        }
        
        // Snow conditions
        switch (iconName) {
            case "day/snow": case "night/snow":
            case "day/sleet": case "night/sleet":
            case "day/fzra": case "night/fzra":
            case "day/sct/snow": case "night/sct/snow":
            case "day/bkn/snow": case "night/bkn/snow":
            case "day/ovc/snow": case "night/ovc/snow":
            case "day/blizzard": case "night/blizzard":
                return isDaytime ? R.drawable.gradient_snow_day : R.drawable.gradient_snow_night;
        }
        
        // Cloudy conditions
        switch (iconName) {
            case "day/bkn": case "night/bkn":
            case "day/ovc": case "night/ovc":
            case "day/wind_bkn": case "night/wind_bkn":
            case "day/wind_ovc": case "night/wind_ovc":
                return isDaytime ? R.drawable.gradient_cloudy_day : R.drawable.gradient_cloudy_night;
        }
        
        // Fog/mist conditions
        switch (iconName) {
            case "day/fog": case "night/fog":
            case "day/haze": case "night/haze":
                return isDaytime ? R.drawable.gradient_fog_day : R.drawable.gradient_fog_night;
        }
        
        // Clear conditions (default)
        return isDaytime ? R.drawable.gradient_clear_day : R.drawable.gradient_clear_night;
    }
}