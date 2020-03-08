package com.mapk.krowmapper

import com.mapk.annotations.KColumnDeserialize
import com.mapk.annotations.KParameterAlias
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

class ParameterForMap<D : Any> private constructor(
    val name: String,
    val index: Int,
    val clazz: Class<D>
) {
    companion object {
        fun newInstance(param: KParameter, propertyNameConverter: (String) -> String = { it }): ParameterForMap<*> {
            var alias: String? = null
            var deserializer: KColumnDeserializer<*, *>

            param.annotations.forEach {
                if (it is KParameterAlias) alias = it.value
                if (it is KColumnDeserialize) deserializer = it.deserializer.objectInstance
                    ?: throw IllegalArgumentException("Deserializer class must be object.")
            }

            return ParameterForMap(
                alias ?: propertyNameConverter(param.name!!),
                param.index,
                (param.type.classifier as KClass<*>).java
            )
        }
    }
}
