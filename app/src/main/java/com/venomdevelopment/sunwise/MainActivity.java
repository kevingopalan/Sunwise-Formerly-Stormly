package com.venomdevelopment.sunwise;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        HomeFragment.OnNavigateToForecastListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FragmentManager fragmentManager;
    private AdView adView;
    private InterstitialAd mInterstitialAd;
    private int fragmentSwitchCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if onboarding is completed
        SharedPreferences prefs = getSharedPreferences("SunwiseSettings", MODE_PRIVATE);
        boolean onboardingCompleted = prefs.getBoolean("onboarding_completed", false);

        if (!onboardingCompleted) {
            // Start onboarding
            Intent onboardingIntent = new Intent(this, OnboardingActivity.class);
            startActivity(onboardingIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Initialize MobileAds
        MobileAds.initialize(this, initializationStatus -> {
            // Initialization completed
        });

        // Setup AdView
        setupAdView();

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        toolbar.setNavigationIcon(R.drawable.baseline_menu_24);
        toolbar.setNavigationContentDescription(R.string.navigation_drawer_open);
        toolbar.setNavigationOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.START));

        // Get fragment manager
        fragmentManager = getSupportFragmentManager();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Set default fragment (Home)
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment(), "home");
            navigationView.setCheckedItem(R.id.nav_home);
        }
//        showTesterDialog();
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
        if (fragmentSwitchCount % 5 == 0 && mInterstitialAd != null) {
            mInterstitialAd.show(this);
            mInterstitialAd = null;
            loadInterstitialAd();
        }
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        String tag = "";
        
        if (item.getItemId() == R.id.nav_home) {
            fragment = new HomeFragment();
            tag = "home";
        } else if (item.getItemId() == R.id.nav_alerts) {
            fragment = new FragmentAlerts();
            tag = "alerts";
        } else if (item.getItemId() == R.id.nav_settings) {
            fragment = new SettingsFragment();
            tag = "settings";
        }

        if (fragment != null) {
            maybeShowInterstitialAd();
            replaceFragment(fragment, tag);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void replaceFragment(Fragment fragment, String tag) {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.commit();
    }

    @Override
    public void onNavigateToForecast(String location) {
        ForecastFragment forecastFragment = new ForecastFragment();
        Bundle args = new Bundle();
        args.putString("location", location);
        forecastFragment.setArguments(args);

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.addToBackStack(null);
        transaction.replace(R.id.fragment_container, forecastFragment, "forecast");
        transaction.commit();
    }

    private void showTesterDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Hey there!")
                .setMessage("Thank you for signing up for our test! You are some of the first people to use this software, and any feedback you submit is highly appreciated. \n \n" +
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