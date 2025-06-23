package com.venomdevelopment.sunwise;

import org.json.JSONException;
import org.json.JSONObject;

public class WeatherDataUtils {
    /**
     * Returns the first period object from the hourly forecast response.
     * @param hourlyResponse The full hourly forecast JSON response.
     * @return The first period JSONObject.
     * @throws JSONException if parsing fails.
     */
    public static JSONObject getFirstHourlyPeriod(JSONObject hourlyResponse) throws JSONException {
        return hourlyResponse.getJSONObject("properties").getJSONArray("periods").getJSONObject(0);
    }
    // Add more shared weather data parsing methods as needed
} 