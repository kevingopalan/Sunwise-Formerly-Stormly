package com.venomdevelopment.sunwise;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.app.ProgressDialog;
import android.view.inputmethod.InputMethodManager;
import android.content.DialogInterface;
import android.view.inputmethod.InputMethodManager;

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
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.utils.ViewPortHandler;
import android.graphics.RectF;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.github.mikephil.charting.utils.Utils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.AdListener;

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
// Ensure this is imported

// Removed GraphView imports (GraphView has been replaced by MPAndroidChart BarChart)
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.time.LocalDate;
import android.util.TypedValue;

public class ForecastFragment extends Fragment {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String BASE_URL_POINTS = "https://api.weather.gov/points/";
    private static final String USER_AGENT = "Sunwise/v1 (venomdevelopmentofficial@gmail.com)" + System.getProperty("http.agent");
    private LottieAnimationView animationViewForecast;
    private RequestQueue requestQueue;
    private TextView currentTempTextForecast; // Changed name for clarity
    private TextView highTempTextForecast;
    private TextView lowTempTextForecast;
    private TextView descTextForecast, humidityTextViewForecast, windTextViewForecast, precipitationTextViewForecast;
    private TextView locationDisplay;
    private RecyclerView dailyRecyclerView;
    private RecyclerView horizontalHourlyRecyclerView;
    private HorizontalHourlyForecastAdapter horizontalHourlyAdapter;
    private WeatherViewModel weatherViewModel;
    // GraphView removed; MPAndroidChart BarCharts are used instead

    public static final String myPref = "addressPref";

    private LinearLayout progressBar;

    private String tempUnit = "us";
    private String windUnit = "mph";
    private boolean use24HourFormat;
    private SharedPreferences sunwisePrefs;

    private final Set<String> processedGeocodeAddresses = new HashSet<>();
    
    private AdView forecastAdView;
    private Handler reloadHandler = new Handler(Looper.getMainLooper());
    private FloatingActionButton reloadFab;
    private BarChart hourlyBarChart;
    private BarChart dailyBarChart;
    private BarChart snowdayBarChart;
    // Snow day widget
    private View snowDayWidgetContainer;
    private TextView snowDayWidgetPrediction;
    private TextView snowDayWidgetTitle;

    public String getPreferenceValue() {
        return sunwisePrefs.getString("address", "");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forecast, container, false);
        
        // Initialize views
        animationViewForecast = view.findViewById(R.id.animation_view);
        currentTempTextForecast = view.findViewById(R.id.currentTempText);
        highTempTextForecast = view.findViewById(R.id.highTempText);
        lowTempTextForecast = view.findViewById(R.id.lowTempText);
        descTextForecast = view.findViewById(R.id.text_desc);
        humidityTextViewForecast = view.findViewById(R.id.humidity);
        windTextViewForecast = view.findViewById(R.id.wind);
        precipitationTextViewForecast = view.findViewById(R.id.precipitation);
        locationDisplay = view.findViewById(R.id.locationDisplay);
        Button saveLocationButton = view.findViewById(R.id.saveLocationButton);
        dailyRecyclerView = view.findViewById(R.id.dailyRecyclerView);
        horizontalHourlyRecyclerView = view.findViewById(R.id.hourlyRecyclerView);
    // GraphView elements removed from layout; BarCharts are used instead
        progressBar = view.findViewById(R.id.progressBar);
        forecastAdView = view.findViewById(R.id.forecast_ad);
        reloadFab = view.findViewById(R.id.reloadFab);
        hourlyBarChart = view.findViewById(R.id.hourlyBarGraph);
        dailyBarChart = view.findViewById(R.id.dailyBarGraph);
        hourlyBarChart.setDrawBarShadow(false);
        hourlyBarChart.setDrawValueAboveBar(true);
        hourlyBarChart.getDescription().setEnabled(false);
        hourlyBarChart.setDrawGridBackground(false);
        hourlyBarChart.setPinchZoom(false);
        if (dailyBarChart != null) {
            dailyBarChart.setDrawBarShadow(false);
            dailyBarChart.setDrawValueAboveBar(false);
            dailyBarChart.getDescription().setEnabled(false);
            dailyBarChart.setDrawGridBackground(false);
            dailyBarChart.setPinchZoom(false);
            dailyBarChart.setScaleEnabled(false);
            dailyBarChart.getLegend().setEnabled(false);
        }
        // Initialize ViewModel
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        // Initialize Volley RequestQueue
        requestQueue = SunwiseApp.getInstance().getRequestQueue();

    // Load preferences (use the same as SettingsFragment)
    sunwisePrefs = requireActivity().getSharedPreferences("SunwiseSettings", Context.MODE_PRIVATE);
    tempUnit = sunwisePrefs.getString("unit", "us");
    windUnit = sunwisePrefs.getString("wind_unit", "mph");
    use24HourFormat = sunwisePrefs.getBoolean("use_24_hour_format", false);

        // Setup RecyclerViews
        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        horizontalHourlyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Setup AdView
        AdRequest adRequest = new AdRequest.Builder().build();
        forecastAdView.loadAd(adRequest);
        forecastAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Forecast fragment ad loaded successfully");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                Log.e(TAG, "Forecast fragment ad failed to load: " + loadAdError.getMessage());
            }
        });

        // Setup reload FAB
        reloadFab.setOnClickListener(v -> {
            reloadFab.setEnabled(false);
            reloadFab.animate().rotationBy(360f).setDuration(1000).withEndAction(() -> {
                reloadFab.setEnabled(true);
                reloadFab.setRotation(0f);
            }).start();
            
            // Reload the entire fragment
            if (getParentFragmentManager() != null) {
                // Get current arguments
                Bundle currentArgs = getArguments();
                // Replace the current fragment with a new instance
                ForecastFragment newFragment = new ForecastFragment();
                if (currentArgs != null) {
                    newFragment.setArguments(currentArgs);
                }
                
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, newFragment)
                    .addToBackStack(null)
                    .commit();
            }
        });

        // Setup save location button
        saveLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String location = locationDisplay.getText().toString();
                if (!location.isEmpty() && !location.equals("Location")) {
                    saveLocationToList(location);
                    Toast.makeText(getContext(), "Location saved!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Observe ViewModel data
        weatherViewModel.getCurrentTemperature().observe(getViewLifecycleOwner(), temp -> {
            if (currentTempTextForecast != null) {
                currentTempTextForecast.setText(temp);
            }
        });

        weatherViewModel.getHighTemperature().observe(getViewLifecycleOwner(), temp -> {
            if (highTempTextForecast != null) {
                highTempTextForecast.setText(temp);
            }
        });

        weatherViewModel.getLowTemperature().observe(getViewLifecycleOwner(), temp -> {
            if (lowTempTextForecast != null) {
                lowTempTextForecast.setText(temp);
            }
        });

        weatherViewModel.getDescription().observe(getViewLifecycleOwner(), desc -> {
            if (descTextForecast != null) {
                descTextForecast.setText(desc);
            }
        });

        weatherViewModel.getHumidity().observe(getViewLifecycleOwner(), humidity -> {
            if (humidityTextViewForecast != null) {
                humidityTextViewForecast.setText(humidity);
            }
        });

        weatherViewModel.getWind().observe(getViewLifecycleOwner(), wind -> {
            if (windTextViewForecast != null) {
                windTextViewForecast.setText(wind);
            }
        });

        weatherViewModel.getPrecipitation().observe(getViewLifecycleOwner(), precip -> {
            if (precipitationTextViewForecast != null) {
                precipitationTextViewForecast.setText(precip);
            }
        });

    // Setup charts with proper styling and fonts
    setupGraphs();

    // Setup snow day widget
    setupSnowDayWidget(view);

        // Get location from arguments or preferences
        Bundle args = getArguments();
        if (args != null && args.containsKey("location")) {
            String location = args.getString("location");
            updateLocationDisplay(location);
            fetchGeocodingData(location);
        } else {
            String location = getPreferenceValue();
            if (!location.isEmpty()) {
                updateLocationDisplay(location);
                fetchGeocodingData(location);
            }
        }

        return view;
    }

    private void setupGraphs() {
        // Old GraphView removed; MPAndroidChart styling is applied where charts are configured.

        // GraphView observers removed (GraphView UI replaced by MPAndroidChart BarCharts).
    }

    private void setupSnowDayWidget(View root) {
        try {
            // Bind views from included layout
            snowDayWidgetContainer = root.findViewById(R.id.snow_day_widget);
            snowDayWidgetPrediction = root.findViewById(R.id.snow_day_widget_prediction);
            snowDayWidgetTitle = root.findViewById(R.id.snow_day_widget_title);
            if (snowDayWidgetContainer == null || snowDayWidgetPrediction == null) return;
            // Inline inputs (snowdays + school type) and bar chart
            EditText snowdaysInput = root.findViewById(R.id.snowdays_input);
            Spinner schoolTypeSpinner = root.findViewById(R.id.snowday_schooltype);
            Button calculateBtn = root.findViewById(R.id.snowday_calculate_button);
            snowdayBarChart = root.findViewById(R.id.snowday_bar_chart);

            // Setup spinner options
            try {
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(), R.array.school_types, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                schoolTypeSpinner.setAdapter(adapter);
            } catch (Exception ignored) {}

            calculateBtn.setOnClickListener(v -> {
                String zipcode = getCurrentZipcode();
                if (zipcode == null || zipcode.isEmpty()) {
                    Toast.makeText(getContext(), "Couldn't get zipcode", Toast.LENGTH_SHORT).show();
                    return;
                }
                int days = 0;
                try { days = Integer.parseInt(snowdaysInput.getText().toString().trim()); } catch (Exception ignored) {}
                com.venomdevelopment.sunwise.SnowDayCalculator.SchoolType schoolType = com.venomdevelopment.sunwise.SnowDayCalculator.SchoolType.values()[schoolTypeSpinner.getSelectedItemPosition()];
                new SnowDayAsyncTask(zipcode, days, schoolType).execute();
            });

            // Do not auto-run at startup using potentially stale stored zipcode.
            // Instead, when the app receives a geocoded location we will save the zipcode
            // and trigger the snow-day calculation from updateLocationDisplay(...).
        } catch (Exception e) {
            Log.e(TAG, "Error setting up snow day widget", e);
        }
    }

    // AsyncTask to call Kotlin SnowDayCalculator and update UI
    private class SnowDayAsyncTask extends android.os.AsyncTask<Void, Void, java.util.Map<String, Long>> {
        private final String zipcode;
        private final int snowdays;
        private final com.venomdevelopment.sunwise.SnowDayCalculator.SchoolType schoolType;
        private Exception error;

        SnowDayAsyncTask(String zipcode, int snowdays, com.venomdevelopment.sunwise.SnowDayCalculator.SchoolType schoolType) {
            this.zipcode = zipcode;
            this.snowdays = snowdays;
            this.schoolType = schoolType;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (snowDayWidgetPrediction != null) snowDayWidgetPrediction.setText("Loading...");
            showLoading();
        }

        @Override
        protected java.util.Map<String, Long> doInBackground(Void... voids) {
            try {
                // Call Kotlin class directly
                com.venomdevelopment.sunwise.SnowDayCalculator calc = new com.venomdevelopment.sunwise.SnowDayCalculator(zipcode, snowdays, schoolType);
                return calc.getPredictions();
            } catch (Exception e) {
                Log.e(TAG, "SnowDay calculation failed", e);
                this.error = e;
                return new java.util.HashMap<>();
            }
        }

        @Override
        protected void onPostExecute(java.util.Map<String, Long> result) {
            hideLoading();
            if (!isAdded()) return;
            try {
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault());
                java.util.Calendar cal = java.util.Calendar.getInstance();
                String today = dateFormat.format(cal.getTime());
                Long pToday = result.get(today);
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
                String tomorrow = dateFormat.format(cal.getTime());
                Long pTomorrow = result.get(tomorrow);
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
                String twoDays = dateFormat.format(cal.getTime());
                Long pTwo = result.get(twoDays);
                // Update textual summary
                String display = "Today: " + (pToday != null ? (pToday < 0 ? "Limited" : pToday + "%") : "N/A")
                    + "\nTomorrow: " + (pTomorrow != null ? (pTomorrow < 0 ? "Limited" : pTomorrow + "%") : "N/A")
                    + "\nDay After: " + (pTwo != null ? (pTwo < 0 ? "Limited" : pTwo + "%") : "N/A");
                if (snowDayWidgetPrediction != null) snowDayWidgetPrediction.setText(display);

                // Build bar chart entries (use 0 for N/A or Limited values)
                if (snowdayBarChart != null) {
                    java.util.ArrayList<BarEntry> entries = new java.util.ArrayList<>();
                    float todayVal = (pToday != null && pToday >= 0) ? pToday.floatValue() : 0f;
                    float tomorrowVal = (pTomorrow != null && pTomorrow >= 0) ? pTomorrow.floatValue() : 0f;
                    float twoVal = (pTwo != null && pTwo >= 0) ? pTwo.floatValue() : 0f;
                    entries.add(new BarEntry(0f, todayVal));
                    entries.add(new BarEntry(1f, tomorrowVal));
                    entries.add(new BarEntry(2f, twoVal));

                    BarDataSet set = new BarDataSet(entries, "Snow Day Chance");
                    int barColor = ContextCompat.getColor(requireContext(), R.color.df_low);
                    set.setColor(barColor);
                    set.setDrawValues(true);
                    set.setValueTextSize(12f);
                    set.setValueTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                    android.graphics.Typeface montsemibold = ResourcesCompat.getFont(getContext(), R.font.montsemibold);
                    set.setValueTypeface(montsemibold);

                    BarData data = new BarData(set);
                    data.setBarWidth(0.6f);
                    snowdayBarChart.setData(data);

                    java.util.ArrayList<String> labels = new java.util.ArrayList<>();
                    labels.add("Today"); labels.add("Tomorrow"); labels.add("Day After");

                    XAxis x = snowdayBarChart.getXAxis();
                    x.setGranularity(1f);
                    x.setPosition(XAxis.XAxisPosition.BOTTOM);
                    x.setDrawGridLines(false);
                    x.setValueFormatter(new IndexAxisValueFormatter(labels));
                    x.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                    x.setTextSize(12f);

                    YAxis y = snowdayBarChart.getAxisLeft();
                    y.setDrawGridLines(false);
                    y.setAxisMinimum(0f);
                    y.setAxisMaximum(100f);
                    y.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                    y.setTextSize(12f);
                    y.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            return Math.round(Double.parseDouble(super.getFormattedValue(value))) + "%";
                        }
                    });
                    setTypefaceIfAvailable(x, R.font.montsemibold);
                    setTypefaceIfAvailable(y, R.font.montsemibold);
                    snowdayBarChart.setRenderer(new RoundedBarChartRenderer(snowdayBarChart, snowdayBarChart.getAnimator(), snowdayBarChart.getViewPortHandler(), Utils.convertDpToPixel(8f)));
                    snowdayBarChart.getAxisRight().setEnabled(false);
                    snowdayBarChart.getLegend().setEnabled(false);
                    snowdayBarChart.getDescription().setEnabled(false);
                    snowdayBarChart.setFitBars(true);
                    snowdayBarChart.invalidate();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating snow day UI", e);
                if (snowDayWidgetPrediction != null) snowDayWidgetPrediction.setText("Error");
            }
        }
    }

    private void fetchGeocodingData(String address) {
        if (processedGeocodeAddresses.contains(address)) return;
        showLoading();
        String encodedAddress = address.replaceAll(" ", "+");
        String baseUrl = NominatimHostManager.getRandomSearchUrl() + encodedAddress;
        final boolean isCensus = NominatimHostManager.isCensusGeocoderUrl(baseUrl);
        final String geocodeUrl = isCensus
            ? baseUrl + NominatimHostManager.getCensusGeocoderParams()
            : baseUrl + "&format=json&addressdetails=1&countrycodes=us";

        // Use a single method for both requests, reduce duplicate logic
        Runnable onFallback = () -> { if (isAdded()) fetchGeocodingDataWithFallback(address); };
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("User-Agent", USER_AGENT);
        headers.put("Accept", "application/geo+json,application/json");
        headers.put("Cache-Control", "no-cache");
        headers.put("Pragma", "no-cache");
        headers.put("Expires", "0");

        if (isCensus) {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, geocodeUrl, null, response -> {
                    try {
                        GeocodingResponseParser.GeocodingResult result =
                            GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                        if (result != null) {
                            NominatimHostManager.recordHostSuccess(geocodeUrl);
                            String pointsUrl = BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude();
                            updateLocationDisplay(address);
                            processedGeocodeAddresses.add(address);
                            fetchWeatherData(pointsUrl);
                        } else {
                            NominatimHostManager.addDelay(onFallback);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        NominatimHostManager.addDelay(onFallback);
                    }
                }, error -> {
                    Log.e(TAG, "Error fetching geocoding data from Census Geocoder: " + error.getMessage());
                    NominatimHostManager.addDelay(onFallback);
                }) {
                @Override
                public java.util.Map<String, String> getHeaders() { return headers; }
            };
            // Prevent Volley from caching geocoding responses and clear any existing cache for this URL
            try {
                jsonObjectRequest.setShouldCache(false);
                if (requestQueue != null && requestQueue.getCache() != null) requestQueue.getCache().remove(geocodeUrl);
            } catch (Exception ignored) {}
            assert requestQueue != null;
            requestQueue.getCache().clear();
            if (isAdded()) requestQueue.add(jsonObjectRequest);
        } else {
            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, geocodeUrl, null, response -> {
                    try {
                        GeocodingResponseParser.GeocodingResult result =
                            GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                        if (result != null) {
                            NominatimHostManager.recordHostSuccess(geocodeUrl);
                            String pointsUrl = BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude();
                            updateLocationDisplay(address);
                            processedGeocodeAddresses.add(address);
                            fetchWeatherData(pointsUrl);
                        } else {
                            NominatimHostManager.addDelay(onFallback);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        NominatimHostManager.addDelay(onFallback);
                    }
                }, error -> {
                    Log.e(TAG, "Error fetching geocoding data from Nominatim: " + error.getMessage());
                    NominatimHostManager.addDelay(onFallback);
                }) {
                @Override
                public java.util.Map<String, String> getHeaders() { return headers; }
            };
            try {
                jsonArrayRequest.setShouldCache(false);
                if (requestQueue != null && requestQueue.getCache() != null) requestQueue.getCache().remove(geocodeUrl);
            } catch (Exception ignored) {}
            requestQueue.getCache().clear();
            if (isAdded()) requestQueue.add(jsonArrayRequest);
        }
    }

    private void fetchGeocodingDataWithFallback(String address) {
        if (processedGeocodeAddresses.contains(address)) return;
        if (!isAdded()) return;
        // Encode the address for the URL
        String encodedAddress = address.replaceAll(" ", "+");
        String baseUrl = NominatimHostManager.getFallbackSearchUrl() + encodedAddress;
        String params = "format=json&addressdetails=1";
        if (!baseUrl.contains("countrycodes=us")) {
            params += "&countrycodes=us";
        }
        String geocodeUrl = baseUrl + "&" + params;

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
                            processedGeocodeAddresses.add(address);
                            fetchWeatherData(pointsUrl);
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
                headers.put("Cache-Control", "no-cache");
                headers.put("Pragma", "no-cache");
                headers.put("Expires", "0");
                Log.d(TAG, "Request headers: " + headers);
                return headers;
            }
        };
        try {
            jsonArrayRequest.setShouldCache(false);
            if (requestQueue != null && requestQueue.getCache() != null) requestQueue.getCache().remove(geocodeUrl);
        } catch (Exception ignored) {}
        requestQueue.getCache().clear();
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
                            updateLocationDisplay(address);
                            processedGeocodeAddresses.add(address);
                            fetchWeatherData(pointsUrl);
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
                headers.put("Cache-Control", "no-cache");
                headers.put("Pragma", "no-cache");
                headers.put("Expires", "0");
                Log.d(TAG, "Request headers: " + headers);
                return headers;
            }
        };
        try {
            jsonObjectRequest.setShouldCache(false);
            if (requestQueue != null && requestQueue.getCache() != null) requestQueue.getCache().remove(geocodeUrl);
        } catch (Exception ignored) {}
        requestQueue.getCache().clear();
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
                        updateLocationDisplay(address);
                        processedGeocodeAddresses.add(address);
                        fetchWeatherData(pointsUrl);
                    }
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
                headers.put("Cache-Control", "no-cache");
                headers.put("Pragma", "no-cache");
                headers.put("Expires", "0");
                Log.d(TAG, "Request headers: " + headers);
                return headers;
            }
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                if (response != null && response.headers != null) {
                    Log.d(TAG, "Response headers: " + response.headers);
                }
                return super.parseNetworkResponse(response);
            }
        };
        jsonObjectRequest.setShouldCache(false);
        requestQueue.getCache().clear();
        requestQueue.add(jsonObjectRequest);
    }

    // Helper to convert temperature for graph plotting
    private double convertTemperatureForGraph(double temp, String unit) {
        switch (unit) {
            case "si":
            case "ca":
            case "uk":
                return (temp - 32) * 5.0 / 9.0;
            case "us":
            default:
                return temp;
        }
    }

    private void fetchDailyForecast(String forecastUrl) {
        JsonObjectRequest forecastRequest = new JsonObjectRequest(
                Request.Method.GET, forecastUrl, null, response -> {
                    if (isAdded()) {
                    try {
                        // Explicitly clear old GraphView references removed; no-op
                        JSONObject properties = response.getJSONObject("properties");
                        JSONArray periods = properties.getJSONArray("periods");
                        // GraphView LineGraphSeries removed; we only collect temps for the BarChart
                        boolean isDaytimeInitial = periods.getJSONObject(0).getBoolean("isDaytime");
                        JSONObject dayPeriod;
                        JSONObject nightPeriod;
                        // collect day and night temps to use for daily bar chart (we'll render stacked/duotone bars)
                        java.util.ArrayList<Float> collectedDayTemps = new java.util.ArrayList<>();
                        java.util.ArrayList<Float> collectedNightTemps = new java.util.ArrayList<>();
                        for (int i = 0; i < 7; i++) {
                            if (isDaytimeInitial) {
                                dayPeriod = periods.getJSONObject(2 * i);
                                nightPeriod = periods.getJSONObject(2 * i + 1);
                            } else {
                                dayPeriod = periods.getJSONObject(2 * i + 1);
                                nightPeriod = periods.getJSONObject(2 * i);
                            }
                            double dayTemp = dayPeriod.getDouble("temperature");
                            double nightTemp = nightPeriod.getDouble("temperature");
                            double dayTempConverted = convertTemperatureForGraph(dayTemp, tempUnit);
                            double nightTempConverted = convertTemperatureForGraph(nightTemp, tempUnit);
                            // collect plotted day temperature (no LineGraphSeries append)
                            // store day and night temps (as plotted) for daily bar chart
                            collectedDayTemps.add((float) dayTempConverted);
                            collectedNightTemps.add((float) nightTempConverted);
                            // collect plotted night temperature (no LineGraphSeries append)
                        }
                        // GraphView data removed: no longer publishing day/night LineGraphSeries to ViewModel

                        // Also populate the new daily BarChart below the GraphView
                        if (dailyBarChart != null) {
                            java.util.ArrayList<BarEntry> dailyBarEntries = new java.util.ArrayList<>();
                            java.util.ArrayList<String> dailyBarLabels = new java.util.ArrayList<>();
                            // Use collectedDayTemps as the primary daily values (avoid calling daySeries.getValues())
                            int daysToUse = Math.min(7, Math.min(collectedDayTemps.size(), collectedNightTemps.size()));
                            for (int i = 0; i < daysToUse; i++) {
                                try {
                                    float dayVal = collectedDayTemps.get(i);
                                    float nightVal = collectedNightTemps.get(i);
                                    // low = min(night, day), high = max(night, day)
                                    float low = Math.min(dayVal, nightVal);
                                    float high = Math.max(dayVal, nightVal);
                                    float lowerSegment = low;
                                    float upperSegment = Math.max(0f, high - low);
                                    // stacked BarEntry: [lowerSegment, upperSegment]
                                    dailyBarEntries.add(new BarEntry(i, new float[]{lowerSegment, upperSegment}));
                                    // Use weekday labels (Mon, Tue, ...)
                                    java.time.LocalDate date = java.time.LocalDate.now().plusDays(i);
                                    String dayLabel = date.format(java.time.format.DateTimeFormatter.ofPattern("EEE"));
                                    dailyBarLabels.add(dayLabel);
                                } catch (Exception e) {
                                    // fallback
                                    dailyBarEntries.add(new BarEntry(i, 0f));
                                    dailyBarLabels.add("");
                                }
                            }

                            BarDataSet dailyDataSet = new BarDataSet(dailyBarEntries, "Daily");
                            int colorOnSurface = ContextCompat.getColor(requireContext(), android.R.color.white);
                            if (getContext() != null) {
                                TypedValue typedValue = new TypedValue();
                                getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
                                colorOnSurface = typedValue.data;
                            }
                            int colorLow = ContextCompat.getColor(requireContext(), R.color.chart_low);
                            int colorHigh = ContextCompat.getColor(requireContext(), R.color.chart_high);
                            dailyDataSet.setColors(new int[]{colorLow, colorHigh});
                            dailyDataSet.setStackLabels(new String[]{"Low","High"});
                            // We draw values ourselves in the custom renderer to have full control
                            // Disable dataset value drawing to avoid duplicate/overlapping draws
                            dailyDataSet.setDrawValues(false);
                            dailyDataSet.setValueTextSize(14f);
                            try {
                                android.graphics.Typeface _tf2 = ResourcesCompat.getFont(getContext(), R.font.montsemibold);
                                if (_tf2 != null) dailyDataSet.setValueTypeface(_tf2);
                            } catch (Exception ignored) {}
                            BarData dailyBarData = new BarData(dailyDataSet);
                            dailyBarData.setBarWidth(0.6f);
                            dailyBarChart.setData(dailyBarData);

                            // X axis labels
                            XAxis dx = dailyBarChart.getXAxis();
                            dx.setGranularity(1f);
                            dx.setGranularityEnabled(true);
                            dx.setPosition(XAxis.XAxisPosition.BOTTOM);
                            dx.setDrawGridLines(false);
                            dx.setValueFormatter(new IndexAxisValueFormatter(dailyBarLabels));
                            dx.setTextColor(colorOnSurface);
                            dx.setTextSize(12f);
                            setTypefaceIfAvailable(dx, R.font.montsemibold);
                            // Push X axis labels a bit further down so value labels don't overlap them
                            dx.setYOffset(Utils.convertDpToPixel(12f));
                            // Don't force label count – let granularity=1 ensure integer steps and use axis min/max
                            // so labels align with bar centers. This avoids skipped/misaligned labels.
                            dx.setCenterAxisLabels(false);

                            // Y axis
                            YAxis dy = dailyBarChart.getAxisLeft();
                            dy.setDrawGridLines(false);
                            dy.setTextColor(colorOnSurface);
                            dy.setTextSize(12f);
                            dy.setValueFormatter(new ValueFormatter() {
                                @Override
                                public String getFormattedValue(float value) {
                                    return Math.round(Double.parseDouble(super.getFormattedValue(value))) + "º";
                                }
                            });
                            setTypefaceIfAvailable(dy, R.font.montsemibold);
                            if (!dailyBarEntries.isEmpty()) {
                                // For stacked bars we need the min of the lower segments and the max total value
                                float minLower = Float.MAX_VALUE;
                                float maxTotal = -Float.MAX_VALUE;
                                for (BarEntry e : dailyBarEntries) {
                                    if (e == null) continue;
                                    if (e.getYVals() != null) {
                                        float[] vals = e.getYVals();
                                        float lower = vals.length > 0 ? vals[0] : 0f;
                                        float total = 0f;
                                        for (float v : vals) total += v;
                                        if (lower < minLower) minLower = lower;
                                        if (total > maxTotal) maxTotal = total;
                                    } else {
                                        float lower = e.getY();
                                        if (lower < minLower) minLower = lower;
                                        if (e.getY() > maxTotal) maxTotal = e.getY();
                                    }
                                }
                                if (minLower == Float.MAX_VALUE) minLower = 0f;
                                if (maxTotal == -Float.MAX_VALUE) maxTotal = 0f;
                                float range = Math.max(1f, maxTotal - minLower);
                                // Start axis 5 units below the lowest low temperature so the low segment is visible
                                float bottomAxis = minLower - 5f;
                                float topPad = Math.max(5f, range * 0.10f);
                                dy.setAxisMinimum(bottomAxis);
                                dy.setAxisMaximum(maxTotal + topPad);
                            }
                            dailyBarChart.getAxisRight().setEnabled(false);
                            // Increase bottom extra offset to ensure labels and axis have room
                            dailyBarChart.setExtraOffsets(Utils.convertDpToPixel(4f), Utils.convertDpToPixel(12f), Utils.convertDpToPixel(4f), Utils.convertDpToPixel(20f));
                            // Make the daily chart non-scrolling and fit all bars into view
                            dailyBarChart.setDragEnabled(false);
                            dailyBarChart.setScaleEnabled(false);
                            dailyBarChart.setTouchEnabled(false);
                            dailyBarChart.setFitBars(true);
                            int labelCount = Math.max(1, dailyBarLabels.size());
                            // Align axis min/max to bar centers so IndexAxisValueFormatter maps correctly
                            // Bars are at x = 0..(n-1); set min to -0.5 and max to n - 0.5 (fitBars may also adjust)
                            float axisMin = -0.5f;
                            float axisMax = Math.max(0f, labelCount - 0.5f);
                            dx.setLabelCount(labelCount, false);
                            dx.setAxisMinimum(axisMin);
                            dx.setAxisMaximum(axisMax);
                            // Ensure the chart shows the full range
                            dailyBarChart.setVisibleXRangeMaximum(labelCount);
                            dailyBarChart.moveViewToX(0f);
                            float radiusDp = 8f;
                            dailyBarChart.setRenderer(new RoundedBarChartRenderer(dailyBarChart, dailyBarChart.getAnimator(), dailyBarChart.getViewPortHandler(), Utils.convertDpToPixel(radiusDp)));
                            dailyBarChart.getLegend().setEnabled(false);
                            dailyBarChart.getDescription().setEnabled(false);
                            // Apply Montserrat semibold to title/labels where possible
                            setTypefaceIfAvailable(dx, R.font.montsemibold);
                            setTypefaceIfAvailable(dy, R.font.montsemibold);
                            dailyBarChart.invalidate();
                        }

                        // ... (rest of your daily forecast parsing for RecyclerView) ...
                        ArrayList<SpannableString> dailyItems = new ArrayList<>();
                        ArrayList<String> dailyTime = new ArrayList<>();
                        ArrayList<String> dailyIcon = new ArrayList<>();
                        ArrayList<String> dailyPrecipitation = new ArrayList<>();
                        ArrayList<String> dailyHumidity = new ArrayList<>();
                        ArrayList<String> dailyLottieAnimList = new ArrayList<>();
                        ArrayList<String> dailyDescList = new ArrayList<>();
                        Map<String, DailyForecastPair> forecastMap = new HashMap<>();
                        
                        // First pass: collect all data by period name
                        for (int i = 0; i < periods.length(); i++) {
                            JSONObject current = periods.getJSONObject(i);
                            String name = current.optString("name");
                            boolean isDaytime = current.getBoolean("isDaytime");
                            
                            // Use the full name as the key, not just the first word
                            DailyForecastPair pair;
                            if (!forecastMap.containsKey(name)) {
                                forecastMap.put(name, new DailyForecastPair());
                            }
                            pair = forecastMap.get(name);
                            double tempVal = current.optDouble("temperature", Double.NaN);
                            String temperature = Double.isNaN(tempVal) ? "--" : formatTemperature(tempVal, tempUnit);
                            String description = current.optString("shortForecast");
                            String precipitationProbability = String.valueOf(current.getJSONObject("probabilityOfPrecipitation").optInt("value", 1013));
                            String humidityValue = current.has("relativeHumidity") ? current.getJSONObject("relativeHumidity").optInt("value") + "%" : "N/A";
                            if (precipitationProbability.equals("1013")) {
                                precipitationProbability = "N/A";
                            }
                            // Use icon URL from API instead
                            String iconUrl = current.optString("icon", "");
                            // Use icon URL for lottie animation
                            assert pair != null;
                            pair.time = name.replace("This", "").trim();
                            if (name.contains("Afternoon")) {
                                pair.afternoonTemperature = temperature;
                                pair.afternoonIcon = iconUrl;
                                pair.afternoonLottieAnim = iconUrl;
                                pair.afternoonDescription = description;
                                pair.afternoonPrecipitation = precipitationProbability;
                                pair.afternoonHumidity = humidityValue;
                            } else if (name.contains("Tonight")) {
                                pair.tonightTemperature = temperature;
                                pair.tonightIcon = iconUrl;
                                pair.tonightLottieAnim = iconUrl;
                                pair.tonightDescription = description;
                                pair.tonightPrecipitation = precipitationProbability;
                                pair.tonightHumidity = humidityValue;
                            } else if (isDaytime && !name.contains("Night")) {
                                pair.dayTemperature = temperature;
                                pair.dayIcon = iconUrl;
                                pair.dayLottieAnim = iconUrl;
                                pair.dayDescription = description;
                                pair.dayPrecipitation = precipitationProbability;
                                pair.dayHumidity = humidityValue;
                            } else if (!isDaytime) {
                                pair.nightTemperature = temperature;
                                pair.nightIcon = iconUrl;
                                pair.nightLottieAnim = iconUrl;
                                pair.nightDescription = description;
                                pair.nightPrecipitation = precipitationProbability;
                                pair.nightHumidity = humidityValue;
                            }
                        }
                        
                        // Second pass: process and consolidate data
                        Set<String> processedPeriods = new HashSet<>();
                        LocalDate today = LocalDate.now();
                        
                        for (int i = 0; i < periods.length(); i++) {
                            JSONObject current = periods.getJSONObject(i);
                            String name = current.optString("name");
                            String periodKey = name;
                            DailyForecastPair pair = forecastMap.get(periodKey);
                            
                            if (pair != null && !processedPeriods.contains(periodKey)) {
                                SpannableString coloredTemperature = new SpannableString("");
                                String descriptionText = "";
                                String precipitationText = "";
                                String humidityText = "";
                                String primaryIcon = "";
                                String primaryLottieAnim = "";
                                boolean isDaytime = current.getBoolean("isDaytime");
                                String description = current.optString("shortForecast");
                                String icon;
                                String lottieAnim;
                                
                                // Use icon URL from API instead
                                String iconUrl = current.optString("icon", "");
                                icon = iconUrl;
                                lottieAnim = iconUrl;
                                
                                // Check if this is today's period
                                boolean isToday = name.contains("This") || name.contains("Today");
                                
                                if (name.contains("Afternoon")) {
                                    coloredTemperature = new SpannableString(pair.afternoonTemperature != null ? pair.afternoonTemperature : "");
                                    if (pair.afternoonTemperature != null) coloredTemperature.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.df_high)), 0, pair.afternoonTemperature.length(), 0);
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
                                    processedPeriods.add(periodKey);
                                } else if (name.contains("Tonight")) {
                                    coloredTemperature = new SpannableString(pair.tonightTemperature != null ? pair.tonightTemperature : "");
                                    if (pair.tonightTemperature != null) coloredTemperature.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.df_low)), 0, pair.tonightTemperature.length(), 0);
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
                                    processedPeriods.add(periodKey);
                                } else {
                                    // For regular day/night periods, consolidate unless it's today
                                    String tempText = "";
                                    if (pair.dayTemperature != null) {
                                        tempText += pair.dayTemperature;
                                    }
                                    if (pair.nightTemperature != null) {
                                        tempText += (pair.dayTemperature != null ? " / " : "") + pair.nightTemperature;
                                    }
                                    coloredTemperature = new SpannableString(tempText);
                                    if (pair.dayTemperature != null) {
                                        coloredTemperature.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.df_high)), 0, pair.dayTemperature.length(), 0);
                                    }
                                    if (pair.nightTemperature != null) {
                                        int nightStart = tempText.indexOf(pair.nightTemperature);
                                        if (nightStart != -1) {
                                            coloredTemperature.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.df_low)), nightStart, nightStart + pair.nightTemperature.length(), 0);
                                        }
                                    }
                                    descriptionText = (pair.dayDescription != null ? pair.dayDescription : "") + (pair.nightDescription != null && pair.dayDescription != null && !pair.dayDescription.equals(pair.nightDescription) ? " / " + pair.nightDescription : (pair.nightDescription != null ? pair.nightDescription : ""));
                                    precipitationText = (pair.dayPrecipitation != null ? "Day: " + pair.dayPrecipitation : "") + (pair.nightPrecipitation != null ? " / Night: " + pair.nightPrecipitation : "");
                                    humidityText = (pair.dayHumidity != null ? "Day: " + pair.dayHumidity : "") + (pair.nightHumidity != null ? " / Night: " + pair.nightHumidity : "");
                                    primaryIcon = pair.dayIcon != null ? pair.dayIcon : pair.nightIcon;
                                    primaryLottieAnim = pair.dayLottieAnim != null ? pair.dayLottieAnim : pair.nightLottieAnim;
                                    dailyItems.add(coloredTemperature);
                                    dailyTime.add(name.replace("This", "").trim());
                                    dailyIcon.add(primaryIcon);
                                    dailyPrecipitation.add(precipitationText);
                                    dailyHumidity.add(humidityText);
                                    dailyLottieAnimList.add(primaryLottieAnim);
                                    dailyDescList.add(descriptionText);
                                    processedPeriods.add(periodKey);
                                }
                            }
                        }
                        if (periods.length() >= 2) {
                            JSONObject firstPeriod = periods.getJSONObject(0);
                            JSONObject secondPeriod = periods.getJSONObject(1);
                            boolean isDaytime = firstPeriod.getBoolean("isDaytime");
                            String highTemp = "";
                            String lowTemp = "";
                            if (isDaytime) {
                                highTemp = formatTemperature(firstPeriod.getDouble("temperature"), tempUnit);
                                lowTemp = formatTemperature(secondPeriod.getDouble("temperature"), tempUnit);
                            } else {
                                lowTemp = formatTemperature(firstPeriod.getDouble("temperature"), tempUnit);
                                highTemp = "--";
                            }
                            weatherViewModel.setHighTemperature(highTemp);
                            weatherViewModel.setLowTemperature(lowTemp);
                            String desc = firstPeriod.optString("shortForecast", "");
                            weatherViewModel.setDescription(desc);
                            hideLoading();
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
                headers.put("Cache-Control", "no-cache");
                headers.put("Pragma", "no-cache");
                headers.put("Expires", "0");
                Log.d(TAG, "Request headers: " + headers);
                return headers;
            }
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                if (response != null && response.headers != null) {
                    Log.d(TAG, "Response headers: " + response.headers);
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
                            String temperature = Double.isNaN(tempVal) ? "--" : formatTemperature(tempVal, tempUnit);
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

                            // Set the top animation to match the current hour's icon
                            if (animationViewForecast != null) {
                                String animationName = extractAnimationNameFromIcon(currentIconUrl);
                                int animationResId = getResources().getIdentifier(animationName, "raw", getContext().getPackageName());
                                if (animationResId == 0) {
                                    Log.w(TAG, "Missing animation for: " + animationName + ", falling back to not_available");
                                    animationResId = getResources().getIdentifier("not_available", "raw", getContext().getPackageName());
                                }
                                if (animationResId != 0) {
                                    animationViewForecast.setVisibility(View.VISIBLE);
                                    animationViewForecast.setAnimation(animationResId);
                                    animationViewForecast.loop(true);
                                    animationViewForecast.playAnimation();
                                } else {
                                    Log.e(TAG, "Even not_available animation not found, hiding animation view");
                                    animationViewForecast.setVisibility(View.GONE);
                                }
                            }
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
                            // Explicitly clear old GraphView references removed; no-op
                        // GraphView LineGraphSeries removed; we build BarEntries for MPAndroidChart instead
                        // Prepare MPAndroidChart data structures
                        java.util.ArrayList<BarEntry> barEntries = new java.util.ArrayList<>();
                        java.util.ArrayList<String> barLabels = new java.util.ArrayList<>();
                        for (int i = 0; i < periods.length() && i < 48; i++) {
                            JSONObject current = periods.getJSONObject(i);
                            String temperatureStr = current.optString("temperature");
                            double tempVal = Double.NaN;
                            try { tempVal = Double.parseDouble(temperatureStr); } catch (Exception ignore) {}
                            String formattedTemp = Double.isNaN(tempVal) ? "--" : formatTemperature(tempVal, tempUnit);
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
                            // For graphing, use converted tempVal
                            DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            Date date = java.sql.Timestamp.valueOf(startTime.format(timestampFormatter));
                            double tempValConverted = convertTemperatureForGraph(tempVal, tempUnit);
                            Log.d(TAG, "Graph Point - Time: " + date + ", Temp: " + tempValConverted);  // Log data point
                            if (!Double.isNaN(tempValConverted)) {
                                // Add bar entry and label for MPAndroidChart
                                barEntries.add(new BarEntry(i, (float) tempValConverted));
                                // Use the same displayTime as label
                                barLabels.add(displayTime);
                            }
                        }
                        // GraphView data removed: not storing hourly LineGraphSeries in ViewModel
                        // Configure BarChart using the collected entries and labels
                        if (hourlyBarChart != null) {
                            BarDataSet barDataSet = new BarDataSet(barEntries, "Temperature");
                            int colorOnSurface = ContextCompat.getColor(requireContext(), android.R.color.white);
                            int barColor = ContextCompat.getColor(requireContext(), R.color.chart_bar);
                            if (getContext() != null) {
                                TypedValue typedValue = new TypedValue();
                                getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
                                colorOnSurface = typedValue.data;
                            }
                            barDataSet.setColor(barColor);
                            // We draw values ourselves in the custom renderer to have full control
                            // Disable dataset value drawing to avoid duplicate/overlapping draws
                            barDataSet.setDrawValues(false);
                            barDataSet.setValueTextSize(14f);
                            try {
                                android.graphics.Typeface _tf = ResourcesCompat.getFont(getContext(), R.font.montsemibold);
                                if (_tf != null) barDataSet.setValueTypeface(_tf);
                            } catch (Exception ignored) {}

                            // Make bars thinner and gaps wider by reducing bar width
                            BarData barData = new BarData(barDataSet);
                            // A smaller bar width (e.g., 0.5f) will make bars thinner and gaps wider
                            barData.setBarWidth(0.5f);
                            hourlyBarChart.setData(barData);

                            // X axis labels
                            XAxis xAxis = hourlyBarChart.getXAxis();
                            xAxis.setGranularity(1f);
                            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                            xAxis.setDrawGridLines(false);
                            xAxis.setLabelRotationAngle(-45f);
                            xAxis.setValueFormatter(new IndexAxisValueFormatter(barLabels));
                            xAxis.setTextColor(colorOnSurface);
                            // Make axis labels larger and apply Montserrat semibold if available
                            xAxis.setTextSize(12f);
                            setTypefaceIfAvailable(xAxis, R.font.montsemibold);
                            // Provide extra offset and Y offset so bottom labels are not clipped
                            xAxis.setYOffset(Utils.convertDpToPixel(6f));
                            float extraLeft = Utils.convertDpToPixel(4f);
                            float extraTop = Utils.convertDpToPixel(12f);
                            float extraRight = Utils.convertDpToPixel(4f);
                            // increase bottom padding to avoid clipping of slanted labels during initial layout
                            float extraBottom = Utils.convertDpToPixel(18f);
                            hourlyBarChart.setExtraOffsets(extraLeft, extraTop, extraRight, extraBottom);

                            // ensure chart reserves minimum offset for labels
                            hourlyBarChart.setMinOffset(Utils.convertDpToPixel(12f));

                            // Y axis styling
                            YAxis leftAxis = hourlyBarChart.getAxisLeft();
                            leftAxis.setDrawGridLines(false);
                            leftAxis.setTextColor(colorOnSurface);
                            leftAxis.setTextSize(12f);
                            setTypefaceIfAvailable(leftAxis, R.font.montsemibold);
                            leftAxis.setValueFormatter(new ValueFormatter() {
                                @Override
                                public String getFormattedValue(float value) {
                                    return Math.round(Double.parseDouble(super.getFormattedValue(value))) + "º";
                                }
                            });
                            // compute min and max Y from entries and set axis limits with padding so labels can be drawn above bars
                            if (!barEntries.isEmpty()) {
                                float minY = Float.MAX_VALUE;
                                float maxY = -Float.MAX_VALUE;
                                for (BarEntry e : barEntries) {
                                    if (e.getY() < minY) minY = e.getY();
                                    if (e.getY() > maxY) maxY = e.getY();
                                }
                                float range = Math.max(1f, maxY - minY);
                                // pad bottom by 5 units and top by either 5 units or 10% of range, whichever larger
                                float bottomPad = 3f;
                                float topPad = Math.max(5f, range * 0.10f);
                                leftAxis.setAxisMinimum(minY - bottomPad);
                                leftAxis.setAxisMaximum(maxY + topPad);
                            } else {
                                leftAxis.setAxisMinimum(0f);
                                leftAxis.setAxisMaximum(10f);
                            }

                            hourlyBarChart.getAxisRight().setEnabled(false);

                            // make labels and chart interactive for horizontal scrolling
                            hourlyBarChart.setDragEnabled(true);
                            hourlyBarChart.setScaleEnabled(false);
                            // show a window of ~6 items so user can scroll horizontally
                            hourlyBarChart.setVisibleXRangeMaximum(6f);
                            // ensure bars have space and view starts at 0
                            hourlyBarChart.moveViewToX(-1f);

                            // Apply rounded corners renderer
                            float radiusDp = 16f; // adjust as needed
                            float radiusPx = Utils.convertDpToPixel(radiusDp);
                            hourlyBarChart.setRenderer(new RoundedBarChartRenderer(hourlyBarChart, hourlyBarChart.getAnimator(), hourlyBarChart.getViewPortHandler(), radiusPx));

                            hourlyBarChart.getLegend().setEnabled(false);
                            hourlyBarChart.getDescription().setEnabled(false);
                            // Invalidate after layout pass to ensure label bounds are measured (prevents clipped labels that snap into place)
                            hourlyBarChart.post(() -> {
                                // cancel any pending animation phases that could move labels
                                try {
                                    // Ensure animator is at final phase so labels/positions don't shift after layout
                                    hourlyBarChart.getAnimator().setPhaseX(1f);
                                    hourlyBarChart.getAnimator().setPhaseY(1f);
                                } catch (Exception ignored) {}
                                hourlyBarChart.invalidate();
                            });
                        }
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
                        hideLoading();
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
                headers.put("Cache-Control", "no-cache");
                headers.put("Pragma", "no-cache");
                headers.put("Expires", "0");
                Log.d(TAG, "Request headers: " + headers);
                return headers;
            }
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                if (response != null && response.headers != null) {
                    Log.d(TAG, "Response headers: " + response.headers);
                }
                return super.parseNetworkResponse(response);
            }
        };
        forecastHourlyRequest.setShouldCache(false);
        requestQueue.getCache().clear();
        requestQueue.add(forecastHourlyRequest);
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
    private String formatTemperature(double temp, String unit) {
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
        // Always round to whole numbers for a consumer-focused display
        return Math.round(displayTemp) + unitLabel;
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
                    unitLabel = "m/s"; break;
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

        // Attempt to extract a 5-digit US zipcode from the geocoded location string.
        // If found, save it into the shared preferences used by the snow-day widget
        // and trigger an update of the widget if it is already initialized.
        try {
            if (location != null) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d{5})");
                java.util.regex.Matcher m = p.matcher(location);
                if (m.find()) {
                    String zip = m.group(1);
                    if (sunwisePrefs != null) {
                        String oldZip = sunwisePrefs.getString("zipcode", "");
                        if (oldZip == null || !oldZip.equals(zip)) {
                            SharedPreferences.Editor editor = sunwisePrefs.edit();
                            editor.putString("zipcode", zip);
                            editor.apply();
                        }
                    }
                    // If the snow-day widget has been set up, auto-run the calculation
                    // using the freshly obtained zipcode (use PUBLIC school type and 1 day by default).
                    if (isAdded() && snowDayWidgetPrediction != null) {
                        try {
                            new SnowDayAsyncTask(zip, 1, com.venomdevelopment.sunwise.SnowDayCalculator.SchoolType.PUBLIC).execute();
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private String getCurrentZipcode() {
        // Prefer stored zipcode in preferences
        if (sunwisePrefs != null) {
            String z = sunwisePrefs.getString("zipcode", "");
            if (z != null && !z.isEmpty()) return z;
        }
        // Fallback: try to extract a 5-digit zipcode from the location display text
        if (locationDisplay != null) {
            try {
                String text = locationDisplay.getText().toString();
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d{5})");
                java.util.regex.Matcher m = p.matcher(text);
                if (m.find()) return m.group(1);
            } catch (Exception ignored) {}
        }
        return "";
    }

    private void setTypefaceIfAvailable(AxisBase axis, int fontResId) {
        if (!isAdded() || getContext() == null) return;
        try {
            android.graphics.Typeface tf = ResourcesCompat.getFont(getContext(), fontResId);
            if (tf != null) axis.setTypeface(tf);
        } catch (Exception e) {
            // ignore if font not available
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel any pending reload attempts
        reloadHandler.removeCallbacksAndMessages(null);
        // Pause AdView
        if (forecastAdView != null) {
            forecastAdView.pause();
        }
    }

    // Custom renderer to draw rounded bars
    public static class RoundedBarChartRenderer extends BarChartRenderer {
        private final float mRadius;

        public RoundedBarChartRenderer(BarChart chart, ChartAnimator animator, ViewPortHandler viewPortHandler, float radiusPx) {
            super(chart, animator, viewPortHandler);
            this.mRadius = radiusPx;
        }

        @Override
        public void drawData(Canvas c) {
            super.drawData(c);
            // The default drawing is done in drawData; we override drawDataSet below.
        }

        @Override
        protected void drawDataSet(Canvas c, IBarDataSet dataSet, int index) {
            mBarBorderPaint.setColor(dataSet.getBarBorderColor());

            final boolean drawBorder = dataSet.getBarBorderWidth() > 0.f;

            RectF barRect = new RectF();
            Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

            // Determine axis minimum for the base of bars (approximate using BarData min minus padding)
            float axisMin = 0f;
            if (mChart.getBarData() != null) {
                axisMin = mChart.getBarData().getYMin() - 5f;
            }

            float barWidth = 0.5f; // default fallback
            if (mChart.getBarData() != null) {
                barWidth = mChart.getBarData().getBarWidth();
            }
            float halfBar = barWidth / 2f;

            for (int i = 0; i < dataSet.getEntryCount(); i++) {
                BarEntry entry = (BarEntry) dataSet.getEntryForIndex(i);
                if (entry == null) continue;

                float x = entry.getX();

                // If this entry contains stacked values, draw each stack segment separately
                if (entry.getYVals() != null) {
                    float[] vals = entry.getYVals();
                    float pos = 0f;
                    for (int k = 0; k < vals.length; k++) {
                        float start = pos;
                        pos += vals[k];
                        float end = pos;

                        // Build rect for this stack segment in value coordinates: left, top(end), right, bottom(start)
                        barRect.left = x - halfBar;
                        barRect.right = x + halfBar;
                        barRect.top = end;
                        barRect.bottom = start;

                        try {
                            trans.rectValueToPixel(barRect);
                        } catch (Exception ex) {
                            continue;
                        }

                        int color = dataSet.getColor(k);
                        mRenderPaint.setColor(color);
                        // Draw per-segment with rounded corners on outer edges only
                        android.graphics.Path segmentPath = new android.graphics.Path();
                        float[] radii = new float[8];
                        // if only one segment, round all corners
                        if (vals.length == 1) {
                            for (int r = 0; r < 8; r++) radii[r] = mRadius;
                        } else {
                            // lower segment (k==0): round bottom corners
                            if (k == 0) {
                                // top-left, top-right = 0; bottom-right, bottom-left = mRadius
                                radii = new float[]{0f,0f, 0f,0f, mRadius,mRadius, mRadius,mRadius};
                            } else if (k == vals.length - 1) {
                                // upper segment: round top corners
                                radii = new float[]{mRadius,mRadius, mRadius,mRadius, 0f,0f, 0f,0f};
                            } else {
                                // middle segment: no rounding
                                radii = new float[]{0f,0f, 0f,0f, 0f,0f, 0f,0f};
                            }
                        }
                        segmentPath.addRoundRect(barRect, radii, android.graphics.Path.Direction.CW);
                        c.drawPath(segmentPath, mRenderPaint);
                        if (drawBorder) {
                            c.drawPath(segmentPath, mBarBorderPaint);
                        }
                    }
                } else {
                    float y = entry.getY();

                    // Build rect in value coordinates: left, top, right, bottom
                    // top should be the higher value (y) and bottom the axis minimum
                    barRect.left = x - halfBar;
                    barRect.right = x + halfBar;
                    barRect.top = y;
                    barRect.bottom = axisMin;

                    // convert to pixels
                    try {
                        trans.rectValueToPixel(barRect);
                    } catch (Exception ex) {
                        continue; // skip if conversion fails
                    }

                    int color = dataSet.getColor(i);
                    mRenderPaint.setColor(color);
                    // single (non-stacked) bar: round all corners
                    android.graphics.Path singlePath = new android.graphics.Path();
                    float[] radiiAll = new float[]{mRadius,mRadius, mRadius,mRadius, mRadius,mRadius, mRadius,mRadius};
                    singlePath.addRoundRect(barRect, radiiAll, android.graphics.Path.Direction.CW);
                    c.drawPath(singlePath, mRenderPaint);
                    if (drawBorder) {
                        c.drawPath(singlePath, mBarBorderPaint);
                    }
                }
            }
        }

        @Override
        public void drawValues(Canvas c) {
            // Draw values above each rounded bar using safe per-entry bounds
            if (mChart.getData() == null) return;
            Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            // Resolve a theme-aware label color (prefer colorOnSurface) so labels adapt to light/dark mode
            int labelColor = Color.WHITE;
            try {
                android.content.Context ctx = ((android.view.View) mChart).getContext();
                TypedValue tv = new TypedValue();
                if (ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv, true)) {
                    labelColor = tv.data;
                } else {
                    labelColor = ContextCompat.getColor(ctx, android.R.color.white);
                }
            } catch (Exception ignored) {}
            valuePaint.setColor(labelColor);
            valuePaint.setTextAlign(Paint.Align.CENTER);
            // Larger and bolder top labels for readability
            valuePaint.setTextSize(Utils.convertDpToPixel(14f));
            valuePaint.setFakeBoldText(true);
            // Try to apply Montserrat semibold for value labels
            try {
                android.graphics.Typeface _chartTf = ResourcesCompat.getFont(((android.view.View) mChart).getContext(), R.font.montsemibold);
                if (_chartTf != null) valuePaint.setTypeface(_chartTf);
            } catch (Exception ignored) {}
            // subtle shadow for contrast
            valuePaint.setShadowLayer(Utils.convertDpToPixel(2f), 0f, Utils.convertDpToPixel(1f), Color.argb(120, 0, 0, 0));

            for (int di = 0; di < mChart.getBarData().getDataSetCount(); di++) {
                IBarDataSet dataSet = mChart.getBarData().getDataSetByIndex(di);
                if (dataSet == null) continue;

                Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());
                RectF rect = new RectF();

                // Simple collision-avoidance: track right edge of last drawn top/low labels
                float lastTopLabelRight = Float.NEGATIVE_INFINITY;
                float lastLowLabelRight = Float.NEGATIVE_INFINITY;
                float minLabelSpacing = Utils.convertDpToPixel(6f);

                for (int j = 0; j < dataSet.getEntryCount(); j++) {
                    BarEntry entry = (BarEntry) dataSet.getEntryForIndex(j);
                    if (entry == null) continue;

                    float x = entry.getX();
                    float barWidth = mChart.getBarData() != null ? mChart.getBarData().getBarWidth() : 0.5f;
                    float halfBar = barWidth / 2f;

                    // If stacked, draw low label inside lower segment and high label above top
                    if (entry.getYVals() != null) {
                        float[] vals = entry.getYVals();
                        float pos = 0f;
                        float total = 0f;
                        for (float v : vals) total += v;

                        for (int k = 0; k < vals.length; k++) {
                            float start = pos;
                            pos += vals[k];
                            float end = pos;

                            rect.left = x - halfBar;
                            rect.right = x + halfBar;
                            rect.top = end;
                            rect.bottom = start;

                            try {
                                trans.rectValueToPixel(rect);
                            } catch (Exception ex) {
                                continue;
                            }

                            // For the low segment (k==0), draw its temperature label inside the segment (centered)
                            if (k == 0) {
                                Paint lowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                // Use same theme-aware label color for low labels
                                int lowLabelColor = labelColor;
                                try {
                                    android.content.Context ctx2 = ((android.view.View) mChart).getContext();
                                    TypedValue tv2 = new TypedValue();
                                    if (ctx2.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv2, true)) {
                                        lowLabelColor = tv2.data;
                                    }
                                } catch (Exception ignored) {}
                                lowPaint.setColor(lowLabelColor);
                                lowPaint.setTextAlign(Paint.Align.CENTER);
                                lowPaint.setTextSize(Utils.convertDpToPixel(12f));
                                lowPaint.setFakeBoldText(true);
                                try {
                                    android.graphics.Typeface _tfLow = ResourcesCompat.getFont(((android.view.View) mChart).getContext(), R.font.montsemibold);
                                    if (_tfLow != null) lowPaint.setTypeface(_tfLow);
                                } catch (Exception ignored) {}
                                // Draw the low label just above the top of the low segment so it isn't hidden by the X axis
                                float textX = rect.centerX();
                                float textY = rect.top - Utils.convertDpToPixel(2f);
                                // If the computed Y would place the label outside the top of the chart area, fallback to center
                                if (textY < mViewPortHandler.contentTop()) {
                                    textY = (rect.top + rect.bottom) / 2f + (lowPaint.getTextSize() / 3f);
                                }
                                String lowLabel = String.valueOf((int) Math.round(start + vals[k]));
                                float lowLabelWidth = lowPaint.measureText(lowLabel);
                                float lowLeft = textX - (lowLabelWidth / 2f);
                                if (lowLeft > lastLowLabelRight + minLabelSpacing) {
                                    c.drawText(lowLabel, textX, textY, lowPaint);
                                    lastLowLabelRight = textX + (lowLabelWidth / 2f);
                                }
                            }

                            // For the top-most segment, draw the high label above the full bar
                            if (k == vals.length - 1) {
                                float textX = rect.centerX();
                                float textY = rect.top - Utils.convertDpToPixel(4f);
                                String highLabel = String.valueOf((int) Math.round(total));
                                float highLabelWidth = valuePaint.measureText(highLabel);
                                float highLeft = textX - (highLabelWidth / 2f);
                                if (highLeft > lastTopLabelRight + minLabelSpacing) {
                                    c.drawText(highLabel, textX, textY, valuePaint);
                                    lastTopLabelRight = textX + (highLabelWidth / 2f);
                                }
                            }
                        }
                    } else {
                        float y = entry.getY();
                        rect.left = x - halfBar;
                        rect.right = x + halfBar;
                        rect.top = y;
                        rect.bottom = (mChart.getBarData() != null ? mChart.getBarData().getYMin() - 5f : 0f);

                        try {
                            trans.rectValueToPixel(rect);
                        } catch (Exception ex) {
                            continue;
                        }

                        // draw the text slightly above the top of the bar (avoid overlapping adjacent labels)
                        float textX = rect.centerX();
                        float textY = rect.top - Utils.convertDpToPixel(4f);
                        String label = String.valueOf((int) Math.round(y));
                        float labelWidth = valuePaint.measureText(label);
                        float labelLeft = textX - (labelWidth / 2f);
                        if (labelLeft > lastTopLabelRight + minLabelSpacing) {
                            c.drawText(label, textX, textY, valuePaint);
                            lastTopLabelRight = textX + (labelWidth / 2f);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (forecastAdView != null) {
            forecastAdView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (forecastAdView != null) {
            forecastAdView.resume();
        }
        // No-op: decimal-precision preference removed — temperatures are displayed rounded.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (forecastAdView != null) {
            forecastAdView.destroy();
        }
    }
}