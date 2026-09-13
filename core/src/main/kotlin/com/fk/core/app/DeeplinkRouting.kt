package com.fk.core.app

import android.net.Uri

/**
 * Parses a URI into a [RouteContext].
 *
 * Conceptually aligned with iOS `FKDeeplinkParsing`.
 */
interface DeeplinkParser {
  fun parse(uri: Uri, source: DeeplinkSource = DeeplinkSource.Other): RouteContext?
}

/** Default parser for `http(s)` / custom-scheme URIs. */
class UrlDeeplinkParser : DeeplinkParser {
  override fun parse(uri: Uri, source: DeeplinkSource): RouteContext? {
    val raw = uri.toString().trim()
    if (raw.isEmpty()) return null
    val path = uri.path.orEmpty().ifBlank { "/" }
    val segments = uri.pathSegments.filter { it.isNotBlank() }
    val query = linkedMapOf<String, String>()
    for (name in uri.queryParameterNames) {
      uri.getQueryParameter(name)?.let { query[name] = it }
    }
    return RouteContext(
      uri = uri,
      host = uri.host,
      path = path,
      pathSegments = segments,
      query = query,
      source = source,
    )
  }
}

/**
 * Handles a parsed [RouteContext].
 *
 * Conceptually aligned with iOS `FKRouteHandling`.
 */
interface RouteHandler {
  val id: String
  fun canHandle(context: RouteContext): Boolean
  fun handle(context: RouteContext): RouteHandlingResult
}

/**
 * Registers handlers and opens URIs.
 *
 * Conceptually aligned with iOS Pluggable `FKDeeplinkRouting`.
 */
interface DeeplinkRouter {
  fun register(handler: RouteHandler)
  fun unregister(handlerId: String)
  fun open(uri: Uri, source: DeeplinkSource = DeeplinkSource.Other): RouteHandlingResult
  fun open(uriString: String, source: DeeplinkSource = DeeplinkSource.Other): RouteHandlingResult
  fun parse(uriString: String, source: DeeplinkSource = DeeplinkSource.Other): RouteContext
}

/**
 * Default [DeeplinkRouter]: parse → first matching [RouteHandler] in registration order.
 *
 * Also supports [PatternRouteHandler] host / path wildcards (BusinessKit-style).
 */
class DefaultDeeplinkRouter(
  private val parser: DeeplinkParser = UrlDeeplinkParser(),
) : DeeplinkRouter {
  private val lock = Any()
  private val handlers = LinkedHashMap<String, RouteHandler>()

  override fun register(handler: RouteHandler) {
    synchronized(lock) {
      handlers.remove(handler.id)
      handlers[handler.id] = handler
    }
  }

  override fun unregister(handlerId: String) {
    synchronized(lock) {
      handlers.remove(handlerId)
    }
  }

  override fun open(uri: Uri, source: DeeplinkSource): RouteHandlingResult {
    val context = parser.parse(uri, source)
      ?: return RouteHandlingResult.Failed("unparseable uri")
    val snapshot = synchronized(lock) { handlers.values.toList() }
    for (handler in snapshot) {
      if (!handler.canHandle(context)) continue
      val result = handler.handle(context)
      if (result !is RouteHandlingResult.NotHandled) return result
    }
    return RouteHandlingResult.NotHandled
  }

  override fun open(uriString: String, source: DeeplinkSource): RouteHandlingResult {
    val trimmed = uriString.trim()
    if (trimmed.isEmpty()) return RouteHandlingResult.Failed("empty uri")
    return open(Uri.parse(trimmed), source)
  }

  override fun parse(uriString: String, source: DeeplinkSource): RouteContext {
    val trimmed = uriString.trim()
    if (trimmed.isEmpty()) throw AppError.InvalidDeeplink(uriString)
    return parser.parse(Uri.parse(trimmed), source)
      ?: throw AppError.InvalidDeeplink(uriString)
  }
}

/**
 * Route matched by optional host and path pattern (`*` segment wildcard).
 *
 * Conceptually aligned with iOS `FKDeeplinkRoute`.
 */
class PatternRouteHandler(
  override val id: String,
  private val host: String? = null,
  private val pathPattern: String? = null,
  private val onMatch: (RouteContext) -> RouteHandlingResult,
) : RouteHandler {
  override fun canHandle(context: RouteContext): Boolean {
    if (host != null && host != context.host) return false
    val pattern = pathPattern ?: return true
    return matchesPath(pattern, context.path)
  }

  override fun handle(context: RouteContext): RouteHandlingResult = onMatch(context)

  companion object {
    fun matchesPath(pattern: String, path: String): Boolean {
      val normalizedPattern = pattern.trim().trim('/')
      if (normalizedPattern == "*") return true
      val expected = normalizedPattern.split('/').filter { it.isNotEmpty() }
      val actual = path.trim('/').split('/').filter { it.isNotEmpty() }
      if (expected.size != actual.size) return false
      return expected.indices.all { i ->
        expected[i] == "*" || expected[i] == actual[i]
      }
    }
  }
}
