package dev.nullzwo.interview.refactoring.errors;

import java.util.Map;

public class ApplicationException extends RuntimeException {
    private final int statusCode;
    private final Map<String, String> headers;

    public <K, V> ApplicationException(int statusCode, String reason, Map<String, String> headers) {
        super(reason);
        this.statusCode = statusCode;
        this.headers = headers;
    }
}
