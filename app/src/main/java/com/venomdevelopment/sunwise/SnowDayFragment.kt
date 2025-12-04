package com.venomdevelopment.sunwise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class SnowDayFragment : Fragment() {

    private lateinit var zipcodeEditText: EditText
    private lateinit var snowdaysEditText: EditText
    private lateinit var schoolTypeSpinner: Spinner
    private lateinit var calculateButton: Button
    private lateinit var predictionToday: TextView
    private lateinit var predictionTomorrow: TextView
    private lateinit var predictionTwoDays: TextView
    private lateinit var dayOne: TextView
    private lateinit var dayTwo: TextView
    private lateinit var dayThree: TextView

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
        predictionTwoDays = rootView.findViewById(R.id.predictionTwoDays)
        dayOne = rootView.findViewById(R.id.dayOne)
        dayTwo = rootView.findViewById(R.id.dayTwo)
        dayThree = rootView.findViewById(R.id.dayThree)

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())

        dayOne.text = "Today (${sdf.format(calendar.time)}):"
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        dayTwo.text = "Tomorrow (${sdf.format(calendar.time)}):"
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        dayThree.text = "Day After Tomorrow (${sdf.format(calendar.time)}):"

        calculateButton.setOnClickListener {
            val zipcode = zipcodeEditText.text.toString()
            val snowdays = snowdaysEditText.text.toString().toIntOrNull() ?: 0
            val schoolType = SnowDayCalculator.SchoolType.values()[schoolTypeSpinner.selectedItemPosition]

            val listener = object : SnowDayTask.OnPredictionReceivedListener {
                override fun onPredictionReceived(predictions: Map<String, Long>) {
                    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val calendar = Calendar.getInstance()

                    // Today
                    var date = dateFormat.format(calendar.time)
                    var prediction = predictions[date]
                    predictionToday.text = prediction?.let { if (it < 0) "Limited" else "$it%" } ?: "N/A"

                    // Tomorrow
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    date = dateFormat.format(calendar.time)
                    prediction = predictions[date]
                    predictionTomorrow.text = prediction?.let { if (it < 0) "Limited" else "$it%" } ?: "N/A"

                    // Day After Tomorrow
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    date = dateFormat.format(calendar.time)
                    prediction = predictions[date]
                    predictionTwoDays.text = prediction?.let { if (it < 0) "Limited" else "$it%" } ?: "N/A"
                }
            }

            SnowDayTask(zipcode, snowdays, schoolType, listener).execute()
        }

        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.school_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        schoolTypeSpinner.adapter = adapter

        arguments?.getString("zipcode")?.let {
            zipcodeEditText.setText(it)
            calculateButton.performClick()
        }

        return rootView
    }
}
