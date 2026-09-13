package com.fk.core.pluggable.mock

import com.fk.core.pluggable.networking.CredentialStore

/** In-memory [CredentialStore] for samples and tests. */
class MockCredentialStore(
  override var accessToken: String? = null,
  override var refreshToken: String? = null,
) : CredentialStore
