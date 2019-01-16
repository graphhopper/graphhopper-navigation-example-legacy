package com.graphhopper.navigation.example;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.graphhopper.directions.api.client.model.GeocodingLocation;
import com.graphhopper.directions.api.client.model.GeocodingPoint;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.exceptions.InvalidLatLngBoundsException;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Telemetry;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.ui.v5.route.OnRouteSelectionChangeListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Response;

public class NavigationLauncherActivity extends AppCompatActivity implements OnMapReadyCallback,
        MapboxMap.OnMapLongClickListener, OnRouteSelectionChangeListener,
        SolutionInputDialog.NoticeDialogListener, FetchSolutionTaskCallbackInterface,
        FetchGeocodingTaskCallbackInterface, GeocodingInputDialog.NoticeDialogListener,
        PermissionsListener {

    private static final int CAMERA_ANIMATION_DURATION = 1000;
    private static final int DEFAULT_CAMERA_ZOOM = 16;
    private static final int CHANGE_SETTING_REQUEST_CODE = 1;
    // If you change the first start dialog (help message), increase this number, all users will be shown the message again
    private static final int FIRST_START_DIALOG_VERSION = 1;

    private LocationLayerPlugin locationLayer;
    private NavigationMapRoute mapRoute;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;

    @BindView(R.id.mapView)
    MapView mapView;
    @BindView(R.id.loading)
    ProgressBar loading;

    private Marker currentMarker;
    private List<Marker> markers;
    private List<Point> waypoints = new ArrayList<>();
    private DirectionsRoute route;
    private LocaleUtils localeUtils;

    private final int[] padding = new int[]{50, 50, 50, 50};

    private String currentJobId = "";
    private String currentVehicleId = "";
    private String currentGeocodingInput = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_launcher);
        Mapbox.getInstance(this.getApplicationContext(), getString(R.string.mapbox_access_token));
        Telemetry.disableOnUserRequest();
        ButterKnife.bind(this);
        mapView.setStyleUrl(getString(R.string.map_view_styleUrl));
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        localeUtils = new LocaleUtils();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }
        showFirstStartIfNecessary();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation_view_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                showSettings();
                return true;
            case R.id.navigate_btn:
                launchNavigationWithRoute();
                return true;
            case R.id.reset_route_btn:
                clearRoute();
                return true;
            case R.id.fetch_solution_btn:
                showSolutionInputDialog();
                return true;
            case R.id.geocoding_search_btn:
                showGeocodingInputDialog();
                return true;
            case R.id.help:
                showHelpDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showSettings() {
        startActivityForResult(new Intent(this, NavigationViewSettingsActivity.class), CHANGE_SETTING_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHANGE_SETTING_REQUEST_CODE && resultCode == RESULT_OK) {
            boolean shouldRefetch = data.getBooleanExtra(NavigationViewSettingsActivity.UNIT_TYPE_CHANGED, false)
                    || data.getBooleanExtra(NavigationViewSettingsActivity.LANGUAGE_CHANGED, false)
                    || data.getBooleanExtra(NavigationViewSettingsActivity.PROFILE_CHANGED, false);
            if (waypoints.size() > 0 && shouldRefetch) {
                fetchRoute();
            }
        }
    }

    @SuppressWarnings({"MissingPermission"})
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        if (locationLayer != null) {
            locationLayer.onStart();
        }
    }

    @SuppressWarnings({"MissingPermission"})
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // getIntent() should always return the most recent
        setIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            Uri data = intent.getData();
            if (data != null && "graphhopper.com".equals(data.getHost())) {
                if (data.getPath() != null) {
                    if (this.mapboxMap == null) {
                        //this happens when onResume is called at the initial start and we will call this method again in onMapReady
                        return;
                    }
                    if (data.getPath().contains("maps")) {
                        clearRoute();
                        //Open Map Url
                        setRouteProfileToSharedPreferences(data.getQueryParameter("vehicle"));

                        List<String> points = data.getQueryParameters("point");
                        for (String point : points) {
                            String[] pointArr = point.split(",");
                            addPointToRoute(Double.parseDouble(pointArr[0]), Double.parseDouble(pointArr[1]));
                        }

                        setStartFromLocationToSharedPreferences(false);
                        updateRouteAfterWaypointChange();
                    }
                    // https://graphhopper.com/api/1/vrp/solution/e7fb8a9b-e441-4ec2-a487-20788e591bb3?vehicle_id=1&key=[KEY]
                    if (data.getPath().contains("api/1/vrp/solution")) {
                        clearRoute();
                        //Open Vrp Url
                        List<String> pathSegments = data.getPathSegments();
                        fetchVrpSolution(pathSegments.get(pathSegments.size() - 1), data.getQueryParameter("vehicle_id"));
                    }
                }

            }
        }
    }

    private void showFirstStartIfNecessary() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getInt(getString(R.string.first_start_dialog_key), -1) < FIRST_START_DIALOG_VERSION) {
            showHelpDialog();
        }
    }

    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.help_message_title);
        builder.setMessage(Html.fromHtml("1. Please note, this is a demo and not a full featured application. The purpose of this application is to provide an easy starting point for developers to create a navigation application with GraphHopper<br/>2. You should enable your GPS/location<br/>3.You can either search for a location using the magnifier icon or by long pressing on the map<br/>4. Start the navigation by tapping the arrow button<br/><br/>This project is 100% open source, contributions are welcome.<br/><br/>Please drive carefully and always abide local law and signs. Roads might be impassible due to construction projects, traffic, weather, or other events."));
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        builder.setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(getString(R.string.first_start_dialog_key), FIRST_START_DIALOG_VERSION);
                editor.apply();
            }
        });
        builder.setNeutralButton(R.string.github, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/graphhopper/graphhopper-navigation-example")));
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void fetchVrpSolution(String jobId, String vehicleId) {
        currentJobId = jobId;
        currentVehicleId = vehicleId;

        showLoading();
        new FetchSolutionTask(this, getString(R.string.gh_key)).execute(new FetchSolutionConfig(currentJobId, currentVehicleId));
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        if (locationLayer != null) {
            locationLayer.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void clearRoute() {
        waypoints.clear();
        mapRoute.removeRoute();
        route = null;
        if (currentMarker != null) {
            mapboxMap.removeMarker(currentMarker);
            currentMarker = null;
        }
    }

    private void clearGeocodingResults() {
        if (markers != null) {
            for (Marker marker : markers) {
                this.mapboxMap.removeMarker(marker);
            }
            markers.clear();
        }
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        this.mapboxMap.getUiSettings().setAttributionDialogManager(new GHAttributionDialogManager(this.mapView.getContext(), this.mapboxMap));
        this.mapboxMap.addOnMapLongClickListener(this);
        initMapRoute();

        this.mapboxMap.setOnInfoWindowClickListener(new MapboxMap.OnInfoWindowClickListener() {
            @Override
            public boolean onInfoWindowClick(@NonNull Marker marker) {
                for (Marker geocodingMarker : markers) {
                    if (geocodingMarker.getId() == marker.getId()) {
                        LatLng position = geocodingMarker.getPosition();
                        addPointToRoute(position.getLatitude(), position.getLongitude());
                        updateRouteAfterWaypointChange();
                        marker.hideInfoWindow();
                        return true;
                    }
                }
                return true;
            }
        });

        // Check for location permission
        permissionsManager = new PermissionsManager(this);
        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            permissionsManager.requestLocationPermissions(this);
        } else {
            initLocationLayer();
        }

        handleIntent(getIntent());
    }

    @Override
    public void onMapLongClick(@NonNull LatLng point) {
        addPointToRoute(point.getLatitude(), point.getLongitude());
        updateRouteAfterWaypointChange();
    }

    private void addPointToRoute(double lat, double lng) {
        waypoints.add(Point.fromLngLat(lng, lat));
    }

    @Override
    public void onNewPrimaryRouteSelected(DirectionsRoute directionsRoute) {
        route = directionsRoute;
    }

    @SuppressWarnings({"MissingPermission"})
    private void initLocationLayer() {
        locationLayer = new LocationLayerPlugin(mapView, mapboxMap);
        locationLayer.setRenderMode(RenderMode.COMPASS);
        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null) {
            // TODO we could think about zoom to the user location later on as well
            animateCamera(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
        }
    }

    private void initMapRoute() {
        mapRoute = new NavigationMapRoute(mapView, mapboxMap);
        mapRoute.setOnRouteSelectionChangeListener(this);
    }

    private void fetchRoute() {
        NavigationRoute.Builder builder = NavigationRoute.builder(this)
                .accessToken("pk." + getString(R.string.gh_key))
                .baseUrl(getString(R.string.base_url))
                .user("gh")
                .alternatives(true);

        boolean startFromLocation = getStartFromLocationFromSharedPreferences();

        if (!startFromLocation && waypoints.size() < 2 || startFromLocation && waypoints.size() < 1) {
            onError(R.string.error_not_enough_waypoints);
            return;
        }

        if (startFromLocation) {
            Location lastKnownLocation = getLastKnownLocation();
            if (lastKnownLocation == null) {
                onError(R.string.error_location_not_found);
                return;
            } else {
                Point location = Point.fromLngLat(lastKnownLocation.getLongitude(), lastKnownLocation.getLatitude());
                if (lastKnownLocation.hasBearing())
                    // 90 seems to be the default tolerance of the SDK
                    builder.origin(location, (double) lastKnownLocation.getBearing(), 90.0);
                else
                    builder.origin(location);
            }
        }

        for (int i = 0; i < waypoints.size(); i++) {
            Point p = waypoints.get(i);
            if (i == 0 && !startFromLocation) {
                builder.origin(p);
            } else if (i < waypoints.size() - 1) {
                builder.addWaypoint(p);
            } else {
                builder.destination(p);
            }
        }

        showLoading();

        setFieldsFromSharedPreferences(builder);
        builder.build().getRoute(new SimplifiedCallback() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (validRouteResponse(response)) {
                    route = response.body().routes().get(0);
                    mapRoute.addRoutes(response.body().routes());
                    boundCameraToRoute();
                } else {
                    Snackbar.make(mapView, R.string.error_calculating_route, Snackbar.LENGTH_LONG).show();
                }
                hideLoading();
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                super.onFailure(call, throwable);
                Snackbar.make(mapView, R.string.error_calculating_route, Snackbar.LENGTH_LONG).show();
                hideLoading();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        if (locationLayer != null) {
            return locationLayer.getLastKnownLocation();
        }
        return null;
    }

    private void updateRouteAfterWaypointChange() {
        if (this.waypoints.isEmpty()) {
            hideLoading();
        } else {
            Point lastPoint = this.waypoints.get(this.waypoints.size() - 1);
            LatLng latLng = new LatLng(lastPoint.latitude(), lastPoint.longitude());
            setCurrentMarkerPosition(latLng);
            if (this.waypoints.size() > 0) {
                fetchRoute();
            } else {
                hideLoading();
            }
        }
    }

    private void setFieldsFromSharedPreferences(NavigationRoute.Builder builder) {
        builder
                .language(getLanguageFromSharedPreferences())
                .voiceUnits(getUnitTypeFromSharedPreferences())
                .profile(getRouteProfileFromSharedPreferences());
    }

    private String getUnitTypeFromSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultUnitType = getString(R.string.default_unit_type);
        String unitType = sharedPreferences.getString(getString(R.string.unit_type_key), defaultUnitType);
        if (unitType.equals(defaultUnitType)) {
            unitType = localeUtils.getUnitTypeForDeviceLocale(this);
        }

        return unitType;
    }

    private Locale getLanguageFromSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultLanguage = getString(R.string.default_locale);
        String language = sharedPreferences.getString(getString(R.string.language_key), defaultLanguage);
        if (language.equals(defaultLanguage)) {
            return localeUtils.inferDeviceLocale(this);
        } else {
            return new Locale(language);
        }
    }

    private boolean getShouldSimulateRouteFromSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean(getString(R.string.simulate_route_key), false);
    }

    private boolean getStartFromLocationFromSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean(getString(R.string.start_from_location_key), true);
    }

    private void setStartFromLocationToSharedPreferences(boolean setStartFromLocation) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.start_from_location_key), setStartFromLocation);
        editor.apply();
    }

    private void setRouteProfileToSharedPreferences(String ghVehicle) {
        if (ghVehicle == null)
            return;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String routeProfile;
        switch (ghVehicle) {
            case "foot":
            case "hike":
                routeProfile = "walking";
                break;
            case "bike":
            case "mtb":
            case "racingbike":
                routeProfile = "cycling";
                break;
            default:
                routeProfile = "driving";
        }
        editor.putString(getString(R.string.route_profile_key), routeProfile);
        editor.apply();
    }

    private String getRouteProfileFromSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString(
                getString(R.string.route_profile_key), DirectionsCriteria.PROFILE_DRIVING
        );
    }

    private void launchNavigationWithRoute() {
        if (route == null) {
            Snackbar.make(mapView, R.string.error_route_not_available, Snackbar.LENGTH_SHORT).show();
            return;
        }

        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null && waypoints.size() > 1) {
            float[] distance = new float[1];
            Location.distanceBetween(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), waypoints.get(0).latitude(), waypoints.get(0).longitude(), distance);

            //Ask the user if he would like to recalculate the route from his current positions
            if (distance[0] > 100) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.error_too_far_from_start_title);
                builder.setMessage(R.string.error_too_far_from_start_message);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        waypoints.set(0, Point.fromLngLat(lastKnownLocation.getLongitude(), lastKnownLocation.getLatitude()));
                        fetchRoute();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        _launchNavigationWithRoute();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                _launchNavigationWithRoute();
            }
        } else {
            _launchNavigationWithRoute();
        }

    }

    private void _launchNavigationWithRoute() {
        NavigationLauncherOptions.Builder optionsBuilder = NavigationLauncherOptions.builder()
                .shouldSimulateRoute(getShouldSimulateRouteFromSharedPreferences())
                .directionsProfile(getRouteProfileFromSharedPreferences())
                .waynameChipEnabled(false);

        optionsBuilder.directionsRoute(route);

        NavigationLauncher.startNavigation(this, optionsBuilder.build());
    }

    private boolean validRouteResponse(Response<DirectionsResponse> response) {
        return response.body() != null && !response.body().routes().isEmpty();
    }

    private void hideLoading() {
        if (loading.getVisibility() == View.VISIBLE) {
            loading.setVisibility(View.INVISIBLE);
        }
    }

    private void showLoading() {
        if (loading.getVisibility() == View.INVISIBLE) {
            loading.setVisibility(View.VISIBLE);
        }
    }

    private void boundCameraToRoute() {
        if (route != null) {
            List<Point> routeCoords = LineString.fromPolyline(route.geometry(),
                    Constants.PRECISION_6).coordinates();
            List<LatLng> bboxPoints = new ArrayList<>();
            for (Point point : routeCoords) {
                bboxPoints.add(new LatLng(point.latitude(), point.longitude()));
            }
            if (bboxPoints.size() > 1) {
                try {
                    LatLngBounds bounds = new LatLngBounds.Builder().includes(bboxPoints).build();
                    // left, top, right, bottom
                    animateCameraBbox(bounds, CAMERA_ANIMATION_DURATION, padding);
                } catch (InvalidLatLngBoundsException exception) {
                    Toast.makeText(this, R.string.error_valid_route_not_found, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void animateCameraBbox(LatLngBounds bounds, int animationTime, int[] padding) {
        CameraPosition position = mapboxMap.getCameraForLatLngBounds(bounds, padding);
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), animationTime);
    }

    private void animateCamera(LatLng point) {
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, DEFAULT_CAMERA_ZOOM), CAMERA_ANIMATION_DURATION);
    }

    private void setCurrentMarkerPosition(LatLng position) {
        if (position != null) {
            if (currentMarker == null) {
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position);
                currentMarker = mapboxMap.addMarker(markerOptions);
            } else {
                currentMarker.setPosition(position);
            }
        }
    }

    private void updateWaypoints(List<Point> points) {
        if (points.size() > 24) {
            onError(R.string.error_too_many_waypoints);
            return;
        }
        clearRoute();
        this.waypoints = points;
        updateRouteAfterWaypointChange();
    }

    private void showSolutionInputDialog() {
        // Create an instance of the dialog fragment and show it
        SolutionInputDialog dialog = new SolutionInputDialog();
        dialog.setJobId(currentJobId);
        dialog.setVehicleId(currentVehicleId);
        dialog.show(getFragmentManager(), "gh-example");
    }

    private void showGeocodingInputDialog() {
        // Create an instance of the dialog fragment and show it
        GeocodingInputDialog dialog = new GeocodingInputDialog();
        dialog.setGeocodingInput(currentGeocodingInput);
        dialog.show(getFragmentManager(), "gh-example");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        EditText jobId = dialog.getDialog().findViewById(R.id.job_id);
        // Check if it's a solution fetch
        if (jobId != null) {
            EditText vehicleId = dialog.getDialog().findViewById(R.id.vehicle_id);

            fetchVrpSolution(jobId.getText().toString(), vehicleId.getText().toString());
        }
        // Check if it's a geocoding search
        EditText search = dialog.getDialog().findViewById(R.id.geocoding_input_id);
        if (search != null) {
            currentGeocodingInput = search.getText().toString();

            showLoading();
            String point = null;
            LatLng pointLatLng = this.mapboxMap.getCameraPosition().target;
            if (pointLatLng != null)
                point = pointLatLng.getLatitude() + "," + pointLatLng.getLongitude();
            new FetchGeocodingTask(this, getString(R.string.gh_key)).execute(new FetchGeocodingConfig(currentGeocodingInput, getLanguageFromSharedPreferences().getLanguage(), 5, false, point, "default"));
        }

    }

    @Override
    public void onError(int message) {
        Snackbar.make(mapView, message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onPostExecuteGeocodingSearch(List<GeocodingLocation> locations) {
        clearGeocodingResults();
        markers = new ArrayList<>(locations.size());

        if (locations.isEmpty()) {
            onError(R.string.error_geocoding_no_location);
            return;
        }

        //List<LatLng> bounds = new ArrayList<>();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null)
            boundsBuilder.include(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));

        for (GeocodingLocation location : locations) {
            GeocodingPoint point = location.getPoint();
            MarkerOptions markerOptions = new MarkerOptions();
            LatLng latLng = new LatLng(point.getLat(), point.getLng());
            markerOptions.position(latLng);
            boundsBuilder.include(latLng);
            markerOptions.title(location.getName());
            String snippet = "";
            if (location.getStreet() != null) {
                snippet += location.getStreet();
                if (location.getHousenumber() != null)
                    snippet += " " + location.getHousenumber();
                snippet += "\n";
            }
            if (location.getCity() != null) {
                if (location.getPostcode() != null)
                    snippet += location.getPostcode() + " ";
                snippet += location.getCity() + "\n";
            }
            if (location.getCountry() != null)
                snippet += location.getCountry() + "\n";
            if (location.getOsmId() != null) {
                snippet += "OSM-Id: " + location.getOsmId() + "\n";
                if (location.getOsmKey() != null)
                    snippet += "OSM-Key: " + location.getOsmKey() + "\n";
                if (location.getOsmType() != null)
                    snippet += "OSM-Type: " + location.getOsmType() + "\n";
            }
            snippet += "\n\n Tap on info window\n to add point to route";
            if (!snippet.isEmpty())
                markerOptions.snippet(snippet);
            markerOptions.icon(IconFactory.getInstance(this.getApplicationContext()).fromResource(R.drawable.ic_map_marker));
            markers.add(mapboxMap.addMarker(markerOptions));
        }

        animateCameraBbox(boundsBuilder.build(), CAMERA_ANIMATION_DURATION, padding);
        hideLoading();
    }

    @Override
    public void onPostExecute(List<Point> points) {
        if (getStartFromLocationFromSharedPreferences() && !points.isEmpty()) {
            // Remove the first point if we want to start from the current location
            points.remove(0);
        }
        updateWaypoints(points);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "This app needs location permissions to work properly.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            initLocationLayer();
        } else {
            Toast.makeText(this, "You didn't grant location permissions.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
