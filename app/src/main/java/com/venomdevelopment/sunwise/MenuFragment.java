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

    private Button btnAlerts;
    private Button btnSettings;
    private Button btnSnow;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        btnAlerts = view.findViewById(R.id.btnAlerts);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnSnow = view.findViewById(R.id.btnSnow);

        // Set the click listener for the "Alerts" button
        btnAlerts.setOnClickListener(v -> {
            // Replace MenuFragment with FragmentAlerts
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_top,  // enter
                            R.anim.fade_out,  // exit
                            R.anim.fade_in,   // popEnter
                            R.anim.slide_out_top  // popExit
                    )
                    .replace(R.id.flFragment, new FragmentAlerts(), "alertsFragment") // Replace current fragment in the container
                    .addToBackStack(null) // Optional: Allows back button to return to the fragment that opened MenuFragment
                    .commit();
        });

        btnSettings.setOnClickListener(v -> {
            // Replace MenuFragment with SettingsFragment
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_top,  // enter
                            R.anim.fade_out,  // exit
                            R.anim.fade_in,   // popEnter
                            R.anim.slide_out_top  // popExit
                    )
                    .replace(R.id.flFragment, new SettingsFragment(), "settingsFragment")
                    .addToBackStack(null)
                    .commit();
        });

        btnSnow.setOnClickListener(v -> {
            // Replace MenuFragment with SnowDayFragment
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_top,  // enter
                            R.anim.fade_out,  // exit
                            R.anim.fade_in,   // popEnter
                            R.anim.slide_out_top  // popExit
                    )
                    .replace(R.id.flFragment, new SnowDayFragment(), "snowFragment")
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    // Keep hideOtherFragments() if it's used elsewhere for other fragment management
    // However, it will no longer be called directly by the buttons in this fragment
    private void hideOtherFragments() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        Fragment homeFragment = getFragmentManager().findFragmentByTag("homeFragment");
        Fragment forecastFragment = getFragmentManager().findFragmentByTag("forecastFragment");
        Fragment menuFragment = getFragmentManager().findFragmentByTag("menuFragment"); // This will now be removed by replace()

        if (homeFragment != null) transaction.setCustomAnimations(
                R.anim.slide_in_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top
        ).hide(homeFragment);
        if (forecastFragment != null) transaction.setCustomAnimations(
                R.anim.slide_in_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top
        ).hide(forecastFragment);
        // If MenuFragment is replacing itself, this hide call for menuFragment is now redundant
        if (menuFragment != null) transaction.setCustomAnimations(
                R.anim.slide_in_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top
        ).hide(menuFragment);

        transaction.commit();
    }

    // This method seems to manage other fragments from a navigation item selection.
    // It will still hide the fragments it finds if they are present.
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        Fragment alertsFragment = getFragmentManager().findFragmentByTag("alertsFragment");
        Fragment settingsFragment = getFragmentManager().findFragmentByTag("settingsFragment");
        Fragment newsFragment = getFragmentManager().findFragmentByTag("newsFragment");
        Fragment snowFragment = getFragmentManager().findFragmentByTag("snowFragment");

        if (alertsFragment != null) transaction.setCustomAnimations(
                R.anim.slide_in_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top
        ).hide(alertsFragment);
        // Note: You had chained .hide() calls which might be an issue if alertsFragment is null.
        // It's safer to check each fragment individually before hiding.
        if (settingsFragment != null) transaction.setCustomAnimations(
                R.anim.slide_in_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top
        ).hide(settingsFragment);
        if (newsFragment != null) transaction.setCustomAnimations(
                R.anim.slide_in_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top
        ).hide(newsFragment);
        if (snowFragment != null) transaction.setCustomAnimations(
                R.anim.slide_in_top,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out_top
        ).hide(snowFragment);


        transaction.commit();
        return true;
    }

    // This onStart() method now becomes largely redundant for the fragments
    // that are being replaced, as new instances will be created each time.
    // It might still be relevant if other parts of your app manage these fragments differently.
    @Override
    public void onStart() {
        super.onStart();

        if (getParentFragmentManager() != null) { // Check if getParentFragmentManager() is not null
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();

            Fragment alertsFragment = getParentFragmentManager().findFragmentByTag("alertsFragment");
            Fragment settingsFragment = getParentFragmentManager().findFragmentByTag("settingsFragment");
            Fragment snowFragment = getParentFragmentManager().findFragmentByTag("snowFragment");

            if (alertsFragment != null) {
                transaction.setCustomAnimations(
                        R.anim.slide_in_top,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.slide_out_top
                ).hide(alertsFragment);
            }
            if (settingsFragment != null) {
                transaction.setCustomAnimations(
                        R.anim.slide_in_top,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.slide_out_top
                ).hide(settingsFragment);
            }
            if (snowFragment != null) {
                transaction.setCustomAnimations(
                        R.anim.slide_in_top,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.slide_out_top
                ).hide(snowFragment);
            }
            // Only commit if there are pending operations
            if (!transaction.isEmpty()) {
                transaction.commitAllowingStateLoss(); // Use commitAllowingStateLoss if there's a chance of state loss
            }
        }
    }
}