package com.venomdevelopment.sunwise;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import java.time.OffsetDateTime;
import java.time.Duration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.graphics.Rect;
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
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.AdListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.time.LocalDate;
import android.util.TypedValue;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;

public class ForecastFragment extends Fragment {

    private static final String TAG = "ForecastFragment";
    private static final String BASE_URL_POINTS = "https://api.weather.gov/points/";
    private static final String USER_AGENT = "Sunwise/v1 (venomdevelopmentofficial@gmail.com)" + System.getProperty("http.agent");
    private LottieAnimationView animationViewForecast;
    private WeatherHourlyChart customHourlyChart;
    private RequestQueue requestQueue;
    private TextView currentTempTextForecast, highTempTextForecast, lowTempTextForecast, descTextForecast, humidityTextViewForecast, windTextViewForecast, precipitationTextViewForecast, dewpointTextViewForecast, locationDisplay;
    private Button saveLocationButton;
    private CircularProgressIndicator humidityProgress, precipitationProgress;
    private RecyclerView horizontalHourlyRecyclerView;
    private WeatherViewModel weatherViewModel;
    private LinearLayout progressBar;
    private String tempUnit = "us", windUnit = "mph";
    private boolean use24HourFormat;
    private SharedPreferences sunwisePrefs;
    private AdView forecastAdView;
    private final Handler reloadHandler = new Handler(Looper.getMainLooper());
    private FloatingActionButton reloadFab;
    private Boolean daytime = false;
    private RecyclerView customDailyBarBeta;
    private Float liveCurrentTemp = null;
    private VerticalBarForecastAdapter verticalDailyAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forecast, container, false);
        initViews(view);

        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        requestQueue = SunwiseApp.getInstance().getRequestQueue();
        sunwisePrefs = requireActivity().getSharedPreferences("SunwiseSettings", Context.MODE_PRIVATE);
        loadPreferences();
        setupRecyclerViews();
        setupAd();
        setupReloadFab();
        setupSaveButton(view);
        observeViewModel();

        Bundle args = getArguments();
        String location = (args != null && args.containsKey("location")) ? args.getString("location") : sunwisePrefs.getString("address", "");
        if (location != null && !location.isEmpty()) {
            updateLocationDisplay(location);
            startWeatherLoad(location);
        }

        return view;
    }

    private void initViews(View view) {
        animationViewForecast = view.findViewById(R.id.animation_view);
        currentTempTextForecast = view.findViewById(R.id.currentTempText);
        highTempTextForecast = view.findViewById(R.id.highTempText);
        lowTempTextForecast = view.findViewById(R.id.lowTempText);
        descTextForecast = view.findViewById(R.id.text_desc);
        humidityTextViewForecast = view.findViewById(R.id.humidity);
        windTextViewForecast = view.findViewById(R.id.wind);
        precipitationTextViewForecast = view.findViewById(R.id.precipitation);
        dewpointTextViewForecast = view.findViewById(R.id.dewpoint);
        humidityProgress = view.findViewById(R.id.humidityProgress);
        precipitationProgress = view.findViewById(R.id.precipitationProgress);
        locationDisplay = view.findViewById(R.id.locationDisplay);
        horizontalHourlyRecyclerView = view.findViewById(R.id.hourlyRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        forecastAdView = view.findViewById(R.id.forecast_ad);
        reloadFab = view.findViewById(R.id.reloadFab);
        customDailyBarBeta = view.findViewById(R.id.dailygraphbeta);
        customHourlyChart = view.findViewById(R.id.customHourlyChart);
        customDailyBarBeta.setClipToOutline(true);
        customDailyBarBeta.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
    }

    private void loadPreferences() {
        tempUnit = sunwisePrefs.getString("unit", "us");
        windUnit = sunwisePrefs.getString("wind_unit", "mph");
        use24HourFormat = sunwisePrefs.getBoolean("use_24_hour_format", false);
    }

    private void setupRecyclerViews() {
        horizontalHourlyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        customDailyBarBeta.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    }

    private void setupAd() {
        forecastAdView.loadAd(new AdRequest.Builder().build());
    }

    private void setupReloadFab() {
        reloadFab.setOnClickListener(v -> {

            String loc = (locationDisplay != null) ? locationDisplay.getText().toString() : "";
            if (loc.isEmpty() || loc.equals("Location")) {
                loc = sunwisePrefs.getString("address", "");
            }

            if (!loc.isEmpty()) {
                startWeatherLoad(loc);
            }
        });
    }

    private void setupSaveButton(View view) {
        saveLocationButton = view.findViewById(R.id.saveLocationButton);
        saveLocationButton.setOnClickListener(v -> {
            String loc = locationDisplay.getText().toString();
            if (!loc.isEmpty() && !loc.equals("Location") && !isLocationSaved(loc)) {
                if (saveLocationToList(loc)) {
                    Toast.makeText(getContext(), "Location saved!", Toast.LENGTH_SHORT).show();
                }
                updateSaveButtonState(loc);
            }
        });

        String initialLoc = (locationDisplay != null) ? locationDisplay.getText().toString() : "";
        updateSaveButtonState(initialLoc);
    }

    private void updateSaveButtonState(String loc) {
        if (saveLocationButton == null) return;
        if (loc == null || loc.isEmpty() || loc.equals("Location")) {
            saveLocationButton.setEnabled(false);
            saveLocationButton.setText("Save");
            saveLocationButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            saveLocationButton.setAlpha(0.5f);
            return;
        }

        boolean saved = isLocationSaved(loc);
        if (saved) {
            saveLocationButton.setEnabled(false);
            saveLocationButton.setText("Saved");
            saveLocationButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_check_circle_24, 0, 0, 0);
            saveLocationButton.setCompoundDrawablePadding(12);
            saveLocationButton.setAlpha(0.6f);
        } else {
            saveLocationButton.setEnabled(true);
            saveLocationButton.setText("Save");
            saveLocationButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            saveLocationButton.setAlpha(1f);
        }
    }

    private boolean isLocationSaved(String loc) {
        if (loc == null || loc.isEmpty() || loc.equals("Location")) return false;
        SharedPreferences sp = requireActivity().getSharedPreferences("addressPref", 0);
        Set<String> set = sp.getStringSet("saved_locations", new HashSet<>());
        return set != null && set.contains(loc);
    }

    private void observeViewModel() {
        weatherViewModel.getCurrentTemperature().observe(getViewLifecycleOwner(), t -> currentTempTextForecast.setText(t));
        weatherViewModel.getHighTemperature().observe(getViewLifecycleOwner(), t -> highTempTextForecast.setText(t));
        weatherViewModel.getLowTemperature().observe(getViewLifecycleOwner(), t -> lowTempTextForecast.setText(t));
        weatherViewModel.getDescription().observe(getViewLifecycleOwner(), d -> descTextForecast.setText(d));
        weatherViewModel.getHumidity().observe(getViewLifecycleOwner(), h -> humidityTextViewForecast.setText(h));
        weatherViewModel.getHumidityInt().observe(getViewLifecycleOwner(), h -> {
            if (humidityProgress != null) humidityProgress.setProgress(h, true);
        });
        weatherViewModel.getWind().observe(getViewLifecycleOwner(), w -> windTextViewForecast.setText(w));
        weatherViewModel.getPrecipitation().observe(getViewLifecycleOwner(), p -> precipitationTextViewForecast.setText(p));
        weatherViewModel.getPrecipitationInt().observe(getViewLifecycleOwner(), p -> {
            if (precipitationProgress != null) precipitationProgress.setProgress(p, true);
        });
        weatherViewModel.getDewpoint().observe(getViewLifecycleOwner(), d -> {
            if (dewpointTextViewForecast != null) dewpointTextViewForecast.setText(d);
        });
    }

    private void startWeatherLoad(String address) {
        showLoading();
        GeocodingRetryManager.geocodeWithRetry(requireContext(), address, USER_AGENT, result -> {
            if (isAdded()) {
                fetchWeatherData(BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude());
            }
        }, err -> {
            if (isAdded()) {
                hideLoading();
                Toast.makeText(getContext(), "Geocoding failed: " + err, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchWeatherData(String pointsUrl) {
        Log.d(TAG, "fetchWeatherData: " + pointsUrl);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, pointsUrl, null, response -> {
            try {
                JSONObject props = response.getJSONObject("properties");
                fetchDailyForecast(props.getString("forecast"));
                fetchHourlyAndGridData(props.getString("forecastHourly"), props.getString("forecastGridData"));
            } catch (JSONException e) {
                hideLoading();
            }
        }, error -> hideLoading()) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("User-Agent", USER_AGENT);
                h.put("Accept", "application/geo+json,application/json");
                return h;
            }
        };
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    private void fetchHourlyAndGridData(String hourlyUrl, String gridUrl) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, hourlyUrl, null, response -> {
            if (!isAdded()) return;
            try {
                JSONArray periods = response.getJSONObject("properties").getJSONArray("periods");
                fetchGridData(gridUrl, periods);
            } catch (JSONException e) { e.printStackTrace(); }
        }, err -> {}) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("User-Agent", USER_AGENT);
                return h;
            }
        };
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    private void fetchGridData(String gridUrl, JSONArray hourlyPeriods) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, gridUrl, null, response -> {
            if (!isAdded()) return;
            try {
                JSONObject props = response.getJSONObject("properties");

                JSONArray qpfValues = new JSONArray();
                if (props.has("quantitativePrecipitation")) {
                    qpfValues = props.getJSONObject("quantitativePrecipitation").getJSONArray("values");
                }

                JSONArray snowValues = new JSONArray();
                if (props.has("snowfallAmount")) {
                    snowValues = props.getJSONObject("snowfallAmount").getJSONArray("values");
                }

                updateHourlyUI(hourlyPeriods, qpfValues, snowValues);
            } catch (JSONException e) { e.printStackTrace(); }
        }, err -> {
            try { updateHourlyUI(hourlyPeriods, new JSONArray(), new JSONArray()); } catch (JSONException e) { e.printStackTrace(); }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("User-Agent", USER_AGENT);
                return h;
            }
        };
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    private void fetchDailyForecast(String url) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            if (!isAdded()) return;
            try {
                JSONArray periods = response.getJSONObject("properties").getJSONArray("periods");
                updateDailyUI(periods);
            } catch (JSONException e) { e.printStackTrace(); }
        }, err -> {}) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("User-Agent", USER_AGENT);
                return h;
            }
        };
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    private void updateDailyUI(JSONArray periods) throws JSONException {
        ArrayList<Float> highTemps = new ArrayList<>();
        ArrayList<Float> lowTemps = new ArrayList<>();
        ArrayList<String> times = new ArrayList<>();
        ArrayList<String> icons = new ArrayList<>();
        ArrayList<String> precips = new ArrayList<>();
        ArrayList<String> hums = new ArrayList<>();
        ArrayList<String> descs = new ArrayList<>();
        ArrayList<String> nightDescs = new ArrayList<>();
        ArrayList<String> nightPrecs = new ArrayList<>();
        ArrayList<String> nightHums = new ArrayList<>();
        ArrayList<Boolean> isDaytimes = new ArrayList<>();

        int i = 0;
        while (i < periods.length()) {
            JSONObject currentPeriod = periods.getJSONObject(i);
            boolean isCurrentDay = currentPeriod.getBoolean("isDaytime");
            float currentTemp = (float) convertTemperatureForGraph(currentPeriod.getDouble("temperature"), tempUnit);

            if (i == 0) {
                daytime = isCurrentDay;
                if (periods.length() >= 2) {
                    if (daytime) {
                        weatherViewModel.setHighTemperature(formatTemperature(currentPeriod.getDouble("temperature"), tempUnit));
                        weatherViewModel.setLowTemperature(formatTemperature(periods.getJSONObject(1).getDouble("temperature"), tempUnit));
                    } else {
                        weatherViewModel.setLowTemperature(formatTemperature(currentPeriod.getDouble("temperature"), tempUnit));
                        weatherViewModel.setHighTemperature("--");
                    }
                }
            }
            LocalDateTime startTime = LocalDateTime.parse(currentPeriod.getString("startTime"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            if (startTime.toLocalDate().equals(LocalDate.now())) {
                if(isCurrentDay) {
                    times.add("Today");
                } else {
                    times.add("Tonight");
                }
            } else {
                times.add(startTime.format(DateTimeFormatter.ofPattern("EEE")));
            }
            icons.add(currentPeriod.getString("icon"));
            precips.add(currentPeriod.getJSONObject("probabilityOfPrecipitation").optInt("value", 0) + "%");
            hums.add(currentPeriod.has("relativeHumidity") ? currentPeriod.getJSONObject("relativeHumidity").optInt("value") + "%" : "N/A");

            if (isCurrentDay) {
                descs.add(currentPeriod.getString("shortForecast"));
            } else {
                descs.add("---");
            }

            nightDescs.add("---");
            nightPrecs.add("--%");
            nightHums.add("N/A");
            isDaytimes.add(isCurrentDay);

            // Group High and Low pairs
            if (isCurrentDay) {
                highTemps.add(currentTemp);
                if (i + 1 < periods.length() && !periods.getJSONObject(i + 1).getBoolean("isDaytime")) {
                    JSONObject nightPeriod = periods.getJSONObject(i + 1);
                    float nightTemp = (float) convertTemperatureForGraph(nightPeriod.getDouble("temperature"), tempUnit);
                    lowTemps.add(nightTemp);
                    nightDescs.set(nightDescs.size() - 1, nightPeriod.getString("shortForecast"));
                    nightPrecs.set(nightPrecs.size() - 1, nightPeriod.getJSONObject("probabilityOfPrecipitation").optInt("value", 0) + "%");
                    nightHums.set(nightHums.size() - 1, nightPeriod.has("relativeHumidity") ? nightPeriod.getJSONObject("relativeHumidity").optInt("value") + "%" : "N/A");
                    i += 2;
                } else {
                    lowTemps.add(Float.NaN);
                    i += 1;
                }
            } else {
                highTemps.add(Float.NaN);
                lowTemps.add(currentTemp);
                nightDescs.set(nightDescs.size() - 1, currentPeriod.getString("shortForecast"));
                nightPrecs.set(nightPrecs.size() - 1, currentPeriod.getJSONObject("probabilityOfPrecipitation").optInt("value", 0) + "%");
                nightHums.set(nightHums.size() - 1, currentPeriod.has("relativeHumidity") ? currentPeriod.getJSONObject("relativeHumidity").optInt("value") + "%" : "N/A");
                precips.set(precips.size() - 1, "--%");
                hums.set(hums.size() - 1, "N/A");
                i += 1;
            }
        }
        verticalDailyAdapter = new VerticalBarForecastAdapter(
                getContext(), highTemps, lowTemps, times, icons, precips, hums, descs,
                nightDescs, nightPrecs, nightHums, isDaytimes, liveCurrentTemp
        );

        verticalDailyAdapter.setOnItemExpandListener(new VerticalBarForecastAdapter.OnItemExpandListener() {
            @Override
            public void onItemExpanded(int position) {
                scrollRecyclerViewToAlignEnd(customDailyBarBeta, position);
            }

            @Override
            public void onItemContracted(int position) {}
        });

        if (customDailyBarBeta != null) {
            customDailyBarBeta.setAdapter(verticalDailyAdapter);
        }

        hideLoading();
    }


    class QpfBlock {
        OffsetDateTime start;
        OffsetDateTime end;
        double valuePerHour;
    }

    private void updateHourlyUI(JSONArray periods, JSONArray qpfValues, JSONArray snowValues) throws JSONException {
        if (periods.length() == 0) return;
        JSONObject current = periods.getJSONObject(0);
        liveCurrentTemp = (float) convertTemperatureForGraph(current.getDouble("temperature"), tempUnit);

        if (verticalDailyAdapter != null) {
            verticalDailyAdapter.setCurrentTemp(liveCurrentTemp);
        }
        weatherViewModel.setCurrentTemperature(formatTemperature(current.getDouble("temperature"), tempUnit));
        weatherViewModel.setDescription(current.getString("shortForecast"));
        weatherViewModel.setWind(formatWind(current.getString("windSpeed"), current.optString("windDirection"), windUnit));

        int precip = current.getJSONObject("probabilityOfPrecipitation").optInt("value", 0);
        weatherViewModel.setPrecipitation(precip + "%");
        weatherViewModel.setPrecipitationInt(precip);

        int hum = current.has("relativeHumidity") ? current.getJSONObject("relativeHumidity").optInt("value") : 0;
        weatherViewModel.setHumidity(hum + "%");
        weatherViewModel.setHumidityInt(hum);

        if (current.has("dewpoint")) {
            try {
                JSONObject dp = current.getJSONObject("dewpoint");
                weatherViewModel.setDewpoint(formatDewpoint(dp.getDouble("value"), tempUnit));
            } catch (Exception ignored) {}
        } else {
            weatherViewModel.setDewpoint("--");
        }

        setDynamicBackgroundFromIcon(current.getString("icon"), current.getBoolean("isDaytime"));
        updateLottieAnimation(current.getString("icon"), current.getBoolean("isDaytime"));

        ArrayList<String> temps = new ArrayList<>(), times = new ArrayList<>(), icons = new ArrayList<>(), precips = new ArrayList<>(), hums = new ArrayList<>(), lotties = new ArrayList<>(), descs = new ArrayList<>();
        ArrayList<Boolean> isDaytimes = new ArrayList<>();

        DateTimeFormatter outFmt = use24HourFormat ? DateTimeFormatter.ofPattern("HH:00") : DateTimeFormatter.ofPattern("h:00 a");

        List<QpfBlock> qpfBlocks = new ArrayList<>();
        if (qpfValues != null) {
            for (int i = 0; i < qpfValues.length(); i++) {
                try {
                    JSONObject valObj = qpfValues.getJSONObject(i);
                    String[] parts = valObj.getString("validTime").split("/");
                    if (parts.length == 2) {
                        OffsetDateTime start = OffsetDateTime.parse(parts[0]);
                        long hours = Duration.parse(parts[1]).toHours();
                        if (hours <= 0) hours = 1;

                        OffsetDateTime end = start.plusHours(hours);
                        double totalVal = valObj.optDouble("value", 0.0);

                        QpfBlock block = new QpfBlock();
                        block.start = start;
                        block.end = end;
                        block.valuePerHour = totalVal / hours;
                        qpfBlocks.add(block);
                    }
                } catch (Exception ignored) {}
            }
        }

        List<QpfBlock> snowBlocks = new ArrayList<>();
        if (snowValues != null) {
            for (int i = 0; i < snowValues.length(); i++) {
                try {
                    JSONObject valObj = snowValues.getJSONObject(i);
                    String[] parts = valObj.getString("validTime").split("/");
                    if (parts.length == 2) {
                        OffsetDateTime start = OffsetDateTime.parse(parts[0]);
                        long hours = Duration.parse(parts[1]).toHours();
                        if (hours <= 0) hours = 1;

                        OffsetDateTime end = start.plusHours(hours);
                        double totalVal = valObj.optDouble("value", 0.0);

                        QpfBlock block = new QpfBlock();
                        block.start = start;
                        block.end = end;
                        block.valuePerHour = totalVal / hours;
                        snowBlocks.add(block);
                    }
                } catch (Exception ignored) {}
            }
        }

        for (int i = 0; i < periods.length(); i++) {
            JSONObject p = periods.getJSONObject(i);
            double val = p.getDouble("temperature");
            temps.add(formatTemperature(val, tempUnit));
            LocalDateTime startTime = LocalDateTime.parse(p.getString("startTime"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String timeLabel = (i == 0) ? "Now" : startTime.format(outFmt) + (startTime.toLocalDate().equals(LocalDate.now()) ? "" : " " + startTime.format(DateTimeFormatter.ofPattern("EEE")));
            times.add(timeLabel);
            icons.add(p.getString("icon"));
            precips.add(p.getJSONObject("probabilityOfPrecipitation").optInt("value", 0) + "%");
            hums.add(p.has("relativeHumidity") ? p.getJSONObject("relativeHumidity").optInt("value") + "%" : "N/A");
            lotties.add(p.getString("icon"));
            descs.add(p.getString("shortForecast"));
            isDaytimes.add(p.getBoolean("isDaytime"));
        }

        boolean isUsUnits = "us".equals(tempUnit);
        if (customHourlyChart != null) {
            customHourlyChart.setUseUsUnits(isUsUnits);
        }

        List<WeatherHourlyChart.WeatherPoint> customPoints = new ArrayList<>();
        for (int i = 0; i < periods.length(); i++) {
            JSONObject p = periods.getJSONObject(i);
            int val = (int) Math.round(convertTemperatureForGraph(p.getDouble("temperature"), tempUnit));
            int precipChance = p.getJSONObject("probabilityOfPrecipitation").optInt("value", 0);

            OffsetDateTime periodStart = OffsetDateTime.parse(p.getString("startTime"));

            double hourlyPrecipMm = 0.0;
            for (QpfBlock block : qpfBlocks) {
                if (!periodStart.isBefore(block.start) && periodStart.isBefore(block.end)) {
                    hourlyPrecipMm = block.valuePerHour;
                    break;
                }
            }

            double hourlySnowMm = 0.0;
            for (QpfBlock block : snowBlocks) {
                if (!periodStart.isBefore(block.start) && periodStart.isBefore(block.end)) {
                    hourlySnowMm = block.valuePerHour;
                    break;
                }
            }

            double precipAmount = isUsUnits ? (hourlyPrecipMm / 25.4) : hourlyPrecipMm;
            double snowAmount = isUsUnits ? (hourlySnowMm / 25.4) : hourlySnowMm;

            LocalDateTime startTimeLocal = periodStart.toLocalDateTime();
            String timeLabel = (i == 0) ? "Now" : startTimeLocal.format(outFmt);

            // Compute short date (e.g. Sep 23) if the period is not today
            String dateLabel = null;
            if (!startTimeLocal.toLocalDate().equals(LocalDate.now())) {
                dateLabel = startTimeLocal.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH));
            }

            String iconUrl = p.getString("icon");
            String animName = WeatherIconUtils.getAnimationResourceName(iconUrl, p.getBoolean("isDaytime"));
            int resId = getResources().getIdentifier(animName, "raw", requireContext().getPackageName());

            LottieDrawable lottieDrawable = new LottieDrawable();
            if (resId != 0) {
                LottieCompositionFactory.fromRawRes(requireContext(), resId).addListener(composition -> {
                    lottieDrawable.setComposition(composition);
                    lottieDrawable.setRepeatCount(LottieDrawable.INFINITE);
                    lottieDrawable.playAnimation();
                });
            }

            customPoints.add(new WeatherHourlyChart.WeatherPoint(timeLabel, dateLabel, val, precipChance, precipAmount, snowAmount, lottieDrawable));
        }

        if (customHourlyChart != null) {
            customHourlyChart.setPoints(customPoints);
        }

        HorizontalHourlyForecastAdapter hourlyAdapter = new HorizontalHourlyForecastAdapter(getContext(), temps, times, icons, precips, hums, lotties, descs, isDaytimes);
        hourlyAdapter.setOnItemExpandListener(new HorizontalHourlyForecastAdapter.OnItemExpandListener() {
            @Override
            public void onItemExpanded(int position) {
                scrollRecyclerViewToAlignEnd(horizontalHourlyRecyclerView, position);
            }

            @Override
            public void onItemContracted(int position) {
                // no-op: only expand behavior is adjusted
            }
        });
        horizontalHourlyRecyclerView.setAdapter(hourlyAdapter);
    }

    private void scrollRecyclerViewToAlignEnd(RecyclerView recyclerView, int position) {
        if (recyclerView == null || recyclerView.getLayoutManager() == null) return;

        recyclerView.post(() -> {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            View itemView = layoutManager.findViewByPosition(position);
            if (itemView == null) {
                recyclerView.scrollToPosition(position);
                recyclerView.post(() -> scrollRecyclerViewToAlignEnd(recyclerView, position));
                return;
            }

            int containerSize;
            int itemSize;
            if (layoutManager instanceof LinearLayoutManager
                    && ((LinearLayoutManager) layoutManager).getOrientation() == LinearLayoutManager.HORIZONTAL) {
                containerSize = recyclerView.getWidth();
                itemSize = itemView.getWidth();
            } else {
                containerSize = recyclerView.getHeight();
                itemSize = itemView.getHeight();
            }

            if (containerSize <= 0 || itemSize <= 0) return;

            int offset = containerSize - itemSize;
            if (offset > 0) {
                if (layoutManager instanceof LinearLayoutManager) {
                    ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, offset);
                }
            }
        });
    }

    private void updateLottieAnimation(String iconUrl, boolean isDaytime) {
        if (animationViewForecast == null) return;
        String name = WeatherIconUtils.getAnimationResourceName(iconUrl, isDaytime);
        int resId = getResources().getIdentifier(name, "raw", getContext().getPackageName());
        if (resId != 0) {
            animationViewForecast.setAnimation(resId);
            animationViewForecast.playAnimation();
        }
    }

    private double convertTemperatureForGraph(double temp, String unit) {
        return "us".equals(unit) ? temp : (temp - 32) * 5.0 / 9.0;
    }

    private String formatTemperature(double temp, String unit) {
        return Math.round(convertTemperatureForGraph(temp, unit)) + ("us".equals(unit) ? "°F" : "°C");
    }

    private String formatDewpoint(double dewpointCelsius, String unit) {
        double displayDewpoint = "us".equals(unit)
                ? dewpointCelsius * 9.0 / 5.0 + 32
                : dewpointCelsius;
        return Math.round(displayDewpoint) + ("us".equals(unit) ? "°F" : "°C");
    }

    private String formatWind(String speedStr, String direction, String unit) {
        if (speedStr == null || speedStr.isEmpty()) return "--";

        double speedVal;
        try {
            speedVal = Double.parseDouble(speedStr.replaceAll("[^\\d.]", ""));
        } catch (Exception e) {
            return speedStr;
        }

        double displaySpeed = speedVal;
        String unitDisplay = unit;

        switch (unit) {
            case "kmh":
                displaySpeed = speedVal * 1.60934;
                unitDisplay = "km/h";
                break;
            case "ms":
                displaySpeed = speedVal * 0.44704;
                unitDisplay = "m/s";
                break;
            case "mph":
            default:
                displaySpeed = speedVal;
                unitDisplay = "mph";
                break;
        }

        String result = Math.round(displaySpeed) + " " + unitDisplay;
        if (direction != null && !direction.isEmpty()) {
            result += " " + direction;
        }
        return result;
    }

    private boolean saveLocationToList(String loc) {
        if (loc == null || loc.isEmpty() || loc.equals("Location")) {
            return false;
        }
        SharedPreferences sp = requireActivity().getSharedPreferences("addressPref", 0);
        Set<String> set = new HashSet<>(sp.getStringSet("saved_locations", new HashSet<>()));
        if (set.contains(loc)) {
            return false;
        }
        set.add(loc);
        sp.edit().putStringSet("saved_locations", set).apply();
        return true;
    }

    private void updateLocationDisplay(String loc) {
        if (locationDisplay != null) {
            locationDisplay.setText(loc);
        }
        updateSaveButtonState(loc);
    }

    private void showLoading() { if (progressBar != null) progressBar.setVisibility(View.VISIBLE); }
    private void hideLoading() { if (progressBar != null) progressBar.setVisibility(View.GONE); }

    private void setDynamicBackgroundFromIcon(String iconUrl, boolean isDaytime) {
        if (getView() == null) return;
        int resId = WeatherIconUtils.getGradientResIdForIcon(iconUrl, isDaytime);
        Drawable background = ContextCompat.getDrawable(requireContext(), resId);
        if (background != null) {
            background = background.mutate();
        }

        getView().setBackground(background);

        if (getActivity() != null) {
            View rootView = getActivity().findViewById(R.id.drawer_layout);
            if (rootView != null) {
                rootView.setBackground(background != null ? background.getConstantState().newDrawable(getResources()) : null);
            }

            View toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setBackground(null);
                toolbar.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).resetWeatherUi();
        }
        super.onDestroyView();
    }

    @Override public void onPause() { super.onPause(); if (forecastAdView != null) forecastAdView.pause(); }
    @Override public void onResume() { super.onResume(); if (forecastAdView != null) forecastAdView.resume(); }
    @Override public void onDestroy() { super.onDestroy(); if (forecastAdView != null) forecastAdView.destroy(); }
}