package com.venomdevelopment.sunwise;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SnowDayFragment extends Fragment {

    private EditText zipcodeEditText;
    private EditText snowdaysEditText;
    private Spinner schoolTypeSpinner;
    private Button calculateButton;
    private TextView predictionToday;
    private TextView predictionTomorrow;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout
        View rootView = inflater.inflate(R.layout.fragment_snow_day, container, false);

        // Find the views by their IDs
        zipcodeEditText = rootView.findViewById(R.id.zipcode);
        snowdaysEditText = rootView.findViewById(R.id.snowdays);
        schoolTypeSpinner = rootView.findViewById(R.id.schooltype);
        calculateButton = rootView.findViewById(R.id.calculateButton); // Add a Button in XML
        predictionToday = rootView.findViewById(R.id.predictionToday);
        predictionTomorrow = rootView.findViewById(R.id.predictionTomorrow);

        // Set up the calculate button's click listener
        calculateButton.setOnClickListener(v -> {
            String zipcode = zipcodeEditText.getText().toString();
            int snowdays = Integer.parseInt(snowdaysEditText.getText().toString());
            SnowDayCalculator.SchoolType schoolType = SnowDayCalculator.SchoolType.values()[schoolTypeSpinner.getSelectedItemPosition()];

            // Start the AsyncTask
            new SnowDayTask(zipcode, snowdays, schoolType, 1, new SnowDayTask.OnPredictionReceivedListener() {
                @Override
                public void onPredictionReceived(long prediction) {
                    // Update the UI with the result
                    // You could update the TextView, GraphView, or show a Toast
                    // Example: show a Toast with the prediction
                    Toast.makeText(getActivity(), "Prediction: " + prediction + "%", Toast.LENGTH_LONG).show();
                    Log.d("prediction i guess","Prediction: " + prediction + "%");
                    Log.d("prediction i guess", schoolType.toString());
                    Log.d("prediction i guess", zipcode);
                    Log.d("prediction i guess", String.valueOf(snowdays));
                    if (prediction < 0) {
                        predictionToday.setText("Limited");
                    }
                    else {
                        predictionToday.setText(prediction + "%");
                    }
                }
            }).execute();
            new SnowDayTask(zipcode, snowdays, schoolType, 2, new SnowDayTask.OnPredictionReceivedListener() {
                @Override
                public void onPredictionReceived(long prediction) {
                    // Update the UI with the result
                    // You could update the TextView, GraphView, or show a Toast
                    // Example: show a Toast with the prediction
                    Toast.makeText(getActivity(), "Prediction: " + prediction + "%", Toast.LENGTH_LONG).show();
                    Log.d("prediction i guess","Prediction: " + prediction + "%");
                    Log.d("prediction i guess", schoolType.toString());
                    Log.d("prediction i guess", zipcode);
                    Log.d("prediction i guess", String.valueOf(snowdays));
                    if (prediction == -154) {
                        predictionTomorrow.setText("Limited");
                    }
                    else {
                        predictionTomorrow.setText(prediction + "%");
                    }
                }
            }).execute();
        });
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.school_types, // Define an array in strings.xml
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        schoolTypeSpinner.setAdapter(adapter);


        return rootView;
    }
}
