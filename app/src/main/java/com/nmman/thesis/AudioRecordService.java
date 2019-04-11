package com.nmman.thesis;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecordService extends Service {

    public static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    public static final int SAMPLE_RATE = 16000;
    public static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS, FORMAT) * 2;
    public static final long DURATION = 18000000; // 5 hours
    public static final String OUTPUT_DIR = "NightAudioRecorder";
    public static final String OUTPUT_FILE_PREFIX = "rc-";
    public static final String EXTENSION = "dat";
    private AudioRecord audioRecord;
    private AtomicBoolean inProgress = new AtomicBoolean(false);
    private AtomicBoolean scheduled = new AtomicBoolean(false);
    private Calendar startTime;
    private Calendar endTime;
    private int terminatedFlag = 0;
    private String errorMsg = "";
    private final File logFile = new File(Environment.getExternalStorageDirectory().toString(),
            OUTPUT_DIR + "/" + "log.txt");

    private final Thread recordThread = new Thread(() -> {
        File outputFile = new File(Environment.getExternalStorageDirectory().toString(),
                OUTPUT_DIR + "/" + createFileName());
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdir();
        }
        FileOutputStream fs = null;
        try {
            fs = new FileOutputStream(outputFile, false);
            while (inProgress.get()) {
                long time = Calendar.getInstance().getTimeInMillis();
                if (time >= endTime.getTimeInMillis()) {
                    AudioRecordService.this.stopSelf();
                    break;
                }
                ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                int result = audioRecord.read(buffer.array(), 0, BUFFER_SIZE);
                if (result >= 0) {
                    fs.write(buffer.array(), 0, BUFFER_SIZE);
                }
                buffer.clear();
            }
        } catch (Exception e) {
            terminatedFlag = -1;
            errorMsg = e.getMessage();
            e.printStackTrace();
            logError(e);
            AudioRecordService.this.stopSelf();
        } finally {
            log("End reading thread");
            terminatedFlag = 0;
            scheduled.set(false);
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    // Nothing to do
                }
            }
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC
                , SAMPLE_RATE
                , CHANNELS
                , FORMAT
                , BUFFER_SIZE);
    }

    @Override
    public void onDestroy() {
        stopRecord();
        log("Service destroy");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("Sevice start command");
        if (!scheduled.get()) {
            startNotification();
            startTime = Calendar.getInstance();
            endTime = Calendar.getInstance();
            endTime.setTimeInMillis(startTime.getTimeInMillis() + DURATION);
            scheduled.set(true);
            inProgress.set(true);
            log("Start recording");
            audioRecord.startRecording();
            recordThread.start();
            Toast.makeText(this, "Đã bắt đầu", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Chương trình vẫn đang chạy", Toast.LENGTH_SHORT).show();
        }
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void stopRecord() {
        log("Stop recording");
        inProgress.set(false);
        scheduled.set(false);
        audioRecord.stop();
        audioRecord.release();
        stopForeground(true);
        String msg = terminatedFlag >= 0 ? "Đã kết thúc" : ("Lỗi: " + errorMsg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void startNotification() {
        String msg = "Đang chạy ...";
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                .setContentTitle("Night Audio Recorder")
                .setContentText(msg)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .build();
        startForeground(1, notification);
    }

    private String createFileName() {
        long time = Calendar.getInstance().getTimeInMillis();
        return String.format("%s%s.%s", OUTPUT_FILE_PREFIX, String.valueOf(time), EXTENSION);
    }

    private void log(String line) {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(logFile, true))) {
            writer.println(currentTimeStr() + ": " + line);
        } catch (Exception e) {

        }
    }

    private void logError(Exception ex) {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(logFile, true))) {
            writer.println(currentTimeStr() + ": Exception");
            ex.printStackTrace(writer);
        } catch (Exception e) {

        }
    }

    private String currentTimeStr() {
        Calendar time = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
        return formatter.format(time.getTime());
    }
}
