package com.mapk.krowmapper;

import com.mapk.annotations.KColumnDeserializer;

public class ByStaticMethod {
    private final String bazString;

    public ByStaticMethod(String bazString) {
        this.bazString = bazString;
    }

    public String getBazString() {
        return bazString;
    }

    @KColumnDeserializer
    public static ByStaticMethod factory(Integer bazArg) {
        return new ByStaticMethod(bazArg.toString());
    }
}
