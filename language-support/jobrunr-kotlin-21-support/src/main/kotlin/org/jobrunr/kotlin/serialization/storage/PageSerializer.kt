package org.jobrunr.kotlin.serialization.storage

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.jobrunr.kotlin.serialization.utils.AnyInlineSerializer
import org.jobrunr.kotlin.serialization.utils.FieldBasedSerializer
import org.jobrunr.storage.Page
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class PageSerializer<T : Any> : FieldBasedSerializer<Page<T>>(
	Page::class as KClass<Page<T>>,
	Field("total", Long.serializer()) { it.total },
	Field("currentPage", Int.serializer()) { it.currentPage },
	Field("totalPages", Int.serializer()) { it.totalPages },
	Field("limit", Int.serializer()) { it.limit },
	Field("offset", Long.serializer()) { it.offset },
	Field("hasPrevious", Boolean.serializer()) { it.hasPreviousPage() },
	Field("hasNext", Boolean.serializer()) { it.hasNextPage() },
	Field("previousPage", String.serializer(), nullable = true) { it.previousPage },
	Field("nextPage", String.serializer(), nullable = true) { it.nextPage },
	Field("items", ListSerializer(AnyInlineSerializer())) { it.items as List<T> }
)
