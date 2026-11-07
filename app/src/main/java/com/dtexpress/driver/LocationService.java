package com.dtexpress.driver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.Looper;
import android.webkit.CookieManager;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationService extends Service {
    public static final String ACTION_START = "START";
    public static final String ACTION_STOP = "STOP";
    private LocationManager lm;
    private LocationListener listener;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean running = false;

    @Override public void onCreate() { super.onCreate(); lm=(LocationManager)getSystemService(LOCATION_SERVICE); }

    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&ACTION_STOP.equals(intent.getAction())) stopTracking();
        else if(intent!=null&&ACTION_START.equals(intent.getAction())) startTracking();
        return START_NOT_STICKY;
    }

    private void startTracking(){
        if(running)return;
        running=true;
        NotificationChannel ch=new NotificationChannel("gps","DT Express GPS",NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
        Notification n=new Notification.Builder(this,"gps")
                .setContentTitle("DT Express — Work GPS active")
                .setContentText("Location is shared only during your active work shift.")
                .setSmallIcon(com.dtexpress.driver.R.drawable.ic_launcher)
                .setOngoing(true).build();
        startForeground(44,n);
        listener=new LocationListener(){@Override public void onLocationChanged(Location l){
            final double lat=l.getLatitude(),lng=l.getLongitude(),acc=l.hasAccuracy()?l.getAccuracy():0;
            io.submit(()->post(lat,lng,acc));
        }};
        try{
            if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000L,10f,listener,Looper.getMainLooper());
            if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,10000L,25f,listener,Looper.getMainLooper());
        }catch(SecurityException e){stopTracking();}
    }

    private void post(double lat,double lng,double acc){
        HttpURLConnection c=null;
        try{
            URL u=new URL(MainActivity.BASE+"/api/driver/location");
            c=(HttpURLConnection)u.openConnection(); c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setConnectTimeout(10000); c.setReadTimeout(10000); c.setRequestProperty("Content-Type","application/json");
            String cookie=CookieManager.getInstance().getCookie(MainActivity.BASE); if(cookie!=null)c.setRequestProperty("Cookie",cookie);
            String body="{\"lat\":"+lat+",\"lng\":"+lng+",\"accuracy\":"+acc+"}";
            try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}
            int code=c.getResponseCode();
            if(code==401||code==403||code==409) stopTracking();
        }catch(Exception ignored){} finally{if(c!=null)c.disconnect();}
    }

    private void stopTracking(){
        if(!running){stopSelf();return;} running=false;
        if(listener!=null)try{lm.removeUpdates(listener);}catch(Exception ignored){}
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
    }

    @Override public void onDestroy(){if(listener!=null)try{lm.removeUpdates(listener);}catch(Exception ignored){}io.shutdownNow();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
