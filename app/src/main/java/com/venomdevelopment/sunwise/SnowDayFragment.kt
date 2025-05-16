package com.venomdevelopment.sunwise

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class SnowDayFragment : Fragment() {

    private lateinit var zipcodeEditText: EditText
    private lateinit var snowdaysEditText: EditText
    private lateinit var schoolTypeSpinner: Spinner
    private lateinit var calculateButton: Button
    private lateinit var predictionToday: TextView
    private lateinit var predictionTomorrow: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_snow_day, container, false)

        zipcodeEditText = rootView.findViewById(R.id.zipcode)
        snowdaysEditText = rootView.findViewById(R.id.snowdays)
        schoolTypeSpinner = rootView.findViewById(R.id.schooltype)
        calculateButton = rootView.findViewById(R.id.calculateButton)
        predictionToday = rootView.findViewById(R.id.predictionToday)
        predictionTomorrow = rootView.findViewById(R.id.predictionTomorrow)

        calculateButton.setOnClickListener {
            val zipcode = zipcodeEditText.text.toString()
            val snowdays = snowdaysEditText.text.toString().toIntOrNull() ?: 0
            val schoolType = SnowDayCalculator.SchoolType.values()[schoolTypeSpinner.selectedItemPosition]

            SnowDayTask(zipcode, snowdays, schoolType, 1, object : SnowDayTask.OnPredictionReceivedListener {
                override fun onPredictionReceived(prediction: Long) {
                    Toast.makeText(activity, "Prediction: $prediction%", Toast.LENGTH_LONG).show()
                    Log.d("prediction i guess", "Prediction: $prediction%")
                    Log.d("prediction i guess", schoolType.toString())
                    Log.d("prediction i guess", zipcode)
                    Log.d("prediction i guess", snowdays.toString())

                    predictionToday.text = if (prediction < 0) "Limited" else "$prediction%"
                }
            }).execute()

            SnowDayTask(zipcode, snowdays, schoolType, 2, object : SnowDayTask.OnPredictionReceivedListener {
                override fun onPredictionReceived(prediction: Long) {
                    Toast.makeText(activity, "Prediction: $prediction%", Toast.LENGTH_LONG).show()
                    Log.d("prediction i guess", "Prediction: $prediction%")
                    Log.d("prediction i guess", schoolType.toString())
                    Log.d("prediction i guess", zipcode)
                    Log.d("prediction i guess", snowdays.toString())

                    predictionTomorrow.text = if (prediction == -154L) "Limited" else "$prediction%"
                }
            }).execute()
        }

        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.school_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        schoolTypeSpinner.adapter = adapter

        return rootView
    }
}
