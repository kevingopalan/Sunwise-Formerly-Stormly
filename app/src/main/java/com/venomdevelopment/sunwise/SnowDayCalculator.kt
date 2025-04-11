package com.venomdevelopment.sunwise;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SnowDayCalculator {

    private String zipcode;
    private int snowdays;
    private SchoolType schoolType;
    private final String BASE_URL = "http://www.snowdaycalculator.com/prediction.php";
    private int theday;


    public SnowDayCalculator(String zipcode, int snowdays, SchoolType schoolType, int theday) {
        this.zipcode = zipcode;
        this.snowdays = snowdays;
        this.schoolType = schoolType;
        this.theday = theday;
    }

    /**
     * Fetches the prediction from the website and parses the result.
     * @return Prediction value, which can be larger than 99 or less than 0.
     * @throws IOException
     */
    public long getPrediction() throws IOException {
        DateFormat format = new SimpleDateFormat("yyyyMMdd");
        Date date = new Date();
        int dateInt = Integer.valueOf(format.format(date)) + theday;

        // Construct the URL to fetch the prediction data
        URL url = new URL(BASE_URL + "?zipcode=" + this.zipcode + "&snowdays=" + this.snowdays + "&extra=" + this.schoolType.getExtra());
        Log.d("URL", url.toString());

        // Open connection and set the User-Agent header (Mozilla)
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0");

        // Get the response code
        int responseCode = connection.getResponseCode();
        Log.d("Response Code", "Response Code: " + responseCode);

        // If we get a redirect (301), follow the Location header
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String location = connection.getHeaderField("Location");
            Log.d("Redirect", "Redirecting to: " + location);

            // Create a new connection to the Location URL
            url = new URL(location);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0");

            // Get the response from the redirected URL
            responseCode = connection.getResponseCode();
            Log.d("Response Code after Redirect", "Response Code: " + responseCode);
        }

        // StringBuilder to hold the HTML response
        StringBuilder htmlContent = new StringBuilder();

        // Read the response
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                htmlContent.append(inputLine);
            }
            in.close();
        } else {
            // If the response code isn't 200, log the error
            Log.e("Error", "Failed to get response: " + responseCode);
        }

        // Convert StringBuilder to String for further parsing
        String html = htmlContent.toString();

        // Log the first 5000 characters of the HTML content to ensure we're getting a response
        Log.d("HTML Response", html.substring(0, Math.min(5000, html.length())));

        // Now, you can parse the HTML content to find the prediction
        String predictionKeyword = "theChance[" + dateInt + "]";

        // Check if the prediction keyword exists in the HTML content
        long chance = 0;
        if (html.contains(predictionKeyword)) {
            int startIndex = html.indexOf(predictionKeyword);
            int endIndex = html.indexOf(";", startIndex);
            if (startIndex != -1 && endIndex != -1) {
                String predictionString = html.substring(startIndex, endIndex);
                try {
                    // Extract the prediction number
                    String[] parts = predictionString.split("=");
                    chance = Math.round(Double.valueOf(parts[1].trim()));
                } catch (Exception e) {
                    Log.e("Parsing Error", "Error parsing chance", e);
                }
            }
        }

        return chance;
    }

    public String getZipcode() {
        return this.zipcode;
    }

    public int getSnowdays() {
        return this.snowdays;
    }

    public SchoolType getSchoolType() {
        return this.schoolType;
    }

    enum SchoolType {
        PUBLIC(0), URBAN_PUBLIC(0.4), RURAL_PUBLIC(-0.4), PRIVATE_PREP(-0.4), BOARDING(1);
        private double extra;

        SchoolType(double extra) {
            this.extra = extra;
        }

        public double getExtra() {
            return this.extra;
        }
    }
}


