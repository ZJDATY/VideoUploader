package com.example.videouploader;

import static com.example.videouploader.UploadConfig.*;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;

import android.widget.Button;
import android.widget.EditText;

import android.widget.Switch;
import android.widget.Toast;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.apache.commons.net.ftp.FTPClient;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView tvLog;
    private ScrollView scrollView;
    private static final String UNIQUE_PERIODIC_NAME = "video_auto_upload_periodic";

    private final ActivityResultLauncher<String[]> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean read = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false));
                boolean write = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.WRITE_EXTERNAL_STORAGE, false));
                if (read && write) {
                    Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "请授予存储权限以访问视频目录", Toast.LENGTH_LONG).show();
                    // 引导用户到设置
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                }
            });

    // UI 元素
    private EditText etFtpServer, etFtpPort, etFtpUser, etFtpPassword, etLocalDir, etRemoteDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        Switch switchDelete = findViewById(R.id.switchDelete);
        etFtpServer = findViewById(R.id.etFtpServer);
        etFtpPort = findViewById(R.id.etFtpPort);
        etFtpUser = findViewById(R.id.etFtpUser);
        etFtpPassword = findViewById(R.id.etFtpPassword);
        etLocalDir = findViewById(R.id.etLocalDir);
        etRemoteDir = findViewById(R.id.etRemoteDir);
        tvLog = findViewById(R.id.tvLog);
        scrollView = findViewById(R.id.scrollView);

        SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);

        // 读取保存状态
        boolean autoDelete = prefs.getBoolean("autoDelete", false);
        switchDelete.setChecked(autoDelete);
        etFtpServer.setText(prefs.getString("ftpServer", "10.100.22.6"));
        etFtpPort.setText(String.valueOf(prefs.getInt("ftpPort", 21)));
        etFtpUser.setText(prefs.getString("ftpUser", "zhang"));
        etFtpPassword.setText(prefs.getString("ftpPassword", "zhangjinduo960714"));
        etLocalDir.setText(prefs.getString("localDir", "/sdcard/DCIM/Camera"));
        etRemoteDir.setText(prefs.getString("remoteDir",  "/upload/videos"));

        Button btnTestFtp = findViewById(R.id.btnTestFtp);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnScanNow = findViewById(R.id.btnScanNow);

        switchDelete.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("autoDelete", isChecked);
            editor.apply();
            //appendLog("自动删除功能: " + (isChecked ? "开启" : "关闭"));
        });

        btnTestFtp.setOnClickListener(v -> testFtpConnection());

        btnStart.setOnClickListener(v -> {
            ensurePermissions();
            schedulePeriodicWork();
        });

        btnScanNow.setOnClickListener(v -> {
            ensurePermissions();
            enqueueOneTime();
        });
    }


    // 在 TextView 中显示日志，并自动滚动到底部
    private void appendLog(String msg) {
        runOnUiThread(() -> {
            tvLog.append(msg + "\n");
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }
    private void ensurePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
        }
    }

    private void schedulePeriodicWork() {
        // 获取用户输入的路径和 FTP 配置
        String ftpServer = etFtpServer.getText().toString();
        int ftpPort;
        String ftpUser = etFtpUser.getText().toString();
        String ftpPassword = etFtpPassword.getText().toString();
        String localDir = etLocalDir.getText().toString();
        String remoteDir = etRemoteDir.getText().toString();

        // 如果输入为空，使用默认值
        if (ftpServer.isEmpty()) ftpServer = FTP_HOST;
        if (etFtpPort.getText().toString().isEmpty()) {
            ftpPort = 21; // 默认端口
        } else {
            try {
                ftpPort = Integer.parseInt(etFtpPort.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "端口号无效", Toast.LENGTH_SHORT).show();
                appendLog("端口号无效");
                return;
            }
        }
        if (ftpUser.isEmpty()) ftpUser = FTP_USER;
        if (ftpPassword.isEmpty()) ftpPassword = FTP_PASS;
        if (localDir.isEmpty()) localDir = LOCAL_DIR;
        if (remoteDir.isEmpty()) remoteDir = REMOTE_DIR;

        // 更新 UploadConfig 配置
        UploadConfig.FTP_HOST = ftpServer;
        UploadConfig.FTP_PORT = ftpPort;
        UploadConfig.FTP_USER = ftpUser;
        UploadConfig.FTP_PASS = ftpPassword;
        UploadConfig.LOCAL_DIR = localDir;
        UploadConfig.REMOTE_DIR = remoteDir;

        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                UploadWorker.class,
                PERIODIC_MINUTES, TimeUnit.MINUTES)
                .setConstraints(UploadWorker.defaultConstraints())
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
        );

        Toast.makeText(this, "已启动周期扫描（每 " + PERIODIC_MINUTES + " 分钟）", Toast.LENGTH_SHORT).show();
        appendLog("已启动周期扫描（每 " + PERIODIC_MINUTES + " 分钟）");
    }

    private void enqueueOneTime() {
        // 同上
        OneTimeWorkRequest once = new OneTimeWorkRequest.Builder(UploadWorker.class)
                .setConstraints(UploadWorker.defaultConstraints())
                .build();
        WorkManager.getInstance(this).enqueueUniqueWork(
                "video_auto_upload_once",
                ExistingWorkPolicy.REPLACE,
                once
        );
        Toast.makeText(this, "已触发一次立即扫描", Toast.LENGTH_SHORT).show();
        appendLog("已触发一次立即扫描");

    }

    // 测试 FTP 连接
    private void testFtpConnection() {
        String ftpServer = etFtpServer.getText().toString();
        int ftpPort = Integer.parseInt(etFtpPort.getText().toString());
        String ftpUser = etFtpUser.getText().toString();
        String ftpPassword = etFtpPassword.getText().toString();
        String localDir = etLocalDir.getText().toString();
        String remoteDir = etRemoteDir.getText().toString();

        SharedPreferences prefs = getSharedPreferences("ftp_config", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("ftpServer", ftpServer);
        editor.putInt("ftpPort", ftpPort);
        editor.putString("ftpUser", ftpUser);
        editor.putString("ftpPassword", ftpPassword);
        editor.putString("localDir", localDir);
        editor.putString("remoteDir", remoteDir);
        editor.apply();

        // 简单的 FTP 连接测试
        new Thread(() -> {
            try {
                FTPClient ftp = new FTPClient();
                ftp.setConnectTimeout(15000);
                ftp.connect(ftpServer,ftpPort);
                boolean login = ftp.login(ftpUser, ftpPassword);
                runOnUiThread(() -> {
                    if (login) {
                        Toast.makeText(this, "FTP 连接成功！", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "FTP 登录失败！", Toast.LENGTH_SHORT).show();
                    }
                    try {
                        ftp.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "FTP 连接失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
