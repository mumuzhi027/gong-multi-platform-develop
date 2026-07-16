package com.sky31.gongmultiplatform.network.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.sky31.gongmultiplatform.network.HttpClientProvider
import com.sky31.gongmultiplatform.network.api.ResourceApiImpl
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

actual object InstallService: KoinComponent {
    private val context: Context by inject()
    private val clientProvider: HttpClientProvider by inject()

    private val resourceApi = ResourceApiImpl(clientProvider.client)
    private val releaseDir by lazy { File(context.cacheDir, "release") }
    private val releaseZip by lazy { File(context.cacheDir, "release.zip") }

    actual suspend fun downloadApk(url: String): Flow<Int> {
        try {
            return resourceApi.getApkZip(url) {response ->
                clearReleaseArtifacts()

                if (shouldUnzipPackage(url, response.headers["Content-Type"])) {
                    releaseZip.outputStream().use { fileOutputStream ->
                        response.bodyAsChannel().copyTo(fileOutputStream)
                    }

                    unzip()
                } else {
                    releaseDir.mkdirs()
                    val outputFile = File(releaseDir, "release.apk")
                    outputFile.outputStream().use { fileOutputStream ->
                        response.bodyAsChannel().copyTo(fileOutputStream)
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private fun clearReleaseArtifacts() {
        if (releaseZip.exists()) {
            releaseZip.delete()
        }

        if (releaseDir.exists()) {
            releaseDir.deleteRecursively()
        }
    }

    private fun shouldUnzipPackage(url: String, contentType: String?): Boolean {
        val normalizedUrl = url.substringBefore('?').lowercase()
        val normalizedContentType = contentType?.lowercase().orEmpty()

        return normalizedUrl.endsWith(".zip") || normalizedContentType.contains("zip")
    }

    private fun unzip() {
        val zipFile = releaseZip
        val outputDir = releaseDir
        outputDir.mkdirs()

        ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
            var entry = zipIn.nextEntry
            while(entry != null) {
                val filePath = File(outputDir, entry.name)
                if(!entry.isDirectory) {
                    filePath.parentFile?.mkdirs()
                    FileOutputStream(filePath).use { out ->
                        zipIn.copyTo(out)
                    }
                } else {
                    filePath.mkdirs()
                }

                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    actual suspend fun installApk() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()) {
            throw Error("No install permission!")
        }

        val apkFile = releaseDir
            .takeIf { it.exists() }
            ?.walkTopDown()
            ?.firstOrNull { file ->
                file.isFile && file.extension.equals("apk", ignoreCase = true)
            }

        if(apkFile == null) {
            throw Error("apk file not found")
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(intent)
    }
}
