package graphhopper.com.graphhopper_navigation_example;

import com.mapbox.geojson.Point;

import java.util.List;

public interface FetchSolutionTaskCallbackInterface {

    void onError(int message);

    void onPostExecute(List<Point> points);

}
