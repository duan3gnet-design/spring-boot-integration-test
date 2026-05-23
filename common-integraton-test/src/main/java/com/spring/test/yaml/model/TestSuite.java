package com.spring.test.yaml.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Root document đọc từ file YAML.
 */
public class TestSuite {

    private String suite;
    private SuiteSetup setup = new SuiteSetup();
    private List<MockSpec> mocks = new ArrayList<>();
    private List<WireMockSpec> wireMocks = new ArrayList<>();
    private List<TestCase> tests = new ArrayList<>();

    public String getSuite() {
        return suite;
    }

    public void setSuite(String suite) {
        this.suite = suite;
    }

    public SuiteSetup getSetup() {
        return setup;
    }

    public void setSetup(SuiteSetup setup) {
        this.setup = setup != null ? setup : new SuiteSetup();
    }

    public List<MockSpec> getMocks() {
        return mocks;
    }

    public void setMocks(List<MockSpec> mocks) {
        this.mocks = mocks != null ? mocks : new ArrayList<>();
    }

    public List<WireMockSpec> getWireMocks() {
        return wireMocks;
    }

    public void setWireMocks(List<WireMockSpec> wireMocks) {
        this.wireMocks = wireMocks != null ? wireMocks : new ArrayList<>();
    }

    public List<TestCase> getTests() {
        return tests;
    }

    public void setTests(List<TestCase> tests) {
        this.tests = tests != null ? tests : new ArrayList<>();
    }

    public static class SuiteSetup {
        private List<String> truncateTables = new ArrayList<>();
        private List<String> scripts = new ArrayList<>();
        private Map<String, Object> properties = new LinkedHashMap<>();

        public List<String> getTruncateTables() {
            return truncateTables;
        }

        public void setTruncateTables(List<String> truncateTables) {
            this.truncateTables = truncateTables != null ? truncateTables : new ArrayList<>();
        }

        public List<String> getScripts() {
            return scripts;
        }

        public void setScripts(List<String> scripts) {
            this.scripts = scripts != null ? scripts : new ArrayList<>();
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties != null ? properties : new LinkedHashMap<>();
        }
    }
}
