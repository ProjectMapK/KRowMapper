package com.mapk.deserialization

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class KColumnDeserializeBy(val deserializer: KClass<out AbstractKColumnDeserializer<*, *, *>>)

abstract class AbstractKColumnDeserializer<A : Annotation, S : Any, D>(protected val annotation: A) {
    abstract val srcClass: Class<S>
    abstract fun deserialize(source: S): D?
}
