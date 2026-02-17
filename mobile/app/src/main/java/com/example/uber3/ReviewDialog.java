package com.example.uber3;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.uber3.network.model.review.RideReviewDetailResponse;
import com.example.uber3.network.model.review.RideReviewResponse;
import com.example.uber3.network.service.ReviewService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReviewDialog {

    public interface OnReviewSubmittedListener {
        void onReviewSubmitted();
    }

    private static int driverRating = 0;
    private static int vehicleRating = 0;

    public static void show(
            @NonNull Context context,
            @NonNull Long rideId,
            @NonNull OnReviewSubmittedListener listener
    ) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_ride_review, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Find views
        TextView tvRideInfo = view.findViewById(R.id.tvRideInfo);
        LinearLayout layoutDeadlineWarning = view.findViewById(R.id.layoutDeadlineWarning);
        TextView tvDeadlineWarning = view.findViewById(R.id.tvDeadlineWarning);
        LinearLayout layoutExistingReview = view.findViewById(R.id.layoutExistingReview);
        TextView tvExistingReview = view.findViewById(R.id.tvExistingReview);

        // Driver rating stars
        TextView[] driverStars = new TextView[]{
                view.findViewById(R.id.tvDriverStar1),
                view.findViewById(R.id.tvDriverStar2),
                view.findViewById(R.id.tvDriverStar3),
                view.findViewById(R.id.tvDriverStar4),
                view.findViewById(R.id.tvDriverStar5)
        };
        TextView tvDriverRatingText = view.findViewById(R.id.tvDriverRatingText);

        // Vehicle rating stars
        TextView[] vehicleStars = new TextView[]{
                view.findViewById(R.id.tvVehicleStar1),
                view.findViewById(R.id.tvVehicleStar2),
                view.findViewById(R.id.tvVehicleStar3),
                view.findViewById(R.id.tvVehicleStar4),
                view.findViewById(R.id.tvVehicleStar5)
        };
        TextView tvVehicleRatingText = view.findViewById(R.id.tvVehicleRatingText);

        EditText etComment = view.findViewById(R.id.etComment);
        TextView tvCharCount = view.findViewById(R.id.tvCharCount);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSubmit = view.findViewById(R.id.btnSubmit);
        ProgressBar pbLoading = view.findViewById(R.id.pbLoading);

        // Reset ratings
        driverRating = 0;
        vehicleRating = 0;

        // Setup char counter
        etComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + " / 1000");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup driver rating
        setupRatingStars(driverStars, tvDriverRatingText, true);

        // Setup vehicle rating
        setupRatingStars(vehicleStars, tvVehicleRatingText, false);

        // Load ride details
        ReviewService reviewService = new ReviewService(context);

        pbLoading.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        reviewService.getRideReviewDetails(rideId, new ReviewService.ReviewDetailCallback() {
            @Override
            public void onSuccess(RideReviewDetailResponse details) {
                pbLoading.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);

                // Set ride info
                String rideInfoText = "Driver: " + details.driverName + " • " +
                        formatDateTime(details.startTime);
                tvRideInfo.setText(rideInfoText);

                // Show deadline warning if applicable
                if (details.canReview) {
                    int daysRemaining = ReviewService.getDaysRemaining(details.reviewDeadline);
                    if (daysRemaining <= 3) {
                        layoutDeadlineWarning.setVisibility(View.VISIBLE);
                        String warningText = daysRemaining + " " +
                                (daysRemaining == 1 ? "day" : "days") +
                                " remaining to review";
                        tvDeadlineWarning.setText(warningText);
                    }
                } else {
                    btnSubmit.setEnabled(false);
                    btnSubmit.setText("Review Period Expired");
                }

                // Show existing review if present
                if (details.existingReview != null) {
                    layoutExistingReview.setVisibility(View.VISIBLE);
                    tvExistingReview.setText("Updating your review from " +
                            formatDateTime(details.existingReview.reviewedAt));

                    // Pre-fill with existing review
                    driverRating = details.existingReview.driverRating;
                    vehicleRating = details.existingReview.vehicleRating;

                    updateStarDisplay(driverStars, driverRating);
                    updateStarDisplay(vehicleStars, vehicleRating);
                    updateRatingText(tvDriverRatingText, driverRating);
                    updateRatingText(tvVehicleRatingText, vehicleRating);

                    if (details.existingReview.comment != null) {
                        etComment.setText(details.existingReview.comment);
                    }

                    btnSubmit.setText("✓ Update Review");
                }
            }

            @Override
            public void onError(String errorMessage) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(context, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });

        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Submit button
        btnSubmit.setOnClickListener(v -> {
            if (driverRating == 0 || vehicleRating == 0) {
                Toast.makeText(context, "Please rate both driver and vehicle", Toast.LENGTH_SHORT).show();
                return;
            }

            pbLoading.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(false);
            btnCancel.setEnabled(false);

            String comment = etComment.getText().toString().trim();
            if (comment.isEmpty()) {
                comment = null;
            }

            reviewService.submitReview(rideId, driverRating, vehicleRating, comment,
                    new ReviewService.SubmitReviewCallback() {
                        @Override
                        public void onSuccess(RideReviewResponse response) {
                            pbLoading.setVisibility(View.GONE);
                            Toast.makeText(context, "✅ " + response.message, Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            listener.onReviewSubmitted();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            pbLoading.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);
                            btnCancel.setEnabled(true);
                            Toast.makeText(context, "❌ " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }

    private static void setupRatingStars(TextView[] stars, TextView ratingText, boolean isDriver) {
        for (int i = 0; i < stars.length; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> {
                if (isDriver) {
                    driverRating = rating;
                } else {
                    vehicleRating = rating;
                }
                updateStarDisplay(stars, rating);
                updateRatingText(ratingText, rating);
            });
        }
    }

    private static void updateStarDisplay(TextView[] stars, int rating) {
        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setText("★");
                stars[i].setTextColor(Color.parseColor("#F59E0B"));
            } else {
                stars[i].setText("☆");
                stars[i].setTextColor(Color.parseColor("#D1D5DB"));
            }
        }
    }

    private static void updateRatingText(TextView textView, int rating) {
        textView.setText(rating + " out of 5 stars");
        textView.setTextColor(Color.parseColor("#374151"));
    }

    private static String formatDateTime(String iso) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(iso.substring(0, Math.min(19, iso.length())));

            SimpleDateFormat outputFormat = new SimpleDateFormat(
                    "MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
            return date != null ? outputFormat.format(date) : iso;
        } catch (Exception e) {
            return iso;
        }
    }
}