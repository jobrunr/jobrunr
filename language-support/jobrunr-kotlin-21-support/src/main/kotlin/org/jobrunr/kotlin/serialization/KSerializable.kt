package org.jobrunr.kotlin.serialization

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

sealed interface KSerializable<KotlinType: Any, JavaType : Any> {
	fun mapToJava(kotlin: KotlinType): JavaType
	
	fun mapToKotlin(java: JavaType): KotlinType
	
	@Suppress("UNCHECKED_CAST")
	@OptIn(InternalSerializationApi::class)
	val serializer: KSerializer<KotlinType>
		get() = (this::class.run {
				if (simpleName == "Companion") java.declaringClass.kotlin
				else this
			}.serializer() as KSerializer<KotlinType>)
}