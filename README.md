# DT Express Driver App

Version 1.2 — DT Express Driver GPS test app.

## User flow
- Each of up to 8 drivers signs in with their own email using Hatchable's passwordless verification code.
- The session is remembered on the phone; the code is not requested every time the app opens.
- On first successful sign-in, the driver enters first and last name once.
- START WORK begins GPS tracking and the Android foreground location service.
- END WORK closes the shift and stops GPS tracking.
- The admin dashboard identifies drivers by their entered full name and email.

Backend: https://dt-express-driver.hatchable.site
Admin: https://dt-express-driver.hatchable.site/admin.html


## Version 1.3 – Camera and gallery chooser
- WebView file chooser is connected through `WebChromeClient.onShowFileChooser`.
- Drivers can select an existing image from the gallery or take a new photo with the device camera.
- Camera output is written to app-specific Pictures storage through AndroidX FileProvider.
- Existing GPS, START WORK, END WORK, and WebView bridge code is preserved.
- This is source code; build and test the APK in Android Studio / GitHub Actions.
