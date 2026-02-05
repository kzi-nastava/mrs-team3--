package com.example.uber3.repository;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.uber3.network.service.ORSService;
import com.example.uber3.network.api.ORSRetrofitClient;

public class ORSRepository {

    private static final String API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjI2YjEzYWE0MjBjOWE5NDU1YWJlNzI1Y2Q1MjFhMmZmNmQ3NTM0YjcwMzk2NmRlNzBmMDIwZmJlIiwiaCI6Im11cm11cjY0In0=";

    public interface RouteCallback {
        void onRouteReady(List<GeoPoint> points);
    }

    public static void getRoute(
            List<GeoPoint> points,
            RouteCallback callback
    ) {

        try {

            JSONObject body = new JSONObject();
            JSONArray coords = new JSONArray();

            for (GeoPoint p : points) {
                JSONArray arr = new JSONArray();
                arr.put(p.getLongitude());
                arr.put(p.getLatitude());
                coords.put(arr);
            }

            body.put("coordinates", coords);

            RequestBody requestBody =
                    RequestBody.create(
                            body.toString(),
                            MediaType.parse("application/json")
                    );

            ORSService service =
                    ORSRetrofitClient
                            .getInstance()
                            .create(ORSService.class);

            service.getRoute(API_KEY, requestBody)
                    .enqueue(new Callback<String>() {

                        @Override
                        public void onResponse(
                                Call<String> call,
                                Response<String> response
                        ) {

                            List<GeoPoint> routePoints =
                                    new ArrayList<>();

                            try {

                                JSONObject json =
                                        new JSONObject(
                                                response.body()
                                        );

                                JSONArray coordsArr =
                                        json.getJSONArray("features")
                                                .getJSONObject(0)
                                                .getJSONObject("geometry")
                                                .getJSONArray("coordinates");

                                for (int i = 0; i < coordsArr.length(); i++) {

                                    JSONArray c =
                                            coordsArr.getJSONArray(i);

                                    double lon = c.getDouble(0);
                                    double lat = c.getDouble(1);

                                    routePoints.add(
                                            new GeoPoint(lat, lon)
                                    );
                                }

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                            callback.onRouteReady(routePoints);
                        }

                        @Override
                        public void onFailure(
                                Call<String> call,
                                Throwable t
                        ) {
                            t.printStackTrace();
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface PlacesCallback {
        void onResult(List<String> places);
    }

    public static void searchPlaces(
            String query,
            PlacesCallback callback
    ) {

        ORSService service =
                ORSRetrofitClient
                        .getInstance()
                        .create(ORSService.class);

        service.autocomplete(API_KEY, query, "RS")
                .enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<String> call,
                            @NonNull Response<String> response
                    ) {

                        List<String> results =
                                new ArrayList<>();

                        try {

                            JSONObject json =
                                    new JSONObject(
                                            response.body()
                                    );

                            JSONArray features =
                                    json.getJSONArray("features");

                            for (int i = 0; i < features.length(); i++) {

                                JSONObject props =
                                        features.getJSONObject(i)
                                                .getJSONObject("properties");

                                String label = props.getString("label");

                                JSONArray coords =
                                        features.getJSONObject(i)
                                                .getJSONObject("geometry")
                                                .getJSONArray("coordinates");

                                double lon = coords.getDouble(0);
                                double lat = coords.getDouble(1);

                                results.add(label + "|" + lat + "|" + lon);

                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        callback.onResult(results);
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<String> call,
                            Throwable t
                    ) {
                        callback.onResult(
                                new ArrayList<>()
                        );
                    }
                });
    }


}
