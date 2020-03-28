package com.mapk.krowmapper;

import com.mapk.annotations.KColumnDeserializer;

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
