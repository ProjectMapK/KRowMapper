package com.mapk.krowmapper

import com.mapk.annotations.KColumnDeserializer
import com.mapk.core.EnumMapper
import com.mapk.core.KFunctionWithInstance
import com.mapk.core.getAliasOrName
import java.lang.IllegalArgumentException
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.functions
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmName

class ParameterForMap private constructor(
    val param: KParameter,
    name: String,
    parameterKClazz: KClass<*>
) {
    private val objectGetter: (ResultSet) -> Any?

    init {
        val deserializer = parameterKClazz.getDeserializer()

        objectGetter = if (deserializer != null) {
            val targetClass = deserializer.parameters.single().type.classifier as KClass<*>

            {
                deserializer.call(it.getObject(name, targetClass.javaObjectType))
            }
        } else {
            {
                val clazz = parameterKClazz.javaObjectType

                when {
                    clazz.isEnum -> EnumMapper.getEnum(clazz, it.getString(name))
                    else -> it.getObject(name, clazz)
                }
            }
        }
    }

    fun getObject(rs: ResultSet): Any? = objectGetter(rs)

    companion object {
        fun newInstance(param: KParameter, propertyNameConverter: (String) -> String = { it }): ParameterForMap {
            return ParameterForMap(
                param,
                propertyNameConverter(param.getAliasOrName()!!),
                param.type.classifier as KClass<*>
            )
        }
    }
}

private fun <T : Any> KClass<T>.getDeserializer(): KFunction<T>? {
    val deserializers = deserializerFromConstructors(this) +
            deserializerFromStaticMethods(this) +
            deserializerFromCompanionObject(this)

    return when {
        deserializers.isEmpty() -> null
        deserializers.size == 1 -> deserializers.single()
        else -> throw IllegalArgumentException("Find multiple deserializer from $jvmName")
    }
}

private fun <T> Collection<KFunction<T>>.getDeserializerFromFunctions(): Collection<KFunction<T>> {
    return filter { it.annotations.any { annotation -> annotation is KColumnDeserializer } }
        .map { func ->
            func.isAccessible = true
            func
        }
}

private fun <T : Any> deserializerFromConstructors(clazz: KClass<T>): Collection<KFunction<T>> {
    return clazz.constructors.getDeserializerFromFunctions()
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> deserializerFromStaticMethods(clazz: KClass<T>): Collection<KFunction<T>> {
    val staticFunctions: Collection<KFunction<T>> = clazz.staticFunctions as Collection<KFunction<T>>
    return staticFunctions.getDeserializerFromFunctions()
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> deserializerFromCompanionObject(clazz: KClass<T>): Collection<KFunction<T>> {
    return clazz.companionObjectInstance?.let { companionObject ->
        companionObject::class.functions
            .filter { it.annotations.any { annotation -> annotation is KColumnDeserializer } }
            .map { function ->
                KFunctionWithInstance(
                    function,
                    companionObject
                ) as KFunction<T>
            }.toSet()
    } ?: emptySet()
}
