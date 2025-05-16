package com.venomdevelopment.sunwise;

import static com.venomdevelopment.sunwise.GraphViewUtils.setLabelTypeface;
import static com.venomdevelopment.sunwise.GraphViewUtils.setTitleTypeface;

import android.content.SharedPreferences;
import android.graphics.Color;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import android.graphics.Paint;

public class ForecastFragment extends Fragment {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String BASE_URL_POINTS = "https://api.weather.gov/points/";
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search?q=";
    private static final String USER_AGENT = "Mozilla/5.0";
    private LottieAnimationView animationViewForecast;
    private RequestQueue requestQueue;
    private TextView tempTextForecast, descTextForecast, humidityTextViewForecast, windTextViewForecast, precipitationTextViewForecast;
    private EditText search;
    private Button searchButton;
    private RecyclerView dailyRecyclerView;
    private RecyclerView horizontalHourlyRecyclerView;
    private HorizontalHourlyForecastAdapter horizontalHourlyAdapter;
    private WeatherViewModel weatherViewModel;
    GraphView hourlyGraphViewForecast, dailyGraphViewForecast;

    public static final String myPref = "addressPref";

    public String getPreferenceValue() {
        SharedPreferences sp = getActivity().getSharedPreferences(myPref, 0);
        String str = sp.getString("address", "");
        return str;
    }

    public void writeToPreference(String thePreference) {
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(myPref, 0).edit();
        editor.putString("address", thePreference);
        editor.commit();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_forecast, container, false);
        tempTextForecast = v.findViewById(R.id.text_home);
        descTextForecast = v.findViewById(R.id.text_desc);
        search = v.findViewById(R.id.text_search);
        animationViewForecast = v.findViewById(R.id.animation_view);
        searchButton = v.findViewById(R.id.search);
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

        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(getContext());

        // Get the Shared ViewModel
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        // Observe LiveData for basic info
        weatherViewModel.getTemperature().observe(getViewLifecycleOwner(), tempTextForecast::setText);
        weatherViewModel.getDescription().observe(getViewLifecycleOwner(), descTextForecast::setText);
        weatherViewModel.getHumidity().observe(getViewLifecycleOwner(), humidityTextViewForecast::setText);
        weatherViewModel.getPrecipitation().observe(getViewLifecycleOwner(), precipitationTextViewForecast::setText);
        weatherViewModel.getWind().observe(getViewLifecycleOwner(), windTextViewForecast::setText);

        // Observe LiveData for graphs
        weatherViewModel.getHourlyGraphData().observe(getViewLifecycleOwner(), series -> {
            hourlyGraphViewForecast.removeAllSeries();
            series.setColor(Color.WHITE); // Set hourly line color to white
            hourlyGraphViewForecast.addSeries(series);
        });
        weatherViewModel.getDailyGraphDataDay().observe(getViewLifecycleOwner(), series -> {
            dailyGraphViewForecast.removeAllSeries();
            series.setColor(getResources().getColor(android.R.color.holo_red_light)); // Set daily day line color to red
            dailyGraphViewForecast.addSeries(series);
        });
        weatherViewModel.getDailyGraphDataNight().observe(getViewLifecycleOwner(), series -> {
            series.setColor(getResources().getColor(android.R.color.holo_blue_light)); // Set daily night line color to blue
            dailyGraphViewForecast.addSeries(series);
        });


        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = search.getText().toString().trim();
                if (!address.isEmpty()) {
                    fetchGeocodingData(address);
                    writeToPreference(address);
                } else {
                    Toast.makeText(getContext(), "Please enter an address", Toast.LENGTH_SHORT).show();
                }
            }
        });
        search.setText(getPreferenceValue(), TextView.BufferType.EDITABLE);
        searchButton.performClick();

        hourlyGraphViewForecast.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        dailyGraphViewForecast.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        hourlyGraphViewForecast.getViewport().setXAxisBoundsManual(true);
        dailyGraphViewForecast.getViewport().setXAxisBoundsManual(true);
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

    void fetchGeocodingData(String address) {
        // Encode the address for the URL
        String encodedAddress = address.replaceAll(" ", "+");
        String geocodeUrl = NOMINATIM_URL + encodedAddress + "&format=json&addressdetails=1";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, geocodeUrl, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            // Get the first result
                            JSONObject firstResult = response.getJSONObject(0);
                            String lat = firstResult.getString("lat");
                            String lon = firstResult.getString("lon");

                            // Build the points URL using the coordinates
                            String pointsUrl = BASE_URL_POINTS + lat + "," + lon;
                            fetchWeatherData(pointsUrl);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error parsing geocoding data", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching geocoding data: " + error.getMessage());
                        Toast.makeText(getContext(), "Error fetching geocoding data", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                return headers;
            }
        };

        requestQueue.add(jsonArrayRequest);
    }

    private void fetchWeatherData(String pointsUrl) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, pointsUrl, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
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
                return headers;
            }
        };
        requestQueue.add(jsonObjectRequest);
    }

    private void fetchDailyForecast(String forecastUrl) {
        JsonObjectRequest forecastRequest = new JsonObjectRequest(
                Request.Method.GET, forecastUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
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
                        daySeries.appendData(new DataPoint(i, dayPeriod.getDouble("temperature")), false, 7);
                        nightSeries.appendData(new DataPoint(i, nightPeriod.getDouble("temperature")), false, 7);
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
                        String temperature = current.optString("temperature") + "°";
                        String description = current.optString("shortForecast");
                        String precipitationProbability = String.valueOf(current.getJSONObject("probabilityOfPrecipitation").optInt("value", 1013));
                        String humidityValue = current.has("relativeHumidity") ? current.getJSONObject("relativeHumidity").optInt("value") + "%" : "N/A";
                        if (precipitationProbability.equals("1013")) {
                            precipitationProbability = "N/A";
                        }
                        String icon;
                        String lottieAnim;
                        String prefix = isDaytime ? "_day" : "_night";
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
                        weatherViewModel.setTemperature(temp);
                        weatherViewModel.setDescription(desc);
                        animationViewForecast.setAnimation(getResources().getIdentifier(dailyLottieAnimList.get(0), "raw", getContext().getPackageName()));
                        animationViewForecast.loop(true);
                        animationViewForecast.playAnimation();
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
                return headers;
            }
        };
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
        JsonObjectRequest forecastHourlyRequest = new JsonObjectRequest(
                Request.Method.GET, forecastHourlyUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (isAdded()) {
                    try {
                        JSONObject properties = response.getJSONObject("properties");
                        JSONArray periods = properties.getJSONArray("periods");
                        if (periods.length() > 0) {
                            JSONObject currentHour = periods.getJSONObject(0);
                            String temperature = currentHour.optString("temperature") + "°";
                            String shortForecast = currentHour.optString("shortForecast");
                            weatherViewModel.setTemperature(temperature);
                            weatherViewModel.setDescription(shortForecast);
                            String windspeed = currentHour.optString("windSpeed");
                            int humidity = currentHour.getJSONObject("relativeHumidity").getInt("value");
                            int precipitationProbability = currentHour.getJSONObject("probabilityOfPrecipitation").getInt("value");
                            weatherViewModel.setHumidity(humidity + "%");
                            weatherViewModel.setPrecipitation(precipitationProbability + "%");
                            weatherViewModel.setWind(windspeed);
                        }
                        ArrayList<String> hourlyItems = new ArrayList<>();
                        ArrayList<String> hourlyTime = new ArrayList<>();
                        ArrayList<String> hourlyIcon = new ArrayList<>();
                        ArrayList<String> hourlyPrecipitation = new ArrayList<>();
                        ArrayList<String> hourlyHumidity = new ArrayList<>();
                        ArrayList<String> hourlyLottieAnimList = new ArrayList<>();
                        ArrayList<String> hourlyDescList = new ArrayList<>();
                        DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("HH:00");
                        DateTimeFormatter dayOutputFormatter = DateTimeFormatter.ofPattern("EEE");
                        LocalDateTime now = LocalDateTime.now();
                        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
                        for (int i = 0; i < periods.length() && i < 24; i++) {
                            JSONObject current = periods.getJSONObject(i);
                            String temperature = current.optString("temperature") + "°";
                            String description = current.optString("shortForecast");
                            String precipitationProbability = String.valueOf(current.getJSONObject("probabilityOfPrecipitation").optInt("value", 1013));
                            String humidityValue = current.has("relativeHumidity") ? current.getJSONObject("relativeHumidity").optInt("value") + "%" : "N/A";
                            if (precipitationProbability.equals("1013")) {
                                precipitationProbability = "N/A";
                            }
                            String icon;
                            String lottieAnim;
                            boolean isDaytime = current.getBoolean("isDaytime");
                            String prefix = isDaytime ? "_day" : "_night";
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
                            hourlyItems.add(temperature);
                            hourlyTime.add(displayTime);
                            hourlyIcon.add(icon);
                            hourlyPrecipitation.add(precipitationProbability + "%");
                            hourlyHumidity.add(humidityValue);
                            hourlyLottieAnimList.add(lottieAnim);
                            hourlyDescList.add(description);
                            series.appendData(new DataPoint(i, Double.parseDouble(current.getString("temperature"))), false, 24);
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
                return headers;
            }
        };
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
}