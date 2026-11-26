package com.truehunt.social.dto;

public class ContentSafetyResult {
    private boolean isSafe = true;
    private String reason;

    public ContentSafetyResult() {}

    public ContentSafetyResult(boolean isSafe, String reason) {
        this.isSafe = isSafe;
        this.reason = reason;
    }

    public static ContentSafetyResult safe() {
        return new ContentSafetyResult(true, "Content passed safety check");
    }

    public static ContentSafetyResult rejected(String reason) {
        return new ContentSafetyResult(false, reason);
    }

    public boolean isSafe() { return isSafe; }
    public void setSafe(boolean safe) { isSafe = safe; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
