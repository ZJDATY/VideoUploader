package com.example.videouploader;

import static com.example.videouploader.UploadConfig.PERIODIC_MINUTES;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class BootReceiver extends BroadcastReceiver {

    private static final String UNIQUE_PERIODIC_NAME = "video_auto_upload_periodic";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 开机后重新安排周期任务
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                UploadWorker.class,
                PERIODIC_MINUTES, TimeUnit.MINUTES)
                .setConstraints(UploadWorker.defaultConstraints())
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req
        );

        Toast.makeText(context, "自动上传服务已恢复", Toast.LENGTH_SHORT).show();
    }
}
