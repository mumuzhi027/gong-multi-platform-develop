package com.sky31.gongmultiplatform.network.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import com.sky31.gongmultiplatform.MainActivity

/** Persists installation confirmation even when the dialog/activity has gone away. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(InstallService.EXTRA_UPDATE_KEY) ?: return
        val preferences = context.getSharedPreferences("background_app_updates", Context.MODE_PRIVATE)
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        if (sessionId != preferences.getInt("$key.session", -1)) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirmation = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
            confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            preferences.edit().putString("$key.confirmation", confirmation.toUri(Intent.URI_INTENT_SCHEME)).commit()
            val activity = MainActivity.getInstance()
            if (activity != null && activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                runCatching { activity.startActivity(confirmation) }
            } else {
                notifyConfirmation(context, key, confirmation)
            }
        } else {
            val editor = preferences.edit().remove("$key.session").remove("$key.confirmation")
            if (status == PackageInstaller.STATUS_SUCCESS) editor.remove("$key.error")
            else editor.putString("$key.error", "安装未完成，更新包已保留，可再次安装")
            editor.commit()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(key.hashCode())
        }
    }

    private fun notifyConfirmation(context: Context, key: String, confirmation: Intent) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL, "应用更新", NotificationManager.IMPORTANCE_DEFAULT
            ))
        }
        val content = PendingIntent.getActivity(context, key.hashCode(), confirmation,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(key.hashCode(), NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("拱拱更新已准备好")
            .setContentText("点击确认安装，也可以稍后回到拱拱安装")
            .setContentIntent(content)
            .setAutoCancel(true)
            .build())
    }

    private companion object { const val CHANNEL = "app_updates" }
}
