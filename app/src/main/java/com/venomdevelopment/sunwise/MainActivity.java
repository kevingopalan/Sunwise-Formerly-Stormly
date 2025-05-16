package com.venomdevelopment.sunwise;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener,
        HomeFragment.OnNavigateToForecastListener {
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        // Set the initial selected item (likely Home)
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.home);
            loadFragment(new HomeFragment(), "homeFragment"); // Assuming loadFragment handles the initial transaction
        }
        bottomNavigationView.setSelectedItemId(R.id.home); // Set the initial selected item

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.statusbarcolor));
        window.setNavigationBarColor(this.getResources().getColor(R.color.statusbarcolor));

        // Load the initial fragment
        loadFragment(new HomeFragment(), "homeFragment");
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        androidx.fragment.app.Fragment fragment = null;
        String tag = null;

        switch (item.getItemId()) {
            case R.id.menu:
                fragment = new MenuFragment();
                tag = "menuFragment";
                break;
            case R.id.home:
                fragment = new HomeFragment();
                tag = "homeFragment";
                break;
            case R.id.forecast:
                fragment = new ForecastFragment();
                tag = "forecastFragment";
                break;
            // Add cases for other menu items and their corresponding fragments
        }

        if (fragment != null) {
            loadFragment(fragment, tag);
            return true;
        }
        return false;
    }


    public static void setWindowFlag(Activity activity, final int bits, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    @Override
    public void onNavigateToForecast() {
        bottomNavigationView.setSelectedItemId(R.id.forecast);
    }
    private void loadFragment(androidx.fragment.app.Fragment fragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
                        R.anim.slide_in,  // enter
                        R.anim.fade_out,  // exit
                        R.anim.fade_in,   // popEnter
                        R.anim.slide_out  // popExit
                )
                .replace(R.id.flFragment, fragment, tag)
                .commit();
    }
}