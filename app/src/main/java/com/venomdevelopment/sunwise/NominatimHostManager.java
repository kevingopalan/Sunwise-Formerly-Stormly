package com.venomdevelopment.sunwise;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.android.volley.Request;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class to manage geocoding hosts and strictly enforce rate limiting.
 */
public class NominatimHostManager {
    private static final String TAG = "NominatimHostManager";
    private static final String NOMINATIM_BASE = "https://nominatim.openstreetmap.org";
    private static final String CENSUS_BASE = "https://geocoding.geo.census.gov";
    private static final int NOMINATIM_DELAY_MS = 1010; // Slightly over 1s to be safe
    
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final AtomicLong nextAllowedTime = new AtomicLong(0);

    public static String getPrimarySearchUrl() {
        return NOMINATIM_BASE + "/search?q=";
    }

    public static String getReverseUrl() {
        return NOMINATIM_BASE + "/reverse?format=jsonv2";
    }

    public static String getCensusGeocoderSearchUrl() {
        return CENSUS_BASE + "/geocoder/locations/onelineaddress?address=";
    }

    public static String getCensusGeocoderParams() {
        return "&benchmark=2020&format=json";
    }

    public static boolean isCensusGeocoderUrl(String url) {
        return url != null && url.contains(CENSUS_BASE);
    }

    /**
     * Executes a Volley request with a guaranteed delay if it's for a Nominatim host.
     */
    public static void enqueueRequest(Request<?> request) {
        String url = request.getUrl();
        executeWithRateLimit(url, () -> SunwiseApp.getInstance().getRequestQueue().add(request));
    }

    /**
     * Executes a runnable with a guaranteed delay if it's for a Nominatim host.
     * This ensures at least 1 second between consecutive Nominatim calls across the whole app.
     */
    public static void executeWithRateLimit(String url, Runnable runnable) {
        if (url != null && url.contains("nominatim.openstreetmap.org")) {
            synchronized (nextAllowedTime) {
                long now = System.currentTimeMillis();
                long scheduledTime = Math.max(now, nextAllowedTime.get());
                long delay = scheduledTime - now;
                
                nextAllowedTime.set(scheduledTime + NOMINATIM_DELAY_MS);
                
                if (delay > 0) {
                    Log.d(TAG, "Rate limiting Nominatim: delaying " + delay + "ms for " + url);
                    handler.postDelayed(runnable, delay);
                } else {
                    runnable.run();
                }
            }
        } else {
            // Census or other hosts don't have the same strict 1s limit
            runnable.run();
        }
    }

    public static void recordHostSuccess(String hostUrl) {
        // Optional: keep track of working hosts if needed for smarter selection
    }

    public static int getMaxRetryAttempts() {
        return 5;
    }
}
