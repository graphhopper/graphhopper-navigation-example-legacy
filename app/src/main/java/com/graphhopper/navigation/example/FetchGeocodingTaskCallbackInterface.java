package com.graphhopper.navigation.example;

import com.graphhopper.directions.api.client.model.GeocodingLocation;

import java.util.List;

public interface FetchGeocodingTaskCallbackInterface {

    void onError(int message);

    void onPostExecuteGeocodingSearch(List<GeocodingLocation> points);

}
