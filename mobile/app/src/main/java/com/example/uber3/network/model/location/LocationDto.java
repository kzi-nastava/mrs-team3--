package com.example.uber3.network.model.location;

import com.google.gson.annotations.SerializedName;

public class LocationDto {
    @SerializedName(value = "latitude", alternate = {"lat"})
    public Double latitude;

    @SerializedName(value = "longitude", alternate = {"lng"})
    public Double longitude;

    public String address;
}
