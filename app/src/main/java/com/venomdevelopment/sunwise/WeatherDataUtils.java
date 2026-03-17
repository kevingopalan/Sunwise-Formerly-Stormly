package com.venomdevelopment.sunwise;

import org.json.JSONException;
import org.json.JSONObject;

public class WeatherDataUtils {
    // idk why I even made this, probably was planning on adding more and transferring stuff but never did
    public static JSONObject getFirstHourlyPeriod(JSONObject hourlyResponse) throws JSONException {
        return hourlyResponse.getJSONObject("properties").getJSONArray("periods").getJSONObject(0);
    }
} 