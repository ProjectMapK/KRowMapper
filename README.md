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
- Define the original deserializer `annotation`.
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
