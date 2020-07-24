[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CircleCI](https://circleci.com/gh/ProjectMapK/KRowMapper.svg?style=svg)](https://circleci.com/gh/ProjectMapK/KRowMapper)
[![](https://jitci.com/gh/ProjectMapK/KRowMapper/svg)](https://jitci.com/gh/ProjectMapK/KRowMapper)
[![codecov](https://codecov.io/gh/ProjectMapK/KRowMapper/branch/master/graph/badge.svg)](https://codecov.io/gh/ProjectMapK/KRowMapper)

KRowMapper
====
`KRowMapper`は`Kotlin`向けの`RowMapper`であり、以下の機能を提供します。

- `BeanPropertyRowMapper`と同等の、最小限の労力でのオブジェク関係トマッピング（`ORM`）
- リフレクションを用いた関数呼び出しベースの柔軟で安全なマッピング
- 豊富な機能による、より柔軟かつ労力の少ないマッピング

## デモコード
手動でマッピングコードを書いた場合と`KRowMapper`を用いた場合を比較します。  
手動で書く場合引数が多ければ多いほど記述がかさみますが、`KRowMapper`を用いることで殆どコードを書かずにマッピングを行えます。  
また、外部の設定ファイルは一切必要ありません。

ただし、引数の命名規則とDBのカラムの命名規則が異なる場合は命名変換関数を渡す必要が有る点にご注意ください（[後述](#引数名の変換)）。

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

最終的にはコンストラクタや`companion object`に定義したファクトリーメソッドなどを呼び出してマッピングを行うため、結果は`Kotlin`上の引数・`nullability`等の制約に従います。  
つまり、`Kotlin`の`null`安全が壊れることによる実行時エラーは発生しません（デシリアライズ方法によっては、型引数の`nullability`に関して`null`安全が壊れる場合が有ります）。

また、`Kotlin`特有の機能であるデフォルト引数等にも対応しています。

## KRowMapperの初期化
`KRowMapper`は呼び出し対象の`method reference(KFunction)`、またはマッピング先の`KClass`から初期化できます。

また、`KRowMapper`はデフォルトでは引数名によってカラムとの対応を見るため、「引数がキャメルケースでカラムはスネークケース」というような場合、引数名を変換する関数も渡す必要が有ります。

### method reference(KFunction)からの初期化
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

### KClassからの初期化
`KRowMapper`は`KClass`からも初期化できます。  
デフォルトではプライマリーコンストラクタが呼び出し対象になります。

```kotlin
data class Dst(...)

val mapper: KRowMapper<Dst> = KRowMapper(Dst::class)
```

ダミーコンストラクタを用いることで以下のようにも書けます。

```kotlin
val mapper: KRowMapper<Dst> = KRowMapper<Dst>()
```

#### KConstructorアノテーションによる呼び出し対象指定
`KClass`から初期化を行う場合、`KConstructor`アノテーションを用いて呼び出し対象の関数を指定することができます。  

以下の例ではセカンダリーコンストラクタが呼び出されます。

```kotlin
data class Dst(...) {
    @KConstructor
    constructor(...) : this(...)
}

val mapper: KRowMapper<Dst> = KRowMapper(Dst::class)
```

同様に、以下の例ではファクトリーメソッドが呼び出されます。

```kotlin
data class Dst(...) {
    companion object {
        @KConstructor
        fun factory(...): Dst {
            ...
        }
    }
}

val mapper: KRowMapper<Dst> = KRowMapper(Dst::class)
```

### 引数名の変換
`KRowMapper`は、デフォルトでは引数名に対応するカラムをそのまま探すという挙動になります。

```kotlin
data class Dst(
    fooFoo: String,
    barBar: String,
    bazBaz: Int?
)

// fooFoo, barBar, bazBazの3引数が要求される
val mapper: KRowMapper<Dst> = KRowMapper(::Dst)

// 挙動としては以下と同等
val rowMapper: RowMapper<Dst> = { rs, _ ->
    Dst(
            rs.getString("fooFoo"),
            rs.getString("barBar"),
            rs.getInt("bazBaz"),
    )
}
```

一方、引数の命名規則がキャメルケースかつDBのカラムの命名規則がスネークケースというような場合、このままでは一致を見ることができません。  
このような状況では`KRowMapper`の初期化時に命名変換関数を渡す必要が有ります。

```kotlin
val mapper: KRowMapper<Dst> = KRowMapper(::Dst) { fieldName: String ->
    /* 命名変換処理 */
}
```

また、当然ながらラムダ内で任意の変換処理を行うこともできます。

#### 実際の変換処理
`KRowMapper`では命名変換処理を提供していませんが、`Spring`やそれを用いたプロジェクトの中で用いられるライブラリでは命名変換処理が提供されている場合が有ります。  
`Jackson`、`Guava`の2つのライブラリで実際に「キャメルケース -> スネークケース」の変換処理を渡すサンプルコードを示します。

##### Jackson
```kotlin
import com.fasterxml.jackson.databind.PropertyNamingStrategy

val parameterNameConverter: (String) -> String = PropertyNamingStrategy.SnakeCaseStrategy()::translate
val mapper: KRowMapper<Dst> = KRowMapper(::Dst, parameterNameConverter)
```

##### Guava
```kotlin
import com.google.common.base.CaseFormat

val parameterNameConverter: (String) -> String = { fieldName: String ->
    CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName)
}
val mapper: KRowMapper<Dst> = KRowMapper(::Dst, parameterNameConverter)
```

## 詳細な使い方
ここまでに記載した内容を用いることで`BeanPropertyRowMapper`以上の柔軟で安全なマッピングを行えますが、`KRowMapper`の提供する豊富な機能を使いこなすことで、更なる労力の削減が可能です。

ただし、よりプレーンな`Kotlin`に近い書き方をしたい場合にはこれらの機能を用いず、呼び出し対象メソッドで全ての初期化処理を書くことをお勧めします。

### 値のデシリアライズ
`KRowMapper`は`java.sql.ResultSet`から値の取得を行うため、デフォルトではこの実装でサポートされていない型を取得することはできません。  
この問題に対応するため、`KRowMapper`ではデフォルトの変換機能に加え以下の3種類のデシリアライズ方法を提供しています。

1. `KColumnDeserializer`アノテーションを利用したデシリアライズ
2. デシリアライズアノテーションを自作してのデシリアライズ
3. 複数引数からのデシリアライズ

#### KColumnDeserializerアノテーションを利用したデシリアライズ
自作のクラスで、かつ単一引数から初期化できる場合、`KColumnDeserializer`アノテーションを用いたデシリアライズが利用できます。  
`KColumnDeserializer`アノテーションは、コンストラクタ、もしくは`companion object`に定義したファクトリーメソッドに対して付与できます。

```kotlin
// プライマリーコンストラクタに付与した場合
data class FooId @KColumnDeserializer constructor(val id: Int)
```

```kotlin
// セカンダリーコンストラクタに付与した場合
data class FooId(val id: Int) {
    @KColumnDeserializer
    constructor(id: String) : this(id.toInt())
}
```

```kotlin
// ファクトリーメソッドに付与した場合
data class FooId(val id: Int) {
    companion object {
        @KColumnDeserializer
        fun of(id: String): FooId = FooId(id.toInt())
    }
}
```

`KColumnDeserializer`アノテーションが設定されているクラスは、特別な記述をしなくても引数としてマッピングが可能です。

```kotlin
// fooIdにKColumnDeserializerが付与されていればDstでは何もせずに正常にマッピングができる
data class Dst(
    fooId: FooId,
    bar: String,
    baz: Int?,

    ...

)
```

#### デシリアライズアノテーションを自作してのデシリアライズ
`KColumnDeserializer`を用いることができない場合、デシリアライズアノテーションを自作してパラメータに付与することでデシリアライズを行うことができます。

デシリアライズアノテーションの自作はデシリアライズアノテーションとデシリアライザーの組を定義することで行います。  
例として`String`から`LocalDateTime`にデシリアライズを行う`LocalDateTimeDeserializer`の作成の様子を示します。

##### デシリアライズアノテーションを定義する
`@Target(AnnotationTarget.VALUE_PARAMETER)`と`KColumnDeserializeBy`アノテーション、他幾つかのアノテーションを付与することで、デシリアライズアノテーションを定義できます。

`KColumnDeserializeBy`アノテーションの引数は、後述するデシリアライザーの`KClass`を渡します。  
この例では`LocalDateTimeDeserializerImpl`がそれです。

また、この例ではアノテーションに引数を定義していますが、この値はデシリアライザーから参照することができます。

```kotlin
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@KColumnDeserializeBy(LocalDateTimeDeserializerImpl::class)
annotation class LocalDateTimeDeserializer(val pattern: String = "yyyy-MM-dd'T'HH:mm:ss")
```

##### デシリアライザーを定義する

デシリアライザーは`AbstractKColumnDeserializer<A, S, D>`を継承して定義します。  
ジェネリクス`A`,`S`,`D`はそれぞれ以下の意味が有ります。
- `A`: デシリアライズアノテーションの`Type`
- `S`: デシリアライズ前の`Type`
- `D`: デシリアライズ後の`Type`

```kotlin
class LocalDateTimeDeserializerImpl(
    annotation: LocalDateTimeDeserializer
) : AbstractKColumnDeserializer<LocalDateTimeDeserializer, String, LocalDateTime>(annotation) {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(annotation.pattern)

    override val srcClass: Class<String> = String::class.javaObjectType

    override fun deserialize(source: String): LocalDateTime = LocalDateTime.parse(source, formatter)
}
```

デシリアライザーのプライマリコンストラクタの引数はデシリアライズアノテーションのみ取る必要が有ります。  
これは`KRowMapper`の初期化時に呼び出されます。

例の通り、アノテーションに定義した引数は適宜参照することができます。

##### 付与する
ここまでで定義したデシリアライズアノテーションとデシリアライザーをまとめて書くと以下のようになります。

```kotlin
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@KColumnDeserializeBy(LocalDateTimeDeserializerImpl::class)
annotation class LocalDateTimeDeserializer(val pattern: String = "yyyy-MM-dd'T'HH:mm:ss")

class LocalDateTimeDeserializerImpl(
    annotation: LocalDateTimeDeserializer
) : AbstractKColumnDeserializer<LocalDateTimeDeserializer, String, LocalDateTime>(annotation) {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(annotation.pattern)

    override val srcClass: Class<String> = String::class.javaObjectType

    override fun deserialize(source: String): LocalDateTime = LocalDateTime.parse(source, formatter)
}
```

これを付与すると以下のようになります。  
`pattern`には任意の引数が渡せるため、柔軟性が高いことが分かります。

```kotlin
data class Dst(
        @LocalDateTimeDeserializer(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        val createTime: LocalDateTime
)
```

#### 複数引数からのデシリアライズ
以下のように、`InnerDst`が複数引数を要求している場合、そのままでは`KRwoMapper`を用いて`Dst`をマッピングすることはできません。  
このように複数引数を要求するようなクラスは、`KParameterFlatten`アノテーションを用いることでデシリアライズできます。

```kotlin
data class InnerDst(val fooFoo: Int, val barBar: String)
data class Dst(val bazBaz: InnerDst, val quxQux: LocalDateTime)
```

`DB`のカラム名がスネークケースであり、引数名をプレフィックスに指定する場合、以下のように付与します。  
ここで、`KParameterFlatten`を指定されたクラスは、前述の`KConstructor`アノテーションで指定した関数またはプライマリコンストラクタから初期化されます。

```kotlin
data class InnerDst(val fooFoo: Int, val barBar: String)
data class Dst(
    @KParameterFlatten(nameJoiner = NameJoiner.Snake::class)
    val bazBaz: InnerDst,
    val quxQux: LocalDateTime
)

// baz_baz_foo_foo, baz_baz_bar_bar, qux_quxの3引数が要求される
val mapper: KRowMapper<Dst> = KRowMapper(::Dst) { /* キャメル -> スネークの命名変換関数 */ }
```

##### KParameterFlattenアノテーションのオプション
`KParameterFlatten`アノテーションはネストしたクラスの引数名の扱いについて2つのオプションを持ちます。

###### fieldNameToPrefix
`KParameterFlatten`アノテーションはデフォルトでは引数名をプレフィックスに置いた名前で一致を見ようとします。  
引数名をプレフィックスに付けたくない場合は`fieldNameToPrefix`オプションに`false`を指定します。

```kotlin
data class InnerDst(val fooFoo: Int, val barBar: String)
data class Dst(
    @KParameterFlatten(fieldNameToPrefix = false)
    val bazBaz: InnerDst,
    val quxQux: LocalDateTime
)

// foo_foo, bar_bar, qux_quxの3引数が要求される
val mapper: KRowMapper<Dst> = KRowMapper(::Dst) { /* キャメル -> スネークの命名変換関数 */ }
```

`fieldNameToPrefix = false`を指定した場合、`nameJoiner`オプションは無視されます。

###### nameJoiner
`nameJoiner`は引数名と引数名の結合方法の指定で、デフォルトでは`camelCase`が指定されており、`snake_case`と`kebab-case`のサポートも有ります。  
`NameJoiner`クラスを継承した`object`を作成することで自作することもできます。

##### 他のデシリアライズ方法との併用
`KParameterFlatten`アノテーションを付与した場合も、これまでに紹介したデシリアライズ方法は全て機能します。  
また、`InnerDst`の中で更に`KParameterFlatten`アノテーションを利用することもできます。

#### デシリアライズ方法早見
ここまでの内容をまとめたデシリアライズ方法の早見です。

- 1つの値から複数の引数に変換したい
  - コンストラクタ/ファクトリーメソッドで変換処理を書く
- 1つの値から1つの引数に変換したい
  - コンストラクタ/ファクトリーメソッドで変換処理を書く
  - [`KColumnDeserializer`アノテーションを用いる](#KColumnDeserializerアノテーションを利用したデシリアライズ)
  - [デシリアライズアノテーションを自作して付与する](#デシリアライズアノテーションを定義する)
  - （[`KParameterFlatten`アノテーションを用いる](#複数引数からのデシリアライズ)）
- 複数の値から1つの引数に変換したい
  - コンストラクタ/ファクトリーメソッドで変換処理を書く
  - [`KParameterFlatten`アノテーションを用いる](#複数引数からのデシリアライズ)

### その他の機能
#### 引数名にエイリアスを付ける
以下のように、引数名とカラム名とで名前の定義が食い違う場合が有ります。

```kotlin
// idフィールドはDB上ではfoo_idという名前で登録されている
data class Foo(val id: Int)
```

このような場合、`KParameterAlias`アノテーションを用いることで、`DB`上のカラム名に合わせたマッピングが可能になります。

```kotlin
data class Foo(
    @param:KParameterAlias("fooId")
    val id: Int
)
```

`KParameterAlias`で設定したエイリアスにも引数名の変換が適用されます。

#### デフォルト引数を用いる
`KRowMapper`では、特定の場面においてデフォルト引数を用いることができます。

##### 必ずデフォルト引数を用いる
DBから取得した値を用いず、必ずデフォルト引数を用いたい場合、`KUseDefaultArgument`アノテーションを利用できます。

```kotlin
class Foo(
    ...,
    @KUseDefaultArgument
    val description: String = ""
)
```

`KRowMapper`で`ResultSet`に存在しないフィールドを取得しようとした場合、通常では例外で落ちますが、`KUseDefaultArgument`アノテーションを付与している場合取得処理そのものが行われません。  
これを応用することで、正常にマッピングを行うこともできます。

##### 取得結果がnullの場合デフォルト引数を用いる
取得結果が`null`であればデフォルト引数を用いたいという場合、`KParameterRequireNonNull`アノテーションを利用できます。

```kotlin
class Foo(
    ...,
    @KParameterRequireNonNull
    val description: String = ""
)
```

#### Enumをデシリアライズする
DBに格納された値と`Enum::name`プロパティが一致する場合、特別な記述無しに`Enum`をデシリアライズすることができます。
