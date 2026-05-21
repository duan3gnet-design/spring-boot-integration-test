package com.spring.test.yaml.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Chỉ định file YAML chứa test cases (classpath).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface YamlTest {

    /** Classpath location, ví dụ {@code yaml/transaction-tests.yml} */
    String value();
}
