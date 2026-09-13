package com.fk.core.app

/**
 * Fetches remote version metadata (Play / backend — host-supplied).
 *
 * Conceptually aligned with iOS `FKRemoteVersionProviding`.
 */
fun interface RemoteVersionProvider {
  suspend fun fetchRemoteVersion(): RemoteVersionInfo
}

/**
 * Compares local app metadata with a remote version payload.
 *
 * Conceptually aligned with iOS `FKBusinessVersioning` (no update UI).
 */
interface VersionChecking {
  fun appMetadata(): AppMetadata

  suspend fun checkForUpdate(provider: RemoteVersionProvider): VersionCheckResult
}

/** Default [VersionChecking] using numeric segment comparison on version names. */
class DefaultVersionChecker(
  private val info: AppInfo,
) : VersionChecking {
  override fun appMetadata(): AppMetadata = info.metadata()

  override suspend fun checkForUpdate(provider: RemoteVersionProvider): VersionCheckResult {
    val local = appMetadata()
    val remote = try {
      provider.fetchRemoteVersion()
    } catch (t: Throwable) {
      throw AppError.VersionCheckFailed(t.message ?: t::class.java.simpleName, t)
    }
    return VersionCheckResult(
      local = local,
      remote = remote,
      decision = decide(local, remote),
    )
  }

  companion object {
    fun decide(local: AppMetadata, remote: RemoteVersionInfo): UpdateDecision {
      if (remote.isForceUpdate) return UpdateDecision.ForceUpdate
      val remoteCode = remote.versionCode
      if (remoteCode != null && remoteCode > local.versionCode) {
        return UpdateDecision.OptionalUpdate
      }
      return if (compareVersionNames(local.versionName, remote.versionName) < 0) {
        UpdateDecision.OptionalUpdate
      } else {
        UpdateDecision.UpToDate
      }
    }

    /** Returns negative when [left] < [right] (numeric segments). */
    fun compareVersionNames(left: String, right: String): Int {
      val a = left.split('.', '-', '_').mapNotNull { it.toIntOrNull() }
      val b = right.split('.', '-', '_').mapNotNull { it.toIntOrNull() }
      val n = maxOf(a.size, b.size)
      for (i in 0 until n) {
        val av = a.getOrElse(i) { 0 }
        val bv = b.getOrElse(i) { 0 }
        if (av != bv) return av.compareTo(bv)
      }
      return 0
    }
  }
}
