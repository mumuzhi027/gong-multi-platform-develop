package com.sky31.gongmultiplatform.network.service

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdatePackageFilesTest {
    private val expected = "expected-version-apk"
    private fun isExpected(file: File) = file.readText() == expected

    @Test
    fun downloadedApkCanBeInstalledAgainWithoutReadingSource() = inDirectory { directory ->
        val source = File(directory, "download").apply { writeText(expected) }
        val target = File(directory, "ready/release.apk")
        UpdatePackageFiles.prepareApk(source, target, ::isExpected)
        assertEquals(expected, source.readText())
        source.delete()
        assertEquals(expected, UpdatePackageFiles.prepareApk(source, target, ::isExpected).readText())
    }

    @Test
    fun zipSelectsMatchingApkAndNeverWritesArchivePaths() = inDirectory { directory ->
        val source = archive(directory, listOf(
            "other.apk" to "wrong-version", "../../outside.apk" to expected
        ))
        val target = File(directory, "ready/release.apk")
        assertEquals(expected, UpdatePackageFiles.prepareApk(source, target, ::isExpected).readText())
        assertTrue(source.isFile)
        assertFalse(File(directory, "outside.apk").exists())
        assertFalse(File(directory.parentFile, "outside.apk").exists())
    }

    @Test
    fun interruptedExtractionIsReplacedFromRetainedDownload() = inDirectory { directory ->
        val source = archive(directory, listOf("release.apk" to expected))
        val target = File(directory, "ready/release.apk")
        target.parentFile!!.mkdirs()
        File(target.parentFile, "release.apk.part").writeText("incomplete")
        target.writeText("incomplete")
        assertEquals(expected, UpdatePackageFiles.prepareApk(source, target, ::isExpected).readText())
        assertFalse(File(target.parentFile, "release.apk.part").exists())
        assertTrue(source.exists())
    }

    @Test
    fun wrongVersionNeverBecomesReadyAndDownloadIsRetained() = inDirectory { directory ->
        val source = archive(directory, listOf("release.apk" to "wrong-version"))
        val target = File(directory, "ready/release.apk")
        assertFailsWith<IllegalStateException> { UpdatePackageFiles.prepareApk(source, target, ::isExpected) }
        assertFalse(target.exists())
        assertFalse(File(target.parentFile, "release.apk.part").exists())
        assertTrue(source.exists())
    }

    @Test
    fun invalidServerResponseNeverBecomesAnInstallationPackage() = inDirectory { directory ->
        val source = File(directory, "download").apply { writeText("<html>server error</html>") }
        val target = File(directory, "ready/release.apk")
        assertFailsWith<InvalidUpdatePackageException> { UpdatePackageFiles.prepareApk(source, target, ::isExpected) }
        assertFalse(target.exists())
        assertTrue(source.exists())
    }

    @Test
    fun extractionFailureLeavesDownloadAvailableForRetry() = inDirectory { directory ->
        val source = archive(directory, listOf("release.apk" to expected))
        val target = File(directory, "ready/release.apk")
        assertFailsWith<IllegalStateException> {
            UpdatePackageFiles.prepareApk(source, target) { file ->
                if (file.name.endsWith(".part")) error("interrupted")
                false
            }
        }
        assertFalse(target.exists())
        assertEquals(expected, UpdatePackageFiles.prepareApk(source, target, ::isExpected).readText())
    }

    private fun archive(directory: File, entries: List<Pair<String, String>>): File =
        File(directory, "update.zip").apply {
            ZipOutputStream(outputStream()).use { zip ->
                entries.forEach { (name, content) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content.toByteArray())
                    zip.closeEntry()
                }
            }
        }

    private fun inDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("update-package-test").toFile()
        try { block(directory) } finally { directory.deleteRecursively() }
    }
}
