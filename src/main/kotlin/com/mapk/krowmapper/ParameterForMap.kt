package com.mapk.krowmapper

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

            param.annotations.forEach {
                if (it is KParameterAlias) alias = it.value
            }

            return ParameterForMap(
                alias ?: propertyNameConverter(param.name!!),
                param.index,
                (param.type.classifier as KClass<*>).java
            )
        }
    }
}
