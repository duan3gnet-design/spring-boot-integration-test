package com.spring.test.yaml.model;

import java.util.ArrayList;
import java.util.List;

public class MockStub {

    private String method;
    private List<Object> args = new ArrayList<>();
    private Object returnValue;
    private String throwsClass;
    private String throwsMessage;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Object> getArgs() {
        return args;
    }

    public void setArgs(List<Object> args) {
        this.args = args != null ? args : new ArrayList<>();
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    /** Alias YAML: return */
    public void setReturn(Object returnValue) {
        this.returnValue = returnValue;
    }

    public String getThrowsClass() {
        return throwsClass;
    }

    public void setThrowsClass(String throwsClass) {
        this.throwsClass = throwsClass;
    }

    public String getThrowsMessage() {
        return throwsMessage;
    }

    public void setThrowsMessage(String throwsMessage) {
        this.throwsMessage = throwsMessage;
    }

    /** Alias YAML: throws */
    public void setThrows(String throwsExpression) {
        if (throwsExpression == null || throwsExpression.isBlank()) {
            return;
        }
        int colon = throwsExpression.indexOf(':');
        if (colon > 0) {
            this.throwsClass = throwsExpression.substring(0, colon).trim();
            this.throwsMessage = throwsExpression.substring(colon + 1).trim();
        } else {
            this.throwsClass = throwsExpression.trim();
        }
    }
}
