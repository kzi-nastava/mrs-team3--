package com.example.uber3.network.service;

import android.content.Context;
import android.util.Log;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.DriverApi;
import com.example.uber3.network.model.history.DriverRideHistoryDetailResponse;
import com.example.uber3.network.model.history.DriverRideHistoryResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DriverHistoryService {

    private static final String TAG = "DriverHistoryService";

    private final DriverApi api;
    private final Context context;

    public DriverHistoryService(Context context) {
        this.context = context;
        this.api = ApiClient.getClient(context).create(DriverApi.class);
    }

    public interface RideHistoryCallback {
        void onSuccess(List<DriverRideHistoryResponse> rides);
        void onError(String errorMessage);
    }

    public interface RideDetailCallback {
        void onSuccess(DriverRideHistoryDetailResponse rideDetail);
        void onError(String errorMessage);
    }

    public void getDriverRideHistory(Long driverId,
                                     Date startDate,
                                     Date endDate,
                                     RideHistoryCallback callback) {

        if (driverId == null) {
            callback.onError("Driver ID is required");
            return;
        }

        // Convert dates to ISO format strings
        String startDateStr = startDate != null ? formatDateToISO(startDate, true) : null;
        String endDateStr = endDate != null ? formatDateToISO(endDate, false) : null;

        Log.d(TAG, "Fetching ride history for driver: " + driverId);
        Log.d(TAG, "Start date: " + startDateStr);
        Log.d(TAG, "End date: " + endDateStr);

        Call<List<DriverRideHistoryResponse>> call = api.getDriverRideHistory(
                driverId,
                startDateStr,
                endDateStr
        );

        call.enqueue(new Callback<List<DriverRideHistoryResponse>>() {
            @Override
            public void onResponse(Call<List<DriverRideHistoryResponse>> call,
                                   Response<List<DriverRideHistoryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<DriverRideHistoryResponse> rides = response.body();
                    Log.d(TAG, "Successfully fetched " + rides.size() + " rides");
                    callback.onSuccess(rides);
                } else {
                    String errorMsg = "Failed to load rides: " + response.code();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<DriverRideHistoryResponse>> call, Throwable t) {
                String errorMsg = "Network error: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }

    public void getAllDriverRides(Long driverId, RideHistoryCallback callback) {
        getDriverRideHistory(driverId, null, null, callback);
    }

    public void getCurrentMonthRides(Long driverId, RideHistoryCallback callback) {
        Calendar calendar = Calendar.getInstance();

        // First day of month
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startDate = calendar.getTime();

        // Last day of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();

        getDriverRideHistory(driverId, startDate, endDate, callback);
    }

    public void getCurrentWeekRides(Long driverId, RideHistoryCallback callback) {
        Calendar calendar = Calendar.getInstance();

        // Start of week (Monday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startDate = calendar.getTime();

        // End of week (Sunday)
        calendar.add(Calendar.DAY_OF_WEEK, 6);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();

        getDriverRideHistory(driverId, startDate, endDate, callback);
    }

    public void getTodayRides(Long driverId, RideHistoryCallback callback) {
        Calendar calendar = Calendar.getInstance();

        // Start of day
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startDate = calendar.getTime();

        // End of day
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();

        getDriverRideHistory(driverId, startDate, endDate, callback);
    }

    public void getDriverRideDetail(Long driverId, Long rideId, RideDetailCallback callback) {

        if (driverId == null || rideId == null) {
            callback.onError("Driver ID and Ride ID are required");
            return;
        }

        Log.d(TAG, "Fetching ride detail - Driver: " + driverId + ", Ride: " + rideId);

        Call<DriverRideHistoryDetailResponse> call = api.getDriverRideDetail(driverId, rideId);

        call.enqueue(new Callback<DriverRideHistoryDetailResponse>() {
            @Override
            public void onResponse(Call<DriverRideHistoryDetailResponse> call,
                                   Response<DriverRideHistoryDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DriverRideHistoryDetailResponse rideDetail = response.body();
                    Log.d(TAG, "Successfully fetched ride detail for ride: " + rideId);
                    callback.onSuccess(rideDetail);
                } else {
                    String errorMsg = "Failed to load ride details: " + response.code();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<DriverRideHistoryDetailResponse> call, Throwable t) {
                String errorMsg = "Network error: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }

    private String formatDateToISO(Date date, boolean startOfDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        if (startOfDay) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }


    public List<DriverRideHistoryResponse> filterByStatus(
            List<DriverRideHistoryResponse> rides,
            String status) {

        List<DriverRideHistoryResponse> filtered = new ArrayList<>();
        for (DriverRideHistoryResponse ride : rides) {
            if (ride.getFormattedStatus().equals(status)) {
                filtered.add(ride);
            }
        }
        return filtered;
    }

    public List<DriverRideHistoryResponse> getCompletedRides(List<DriverRideHistoryResponse> rides) {
        return filterByStatus(rides, "COMPLETED");
    }

    public List<DriverRideHistoryResponse> getCancelledRides(List<DriverRideHistoryResponse> rides) {
        List<DriverRideHistoryResponse> cancelled = new ArrayList<>();
        for (DriverRideHistoryResponse ride : rides) {
            String status = ride.getFormattedStatus();
            if (status.contains("CANCELLED")) {
                cancelled.add(ride);
            }
        }
        return cancelled;
    }


    public List<DriverRideHistoryResponse> getPanicRides(List<DriverRideHistoryResponse> rides) {
        List<DriverRideHistoryResponse> panicRides = new ArrayList<>();
        for (DriverRideHistoryResponse ride : rides) {
            if (ride.hadPanicEvent) {
                panicRides.add(ride);
            }
        }
        return panicRides;
    }

    public List<DriverRideHistoryResponse> sortByDateDescending(List<DriverRideHistoryResponse> rides) {
        List<DriverRideHistoryResponse> sorted = new ArrayList<>(rides);
        sorted.sort((r1, r2) -> {
            if (r1.startedAt == null || r2.startedAt == null) return 0;
            return r2.startedAt.compareTo(r1.startedAt);
        });
        return sorted;
    }

    public List<DriverRideHistoryResponse> sortByEarningsDescending(List<DriverRideHistoryResponse> rides) {
        List<DriverRideHistoryResponse> sorted = new ArrayList<>(rides);
        sorted.sort((r1, r2) -> {
            double price1 = r1.price != null ? r1.price : 0;
            double price2 = r2.price != null ? r2.price : 0;
            return Double.compare(price2, price1);
        });
        return sorted;
    }
}