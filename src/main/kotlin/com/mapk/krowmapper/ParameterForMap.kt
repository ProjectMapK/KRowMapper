package com.mapk.krowmapper

import com.mapk.annotations.KParameterAlias
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

class ParameterForMap<D : Any> private constructor(
    val name: String,
    val index: Int,
    val clazz: Class<D>
) {
    companion object {
        fun newInstance(param: KParameter, propertyNameConverter: (String) -> String = { it }): ParameterForMap<*> {
            val name: String = (param.findAnnotation<KParameterAlias>()?.value ?: propertyNameConverter(param.name!!))
            return ParameterForMap(name, param.index, (param.type.classifier as KClass<*>).java)
        }
    }
}
