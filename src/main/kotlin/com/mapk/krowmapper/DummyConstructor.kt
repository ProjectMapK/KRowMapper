package com.mapk.krowmapper

import org.springframework.core.convert.ConversionService
import com.mapk.krowmapper.KRowMapper as Mapper

@Suppress("FunctionName")
inline fun <reified T : Any> KRowMapper(
    conversionService: ConversionService,
    noinline parameterNameConverter: ((String) -> String)? = null
) = Mapper(T::class, conversionService, parameterNameConverter)

@Suppress("FunctionName")
inline fun <reified T : Any> KRowMapper(
    noinline parameterNameConverter: ((String) -> String)? = null
) = Mapper(T::class, parameterNameConverter)
