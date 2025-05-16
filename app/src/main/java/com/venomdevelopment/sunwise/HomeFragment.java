package com.venomdevelopment.sunwise;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HomeFragment extends Fragment implements SavedLocationAdapter.OnLocationClickListener {

    private static final String TAG = "HomeFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse?format=jsonv2";
    private static final String USER_AGENT = "SunwiseApp";
    private static final String PREF_SAVED_LOCATIONS = "saved_locations";

    private EditText search;
    private Button searchButton;
    private Button locationButton;
    private TextView locationTextView;
    private RecyclerView savedLocationsRecyclerView;
    private SavedLocationAdapter savedLocationAdapter;
    private List<String> savedLocationsList = new ArrayList<>();
    private LottieAnimationView animationViewHome;
    public static final String myPref = "addressPref";
    private LocationManager locationManager;
    private RequestQueue requestQueue;
    private String currentDetectedLocation = "";

    public String getPreferenceValue() {
        SharedPreferences sp = requireActivity().getSharedPreferences(myPref, 0);
        String str = sp.getString("address", "");
        return str;
    }

    public void writeToPreference(String thePreference) {
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(myPref, 0).edit();
        editor.putString("address", thePreference);
        editor.apply();
    }

    private Set<String> getSavedLocations() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(myPref, 0);
        return prefs.getStringSet(PREF_SAVED_LOCATIONS, new HashSet<>());
    }

    private void saveSavedLocations(Set<String> locations) {
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(myPref, 0).edit();
        editor.putStringSet(PREF_SAVED_LOCATIONS, locations);
        editor.apply();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        search = v.findViewById(R.id.text_search);
        searchButton = v.findViewById(R.id.search);
        locationButton = v.findViewById(R.id.locationButton);
        locationTextView = v.findViewById(R.id.locationTextView);
        savedLocationsRecyclerView = v.findViewById(R.id.savedLocationsRecyclerView);
        animationViewHome = v.findViewById(R.id.animation_view);
        requestQueue = Volley.newRequestQueue(requireContext());

        savedLocationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        savedLocationAdapter = new SavedLocationAdapter(savedLocationsList, this);
        savedLocationsRecyclerView.setAdapter(savedLocationAdapter);
        loadSavedLocations();

        searchButton.setOnClickListener(v1 -> {
            String address = search.getText().toString().trim();
            if (!address.isEmpty()) {
                setLocationAndNavigateToForecast(address);
            } else {
                Toast.makeText(requireContext(), "Please enter an address", Toast.LENGTH_SHORT).show();
            }
        });

        locationButton.setOnClickListener(v1 -> {
            if (checkLocationPermission()) {
                getCurrentLocation();
            } else {
                requestLocationPermission();
            }
        });

        search.setText(getPreferenceValue(), TextView.BufferType.EDITABLE);

        return v;
    }

    private void loadSavedLocations() {
        Set<String> savedSet = getSavedLocations();
        savedLocationsList.clear();
        savedLocationsList.addAll(savedSet);
        savedLocationAdapter.notifyDataSetChanged();
    }

    private void updateSavedLocations(String location) {
        Set<String> savedSet = getSavedLocations();
        if (savedSet.contains(location)) {
            savedSet.remove(location); // Move to top
        }
        savedSet.add(location);
        saveSavedLocations(savedSet);
        loadSavedLocations(); // Reload to update the order
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            locationTextView.setText("Detecting Location...");
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER);
            if (lastKnownLocation != null) {
                reverseGeocode(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            } else {
                locationManager.requestSingleUpdate(LocationManager.FUSED_PROVIDER, locationListener, null);
            }
        } else {
            locationTextView.setText("Could not access location service.");
        }
    }

    private final android.location.LocationListener locationListener = new android.location.LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            reverseGeocode(location.getLatitude(), location.getLongitude());
            locationManager.removeUpdates(this);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {}

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            locationTextView.setText("Location services disabled.");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    private void reverseGeocode(double latitude, double longitude) {
        String url = String.format(Locale.US, "%s&lat=%f&lon=%f", NOMINATIM_REVERSE_URL, latitude, longitude);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String displayName = response.getString("display_name");
                        locationTextView.setText(displayName);
                        currentDetectedLocation = displayName;
                        setLocationAndNavigateToForecast(displayName);
                        updateSavedLocations(displayName);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing reverse geocoding response: " + e.getMessage());
                        locationTextView.setText("Error getting location name.");
                    }
                }, error -> {
            Log.e(TAG, "Reverse geocoding error: " + error.toString());
            locationTextView.setText("Error getting location name.");
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", USER_AGENT);
                return headers;
            }
        };
        requestQueue.add(jsonObjectRequest);
    }

    public interface OnNavigateToForecastListener {
        void onNavigateToForecast();
    }

    private OnNavigateToForecastListener navigateToForecastListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNavigateToForecastListener) {
            navigateToForecastListener = (OnNavigateToForecastListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement OnNavigateToForecastListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigateToForecastListener = null;
    }

    private void setLocationAndNavigateToForecast(String location) {
        writeToPreference(location);
        updateSavedLocations(location);

        // Notify the activity to select the forecast item in the BottomNavigationView
        if (navigateToForecastListener != null) {
            navigateToForecastListener.onNavigateToForecast();
        }

        // Using FragmentTransaction for navigation
        getParentFragmentManager().beginTransaction()
        .setCustomAnimations(
                        R.anim.slide_in,  // enter
                        R.anim.fade_out,  // exit
                        R.anim.fade_in,   // popEnter
                        R.anim.slide_out  // popExit
                )
                .replace(R.id.flFragment, new ForecastFragment(), "forecastFragment")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onLocationClick(String location) {
        setLocationAndNavigateToForecast(location);
    }
}