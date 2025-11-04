package com.venomdevelopment.sunwise;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        HomeFragment.OnNavigateToForecastListener {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;
    private FragmentManager fragmentManager;
    private AdView adView;
    private String currentDrawerFragmentTag = "home"; // Track current drawer fragment
    private InterstitialAd mInterstitialAd;
    private int fragmentSwitchCount = 0;
    private Fragment pendingFragment = null;
    private String pendingTag = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Initialize MobileAds
        MobileAds.initialize(this, initializationStatus -> {
            // Initialization completed
        });

        // Setup AdView
        setupAdView();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Setup drawer toggle
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Get fragment manager
        fragmentManager = getSupportFragmentManager();

        // Set default fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "home");
            navigationView.setCheckedItem(R.id.nav_home);
        } else {
            // Restore the current drawer fragment if it exists
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
            if (currentFragment == null) {
                // No fragment exists, restore the home fragment
                loadFragment(new HomeFragment(), "home");
                navigationView.setCheckedItem(R.id.nav_home);
            } else {
                // Fragment exists, update the navigation selection based on the fragment tag
                String fragmentTag = currentFragment.getTag();
                if (fragmentTag != null) {
                    currentDrawerFragmentTag = fragmentTag;
                    switch (fragmentTag) {
                        case "home":
                            navigationView.setCheckedItem(R.id.nav_home);
                            break;
                        case "alerts":
                            navigationView.setCheckedItem(R.id.nav_alerts);
                            break;
                        case "settings":
                            navigationView.setCheckedItem(R.id.nav_settings);
                            break;
                        case "snow_day":
                            navigationView.setCheckedItem(R.id.nav_snow_day);
                            break;
                        default:
                            navigationView.setCheckedItem(R.id.nav_home);
                            break;
                    }
                }
            }
        }
        loadInterstitialAd();
        showTesterDialog();
    }

    private void setupAdView() {
        adView = findViewById(R.id.banner_ad_view);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                android.util.Log.d("MainActivity", "MainActivity ad loaded successfully");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                android.util.Log.e("MainActivity", "MainActivity ad failed to load: " + adError.getMessage());
            }
        });
    }

    private void loadInterstitialAd() {
        com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
        String adUnitId = getString(R.string.ad_unit_interstitial);
        InterstitialAd.load(this, adUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                android.util.Log.d("MainActivity", "Interstitial ad loaded");
            }

            @Override
            public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError adError) {
                mInterstitialAd = null;
                android.util.Log.e("MainActivity", "Interstitial ad failed to load: " + adError.getMessage());
            }
        });
    }

    private void maybeShowInterstitialAd() {
        fragmentSwitchCount++;
        if (fragmentSwitchCount % 3 == 0 && mInterstitialAd != null) {
            mInterstitialAd.show(this);
            mInterstitialAd = null;
            loadInterstitialAd();
        }
    }

    private void maybeShowInterstitialAdOrSwitch(Fragment fragment, String tag, int navId) {
        fragmentSwitchCount++;
        if (fragmentSwitchCount % 3 == 0 && mInterstitialAd != null) {
            pendingFragment = fragment;
            pendingTag = tag;
            navigationView.setCheckedItem(navId);
            mInterstitialAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mInterstitialAd = null;
                    loadInterstitialAd();
                    switchFragmentAfterAd();
                }
            });
            mInterstitialAd.show(this);
        } else {
            switchFragment(fragment, tag, navId);
        }
    }

    private void switchFragmentAfterAd() {
        if (pendingFragment != null && pendingTag != null) {
            switchFragment(pendingFragment, pendingTag, -1);
            pendingFragment = null;
            pendingTag = null;
        }
    }

    private void switchFragment(Fragment fragment, String tag, int navId) {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.commit();
        currentDrawerFragmentTag = tag;
        if (navId != -1) navigationView.setCheckedItem(navId);
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (fragmentManager.getBackStackEntryCount() > 0) {
            // If we're in a forecast fragment, go back to the drawer fragment
            fragmentManager.popBackStack();

            // Recreate the appropriate drawer fragment since it was destroyed
            Fragment fragment = null;
            int navItemId = R.id.nav_home; // Default to home

            switch (currentDrawerFragmentTag) {
                case "home":
                    fragment = new HomeFragment();
                    navItemId = R.id.nav_home;
                    break;
                case "alerts":
                    fragment = new FragmentAlerts();
                    navItemId = R.id.nav_alerts;
                    break;
                case "settings":
                    fragment = new SettingsFragment();
                    navItemId = R.id.nav_settings;
                    break;
                case "snow_day":
                    fragment = new SnowDayFragment();
                    navItemId = R.id.nav_snow_day;
                    break;
            }

            if (fragment != null) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, fragment, currentDrawerFragmentTag);
                transaction.commit();
                navigationView.setCheckedItem(navItemId);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        String tag = "";

        if (id == R.id.nav_home) {
            fragment = new HomeFragment();
            tag = "home";
        } else if (id == R.id.nav_alerts) {
            fragment = new FragmentAlerts();
            tag = "alerts";
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
            tag = "settings";
        } else if (id == R.id.nav_snow_day) {
            fragment = new SnowDayFragment();
            tag = "snow_day";
        }

        if (fragment != null) {
            maybeShowInterstitialAdOrSwitch(fragment, tag, id);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Remove all existing fragments instead of hiding them
        Fragment homeFragment = fragmentManager.findFragmentByTag("home");
        Fragment alertsFragment = fragmentManager.findFragmentByTag("alerts");
        Fragment settingsFragment = fragmentManager.findFragmentByTag("settings");
        Fragment snowFragment = fragmentManager.findFragmentByTag("snow_day");

        if (homeFragment != null) transaction.remove(homeFragment);
        if (alertsFragment != null) transaction.remove(alertsFragment);
        if (settingsFragment != null) transaction.remove(settingsFragment);
        if (snowFragment != null) transaction.remove(snowFragment);

        // Add the new fragment
        transaction.add(R.id.fragment_container, fragment, tag);

        // Track the current drawer fragment
        currentDrawerFragmentTag = tag;

        transaction.commit();
    }

    @Override
    public void onNavigateToForecast(String location) {
        ForecastFragment forecastFragment = new ForecastFragment();
        Bundle args = new Bundle();
        args.putString("location", location);
        forecastFragment.setArguments(args);

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Remove the current drawer fragment to free up memory
        Fragment currentFragment = fragmentManager.findFragmentByTag(currentDrawerFragmentTag);
        if (currentFragment != null) {
            transaction.remove(currentFragment);
        }

        transaction.replace(R.id.fragment_container, forecastFragment, "forecast");
        transaction.commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private void showTesterDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Hey there!")
                .setMessage("Thank you for signing up for our closed test! You are one of the first people to use this software, and any feedback you submit is highly appreciated. \n \n" +
                        "Here are some things you may want to know: \n \n" +
                        "- This app is going to be targeted to the US only during production/open testing. This is due to a limitation with the APIs we use, which include the National Weather Service. \n \n" +
                        "- This app will have bugs, so please leave feedback, it helps improve Sunwise and brings this app one step closer to production. \n \n" +
                        "- You are signing up for a closed test, and things can change at any time. What you see here is a pre-release version of Sunwise and is not the final product.\n \n" +
                        "- Please keep this app installed and give constructive feedback on Google Play. Don't feel like you have to be super nice and sugarcoat all your feedback, being more critical helps improve this app. \n \n" +
                        "Thank you for taking the time to read this message, have a nice day.")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
}