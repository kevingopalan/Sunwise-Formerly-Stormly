package com.venomdevelopment.sunwise

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import kotlin.math.min

class SnowDayCalculator(
    val zipcode: String,
    val snowdays: Int,
    val schoolType: SchoolType
) {
    private val BASE_URL = "https://www.snowdaycalculator.com/prediction.php"

    @get:Throws(IOException::class)
    val predictions: Map<String, Long>
        /**
         * Fetches the prediction from the website and parses the result.
         * @return Prediction value, which can be larger than 99 or less than 0.
         * @throws IOException
         */
        get() {
            val predictions = mutableMapOf<String, Long>()

            // Construct the URL to fetch the prediction data
            var url =
                URL(BASE_URL + "?zipcode=" + this.zipcode + "&snowdays=" + this.snowdays + "&extra=" + schoolType.extra)
            Log.d("URL", url.toString())

            // Open connection and set the User-Agent header (Mozilla)
            var connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0"
            )

            // Get the response code
            var responseCode = connection.responseCode
            Log.d("Response Code", "Response Code: $responseCode")

            // If we get a redirect (301), follow the Location header
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                val location = connection.getHeaderField("Location")
                Log.d("Redirect", "Redirecting to: $location")

                // Create a new connection to the Location URL
                url = URL(location)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0"
                )

                // Get the response from the redirected URL
                responseCode = connection.responseCode
                Log.d("Response Code after Redirect", "Response Code: $responseCode")
            }

            // StringBuilder to hold the HTML response
            val htmlContent = StringBuilder()

            // Read the response
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val `in` =
                    BufferedReader(InputStreamReader(connection.inputStream))
                var inputLine: String?
                while ((`in`.readLine().also { inputLine = it }) != null) {
                    htmlContent.append(inputLine)
                }
                `in`.close()
            } else {
                // If the response code isn't 200, log the error
                Log.e("Error", "Failed to get response: $responseCode")
            }

            // Convert StringBuilder to String for further parsing
            val html = htmlContent.toString()

            // Log the first 5000 characters of the HTML content to ensure we're getting a response
            Log.d(
                "HTML Response",
                html.substring(0, min(5000.0, html.length.toDouble()).toInt())
            )

            // Now, you can parse the HTML content to find the prediction
            val pattern = Pattern.compile("""theChance\[(\d{8})\]\s*=\s*([\d.]+);""")

            val matcher = pattern.matcher(html)

            while (matcher.find()) {
                val date = matcher.group(1)
                val chance = matcher.group(2)?.toDouble()?.let { Math.round(it) }
                if (date != null && chance != null) {
                    predictions[date] = chance
                }
            }

            return predictions
        }

    enum class SchoolType(val extra: Double) {
        PUBLIC(0.0), URBAN_PUBLIC(0.4), RURAL_PUBLIC(-0.4), PRIVATE_PREP(-0.4), BOARDING(1.0)
    }
}