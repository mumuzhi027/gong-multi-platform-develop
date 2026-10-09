package com.sky31.gongmultiplatform.network.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.DownloadManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class UpdateInstallWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        return try {
            InstallService.installBackgroundUpdates()
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val preferences = context.getSharedPreferences("background_app_updates", Context.MODE_PRIVATE)
            if (preferences.all.none { it.key.endsWith(".auto") && it.value == true }) return
            WorkManager.getInstance(context).enqueueUniqueWork(
                "install_downloaded_update", ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<UpdateInstallWorker>().setInitialDelay(3, TimeUnit.SECONDS).build()
            )
        }
    }
}

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        val preferences = context.getSharedPreferences("background_app_updates", Context.MODE_PRIVATE)
        if (id != -1L && preferences.all.any { it.key.endsWith(".download") && it.value == id }) {
            UpdateInstallWorker.enqueue(context)
        }
    }
}
