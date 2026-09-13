package com.fk.sample.core.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fk.core.image.ImageLoadError
import com.fk.core.image.ImageLoadRequest
import com.fk.core.image.Images
import com.fk.sample.ui.SampleTopBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Smoke demo for Phase C2 image loading (`ImageLoading` via Coil).
 */
@Composable
fun ImageDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val images = remember { Images.create(context.applicationContext) }

  var status by remember { mutableStateOf("Idle") }
  var detail by remember { mutableStateOf("—") }
  var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
  var loadJob by remember { mutableStateOf<Job?>(null) }
  var lastRequest by remember { mutableStateOf<ImageLoadRequest?>(null) }

  Scaffold(
    topBar = { SampleTopBar(title = "Image", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Images package v${Images.VERSION}")
      Text("ImageLoading contract · Coil default · cancel / prefetch")
      HorizontalDivider()
      Text("Status: $status")
      Text("Detail:\n$detail")

      bitmap?.let { bmp ->
        Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = "Loaded sample",
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
          contentScale = ContentScale.Fit,
        )
      }

      Button(
        onClick = {
          loadJob?.cancel()
          loadJob = scope.launch {
            try {
              status = "Loading…"
              bitmap = null
              val request = ImageLoadRequest(
                url = SAMPLE_URL,
                targetWidth = 400,
                targetHeight = 300,
              )
              lastRequest = request
              val result = images.loadImageResult(request)
              bitmap = result.bitmap
              status = "Loaded"
              detail = buildString {
                append("size=").append(result.bitmap.width).append('x').append(result.bitmap.height)
                append('\n')
                append("wasCached=").append(result.wasCached)
                append('\n')
                append("dataSource=").append(result.dataSource)
                append('\n')
                append("cacheKey=").append(request.resolvedCacheKey())
              }
            } catch (e: ImageLoadError) {
              status = "Error"
              detail = e.message ?: e.toString()
            } catch (e: Exception) {
              status = "Error"
              detail = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Load remote image")
      }

      Button(
        onClick = {
          val request = lastRequest
          if (request == null) {
            status = "No request"
            return@Button
          }
          images.cancelLoad(request)
          loadJob?.cancel()
          status = "Cancel requested"
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = lastRequest != null,
      ) {
        Text("Cancel load")
      }

      Button(
        onClick = {
          scope.launch {
            try {
              status = "Prefetching…"
              images.prefetch(
                urls = listOf(SAMPLE_URL, SAMPLE_URL_ALT),
                targetWidth = 200,
                targetHeight = 200,
              )
              status = "Prefetch done"
              detail = "Prefetched 2 URLs into Coil caches"
            } catch (e: Exception) {
              status = "Error"
              detail = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Prefetch sample URLs")
      }

      Button(
        onClick = {
          images.removeAllImages()
          bitmap = null
          status = "Caches cleared"
          detail = "Memory + disk caches cleared"
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Clear caches")
      }
    }
  }
}

private const val SAMPLE_URL: String = "https://picsum.photos/seed/fk-android/400/300"
private const val SAMPLE_URL_ALT: String = "https://picsum.photos/seed/fk-android-2/200/200"
