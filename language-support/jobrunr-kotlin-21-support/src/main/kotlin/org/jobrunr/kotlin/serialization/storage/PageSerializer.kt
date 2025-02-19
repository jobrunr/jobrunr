package org.jobrunr.kotlin.serialization.storage

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.kotlin.serialization.utils.AnyInlineSerializer
import org.jobrunr.storage.Page

@OptIn(ExperimentalSerializationApi::class)
class PageSerializer<T : Any>() : KSerializer<Page<T>> {
	private val elementSerializer = AnyInlineSerializer<T>()
	
	override val descriptor = buildClassSerialDescriptor(Page::class.qualifiedName!!) {
		element("total", Long.serializer().descriptor)
		element("currentPage", Int.serializer().descriptor)
		element("totalPages", Int.serializer().descriptor)
		element("limit", Int.serializer().descriptor)
		element("offset", Long.serializer().descriptor)
		element("hasPrevious", Boolean.serializer().descriptor)
		element("hasNext", Boolean.serializer().descriptor)
		element("previousPage", String.serializer().descriptor)
		element("nextPage", String.serializer().descriptor)
		element("items", ListSerializer(elementSerializer).descriptor)
	}

	override fun serialize(encoder: Encoder, value: Page<T>) = encoder.encodeStructure(descriptor) {
		encodeLongElement(descriptor, 0, value.total)
		encodeIntElement(descriptor, 1, value.currentPage)
		encodeIntElement(descriptor, 2, value.totalPages)
		encodeIntElement(descriptor, 3, value.limit)
		encodeLongElement(descriptor, 4, value.offset)
		encodeBooleanElement(descriptor, 5, value.hasPreviousPage())
		encodeBooleanElement(descriptor, 6, value.hasNextPage())
		encodeNullableSerializableElement(descriptor, 7, String.serializer(), value.previousPage)
		encodeNullableSerializableElement(descriptor, 8, String.serializer(), value.nextPage)
		encodeSerializableElement(descriptor, 9, ListSerializer(elementSerializer), value.items as List<T>)
	}

	override fun deserialize(decoder: Decoder) = TODO("Not yet implemented")
}