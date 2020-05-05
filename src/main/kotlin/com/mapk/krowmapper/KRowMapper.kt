package com.mapk.krowmapper

import com.mapk.core.KFunctionForCall
import com.mapk.core.toKConstructor
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import org.springframework.jdbc.core.RowMapper

class KRowMapper<T : Any> private constructor(
    private val function: KFunctionForCall<T>
) : RowMapper<T> {
    constructor(function: KFunction<T>, parameterNameConverter: (String) -> String = { it }) : this(
        KFunctionForCall(function, parameterNameConverter)
    )

    constructor(clazz: KClass<T>, parameterNameConverter: (String) -> String = { it }) : this(
        clazz.toKConstructor(parameterNameConverter)
    )

    private val parameters: List<ParameterForMap> = function.requiredParameters.values
        .map { ParameterForMap.newInstance(it) }

    override fun mapRow(rs: ResultSet, rowNum: Int): T {
        val adaptor = function.getArgumentAdaptor()

        parameters.forEach { adaptor.putIfAbsent(it.name, it.getObject(rs)) }

        return function.call(adaptor)
    }
}
