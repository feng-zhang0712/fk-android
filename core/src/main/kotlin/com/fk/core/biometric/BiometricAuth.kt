package com.fk.core.biometric

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Default [BiometricAuthenticating] backed by Jetpack [BiometricManager] / [BiometricPrompt].
 *
 * Conceptually aligned with iOS `FKBiometricAuth`. Prefer injecting this over a process singleton.
 */
class BiometricAuth(
  context: Context,
  private val configuration: BiometricAuthConfiguration = BiometricAuthConfiguration(),
) : BiometricAuthenticating {
  private val appContext: Context = context.applicationContext
  private val gate = Any()
  private val active = AtomicReference<ActiveSession?>(null)

  override fun capability(policy: BiometricPolicy): BiometricCapability =
    BiometricCapabilityProbe.probe(appContext, policy)

  override fun capability(): BiometricCapability =
    capability(configuration.defaultPolicy)

  override suspend fun authenticate(
    activity: FragmentActivity,
    reason: String,
    options: BiometricAuthOptions,
  ) {
    val policy = options.policy ?: configuration.defaultPolicy
    authenticate(activity, reason, policy, options)
  }

  override suspend fun authenticate(
    activity: FragmentActivity,
    reason: String,
    policy: BiometricPolicy,
    options: BiometricAuthOptions,
  ) {
    val trimmed = reason.trim()
    if (trimmed.isEmpty()) throw BiometricError.InvalidReason
    if (activity.isFinishing || activity.isDestroyed) throw BiometricError.NotInteractive

    val effectivePolicy = options.policy ?: policy
    val authenticators = BiometricCapabilityProbe.authenticators(
      policy = effectivePolicy,
      allowDeviceCredential = options.allowDeviceCredential,
    )

    val probe = BiometricCapabilityProbe.probe(
      context = appContext,
      policy = effectivePolicy,
      allowDeviceCredential = options.allowDeviceCredential,
    )
    if (!probe.canAuthenticate) {
      throw probe.probeError ?: BiometricError.BiometryNotAvailable
    }

    suspendCancellableCoroutine { continuation ->
      val session: ActiveSession
      val prompt: BiometricPrompt
      synchronized(gate) {
        if (active.get() != null) {
          continuation.resumeWithException(BiometricError.AuthenticationInProgress)
          return@suspendCancellableCoroutine
        }
        prompt = BiometricPrompt(
          activity,
          ContextCompat.getMainExecutor(activity),
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
              complete(continuation) { it.resume(Unit) }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
              val mapped = if (active.get()?.appCancelled == true) {
                BiometricError.AppCancelled
              } else {
                BiometricErrorMapper.fromPromptError(errorCode, errString)
              }
              complete(continuation) { it.resumeWithException(mapped) }
            }

            override fun onAuthenticationFailed() {
              // Intermediate failure (wrong biometry); wait for success / terminal error.
            }
          },
        )
        session = ActiveSession(prompt, continuation)
        active.set(session)
      }

      continuation.invokeOnCancellation {
        session.appCancelled = true
        runCatching { prompt.cancelAuthentication() }
        active.compareAndSet(session, null)
      }

      val allowsDeviceCredential =
        (authenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0
      val builder = BiometricPrompt.PromptInfo.Builder()
        .setTitle(options.title ?: trimmed)
        .setSubtitle(options.subtitle)
        .setAllowedAuthenticators(authenticators)

      if (!allowsDeviceCredential) {
        builder.setNegativeButtonText(
          options.negativeButtonText ?: configuration.defaultNegativeButtonText,
        )
      }

      runCatching {
        prompt.authenticate(builder.build())
      }.onFailure { error ->
        active.compareAndSet(session, null)
        if (continuation.isActive) {
          continuation.resumeWithException(
            BiometricError.Underlying(-1, error.message),
          )
        }
      }
    }
  }

  override fun cancelAuthentication() {
    val session = active.get() ?: return
    session.appCancelled = true
    runCatching { session.prompt.cancelAuthentication() }
  }

  /**
   * Opens biometric enrollment or security settings so the user can enroll biometrics /
   * set a device credential.
   *
   * @return `true` when an activity was started.
   */
  fun openBiometricSettings(context: Context = appContext): Boolean {
    val packageManager = context.packageManager
    val candidates = buildList {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        add(
          Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
              Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
              BiometricCapabilityProbe.ANY_BIOMETRIC,
            )
          },
        )
      }
      add(Intent(Settings.ACTION_SECURITY_SETTINGS))
      add(Intent(Settings.ACTION_SETTINGS))
    }
    for (intent in candidates) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      if (intent.resolveActivity(packageManager) != null) {
        return runCatching {
          context.startActivity(intent)
          true
        }.getOrDefault(false)
      }
    }
    return false
  }

  private fun complete(
    continuation: CancellableContinuation<Unit>,
    block: (CancellableContinuation<Unit>) -> Unit,
  ) {
    val session = active.get()
    if (session != null) {
      active.compareAndSet(session, null)
    }
    if (continuation.isActive) {
      block(continuation)
    }
  }

  private class ActiveSession(
    val prompt: BiometricPrompt,
    val continuation: CancellableContinuation<Unit>,
    @Volatile var appCancelled: Boolean = false,
  )
}

/**
 * Convenience: probe then authenticate; throws [BiometricCapability.probeError] when unavailable.
 */
suspend fun BiometricAuthenticating.authenticateIfAvailable(
  activity: FragmentActivity,
  reason: String,
  policy: BiometricPolicy? = null,
  options: BiometricAuthOptions = BiometricAuthOptions(),
) {
  val evaluated = policy ?: options.policy
  val capability = if (evaluated != null) capability(evaluated) else capability()
  if (!capability.canAuthenticate) {
    throw capability.probeError ?: BiometricError.BiometryNotAvailable
  }
  if (evaluated != null) {
    authenticate(activity, reason, evaluated, options)
  } else {
    authenticate(activity, reason, options)
  }
}
