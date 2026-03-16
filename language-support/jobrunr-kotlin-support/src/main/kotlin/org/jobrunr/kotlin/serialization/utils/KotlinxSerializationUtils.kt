package org.jobrunr.kotlin.serialization.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializerOrNull
import kotlin.reflect.KClass

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
fun <T : Any> SerializersModule.serializer(kClass: KClass<T>): KSerializer<T>? =
    kClass.serializerOrNull()
        ?: getContextual(kClass)
        ?: getPolymorphic(Any::class, kClass) as KSerializer<T>?
        ?: getPolymorphic(kClass, kClass.qualifiedName) as KSerializer<T>?
        ?: kClass.supertypes
            .firstNotNullOfOrNull {
                getPolymorphic(it.classifier as KClass<*>, kClass.qualifiedName)
            } as KSerializer<T>?

class DeserializationUnsupportedException(serialName: String) : SerializationException("Deserialization for $serialName is unsupported!")

fun KSerializer<*>.DeserializationUnsupportedException() = DeserializationUnsupportedException(descriptor.serialName)
