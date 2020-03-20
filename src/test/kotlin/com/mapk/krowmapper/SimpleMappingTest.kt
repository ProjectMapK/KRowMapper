package com.mapk.krowmapper

import com.google.common.base.CaseFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.sql.ResultSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("単純なマッピングテスト")
class SimpleMappingTest {
    data class Dst(val fooId: Int, val strValue: String)

    private fun camelToSnake(camel: String): String = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, camel)

    @Test
    @DisplayName("スネークケースsrc -> キャメルケースdst")
    fun test() {
        val resultSet = mockk<ResultSet>()
        every { resultSet.getObject("foo_id", any<Class<*>>()) } returns 1
        every { resultSet.getObject("str_value", any<Class<*>>()) } returns "str"

        val result = KRowMapper(::Dst, this::camelToSnake).mapRow(resultSet, 0)

        assertEquals(1, result.fooId)
        assertEquals("str", result.strValue)

        verify(exactly = 1) { resultSet.getObject("foo_id", Integer::class.java) }
        verify(exactly = 1) { resultSet.getObject("str_value", String::class.java) }
    }
}
