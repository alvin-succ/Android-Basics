package com.example.servicetest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_MANAGE_OWN_CALLS = 1001;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1002;
    private MyService.DownloadBinder downloadBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            downloadBinder = (MyService.DownloadBinder) iBinder;
            downloadBinder.startDownLoad();
            downloadBinder.getProgress();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startBtn = findViewById(R.id.start_service);
        Button stopBtn = findViewById(R.id.stop_service);
        Button bindBtn = findViewById(R.id.bind_service);
        Button unbindBtn = findViewById(R.id.unbind_service);
        Button startIntentBtn = findViewById(R.id.start_intent_service);
        Button startIntentBtnExecutor = findViewById(R.id.start_intent_service_executor);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startServiceWithPermissions();
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
            }
        });

        bindBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bindIntent = new Intent(MainActivity.this, MyService.class);
                bindService(bindIntent, connection, BIND_AUTO_CREATE);
            }
        });

        unbindBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unbindService(connection);
            }
        });


        //deprecated usage
//        startIntentBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.d("MainActivity", "Thread is " + Thread.currentThread().getId());
//                Intent intentService = new Intent(MainActivity.this, MyIntentService.class);
//                startService(intentService);
//            }
//        });

        startIntentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MainActivity", "Thread is " + Thread.currentThread().getId());
                //构建输入数据
                Data inputData = new Data.Builder()
                        .putString("data", "需要处理的数据")
                        .build();
                //创建一次性工作请求
                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWorker.class)
                        .setInputData(inputData)
                        .build();
                //提交工作请求
                WorkManager.getInstance(MainActivity.this)
                        .enqueue(workRequest);

                //可选，监听任务状态
                WorkManager.getInstance(MainActivity.this)
                        .getWorkInfoByIdLiveData(workRequest.getId())
                        .observe(MainActivity.this, workInfo -> {
                            if(workInfo != null && workInfo.getState().isFinished()){
                                Log.d("MainActivity", "Work 完成状态: " + workInfo.getState());
                            }
                        });
            }
        });

        startIntentBtnExecutor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MainActivity", "Thread is " + Thread.currentThread().getId());
                Intent intentService = new Intent(MainActivity.this, MyBackgroundService.class);
                intentService.putExtra("data", "需要处理的数据");
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    // Android 8.0+ 需要使用 startForegroundService 或 startForeground
                    startForegroundService(intentService);
                }else{
                    startService(intentService);
                }
            }
        });
    }

    private void startServiceWithPermissions() {
        // 检查并请求所需权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要通知权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 需要 MANAGE_OWN_CALLS 权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.MANAGE_OWN_CALLS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.MANAGE_OWN_CALLS},
                        REQUEST_CODE_MANAGE_OWN_CALLS);
                return;
            }
        }

        // 权限已授予，启动服务
        startMyService();
    }

    private void startMyService() {
        Intent intent = new Intent(this, MyService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ 使用 startForegroundService
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show();
    }

    private void stopService() {
        Intent intent = new Intent(this, MyService.class);
        stopService(intent);
        Toast.makeText(this, "服务已停止", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS ||
                    requestCode == REQUEST_CODE_MANAGE_OWN_CALLS) {
                startMyService();
            }
        } else {
            Toast.makeText(this, "需要权限才能启动服务", Toast.LENGTH_SHORT).show();
        }
    }
}