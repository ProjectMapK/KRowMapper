package com.mapk.krowmapper

import com.mapk.annotations.KPropertyAlias
import com.mapk.core.KFunctionForCall
import java.sql.ResultSet
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import org.springframework.jdbc.core.RowMapper

class KRowMapper<T>(
    private val function: KFunctionForCall<T>,
    propertyNameConverter: (String) -> String = { it }
) : RowMapper<T> {
    private val parameterMap: Map<String, KParameter> = function.parameters
        .filter { it.kind != KParameter.Kind.INSTANCE }
        .associateBy { (it.findAnnotation<KPropertyAlias>()?.value ?: propertyNameConverter(it.name!!)) }

    override fun mapRow(rs: ResultSet?, rowNum: Int): T {
        val argumentBucket = function.getArgumentBucket()

        /* TODO: 実装 */

        return function.call(argumentBucket)
    }
}
