package com.graphhopper.navigation.example;

import android.os.AsyncTask;

import com.graphhopper.directions.api.client.ApiException;
import com.graphhopper.directions.api.client.api.GeocodingApi;
import com.graphhopper.directions.api.client.model.GeocodingLocation;
import com.graphhopper.directions.api.client.model.GeocodingResponse;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class FetchGeocodingTask extends AsyncTask<FetchGeocodingConfig, Void, List<GeocodingLocation>> {

    private final String ghKey;
    private final FetchGeocodingTaskCallbackInterface callbackInterface;

    FetchGeocodingTask(FetchGeocodingTaskCallbackInterface callbackInterface, String ghKey) {
        this.callbackInterface = callbackInterface;
        this.ghKey = ghKey;
    }

    @Override
    protected List<GeocodingLocation> doInBackground(FetchGeocodingConfig... geocodingConfigs) {

        if (geocodingConfigs.length != 1)
            throw new IllegalArgumentException("It's only possible to fetch one geocoding at a time");

        List<GeocodingLocation> locations = new ArrayList<>();
        GeocodingApi api = new GeocodingApi();

        try {
            FetchGeocodingConfig geocodingConfig = geocodingConfigs[0];
            GeocodingResponse res = api.geocodeGet(ghKey, geocodingConfig.query, geocodingConfig.locale, geocodingConfig.limit, geocodingConfig.reverse, geocodingConfig.point, geocodingConfig.provider);
            locations = res.getHits();

            if (locations.isEmpty())
                callbackInterface.onError(R.string.error_location_not_found);

        } catch (ApiException e) {
            callbackInterface.onError(R.string.error_fetching_geocoding);
            Timber.e(e, "An exception occured when fetching geocoding results for %s", geocodingConfigs[0].query);
        }

        return locations;
    }

    @Override
    protected void onPostExecute(List<GeocodingLocation> locations) {
        callbackInterface.onPostExecuteGeocodingSearch(locations);
    }
}