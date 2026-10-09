package com.sky31.gongmultiplatform.network.service

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.lifecycle.Lifecycle
import com.sky31.gongmultiplatform.MainActivity
import com.sky31.gongmultiplatform.SystemGlobalConfig
import com.sky31.gongmultiplatform.util.AppUpdateState
import com.sky31.gongmultiplatform.util.getAppUpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.security.MessageDigest

actual object InstallService : KoinComponent {
    private val injectedContext: Context by inject()
    private val context get() = injectedContext.applicationContext
    private val downloads get() = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val preferences get() = context.getSharedPreferences("background_app_updates", Context.MODE_PRIVATE)
    private val mutationLock = Mutex()

    actual fun observeDownload(url: String, version: String): Flow<UpdateDownloadState> = flow {
        val key = updateKey(url, version)
        while (currentCoroutineContext().isActive) {
            emit(mutationLock.withLock { readState(key) })
            delay(1000)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    actual suspend fun startDownload(url: String, version: String, autoInstall: Boolean) = withContext(Dispatchers.IO) {
        mutationLock.withLock {
            val key = updateKey(url, version)
            val settings = preferences.edit().putString("$key.url", url).putString("$key.version", version)
                .putBoolean("$key.auto", autoInstall)
            // A newly chosen release supersedes earlier automatic-install requests.
            preferences.all.keys.filter { it.endsWith(".auto") && it != "$key.auto" }
                .forEach { settings.putBoolean(it, false) }
            check(settings.commit()) { "无法保存更新设置" }
            // Reopening the dialog or tapping twice must never replace a live/completed task.
            if (readState(key).phase in setOf(
                    UpdateDownloadPhase.DOWNLOADING, UpdateDownloadPhase.PAUSED,
                    UpdateDownloadPhase.READY, UpdateDownloadPhase.INSTALLING
                )) return@withLock

            val oldId = preferences.getLong("$key.download", -1)
            if (oldId != -1L) downloads.remove(oldId)
            val destination = downloadFile(key)
            check(destination.parentFile!!.mkdirs() || destination.parentFile!!.isDirectory) {
                "无法创建更新下载目录"
            }
            if (destination.exists()) check(destination.delete()) { "无法清理失效的更新包" }
            val request = DownloadManager.Request(Uri.parse(resolvedUrl(url)))
                .setTitle("拱拱 $version 更新")
                .setDescription("后台下载中，可继续使用；下载完成后回到拱拱安装")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destination))
                .setAllowedOverRoaming(false)
            val id = downloads.enqueue(request)
            if (!preferences.edit().putLong("$key.download", id)
                    .remove("$key.error").remove("$key.invalid").commit()) {
                downloads.remove(id)
                error("无法保存下载任务，请重试")
            }
        }
    }

    actual suspend fun installApk(url: String, version: String) = installPackage(url, version, backgroundOnly = false)

    private suspend fun installPackage(url: String, version: String, backgroundOnly: Boolean) = withContext(Dispatchers.IO) {
        mutationLock.withLock {
            val key = updateKey(url, version)
            // Drops expired confirmation intents and sessions left uncommitted by process death.
            val currentState = readState(key)
            preferences.getString("$key.confirmation", null)?.let { saved ->
                val confirmation = Intent.parseUri(saved, Intent.URI_INTENT_SCHEME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                withContext(Dispatchers.Main) { context.startActivity(confirmation) }
                return@withLock
            }
            check(currentState.phase == UpdateDownloadPhase.READY) { "请等待更新包下载完成" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                check(context.packageManager.canRequestPackageInstalls()) { "请先授予安装权限" }
            }

            val apk = try {
                UpdatePackageFiles.prepareApk(downloadFile(key), apkFile(key)) { file ->
                    context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.let { info ->
                        info.packageName == context.packageName && info.versionName == version
                    } == true
                }
            } catch (invalid: InvalidUpdatePackageException) {
                preferences.edit().putBoolean("$key.invalid", true)
                    .putString("$key.error", invalid.message).commit()
                throw invalid
            }
            // PackageInstaller also verifies the signing certificate and rejects incompatible APKs.
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                setSize(apk.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }
            val sessionId = installer.createSession(params)
            try {
                installer.openSession(sessionId).use { session ->
                    apk.inputStream().use { input ->
                        session.openWrite("base.apk", 0, apk.length()).use { output ->
                            input.copyTo(output)
                            session.fsync(output)
                        }
                    }
                    val callback = Intent(context, UpdateInstallReceiver::class.java)
                        .setAction("${context.packageName}.UPDATE_INSTALL_RESULT.$key")
                        .putExtra(EXTRA_UPDATE_KEY, key)
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                    val result = PendingIntent.getBroadcast(context, sessionId, callback, flags)
                    check(preferences.edit().putInt("$key.session", sessionId)
                        .remove("$key.error").commit()) { "无法保存安装状态" }
                    withContext(Dispatchers.Main) {
                        // The user may have returned while the APK was being prepared/copied.
                        if (backgroundOnly && isAppVisible()) throw DeferredUpdateInstallException()
                        session.commit(result.intentSender)
                    }
                }
            } catch (failure: Exception) {
                runCatching { installer.abandonSession(sessionId) }
                preferences.edit().remove("$key.session").commit()
                throw failure
            }
        }
    }

    private fun readState(key: String): UpdateDownloadState {
        val sessionId = preferences.getInt("$key.session", -1)
        if (sessionId != -1) {
            val installer = context.packageManager.packageInstaller
            val session = installer.getSessionInfo(sessionId)
            if (session == null || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !session.isSealed)) {
                if (session != null) runCatching { installer.abandonSession(sessionId) }
                preferences.edit().remove("$key.session").remove("$key.confirmation").commit()
            } else if (preferences.getString("$key.confirmation", null) == null) {
                return UpdateDownloadState(UpdateDownloadPhase.INSTALLING, 100, "正在安装更新")
            }
        }
        if (preferences.getBoolean("$key.invalid", false)) {
            return UpdateDownloadState(UpdateDownloadPhase.FAILED, message =
                preferences.getString("$key.error", null) ?: "更新包无效，请重新下载")
        }
        // Prepared APKs are private, atomically written and reusable even if the download record is removed.
        if (apkFile(key).isFile && apkFile(key).length() > 0) {
            return UpdateDownloadState(UpdateDownloadPhase.READY, 100,
                preferences.getString("$key.error", null) ?: "更新包已下载，可直接安装")
        }
        val id = preferences.getLong("$key.download", -1)
        if (id == -1L) return UpdateDownloadState()
        downloads.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (!cursor.moveToFirst()) {
                return UpdateDownloadState(UpdateDownloadPhase.FAILED, message = "下载任务已移除，请重新下载")
            }
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val progress = if (total > 0) (bytes * 100 / total).toInt().coerceIn(0, 99) else 0
            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    if (downloadFile(key).isFile && downloadFile(key).length() > 0) {
                        UpdateDownloadState(UpdateDownloadPhase.READY, 100,
                            preferences.getString("$key.error", null) ?: "更新包已下载，可直接安装")
                    } else UpdateDownloadState(UpdateDownloadPhase.FAILED, message = "更新包已被移除，请重新下载")
                }
                DownloadManager.STATUS_PAUSED -> UpdateDownloadState(
                    UpdateDownloadPhase.PAUSED, progress, "下载暂时暂停，等待网络恢复后继续"
                )
                DownloadManager.STATUS_FAILED -> UpdateDownloadState(
                    UpdateDownloadPhase.FAILED, progress, "下载失败，请重试（${cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))}）"
                )
                else -> UpdateDownloadState(UpdateDownloadPhase.DOWNLOADING, progress,
                    if (total > 0) "下载中 $progress%" else "等待下载或获取文件大小，可继续使用")
            }
        }
    }

    private fun downloadFile(key: String): File = File(
        checkNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)) { "下载存储不可用" },
        "updates/$key/package.download"
    )

    private fun apkFile(key: String) = File(context.filesDir, "updates/$key/release.apk")

    private fun resolvedUrl(url: String): String {
        require(url.isNotBlank()) { "更新下载地址为空" }
        val resolved = java.net.URI(SystemGlobalConfig.updateEndpoint.baseUrl + "/").resolve(url.trim())
        require(resolved.scheme in setOf("http", "https") && !resolved.host.isNullOrBlank()) {
            "更新下载地址无效"
        }
        return resolved.toString()
    }

    private fun updateKey(url: String, version: String): String = MessageDigest.getInstance("SHA-256")
        .digest((version + "\n" + resolvedUrl(url)).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    /** Called by persistent work after download completion and after the user leaves the app. */
    internal suspend fun installBackgroundUpdates() {
        val requested = withContext(Dispatchers.IO) {
            preferences.all.filter { it.key.endsWith(".auto") && it.value == true }.keys.toList()
        }
        for (entry in requested) {
            val key = entry.removeSuffix(".auto")
            val url = preferences.getString("$key.url", null) ?: continue
            val version = preferences.getString("$key.version", null) ?: continue
            val foreground = withContext(Dispatchers.Main) { isAppVisible() }
            if (foreground) return
            val state = withContext(Dispatchers.IO) { mutationLock.withLock { readState(key) } }
            if (state.phase != UpdateDownloadPhase.READY ||
                preferences.getString("$key.confirmation", null) != null) continue
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()) continue
            val installedVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            if (installedVersion != null && getAppUpdateState(installedVersion, version, version) == AppUpdateState.UP_TO_DATE) {
                preferences.edit().putBoolean(entry, false).commit()
                continue
            }
            // Attempt once automatically. Cancellation/failure leaves the package for manual retries.
            preferences.edit().putBoolean(entry, false).commit()
            try {
                installPackage(url, version, backgroundOnly = true)
            } catch (deferred: DeferredUpdateInstallException) {
                preferences.edit().putBoolean(entry, true).commit()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                preferences.edit().putBoolean(entry, true).commit()
                throw cancelled
            } catch (failure: Exception) {
                preferences.edit().putString("$key.error", "自动安装未完成，更新包已保留，可手动安装").commit()
            }
        }
    }

    private fun isAppVisible() = MainActivity.getInstance()?.lifecycle?.currentState
        ?.isAtLeast(Lifecycle.State.STARTED) == true

    private class DeferredUpdateInstallException : IllegalStateException("等待离开应用后安装")

    internal const val EXTRA_UPDATE_KEY = "update_key"
}
