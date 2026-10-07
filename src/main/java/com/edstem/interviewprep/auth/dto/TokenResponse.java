package com.edstem.interviewprep.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
