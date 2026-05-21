package com.spring.test.yaml;

import com.spring.test.yaml.model.TestCase;
import com.spring.test.yaml.model.TestSuite;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Đọc file YAML (classpath hoặc filesystem) thành {@link TestSuite}.
 */
public final class YamlTestDataReader {

    private final YAMLMapper yamlMapper;

    public YamlTestDataReader() {
        this.yamlMapper = YAMLMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public YamlTestDataReader(YAMLMapper yamlMapper) {
        this.yamlMapper = yamlMapper;
    }

    public TestSuite loadClasspath(String classpathLocation) {
        String normalized = classpathLocation.startsWith("/") ? classpathLocation : "/" + classpathLocation;
        InputStream in = YamlTestDataReader.class.getResourceAsStream(normalized);
        if (in == null) {
            throw new IllegalArgumentException("YAML not found on classpath: " + classpathLocation);
        }
        try (in) {
            return yamlMapper.readValue(in, TestSuite.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read YAML: " + classpathLocation, e);
        }
    }

    public TestSuite loadFile(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return yamlMapper.readValue(in, TestSuite.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read YAML: " + path, e);
        }
    }

    public List<TestCase> loadEnabledCases(String classpathLocation) {
        return loadClasspath(classpathLocation).getTests().stream()
                .filter(TestCase::isEnabled)
                .toList();
    }
}
