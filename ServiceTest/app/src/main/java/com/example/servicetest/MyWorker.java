package com.example.servicetest;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MyWorker extends Worker {
    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("MyWorker", "Thread is " + Thread.currentThread().getId());
        // 获取传入的数据
        String data = getInputData().getString("data");

        //执行后台任务
        try{
            Thread.sleep(3000);
            Log.d("MyWorker", "Task completed=" + data);
            //返回成功
            return Result.success();
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            return Result.failure();
        }
    }


}
