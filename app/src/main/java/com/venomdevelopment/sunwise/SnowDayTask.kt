package com.venomdevelopment.sunwise

import android.os.AsyncTask
import android.util.Log
import com.venomdevelopment.sunwise.SnowDayCalculator.SchoolType

class SnowDayTask(
    private val zipcode: String,
    private val snowdays: Int,
    private val schoolType: SchoolType,
    private val theday: Int,
    private val listener: OnPredictionReceivedListener?
) :
    AsyncTask<Void?, Void?, Long>() {
    override fun doInBackground(vararg p0: Void?): Long? {
        try {
            // Create a SnowDayCalculator object and get the prediction
            val calculator = SnowDayCalculator(
                zipcode,
                snowdays,
                schoolType,
                theday
            )
            return calculator.prediction
        } catch (e: Exception) {
            Log.e("SnowDayTask", "Error getting snow day prediction", e)
            return 0L
        }
    }

    override fun onPostExecute(result: Long) {
        super.onPostExecute(result)

        // Return the result back to the listener on the main thread
        listener?.onPredictionReceived(result)
    }

    interface OnPredictionReceivedListener {
        fun onPredictionReceived(prediction: Long)
    }
}

