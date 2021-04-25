package com.mapk.krowmapper

import com.mapk.core.KFunctionForCall
import com.mapk.core.toKConstructor
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class KRowMapper<T : Any> private constructor(
    private val function: KFunctionForCall<T>,
    conversionService: ConversionService?
) : RowMapper<T> {
    constructor(
        function: KFunction<T>,
        conversionService: ConversionService,
        parameterNameConverter: ((String) -> String)? = null
    ) : this(KFunctionForCall(function, parameterNameConverter), conversionService)

    constructor(
        function: KFunction<T>,
        parameterNameConverter: ((String) -> String)? = null
    ) : this(KFunctionForCall(function, parameterNameConverter), null)

    constructor(
        clazz: KClass<T>,
        conversionService: ConversionService,
        parameterNameConverter: ((String) -> String)? = null
    ) : this(clazz.toKConstructor(parameterNameConverter), conversionService)

    constructor(
        clazz: KClass<T>,
        parameterNameConverter: ((String) -> String)? = null
    ) : this(clazz.toKConstructor(parameterNameConverter), null)

    private val conversionService: ConversionService = conversionService ?: DefaultConversionService.getSharedInstance()
    private val parameters: List<ParameterForMap<*, *>> =
        function.requiredParameters.map { ParameterForMap.newInstance(it, this.conversionService) }

    override fun mapRow(rs: ResultSet, rowNum: Int): T {
        val adaptor = function.getArgumentAdaptor()

        parameters.forEach { adaptor.forcePut(it.name, it.getObject(rs)) }

        return function.call(adaptor)
    }
}
