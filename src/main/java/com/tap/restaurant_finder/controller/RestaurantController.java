package com.tap.restaurant_finder.controller;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.tap.restaurant_finder.model.Restaurant;

@Controller
public class RestaurantController {

    @Value("${google.api.key}")
    private String apiKey;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/search")
    public String search(@RequestParam("city") String city, Model model) {
        List<Restaurant> restaurants = new ArrayList<>();
        String errorMessage = null;

        try {
            String url = String.format(
                "https://maps.googleapis.com/maps/api/place/textsearch/json?query=%s&key=%s",
                URLEncoder.encode("restaurants in " + city, "UTF-8"),
                apiKey
            );

            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            JSONObject json = new JSONObject(response);
            String status = json.getString("status");

            switch (status) {
                case "REQUEST_DENIED":
                    errorMessage = "API key not active or billing not enabled. Please enable billing in Google Cloud.";
                    break;
                case "ZERO_RESULTS":
                    errorMessage = "No restaurants found in " + city;
                    break;
                case "OK":
                    JSONArray results = json.getJSONArray("results");
                    for (int i = 0; i < Math.min(results.length(), 10); i++) {
                        JSONObject obj = results.getJSONObject(i);
                        String name = obj.optString("name", "Unknown");
                        String address = obj.optString("formatted_address", "Address not available");
                        double rating = obj.optDouble("rating", 0.0);

                        restaurants.add(new Restaurant(name, address, rating));
                    }
                    break;
                default:
                    errorMessage = "Unexpected API response: " + status;
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = "Failed to fetch data from Google Places API.";
        }

        model.addAttribute("restaurants", restaurants);
        model.addAttribute("city", city);
        model.addAttribute("error", errorMessage);

        return "index";
    }
}
