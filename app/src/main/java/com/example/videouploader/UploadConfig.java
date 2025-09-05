package com.example.videouploader;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class UploadConfig {
    private UploadConfig() {}

    // ★★★★★ 把下面这些改成你的真实配置 ★★★★★
    public static  String FTP_HOST = "10.100.22.6";
    public static  int    FTP_PORT = 21;
    public static  String FTP_USER = "zhang";
    public static  String FTP_PASS = "zhangjinduo960714";
    public static  String REMOTE_DIR = "/upload/videos"; // 服务器保存目录

    // 本地要扫描的目录（示例：/sdcard/DCIM/Camera）
    public static  String LOCAL_DIR = "/sdcard/DCIM/Camera";

    // 扫描间隔（WorkManager 周期，单位分钟；最小 1）
    public static final long PERIODIC_MINUTES = 1;

    // 文件稳定判定（毫秒内大小不变化）
    public static final long STABLE_CHECK_INTERVAL_MS = 2000;

    // 要识别为视频的扩展名
    public static final Set<String> VIDEO_EXTS = new HashSet<>(Arrays.asList(
            "mp4","mov","avi","mkv","wmv","flv","m4v","3gp"
    ));

    // 通知渠道
    public static final String NOTIF_CHANNEL_ID = "upload_channel";
    public static final int    NOTIF_ID = 1001;
}
