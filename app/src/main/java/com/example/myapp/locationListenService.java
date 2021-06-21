package com.example.myapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class locationListenService extends JobIntentService {
    final Handler mHandler = new Handler();
    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "Channel_Id";
    static final long TIMEBETWEENUPDATE = 3000;
    String path;
    String[] currentStreetCity;
    String[] HomeStreetCity;
    int min_pics_int;
    long start;
    long end;
    String album_name;
    public Boolean stopLocationListner = false;


    @Override
    public void onCreate() {
        super.onCreate();

    }


    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            path = (String) extras.get("path");
            currentStreetCity = (String[]) extras.get("currentStreetCity");
            HomeStreetCity = (String[]) extras.get("HomeStreetCity");
            min_pics_int = (int) extras.get("min_pics_int");

        }

        startLocationListen();
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        return Service.START_NOT_STICKY;
    }

    // Helper for showing tests
    void showToast(final CharSequence text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(locationListenService.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void startLocationListen() {

        while (!(stopLocationListner)) {

            currentStreetCity = getCurrentLocation();
            while ((currentStreetCity[0].equals((HomeStreetCity)[0])) && (!stopLocationListner)) {
                try {
                    Thread.sleep(TIMEBETWEENUPDATE); /// need to change

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                currentStreetCity = getCurrentLocation();

            }
            start = new Date().getTime();
            while (!(currentStreetCity)[0].equals((HomeStreetCity)[0]) && (!stopLocationListner)) {
                try {
                    Thread.sleep(TIMEBETWEENUPDATE);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                currentStreetCity = getCurrentLocation();
            }

            if (!stopLocationListner) {

                end = new Date().getTime();


                //cheack if there are enough picture since left home until comming back
                int picsAmount = countPicsSinceLeft();

                if (picsAmount >= min_pics_int) {
                    try {
                        album_name = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        CreateAlbum();
                        createNotification(album_name);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        stopLocationListner = false;
        super.onDestroy();


    }

    //create new album and move the suitable photos into it
    public void CreateAlbum() throws IOException {
        //make array of all pics
        File directory = new File(path);
        File[] photos = directory.listFiles();
        File newDir = new File(path + album_name);
        newDir.mkdir();

        for (int i = 0; i < photos.length; i++){
            if ((!photos[i].isDirectory()) && (photos[i].lastModified() > start) && (photos[i].lastModified() < end)){
                exportFile(photos[i], newDir,i);
                photos[i].delete();
            }
        }
    }


    private File exportFile(File src, File dst,int i) throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File expFile = new File(dst.getPath() + File.separator + "IMG_" + i + timeStamp + ".jpg");
        FileChannel inChannel = null;
        FileChannel outChannel = null;

        try {
            inChannel = new FileInputStream(src).getChannel();
            outChannel = new FileOutputStream(expFile).getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }

        return expFile;
    }

    public int countPicsSinceLeft(){
        File directory = new File(path);
        File[] photos = directory.listFiles();
        int pics_counter = 0;
        for (int i = 0; i < photos.length; i++) {
            if ((!photos[i].isDirectory()) && (photos[i].lastModified() > start) && (photos[i].lastModified() < end)) {
                pics_counter++;
            }
        }
        return pics_counter;
    }

    public String[] getCurrentLocation() {
        LocationManager locationManager;
        LocationListener locationListener;
        double currentLongitude = -1;
        double currentLatitude = -1;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //get location if changed
        locationListener = new MyLocationListener(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return new String[]{"",""};
        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10, locationListener,Looper.getMainLooper());


        while ((currentLongitude == -1) && (currentLatitude == -1)) {
            try {
                Thread.sleep(1000);
                currentLongitude = ((MyLocationListener)locationListener).getLongitude();
                currentLatitude = ((MyLocationListener)locationListener).getLatitude();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
//        stop from getting location
        locationManager.removeUpdates(locationListener);
        return getAddress(currentLongitude, currentLatitude);
    }

    private String[] getAddress(double Longitude, double Latitude){
        String address = null;
        String city = null;
        Geocoder gcd = new Geocoder(this, Locale.US);
        List<Address> addresses;
        try {
            addresses = gcd.getFromLocation(Latitude, Longitude, 1);
            if (addresses.size() > 0) {
                address = addresses.get(0).getAddressLine(0);
                city = addresses.get(0).getLocality();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if ((city == null) || (address == null)){
            return currentStreetCity;
        }
        String street = (address.split(" "))[0];
        return new String[]{street,city};
    }

    public void createNotification(String message) {
        Intent landingIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingLandingIntent = PendingIntent.getActivity(this, 0,
                landingIntent,0);
//        Notification notification = notificationBuilder;

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);




        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId =
                    "MY_CHANNEL_ID";
            NotificationChannel channel = new NotificationChannel(channelId, "Albom created",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("created new albom");
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder builder = new
                    NotificationCompat.Builder(getApplicationContext(), channelId);

            Notification notification = builder.setContentIntent(pendingLandingIntent)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.logo)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setContentTitle("New Albom Created!")
                    .setContentText("Albom name: " + message)
                    .build();

            notificationManager.notify(1, notification);
        }
    }

}