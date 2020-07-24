package com.mapk.krowmapper

import com.mapk.deserialization.AbstractKColumnDeserializer
import com.mapk.deserialization.KColumnDeserializeBy
import io.mockk.every
import io.mockk.mockk
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("アノテーションによるデシリアライザー指定のテスト")
class DeserializerTest {
    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.RUNTIME)
    @MustBeDocumented
    @KColumnDeserializeBy(LocalDateTimeDeserializerImpl::class)
    annotation class LocalDateTimeDeserializer(val pattern: String = "yyyy-MM-dd'T'HH:mm:ss")

    class LocalDateTimeDeserializerImpl(
        annotation: LocalDateTimeDeserializer
    ) : AbstractKColumnDeserializer<LocalDateTimeDeserializer, String, LocalDateTime>(annotation) {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(annotation.pattern)

        override val srcClass: Class<String> = String::class.javaObjectType

        override fun deserialize(source: String): LocalDateTime = LocalDateTime.parse(source, formatter)
    }

    data class Dst(@LocalDateTimeDeserializer val dateTime: LocalDateTime?)

    val mapper = KRowMapper(::Dst)

    @Test
    @DisplayName("正常に変換した場合")
    fun isCollect() {
        val resultSet = mockk<ResultSet> {
            every { getObject("dateTime", any<Class<*>>()) } returns "2020-02-01T01:23:45"
        }

        val result = mapper.mapRow(resultSet, 0)

        assertEquals(
            LocalDateTime.parse("2020-02-01T01:23:45", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
            result.dateTime
        )
    }

    @Test
    @DisplayName("取得結果がnullだった場合")
    fun isNull() {
        val resultSet = mockk<ResultSet> {
            every { getObject("dateTime", any<Class<*>>()) } returns null
        }

        val result = mapper.mapRow(resultSet, 0)

        assertNull(result.dateTime)
    }
}
