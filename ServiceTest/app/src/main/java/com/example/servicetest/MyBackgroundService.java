package com.example.servicetest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyBackgroundService extends Service {
    private static final String CHANNEL_ID = "MyBackgroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private ExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
        createNotificationChannel();
        Log.d("MyBackgroundService", "Service created");
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MyBackgroundService", "onStartCommand called");

        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
        String data = intent != null ? intent.getStringExtra("data"):null;

        executorService.submit(() -> {
            Log.d("MyBackgroundService", "Thread is " + Thread.currentThread().getId());

            try{
                Thread.sleep(3000);
                Log.d("MyBackgroundService", "任务完成，处理数据: " + data);
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
            }finally {
                stopSelf(startId);
            }
        });
        return START_NOT_STICKY;
    }

    /**
     * 创建并返回用于前台服务的通知。
     * 此方法必须在调用 startForeground() 前执行。
     */
    private Notification createNotification() {
        // 1. 创建一个点击通知时跳转回本应用的 Intent
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // 2. 创建 PendingIntent (处理 Android 12+ 的变更)
        int flags;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);

        // 3. 使用 NotificationCompat.Builder 构建通知（确保兼容性）
        //    注意：CHANNEL_ID 必须在之前通过 createNotificationChannel() 创建好。
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("后台服务运行中") // 通知标题
                .setContentText("正在执行任务...") // 通知内容
                .setSmallIcon(R.drawable.ic_launcher_foreground) // ★ 关键：必须设置有效的图标
                .setContentIntent(pendingIntent) // 设置点击意图
                .setPriority(NotificationCompat.PRIORITY_LOW) // Android 8.0+ 建议使用低优先级
                .setCategory(NotificationCompat.CATEGORY_SERVICE) // 明确通知类别为服务
                .setAutoCancel(false) // 点击后不自动消失
                .setOngoing(true) // 设置为持续通知，用户无法手动滑动清除
                .setWhen(System.currentTimeMillis()) // 设置通知时间
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        Log.d("MyBackgroundService", "onDestroy executed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
