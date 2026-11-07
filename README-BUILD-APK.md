# DT Express Driver — one-click APK build

This project includes a GitHub Actions workflow that builds a directly installable Android APK.

## On a computer
1. Create a new **private** GitHub repository.
2. Upload all files from this project.
3. Open **Actions** → **Build DT Express Driver APK**.
4. Run the workflow (or push to `main`).
5. When it finishes, open the workflow run and download the artifact **DTExpressDriver-debug-apk**.
6. Inside it is `app-debug.apk` — this is the single APK file you can send to a test driver.

The debug APK is intended for controlled testing. For Google Play we will later make a signed release/AAB.
