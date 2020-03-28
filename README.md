[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CircleCI](https://circleci.com/gh/ProjectMapK/KRowMapper.svg?style=svg)](https://circleci.com/gh/ProjectMapK/KRowMapper)
[![](https://jitci.com/gh/ProjectMapK/KRowMapper/svg)](https://jitci.com/gh/ProjectMapK/KRowMapper)

KRowMapper
====
This is a `RowMapper` like a `BeanPropertyRowMapper` for `Kotlin`.  
You can call `KFunction`(e.g. `method reference`) from `ResultSet`.

```kotlin
data class Dst(
    foo: String,
    bar: String,
    baz: Int?,
    ...
)

// before
val dst: Dst = jdbcTemplate.query(query) { rs, _ ->
    Dst(
            rs.getString("foo"),
            rs.getString("bar"),
            rs.getInt("baz"),
            ...
    )
}

// after
val dst: Dst = jdbcTemplate.query(query, KRowMapper(::Dst))
```

## Usage
### Convert Naming conventions
`KRowMapper` searches columns by default in camel case.  
If the DB is named in snake case, mapping can be done by passing a conversion function(e.g. defined in `JackSon`, `Guava`) to `KRowMapper`.

```kotlin
// if use Guava.
KRowMapper(::Dst) { colName: String ->
    CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, colName)
}
```

### Deserialize column
`KRowMapper` provides a deserialization function for the acquisition results of three patterns.

- Define a deserializer for the `class`.
- Define custom deserializer `annotation`.
- Deserialize at initialization using the `backing property`.

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

## Installation
Published on JitPack.  
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
