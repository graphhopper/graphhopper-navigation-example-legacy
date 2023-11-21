## Update to MapLibre Navigation SDK for Android

We recommend that you use the successor of the GraphHopper Navigation SDK, which is the [MapLibre Navigation SDK](https://github.com/maplibre/maplibre-navigation-android).

To build an example with MapLibre follow these steps:

 1. Download IntelliJ with Android support (or IntelliJ Ultimate)
 2. git clone https://github.com/maplibre/maplibre-navigation-android
 3. Open the downloaded project in IntelliJ (this might take a while as it loads the dependencies)
 4. In app/src/main/res/values/developer-config.xml (created while the build) replace two lines with 'base_url' and 'mapbox_access_token' and set the correct GraphHopper GH_API_KEY:
   ```xml
   <string name="base_url" translatable="false">https://graphhopper.com/api/1/navigate/</string>
   <string name="mapbox_access_token" translatable="false">pk.[GH_API_KEY]</string>
   ```
 5. Additionally to the routing (provided by GraphHopper) you need a map tiles API from a different provider. In the same developer-config.xml enter the style URLs for e.g. mapilion:
   ```xml
   <string name="map_style_light" translatable="false">https://tiles.mapilion.com/assets/osm-bright/style.json?key=MAP_API_KEY</string>
   <string name="map_style_dark"  translatable="false">https://tiles.mapilion.com/assets/dark-matter/style.json?key=MAP_API_KEY</string>
   ```
 6. In NavigationUIActivity for NavigationRoute.builder uncomment `this.user("gh")` and `this.profile("car")`. (Do the same in the example "MockNavigationActivity".)
 7. You can now run the application on your Android device from IntelliJ (connect your device and a green triangle near your device name will show up)
 8. Click "Navigation UI" where it should show your location as a gray circle.
 9. Tap on the map to add a destination. It will now show the route on the map and a "Start Route" button will appear.
 10. Click the "Start Route" button and the turn by turn navigation will start.

---------

# Legacy Readme

## GraphHopper Navigation Sample

This Android example application showcases navigation based on instructions returned from the [GraphHopper Navigation](https://github.com/graphhopper/graphhopper/tree/master/navigation) component.

## Try

[Download from Google Play](https://play.google.com/store/apps/details?id=com.graphhopper.navigation.example).

## Features

![features](./files/graphhopper-navigation-example.png "Feature Screenshot")

- Turn-by-turn navigation including spoken turn instruction
- Route planning (from A to B via intermediate points)
- Load route optimization solutions from the [GraphHopper Route Optimization API](https://graphhopper.com/api/1/docs/route-optimization/)
- Load routes planned on [GraphHopper Maps](https://graphhopper.com/maps/)
- Search for places using the [GraphHopper Geocoding API](https://graphhopper.com/api/1/docs/geocoding/)
- 100% open source
- no dependency to Mapbox i.e. no Mapbox contract required. The Mapbox key can be a random string.

## Getting Started

Building your own turn-by-turn navigation app based on GraphHopper is easy. Just sign up for the [GraphHopper Directions API](https://www.graphhopper.com/products/).

GraphHopper does not provide map tiles. The navigation sdk is compatible with MVT vector tiles and raster tiles. There are several map providers, including:
- [Mapilion](https://mapilion.com/)
- [MapTiler](https://www.maptiler.com/cloud/)
- [ThunderForest](http://thunderforest.com/)

*Note: while it's possible to use raster tiles, we recommend vector tiles for mobile applications.*

Enter your GraphHopper API key and map style url in the developer config: `app/src/main/res/values/developer-config.xml`

You can fork this project and adapt it to your needs, or use it as orientation when developing your own application.

## License

This project stands under the Apache License.

It is a fork of the [Mapbox Sample App](https://github.com/mapbox/mapbox-navigation-android/tree/master/app),
which is licensed under the [MIT](https://github.com/mapbox/mapbox-navigation-android/blob/master/LICENSE) license.

## Updating the developer-config file

```
Ignore:
git update-index --assume-unchanged app/src/main/res/values/developer-config.xml
Unignore:
git update-index --no-assume-unchanged app/src/main/res/values/developer-config.xml
```
