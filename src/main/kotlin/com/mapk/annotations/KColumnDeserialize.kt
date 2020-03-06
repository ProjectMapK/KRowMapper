package com.mapk.annotations

import com.mapk.krowmapper.KColumnDeserializer
import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class KColumnDeserialize(val deserializer: KClass<out KColumnDeserializer<*, *>>)
