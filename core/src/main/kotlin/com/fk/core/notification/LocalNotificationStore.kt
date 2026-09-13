package com.fk.core.notification

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Persists pending alarm-backed requests so they can be restored after process death / reboot.
 */
internal class LocalNotificationStore(
  context: Context,
) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  fun upsert(request: LocalNotificationRequest, nextFireEpochMs: Long) {
    val entries = loadMutable()
    entries[request.identifier] = StoredPending(
      request = request,
      nextFireEpochMs = nextFireEpochMs,
    )
    save(entries.values.toList())
  }

  fun remove(identifier: String) {
    val entries = loadMutable()
    if (entries.remove(identifier) != null) {
      save(entries.values.toList())
    }
  }

  fun clear() {
    prefs.edit().remove(KEY_PENDING).apply()
  }

  fun get(identifier: String): StoredPending? = loadMutable()[identifier]

  fun all(): List<StoredPending> = loadMutable().values.toList()

  private fun loadMutable(): LinkedHashMap<String, StoredPending> {
    val raw = prefs.getString(KEY_PENDING, null) ?: return linkedMapOf()
    return try {
      val list = json.decodeFromString(ListSerializer(StoredPending.serializer()), raw)
      linkedMapOf<String, StoredPending>().apply {
        list.forEach { put(it.request.identifier, it) }
      }
    } catch (_: Throwable) {
      linkedMapOf()
    }
  }

  private fun save(entries: List<StoredPending>) {
    val raw = json.encodeToString(ListSerializer(StoredPending.serializer()), entries)
    prefs.edit().putString(KEY_PENDING, raw).apply()
  }

  companion object {
    private const val PREFS_NAME: String = "fk_local_notification"
    private const val KEY_PENDING: String = "pending"
  }
}

@kotlinx.serialization.Serializable
internal data class StoredPending(
  val request: LocalNotificationRequest,
  val nextFireEpochMs: Long,
)
