package com.example.uber3.network.model.register;

import com.example.uber3.network.model.vehicle.VehicleRequest;

public class RegisterDriverRequest {

    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private VehicleRequest request;

    public RegisterDriverRequest() {}

    public RegisterDriverRequest(String email,
                                 String password,
                                 String firstName,
                                 String lastName,
                                 String phoneNumber,
                                 String address,
                                 VehicleRequest request) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.request = request;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public VehicleRequest getRequest() { return request; }
    public void setRequest(VehicleRequest request) { this.request = request; }
}
