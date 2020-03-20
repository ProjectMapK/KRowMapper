package com.mapk.krowmapper

import com.mapk.core.EnumMapper
import com.mapk.core.getAliasOrName
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

class ParameterForMap private constructor(
    val param: KParameter,
    private val name: String,
    kClazz: KClass<*>
) {
    private val javaClazz: Class<*> = kClazz.java
    private val deserializer: KFunction<*>?

    init {
        deserializer = null
    }

    fun getObject(rs: ResultSet): Any? = when {
        javaClazz.isEnum -> EnumMapper.getEnum(javaClazz, rs.getString(name))
        else -> {
            val value: Any? = rs.getObject(name, javaClazz)
            deserializer?.call(value) ?: value
        }
    }

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
