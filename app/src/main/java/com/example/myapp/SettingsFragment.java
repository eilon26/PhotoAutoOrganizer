package com.example.myapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsFragment extends PreferenceFragmentCompat{
    public String album_name;
    private String path = "/storage/emulated/0/DCIM/Camera/";
    public String[] currentStreetCity = {"","Tel Aviv"};
    public String[] HomeStreetCity = {"", "Tel Aviv"};
    public int min_pics_int = 10;
    public long start;
    public long end;
    public Boolean stopLocationListner = false;

    LocationManager locationManager;
    LocationListener locationListener;
    Thread listnLocation;

    MainFragment mainFragment;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        Preference city = findPreference("city_name");
        city.setSummary(HomeStreetCity[1]);
        city.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                HomeStreetCity[1] = (String) newValue;
                city.setSummary(HomeStreetCity[1]);
                return true;
            }
        });

        Preference street = findPreference("street_name");
        street.setSummary(HomeStreetCity[0]);
        street.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                HomeStreetCity[0] = (String) newValue;
                street.setSummary(HomeStreetCity[0]);
                return true;
            }
        });

        Preference homeLocation = findPreference("get_location");
        homeLocation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                HomeStreetCity = getCurrentLocation();
                street.setSummary(HomeStreetCity[0]);
                city.setSummary(HomeStreetCity[1]);
                Log.d("my curr location", HomeStreetCity[0]);
                return false;
            }
        });

        SwitchPreferenceCompat activate = findPreference("activate");
        activate.setChecked(false);
        activate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                stopLocationListner = false;

                if ((Boolean) newValue) {
                    startLocationListnService();
//                    try {
//                        StartLocationListen();
//                    } catch (IOException | InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }else{
//                    if ((listnLocation != null) && (listnLocation.isAlive())) stopLocationListner = true;

                    try {
                        Thread.sleep(5000);
                        Toast.makeText(getActivity().getApplicationContext(), "thanks for using the app! ", Toast.LENGTH_LONG).show();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    getActivity().finish();
                    System.exit(0);
                }

                return true;
            }
        });

        ListPreference choose_path = findPreference("pics_path");
        choose_path.setSummary("" + path);
        choose_path.setValueIndex(0);
        choose_path.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                path = (String) newValue;
                choose_path.setSummary("" + path);
                return true;
            }
        });

        Preference min_pics = findPreference("min_pictures");
        min_pics.setSummary("" + min_pics_int);
        min_pics.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String min_pics_S = (String) newValue;
                Log.d("min_pic", min_pics_S);


                try {
                    min_pics_int = Integer.parseInt(min_pics_S);
                } catch (Exception e) {
                    min_pics_int = 10;
                    Toast.makeText(getActivity().getApplicationContext(), "invalid number of minimum pics", Toast.LENGTH_LONG).show();
                }
                min_pics.setSummary("" + min_pics_int);
                return true;
            }
        });



    }


    private String[] getAddress(double Longitude, double Latitude){
        String address = null;
        String city = null;
        Geocoder gcd = new Geocoder(getActivity(), Locale.US);
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
        return new String[]{(address.split(" "))[0],city};
    }

    //return the city in the user current location
    public String[] getCurrentLocation() {
        double currentLongitude = -1;
        double currentLatitude = -1;
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        //get location if changed
        locationListener = new MyLocationListener(getActivity());
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return new String[]{"",""};
        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10, locationListener);

        //get current location
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return new String[]{"",""};
        }
        Location getLastLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (getLastLocation !=null) {
            currentLongitude = getLastLocation.getLongitude();
            currentLatitude = getLastLocation.getLatitude();
        }


        while ((currentLongitude == -1) && (currentLatitude == -1)) {
            try {
                Thread.sleep(1000);
                currentLongitude = ((MyLocationListener)locationListener).getLongitude();
                currentLatitude = ((MyLocationListener)locationListener).getLatitude();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //stop from getting location
        locationManager.removeUpdates(locationListener);
        return getAddress(currentLongitude, currentLatitude);
    }


    private void StartLocationListen() throws IOException, InterruptedException {
        listnLocation = new ListenLeaveComebackHome(this);
        listnLocation.start();
    }

    public void PopupWindowForCreatingAlbum() throws IOException {
//

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle("Enter Album Name:");

                // Set an EditText view to get user input
                final EditText input = new EditText(getActivity());
                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        album_name = "" + input.getText();
                        try {
                            CreateAlbum(album_name);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        album_name = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        try {
                            CreateAlbum(album_name);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                alert.show();
            }
        });


//        album_name = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
//        CreateAlbum(album_name);
        ((MainActivity) getActivity()).createNotification(album_name);

    }

    //create new album and move the suitable photos into it
    public void CreateAlbum(String name) throws IOException {
        //make array of all pics
        File directory = new File(path);
        File[] photos = directory.listFiles();
        File newDir = new File(path + album_name);
        newDir.mkdir();

        for (int i = 0; i < photos.length; i++){
            if ((!photos[i].isDirectory())  && (photos[i].lastModified() > start) && (photos[i].lastModified() < end)){
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

    private void startLocationListnService(){
        Intent intent = new Intent(getActivity().getApplicationContext(), locationListenService.class);
        intent.putExtra("path", path);
        intent.putExtra("currentStreetCity", currentStreetCity);
        intent.putExtra("HomeStreetCity", HomeStreetCity);
        intent.putExtra("min_pics_int", min_pics_int);
        locationListenService.enqueueWork(getActivity().getApplicationContext(), locationListenService.class, 1000, intent);
//        getActivity().startService(intent);

    }



}
