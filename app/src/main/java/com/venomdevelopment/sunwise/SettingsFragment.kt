package com.venomdevelopment.sunwise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {
    private lateinit var unitSpinner: Spinner
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)
        unitSpinner = rootView.findViewById(R.id.unit)
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.unit_entries,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = adapter

        return super.onCreateView(inflater, container, savedInstanceState)
    }
}