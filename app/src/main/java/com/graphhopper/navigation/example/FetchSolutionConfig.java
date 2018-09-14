package com.graphhopper.navigation.example;

public class FetchSolutionConfig {

    final String jobId;
    final String vehicleId;

    FetchSolutionConfig(String jobId, String vehicleId) {
        this.jobId = jobId;
        this.vehicleId = vehicleId;
    }
}