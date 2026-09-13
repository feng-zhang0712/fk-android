package com.fk.core.file

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Persists transfer snapshots for pause/resume across process death. */
internal class FileTransferStore(
  context: Context,
  private val persistenceKey: String,
) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  fun saveAll(entries: List<PersistedTransfer>) {
    val raw = json.encodeToString(ListSerializer(PersistedTransfer.serializer()), entries)
    prefs.edit().putString(persistenceKey, raw).apply()
  }

  fun loadAll(): List<PersistedTransfer> {
    val raw = prefs.getString(persistenceKey, null) ?: return emptyList()
    return try {
      json.decodeFromString(ListSerializer(PersistedTransfer.serializer()), raw)
    } catch (_: Throwable) {
      emptyList()
    }
  }

  fun upsert(entry: PersistedTransfer) {
    val map = loadAll().associateBy { it.id }.toMutableMap()
    map[entry.id] = entry
    saveAll(map.values.toList())
  }

  fun remove(id: Int) {
    val next = loadAll().filterNot { it.id == id }
    saveAll(next)
  }

  fun clear() {
    prefs.edit().remove(persistenceKey).apply()
  }

  companion object {
    private const val PREFS_NAME: String = "fk_file_transfers"
  }
}
