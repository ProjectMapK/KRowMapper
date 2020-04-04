package com.mapk.krowmapper

import com.google.common.base.CaseFormat
import javax.sql.DataSource
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource
import org.springframework.jdbc.core.simple.SimpleJdbcInsert

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("DBを用いてマッピングを行うテスト")
class UseDBMappingTest {
    enum class FooStatus {
        active, archive, deleted
    }

    data class Foo(
        val fooId: Int,
        val fooName: String,
        val fooStatus: FooStatus,
        val isBar: Boolean,
        val description: String?
    ) {
        companion object {
            fun fooFactory(
                fooId: Int,
                fooName: String,
                fooStatus: FooStatus,
                isBar: String,
                description: String?
            ) = Foo(
                fooId, fooName, fooStatus, isBar.toBoolean(), description
            )
        }
    }

    data class FooInsert(
        val fooId: Int,
        val fooName: String,
        private val fooStatus: FooStatus,
        private val isBar: Boolean,
        val description: String?
    ) {
        fun getFooStatus(): String = fooStatus.name
        fun getIsBar(): String = isBar.toString()
    }

    lateinit var jdbcTemplate: JdbcTemplate

    @BeforeAll
    fun beforeAll() {
        val dataSource: DataSource = JdbcDataSource().apply {
            setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS APP\\;SET SCHEMA APP;")
        }

        jdbcTemplate = JdbcTemplate(dataSource)

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS `foo_table` (
              `foo_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
              `foo_name` VARCHAR(255) NOT NULL,
              `foo_status` ENUM('active', 'archive', 'deleted') NOT NULL,
              `is_bar` ENUM('true', 'false') NOT NULL,
              `description` VARCHAR(1023) NULL DEFAULT NULL,
              PRIMARY KEY (`foo_id`)
            );
        """.trimIndent())

        val data = FooInsert(10, "Foo", FooStatus.archive, false, null)

        SimpleJdbcInsert(jdbcTemplate).withTableName("foo_table").execute(BeanPropertySqlParameterSource(data))
    }

    @Test
    fun test() {
        val result = jdbcTemplate.query("SELECT * FROM foo_table", KRowMapper((Foo)::fooFactory) {
            CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it)
        }).single()

        assertEquals(
            Foo(10, "Foo", FooStatus.archive, false, null),
            result
        )
    }

    @AfterAll
    fun afterAll() {
        jdbcTemplate.dataSource!!.connection.close()
    }
}
