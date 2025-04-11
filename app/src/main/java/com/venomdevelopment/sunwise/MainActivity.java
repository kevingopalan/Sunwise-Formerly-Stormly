package com.venomdevelopment.sunwise;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements BottomNavigationView
        .OnNavigationItemSelectedListener {
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        bottomNavigationView
                = findViewById(R.id.bottomNavigationView);

        bottomNavigationView
                .setOnNavigationItemSelectedListener(this);
        bottomNavigationView.setSelectedItemId(R.id.home);
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.statusbarcolor));
        window.setNavigationBarColor(this.getResources().getColor(R.color.statusbarcolor));
    }
    HomeFragment homeFragment = new HomeFragment();
    MenuFragment menuFragment = new MenuFragment();
    ForecastFragment forecastFragment = new ForecastFragment();
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        // Hide the fragments
        FragmentAlerts fragmentAlerts = (FragmentAlerts) fragmentManager.findFragmentByTag("FragmentAlerts");
        if (fragmentAlerts != null) transaction.hide(fragmentAlerts);
        SettingsFragment fragmentSettings = (SettingsFragment) fragmentManager.findFragmentByTag("FragmentSettings");
        if (fragmentSettings != null) transaction.hide(fragmentSettings);
        NewsFragment fragmentNews = (NewsFragment) fragmentManager.findFragmentByTag("FragmentNews");
        if (fragmentNews != null) transaction.hide(fragmentNews);
        SnowDayFragment fragmentSnow = (SnowDayFragment) fragmentManager.findFragmentByTag("FragmentSnow");
        if (fragmentSnow != null) transaction.hide(fragmentSnow);

        switch (item.getItemId()) {
            case R.id.menu:
                if (!menuFragment.isAdded()) {
                    transaction.add(R.id.flFragment, menuFragment, "menuFragment");
                }
                transaction.show(menuFragment)
                        .setCustomAnimations(
                                R.anim.slide_in_left,  // enter
                                R.anim.fade_out,  // exit
                                R.anim.fade_in,   // popEnter
                                R.anim.slide_out_left  // popExit
                        )
                        .hide(homeFragment)
                        .hide(forecastFragment);
                break;

            case R.id.home:
                if (!homeFragment.isAdded()) {
                    transaction.add(R.id.flFragment, homeFragment, "homeFragment");
                }
                transaction.show(homeFragment)
                        .setCustomAnimations(
                                R.anim.slide_in_top,  // enter
                                R.anim.fade_out,  // exit
                                R.anim.fade_in,   // popEnter
                                R.anim.slide_out_top  // popExit
                        )
                        .hide(menuFragment)
                        .hide(forecastFragment);
                break;

            case R.id.forecast:
                if (!forecastFragment.isAdded()) {
                    transaction.add(R.id.flFragment, forecastFragment, "forecastFragment");
                }
                transaction.show(forecastFragment)
                        .setCustomAnimations(
                                R.anim.slide_in,  // enter
                                R.anim.fade_out,  // exit
                                R.anim.fade_in,   // popEnter
                                R.anim.slide_out  // popExit
                        )
                        .hide(menuFragment)
                        .hide(homeFragment);
                break;
        }

        transaction.commit();
        return true;
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
}
