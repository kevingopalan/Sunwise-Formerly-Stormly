package com.venomdevelopment.sunwise

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.min

class SnowDayCalculator(
    val zipcode: String,
    val snowdays: Int,
    val schoolType: SchoolType,
    private val theday: Int
) {
    private val BASE_URL = "https://www.snowdaycalculator.com/prediction.php"


    @get:Throws(IOException::class)
    val prediction: Long
        /**
         * Fetches the prediction from the website and parses the result.
         * @return Prediction value, which can be larger than 99 or less than 0.
         * @throws IOException
         */
        get() {
            val format: DateFormat = SimpleDateFormat("yyyyMMdd")
            val date = Date()
            val dateInt = format.format(date).toInt() + theday

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
            val predictionKeyword = "theChance[$dateInt]"

            // Check if the prediction keyword exists in the HTML content
            var chance: Long = 0
            if (html.contains(predictionKeyword)) {
                val startIndex = html.indexOf(predictionKeyword)
                val endIndex = html.indexOf(";", startIndex)
                if (startIndex != -1 && endIndex != -1) {
                    val predictionString = html.substring(startIndex, endIndex)
                    try {
                        // Extract the prediction number
                        val parts =
                            predictionString.split("=".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        chance = Math.round(parts[1].trim { it <= ' ' }.toDouble())
                    } catch (e: Exception) {
                        Log.e("Parsing Error", "Error parsing chance", e)
                    }
                }
            }

            return chance
        }

    enum class SchoolType(val extra: Double) {
        PUBLIC(0.0), URBAN_PUBLIC(0.4), RURAL_PUBLIC(-0.4), PRIVATE_PREP(-0.4), BOARDING(1.0)
    }
}