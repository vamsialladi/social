package com.truehunt.social.dto;

public class OtpRequest {
    private String email;
    private String destination;
    private String otp;

    public OtpRequest() {}

    public String getEmail() {
        return email != null ? email : destination;
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

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}

