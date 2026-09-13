package com.fk.core.background

/**
 * Process-wide handler registry shared by [BackgroundTaskManager] and [BackgroundCoroutineWorker].
 *
 * WorkManager constructs workers independently of the manager instance, so handlers must live
 * in a singleton keyed by identifier.
 */
internal object BackgroundTaskRegistry {
  data class Entry(
    val kind: BackgroundTaskKind,
    val handler: BackgroundTaskHandler,
  )

  private val lock = Any()
  private val handlers = linkedMapOf<String, Entry>()
  @Volatile
  private var installed: Boolean = false

  fun register(
    identifier: String,
    kind: BackgroundTaskKind,
    handler: BackgroundTaskHandler,
  ) {
    synchronized(lock) {
      val existing = handlers[identifier]
      if (existing != null && existing.kind != kind) {
        throw BackgroundTaskError.DuplicateRegistration(identifier)
      }
      handlers[identifier] = Entry(kind, handler)
    }
  }

  fun get(identifier: String): Entry? =
    synchronized(lock) { handlers[identifier] }

  fun require(
    identifier: String,
    expectedKind: BackgroundTaskKind,
  ): Entry {
    val entry = get(identifier)
      ?: throw BackgroundTaskError.UnregisteredIdentifier(identifier)
    if (entry.kind != expectedKind) {
      throw BackgroundTaskError.UnregisteredIdentifier(identifier)
    }
    return entry
  }

  fun install(
    registrations: List<BackgroundTaskRegistration>,
    allowsMultipleInstall: Boolean,
  ) {
    synchronized(lock) {
      if (installed && !allowsMultipleInstall) {
        throw BackgroundTaskError.AlreadyInstalled()
      }
      for (registration in registrations) {
        val entry = handlers[registration.identifier]
          ?: throw BackgroundTaskError.UnregisteredIdentifier(registration.identifier)
        if (entry.kind != registration.kind) {
          throw BackgroundTaskError.UnregisteredIdentifier(registration.identifier)
        }
      }
      installed = true
    }
  }

  fun ensureInstalled() {
    if (!installed) throw BackgroundTaskError.NotInstalled()
  }

  /** Test / mock reset. Not for production hosts. */
  fun resetForTests() {
    synchronized(lock) {
      handlers.clear()
      installed = false
    }
  }

  fun snapshotKinds(): Map<String, BackgroundTaskKind> =
    synchronized(lock) { handlers.mapValues { it.value.kind } }
}
