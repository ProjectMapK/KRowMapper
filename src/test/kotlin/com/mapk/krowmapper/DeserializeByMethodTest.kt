package com.mapk.krowmapper

import com.mapk.annotations.KColumnDeserializer
import io.mockk.every
import io.mockk.mockk
import java.sql.ResultSet
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DeserializeByMethodTest {
    data class ByConstructor @KColumnDeserializer constructor(val fooString: String)
    data class BySecondaryConstructor(val barShort: Short) {
        @KColumnDeserializer
        constructor(bar: String) : this(bar.toShort())
    }
    data class ByCompanionObject(val bazInt: Int) {
        companion object {
            @KColumnDeserializer
            fun factory(baz: String) = ByCompanionObject(baz.toInt())
        }
    }

    data class Dst(
        val foo: ByConstructor,
        val bar: BySecondaryConstructor,
        val baz: ByCompanionObject,
        val qux: ByStaticMethod
    )

    @Test
    @DisplayName("マッピングテスト")
    fun test() {
        val resultSet = mockk<ResultSet>()
        every { resultSet.getObject("foo", any<Class<*>>()) } returns "foo"
        every { resultSet.getObject("bar", any<Class<*>>()) } returns "123"
        every { resultSet.getObject("baz", any<Class<*>>()) } returns "321"
        every { resultSet.getObject("qux", any<Class<*>>()) } returns 777

        val result = KRowMapper(::Dst).mapRow(resultSet, 0)

        Assertions.assertEquals("foo", result.foo.fooString)
        Assertions.assertEquals(123, result.bar.barShort)
        Assertions.assertEquals(321, result.baz.bazInt)
        Assertions.assertEquals("777", result.qux.quxString)
    }
}
