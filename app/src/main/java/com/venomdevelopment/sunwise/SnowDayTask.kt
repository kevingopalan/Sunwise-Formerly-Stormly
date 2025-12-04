package com.venomdevelopment.sunwise

import android.os.AsyncTask
import android.util.Log
import com.venomdevelopment.sunwise.SnowDayCalculator.SchoolType

class SnowDayTask(
    private val zipcode: String,
    private val snowdays: Int,
    private val schoolType: SchoolType,
    private val listener: OnPredictionReceivedListener?
) :
    AsyncTask<Void?, Void?, Map<String, Long>>() {
    override fun doInBackground(vararg p0: Void?): Map<String, Long>? {
        try {
            // Create a SnowDayCalculator object and get the prediction
            val calculator = SnowDayCalculator(
                zipcode,
                snowdays,
                schoolType
            )
            return calculator.predictions
        } catch (e: Exception) {
            Log.e("SnowDayTask", "Error getting snow day prediction", e)
            return emptyMap()
        }
    }

    override fun onPostExecute(result: Map<String, Long>?) {
        super.onPostExecute(result)

        // Return the result back to the listener on the main thread
        if (result != null) {
            listener?.onPredictionReceived(result)
        }
    }

    interface OnPredictionReceivedListener {
        fun onPredictionReceived(predictions: Map<String, Long>)
    }
}