package com.venomdevelopment.sunwise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.Utils
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
    private lateinit var snowdayBarChart: BarChart

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
        snowdayBarChart = rootView.findViewById(R.id.snowday_bar_chart)

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
                    if (!isAdded) return // Prevent crash if fragment is detached

                    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val cal = Calendar.getInstance()

                    // Extract Today
                    val dateToday = dateFormat.format(cal.time)
                    val pToday = predictions[dateToday]

                    // Extract Tomorrow
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val dateTomorrow = dateFormat.format(cal.time)
                    val pTomorrow = predictions[dateTomorrow]

                    // Extract Day After Tomorrow
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val dateTwoDays = dateFormat.format(cal.time)
                    val pTwoDays = predictions[dateTwoDays]

                    // Update UI Texts
                    predictionToday.text = pToday?.let { if (it < 0) "Limited" else "$it%" } ?: "N/A"
                    predictionTomorrow.text = pTomorrow?.let { if (it < 0) "Limited" else "$it%" } ?: "N/A"
                    predictionTwoDays.text = pTwoDays?.let { if (it < 0) "Limited" else "$it%" } ?: "N/A"

                    // Setup Bar Chart Data
                    val entries = ArrayList<BarEntry>()
                    val todayVal = if (pToday != null && pToday >= 0) pToday.toFloat() else 0f
                    val tomorrowVal = if (pTomorrow != null && pTomorrow >= 0) pTomorrow.toFloat() else 0f
                    val twoVal = if (pTwoDays != null && pTwoDays >= 0) pTwoDays.toFloat() else 0f

                    entries.add(BarEntry(0f, todayVal))
                    entries.add(BarEntry(1f, tomorrowVal))
                    entries.add(BarEntry(2f, twoVal))

                    val set = BarDataSet(entries, "Snow Day Chance").apply {
                        color = ContextCompat.getColor(requireContext(), R.color.df_low)
                        setDrawValues(true)
                        valueTextSize = 12f
                        valueTextColor = ContextCompat.getColor(requireContext(), android.R.color.white)
                        try {
                            valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.montsemibold)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val data = BarData(set).apply {
                        barWidth = 0.6f
                    }
                    snowdayBarChart.data = data

                    // Setup X-Axis
                    val labels = arrayListOf("Today", "Tomorrow", "Day After")
                    snowdayBarChart.xAxis.apply {
                        granularity = 1f
                        position = XAxis.XAxisPosition.BOTTOM
                        setDrawGridLines(false)
                        valueFormatter = IndexAxisValueFormatter(labels)
                        textColor = ContextCompat.getColor(requireContext(), android.R.color.white)
                        textSize = 12f
                    }

                    // Setup Y-Axis (Left)
                    snowdayBarChart.axisLeft.apply {
                        setDrawGridLines(false)
                        axisMinimum = 0f
                        axisMaximum = 100f
                        textColor = ContextCompat.getColor(requireContext(), android.R.color.white)
                        textSize = 12f
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return "${Math.round(value.toDouble())}%"
                            }
                        }
                    }

                    // Apply Custom Font to Axes
                    try {
                        val tf = ResourcesCompat.getFont(requireContext(), R.font.montsemibold)
                        snowdayBarChart.xAxis.typeface = tf
                        snowdayBarChart.axisLeft.typeface = tf
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Render Settings (Using existing Java renderer from ForecastFragment)
                    snowdayBarChart.apply {
                        renderer = ForecastFragment.RoundedBarChartRenderer(
                            this,
                            animator,
                            viewPortHandler,
                            Utils.convertDpToPixel(8f)
                        )
                        axisRight.isEnabled = false
                        legend.isEnabled = false
                        description.isEnabled = false
                        setFitBars(true)
                        invalidate() // Refresh chart
                    }
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