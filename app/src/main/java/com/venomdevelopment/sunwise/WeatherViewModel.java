package com.venomdevelopment.sunwise;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherViewModel extends ViewModel {
    private static final String TAG = "WeatherViewModel";
    private static final String BASE_URL_POINTS = "https://api.weather.gov/points/";
    private static final String USER_AGENT = "Sunwise/v1 (venomdevelopmentofficial@gmail.com)" + System.getProperty("http.agent");

    private final MutableLiveData<String> currentTemperature = new MutableLiveData<>();
    private final MutableLiveData<String> highTemperature = new MutableLiveData<>();
    private final MutableLiveData<String> lowTemperature = new MutableLiveData<>();
    private final MutableLiveData<String> description = new MutableLiveData<>();
    private final MutableLiveData<String> humidity = new MutableLiveData<>();
    private final MutableLiveData<Integer> humidityInt = new MutableLiveData<>(0);
    private final MutableLiveData<String> wind = new MutableLiveData<>();
    private final MutableLiveData<String> precipitation = new MutableLiveData<>();
    private final MutableLiveData<Integer> precipitationInt = new MutableLiveData<>(0);
    private final MutableLiveData<String> dewpoint = new MutableLiveData<>();

    private final Set<String> pendingRequests = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static class WeatherSummary {
        public String temperature;
        public String description;
        public String icon;
        public String wind;
        public String humidity;
        public String precipitation;
        public String dewpoint;
        public WeatherSummary(String temperature, String description, String icon, String wind, String humidity, String precipitation, String dewpoint) {
            this.temperature = temperature;
            this.description = description;
            this.icon = icon;
            this.wind = wind;
            this.humidity = humidity;
            this.precipitation = precipitation;
            this.dewpoint = dewpoint;
        }
    }

    private final MutableLiveData<Map<String, WeatherSummary>> locationWeatherMap = new MutableLiveData<>(new ConcurrentHashMap<>());
    
    public LiveData<Map<String, WeatherSummary>> getLocationWeatherMap() { return locationWeatherMap; }
    public LiveData<String> getCurrentTemperature() { return currentTemperature; }
    public void setCurrentTemperature(String temperature) { this.currentTemperature.setValue(temperature); }
    public LiveData<String> getHighTemperature() { return highTemperature; }
    public void setHighTemperature(String highTemperature) { this.highTemperature.setValue(highTemperature); }
    public LiveData<String> getLowTemperature() { return lowTemperature; }
    public void setLowTemperature(String lowTemperature) { this.lowTemperature.setValue(lowTemperature); }
    public LiveData<String> getDescription() { return description; }
    public void setDescription(String description) { this.description.setValue(description); }
    public LiveData<String> getHumidity() { return humidity; }
    public void setHumidity(String humidity) { this.humidity.setValue(humidity); }
    public LiveData<Integer> getHumidityInt() { return humidityInt; }
    public void setHumidityInt(int humidity) { this.humidityInt.setValue(humidity); }
    public LiveData<String> getWind() { return wind; }
    public void setWind(String wind) { this.wind.setValue(wind); }
    public LiveData<String> getPrecipitation() { return precipitation; }
    public void setPrecipitation(String precipitation) { this.precipitation.setValue(precipitation); }
    public LiveData<Integer> getPrecipitationInt() { return precipitationInt; }
    public void setPrecipitationInt(int precipitation) { this.precipitationInt.setValue(precipitation); }
    public LiveData<String> getDewpoint() { return dewpoint; }
    public void setDewpoint(String dewpoint) { this.dewpoint.setValue(dewpoint); }

    public void fetchWeatherForLocations(Context context, List<String> locations) {
        for (String location : locations) {
            fetchWeatherForLocation(context, location, null);
        }
    }

    public void fetchWeatherForLocation(Context context, String address, Runnable onComplete) {
        if (pendingRequests.contains(address)) {
            Log.d(TAG, "Request already pending for: " + address);
            if (onComplete != null) onComplete.run();
            return;
        }

        // Check if we already have data in the map to avoid re-fetching on simple navigation
        Map<String, WeatherSummary> currentMap = locationWeatherMap.getValue();
        if (currentMap != null && currentMap.containsKey(address)) {
            WeatherSummary s = currentMap.get(address);
            if (s != null && s.temperature != null && !s.temperature.equals("--")) {
                if (onComplete != null) onComplete.run();
                return;
            }
        }

        pendingRequests.add(address);
        GeocodingRetryManager.geocodeWithRetry(context, address, USER_AGENT, result -> {
            String pointsUrl = BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude();
            fetchPointsData(pointsUrl, address, onComplete);
        }, errorMessage -> {
            Log.e(TAG, "Geocoding failed for " + address + ": " + errorMessage);
            pendingRequests.remove(address);
            if (onComplete != null) onComplete.run();
        });
    }

    private void fetchPointsData(String pointsUrl, String originalAddress, Runnable onComplete) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, pointsUrl, null, response -> {
            try {
                JSONObject props = response.getJSONObject("properties");
                fetchCurrentWeatherData(props.getString("forecastHourly"), originalAddress, onComplete);
            } catch (JSONException e) {
                pendingRequests.remove(originalAddress);
                if (onComplete != null) onComplete.run();
            }
        }, error -> {
            pendingRequests.remove(originalAddress);
            if (onComplete != null) onComplete.run();
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                headers.put("Accept", "application/geo+json,application/json");
                return headers;
            }
        };
        request.setShouldCache(true); // Allow Volley to cache these points
        SunwiseApp.getInstance().getRequestQueue().add(request);
    }

    private void fetchCurrentWeatherData(String forecastUrl, String originalAddress, Runnable onComplete) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, forecastUrl, null, response -> {
            try {
                JSONArray periods = response.getJSONObject("properties").getJSONArray("periods");
                if (periods.length() > 0) {
                    JSONObject current = periods.getJSONObject(0);
                    String dewpointVal = "--";
                    if (current.has("dewpoint")) {
                        try {
                            JSONObject dp = current.getJSONObject("dewpoint");
                            dewpointVal = Math.round(dp.getDouble("value")) + "°";
                        } catch (Exception ignored) {}
                    }
                    WeatherSummary summary = new WeatherSummary(
                        current.getString("temperature"),
                        current.getString("shortForecast"),
                        current.getString("icon"),
                        current.getString("windSpeed") + " " + current.getString("windDirection"),
                        current.has("relativeHumidity") ? current.getJSONObject("relativeHumidity").optInt("value") + "%" : "N/A",
                        current.has("probabilityOfPrecipitation") ? current.getJSONObject("probabilityOfPrecipitation").optInt("value") + "%" : "N/A",
                        dewpointVal
                    );
                    Map<String, WeatherSummary> map = locationWeatherMap.getValue();
                    if (map != null) {
                        map.put(originalAddress, summary);
                        locationWeatherMap.postValue(map);
                    }
                }
            } catch (JSONException ignored) {}
            pendingRequests.remove(originalAddress);
            if (onComplete != null) onComplete.run();
        }, error -> {
            pendingRequests.remove(originalAddress);
            if (onComplete != null) onComplete.run();
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                headers.put("Accept", "application/geo+json,application/json");
                return headers;
            }
        };
        request.setShouldCache(false);
        SunwiseApp.getInstance().getRequestQueue().add(request);
    }
}
