[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CircleCI](https://circleci.com/gh/ProjectMapK/KRowMapper.svg?style=svg)](https://circleci.com/gh/ProjectMapK/KRowMapper)
[![](https://jitci.com/gh/ProjectMapK/KRowMapper/svg)](https://jitci.com/gh/ProjectMapK/KRowMapper)

KRowMapper
====
This is a `RowMapper` like a `BeanPropertyRowMapper` for `Kotlin`.  
You can call `KFunction`(e.g. `method reference`) from `ResultSet`.

```kotlin
// before
val dst = jdbcTemplate.query(query) { rs, _ ->
    Dst(
            rs.getString("foo"),
            rs.getString("bar"),
            rs.getInt("baz"),
            ...
    )
}

// after
val dst = jdbcTemplate.query(query, KRowMapper(::Dst))
```

## How to use
Published on JitPack.  
Please see [here](https://jitpack.io/#ProjectMapK/KRowMapper/) for the introduction method.  
