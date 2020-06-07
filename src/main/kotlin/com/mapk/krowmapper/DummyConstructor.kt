package com.mapk.krowmapper

import com.mapk.krowmapper.KRowMapper as Mapper

@Suppress("FunctionName")
inline fun <reified T : Any> KRowMapper(
    noinline parameterNameConverter: ((String) -> String)? = null
) = Mapper(T::class, parameterNameConverter)
