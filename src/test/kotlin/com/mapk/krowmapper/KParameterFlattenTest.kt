package com.mapk.krowmapper

import com.google.common.base.CaseFormat
import com.mapk.annotations.KParameterFlatten
import com.mapk.core.NameJoiner
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.sql.ResultSet
import java.time.LocalDateTime

@DisplayName("KParameterFlattenテスト")
class KParameterFlattenTest {
    data class InnerDst(val fooFoo: Int, val barBar: String)
    data class Dst(
        @KParameterFlatten(nameJoiner = NameJoiner.Snake::class) val bazBaz: InnerDst,
        val quxQux: LocalDateTime
    )

    private fun camelToSnake(camel: String): String = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, camel)

    private val expected = Dst(InnerDst(1, "str"), LocalDateTime.MIN)

    @Test
    @DisplayName("スネークケースsrc -> キャメルケースdst")
    fun test() {
        val resultSet = mockk<ResultSet>() {
            every { getObject("baz_baz_foo_foo", any<Class<*>>()) } returns 1
            every { getObject("baz_baz_bar_bar", any<Class<*>>()) } returns "str"
            every { getObject("qux_qux", any<Class<*>>()) } returns LocalDateTime.MIN
        }

        val result = KRowMapper<Dst>(this::camelToSnake).mapRow(resultSet, 0)
        assertEquals(expected, result)

        verify(exactly = 1) { resultSet.getObject("baz_baz_foo_foo", Integer::class.java) }
        verify(exactly = 1) { resultSet.getObject("baz_baz_bar_bar", String::class.java) }
        verify(exactly = 1) { resultSet.getObject("qux_qux", LocalDateTime::class.java) }
    }
}
