package com.spring.test.yaml;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thay thế biến {@code ${name}} trong path, body, headers.
 */
public final class YamlVariableResolver {

    private static final Pattern VARIABLE = Pattern.compile("\\$\\{([^}]+)}");

    private YamlVariableResolver() {}

    public static String resolve(String template, Map<String, Object> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }
        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = variables.get(name);
            if (value == null) {
                throw new IllegalArgumentException("Undefined YAML variable: ${" + name + "}");
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> resolveMap(Map<String, String> source, Map<String, Object> variables) {
        if (source == null) {
            return Map.of();
        }
        Map<String, String> resolved = new LinkedHashMap<>();
        source.forEach((k, v) -> resolved.put(
                resolve(k, variables),
                resolve(v, variables)));
        return resolved;
    }

    @SuppressWarnings("unchecked")
    public static Object resolveBody(Object body, Map<String, Object> variables) {
        if (body == null || variables == null || variables.isEmpty()) {
            return body;
        }
        if (body instanceof String str) {
            return resolve(str, variables);
        }
        if (body instanceof Map<?, ?> map) {
            Map<String, Object> resolved = new LinkedHashMap<>();
            map.forEach((k, v) -> resolved.put(
                    resolve(String.valueOf(k), variables),
                    resolveValue(v, variables)));
            return resolved;
        }
        return body;
    }

    private static Object resolveValue(Object value, Map<String, Object> variables) {
        if (value instanceof String str) {
            return resolve(str, variables);
        }
        if (value instanceof Map<?, ?> map) {
            return resolveBody(map, variables);
        }
        return value;
    }
}
