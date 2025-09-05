# Android FTP 视频上传器

一个用于 Android 7.1+ 设备的应用，可自动监控指定文件夹中的视频文件并上传到 FTP 服务器，上传成功后自动删除本地文件。

## 功能特点

-   📁 监控指定文件夹中的视频文件（支持 MP4、AVI、MOV、MKV 格式）
    
-   ☁️ 自动上传视频文件到配置的 FTP 服务器
    
-   🗑️ 上传成功后自动删除本地文件以节省存储空间
    
-   ⚙️ 可配置 FTP 服务器连接参数
    
-   🔄 后台服务持续监控（每分钟检查一次）
    
-   💾 自动保存和恢复用户设置
    

## 技术栈

-   Java
    
-   Android SDK
    
-   Apache Commons Net (FTP 客户端)
    
-   SharedPreferences (配置存储)
    

## 要求

-   Android 7.1 Nougat (API 25) 或更高版本
    
-   网络连接权限
    
-   存储读写权限
    
-   FTP 服务器账户
    

## 安装与部署

### 1\. 环境设置

1.  安装 JDK 8 (Android 7.1 兼容版本)
    
2.  下载并安装 Android Studio
    
3.  配置 Android SDK (API 25)
    

### 2\. 导入项目

1.  克隆或下载本项目
    
2.  在 Android Studio 中选择 "Open an existing project"
    
3.  选择项目根目录
    
4.  等待 Gradle 同步完成
    

### 3\. 添加依赖

项目使用 Apache Commons Net 库处理 FTP 连接，已在 `build.gradle` 中配置：



```
dependencies {
    implementation 'commons-net:commons-net:3.6'
}
```

### 4\. 配置权限

确保 AndroidManifest.xml 中包含以下权限：



```
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### 5\. 构建和运行

1.  连接 Android 设备或启动模拟器
    
2.  点击 Android Studio 中的 "Run" 按钮
    
3.  选择目标设备
    
4.  等待应用安装和启动
    

## 使用方法

1.  打开应用
    
2.  配置以下参数：
    
    -   **文件夹路径**：要监控的视频文件所在路径（如 `/sdcard/Movies/`）
        
    -   **FTP 服务器**：FTP 服务器地址
        
    -   **FTP 端口**：FTP 服务器端口（默认 21）
        
    -   **FTP 用户名**：FTP 登录用户名
        
    -   **FTP 密码**：FTP 登录密码
        
3.  点击"启动服务"开始监控和上传
    
4.  将视频文件放入指定文件夹，应用会自动上传并删除本地文件
    
5.  点击"停止服务"可停止监控
    

## 项目结构


```
app/
├── src/
│   └── main/
│       ├── java/com/example/ftpuploader/
│       │   ├── MainActivity.java      # 主界面和用户交互
│       │   └── UploadService.java     # 后台服务和FTP上传逻辑
│       ├── res/
│       │   └── layout/
│       │       └── activity_main.xml  # 用户界面布局
│       └── AndroidManifest.xml        # 应用清单和权限声明
```

## 注意事项

1.  确保 Android 设备已授予应用存储读写权限
    
2.  FTP 服务器必须支持被动模式
    
3.  服务默认每分钟检查一次新文件
    
4.  为确保服务在后台持续运行，建议将应用添加到系统的电池优化白名单中
    
5.  上传大文件时请保持网络连接稳定
    

## 自定义和扩展

-   修改 `UploadService.java` 中的检查间隔（当前为 60 秒）
    
-   添加更多视频格式支持（修改文件过滤逻辑）
    
-   实现上传进度通知
    
-   添加上传失败重试机制
    
-   支持多个监控文件夹
    

## 故障排除

1.  **应用无法安装**：确保设备运行 Android 7.1 或更高版本
    
2.  **权限被拒绝**：在应用设置中手动授予存储权限
    
3.  **FTP 连接失败**：检查服务器地址、端口、用户名和密码是否正确
    
4.  **文件未删除**：检查文件是否被其他应用占用
    

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](https://LICENSE) 文件了解详情

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个项目

## 支持

如果您遇到任何问题或有建议，请创建 Issue 或通过电子邮件联系维护者