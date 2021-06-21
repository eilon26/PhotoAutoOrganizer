package com.example.myapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentTransaction;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity  {
    private SettingsFragment settingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().beginTransaction().add(R.id.OuterFragmentContainer, settingsFragment).commit();

        openPermmisionWindow();

    }

    private void openPermmisionWindow() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Enable Permissions");
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        alert.show();
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
                    .setContentTitle("you have a new albom name "+ message+ " change it if you want")
                    .build();

            notificationManager.notify(1, notification);
        }
    }




}