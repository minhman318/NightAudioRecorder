package com.nmman.thesis;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String CHANNEL_ID = "13";
    public static final int PERMISSION_REQUEST_CODE = 13;
    private static final String[] PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ?
            new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.FOREGROUND_SERVICE
            } : new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    private Button startBtn;
    private Button stopBtn;
    private Button supportBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startBtn = findViewById(R.id.start_btn);
        stopBtn = findViewById(R.id.stop_btn);
        supportBtn = findViewById(R.id.support_btn);
        initView();
        createNotificationChannel();
    }

    private void initView() {
        startBtn.setOnClickListener((v) -> {
            if (checkAndRequestPermission(PERMISSIONS)) {
                Intent intent = new Intent(MainActivity.this, AudioRecordService.class);
                startService(intent);
            }
            Intent intent = new Intent(this, CompletionActivity.class);
            startActivity(intent);
            finish();
        });
        stopBtn.setOnClickListener((v) -> {
            stopService(new Intent(MainActivity.this, AudioRecordService.class));
        });
        supportBtn.setOnClickListener((v) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://messaging/" + "100003333298743"));
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndRequestPermission(PERMISSIONS);
    }

    private boolean checkAndRequestPermission(String[] permissions) {
        List<String> reqList = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                reqList.add(p);
            }
        }
        if (reqList.isEmpty()) {
            return true;
        }
        String[] reqArr = new String[reqList.size()];
        reqArr = reqList.toArray(reqArr);
        for (int i = 0; i < reqList.size(); i++) {
            ActivityCompat.requestPermissions(this, reqArr, PERMISSION_REQUEST_CODE);
        }
        return false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Recording";
            String description = "Show a notification during recording";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
