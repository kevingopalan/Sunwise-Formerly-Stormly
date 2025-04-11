package com.venomdevelopment.sunwise;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsFragment extends Fragment {

    private RecyclerView recyclerView;
    private NewsAdapter newsAdapter;
    private List<NewsItem> newsItems;
    private int currentPage = 1;

    // Replace with your actual API key and endpoint (NewsAPI example)
    private static final String API_URL = "https://newsapi.org/v2/everything?q=weather%20AND%20\"United%20States\"&pageSize=10&language=en&page=";
    private static final String API_KEY = "14ef10f0746447068251aff9f102e2de"; // Replace with your API key from NewsAPI

    public NewsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_news, container, false);

        recyclerView = rootView.findViewById(R.id.newsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        newsItems = new ArrayList<>();
        newsAdapter = new NewsAdapter(newsItems);
        recyclerView.setAdapter(newsAdapter);

        loadNewsData(currentPage);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1)) {
                    // Load more data when scrolled to the bottom
                    loadNewsData(currentPage++);
                }
            }
        });

        return rootView;
    }

    private void loadNewsData(int page) {
        LocalDate localDate = LocalDate.now();
        LocalDate yesterdayDate = localDate.minusDays(1);
        String url = API_URL + page + "&apiKey=" + API_KEY + "&from=" + yesterdayDate.toString();

        // Create a new JsonObjectRequest with a custom User-Agent
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray articles = response.getJSONArray("articles");

                            // Parse the JSON response
                            for (int i = 0; i < articles.length(); i++) {
                                JSONObject newsObject = articles.getJSONObject(i);

                                String title = newsObject.getString("title");
                                String description = newsObject.getString("description");
                                String imageUrl = newsObject.getString("urlToImage");
                                String url = newsObject.getString("url");

                                // Create a NewsItem and add it to the list
                                NewsItem newsItem = new NewsItem(title, description, imageUrl);
                                newsItems.add(newsItem);
                            }

                            // Notify the adapter that data has changed
                            newsAdapter.notifyDataSetChanged();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), "Error parsing JSON data", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(com.android.volley.VolleyError error) {
                        Toast.makeText(getActivity(), "Error fetching news", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
                // Create a new HashMap to store custom headers
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                return headers;
            }
        };

        // Add the request to the RequestQueue
        Volley.newRequestQueue(getActivity()).add(jsonObjectRequest);
    }
}
