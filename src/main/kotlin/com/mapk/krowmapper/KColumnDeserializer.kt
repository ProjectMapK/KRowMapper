package com.mapk.krowmapper

abstract class KColumnDeserializer<Src : Any, Dst : Any> {
    abstract fun deserialize(column: Src): Dst?
}
