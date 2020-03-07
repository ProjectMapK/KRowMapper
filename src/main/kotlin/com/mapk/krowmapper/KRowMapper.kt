package com.mapk.krowmapper

import com.mapk.core.EnumMapper
import com.mapk.core.KFunctionForCall
import java.sql.ResultSet
import kotlin.reflect.KParameter
import org.springframework.jdbc.core.RowMapper

class KRowMapper<T>(
    private val function: KFunctionForCall<T>,
    propertyNameConverter: (String) -> String = { it }
) : RowMapper<T> {
    private val parameters: List<ParameterForMap<*>> = function.parameters
        .filter { it.kind != KParameter.Kind.INSTANCE }
        .map { ParameterForMap.newInstance(it, propertyNameConverter) }

    override fun mapRow(rs: ResultSet, rowNum: Int): T {
        val argumentBucket = function.getArgumentBucket()

        parameters.forEach { param ->
            argumentBucket.setArgument(when {
                param.clazz.isEnum -> EnumMapper.getEnum(param.clazz, rs.getObject(param.name, stringClazz))
                else -> rs.getObject(param.name, param.clazz)
            }, param.index)
        }

        return function.call(argumentBucket)
    }

    companion object {
        private val stringClazz = String::class.java
    }
}
