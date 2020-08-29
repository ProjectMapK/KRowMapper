package com.mapk.krowmapper

import com.mapk.annotations.KColumnDeserializer
import com.mapk.core.EnumMapper
import com.mapk.core.KFunctionWithInstance
import com.mapk.core.ValueParameter
import com.mapk.core.getAnnotatedFunctions
import com.mapk.core.getAnnotatedFunctionsFromCompanionObject
import com.mapk.core.getKClass
import com.mapk.deserialization.AbstractKColumnDeserializer
import com.mapk.deserialization.KColumnDeserializeBy
import java.lang.IllegalArgumentException
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmName

internal sealed class ParameterForMap<S, D> {
    abstract val name: String
    abstract fun getObject(rs: ResultSet): D?

    private class Plain<T>(override val name: String, val requiredClazz: Class<T>) : ParameterForMap<T, T>() {
        override fun getObject(rs: ResultSet): T? = rs.getObject(name, requiredClazz)
    }

    private class Enum<D>(override val name: String, val enumClazz: Class<D>) : ParameterForMap<String, D>() {
        override fun getObject(rs: ResultSet): D? = EnumMapper.getEnum(enumClazz, rs.getString(name))
    }

    private class Deserializer<S : Any, D>(
        override val name: String,
        val srcClazz: Class<S>,
        private val deserializer: KFunction<D?>
    ) : ParameterForMap<S, D>() {
        constructor(
            name: String,
            deserializer: AbstractKColumnDeserializer<*, S, D>
        ) : this(name, deserializer.srcClass, deserializer::deserialize)

        override fun getObject(rs: ResultSet): D? = rs.getObject(name, srcClazz)?.let { deserializer.call(it) }
    }

    companion object {
        fun <T : Any> newInstance(param: ValueParameter<T>): ParameterForMap<*, T> {
            param.getDeserializer()?.let {
                return Deserializer(param.name, it)
            }

            param.requiredClazz.getDeserializer()?.let {
                val targetClass = it.parameters.single().getKClass().javaObjectType
                return Deserializer(param.name, targetClass, it)
            }

            return param.requiredClazz.javaObjectType.let {
                when (it.isEnum) {
                    true -> Enum(param.name, it)
                    false -> Plain(param.name, it)
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> ValueParameter<T>.getDeserializer(): AbstractKColumnDeserializer<*, *, T>? {
    val deserializers = this.annotations.mapNotNull { paramAnnotation ->
        paramAnnotation.annotationClass
            .findAnnotation<KColumnDeserializeBy>()
            ?.let { it.deserializer.primaryConstructor!!.call(paramAnnotation) }
    }

    if (1 < deserializers.size)
        throw IllegalArgumentException("Find multiple deserializer from ${(this.requiredClazz).jvmName}")

    return deserializers.singleOrNull() as AbstractKColumnDeserializer<*, *, T>?
}

private fun <T : Any> KClass<T>.getDeserializer(): KFunction<T>? {
    val deserializers: List<KFunction<T>> = ArrayList<KFunction<T>>().also {
        it.addAll(deserializerFromConstructors(this))
        it.addAll(deserializerFromStaticMethods(this))
        it.addAll(deserializerFromCompanionObject(this))
    }

    return when {
        deserializers.size <= 1 -> deserializers.singleOrNull()
        else -> throw IllegalArgumentException("Find multiple deserializer from $jvmName")
    }
}

private fun <T> Collection<KFunction<T>>.getDeserializerFromFunctions(): Collection<KFunction<T>> {
    return getAnnotatedFunctions<KColumnDeserializer, T>().onEach { it.isAccessible = true }
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
    return clazz.getAnnotatedFunctionsFromCompanionObject<KColumnDeserializer>()?.let { (instance, functions) ->
        functions.map {
            KFunctionWithInstance(it, instance) as KFunction<T>
        }
    } ?: emptyList()
}
