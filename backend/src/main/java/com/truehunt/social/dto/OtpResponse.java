package com.truehunt.social.dto;

public class OtpResponse {
    private String otp;
    private String message;
    private Boolean valid;

    public OtpResponse() {}

    public OtpResponse(String otp, String message) {
        this.otp = otp;
        this.message = message;
    }

    public OtpResponse(Boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }
}

