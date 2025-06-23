package com.venomdevelopment.sunwise;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        HomeFragment.OnNavigateToForecastListener {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;
    private FragmentManager fragmentManager;
    private String currentDrawerFragmentTag = "home"; // Track current drawer fragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply theme before setting content view
        applyTheme();
        
        setContentView(R.layout.activity_main);
        
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
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (fragmentManager.getBackStackEntryCount() > 0) {
            // If we're in a forecast fragment, go back to the drawer fragment
            fragmentManager.popBackStack();
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
            // Clear back stack when navigating from drawer
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            loadFragment(fragment, tag);
            navigationView.setCheckedItem(id);
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        // Hide all existing fragments
        Fragment homeFragment = fragmentManager.findFragmentByTag("home");
        Fragment alertsFragment = fragmentManager.findFragmentByTag("alerts");
        Fragment settingsFragment = fragmentManager.findFragmentByTag("settings");
        Fragment snowFragment = fragmentManager.findFragmentByTag("snow_day");
        
        if (homeFragment != null) transaction.hide(homeFragment);
        if (alertsFragment != null) transaction.hide(alertsFragment);
        if (settingsFragment != null) transaction.hide(settingsFragment);
        if (snowFragment != null) transaction.hide(snowFragment);
        
        // Show or add the target fragment
        Fragment existingFragment = fragmentManager.findFragmentByTag(tag);
        if (existingFragment != null) {
            transaction.show(existingFragment);
        } else {
            transaction.add(R.id.fragment_container, fragment, tag);
        }
        
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
        transaction.replace(R.id.fragment_container, forecastFragment, "forecast");
        transaction.addToBackStack("forecast");
        transaction.commit();
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences("SunwiseSettings", Context.MODE_PRIVATE);
        boolean darkModeEnabled = prefs.getBoolean("dark_mode_enabled", false);
        
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
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
}