[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CircleCI](https://circleci.com/gh/ProjectMapK/KRowMapper.svg?style=svg)](https://circleci.com/gh/ProjectMapK/KRowMapper)
[![](https://jitci.com/gh/ProjectMapK/KRowMapper/svg)](https://jitci.com/gh/ProjectMapK/KRowMapper)
[![codecov](https://codecov.io/gh/ProjectMapK/KRowMapper/branch/master/graph/badge.svg)](https://codecov.io/gh/ProjectMapK/KRowMapper)

---

[日本語版](https://github.com/ProjectMapK/KRowMapper/blob/master/README.ja.md)

---

KRowMapper
====
`KRowMapper` is a `RowMapper` for `Kotlin`, which provides the following features.

- Object relationship mapping with minimal effort, equivalent to `BeanPropertyRowMapper`.
- Flexible and safe mapping based on function calls with `reflection`.
- Richer features and thus more flexible and labor-saving mapping.

## Demo code
Here is a comparison between writing the mapping code manually and using `KRowMapper`.  
The more arguments you write manually, the more you need to write, but if you use `KRowMapper` This allows you to do the mapping without writing any code.  
Also, you don't need external configuration file at all.

However, if the naming conventions of arguments and DB columns are different, you will need to pass a naming conversion function.  
Please note that there are (see below).

```kotlin
// mapping destination
data class Dst(
    foo: String,
    bar: String,
    baz: Int?,

    ...

)

// If you write RowMapper manually.
val dst: Dst = jdbcTemplate.query(query) { rs, _ ->
    Dst(
            rs.getString("foo"),
            rs.getString("bar"),
            rs.getInt("baz"),

            ...

    )
}

// If you use KRowMapper
val dst: Dst = jdbcTemplate.query(query, KRowMapper(::Dst, /* Naming transformation functions as needed. */))
```

## Installation
`KRowMapper` is published on JitPack.  
You can use this library on `maven`, `gradle` and any other build tools.  
Please see [here](https://jitpack.io/#ProjectMapK/KRowMapper/) for the formal introduction method. 

### Example on maven
**1. add repository reference for JitPack**

```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

**2. add dependency**

```xml
	<dependency>
	    <groupId>com.github.ProjectMapK</groupId>
	    <artifactId>KRowMapper</artifactId>
	    <version>Tag</version>
	</dependency>
```

## Principle of operation
The behavior of `KRowMapper` is as follows.

1. Get the `KFunction` to be called.
2. Analyze the `KFunction` and determine what arguments are needed and how to deserialize them.
3. Get the value for each argument from the `ResultSet` and deserialize it. and call the `KFunction`.

`KRowMapper` performs the mapping by calling a `function`, so the result is a Subject to the constraints on the `argument` and `nullability`.  
That is, there is no runtime error due to breaking the `null` safety of `Kotlin`(The `null` safety on type arguments may be broken due to problems on the `Kotlin` side). 

Also, it supports the default arguments which are peculiar to `Kotlin`.

## Initialization
`KRowMapper` can be initialized from `method reference(KFunction)` to be called or the `KClass` to be mapped.

Also, by default, `KRowMapper` compares argument names and column names to see if they correspond.  
Therefore, in the case of "argument name is `camelCase` and column name is `snake_case`", it is necessary to pass a function that appropriately converts the naming convention of the argument name.

### Initialization from method reference(KFunction)
You can initialize `KRowMapper` from `method reference(KFunction)` as follows It is.

```kotlin
data class Dst(
    foo: String,
    bar: String,
    baz: Int?,

    ...

)

// get constructor method reference
val dstConstructor: KFunction<Dst> = ::Dst
// initialize KRowMapper from KFunction
val mapper: KRowMapper<Dst> = KRowMapper(dstConstructor)
```

The following three methods are the main ways to get the `method reference`.

- from `constructor`: `::Dst`
- from `factory method` in `companion object`: `(Dst)::factoryMethod`
- from instance method in `this` scope: `this::factoryMethod`

### Initialization from KClass
The `KRowMapper` can be initialized by `KClass`.  
By default, the primary constructor is the target of the call.

```kotlin
data class Dst(...)

val mapper: KRowMapper<Dst> = KRowMapper(Dst::class)
```


You can also write the following using a `dummy constructor`.

```kotlin
val mapper: KRowMapper<Dst> = KRowMapper<Dst>()
```

#### Specifying the target of a call by KConstructor annotation
When you initialize from the `KClass`, you can use the `KConstructor` annotation and to specify the function to be called. 

In the following example, the `secondary constructor` is called.

```kotlin
data class Dst(...) {
    @KConstructor
    constructor(...) : this(...)
}

val mapper: KRowMapper<Dst> = KRowMapper(Dst::class)
```

Similarly, the following example calls the `factory method`.

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

### Conversion of argument names
By default, `KRowMapper` looks for the column corresponding to the argument name.

```kotlin
data class Dst(
    fooFoo: String,
    barBar: String,
    bazBaz: Int?
)

// required arguments: fooFoo, barBar, bazBaz
val mapper: KRowMapper<Dst> = KRowMapper(::Dst)

// the behavior is equivalent to the following
val rowMapper: RowMapper<Dst> = { rs, _ ->
    Dst(
            rs.getString("fooFoo"),
            rs.getString("barBar"),
            rs.getInt("bazBaz"),
    )
}
```

On the other hand, if the argument naming convention is a `camelCase` and the DB column naming convention is a `snake_case` You will not be able to see the match in this case.  
In this situation, you need to pass the naming transformation function at the initialization of `KRowMapper`. 

```kotlin
val mapper: KRowMapper<Dst> = KRowMapper(::Dst) { fieldName: String ->
    /* some naming transformation process */
}
```

#### The actual conversion process
Since `KRowMapper` does not provide the naming transformation process, the naming transformation process requires an external library.  

As an example, sample code that passes the conversion process from `camelCase` to `snake_case` is shown for two libraries, `Jackson` and `Guava`.
These libraries are often used by `Spring framework` and other libraries.

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

## Detailed usage
By using the contents described so far, you can perform more flexible and safe mapping compared to `BeanPropertyRowMapper`.
In addition, by making full use of the abundant functions provided by `KRowMapper`, further labor saving is possible.

### Deserialization
Since `KRowMapper` gets the value from `java.sql.ResultSet`, by default it is not possible to get the type which is not supported by this implementation.
To deal with this problem, `KRowMapper` provides the following three types of deserialization methods in addition to the default conversion function.

1. Deserialization by using the `KColumnDeserializer` annotation.
2. Deserialization by creating your own custom deserialization annotations.
3. Deserialization from multiple arguments.

#### Deserialization by using the KColumnDeserializer annotation
If it is a self-made class and can be initialized from a single argument, deserialization using the `KColumnDeserializer` annotation can be used.
`KColumnDeserializer` annotation can be used to `constructor` or `factory method` defined in `companion object`.

```kotlin
// for primary constructor
data class FooId @KColumnDeserializer constructor(val id: Int)
```

```kotlin
// for secondary constructor
data class FooId(val id: Int) {
    @KColumnDeserializer
    constructor(id: String) : this(id.toInt())
}
```

```kotlin
// for factory method
data class FooId(val id: Int) {
    companion object {
        @KColumnDeserializer
        fun of(id: String): FooId = FooId(id.toInt())
    }
}
```

Class with `KColumnDeserializer` annotation can be mapped as an argument without any special description.

```kotlin
data class Dst(
    fooId: FooId,
    bar: String,
    baz: Int?,

    ...

)
```

#### Deserialization by creating your own custom deserialization annotations
If you cannot use `KColumnDeserializer`, you can deserialize it by creating a custom deserialization annotations and adding it to the parameter.  


Custom deserialization annotation is made by defining a pair of `deserialization annotation` and `deserializer`.
As an example, we will show how to create a `LocalDateTimeDeserializer` that deserializes from `String` to `LocalDateTime`.

##### Create deserialization annotation
`@Target(AnnotationTarget.VALUE_PARAMETER)` and `KColumnDeserializeBy` annotation and several other annotations You can define a deserialization annotation by assigning a `KColumnDeserializeBy` annotation.

The argument of the `KColumnDeserializeBy` annotation requires the `KClass` of the deserializer.  
In this example, it is `LocalDateTimeDeserializerImpl`.

Also, arguments defined in the annotation can be referenced from the deserializer.

```kotlin
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@KColumnDeserializeBy(LocalDateTimeDeserializerImpl::class)
annotation class LocalDateTimeDeserializer(val pattern: String = "yyyy-MM-dd'T'HH:mm:ss")
```

##### Create deserializer
You can define `deserializer` by inheriting `AbstractKColumnDeserializer <A, S, D>`.  
Generics `A`,`S`,`D` have the following meanings.

- `A`: `deserialization annotation` `Type`.
- `S`: Source `Type`.
- `D`: Destination `Type`.

```kotlin
class LocalDateTimeDeserializerImpl(
    annotation: LocalDateTimeDeserializer
) : AbstractKColumnDeserializer<LocalDateTimeDeserializer, String, LocalDateTime>(annotation) {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(annotation.pattern)

    override val srcClass: Class<String> = String::class.javaObjectType

    override fun deserialize(source: String?): LocalDateTime? {
        return source?.let {
            LocalDateTime.parse(it, formatter)
        }
    }
}
```

The `primary constructor` of the `deserializer` must request only the `deserialization annotation` specified in the `generics` as an argument.  
This is called when `KRowMapper` is initialized.

As shown in the example, you can refer to the annotation argument as you wish.

##### Using custom deserialization annotations
The following is a summary of the `deserialization annotation` and `deserializer` created so far.

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

    override fun deserialize(source: String?): LocalDateTime? {
        return source?.let {
            LocalDateTime.parse(it, formatter)
        }
    }
}
```

If you give it, you get the following.  
Since we can pass arbitrary arguments to `pattern`, we can see that it is highly flexible.

```kotlin
data class Dst(
        @LocalDateTimeDeserializer(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        val createTime: LocalDateTime
)
```

#### Deserialization from multiple arguments
If `InnerDst` requires multiple arguments, you can not map `Dst` with `KRwoMapper` as it is.  
You can deserialize such a class which requires multiple arguments by using the `KParameterFlatten` annotation.

```kotlin
data class InnerDst(val fooFoo: Int, val barBar: String)
data class Dst(val bazBaz: InnerDst, val quxQux: LocalDateTime)
```

If the column name of `DB` is `snake_case` and you want to specify the argument name as a prefix, the following annotation is added.  
The class specified with `KParameterFlatten` is initialized from the function specified with the aforementioned `KConstructor` annotation or the `primary constructor`.

```kotlin
data class InnerDst(val fooFoo: Int, val barBar: String)
data class Dst(
    @KParameterFlatten(nameJoiner = NameJoiner.Snake::class)
    val bazBaz: InnerDst,
    val quxQux: LocalDateTime
)

// required 3 arguments that baz_baz_foo_foo, baz_baz_bar_bar, qux_qux
val mapper: KRowMapper<Dst> = KRowMapper(::Dst) { /* some naming transformation process */ }
```

##### Annotation options of KParameterFlatten
The `KParameterFlatten` annotation has two options for handling argument names.

###### fieldNameToPrefix
By default, the `KParameterFlatten` annotation tries to find a match by prefixing the name of the argument with the name of the prefix.  
If you don't want to prefix the argument names, you can set the `fieldNameToPrefix` option to `false`.

```kotlin
data class InnerDst(val fooFoo: Int, val barBar: String)
data class Dst(
    @KParameterFlatten(fieldNameToPrefix = false)
    val bazBaz: InnerDst,
    val quxQux: LocalDateTime
)

// required 3 arguments that foo_foo, bar_bar, qux_qux
val mapper: KRowMapper<Dst> = KRowMapper(::Dst) { /* some naming transformation process */ }
```

If `fieldNameToPrefix = false` is specified, the `nameJoiner` option is ignored.

##### nameJoiner
The `nameJoiner` specifies how to join argument names to argument names.  
By default, `camelCase` is specified, and `snake_case` and `kebab-case` are also supported.  
You can also write your own by creating `object` which extends the `NameJoiner` class.

##### Use with other deserialization methods
The `KParameterFlatten` annotation also works with all the deserialization methods we have introduced so far.  
Also, you can use further `KParameterFlatten` annotations in `InnerDst`.

#### A quick look at how to deserialize
Here's a quick look at how to deserialize the content so far.

- I want to convert from a single value to multiple arguments.
  - Use the `constructor` or `factory methods` to perform the conversion.
- I want to convert a single value to a single argument.
  - Use the `constructor` or `factory methods` to perform the conversion.
  - Use `KColumnDeserializer` annotation.
  - Use custom deserialization annotations.
  - (Use `KParameterFlatten` annotation.)
- I want to convert single argument from multiple values.
  - Use the `constructor` or `factory methods` to perform the conversion.
  - Use `KParameterFlatten` annotation.

### Other Features
#### Adding an alias to argument names
There is a case where the name of the argument and the column name are different as follows.

```kotlin
// The argument "id" is defined in the DB as "foo_id".
data class Foo(val id: Int)
```

In such a case, the `KParameterAlias` annotation is used to make the mapping according to the column name on the DB.

```kotlin
data class Foo(
    @param:KParameterAlias("fooId")
    val id: Int
)
```

The transformation of the argument name is also applied to the alias set by `KParameterAlias`.

#### Use default arguments
The `KRowMapper` allows you to use default arguments in certain situations.

##### Always use the default arguments
If you don't want to use the value from the DB and always use the default argument, you can use the `KUseDefaultArgument` annotation.

```kotlin
class Foo(
    ...,
    @KUseDefaultArgument
    val description: String = ""
)
```

##### Use default argument if the result is null
If you want to use the default argument if the result is `null`, you can use the `KParameterRequireNonNull` annotation.

```kotlin
class Foo(
    ...,
    @KParameterRequireNonNull
    val description: String = ""
)
```

#### Deserialize Enum
If the value stored in the DB and the `Enum::name` property of the map destination are the same, it will be automatically converted to You can deserialize the `Enum`.
