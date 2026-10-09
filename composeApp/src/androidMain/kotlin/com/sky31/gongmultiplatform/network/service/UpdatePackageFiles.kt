package com.sky31.gongmultiplatform.network.service

import java.io.File
import java.util.zip.ZipInputStream
import java.util.zip.ZipException

internal class InvalidUpdatePackageException(message: String) : IllegalStateException(message)

/** Retains the downloaded source; an interrupted extraction never looks like a ready APK. */
internal object UpdatePackageFiles {
    fun prepareApk(source: File, destination: File, isExpectedApk: (File) -> Boolean): File {
        if (destination.isFile && isExpectedApk(destination)) return destination
        check(source.isFile && source.length() > 0) { "更新包不存在，请重新下载" }
        check(destination.parentFile!!.mkdirs() || destination.parentFile!!.isDirectory) {
            "无法创建安装目录"
        }
        val temporary = File(destination.parentFile, "release.apk.part")
        try {
            if (isExpectedApk(source)) {
                source.copyTo(temporary, overwrite = true)
            } else {
                var found = false
                ZipInputStream(source.inputStream().buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                            // Never use archive entry paths as filesystem destinations.
                            temporary.outputStream().use { zip.copyTo(it) }
                            if (isExpectedApk(temporary)) {
                                found = true
                                break
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                if (!found) throw InvalidUpdatePackageException("更新包中没有对应版本的拱拱安装包，请重新下载")
            }
            if (!isExpectedApk(temporary)) throw InvalidUpdatePackageException("更新安装包校验失败，请重新下载")
            if (destination.exists()) check(destination.delete()) { "无法替换安装包" }
            check(temporary.renameTo(destination)) { "无法保存安装包" }
            return destination
        } catch (invalid: ZipException) {
            throw InvalidUpdatePackageException("更新压缩包损坏，请重新下载")
        } finally {
            temporary.delete()
        }
    }
}
