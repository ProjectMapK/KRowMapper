package com.mapk.krowmapper

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.convert.ConversionService
import java.sql.ResultSet
import java.time.YearMonth

@DisplayName("ConversionServiceによるデシリアライズのテスト")
private class DeserializeByConversionServiceTest {
    data class Dst(val yearMonth: YearMonth)

    @Test
    fun test() {
        val conversionService = mockk<ConversionService> {
            every { convert(202101, YearMonth::class.java) } returns (YearMonth.of(2021, 1))
        }
        val mapper = KRowMapper<Dst>(conversionService)
        val resultSet = mockk<ResultSet> {
            every { getObject("yearMonth") } returns 202101
        }

        assertEquals(Dst(YearMonth.of(2021, 1)), mapper.mapRow(resultSet, 0))
        verify(exactly = 1) { conversionService.convert(202101, YearMonth::class.java) }
    }
}
