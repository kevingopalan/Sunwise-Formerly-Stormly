package com.venomdevelopment.sunwise;

import android.os.AsyncTask;
import android.util.Log;

public class SnowDayTask extends AsyncTask<Void, Void, Long> {

    private String zipcode;
    private int snowdays;
    private SnowDayCalculator.SchoolType schoolType;
    private OnPredictionReceivedListener listener;
    private int theday;

    public SnowDayTask(String zipcode, int snowdays, SnowDayCalculator.SchoolType schoolType, int theday, OnPredictionReceivedListener listener) {
        this.zipcode = zipcode;
        this.snowdays = snowdays;
        this.schoolType = schoolType;
        this.listener = listener;
        this.theday = theday;
    }

    @Override
    protected Long doInBackground(Void... voids) {
        try {
            // Create a SnowDayCalculator object and get the prediction
            SnowDayCalculator calculator = new SnowDayCalculator(zipcode, snowdays, schoolType, theday);
            return calculator.getPrediction();
        } catch (Exception e) {
            Log.e("SnowDayTask", "Error getting snow day prediction", e);
            return 0L;
        }
    }

    @Override
    protected void onPostExecute(Long result) {
        super.onPostExecute(result);

        // Return the result back to the listener on the main thread
        if (listener != null) {
            listener.onPredictionReceived(result);
        }
    }

    public interface OnPredictionReceivedListener {
        void onPredictionReceived(long prediction);
    }
}

