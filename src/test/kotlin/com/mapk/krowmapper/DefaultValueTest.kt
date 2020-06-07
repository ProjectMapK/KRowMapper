package com.mapk.krowmapper

import com.google.common.base.CaseFormat
import com.mapk.annotations.KUseDefaultArgument
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.sql.ResultSet
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DefaultValueTest {
    data class Dst(val fooId: Int, @param:KUseDefaultArgument val barValue: String = "default")

    private fun camelToSnake(camel: String): String = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, camel)

    @Test
    @DisplayName("デフォルト値を用いたマッピングテスト")
    fun test() {
        val resultSet = mockk<ResultSet>()
        every { resultSet.getObject("foo_id", any<Class<*>>()) } returns 1
        every { resultSet.getObject("bar_value", any<Class<*>>()) } returns "From result set."

        val result = KRowMapper(Dst::class, this::camelToSnake).mapRow(resultSet, 0)

        Assertions.assertEquals(1, result.fooId)
        Assertions.assertEquals("default", result.barValue)

        verify(exactly = 1) { resultSet.getObject("foo_id", Integer::class.java) }
        verify(exactly = 0) { resultSet.getObject("bar_value", String::class.java) }
    }
}
