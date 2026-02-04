package com.example.uber3.helpers;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.util.List;
import java.util.Locale;

public class GeocodingHelper {

    public static String getAddress(Context context, double lat, double lng) {

        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());

            if (!Geocoder.isPresent()) {
                return "Geocoder not available";
            }

            List<Address> addresses =
                    geocoder.getFromLocation(lat, lng, 1);

            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }

            return "No address found";

        } catch (Exception e) {
            e.printStackTrace();
            return "Geocoder error";
        }
    }

}
