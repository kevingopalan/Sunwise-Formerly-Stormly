package com.venomdevelopment.sunwise;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherViewModel extends ViewModel {

    private MutableLiveData<String> currentTemperature = new MutableLiveData<>();
    private MutableLiveData<String> highTemperature = new MutableLiveData<>();
    private MutableLiveData<String> lowTemperature = new MutableLiveData<>();
    private MutableLiveData<String> description = new MutableLiveData<>();
    private MutableLiveData<String> humidity = new MutableLiveData<>();
    private MutableLiveData<String> wind = new MutableLiveData<>();
    private MutableLiveData<String> precipitation = new MutableLiveData<>();
    private MutableLiveData<LineGraphSeries<DataPoint>> hourlyGraphData = new MutableLiveData<>();
    private MutableLiveData<LineGraphSeries<DataPoint>> dailyGraphDataDay = new MutableLiveData<>();
    private MutableLiveData<LineGraphSeries<DataPoint>> dailyGraphDataNight = new MutableLiveData<>();

    public static class WeatherSummary {
        public String temperature;
        public String description;
        public String icon;
        public String wind;
        public String humidity;
        public String precipitation;
        public WeatherSummary(String temperature, String description, String icon, String wind, String humidity, String precipitation) {
            this.temperature = temperature;
            this.description = description;
            this.icon = icon;
            this.wind = wind;
            this.humidity = humidity;
            this.precipitation = precipitation;
        }
    }

    private final MutableLiveData<Map<String, WeatherSummary>> locationWeatherMap = new MutableLiveData<>(new ConcurrentHashMap<>());
    public LiveData<Map<String, WeatherSummary>> getLocationWeatherMap() {
        return locationWeatherMap;
    }

    public LiveData<String> getCurrentTemperature() {
        return currentTemperature;
    }

    public void setCurrentTemperature(String temperature) {
        this.currentTemperature.setValue(temperature);
    }

    public LiveData<String> getHighTemperature() {
        return highTemperature;
    }

    public void setHighTemperature(String highTemperature) {
        this.highTemperature.setValue(highTemperature);
    }

    public LiveData<String> getLowTemperature() {
        return lowTemperature;
    }

    public void setLowTemperature(String lowTemperature) {
        this.lowTemperature.setValue(lowTemperature);
    }

    public LiveData<String> getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description.setValue(description);
    }

    public LiveData<String> getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity.setValue(humidity);
    }

    public LiveData<String> getWind() {
        return wind;
    }

    public void setWind(String wind) {
        this.wind.setValue(wind);
    }

    public LiveData<String> getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(String precipitation) {
        this.precipitation.setValue(precipitation);
    }

    public LiveData<LineGraphSeries<DataPoint>> getHourlyGraphData() {
        return hourlyGraphData;
    }

    public void setHourlyGraphData(LineGraphSeries<DataPoint> hourlyGraphData) {
        this.hourlyGraphData.setValue(hourlyGraphData);
    }

    public LiveData<LineGraphSeries<DataPoint>> getDailyGraphDataDay() {
        return dailyGraphDataDay;
    }

    public void setDailyGraphDataDay(LineGraphSeries<DataPoint> dailyGraphDataDay) {
        this.dailyGraphDataDay.setValue(dailyGraphDataDay);
    }

    public LiveData<LineGraphSeries<DataPoint>> getDailyGraphDataNight() {
        return dailyGraphDataNight;
    }

    public void setDailyGraphDataNight(LineGraphSeries<DataPoint> dailyGraphDataNight) {
        this.dailyGraphDataNight.setValue(dailyGraphDataNight);
    }

    public void fetchWeatherForLocations(Context context, List<String> locations) {
        for (String location : locations) {
            fetchWeatherForLocation(context, location, true);
        }
    }

    // Overload: single-location fetch, updates both single and map LiveData
    public void fetchWeatherForLocation(Context context, String address) {
        fetchWeatherForLocation(context, address, false);
    }

    // Internal method: if isBatch, only update the map, not the single-location LiveData
    private void fetchWeatherForLocation(Context context, String address, boolean isBatch) {
        final String TAG = "WeatherViewModel";
        final String BASE_URL_POINTS = "https://api.weather.gov/points/";
        final String USER_AGENT = "Sunwise/v0-prerelease" + System.getProperty("http.agent");
        final com.android.volley.RequestQueue requestQueue = SunwiseApp.getInstance().getRequestQueue();

        // Helper methods for fallback
        class Fallbacks {
            void fetchGeocodingDataWithFallback(String address) {
                String encodedAddress = address.replaceAll(" ", "+");
                String geocodeUrl = NominatimHostManager.getFallbackSearchUrl() + encodedAddress + "&format=json&addressdetails=1";
                JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, geocodeUrl, null, response -> {
                    try {
                        GeocodingResponseParser.GeocodingResult result = GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                        if (result != null) {
                            NominatimHostManager.recordHostSuccess(geocodeUrl);
                            String pointsUrl = BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude();
                            fetchWeatherData(requestQueue, USER_AGENT, pointsUrl, address);
                        } else {
                            NominatimHostManager.addDelay(() -> fetchGeocodingDataWithCensusFallback(address));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        NominatimHostManager.addDelay(() -> fetchGeocodingDataWithCensusFallback(address));
                    }
                }, error -> {
                    Log.e(TAG, "Error fetching geocoding data from fallback host: " + error.getMessage());
                    NominatimHostManager.addDelay(() -> fetchGeocodingDataWithCensusFallback(address));
                }) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("User-Agent", USER_AGENT);
                        headers.put("Accept", "application/geo+json,application/json");
                        return headers;
                    }
                };
                requestQueue.getCache().clear();
                requestQueue.add(jsonArrayRequest);
            }
            void fetchGeocodingDataWithCensusFallback(String address) {
                String encodedAddress = address.replaceAll(" ", "+");
                final String geocodeUrl = NominatimHostManager.getCensusGeocoderSearchUrl() + encodedAddress + NominatimHostManager.getCensusGeocoderParams();
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, geocodeUrl, null, response -> {
                    try {
                        GeocodingResponseParser.GeocodingResult result = GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                        if (result != null) {
                            NominatimHostManager.recordHostSuccess(geocodeUrl);
                            String pointsUrl = BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude();
                            fetchWeatherData(requestQueue, USER_AGENT, pointsUrl, address);
                        } else {
                            // All hosts failed, do nothing or notify
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, error -> {
                    Log.e(TAG, "Error fetching geocoding data from Census Geocoder: " + error.getMessage());
                }) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("User-Agent", USER_AGENT);
                        headers.put("Accept", "application/geo+json,application/json");
                        return headers;
                    }
                };
                requestQueue.getCache().clear();
                requestQueue.add(jsonObjectRequest);
            }
        }
        Fallbacks fallbacks = new Fallbacks();

        // Main geocoding fetch
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
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, geocodeUrl, null, response -> {
                try {
                    GeocodingResponseParser.GeocodingResult result = GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                    if (result != null) {
                        NominatimHostManager.recordHostSuccess(geocodeUrl);
                        String pointsUrl = BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude();
                        fetchWeatherData(requestQueue, USER_AGENT, pointsUrl, address);
                    } else {
                        NominatimHostManager.addDelay(() -> fallbacks.fetchGeocodingDataWithFallback(address));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    NominatimHostManager.addDelay(() -> fallbacks.fetchGeocodingDataWithFallback(address));
                }
            }, error -> {
                Log.e(TAG, "Error fetching geocoding data from Census Geocoder: " + error.getMessage());
                NominatimHostManager.addDelay(() -> fallbacks.fetchGeocodingDataWithFallback(address));
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", USER_AGENT);
                    headers.put("Accept", "application/geo+json,application/json");
                    return headers;
                }
            };
            requestQueue.getCache().clear();
            requestQueue.add(jsonObjectRequest);
        } else {
            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, geocodeUrl, null, response -> {
                try {
                    GeocodingResponseParser.GeocodingResult result = GeocodingResponseParser.parseGeocodingResponse(response, geocodeUrl);
                    if (result != null) {
                        NominatimHostManager.recordHostSuccess(geocodeUrl);
                        String pointsUrl = BASE_URL_POINTS + result.getLatitude() + "," + result.getLongitude();
                        fetchWeatherData(requestQueue, USER_AGENT, pointsUrl, address);
                    } else {
                        NominatimHostManager.addDelay(() -> fallbacks.fetchGeocodingDataWithFallback(address));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    NominatimHostManager.addDelay(() -> fallbacks.fetchGeocodingDataWithFallback(address));
                }
            }, error -> {
                Log.e(TAG, "Error fetching geocoding data from primary host: " + error.getMessage());
                NominatimHostManager.addDelay(() -> fallbacks.fetchGeocodingDataWithFallback(address));
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", USER_AGENT);
                    headers.put("Accept", "application/geo+json,application/json");
                    return headers;
                }
            };
            requestQueue.getCache().clear();
            requestQueue.add(jsonArrayRequest);
        }
    }

    // Move fetchWeatherData outside as a private method
    private void fetchWeatherData(com.android.volley.RequestQueue requestQueue, String USER_AGENT, String pointsUrl, String originalAddress) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, pointsUrl, null, response -> {
            try {
                JSONObject properties = response.getJSONObject("properties");
                String forecastUrl = properties.getString("forecast");
                String forecastHourlyUrl = properties.getString("forecastHourly");
                
                // Use the hourly forecast for current weather data
                fetchCurrentWeatherData(requestQueue, USER_AGENT, forecastHourlyUrl, originalAddress);
                
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> {
            Log.e("WeatherViewModel", "Error fetching points data: " + error.getMessage());
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                headers.put("Accept", "application/geo+json,application/json");
                return headers;
            }
        };
        requestQueue.getCache().clear();
        requestQueue.add(jsonObjectRequest);
    }

    private void fetchCurrentWeatherData(com.android.volley.RequestQueue requestQueue, String USER_AGENT, String forecastUrl, String originalAddress) {
        Log.d("WeatherViewModel", "Fetching current weather data for: " + originalAddress + " from: " + forecastUrl);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, forecastUrl, null, response -> {
            try {
                JSONObject properties = response.getJSONObject("properties");
                JSONArray periods = properties.getJSONArray("periods");
                
                Log.d("WeatherViewModel", "Received forecast data for " + originalAddress + ", periods: " + periods.length());
                
                if (periods.length() > 0) {
                    JSONObject currentPeriod = periods.getJSONObject(0);
                    
                    String temperature = currentPeriod.getString("temperature");
                    String description = currentPeriod.getString("shortForecast");
                    String icon = currentPeriod.getString("icon");
                    String wind = currentPeriod.getString("windSpeed") + " " + currentPeriod.getString("windDirection");
                    String humidity = "N/A"; // Not available in this API
                    String precipitation = "N/A"; // Not available in this API
                    
                    Log.d("WeatherViewModel", "Weather data for " + originalAddress + ": temp=" + temperature + ", desc=" + description + ", icon=" + icon);
                    
                    Map<String, WeatherSummary> map = locationWeatherMap.getValue();
                    map.put(originalAddress, new WeatherSummary(
                        temperature,
                        description,
                        icon,
                        wind,
                        humidity,
                        precipitation
                    ));
                    locationWeatherMap.postValue(map);
                    Log.d("WeatherViewModel", "Updated weather map, total entries: " + map.size());
                } else {
                    Log.w("WeatherViewModel", "No periods found in forecast data for " + originalAddress);
                }
            } catch (JSONException e) {
                Log.e("WeatherViewModel", "Error parsing forecast data for " + originalAddress + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, error -> {
            Log.e("WeatherViewModel", "Error fetching forecast data for " + originalAddress + ": " + error.getMessage());
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                headers.put("Accept", "application/geo+json,application/json");
                return headers;
            }
        };
        requestQueue.getCache().clear();
        requestQueue.add(jsonObjectRequest);
    }
}