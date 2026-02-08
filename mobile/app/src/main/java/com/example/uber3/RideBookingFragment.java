package com.example.uber3;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.text.TextWatcher;
import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.uber3.helpers.GeocodingHelper;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ApiService;
import com.example.uber3.network.model.location.LocationRequest;
import com.example.uber3.network.model.ride.CreateRideRequest;
import com.example.uber3.network.model.ride.RideResponse;
import com.example.uber3.network.model.ride.RouteEstimateRequest;
import com.example.uber3.network.model.ride.RouteEstimateResponse;
import com.example.uber3.repository.ORSRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import org.osmdroid.util.GeoPoint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideBookingFragment extends BottomSheetDialogFragment {
    private LinearLayout stopsContainer;
    private LinearLayout priceInfoLayout;
    private AutoCompleteTextView pickupInput;
    private AutoCompleteTextView destinationInput;

    private EditText etScheduledAt;
    private String scheduledIso = null;


    private MaterialButton bookRideButton;
    private MaterialButton resetButton;
    private int stopCount = 0;

    private ApiService apiService;


    private EditText etPassengerEmail;
    private LinearLayout passengerContainer;


    public RideBookingFragment() {}

    public static RideBookingFragment newInstance() {
        return new RideBookingFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_ride_booking,
                container,
                false
        );

        apiService =
                ApiClient
                        .getClient(requireContext())
                        .create(ApiService.class);


        stopsContainer = view.findViewById(R.id.stopsContainer);
        pickupInput = view.findViewById(R.id.pickupInput);
        destinationInput = view.findViewById(R.id.destinationInput);
        priceInfoLayout = view.findViewById(R.id.priceInfoLayout);
        bookRideButton = view.findViewById(R.id.bookRideButton);
        resetButton = view.findViewById(R.id.resetButton);
        etScheduledAt = view.findViewById(R.id.etScheduledAt);

        etScheduledAt.setOnLongClickListener(v -> {
            scheduledIso = null;
            etScheduledAt.setText("");
            return true;
        });


        etScheduledAt.setOnClickListener(v -> openDateTimePicker());

        etPassengerEmail =
                view.findViewById(R.id.etPassengerEmail);

        passengerContainer =
                view.findViewById(R.id.passengerContainer);

        ImageButton btnAddPassenger =
                view.findViewById(R.id.btnAddPassenger);

        btnAddPassenger.setOnClickListener(
                v -> addPassenger()
        );

        MaterialButton addStopButton =
                view.findViewById(R.id.addStopButton);

        addStopButton.setOnClickListener(
                v -> addStop()
        );

        bookRideButton.setOnClickListener(
                v -> bookRide()
        );

        resetButton.setOnClickListener(
                v -> resetForm()
        );

        setupLiveAutocomplete(
                pickupInput,
                MapFragment.PointType.PICKUP
        );

        setupLiveAutocomplete(
                destinationInput,
                MapFragment.PointType.DESTINATION
        );


        fillFromMapPoints();

        return view;
    }




    private void addPassenger() {

        String email =
                etPassengerEmail.getText()
                        .toString()
                        .trim();

        if (email.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "Enter email",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        LinearLayout row =
                new LinearLayout(requireContext());

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        TextView tv =
                new TextView(requireContext());

        tv.setText(email);
        tv.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        ImageButton btnRemove =
                new ImageButton(requireContext());

        btnRemove.setImageResource(
                android.R.drawable.ic_menu_close_clear_cancel
        );

        btnRemove.setBackground(null);

        btnRemove.setOnClickListener(
                v -> passengerContainer.removeView(row)
        );

        row.addView(tv);
        row.addView(btnRemove);

        passengerContainer.addView(row);

        etPassengerEmail.setText("");
    }


    @SuppressLint("SetTextI18n")
    private void fillFromMapPoints() {

        if (MapFragment.selectedPoints.isEmpty()) return;

        // ---------- PICKUP ----------
        GeoPoint first = MapFragment.selectedPoints.get(0);

        new Thread(() -> {

            String addr = GeocodingHelper.getAddress(
                    requireContext(),
                    first.getLatitude(),
                    first.getLongitude()
            );

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (pickupInput != null) {
                        pickupInput.setText(addr);
                    }
                });
            }


        }).start();


        // ---------- DESTINATION ----------
        if (MapFragment.selectedPoints.size() > 1) {

            GeoPoint last = MapFragment.selectedPoints.get(
                    MapFragment.selectedPoints.size() - 1
            );

            new Thread(() -> {

                String addr = GeocodingHelper.getAddress(
                        requireContext(),
                        last.getLatitude(),
                        last.getLongitude()
                );

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (destinationInput != null) {
                            destinationInput.setText(addr);
                        }
                    });
                }


            }).start();
        }


        for (int i = 1; i < MapFragment.selectedPoints.size() - 1; i++) {

            GeoPoint p = MapFragment.selectedPoints.get(i);

            addStop();

            LinearLayout row =
                    (LinearLayout) stopsContainer.getChildAt(
                            stopsContainer.getChildCount() - 1
                    );

            TextInputLayout til = (TextInputLayout) row.getChildAt(0);
            AutoCompleteTextView et =
                    (AutoCompleteTextView) til.getEditText();


            if (et == null) continue;

            new Thread(() -> {

                String addr = GeocodingHelper.getAddress(
                        requireContext(),
                        p.getLatitude(),
                        p.getLongitude()
                );

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        et.setText(addr);
                    });
                }


            }).start();
        }
    }


    private void addStop() {
        stopCount++;

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        row.setPadding(0, 8, 0, 8);

        TextInputLayout inputLayout = new TextInputLayout(requireContext());
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        inputLayout.setLayoutParams(inputParams);
        inputLayout.setHint("Stop " + stopCount);
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setBoxCornerRadii(12, 12, 12, 12);

        AutoCompleteTextView editText =
                new AutoCompleteTextView(requireContext());

        inputLayout.addView(editText);

        setupLiveAutocomplete(
                editText,
                MapFragment.PointType.STOP
        );

        ImageButton removeBtn = new ImageButton(requireContext());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(8, 0, 0, 0);
        removeBtn.setLayoutParams(btnParams);
        removeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        removeBtn.setBackground(null);

        removeBtn.setOnClickListener(v -> {

            int index =
                    stopsContainer.indexOfChild(row);

            stopsContainer.removeView(row);
            stopCount--;

            if (MapFragment.instance != null) {
                MapFragment.instance.removeStopAt(index);
            }
        });


        row.addView(inputLayout);
        row.addView(removeBtn);

        stopsContainer.addView(row);
    }

    private void bookRide() {
        String pickup = pickupInput.getText() != null ? pickupInput.getText().toString().trim() : "";
        String destination = destinationInput.getText() != null ? destinationInput.getText().toString().trim() : "";

        if (pickup.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter pickup location", Toast.LENGTH_SHORT).show();
            return;
        }

        if (destination.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter destination", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < stopsContainer.getChildCount(); i++) {
            View child = stopsContainer.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                TextInputLayout inputLayout = (TextInputLayout) row.getChildAt(0);
                AutoCompleteTextView editText =
                        (AutoCompleteTextView) inputLayout.getEditText();

                if (editText != null) {
                    String stopText = editText.getText() != null ? editText.getText().toString().trim() : "";
                    if (stopText.isEmpty()) {
                        Toast.makeText(requireContext(), "Please fill all stops", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        }

        List<String> passengers =
                getPassengerEmails();
        estimateRide();
    }

    private void resetForm() {

        pickupInput.setText("");
        destinationInput.setText("");

        stopsContainer.removeAllViews();
        stopCount = 0;

        passengerContainer.removeAllViews();
        etPassengerEmail.setText("");

        priceInfoLayout.setVisibility(View.GONE);

        if (MapFragment.instance != null) {
            MapFragment.instance.clearMap();
        }

        Toast.makeText(
                requireContext(),
                "Form reset",
                Toast.LENGTH_SHORT
        ).show();
    }




    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view == null) return;

        View parent = (View) view.getParent();

        com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior =
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(parent);

        behavior.setPeekHeight(600);

        // OMOGUĆAVA full expand
        behavior.setFitToContents(true);

        // DOZVOLJAVA više stanja
        behavior.setSkipCollapsed(false);

        // početno stanje
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
    }


    private Runnable onDismissCallback;

    public void setOnDismissCallback(Runnable callback) {
        this.onDismissCallback = callback;
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);

        if (onDismissCallback != null) {
            onDismissCallback.run();
        }
    }

    @SuppressLint("SetTextI18n")
    public void setPickupFromMap(double lat, double lng) {

        if (!isAdded() || pickupInput == null) return;

        pickupInput.setText("Loading address...");

        // uzmi context dok je fragment živ
        final android.content.Context ctx = getContext();

        new Thread(() -> {

            if (ctx == null) return;

            String address =
                    GeocodingHelper.getAddress(
                            ctx,
                            lat,
                            lng
                    );

            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                if (pickupInput != null) {
                    pickupInput.setText(address);
                }
            });

        }).start();
    }


    private void setupLiveAutocomplete(
            AutoCompleteTextView field,
            MapFragment.PointType type
    ) {

        field.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {}

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {

                String query = s.toString();

                if (query.length() < 3) return;

                ORSRepository.searchPlaces(
                        query,
                        places -> {

                            if (!isAdded()) return;

                            requireActivity()
                                    .runOnUiThread(() -> {

                                        ArrayAdapter<String> adapter =
                                                new ArrayAdapter<>(
                                                        requireContext(),
                                                        android.R.layout.simple_dropdown_item_1line,
                                                        places
                                                );

                                        field.setAdapter(adapter);
                                        field.showDropDown();
                                    });
                        }
                );
            }

            @Override
            public void afterTextChanged(
                    Editable s
            ) {}
        });

        field.setOnItemClickListener((parent, view, position, id) -> {

            String item =
                    (String) parent.getItemAtPosition(position);

            String[] parts = item.split("\\|");

            if (parts.length < 3) return;

            String label = parts[0];
            double lat = Double.parseDouble(parts[1]);
            double lon = Double.parseDouble(parts[2]);

            field.setText(label);
            field.setSelection(label.length());

            if (MapFragment.instance != null) {

                GeoPoint point =
                        new GeoPoint(lat, lon);

                MapFragment.instance.addTypedPoint(point, type);
            }

        });
    }

    private List<String> getPassengerEmails() {

        List<String> emails =
                new ArrayList<>();

        for (int i = 0;
             i < passengerContainer.getChildCount();
             i++) {

            LinearLayout row =
                    (LinearLayout)
                            passengerContainer.getChildAt(i);

            TextView tv =
                    (TextView) row.getChildAt(0);

            emails.add(
                    tv.getText().toString()
            );
        }

        return emails;
    }


    private LocationRequest toLocationRequest(
            GeoPoint p,
            String address
    ) {
        return new LocationRequest(
                p.getLatitude(),
                p.getLongitude(),
                address
        );
    }

    private void estimateRide() {

        if (MapFragment.selectedPoints.size() < 2) {
            Toast.makeText(
                    requireContext(),
                    "Select route first",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        GeoPoint start =
                MapFragment.selectedPoints.get(0);

        GeoPoint end =
                MapFragment.selectedPoints.get(
                        MapFragment.selectedPoints.size() - 1
                );

        LocationRequest startReq =
                toLocationRequest(
                        start,
                        pickupInput.getText().toString()
                );

        LocationRequest endReq =
                toLocationRequest(
                        end,
                        destinationInput.getText().toString()
                );

        List<LocationRequest> stops =
                new ArrayList<>();

        for (int i = 1;
             i < MapFragment.selectedPoints.size() - 1;
             i++) {

            GeoPoint s =
                    MapFragment.selectedPoints.get(i);

            stops.add(
                    new LocationRequest(
                            s.getLatitude(),
                            s.getLongitude(),
                            "Stop"
                    )
            );
        }

        String vehicleType =
                ((Spinner)
                        getView().findViewById(R.id.spVehicleType))
                        .getSelectedItem()
                        .toString();

        RouteEstimateRequest req =
                new RouteEstimateRequest(
                        startReq,
                        endReq,
                        stops,
                        vehicleType
                );

        apiService
                .estimateRoute(req)
                .enqueue(new Callback<RouteEstimateResponse>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<RouteEstimateResponse> call,
                            @NonNull Response<RouteEstimateResponse> response
                    ) {

                        if (!response.isSuccessful()
                                || response.body() == null)
                            return;

                        RouteEstimateResponse res = response.body();

                        showEstimateDialog(res);
                    }


                    @Override
                    public void onFailure(
                            @NonNull Call<RouteEstimateResponse> call,
                            @NonNull Throwable t
                    ) {
                        Toast.makeText(
                                requireContext(),
                                "Estimate failed",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }


    private void showEstimateDialog(
            RouteEstimateResponse res
    ) {

        @SuppressLint("DefaultLocale") String price =
                String.format("%.2f", res.estimatedPrice);

        new androidx.appcompat.app.AlertDialog.Builder(
                requireContext()
        )
                .setTitle("Confirm ride")

                .setMessage(
                        "Estimated time: "
                                + res.estimatedTimeMinutes
                                + " min\n\n"
                                + "Estimated price: "
                                + price
                                + " RSD"
                )

                .setNegativeButton(
                        "Cancel",
                        (d, w) -> d.dismiss()
                )

                .setPositiveButton(
                        "Confirm ride",
                        (d, w) -> createRide()
                )

                .show();
    }


    private void createRide() {

        GeoPoint start =
                MapFragment.selectedPoints.get(0);

        GeoPoint end =
                MapFragment.selectedPoints.get(
                        MapFragment.selectedPoints.size() - 1
                );

        LocationRequest startReq =
                toLocationRequest(
                        start,
                        pickupInput.getText().toString()
                );

        LocationRequest endReq =
                toLocationRequest(
                        end,
                        destinationInput.getText().toString()
                );

        List<LocationRequest> stops =
                new ArrayList<>();

        for (int i = 1;
             i < MapFragment.selectedPoints.size() - 1;
             i++) {

            GeoPoint s =
                    MapFragment.selectedPoints.get(i);

            stops.add(
                    new LocationRequest(
                            s.getLatitude(),
                            s.getLongitude(),
                            "Stop"
                    )
            );
        }

        List<String> passengers =
                getPassengerEmails();

        assert getView() != null;
        String vehicleType =
                ((Spinner)getView()
                        .findViewById(R.id.spVehicleType))
                        .getSelectedItem()
                        .toString();

        CreateRideRequest req =
                new CreateRideRequest(
                        startReq,
                        endReq,
                        stops,
                        passengers,
                        vehicleType,
                        false,
                        false,
                        scheduledIso
                );

        apiService
                .createRide(req)
                .enqueue(new Callback<RideResponse>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<RideResponse> call,
                            @NonNull Response<RideResponse> response
                    ) {

                        if (response.isSuccessful()) {

                            Toast.makeText(
                                    requireContext(),
                                    "Ride created!",
                                    Toast.LENGTH_LONG
                            ).show();

                            scheduledIso = null;
                            etScheduledAt.setText("");
                            resetForm();
                        }
                        else {
                            Toast.makeText(
                                    requireContext(),
                                    "Error: " + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<RideResponse> call,
                            @NonNull Throwable t
                    ) {
                        Toast.makeText(
                                requireContext(),
                                "Fail: " + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

    }


    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void openDateTimePicker() {

        // DATE PICKER
        MaterialDatePicker<Long> datePicker =
                MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select date")
                        .setSelection(
                                System.currentTimeMillis() + 15 * 60 * 1000
                        ).build();

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");

        datePicker.addOnPositiveButtonClickListener(date -> {

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(date);

            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);

            // TIME PICKER
            MaterialTimePicker timePicker =
                    new MaterialTimePicker.Builder()
                            .setTimeFormat(TimeFormat.CLOCK_24H)
                            .setHour(cal.get(Calendar.HOUR_OF_DAY))
                            .setMinute(cal.get(Calendar.MINUTE))
                            .setTitleText("Select time")
                            .build();

            timePicker.show(getParentFragmentManager(), "TIME_PICKER");

            timePicker.addOnPositiveButtonClickListener(v -> {

                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();

                LocalDateTime ldt =
                        LocalDateTime.of(
                                year,
                                month + 1,
                                day,
                                hour,
                                minute
                        );

                if (ldt.isBefore(LocalDateTime.now().plusMinutes(1))) {
                    Toast.makeText(
                            requireContext(),
                            "Time must be in the future",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }


                if (ldt.isAfter(LocalDateTime.now().plusHours(5))) {
                    Toast.makeText(
                            requireContext(),
                            "Max 5 hours ahead",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                scheduledIso = ldt.toString();

                etScheduledAt.setText(
                        day + "/" + (month+1) +
                                " " + hour + ":" +
                                String.format("%02d", minute)
                );
            });
        });
    }







}