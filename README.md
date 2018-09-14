# GraphHopper Navigation Sample

This application provides navigation based on instructions returned from the [GraphHopper Navigation](https://github.com/graphhopper/graphhopper-navigation) component. 

## Serverless

It is possible to create your navigation experience without any server component. Then you'll need to sign up at the [GraphHopper Directions API](https://www.graphhopper.com/products/) and at [MapTiler](https://www.maptiler.com/cloud/) or compatible vector tile API providers.

## License

This project stands under the Apache License.

It was originally based on the [Mapbox Sample App](https://github.com/mapbox/mapbox-navigation-android/tree/master/app),
which is licensed under the [MIT](https://github.com/mapbox/mapbox-navigation-android/blob/master/LICENSE) license.

## Screenshots

![screenshots of demo](https://www.graphhopper.com/wp-content/uploads/2018/09/overview-screenshots-1024x585.png)

## Getting started

In order to try this app, please enter your credentials in `app/src/main/res/values/developer-config.xml`.

## Updating the developer-config file

```
Ignore:
git update-index --assume-unchanged app/src/main/res/values/developer-config.xml
Unignore:
git update-index --no-assume-unchanged app/src/main/res/values/developer-config.xml
```
