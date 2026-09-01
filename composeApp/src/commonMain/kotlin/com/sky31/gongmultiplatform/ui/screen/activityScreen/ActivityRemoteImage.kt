package com.sky31.gongmultiplatform.ui.screen.activityScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.sky31.gongmultiplatform.SystemGlobalConfig
import com.sky31.gongmultiplatform.network.HttpClientProvider
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.decodeToImageBitmap

private const val maxActivityImageMemoryEntries = 6
private val activityImageMemoryCache = LinkedHashMap<String, ImageBitmap>()
private val activityImageRequests = mutableMapOf<String, CompletableDeferred<ImageBitmap?>>()
private val activityImageCacheMutex = Mutex()

@Composable
internal fun rememberActivityImageBitmap(url: String): ImageBitmap? {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        bitmap = null
        if (url.isBlank()) return@LaunchedEffect

        val resolvedUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "${SystemGlobalConfig.apiEndpoint.baseUrl}/${url.trimStart('/')}"
        }
        bitmap = loadActivityImage(resolvedUrl)
    }

    return bitmap
}

private suspend fun loadActivityImage(resolvedUrl: String): ImageBitmap? {
    var ownsRequest = false
    var cachedBitmap: ImageBitmap? = null
    val request = activityImageCacheMutex.withLock {
        cachedBitmap = activityImageMemoryCache[resolvedUrl]
        if (cachedBitmap == null) {
            activityImageRequests[resolvedUrl] ?: CompletableDeferred<ImageBitmap?>().also {
                activityImageRequests[resolvedUrl] = it
                ownsRequest = true
            }
        } else {
            null
        }
    }
    cachedBitmap?.let { return it }
    request ?: return null

    if (!ownsRequest) return request.await()

    val loaded = runCatching {
        HttpClientProvider.client
            .get(resolvedUrl)
            .body<ByteArray>()
            .decodeToImageBitmap()
    }.getOrNull()

    activityImageCacheMutex.withLock {
        if (loaded != null) {
            activityImageMemoryCache.remove(resolvedUrl)
            activityImageMemoryCache[resolvedUrl] = loaded
            while (activityImageMemoryCache.size > maxActivityImageMemoryEntries) {
                val oldestKey = activityImageMemoryCache.keys.firstOrNull() ?: break
                activityImageMemoryCache.remove(oldestKey)
            }
        }
        activityImageRequests.remove(resolvedUrl)
        request.complete(loaded)
    }
    return loaded
}
