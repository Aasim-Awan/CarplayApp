//package com.example.carplaytest.notifications
//
//import android.util.Log
//import androidx.work.Constraints
//import androidx.work.ExistingPeriodicWorkPolicy
//import androidx.work.NetworkType
//import androidx.work.PeriodicWorkRequest
//import androidx.work.WorkManager
//import java.util.concurrent.TimeUnit
//
//private fun scheduleNotificationWorker() {
//
//    val workRequest = PeriodicWorkRequest.Builder(
//        NotificationWorker::class.java,
//        1, TimeUnit.DAYS
//    )
//        .setConstraints(
//            Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()
//        )
//        .setInitialDelay(1 , TimeUnit.DAYS)
//        .addTag("app_open_notification")
//        .build()
//
//    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//        "app_open_notification",
//        ExistingPeriodicWorkPolicy.REPLACE,
//        workRequest
//    )
//
//    Log.d("Notification", "Notification worker scheduled for 20 minutes later.")
//}