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

ただし、フィールドの命名規則とDBのカラムの命名規則が異なる場合は命名変換関数を渡す必要が有る点にご注意ください（後述）。

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
val dst: Dst = jdbcTemplate.query(query, KRowMapper(::Dst, /* 必要に応じた命名変換関数 */))
```

## インストール方法
`KRowMapper`は`JitPack`にて公開しており、`Maven`や`Gradle`といったビルドツールから手軽に利用できます。  
各ツールでの正確なインストール方法については下記をご参照ください。

- [ProjectMapK / KRowMapper](https://jitpack.io/#ProjectMapK/KRowMapper)

### Mavenでのインストール方法
以下は`Maven`でのインストール例です。

**1. JitPackのリポジトリへの参照を追加する**

```xml
<repositories>
	<repository>
	    <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
	</repository>
</repositories>
```

**2. dependencyを追加する**

```xml
<dependency>
    <groupId>com.github.ProjectMapK</groupId>
    <artifactId>KRowMapper</artifactId>
    <version>Tag</version>
</dependency>
```
