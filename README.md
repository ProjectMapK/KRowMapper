[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CircleCI](https://circleci.com/gh/ProjectMapK/KRowMapper.svg?style=svg)](https://circleci.com/gh/ProjectMapK/KRowMapper)
[![](https://jitci.com/gh/ProjectMapK/KRowMapper/svg)](https://jitci.com/gh/ProjectMapK/KRowMapper)

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

### Deserialize column
`KRowMapper` provides a deserialization function for the acquisition results of three patterns.

- Deserialize at initialization using the `factory method` or deserialize on initialize.
- Define a deserializer for the `class`.
- Define custom deserializer `annotation`.

#### Deserialize at initialization using the factory method or deserialize on initialize
Deserialization within a `factory method` or initialization is the simplest method.  
Also, deserialization from multiple columns to one argument or from one column to multiple arguments cannot be realized other than this method.

```kotlin
// example of deserialize on factory method
data class Dst(
    foo: Foo,
    bar: Bar,
    baz: Baz?,
    ...
) {
    companion object {
        fun factory(
            foo: String,
            bar: String,
            baz: Int?,
            ...
        ): Dst {
            return Dst(
                Foo(foo),
                Bar.fromString(bar),
                baz?.let { Baz(it) },
                ...
            )
        }
    }
}

val dst: Dst = jdbcTemplate.query(query, KRowMapper((Dst)::factory))
```

#### Define a deserializer for the class
By assigning the `KColumnDeserializer` `annotation` to the `constructor` or `factory method`, deserialization by the `KFunction` can be performed.  
The `method` that assigns this `annotation` must have one argument.

When the deserializer is defined in this way, the destination will be as follows.

```kotlin
data class Dst(
    val foo: ByConstructor,
    val bar: BySecondaryConstructor,
    val baz: ByCompanionObject,
    val qux: ByStaticMethod
)
```

##### constructor
```kotlin
data class ByConstructor @KColumnDeserializer constructor(val fooString: String)
```

##### secondary constructor
```kotlin
data class BySecondaryConstructor(val barShort: Short) {
    @KColumnDeserializer
    constructor(bar: String) : this(bar.toShort())
}
``` 

##### factory method
```kotlin
data class ByCompanionObject(val bazInt: Int) {
    companion object {
        @KColumnDeserializer
        fun factory(baz: String) = ByCompanionObject(baz.toInt())
    }
}
```

##### (static method)
`Java` `static method` is also supported.

```java
public class ByStaticMethod {
    private final String quxString;

    public ByStaticMethod(String quxString) {
        this.quxString = quxString;
    }

    public String getQuxString() {
        return quxString;
    }

    @KColumnDeserializer
    public static ByStaticMethod factory(Integer quxArg) {
        return new ByStaticMethod(quxArg.toString());
    }
}
```

#### Define custom deserializer annotation
`KRowMapper` supports complex deserialization by defining custom deserializer `annotations`.  
As an example, a custom deserializer `annotation` that performs deserialization from a `String` to `LocalDateTime` is shown.

```kotlin
// annotation
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@KColumnDeserializeBy(LocalDateTimeDeserializerImpl::class)
annotation class LocalDateTimeDeserializer(val pattern: String = "yyyy-MM-dd'T'HH:mm:ss")

// deserializer
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

```kotlin
// usage
data class Dst(@LocalDateTimeDeserializer val dateTime: LocalDateTime)
```

##### annotation
For the `annotation class`, specify the deserializer `class` with the `KColumnDeserializeBy` `annotation`.  
Also, the fields prepared for this `annotation class` can be used from the deserializer.

```kotlin
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
// LocalDateTimeDeserializerImpl is deserializer class
@KColumnDeserializeBy(LocalDateTimeDeserializerImpl::class)
annotation class LocalDateTimeDeserializer(val pattern: String = "yyyy-MM-dd'T'HH:mm:ss")
```

##### deserializer
Deserializer is created by inheriting `AbstractKColumnDeserializer`.  
The meaning of each type parameter is as follows.

- `A`: `Annotation class` (`LocalDateTimeDeserializer` in this example)
- `S`: `Java class` of argument required at deserialization
- `D`: `Class` returned after deserialization

```kotlin
abstract class AbstractKColumnDeserializer<A : Annotation, S : Any, D : Any>(protected val annotation: A) {
    abstract val srcClass: Class<S>
    abstract fun deserialize(source: S?): D?
}
```

In the example, deserialization from a `String` to `LocalDateTime` is performed based on the `pattern` obtained from the
 `LocalDateTimeDeserializer`.

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

### Use default arguments
`KRowMapper` supports `default arguments`.  
`Default arguments` are available in the following situations:

- When not referring to the acquisition result
- When the acquisition result is `null`

As of `KRowMapper` 0.8, it does not support the use of `default argument` when columns cannot be obtained.

#### When not referring to the acquisition result
When the `KUseDefaultArgument` `annotation` is added to the parameter,
 the `default argument` can be used forcibly without referring to the obtained result.

```kotlin
data class Dst(val fooId: Int, @param:KUseDefaultArgument val barValue: String = "default")
```

#### When the acquisition result is null
When `KParameterRequireNonNull` `annotation` is given to a parameter,
 the default argument can be used if the obtained result is `null`.

```kotlin
data class Dst(val fooId: Int, @param:KParameterRequireNonNull val barValue: String = "default")
```

### Parameter aliasing
In `KRowMapper`, the column name of the acquisition target can be specified by giving the `KParameterAlias` `annotation` to the `parameter`.  
The name conversion function is applied to the name specified here.

```kotlin
data class Dst(@param:KParameterAlias("fooId") val barValue: Int)
```
