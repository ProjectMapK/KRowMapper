package com.mapk.krowmapper

import com.mapk.annotations.KColumnDeserializer
import com.mapk.core.EnumMapper
import com.mapk.core.KFunctionWithInstance
import com.mapk.core.getAliasOrName
import com.mapk.deserialization.AbstractKColumnDeserializer
import com.mapk.deserialization.KColumnDeserializeBy
import java.lang.IllegalArgumentException
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmName

internal sealed class ParameterForMap {
    abstract val param: KParameter
    abstract val name: String
    abstract val clazz: Class<*>
    abstract fun getObject(rs: ResultSet): Any?

    private class Plain(
        override val param: KParameter,
        override val name: String,
        override val clazz: Class<*>
    ) : ParameterForMap() {
        override fun getObject(rs: ResultSet): Any? = rs.getObject(name, clazz)
    }

    private class Enum(
        override val param: KParameter,
        override val name: String,
        override val clazz: Class<*>
    ) : ParameterForMap() {
        override fun getObject(rs: ResultSet): Any? = EnumMapper.getEnum(clazz, rs.getString(name))
    }

    private class Deserializer(
        override val param: KParameter,
        override val name: String,
        override val clazz: Class<*>,
        private val deserializer: KFunction<*>
    ) : ParameterForMap() {
        constructor(
            param: KParameter,
            name: String,
            deserializer: AbstractKColumnDeserializer<*, *, *>
        ) : this(param, name, deserializer.srcClass, deserializer::deserialize)

        override fun getObject(rs: ResultSet): Any? = deserializer.call(rs.getObject(name, clazz))
    }

    companion object {
        fun newInstance(param: KParameter, parameterNameConverter: (String) -> String): ParameterForMap {
            val name = parameterNameConverter(param.getAliasOrName()!!)

            param.getDeserializer()?.let {
                return Deserializer(param, name, it)
            }

            val parameterKClazz = param.type.classifier as KClass<*>

            parameterKClazz.getDeserializer()?.let {
                val targetClass = (it.parameters.single().type.classifier as KClass<*>).javaObjectType
                return Deserializer(param, name, targetClass, it)
            }

            return parameterKClazz.javaObjectType.let {
                when (it.isEnum) {
                    true -> Enum(param, name, it)
                    false -> Plain(param, name, it)
                }
            }
        }
    }
}

private fun KParameter.getDeserializer(): AbstractKColumnDeserializer<*, *, *>? {
    val deserializers = this.annotations.mapNotNull { paramAnnotation ->
        paramAnnotation.annotationClass
            .findAnnotation<KColumnDeserializeBy>()
            ?.let { it.deserializer.primaryConstructor!!.call(paramAnnotation) }
    }

    if (1 < deserializers.size)
        throw IllegalArgumentException("Find multiple deserializer from ${(this.type.classifier as KClass<*>).jvmName}")

    return deserializers.singleOrNull()
}

private fun <T : Any> KClass<T>.getDeserializer(): KFunction<T>? {
    val deserializers = deserializerFromConstructors(this) +
            deserializerFromStaticMethods(this) +
            deserializerFromCompanionObject(this)

    return when {
        deserializers.size <= 1 -> deserializers.singleOrNull()
        else -> throw IllegalArgumentException("Find multiple deserializer from $jvmName")
    }
}

private fun <T> Collection<KFunction<T>>.getDeserializerFromFunctions(): Collection<KFunction<T>> {
    return filter { it.annotations.any { annotation -> annotation is KColumnDeserializer } }
        .onEach { it.isAccessible = true }
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
