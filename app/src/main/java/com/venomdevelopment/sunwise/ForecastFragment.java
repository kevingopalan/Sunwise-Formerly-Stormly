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
import android.widget.ImageView;
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
import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.renderer.LineChartRenderer;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.time.LocalDate;
import android.util.TypedValue;

public class ForecastFragment extends Fragment {

    private static final String TAG = "ForecastFragment";
    private static final String BASE_URL_POINTS = "https://api.weather.gov/points/";
    private static final String USER_AGENT = "Sunwise/v1 (venomdevelopmentofficial@gmail.com)" + System.getProperty("http.agent");
    private LottieAnimationView animationViewForecast;
    private RequestQueue requestQueue;
    private TextView currentTempTextForecast, highTempTextForecast, lowTempTextForecast, descTextForecast, humidityTextViewForecast, windTextViewForecast, precipitationTextViewForecast, dewpointTextViewForecast, locationDisplay;
    private Button saveLocationButton;
    private CircularProgressIndicator humidityProgress, precipitationProgress;
    private RecyclerView dailyRecyclerView, horizontalHourlyRecyclerView;
    private WeatherViewModel weatherViewModel;
    private LinearLayout progressBar;
    private String tempUnit = "us", windUnit = "mph";
    private boolean use24HourFormat;
    private SharedPreferences sunwisePrefs;
    private AdView forecastAdView;
    private final Handler reloadHandler = new Handler(Looper.getMainLooper());
    private FloatingActionButton reloadFab;
    private BarChart dailyBarChart;
    private LineChart hourlyBarChart;
    private Boolean daytime = false;

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
        dailyRecyclerView = view.findViewById(R.id.dailyRecyclerView);
        horizontalHourlyRecyclerView = view.findViewById(R.id.hourlyRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        forecastAdView = view.findViewById(R.id.forecast_ad);
        reloadFab = view.findViewById(R.id.reloadFab);
        hourlyBarChart = view.findViewById(R.id.hourlyBarGraph);
        dailyBarChart = view.findViewById(R.id.dailyBarGraph);
        initCharts();
    }

    private void initCharts() {
        if (hourlyBarChart != null) {
            hourlyBarChart.getDescription().setEnabled(false);
            hourlyBarChart.setDrawGridBackground(false);
            hourlyBarChart.setPinchZoom(false);
            hourlyBarChart.setTouchEnabled(true);
            hourlyBarChart.setDragEnabled(true);
            hourlyBarChart.setScaleYEnabled(false);
            hourlyBarChart.setScaleXEnabled(true);
            hourlyBarChart.getLegend().setEnabled(false);
        }
        if (dailyBarChart != null) {
            dailyBarChart.setDrawBarShadow(false);
            dailyBarChart.setDrawValueAboveBar(true);
            dailyBarChart.getDescription().setEnabled(false);
            dailyBarChart.setDrawGridBackground(false);
            dailyBarChart.setPinchZoom(false);
            dailyBarChart.setScaleEnabled(false);
            dailyBarChart.getLegend().setEnabled(false);
            dailyBarChart.setTouchEnabled(true);
        }
    }

    private void loadPreferences() {
        tempUnit = sunwisePrefs.getString("unit", "us");
        windUnit = sunwisePrefs.getString("wind_unit", "mph");
        use24HourFormat = sunwisePrefs.getBoolean("use_24_hour_format", false);
    }

    private void setupRecyclerViews() {
        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        horizontalHourlyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
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
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, pointsUrl, null, response -> {
            try {
                JSONObject props = response.getJSONObject("properties");
                fetchDailyForecast(props.getString("forecast"));
                fetchHourlyForecast(props.getString("forecastHourly"));
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
        ArrayList<Float> dayTemps = new ArrayList<>();
        ArrayList<Float> nightTemps = new ArrayList<>();

        int i = 0;
        while (i < periods.length()) {
            JSONObject currentPeriod = periods.getJSONObject(i);
            boolean isCurrentDay = currentPeriod.getBoolean("isDaytime");
            float currentTemp = (float) convertTemperatureForGraph(currentPeriod.getDouble("temperature"), tempUnit);

            if (i == 0) {
                daytime = isCurrentDay;
            }

            if (isCurrentDay) {
                dayTemps.add(currentTemp);
                if (i + 1 < periods.length() && !periods.getJSONObject(i + 1).getBoolean("isDaytime")) {
                    float nightTemp = (float) convertTemperatureForGraph(periods.getJSONObject(i + 1).getDouble("temperature"), tempUnit);
                    nightTemps.add(nightTemp);
                    i += 2;
                } else {
                    nightTemps.add(Float.NaN);
                    i += 1;
                }
            } else {
                dayTemps.add(Float.NaN);
                nightTemps.add(currentTemp);
                i += 1;
            }
        }

        setupDailyChart(dayTemps, nightTemps);

        ArrayList<SpannableString> items = new ArrayList<>();
        ArrayList<String> times = new ArrayList<>(), icons = new ArrayList<>(), precips = new ArrayList<>(), hums = new ArrayList<>(), lotties = new ArrayList<>(), descs = new ArrayList<>();

        for (int j = 0; j < periods.length(); j++) {
            JSONObject p = periods.getJSONObject(j);
            String name = p.getString("name");
            String temp = formatTemperature(p.getDouble("temperature"), tempUnit);
            SpannableString ss = new SpannableString(temp);
            int color = ContextCompat.getColor(requireContext(), p.getBoolean("isDaytime") ? R.color.df_high : R.color.df_low);
            ss.setSpan(new ForegroundColorSpan(color), 0, temp.length(), 0);

            items.add(ss);
            times.add(name.replace("This", "").trim());
            icons.add(p.getString("icon"));
            precips.add(p.getJSONObject("probabilityOfPrecipitation").optInt("value", 0) + "%");
            hums.add(p.has("relativeHumidity") ? p.getJSONObject("relativeHumidity").optInt("value") + "%" : "N/A");
            lotties.add(p.getString("icon"));
            descs.add(p.getString("shortForecast"));
        }

        if (periods.length() >= 2) {
            weatherViewModel.setHighTemperature(formatTemperature(periods.getJSONObject(0).getDouble("temperature"), tempUnit));
            weatherViewModel.setLowTemperature(formatTemperature(periods.getJSONObject(1).getDouble("temperature"), tempUnit));
        }

        dailyRecyclerView.setAdapter(new DailyForecastAdapter(getContext(), items, times, icons, precips, hums, lotties, descs));
        hideLoading();
    }

    private void fetchHourlyForecast(String url) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            if (!isAdded()) return;
            try {
                JSONArray periods = response.getJSONObject("properties").getJSONArray("periods");
                updateHourlyUI(periods);
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

    private void updateHourlyUI(JSONArray periods) throws JSONException {
        if (periods.length() == 0) return;
        JSONObject current = periods.getJSONObject(0);
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
                weatherViewModel.setDewpoint(Math.round(convertTemperatureForGraph(dp.getDouble("value"), tempUnit)) + "°");
            } catch (Exception ignored) {}
        } else {
            weatherViewModel.setDewpoint("--");
        }

        setDynamicBackgroundFromIcon(current.getString("icon"), current.getBoolean("isDaytime"));
        updateLottieAnimation(current.getString("icon"));

        ArrayList<String> temps = new ArrayList<>(), times = new ArrayList<>(), icons = new ArrayList<>(), precips = new ArrayList<>(), hums = new ArrayList<>(), lotties = new ArrayList<>(), descs = new ArrayList<>();
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        DateTimeFormatter outFmt = use24HourFormat ? DateTimeFormatter.ofPattern("HH:00") : DateTimeFormatter.ofPattern("h:00 a");

        for (int i = 0; i < Math.min(periods.length(), 48); i++) {
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

            entries.add(new BarEntry(i, (float) convertTemperatureForGraph(val, tempUnit)));
            labels.add(timeLabel);
        }
        setupHourlyChart(entries, labels);
        horizontalHourlyRecyclerView.setAdapter(new HorizontalHourlyForecastAdapter(getContext(), temps, times, icons, precips, hums, lotties, descs));
    }

    private void setupDailyChart(ArrayList<Float> days, ArrayList<Float> nights) {
        if (dailyBarChart == null) return;
        int colorOnSurface = getThemeColor(com.google.android.material.R.attr.colorOnSurface);
        Typeface tf = ResourcesCompat.getFont(requireContext(), R.font.montsemibold);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < days.size(); i++) {
            if (i >= nights.size()) break;

            Float dayVal = days.get(i);
            Float nightVal = nights.get(i);

            if (dayVal.isNaN() && !nightVal.isNaN()) {
                entries.add(new BarEntry(i, nightVal));
            } else if (!dayVal.isNaN() && nightVal.isNaN()) {
                entries.add(new BarEntry(i, dayVal));
            } else {
                float low = Math.min(dayVal, nightVal);
                float high = Math.max(dayVal, nightVal);
                entries.add(new BarEntry(i, new float[]{low, high - low}));
            }

            labels.add(LocalDate.now().plusDays(i).format(DateTimeFormatter.ofPattern("EEE")));
        }

        BarDataSet ds = new BarDataSet(entries, "Daily");
        ds.setColors(new int[]{
                ContextCompat.getColor(requireContext(), R.color.chart_low),
                ContextCompat.getColor(requireContext(), R.color.chart_high)
        });

        ds.setDrawValues(true);
        ds.setValueTextColor(colorOnSurface);
        ds.setValueTypeface(tf);
        ds.setValueTextSize(14f);

        ds.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry entry) {
                return Math.round(entry.getY()) + "°";
            }

            @Override
            public String getBarStackedLabel(float value, BarEntry entry) {
                float[] vals = entry.getYVals();
                if (vals == null) return "";

                if (value == vals[0]) {
                    return Math.round(vals[0]) + "°";
                }
                if (value == vals[vals.length - 1]) {
                    return Math.round(entry.getY()) + "°";
                }
                return "";
            }
        });

        dailyBarChart.setRenderer(new RoundedBarChartRenderer(dailyBarChart, dailyBarChart.getAnimator(), dailyBarChart.getViewPortHandler(), Utils.convertDpToPixel(8f)));
        BarData data = new BarData(ds);
        data.setBarWidth(0.8f);
        dailyBarChart.setData(data);
        dailyBarChart.setExtraBottomOffset(15f);

        XAxis x = dailyBarChart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setDrawGridLines(false);
        x.setTextColor(colorOnSurface);
        x.setTypeface(tf);
        x.setTextSize(14f);
        x.setYOffset(5f);

        YAxis y = dailyBarChart.getAxisLeft();
        y.setDrawGridLines(false);
        y.setTextColor(colorOnSurface);
        y.setTypeface(tf);
        y.setTextSize(14f);
        y.setSpaceTop(25f);

        dailyBarChart.getAxisRight().setEnabled(false);
        dailyBarChart.invalidate();
    }

    private void setupHourlyChart(ArrayList<Entry> entries, ArrayList<String> labels) {
        if (hourlyBarChart == null) return;
        int colorOnSurface = getThemeColor(com.google.android.material.R.attr.colorOnSurface);
        Typeface tf = ResourcesCompat.getFont(requireContext(), R.font.montsemibold);

        LineDataSet ds = new LineDataSet(entries, "Hourly");
        ds.setColor(ContextCompat.getColor(requireContext(), R.color.chart_bar));

        ds.setDrawValues(true);
        ds.setValueTextColor(colorOnSurface);
        ds.setDrawFilled(true);
        ds.setFillDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.linegraphgradient));
        ds.setValueTypeface(tf);
        ds.setValueTextSize(14f);
        ds.setCircleColor(getResources().getColor(R.color.md_theme_primary));
        ds.setCircleHoleColor(getResources().getColor(R.color.md_theme_primary));
        ds.setColor(getResources().getColor(R.color.md_theme_primary));
        ds.setLineWidth(4f);
        ds.setDrawCircleHole(false);
        ds.setDrawCircles(false);
        ds.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPointLabel(Entry entry) {
                if (entry.getX() % 2 != 0) {
                    return "";
                }
                return Math.round(entry.getY()) + "°";
            }
        });

        hourlyBarChart.setRenderer(new LineChartRenderer(hourlyBarChart, hourlyBarChart.getAnimator(), hourlyBarChart.getViewPortHandler()));
        LineData data = new LineData(ds);
        hourlyBarChart.setData(data);
        hourlyBarChart.setExtraBottomOffset(30f);

        XAxis x = hourlyBarChart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(4f);
        x.setTextColor(colorOnSurface);
        x.setTypeface(tf);
        x.setTextSize(12f);
        x.setDrawGridLines(false);
        x.setYOffset(10f);

        hourlyBarChart.setVisibleXRangeMaximum(12f);
        hourlyBarChart.moveViewToX(0f);
        hourlyBarChart.getAxisRight().setEnabled(false);
        hourlyBarChart.getAxisLeft().setEnabled(false);
        hourlyBarChart.getLegend().setEnabled(false);
        hourlyBarChart.invalidate();
    }

    private void updateLottieAnimation(String iconUrl) {
        if (animationViewForecast == null) return;
        String name = extractAnimationNameFromIcon(iconUrl);
        int resId = getResources().getIdentifier(name, "raw", getContext().getPackageName());
        if (resId != 0) {
            animationViewForecast.setAnimation(resId);
            animationViewForecast.playAnimation();
        }
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        if (getContext() != null && getContext().getTheme().resolveAttribute(attr, typedValue, true)) {
            return typedValue.data;
        }
        return Color.BLACK;
    }

    private double convertTemperatureForGraph(double temp, String unit) {
        return "us".equals(unit) ? temp : (temp - 32) * 5.0 / 9.0;
    }

    private String formatTemperature(double temp, String unit) {
        return Math.round(convertTemperatureForGraph(temp, unit)) + ("us".equals(unit) ? "°F" : "°C");
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

    private String extractAnimationNameFromIcon(String iconUrl) {
        if (iconUrl == null || iconUrl.isEmpty()) return "clear_day";
        if (iconUrl.contains("tsra")) return "lightning_bolt";
        if (iconUrl.contains("rain")) return "rain";
        if (iconUrl.contains("snow")) return "snow";
        if (iconUrl.contains("sct") || iconUrl.contains("few")) return "partly_cloudy_day";
        if (iconUrl.contains("ovc") || iconUrl.contains("bkn")) return "cloudy";
        return "clear_day";
    }

    private void setDynamicBackgroundFromIcon(String iconUrl, boolean isDaytime) {
        if (getView() == null) return;
        int resId = isDaytime ? R.drawable.gradient_clear_day : R.drawable.gradient_clear_night;
        if (iconUrl.contains("rain")) resId = isDaytime ? R.drawable.gradient_rain_day : R.drawable.gradient_rain_night;
        else if (iconUrl.contains("snow")) resId = isDaytime ? R.drawable.gradient_snow_day : R.drawable.gradient_snow_night;
        getView().setBackground(ContextCompat.getDrawable(requireContext(), resId));
    }

    @Override public void onPause() { super.onPause(); if (forecastAdView != null) forecastAdView.pause(); }
    @Override public void onResume() { super.onResume(); if (forecastAdView != null) forecastAdView.resume(); }
    @Override public void onDestroy() { super.onDestroy(); if (forecastAdView != null) forecastAdView.destroy(); }

    public static class RoundedBarChartRenderer extends BarChartRenderer {
        private final float mRadius;

        public RoundedBarChartRenderer(BarChart chart, ChartAnimator animator, ViewPortHandler vph, float radiusPx) {
            super(chart, animator, vph);
            this.mRadius = radiusPx;
        }

        @Override
        public void drawValues(Canvas c) {
            if (mChart.getBarData() == null) return;
            if (mBarBuffers == null || mBarBuffers.length != mChart.getBarData().getDataSetCount()) {
                initBuffers();
            }

            BarData barData = mChart.getBarData();
            List<IBarDataSet> dataSets = barData.getDataSets();
            float valueOffset = Utils.convertDpToPixel(5f);

            for (int i = 0; i < dataSets.size(); i++) {
                IBarDataSet dataSet = dataSets.get(i);
                if (!shouldDrawValues(dataSet)) continue;

                applyValueTextStyle(dataSet);
                BarBuffer buffer = mBarBuffers[i];
                buffer.setPhases(mAnimator.getPhaseX(), mAnimator.getPhaseY());
                buffer.setBarWidth(barData.getBarWidth());
                buffer.setInverted(mChart.isInverted(dataSet.getAxisDependency()));
                buffer.feed(dataSet);
                mChart.getTransformer(dataSet.getAxisDependency()).pointValuesToPixel(buffer.buffer);

                ValueFormatter formatter = dataSet.getValueFormatter();
                int bufferIndex = 0;

                for (int j = 0; j < dataSet.getEntryCount(); j++) {
                    BarEntry entry = (BarEntry) dataSet.getEntryForIndex(j);
                    float[] vals = entry.getYVals();

                    if (vals == null) {
                        float x = (buffer.buffer[bufferIndex] + buffer.buffer[bufferIndex + 2]) / 2f;
                        if (!mViewPortHandler.isInBoundsRight(x)) break;
                        if (!mViewPortHandler.isInBoundsLeft(x)) {
                            bufferIndex += 4;
                            continue;
                        }

                        float y = buffer.buffer[bufferIndex + 1];
                        String valText = formatter.getBarLabel(entry);
                        drawValue(c, valText, x, y - valueOffset, dataSet.getValueTextColor(j));
                        bufferIndex += 4;
                    } else {
                        for (int k = 0; k < vals.length; k++) {
                            float x = (buffer.buffer[bufferIndex] + buffer.buffer[bufferIndex + 2]) / 2f;
                            float y = buffer.buffer[bufferIndex + 1];
                            
                            if (!mViewPortHandler.isInBoundsRight(x)) break;
                            if (!mViewPortHandler.isInBoundsLeft(x)) {
                                bufferIndex += 4;
                                continue;
                            }

                            String valText = formatter.getBarStackedLabel(vals[k], entry);
                            if (valText != null && !valText.isEmpty()) {
                                float yPos;
                                if (k == 0) {
                                    yPos = y + Utils.convertDpToPixel(20f);
                                } else {
                                    yPos = y - valueOffset;
                                }
                                drawValue(c, valText, x, yPos, dataSet.getValueTextColor(j));
                            }
                            bufferIndex += 4;
                        }
                    }
                }
            }
        }

        @Override
        protected void drawDataSet(Canvas c, IBarDataSet dataSet, int index) {
            Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());
            mRenderPaint.setAntiAlias(true);

            BarBuffer buffer = mBarBuffers[index];
            buffer.setPhases(mAnimator.getPhaseX(), mAnimator.getPhaseY());
            buffer.setBarWidth(mChart.getBarData().getBarWidth());
            buffer.setInverted(mChart.isInverted(dataSet.getAxisDependency()));
            buffer.feed(dataSet);
            trans.pointValuesToPixel(buffer.buffer);

            final boolean isSingleColor = dataSet.getColors().size() == 1;

            for (int j = 0; j < buffer.size(); j += 4) {
                if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) continue;
                if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j])) break;

                if (!isSingleColor) {
                    mRenderPaint.setColor(dataSet.getColor(j / 4));
                } else {
                    mRenderPaint.setColor(dataSet.getColor());
                }

                float left = buffer.buffer[j];
                float top = buffer.buffer[j + 1];
                float right = buffer.buffer[j + 2];
                float bottom = buffer.buffer[j + 3];

                float gap = Utils.convertDpToPixel(1.5f);
                if (top < bottom) {
                    top += gap;
                    bottom -= gap;
                } else {
                    top -= gap;
                    bottom += gap;
                }

                c.drawRoundRect(left, top, right, bottom, mRadius, mRadius, mRenderPaint);
            }
        }
    }
}
