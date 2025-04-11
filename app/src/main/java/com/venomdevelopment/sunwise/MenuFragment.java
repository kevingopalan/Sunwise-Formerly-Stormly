package com.venomdevelopment.sunwise;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

public class MenuFragment extends Fragment {

    private Button btnAlerts;  // Declare the alerts button
    private Button btnSettings;
    private Button btnSnow;
    private FragmentAlerts fragmentAlerts;  // Reference to FragmentAlerts
    private SettingsFragment fragmentSettings;
    private SnowDayFragment fragmentSnow;
    private boolean isFragmentAlertsAdded = false;  // Track if FragmentAlerts is added
    private boolean isFragmentSettingsAdded = false;
    private boolean isFragmentSnowAdded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        // Initialize the button from the layout
        btnAlerts = view.findViewById(R.id.btnAlerts);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnSnow = view.findViewById(R.id.btnSnow);

        // Set the click listener for the "Alerts" button
        btnAlerts.setOnClickListener(v -> {
            // Hide the other fragments
            hideOtherFragments();

            // Show FragmentAlerts
            showFragmentAlerts();
        });
        btnSettings.setOnClickListener(v -> {
            // Hide the other fragments
            hideOtherFragments();
            showFragmentSettings();
        });
        btnSnow.setOnClickListener(v -> {
            // Hide the other fragments
            hideOtherFragments();
            showFragmentSnow();
        });

        return view;
    }

    // Hide the other fragments (home, forecast, and menu)
    private void hideOtherFragments() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Get the fragments from the fragment manager
        Fragment homeFragment = getFragmentManager().findFragmentByTag("homeFragment");
        Fragment forecastFragment = getFragmentManager().findFragmentByTag("forecastFragment");
        Fragment menuFragment = getFragmentManager().findFragmentByTag("menuFragment");

        // Hide the fragments
        if (homeFragment != null) transaction.setCustomAnimations(
                R.anim.slide_in_top,  // enter
                R.anim.fade_out,  // exit
                R.anim.fade_in,   // popEnter
                R.anim.slide_out_top  // popExit
        ).hide(homeFragment);
        if (forecastFragment != null) transaction.setCustomAnimations(
                R.anim.slide_in_top,  // enter
                R.anim.fade_out,  // exit
                R.anim.fade_in,   // popEnter
                R.anim.slide_out_top  // popExit
        ).hide(forecastFragment);
        if (menuFragment != null) transaction.setCustomAnimations(
                R.anim.slide_in_top,  // enter
                R.anim.fade_out,  // exit
                R.anim.fade_in,   // popEnter
                R.anim.slide_out_top  // popExit
        ).hide(menuFragment);

        // Commit the transaction
        transaction.commit();
    }

    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Get the fragments from the fragment manager
        Fragment alertsFragment = getFragmentManager().findFragmentByTag("alertsFragment");
        Fragment settingsFragment = getFragmentManager().findFragmentByTag("settingsFragment");
        Fragment newsFragment = getFragmentManager().findFragmentByTag("newsFragment");
        Fragment snowFragment = getFragmentManager().findFragmentByTag("snowFragment");

        // Hide the fragments
        if (alertsFragment != null) transaction.setCustomAnimations(
                R.anim.slide_in_top,  // enter
                R.anim.fade_out,  // exit
                R.anim.fade_in,   // popEnter
                R.anim.slide_out_top  // popExit
        ).hide(alertsFragment)
        .hide(settingsFragment)
        .hide(newsFragment)
        .hide(snowFragment);

        // Commit the transaction
        transaction.commit();
        return true;
    }

    // Show the FragmentAlerts (ensure it's added only once)
    private void showFragmentAlerts() {
        if (!isFragmentAlertsAdded) {
            fragmentAlerts = new FragmentAlerts();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.add(R.id.flFragment, fragmentAlerts, "FragmentAlerts");
            transaction.setCustomAnimations(
                    R.anim.slide_in_top,  // enter
                    R.anim.fade_out,  // exit
                    R.anim.fade_in,   // popEnter
                    R.anim.slide_out_top  // popExit
            );
            transaction.hide(fragmentAlerts); // Initially hide FragmentAlerts
            transaction.commit();
            isFragmentAlertsAdded = true;
        }

        // Now, show FragmentAlerts
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.anim.slide_in_top,  // enter
                R.anim.fade_out,  // exit
                R.anim.fade_in,   // popEnter
                R.anim.slide_out_top  // popExit
        ).show(fragmentAlerts);  // Show FragmentAlerts
        transaction.commit();
    }

    private void showFragmentSettings() {
        if (!isFragmentSettingsAdded) {
            fragmentSettings = new SettingsFragment();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.add(R.id.flFragment, fragmentSettings, "FragmentSettings");
            transaction.setCustomAnimations(
                    R.anim.slide_in_top,  // enter
                    R.anim.fade_out,  // exit
                    R.anim.fade_in,   // popEnter
                    R.anim.slide_out_top  // popExit
            );
            transaction.hide(fragmentSettings); // Initially hide FragmentSettings
            transaction.commit();
            isFragmentSettingsAdded = true;
        }

        // Now, show FragmentSettings
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.anim.slide_in_top,  // enter
                R.anim.fade_out,  // exit
                R.anim.fade_in,   // popEnter
                R.anim.slide_out_top  // popExit
        ).show(fragmentSettings);  // Show FragmentAlerts
        transaction.commit();
    }
    private void showFragmentSnow() {
        if (!isFragmentSnowAdded) {
            fragmentSnow = new SnowDayFragment();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.add(R.id.flFragment, fragmentSnow, "FragmentSnow");
            transaction.setCustomAnimations(
                    R.anim.slide_in_top,  // enter
                    R.anim.fade_out,  // exit
                    R.anim.fade_in,   // popEnter
                    R.anim.slide_out_top  // popExit
            );
            transaction.hide(fragmentSnow); // Initially hide FragmentSnow
            transaction.commit();
            isFragmentSnowAdded = true;
        }
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.anim.slide_in_top,  // enter
                R.anim.fade_out,  // exit
                R.anim.fade_in,   // popEnter
                R.anim.slide_out_top  // popExit
        ).show(fragmentSnow);  // Show FragmentSnow
        transaction.commit();
    }



    // Optional: Hide FragmentAlerts when you navigate back to the MenuFragment
    @Override
    public void onStart() {
        super.onStart();

        // Make sure FragmentAlerts is hidden when we come back to the MenuFragment
        if (fragmentAlerts != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    R.anim.slide_in_top,  // enter
                    R.anim.fade_out,  // exit
                    R.anim.fade_in,   // popEnter
                    R.anim.slide_out_top  // popExit
            );
            transaction.hide(fragmentAlerts);
            transaction.commit();
        }
        if (fragmentSettings != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    R.anim.slide_in_top,  // enter
                    R.anim.fade_out,  // exit
                    R.anim.fade_in,   // popEnter
                    R.anim.slide_out_top  // popExit
            );
            transaction.hide(fragmentSettings);
            transaction.commit();
        }
        if (fragmentSnow != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    R.anim.slide_in_top,  // enter
                    R.anim.fade_out,  // exit
                    R.anim.fade_in,   // popEnter
                    R.anim.slide_out_top  // popExit
            );
            transaction.hide(fragmentSnow);
            transaction.commit();
        }
    }
}
