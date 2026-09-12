package com.venomdevelopment.sunwise;

import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Locale;

public final class WeatherIconUtils {
    private WeatherIconUtils() {
    }

    public static String getCanonicalNwsIconKey(@Nullable String iconUrl) {
        if (iconUrl == null || iconUrl.trim().isEmpty()) {
            return "day/skc";
        }

        String normalized = iconUrl.trim().replace('\\', '/').toLowerCase(Locale.US);

        int iconsIndex = normalized.indexOf("/icons/");
        if (iconsIndex >= 0) {
            int startIndex = normalized.indexOf("/icons/land/", iconsIndex);
            if (startIndex >= 0) {
                normalized = normalized.substring(startIndex + "/icons/land/".length());
            } else {
                normalized = normalized.substring(iconsIndex + "/icons/".length());
            }
        }

        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }

        int commaIndex = normalized.indexOf(',');
        if (commaIndex >= 0) {
            normalized = normalized.substring(0, commaIndex);
        }

        normalized = normalized.replaceAll("^/+", "").replaceAll("/+", "/");
        String[] parts = normalized.split("/");
        if (parts.length > 2) {
            normalized = parts[0] + "/" + parts[parts.length - 1];
        }

        return normalized.isEmpty() ? "day/skc" : normalized;
    }

    public static String getAnimationResourceName(@Nullable String iconUrl) {
        return getAnimationResourceName(iconUrl, !getCanonicalNwsIconKey(iconUrl).startsWith("night/"));
    }

    public static String getAnimationResourceName(@Nullable String iconUrl, boolean isDaytime) {
        String iconKey = getCanonicalNwsIconKey(iconUrl);
        String normalizedKey = isDaytime ? iconKey : (iconKey.startsWith("day/") ? "night/" + iconKey.substring("day/".length()) : iconKey);

        switch (normalizedKey) {
            case "day/skc":
            case "night/skc":
            case "day/few":
            case "night/few":
            case "day/wind_skc":
            case "night/wind_skc":
            case "day/wind_few":
            case "night/wind_few":
            case "day/hot":
            case "night/hot":
            case "day/cold":
            case "night/cold":
            case "day/clear":
            case "night/clear":
                return isDaytime ? "clear_day" : "clear_night";

            case "day/sct":
            case "night/sct":
            case "day/wind_sct":
            case "night/wind_sct":
            case "day/partly_cloudy":
            case "night/partly_cloudy":
                return isDaytime ? "partly_cloudy_day" : "partly_cloudy_night";

            case "day/bkn":
            case "night/bkn":
            case "day/mostly_cloudy":
            case "night/mostly_cloudy":
            case "day/wind_bkn":
            case "night/wind_bkn":
                return "cloudy";

            case "day/ovc":
            case "night/ovc":
            case "day/wind_ovc":
            case "night/wind_ovc":
                return isDaytime ? "overcast" : "overcast_night";

            case "day/snow":
            case "night/snow":
            case "day/rain_snow":
            case "night/rain_snow":
            case "day/blizzard":
            case "night/blizzard":
                return "snow";

            case "day/rain_sleet":
            case "night/rain_sleet":
            case "day/snow_sleet":
            case "night/snow_sleet":
            case "day/fzra":
            case "night/fzra":
            case "day/rain_fzra":
            case "night/rain_fzra":
            case "day/snow_fzra":
            case "night/snow_fzra":
            case "day/sleet":
            case "night/sleet":
                return "sleet";

            case "day/rain":
            case "night/rain":
            case "day/rain_showers":
            case "night/rain_showers":
            case "day/rain_showers_hi":
            case "night/rain_showers_hi":
            case "day/drizzle":
            case "night/drizzle":
                return "rain";

            case "day/tsra":
            case "night/tsra":
            case "day/tsra_sct":
            case "night/tsra_sct":
            case "day/tsra_hi":
            case "night/tsra_hi":
            case "day/sct/tsra":
            case "night/sct/tsra":
            case "day/sct/tsra_hi":
            case "night/sct/tsra_hi":
            case "day/tornado":
            case "night/tornado":
                return isDaytime ? "lightning_bolt" : "thunderstorms_night";

            case "day/hurricane":
            case "night/hurricane":
            case "day/tropical_storm":
            case "night/tropical_storm":
                return "hurricane";

            case "day/dust":
            case "night/dust":
                return isDaytime ? "dust" : "dust_night";

            case "day/smoke":
            case "night/smoke":
                return "smoke";

            case "day/haze":
            case "night/haze":
                return isDaytime ? "haze" : "haze_night";

            case "day/fog":
            case "night/fog":
                return isDaytime ? "fog" : "fog_night";

            case "day/wind":
            case "night/wind":
                return "wind";

            default:
                Log.w("WeatherIconUtils", "Unknown NWS icon key: " + iconKey + ", using clear_day as fallback");
                return isDaytime ? "clear_day" : "clear_night";
        }
    }

    public static int getGradientResIdForIcon(@Nullable String iconUrl, boolean isDaytime) {
        String iconKey = getCanonicalNwsIconKey(iconUrl);

        if (iconKey.contains("tsra") || iconKey.contains("tornado") || iconKey.contains("hurricane") || iconKey.contains("tstm")) {
            return isDaytime ? R.drawable.gradient_thunderstorm_day : R.drawable.gradient_thunderstorm_night;
        }

        if (iconKey.contains("fog") || iconKey.contains("mist") || iconKey.contains("haze") || iconKey.contains("smoke") || iconKey.contains("dust")) {
            return isDaytime ? R.drawable.gradient_fog_day : R.drawable.gradient_fog_night;
        }

        if (iconKey.contains("snow") || iconKey.contains("sleet") || iconKey.contains("ice") || iconKey.contains("flurr") || iconKey.contains("blizzard")) {
            return isDaytime ? R.drawable.gradient_snow_day : R.drawable.gradient_snow_night;
        }

        if (iconKey.contains("rain") || iconKey.contains("showers") || iconKey.contains("drizzle") || iconKey.contains("ra")) {
            return isDaytime ? R.drawable.gradient_rain_day : R.drawable.gradient_rain_night;
        }

        switch (iconKey) {
            case "day/skc":
            case "night/skc":
            case "day/few":
            case "night/few":
            case "day/wind_skc":
            case "night/wind_skc":
            case "day/wind_few":
            case "night/wind_few":
            case "day/hot":
            case "night/hot":
            case "day/cold":
            case "night/cold":
            case "day/clear":
            case "night/clear":
                return isDaytime ? R.drawable.gradient_clear_day : R.drawable.gradient_clear_night;

            case "day/sct":
            case "night/sct":
            case "day/wind_sct":
            case "night/wind_sct":
            case "day/partly_cloudy":
            case "night/partly_cloudy":
                return isDaytime ? R.drawable.gradient_cloudy_day : R.drawable.gradient_cloudy_night;

            case "day/bkn":
            case "night/bkn":
            case "day/mostly_cloudy":
            case "night/mostly_cloudy":
            case "day/wind_bkn":
            case "night/wind_bkn":
            case "day/ovc":
            case "night/ovc":
            case "day/wind_ovc":
            case "night/wind_ovc":
                return isDaytime ? R.drawable.gradient_cloudy_day : R.drawable.gradient_cloudy_night;

            default:
                return isDaytime ? R.drawable.gradient_clear_day : R.drawable.gradient_clear_night;
        }
    }
}
