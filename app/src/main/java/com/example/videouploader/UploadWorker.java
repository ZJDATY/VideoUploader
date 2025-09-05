package com.example.videouploader;

import static com.example.videouploader.UploadConfig.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.Manifest;
import android.content.pm.PackageManager;

import java.io.IOException;


import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import androidx.work.Constraints;
import androidx.work.ForegroundInfo;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;


import java.io.File;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class UploadWorker extends Worker {



    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {

        SharedPreferences prefs = getApplicationContext().getSharedPreferences("ftp_config", Context.MODE_PRIVATE);
        // 从界面保存的配置里取值
        FTP_HOST = prefs.getString("ftpServer", "");
        FTP_PORT = prefs.getInt("ftpPort", 21);  // 默认 21
        FTP_USER = prefs.getString("ftpUser", "");
        FTP_PASS = prefs.getString("ftpPassword", "");
        LOCAL_DIR = prefs.getString("localDir", "");
        REMOTE_DIR = prefs.getString("remoteDir", "");
        boolean autoDelete = prefs.getBoolean("autoDelete", false);

        try {
            // 前台通知
            setForegroundAsync(createForegroundInfo("准备扫描并上传…"));


            File dir = new File(LOCAL_DIR);
            if (!dir.exists() || !dir.isDirectory()) {
                notifySmall("本地目录不存在：" + LOCAL_DIR);
                return Result.success(); // 目录不存在也不算失败，等待下次
            }

            File[] files = dir.listFiles((f) -> {
                if (!f.isFile()) return false;
                String name = f.getName().toLowerCase();
                int dot = name.lastIndexOf('.');
                if (dot < 0) return false;
                String ext = name.substring(dot + 1);
                return VIDEO_EXTS.contains(ext);
            });

            if (files == null || files.length == 0) {
                updateForegroundText("没有可上传的视频文件。");
                SystemClock.sleep(1000);
                return Result.success();
            }

            FTPClient ftp = new FTPClient();
            ftp.setConnectTimeout(15000);
            ftp.setDataTimeout(60000);
            ftp.setDefaultTimeout(15000);

            try {
                updateForegroundText("正在连接 FTP…");
                //Toast.makeText(getApplicationContext(), "Transform Selected Code...", Toast.LENGTH_SHORT).show();
                ftp.connect(FTP_HOST, FTP_PORT);
                if (!ftp.login(FTP_USER, FTP_PASS)) {
                    notifySmall("FTP 登录失败");
                    //Toast.makeText(getApplicationContext(), "FTP 登录失败", Toast.LENGTH_SHORT).show();
                    return Result.retry();
                }
                ftp.enterLocalPassiveMode();
                ftp.setFileType(FTP.BINARY_FILE_TYPE);

                // 确保远端目录存在
                if (!ensureRemoteDir(ftp, REMOTE_DIR)) {
                    notifySmall("无法创建/进入远端目录：" + REMOTE_DIR);
                    //Toast.makeText(getApplicationContext(), "无法创建/进入远端目录：" + REMOTE_DIR, Toast.LENGTH_SHORT).show();
                    return Result.retry();
                }

                int success = 0;
                int success1 = 0;
                int fail = 0;

                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    // 进度更新
                    updateForegroundText("上传中（" + (i + 1) + "/" + files.length + "）：" + f.getName());
                    //Toast.makeText(getApplicationContext(), "上传中（" + (i + 1) + "/" + files.length + "）：" + f.getName(), Toast.LENGTH_SHORT).show();

                    // 跳过正在写入的文件
                    if (!isFileStable(f)) {
                        // 下次再试
                        continue;
                    }

                    boolean uploaded = uploadSingleFile(ftp, f, f.getName());
                    if (uploaded) {
                        //Toast.makeText(getApplicationContext(), "上传成功", Toast.LENGTH_SHORT).show();
                        success++;
                        //SharedPreferences prefs = getApplicationContext().getSharedPreferences("ftp_config", Context.MODE_PRIVATE);

                        if (autoDelete)
                        {
                            boolean deleted = f.delete();

                            if (!deleted) {
                                // 删除失败也不算致命错误；下次仍会被发现并尝试删除
                                //Toast.makeText(getApplicationContext(), "删除失败，下次仍会被发现并尝试删除！", Toast.LENGTH_SHORT).show();
                            }
                            success1++;
                        }

                    } else {
                        fail++;
                        //Toast.makeText(getApplicationContext(), "上传失败", Toast.LENGTH_SHORT).show();
                    }
                }

                notifySmall("上传完成：成功 " + success  + "，失败 " + fail+ "，删除成功： " + success1);
                return fail > 0 ? Result.retry() : Result.success();

            } finally {
                if (ftp.isConnected()) {
                    try { ftp.logout(); } catch (Exception ignored) {}
                    try { ftp.disconnect(); } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            notifySmall("上传异常：" + e.getMessage());
            return Result.retry();
        }
    }

    // 单个文件上传
    private boolean uploadSingleFile(FTPClient ftp, File local, String remoteName) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(local);
            return ftp.storeFile(remoteName, fis);
        } catch (IOException e) {
            return false;
        } finally {
            if (fis != null) try { fis.close(); } catch (IOException ignored) {}
        }
    }

    // 递归创建远端目录
    private boolean ensureRemoteDir(FTPClient ftp, String remoteDir) throws IOException {
        if (remoteDir == null || remoteDir.trim().isEmpty() || "/".equals(remoteDir)) {
            return ftp.changeWorkingDirectory("/");
        }
        String path = remoteDir.replace('\\','/');

        if (path.startsWith("/")) {
            ftp.changeWorkingDirectory("/");
            path = path.substring(1);
        }

        String[] parts = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append("/").append(p);
            if (!ftp.changeWorkingDirectory(sb.toString())) {
                if (!ftp.makeDirectory(sb.toString())) {
                    return false;
                }
                if (!ftp.changeWorkingDirectory(sb.toString())) {
                    return false;
                }
            }
        }
        return true;
    }

    // 判定文件是否“稳定”（短时间大小不变）
    private boolean isFileStable(File f) {
        long s1 = f.length();
        SystemClock.sleep(STABLE_CHECK_INTERVAL_MS);
        long s2 = f.length();
        return s1 == s2;
    }

    // ===== 前台通知相关 =====
    private ForegroundInfo createForegroundInfo(String text) {
        createChannelIfNeeded();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NOTIF_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("视频自动上传")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        return new ForegroundInfo(NOTIF_ID, builder.build());
    }

    private void updateForegroundText(String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NOTIF_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("视频自动上传")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        // 权限检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // 权限没授予，不发通知
                return;
            }
        }
        NotificationManagerCompat.from(getApplicationContext()).notify(NOTIF_ID, builder.build());
    }
    private void notifySmall(String text) {
        createChannelIfNeeded();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NOTIF_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle("视频自动上传")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // 权限检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // 权限没授予，不发通知
                return;
            }
        }

        NotificationManagerCompat.from(getApplicationContext())
                .notify((int) (System.currentTimeMillis() % 100000), builder.build());
    }


    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel ch =
                    new android.app.NotificationChannel(NOTIF_CHANNEL_ID, "上传服务", android.app.NotificationManager.IMPORTANCE_LOW);
            android.app.NotificationManager nm = (android.app.NotificationManager)
                    getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    // 提供给外部使用的网络约束
    public static Constraints defaultConstraints() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }
}
