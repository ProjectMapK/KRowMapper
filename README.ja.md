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
また、外部の設定ファイルは一切必要ありません。

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

## 動作原理
`KRowMapper`は以下のように動作します。

1. 呼び出し対象の`KFunction`を取り出す
2. `KFunction`を解析し、必要な引数とその取り出し方・デシリアライズ方法を決定する
3. `ResultSet`からそれぞれの引数に対応する値の取り出し・デシリアライズを行い、`KFunction`を呼び出す

最終的にはコンストラクタやファクトリーメソッドなどを呼び出してマッピングを行うため、結果は確実に`Kotlin`上の引数・`nullability`等の制約に従います。  
つまり、`Kotlin`の`null`安全が壊れることによる実行時エラーは発生しません。

また、`Kotlin`特有の機能であるデフォルト引数等にも対応しています。

## 詳細な使い方
### KRowMapperの初期化
`KRowMapper`は`method reference(KFunction)`または`KClass`から初期化できます。
また、`KRowMapper`はデフォルトではフィールドの命名によってカラムとの対応を見るため、「フィールドがキャメルケースでカラムはスネークケース」というような場合、パラメータ名を変換する関数も渡す必要が有ります。

#### method reference(KFunction)からの初期化
`KRowMapper`は`method reference`から初期化できます。

```kotlin
data class Dst(
    foo: String,
    bar: String,
    baz: Int?,

    ...

)

// コンストラクタのメソッドリファレンスを取得
val dstConstructor: KFunction<Dst> = ::Dst
// KFunctionからKRowMapperを初期化
val mapper: KRowMapper<Dst> = KRowMapper(dstConstructor)
```

ユースケースとしては特に以下の3種類の`method reference`を利用することが大半だと思われます。

- コンストラクタのメソッドリファレンス: `::Dst`
- `companion object`からのメソッドリファレンス: `(Dst)::factoryMethod`
- `this`に定義されたメソッドのメソッドリファレンス: `this::factoryMethod`
