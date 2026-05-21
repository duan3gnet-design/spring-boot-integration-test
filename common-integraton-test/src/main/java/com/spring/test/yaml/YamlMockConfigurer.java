package com.spring.test.yaml;

import tools.jackson.databind.json.JsonMapper;
import com.spring.test.yaml.model.MockSpec;
import com.spring.test.yaml.model.MockStub;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Cấu hình Mockito stub từ định nghĩa YAML.
 * Bean phải là Mockito mock (ví dụ {@code @MockitoBean} / {@code @MockBean}).
 */
public class YamlMockConfigurer {

    private static final Logger log = LoggerFactory.getLogger(YamlMockConfigurer.class);

    private final ApplicationContext applicationContext;
    private final JsonMapper jsonMapper;

    public YamlMockConfigurer(ApplicationContext applicationContext, JsonMapper jsonMapper) {
        this.applicationContext = applicationContext;
        this.jsonMapper = jsonMapper;
    }

    public void applyMocks(List<MockSpec> mocks) {
        if (mocks == null || mocks.isEmpty()) {
            return;
        }
        for (MockSpec mock : mocks) {
            Object bean = resolveBean(mock);
            if (!Mockito.mockingDetails(bean).isMock()) {
                log.warn("Bean '{}' is not a Mockito mock — skip YAML stubs", mock.getBean());
                continue;
            }
            for (MockStub stub : mock.getStubs()) {
                applyStub(bean, stub);
            }
        }
    }

    public void resetMocks(List<MockSpec> mocks) {
        if (mocks == null) {
            return;
        }
        for (MockSpec mock : mocks) {
            try {
                Object bean = resolveBean(mock);
                if (Mockito.mockingDetails(bean).isMock()) {
                    Mockito.reset(bean);
                }
            } catch (Exception e) {
                log.debug("Skip reset mock {}: {}", mock.getBean(), e.getMessage());
            }
        }
    }

    private Object resolveBean(MockSpec mock) {
        if (mock.getBean() != null && !mock.getBean().isBlank()) {
            return applicationContext.getBean(mock.getBean());
        }
        if (mock.getType() != null && !mock.getType().isBlank()) {
            try {
                Class<?> type = Class.forName(mock.getType());
                return applicationContext.getBean(type);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Unknown mock type: " + mock.getType(), e);
            }
        }
        throw new IllegalArgumentException("Mock spec requires 'bean' or 'type'");
    }

    private void applyStub(Object bean, MockStub stub) {
        Method method = findMethod(bean, stub.getMethod(), stub.getArgs().size());
        Object[] args = coerceArgs(method.getParameterTypes(), stub.getArgs());
        try {
            if (stub.getThrowsClass() != null) {
                Throwable throwable = createThrowable(stub);
                stubThrow(bean, method, args, throwable);
            } else if (method.getReturnType() == void.class) {
                stubVoid(bean, method, args);
            } else {
                Object returnValue = coerceReturn(method.getReturnType(), stub.getReturnValue());
                when(method.invoke(bean, args)).thenAnswer(invocation -> returnValue);
            }
            log.debug("Applied mock {}.{}(...)", bean.getClass().getSimpleName(), stub.getMethod());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to apply mock stub " + bean.getClass().getSimpleName() + "." + stub.getMethod(), e);
        }
    }

    /**
     * Đăng ký stub throw — dùng {@code doAnswer/doThrow.when(bean)} rồi {@code method.invoke}
     * để Mockito ghi nhận lời gọi (tránh {@code thenReturn(Object)} không khớp kiểu primitive).
     */
    private void stubThrow(Object bean, Method method, Object[] args, Throwable throwable) throws Exception {
        doAnswer(invocation -> {
            throw throwable;
        }).when(bean);
        method.invoke(bean, args);
    }

    private void stubVoid(Object bean, Method method, Object[] args) throws Exception {
        doAnswer(invocation -> null).when(bean);
        method.invoke(bean, args);
    }

    private Method findMethod(Object bean, String name, int argCount) {
        Class<?> type = bean.getClass();
        if (Mockito.mockingDetails(bean).isMock() && type.getName().contains("$")) {
            type = firstInterface(type);
        }

        List<Method> candidates = new ArrayList<>();
        collectMethods(type, name, argCount, candidates);
        for (Class<?> iface : type.getInterfaces()) {
            collectMethods(iface, name, argCount, candidates);
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "Method not found: " + type.getName() + "." + name + " with " + argCount + " args");
        }
        if (candidates.size() > 1) {
            log.warn("Multiple methods match {}.{} — using {}", type.getSimpleName(), name,
                    candidates.getFirst().getName());
        }
        return candidates.getFirst();
    }

    private void collectMethods(Class<?> type, String name, int argCount, List<Method> candidates) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == argCount) {
                candidates.add(method);
            }
        }
    }

    private Class<?> firstInterface(Class<?> type) {
        Class<?>[] interfaces = type.getInterfaces();
        return interfaces.length > 0 ? interfaces[0] : type;
    }

    private Object[] coerceArgs(Class<?>[] paramTypes, List<Object> yamlArgs) {
        Object[] result = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Object raw = i < yamlArgs.size() ? yamlArgs.get(i) : null;
            result[i] = jsonMapper.convertValue(raw, paramTypes[i]);
        }
        return result;
    }

    private Object coerceReturn(Class<?> returnType, Object raw) {
        if (raw == null) {
            return defaultValue(returnType);
        }
        if (returnType.isInstance(raw)) {
            return raw;
        }
        return jsonMapper.convertValue(raw, returnType);
    }

    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }

    private Throwable createThrowable(MockStub stub) {
        String className = stub.getThrowsClass();
        String message = stub.getThrowsMessage() != null ? stub.getThrowsMessage() : "";
        try {
            Class<?> type = Class.forName(className);
            return (Throwable) type.getConstructor(String.class).newInstance(message);
        } catch (Exception e) {
            return new RuntimeException(className + ": " + message);
        }
    }
}
