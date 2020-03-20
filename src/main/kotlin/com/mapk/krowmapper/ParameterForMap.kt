package com.mapk.krowmapper

import com.mapk.core.EnumMapper
import com.mapk.core.getAliasOrName
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

class ParameterForMap private constructor(
    val name: String,
    val param: KParameter,
    val clazz: Class<*>,
    private val deserializer: KFunction<*>?
) {
    fun getObject(rs: ResultSet): Any? = when {
        clazz.isEnum -> EnumMapper.getEnum(clazz, rs.getString(name))
        else -> {
            val value: Any? = rs.getObject(name, clazz)
            deserializer?.call(value) ?: value
        }
    }

    companion object {
        fun newInstance(param: KParameter, propertyNameConverter: (String) -> String = { it }): ParameterForMap {
            return ParameterForMap(
                propertyNameConverter(param.getAliasOrName()!!),
                param,
                (param.type.classifier as KClass<*>).java,
                null
            )
        }
    }
}
