package com.fk.core.mapping

import kotlinx.serialization.Serializable

/**
 * Generic paginated list payload (`items` + `total` [+ optional page metadata]).
 *
 * With [MappingJson.Api] / [MappingJson.LenientApi], `pageSize` maps to `page_size`.
 */
@Serializable
data class Page<T>(
  val items: List<T>,
  val total: Int,
  val page: Int? = null,
  val pageSize: Int? = null,
)

/**
 * Alternate list shape using `list` + `count`.
 */
@Serializable
data class ListResponse<T>(
  val list: List<T>,
  val count: Int,
)
