package com.example.servicetest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class MyService extends Service {

    private static final String CHANNEL_ID = "my_service_channel";
    private static final int NOTIFICATION_ID = 1001;

    private DownloadBinder mBinder = new DownloadBinder();

    class DownloadBinder extends Binder {
        public void startDownLoad() {
            Log.d("MyService", "startdownload executed");
        }

        public int getProgress() {
            Log.d("MyService", "getProgress executed");
            return 0;
        }
    }

    public MyService() {
    }

    @Override
    public void onCreate() {
        Log.d("MyService", "onCreate===>");
        super.onCreate();

        // 1. 创建通知渠道（Android 8.0+ 必需）
        createNotificationChannel();

        // 2. 构建通知
        Notification notification = buildNotification();

        // 3. 启动前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 需要指定前台服务类型
            startForeground(NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        // Android 8.0 (API 26) 及以上需要通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "我的服务通道",
                    NotificationManager.IMPORTANCE_LOW  // 必须至少是 LOW，不能是 NONE
            );

            channel.setDescription("用于前台服务的通知通道");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);

        // 处理 Android 12+ 的 PendingIntent 标志
        int pendingIntentFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags);

        // 使用 NotificationCompat 确保兼容性
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("服务运行中")
                .setContentText("服务正在后台执行任务")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(getNotificationIcon())  // 使用有效图标
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), getNotificationIcon()))
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_LOW)  // Android 12+ 必需
                .setCategory(NotificationCompat.CATEGORY_SERVICE)  // 设置类别
                .setAutoCancel(false)
                .setOngoing(true);  // 设置为持续通知

        return builder.build();
    }

    private int getNotificationIcon() {
        // 尝试使用应用图标，如果不存在则使用系统图标
        int iconId = R.mipmap.ic_launcher;

        // 检查图标是否存在
        try {
            if (getResources().getDrawable(iconId, null) == null) {
                // 如果图标不存在，使用系统默认图标
                iconId = android.R.drawable.ic_dialog_info;
            }
        } catch (Exception e) {
            iconId = android.R.drawable.ic_dialog_info;
        }

        return iconId;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MyService", "onStartCommand===>");
        return START_STICKY;  // 建议返回 START_STICKY 而不是 super.onStartCommand
    }

    @Override
    public void onDestroy() {
        Log.d("MyService", "onDestroy===>");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}