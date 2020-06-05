# GraphHopper Navigation Sample

This Android example application showcases navigation based on instructions returned from the [GraphHopper Navigation](https://github.com/graphhopper/graphhopper-navigation) component.

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
