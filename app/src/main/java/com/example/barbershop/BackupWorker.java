package com.example.barbershop;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileOutputStream;

public class BackupWorker extends Worker {

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SharedPreferences prefs = getApplicationContext()
                    .getSharedPreferences("BarberShopData", Context.MODE_PRIVATE);

            String json = new org.json.JSONObject(prefs.getAll()).toString(2);

            File backupDir = new File(getApplicationContext().getFilesDir(), "automatic_backups");
            if (!backupDir.exists()) backupDir.mkdirs();

            File backupFile = new File(backupDir, "latest_backup.json");

            FileOutputStream out = new FileOutputStream(backupFile);
            out.write(json.getBytes("UTF-8"));
            out.close();

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
