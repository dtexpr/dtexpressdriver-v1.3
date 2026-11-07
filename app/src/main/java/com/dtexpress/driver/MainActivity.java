package com.dtexpress.driver;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Environment;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.webkit.PermissionRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static final String BASE = "https://dt-express-driver.hatchable.site";
    private static final int LOCATION_REQUEST = 20;
    private static final int NOTIFICATION_REQUEST = 21;
    private WebView web;
    private LocationManager lm;
    private boolean waitingForStart = false;
    private static final int FILE_CHOOSER_REQUEST = 501;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraOutputUri;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        lm = (LocationManager)getSystemService(LOCATION_SERVICE);
        web = findViewById(R.id.web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                launchPhotoPicker();
                return true;
            }
        });
        web.addJavascriptInterface(new DriverBridge(), "AndroidDriver");
        web.setWebViewClient(new WebViewClient());
        web.loadUrl(BASE + "/login?next=/");
    }

    private void launchPhotoPicker() {
        Intent gallery = new Intent(Intent.ACTION_GET_CONTENT);
        gallery.addCategory(Intent.CATEGORY_OPENABLE);
        gallery.setType("image/*");

        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (camera.resolveActivity(getPackageManager()) != null) {
            try {
                File photo = File.createTempFile("dtexpress_photo_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
                cameraOutputUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photo);
                camera.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputUri);
                camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (IOException e) {
                camera = null;
                cameraOutputUri = null;
            }
        } else {
            camera = null;
            cameraOutputUri = null;
        }

        Intent chooser = Intent.createChooser(gallery, "Choose a photo or take a picture");
        if (camera != null) chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{camera});
        try {
            startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
        } catch (Exception e) {
            if (filePathCallback != null) filePathCallback.onReceiveValue(null);
            filePathCallback = null;
            cameraOutputUri = null;
            Toast.makeText(this, "Unable to open camera or gallery", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            Uri[] results = null;
            if (resultCode == RESULT_OK) {
                if (data != null && data.getData() != null) {
                    results = new Uri[]{data.getData()};
                } else if (cameraOutputUri != null) {
                    results = new Uri[]{cameraOutputUri};
                }
            }
            if (filePathCallback != null) filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            cameraOutputUri = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        super.onDestroy();
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationForStart() {
        waitingForStart = true;
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        obtainOneLocationAndStart();
    }

    private void obtainOneLocationAndStart() {
        if (!hasLocationPermission()) return;
        try {
            Location last = null;
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (last != null && System.currentTimeMillis() - last.getTime() < 120000) {
                startWithLocation(last);
                return;
            }
            final LocationListener oneShot = new LocationListener() {
                @Override public void onLocationChanged(Location location) {
                    lm.removeUpdates(this);
                    startWithLocation(location);
                }
            };
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, oneShot, Looper.getMainLooper());
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, oneShot, Looper.getMainLooper());
            web.postDelayed(() -> {
                try { lm.removeUpdates(oneShot); } catch(Exception ignored) {}
                if (waitingForStart) {
                    waitingForStart = false;
                    web.evaluateJavascript("window.nativeStartError && window.nativeStartError('Could not get a GPS location. Turn on Location and try again.');", null);
                }
            }, 15000);
        } catch (SecurityException e) {
            waitingForStart = false;
            web.evaluateJavascript("window.nativeStartError && window.nativeStartError('Location permission is required during work.');", null);
        }
    }

    private void startWithLocation(Location l) {
        if (!waitingForStart) return;
        waitingForStart = false;
        double acc = l.hasAccuracy() ? l.getAccuracy() : 0;
        String js = "window.nativeStartLocation && window.nativeStartLocation(" + l.getLatitude() + "," + l.getLongitude() + "," + acc + ");";
        web.evaluateJavascript(js, null);
    }

    public void stopNativeTracking() {
        Intent i = new Intent(this, LocationService.class);
        i.setAction(LocationService.ACTION_STOP);
        startService(i);
    }

    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) {
        super.onRequestPermissionsResult(r,p,g);
        if (r == LOCATION_REQUEST) {
            if (hasLocationPermission()) obtainOneLocationAndStart();
            else web.evaluateJavascript("window.nativeStartError && window.nativeStartError('Location permission is required during work.');", null);
        }
    }

    public void confirmNativeTrackingStarted() {
        Intent i = new Intent(this, LocationService.class);
        i.setAction(LocationService.ACTION_START);
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    public class DriverBridge {
        @JavascriptInterface public void startWork() { runOnUiThread(MainActivity.this::requestLocationForStart); }
        @JavascriptInterface public void confirmStarted() { runOnUiThread(MainActivity.this::confirmNativeTrackingStarted); }
        @JavascriptInterface public void stopWork() { runOnUiThread(MainActivity.this::stopNativeTracking); }
    }
}
