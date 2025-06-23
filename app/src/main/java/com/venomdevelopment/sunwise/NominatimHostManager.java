package com.venomdevelopment.sunwise;

import android.os.Handler;
import android.os.Looper;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to manage geocoding hosts with randomization and fallback logic.
 * This helps distribute load and provides redundancy if one host is down.
 * Supports both Nominatim and U.S. Census Bureau Geocoder APIs.
 */
public class NominatimHostManager {
    
    // Nominatim hosts (same API format)
    private static final String[] NOMINATIM_HOSTS = {
        "https://osm-nominatim.gs.mil",
        "https://nominatim.openstreetmap.org"
    };
    
    // Census Geocoder host (different API format)
    private static final String CENSUS_GEOCODER_HOST = "https://geocoding.geo.census.gov";
    
    private static final Random random = new Random();
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final int DELAY_BETWEEN_REQUESTS_MS = 10;
    private static final int MAX_RETRY_ATTEMPTS = 8;
    
    // Track if any host has been successful recently
    private static boolean hasSuccessfulHost = false;
    private static long lastSuccessTime = 0;
    private static final long SUCCESS_RESET_TIME_MS = 60000; // Reset after 1 minute
    
    // Track which specific hosts are working
    private static final ConcurrentHashMap<String, Boolean> workingHosts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> hostSuccessTimes = new ConcurrentHashMap<>();
    private static final long HOST_SUCCESS_RESET_TIME_MS = 300000; // Reset after 5 minutes
    
    private static int dynamicMaxRetryAttempts = MAX_RETRY_ATTEMPTS;
    
    /**
     * Gets a random geocoding host for search operations
     * @return A randomly selected geocoding search URL
     */
    public static String getRandomSearchUrl() {
        // 70% chance for Nominatim hosts, 30% chance for Census Geocoder
        if (random.nextDouble() < 0.7) {
            String host = NOMINATIM_HOSTS[random.nextInt(NOMINATIM_HOSTS.length)];
            return host + "/search?q=";
        } else {
            return CENSUS_GEOCODER_HOST + "/geocoder/locations/onelineaddress?address=";
        }
    }
    
    /**
     * Gets a random Nominatim host for reverse geocoding operations
     * Note: Census Geocoder doesn't support reverse geocoding, so only Nominatim hosts are used
     * @return A randomly selected Nominatim reverse geocoding URL
     */
    public static String getRandomReverseUrl() {
        String host = NOMINATIM_HOSTS[random.nextInt(NOMINATIM_HOSTS.length)];
        return host + "/reverse?format=jsonv2";
    }
    
    /**
     * Gets the primary Nominatim host for search operations
     * @return The primary Nominatim search URL
     */
    public static String getPrimarySearchUrl() {
        return NOMINATIM_HOSTS[0] + "/search?q=";
    }
    
    /**
     * Gets the primary Nominatim host for reverse geocoding operations
     * @return The primary Nominatim reverse geocoding URL
     */
    public static String getPrimaryReverseUrl() {
        return NOMINATIM_HOSTS[0] + "/reverse?format=jsonv2";
    }
    
    /**
     * Gets the fallback Nominatim host for search operations
     * @return The fallback Nominatim search URL
     */
    public static String getFallbackSearchUrl() {
        return NOMINATIM_HOSTS[1] + "/search?q=";
    }
    
    /**
     * Gets the fallback Nominatim host for reverse geocoding operations
     * @return The fallback Nominatim reverse geocoding URL
     */
    public static String getFallbackReverseUrl() {
        return NOMINATIM_HOSTS[1] + "/reverse?format=jsonv2";
    }
    
    /**
     * Gets the Census Geocoder host for search operations
     * @return The Census Geocoder search URL
     */
    public static String getCensusGeocoderSearchUrl() {
        return CENSUS_GEOCODER_HOST + "/geocoder/locations/onelineaddress?address=";
    }
    
    /**
     * Checks if a URL is from the Census Geocoder
     * @param url The URL to check
     * @return true if it's a Census Geocoder URL, false otherwise
     */
    public static boolean isCensusGeocoderUrl(String url) {
        return url != null && url.contains(CENSUS_GEOCODER_HOST);
    }
    
    /**
     * Gets the appropriate parameters for the Census Geocoder API
     * @return The Census Geocoder API parameters
     */
    public static String getCensusGeocoderParams() {
        return "&benchmark=2020&format=json";
    }
    
    /**
     * Records a successful geocoding request
     */
    public static void recordSuccess() {
        hasSuccessfulHost = true;
        lastSuccessTime = System.currentTimeMillis();
    }
    
    /**
     * Records a successful geocoding request for a specific host
     * @param hostUrl The host URL that was successful
     */
    public static void recordHostSuccess(String hostUrl) {
        String hostKey = getHostKey(hostUrl);
        workingHosts.put(hostKey, true);
        hostSuccessTimes.put(hostKey, System.currentTimeMillis());
        recordSuccess();
    }
    
    /**
     * Checks if a specific host is working (has been successful recently)
     * @param hostUrl The host URL to check
     * @return true if the host was successful within the last 5 minutes
     */
    public static boolean isHostWorking(String hostUrl) {
        String hostKey = getHostKey(hostUrl);
        Boolean isWorking = workingHosts.get(hostKey);
        if (isWorking == null || !isWorking) {
            return false;
        }
        
        // Check if success was recent enough
        Long successTime = hostSuccessTimes.get(hostKey);
        if (successTime == null || System.currentTimeMillis() - successTime > HOST_SUCCESS_RESET_TIME_MS) {
            workingHosts.remove(hostKey);
            hostSuccessTimes.remove(hostKey);
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets a working host URL for retries
     * @return A working host URL, or null if none available
     */
    public static String getWorkingHostUrl() {
        // Check Nominatim hosts first
        for (String host : NOMINATIM_HOSTS) {
            if (isHostWorking(host + "/search?q=")) {
                return host + "/search?q=";
            }
        }
        
        // Check Census Geocoder
        if (isHostWorking(CENSUS_GEOCODER_HOST + "/geocoder/locations/onelineaddress?address=")) {
            return CENSUS_GEOCODER_HOST + "/geocoder/locations/onelineaddress?address=";
        }
        
        return null;
    }
    
    /**
     * Extracts a host key from a URL for tracking
     * @param hostUrl The full host URL
     * @return A simplified host key
     */
    private static String getHostKey(String hostUrl) {
        if (hostUrl == null) return "unknown";
        
        if (hostUrl.contains(CENSUS_GEOCODER_HOST)) {
            return "census";
        } else if (hostUrl.contains("osm-nominatim.gs.mil")) {
            return "nominatim_gs";
        } else if (hostUrl.contains("nominatim.openstreetmap.org")) {
            return "nominatim_osm";
        }
        
        return "unknown";
    }
    
    /**
     * Checks if any host has been successful recently
     * @return true if a host was successful within the last minute
     */
    public static boolean hasSuccessfulHost() {
        // Reset success flag if too much time has passed
        if (System.currentTimeMillis() - lastSuccessTime > SUCCESS_RESET_TIME_MS) {
            hasSuccessfulHost = false;
        }
        return hasSuccessfulHost;
    }
    
    /**
     * Adds a delay between requests to avoid rate limiting
     * @param runnable The code to run after the delay
     */
    public static void addDelay(Runnable runnable) {
        handler.postDelayed(runnable, DELAY_BETWEEN_REQUESTS_MS);
    }
    
    /**
     * Gets the maximum number of retry attempts
     * @return The maximum number of retry attempts
     */
    public static int getMaxRetryAttempts() {
        return MAX_RETRY_ATTEMPTS;
    }
    
    /**
     * Sets the dynamic max retry attempts for geocoding
     */
    public static void setDynamicMaxRetryAttempts(int attempts) {
        dynamicMaxRetryAttempts = attempts;
    }
    
    /**
     * Gets the dynamic max retry attempts (used for per-request logic)
     */
    public static int getDynamicMaxRetryAttempts() {
        return dynamicMaxRetryAttempts;
    }
    
    /**
     * Returns true if any API (Nominatim or Census) is working
     */
    public static boolean isAnyApiWorking() {
        for (String host : NOMINATIM_HOSTS) {
            if (isHostWorking(host + "/search?q=")) {
                return true;
            }
        }
        if (isHostWorking(CENSUS_GEOCODER_HOST + "/geocoder/locations/onelineaddress?address=")) {
            return true;
        }
        return false;
    }
} 