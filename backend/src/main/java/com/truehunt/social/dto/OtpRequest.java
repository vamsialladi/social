package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OtpRequest {
    private String email;
    private String destination;
    private String phone;
    private String phoneNumber;
    private String otp;

    public OtpRequest() {}

    public String getEmail() {
        if (email != null && !email.isBlank()) return email;
        if (destination != null && !destination.isBlank()) return destination;
        if (phone != null && !phone.isBlank()) return phone;
        return phoneNumber;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}

