package com.spring.test.yaml.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Khai báo một WireMock server trong {@link EnableWireMock}.
 *
 * <pre>
 * &#64;EnableWireMock(
 *     &#64;WireMockServer(name = "payment-service", port = 0)
 * )
 * </pre>
 *
 * {@code port = 0} → dynamic port (khuyến nghị).
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface WireMockServer {

    /**
     * Tên server — phải khớp với {@code wireMocks[].server} trong YAML.
     * Để trống nếu chỉ có một server duy nhất (dùng làm default).
     */
    String name() default "";

    /**
     * Port lắng nghe. {@code 0} = dynamic port (mặc định, khuyến nghị).
     */
    int port() default 0;
}
