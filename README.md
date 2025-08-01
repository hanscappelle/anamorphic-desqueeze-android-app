# Anamorphic Desqueeze App

Simple open source Android app to change the aspect ratio of any image using default intents.

This app allows you to pick any image from your preferred Android Image Gallery and share it directly into this app resulting in a newly generated, desqueezed image in your Gallery.

For this the width of the original image is multiplied by 1.33 to get the full image width. Current release is limited to images (not video) and has a fixed 1.33 ratio and 85% JPEG output. Future releases may introduce more settings to control this.

Project https://github.com/hanscappelle/anamorphic-desqueeze-android-app.git

## How to Use

To use this app:

- Start from your typical image gallery and use the share option selecting this app to open the image with.
- Landing in this app again, select the desired image ratio and execute the resize.
- Finally you\'ll find the result in your preferred gallery app again.

# Features

Currently very basic implementation

- Open an image from your preferred Image Gallery using default Android Intent
- Desqueeze to fixed aspect ratio of width*1,33 x height
- Fixed JPEG full original size 85% compression
- Opens result in Gallery again

## Upcoming features

- Allow for different aspect ratio's like 1,55 and/or fully custom ones
- General UI improvements
- Configuration for export format
- Also support Video !? (unlikely, would need background processing and what not)

## Release Info

- use original release key (package re-used)
- create an apk (not app bundle yet)
- create tag on each release

# Version History

## 0.2.0

Update app icon and app store listing

## 0.1.0

First version, minimal app release

# Resources

About sharing image intent on Android: https://developer.android.com/training/sharing/receive

Display image in compose, including from uri:
https://developer.android.com/develop/ui/compose/graphics/images/loading
https://stackoverflow.com/questions/77226214/jetpack-compose-load-uri-image-on-screen

About permissions to write to storage:
https://developer.android.com/training/data-storage/shared/media

App logo created with:
https://hotpot.ai/logo-generator/create