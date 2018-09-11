package graphhopper.com.graphhopper_navigation_example;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ArrayAdapter;

import com.mapbox.mapboxsdk.attribution.Attribution;
import com.mapbox.mapboxsdk.attribution.AttributionParser;
import com.mapbox.mapboxsdk.maps.AttributionDialogManager;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.sources.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class is mostly based on {@link AttributionDialogManager}, but adds GraphHopper as
 * additional attribution.
 */
public class GHAttributionDialogManager extends AttributionDialogManager {

    private final Context context;
    private final MapboxMap mapboxMap;
    private Set<Attribution> attributionSet;

    public GHAttributionDialogManager(@NonNull Context context, @NonNull MapboxMap mapboxMap) {
        super(context, mapboxMap);
        this.context = context;
        this.mapboxMap = mapboxMap;
    }

    // Called when someone presses the attribution icon on the map
    @Override
    public void onClick(View view) {
        attributionSet = new GHAttributionDialogManager.AttributionBuilder(mapboxMap).build();

        boolean isActivityFinishing = false;
        if (context instanceof Activity) {
            isActivityFinishing = ((Activity) context).isFinishing();
        }

        // check is hosting activity isn't finishing
        // https://github.com/mapbox/mapbox-gl-native/issues/11238
        if (!isActivityFinishing) {
            showAttributionDialog(getAttributionTitles());
        }
    }

    protected void showAttributionDialog(String[] attributionTitles) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.app_name);
        builder.setAdapter(new ArrayAdapter<>(context, R.layout.attribution_list_item, attributionTitles), this);
        builder.show();
    }

    private String[] getAttributionTitles() {
        List<String> titles = new ArrayList<>();
        for (Attribution attribution : attributionSet) {
            titles.add(attribution.getTitle());
        }
        return titles.toArray(new String[titles.size()]);
    }


    private static class AttributionBuilder {

        private final MapboxMap mapboxMap;

        AttributionBuilder(MapboxMap mapboxMap) {
            this.mapboxMap = mapboxMap;
        }

        private Set<Attribution> build() {
            List<String> attributions = new ArrayList<>();
            attributions.add("<a href=\"https://www.graphhopper.com/\" target=\"_blank\">Powered by GraphHopper API</a>");
            for (Source source : mapboxMap.getSources()) {
                attributions.add(source.getAttribution());
            }

            return new AttributionParser.Options()
                    .withCopyrightSign(true)
                    // TODO when using Mapbox as Tilesource we should keep this, should we automatically remove it otherwise?
                    .withImproveMap(true)
                    .withAttributionData(attributions.toArray(new String[attributions.size()]))
                    .build().getAttributions();
        }
    }
}
