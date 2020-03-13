package com.mapk.krowmapper

import com.mapk.core.EnumMapper
import com.mapk.core.KFunctionForCall
import java.sql.ResultSet
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import org.springframework.jdbc.core.RowMapper

class KRowMapper<T : Any> private constructor(
    private val function: KFunctionForCall<T>,
    propertyNameConverter: (String) -> String = { it }
) : RowMapper<T> {
    constructor(function: KFunction<T>, propertyNameConverter: (String) -> String = { it }) : this(
        KFunctionForCall(function), propertyNameConverter
    )

    private val parameters: List<ParameterForMap<*>> = function.parameters
        .filter { it.kind != KParameter.Kind.INSTANCE }
        .map { ParameterForMap.newInstance(it, propertyNameConverter) }

    override fun mapRow(rs: ResultSet, rowNum: Int): T {
        val argumentBucket = function.getArgumentBucket()

        parameters.forEach { param ->
            argumentBucket.putIfAbsent(param.param, when {
                param.clazz.isEnum -> EnumMapper.getEnum(param.clazz, rs.getObject(param.name, stringClazz))
                else -> rs.getObject(param.name, param.clazz)
            })
        }

        return function.call(argumentBucket)
    }

    companion object {
        private val stringClazz = String::class.java
    }
}
