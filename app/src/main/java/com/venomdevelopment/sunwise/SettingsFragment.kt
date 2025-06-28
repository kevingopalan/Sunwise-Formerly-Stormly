package com.venomdevelopment.sunwise

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import android.widget.CompoundButton

class SettingsFragment : Fragment() {
    private lateinit var unitSpinner: Spinner
    private lateinit var windUnitSpinner: Spinner
//    private lateinit var notificationsSwitch: Switch
    private lateinit var darkModeSwitch: Switch
    private lateinit var autoLocationSwitch: Switch
    private lateinit var precisionSwitch: Switch
    private lateinit var timeFormatSwitch: Switch
    private lateinit var clearLocationsButton: Button
    private lateinit var feedbackButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private val darkModeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        val currentPref = sharedPreferences.getBoolean("dark_mode_enabled", false)
        if (isChecked != currentPref) {
            sharedPreferences.edit().putBoolean("dark_mode_enabled", isChecked).apply()
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
            sharedPreferences.edit().putString("last_fragment_tag", "settingsFragment").apply()
            // Update nav bar color (optional)
            val window = requireActivity().window
            val isDark = mode == AppCompatDelegate.MODE_NIGHT_YES ||
                (mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM &&
                 (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES)
            val navColor = if (isDark) {
                requireContext().getColor(R.color.md_theme_background)
            } else {
                requireContext().getColor(R.color.md_theme_background)
            }
            window.navigationBarColor = navColor
            requireActivity().recreate()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)
        sharedPreferences = requireContext().getSharedPreferences("SunwiseSettings", Context.MODE_PRIVATE)
        unitSpinner = rootView.findViewById(R.id.unit)
        windUnitSpinner = rootView.findViewById(R.id.windUnitSpinner)
        // notificationsSwitch = rootView.findViewById(R.id.notificationsSwitch)
        darkModeSwitch = rootView.findViewById(R.id.darkModeSwitch)
        autoLocationSwitch = rootView.findViewById(R.id.autoLocationSwitch)
        precisionSwitch = rootView.findViewById(R.id.precisionSwitch)
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
        // Set dark mode switch to match current theme, but don't trigger listener
        darkModeSwitch.setOnCheckedChangeListener(null)
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        darkModeSwitch.isChecked = when (nightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> sharedPreferences.getBoolean("dark_mode_enabled", false)
        }
        darkModeSwitch.setOnCheckedChangeListener(darkModeListener)
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
//        notificationsSwitch.isChecked = sharedPreferences.getBoolean("notifications_enabled", true)
        autoLocationSwitch.isChecked = sharedPreferences.getBoolean("auto_location_enabled", true)
        precisionSwitch.isChecked = sharedPreferences.getBoolean("show_decimal_temp", false)
        timeFormatSwitch.isChecked = sharedPreferences.getBoolean("use_24_hour_format", false)
    }

    private fun setupListeners() {
        unitSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val unitValues = resources.getStringArray(R.array.unit_values)
                val selectedUnit = unitValues[position]
                sharedPreferences.edit().putString("unit", selectedUnit).apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
        windUnitSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val windUnitValues = resources.getStringArray(R.array.wind_unit_values)
                val selectedWindUnit = windUnitValues[position]
                sharedPreferences.edit().putString("wind_unit", selectedWindUnit).apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
//        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
//            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
//            if (isChecked) {
//                Toast.makeText(requireContext(), "Weather notifications enabled (demo)", Toast.LENGTH_SHORT).show()
//            }
//        }
        precisionSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("show_decimal_temp", isChecked).apply()
        }
        timeFormatSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("use_24_hour_format", isChecked).apply()
        }
        darkModeSwitch.setOnCheckedChangeListener(darkModeListener)
        autoLocationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("auto_location_enabled", isChecked).apply()
        }
        clearLocationsButton.setOnClickListener {
            // Clear saved locations in both SunwiseSettings and addressPref
            sharedPreferences.edit().remove("saved_locations").apply()
            val addressPrefs = requireContext().getSharedPreferences("addressPref", Context.MODE_PRIVATE)
            addressPrefs.edit().remove("saved_locations").apply()
            Toast.makeText(requireContext(), "Saved locations cleared", Toast.LENGTH_SHORT).show()
        }
        feedbackButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("venomdevelopmentofficial@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Sunwise Feedback")
            }
            startActivity(Intent.createChooser(intent, "Send Feedback"))
        }
    }
}