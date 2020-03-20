package com.mapk.krowmapper

import com.mapk.core.KFunctionForCall
import com.mapk.core.isUseDefaultArgument
import com.mapk.core.toKConstructor
import java.sql.ResultSet
import kotlin.reflect.KClass
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

    constructor(clazz: KClass<T>, propertyNameConverter: (String) -> String = { it }) : this(
        clazz.toKConstructor(), propertyNameConverter
    )

    private val parameters: List<ParameterForMap> = function.parameters
        .filter { it.kind != KParameter.Kind.INSTANCE && !it.isUseDefaultArgument() }
        .map { ParameterForMap.newInstance(it, propertyNameConverter) }

    override fun mapRow(rs: ResultSet, rowNum: Int): T {
        val argumentBucket = function.getArgumentBucket()

        parameters.forEach { param ->
            argumentBucket.putIfAbsent(param.param, param.getObject(rs))
        }

        return function.call(argumentBucket)
    }
}
