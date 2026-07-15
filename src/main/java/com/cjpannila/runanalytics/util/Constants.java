package com.cjpannila.runanalytics.util;

public class Constants {
    private Constants() {
    }

    public static final String APPLICATION_VERSION = "1.1";
    public static final String APPLICATION_NAME = "Running Analytics System";

    // Strava OAuth
    public static final String GRANT_TYPE = "authorization_code";
    //Strava API
    public static final String API_ATHLETE_CLUBS = "/athlete/clubs";
    public static final String API_ATHLETE_ACTIVITIES = "/athlete/activities";

    //Estimated max HR (can be user-specific later)
    public static final double MAX_HEART_RATE = 190.0;
}
