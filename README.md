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
