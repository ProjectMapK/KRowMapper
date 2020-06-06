[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CircleCI](https://circleci.com/gh/ProjectMapK/KRowMapper.svg?style=svg)](https://circleci.com/gh/ProjectMapK/KRowMapper)
[![](https://jitci.com/gh/ProjectMapK/KRowMapper/svg)](https://jitci.com/gh/ProjectMapK/KRowMapper)

KRowMapper
====
`KRowMapper`は`Kotlin`向けの`RowMapper`であり、以下のような機能を提供します。

- `BeanPropertyRowMapper`のような、最小限の労力でのオブジェク関係トマッピング（`ORM`）
- リフレクションを用いた関数呼び出しベースの安全なマッピング
- 豊富なデシリアライズ機能による柔軟なマッピング

## デモコード
手動でマッピングコードを書いた場合と`KRowMapper`を用いた場合を比較します。  
手動で書く場合フィールド件数が多ければ多いほど記述がかさみますが、`KRowMapper`を用いることで殆どコードを書かずにマッピングを行えます。

```kotlin
// マップ対象クラス
data class Dst(
    foo: String,
    bar: String,
    baz: Int?,

    ...

)

// 手動でRowMapperを書いた場合
val dst: Dst = jdbcTemplate.query(query) { rs, _ ->
    Dst(
            rs.getString("foo"),
            rs.getString("bar"),
            rs.getInt("baz"),

            ...

    )
}

// KRowMapperを用いた場合
val dst: Dst = jdbcTemplate.query(query, KRowMapper(::Dst))
```
