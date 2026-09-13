package com.fk.core.app

import java.util.UUID
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

/**
 * Supplies parameters merged into every analytics event.
 *
 * Conceptually aligned with iOS `FKAnalyticsCommonParametersProviding`.
 */
fun interface AnalyticsCommonParametersProvider {
  fun commonParameters(): Map<String, String>
}

/**
 * Uploads a batch of analytics events.
 *
 * Conceptually aligned with iOS `FKAnalyticsUploading`.
 */
fun interface AnalyticsUploader {
  suspend fun upload(batch: List<AnalyticsEvent>)
}

/**
 * Buffered analytics sink (page / click / custom).
 *
 * Conceptually aligned with iOS `FKBusinessTracking`.
 */
interface AnalyticsTracking {
  fun setCommonParametersProvider(provider: AnalyticsCommonParametersProvider?)
  fun setUploader(uploader: AnalyticsUploader?)
  fun trackPageView(page: String, parameters: Map<String, String> = emptyMap())
  fun trackClick(
    element: String,
    page: String? = null,
    parameters: Map<String, String> = emptyMap(),
  )
  fun trackEvent(name: String, parameters: Map<String, String> = emptyMap())
  /** Pending in-memory events (for tests / demos). */
  fun pendingEvents(): List<AnalyticsEvent>
  /** Drops pending events without uploading. */
  fun clearPending()
  suspend fun flush()
}

/**
 * In-memory buffered [AnalyticsTracking].
 *
 * Host apps typically set an [AnalyticsUploader] that posts to their backend.
 */
class BufferedAnalyticsTracker(
  private val configuration: AppConfiguration = AppConfiguration(),
) : AnalyticsTracking {
  private val queue = ConcurrentLinkedDeque<AnalyticsEvent>()
  private val commonProvider = AtomicReference<AnalyticsCommonParametersProvider?>(null)
  private val uploader = AtomicReference<AnalyticsUploader?>(null)

  override fun setCommonParametersProvider(provider: AnalyticsCommonParametersProvider?) {
    commonProvider.set(provider)
  }

  override fun setUploader(uploader: AnalyticsUploader?) {
    this.uploader.set(uploader)
  }

  override fun trackPageView(page: String, parameters: Map<String, String>) {
    enqueue(AnalyticsEventType.PageView, page, parameters)
  }

  override fun trackClick(
    element: String,
    page: String?,
    parameters: Map<String, String>,
  ) {
    val merged = parameters.toMutableMap()
    if (page != null) merged["page"] = page
    enqueue(AnalyticsEventType.Click, element, merged)
  }

  override fun trackEvent(name: String, parameters: Map<String, String>) {
    enqueue(AnalyticsEventType.Custom, name, parameters)
  }

  override fun pendingEvents(): List<AnalyticsEvent> = queue.toList()

  override fun clearPending() {
    queue.clear()
  }

  override suspend fun flush() {
    val sink = uploader.get() ?: return
    while (true) {
      val batch = ArrayList<AnalyticsEvent>(configuration.analyticsBatchSize)
      while (batch.size < configuration.analyticsBatchSize) {
        val next = queue.pollFirst() ?: break
        batch += next
      }
      if (batch.isEmpty()) return
      try {
        sink.upload(batch)
      } catch (_: Throwable) {
        for (i in batch.indices.reversed()) {
          queue.addFirst(batch[i])
        }
        return
      }
    }
  }

  private fun enqueue(
    type: AnalyticsEventType,
    name: String,
    parameters: Map<String, String>,
  ) {
    val common = commonProvider.get()?.commonParameters().orEmpty()
    val merged = LinkedHashMap<String, String>(common.size + parameters.size)
    merged.putAll(common)
    merged.putAll(parameters)
    queue.addLast(
      AnalyticsEvent(
        id = UUID.randomUUID().toString(),
        type = type,
        name = name,
        parameters = merged,
        timestampEpochMs = System.currentTimeMillis(),
      ),
    )
    while (queue.size > configuration.analyticsMaxBuffer) {
      queue.pollFirst()
    }
  }
}
