package com.venomdevelopment.sunwise

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.net.toUri
import androidx.core.content.edit

@SuppressLint("UseSwitchCompatOrMaterialCode")
class SettingsFragment : Fragment() {
    private lateinit var unitSpinner: Spinner
    private lateinit var windUnitSpinner: Spinner
    private lateinit var autoLocationSwitch: Switch
    private lateinit var timeFormatSwitch: Switch
    private lateinit var clearLocationsButton: Button
    private lateinit var feedbackButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)
        sharedPreferences = requireContext().getSharedPreferences("SunwiseSettings", Context.MODE_PRIVATE)
        unitSpinner = rootView.findViewById(R.id.unit)
        windUnitSpinner = rootView.findViewById(R.id.windUnitSpinner)
        autoLocationSwitch = rootView.findViewById(R.id.autoLocationSwitch)
    // precisionSwitch removed from code
        timeFormatSwitch = rootView.findViewById(R.id.timeFormatSwitch)
        clearLocationsButton = rootView.findViewById(R.id.clearLocationsButton)
        feedbackButton = rootView.findViewById(R.id.feedbackButton)

        // Setup unit spinner
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.unit_entries,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = adapter

        // Setup wind unit spinner
        val windAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.wind_unit_entries,
            android.R.layout.simple_spinner_item
        )
        windAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        windUnitSpinner.adapter = windAdapter

        loadSettings()
        setupListeners()
        return rootView
    }

    override fun onResume() {
        super.onResume()
        // Set auto location switch to match current preference
        autoLocationSwitch.isChecked = sharedPreferences.getBoolean("auto_location_enabled", true)
    // precision option removed; temperatures are displayed rounded by default
        // Set time format switch to match current preference
        timeFormatSwitch.isChecked = sharedPreferences.getBoolean("use_24_hour_format", false)
    }

    private fun loadSettings() {
        // Load unit preference
        val savedUnit = sharedPreferences.getString("unit", "us")
        val unitValues = resources.getStringArray(R.array.unit_values)
        val unitIndex = unitValues.indexOf(savedUnit)
        if (unitIndex != -1) {
            unitSpinner.setSelection(unitIndex)
        }
        // Load wind unit preference
        val savedWindUnit = sharedPreferences.getString("wind_unit", "mph")
        val windUnitValues = resources.getStringArray(R.array.wind_unit_values)
        val windUnitIndex = windUnitValues.indexOf(savedWindUnit)
        if (windUnitIndex != -1) {
            windUnitSpinner.setSelection(windUnitIndex)
        }
        // Load other preferences
    autoLocationSwitch.isChecked = sharedPreferences.getBoolean("auto_location_enabled", true)
        timeFormatSwitch.isChecked = sharedPreferences.getBoolean("use_24_hour_format", false)
    }

    private fun setupListeners() {
        unitSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val unitValues = resources.getStringArray(R.array.unit_values)
                val selectedUnit = unitValues[position]
                sharedPreferences.edit { putString("unit", selectedUnit) }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
        windUnitSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val windUnitValues = resources.getStringArray(R.array.wind_unit_values)
                val selectedWindUnit = windUnitValues[position]
                sharedPreferences.edit { putString("wind_unit", selectedWindUnit) }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        // precision switch removed; nothing to do here
        timeFormatSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean("use_24_hour_format", isChecked) }
        }
        autoLocationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean("auto_location_enabled", isChecked) }
        }
        clearLocationsButton.setOnClickListener {
            // Clear saved locations in both SunwiseSettings and addressPref
            sharedPreferences.edit { remove("saved_locations") }
            val addressPrefs = requireContext().getSharedPreferences("addressPref", Context.MODE_PRIVATE)
            addressPrefs.edit { remove("saved_locations") }
            Toast.makeText(requireContext(), "Saved locations cleared", Toast.LENGTH_SHORT).show()
        }
        feedbackButton.setOnClickListener {
            val appPackageName: String? = "com.venomdevelopment.sunwise"
            Log.d("SettingsFragment", "appPackageName:$appPackageName")
            val marketIntent =
                Intent(Intent.ACTION_VIEW, ("market://details?id=$appPackageName").toUri())
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(marketIntent)
        }
    }
}