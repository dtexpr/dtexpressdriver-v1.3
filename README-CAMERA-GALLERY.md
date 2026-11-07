# DT Express Driver Android — Camera & Gallery build

This project is based on the branded DT Express Driver Android project.

## Included
- Existing WebView app and Hatchable driver portal URL
- Existing GPS location bridge and foreground LocationService
- Existing START WORK / END WORK native bridge
- WebView file chooser (`WebChromeClient.onShowFileChooser`)
- Gallery image selection
- Camera capture using Android FileProvider
- Android 11+ package visibility declaration for the camera app

## Important
This archive is the Android Studio source project, not an APK.
The camera/gallery flow must still be tested on the target Android phone after building.
The existing backend order-photo upload and dashboard storage are not changed by this native wrapper update.

## GitHub upload
Extract this ZIP on your computer first. On the repository page, use **Add file → Upload files**
and drag the *contents of the extracted `dtapp` folder* into the upload area, preserving folders.
Do not upload only this ZIP if you want GitHub Actions to build the Android project.
Commit the uploaded files to the `main` branch.
