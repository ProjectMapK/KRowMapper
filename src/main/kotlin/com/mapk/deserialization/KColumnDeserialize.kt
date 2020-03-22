package com.mapk.deserialization

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class KColumnDeserializeBy(val deserializer: KClass<out KColumnDeserializer<*, *, *>>)

abstract class KColumnDeserializer<A : Annotation, S : Any, D : Any>(protected val annotation: A) {
    abstract val srcClass: Class<S>
    abstract fun deserialize(source: S?): D?
}
